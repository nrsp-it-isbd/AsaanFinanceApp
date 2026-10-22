package com.assan.finance.utils

import android.util.Log
import com.assan.finance.network.assan.AssanRetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class LoanAssistantAiHelper {

    companion object {
        private const val TAG = "LoanAssistantAi"
    }

    private var cachedLoanHistory: String = "No previous financing history found."

    /** Real min/max amount per product, taken from the form (e.g. "Livestock: Rs. 10,000 - 750,000"). */
    private var productLimitsText: String = ""

    fun setProductLimits(text: String) {
        productLimitsText = text
    }
    private var validDistrictsList: List<String> = emptyList()
    private var validTehsilsList: List<String> = emptyList()
    private var validBanksList: List<String> = emptyList()
    private var validPurposesList: List<String> = emptyList()

    private val defaultPurposes = listOf(
        "Enterprise", "Tajarat", "Agriculture Inputs", "Agriculture Implements",
        "Livestock", "House improvement", "New House Construction", "Solar Tube wells"
    )

    fun initialize(
        loanHistoryJson: String,
        validDistricts: List<String>,
        validTehsils: List<String>,
        validBanks: List<String>,
        validPurposes: List<String>
    ) {
        this.cachedLoanHistory = loanHistoryJson.ifBlank { "No previous financing history found." }
        this.validDistrictsList = validDistricts.filter { it.isNotBlank() && !it.startsWith("Select", ignoreCase = true) }
        this.validTehsilsList = validTehsils.filter { it.isNotBlank() && !it.startsWith("Select", ignoreCase = true) }
        this.validBanksList = validBanks.filter { it.isNotBlank() && !it.startsWith("Select", ignoreCase = true) }
        val cleanedPurposes = validPurposes.filter { it.isNotBlank() && !it.startsWith("Select", ignoreCase = true) }
        this.validPurposesList = if (cleanedPurposes.isNotEmpty()) cleanedPurposes else defaultPurposes
    }

    interface AiResultCallback {
        fun onResult(response: JSONObject)
    }

    fun processUserInputAsync(
        userInput: String,
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject,
        callback: AiResultCallback
    ) = processUserInputAsync(userInput, conversationHistory, currentlyExtracted, false, callback)

    /** @param voiceMode true when the reply will be spoken (short, formal, no lists). */
    fun processUserInputAsync(
        userInput: String,
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject,
        voiceMode: Boolean,
        callback: AiResultCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = processUserInput(userInput, conversationHistory, currentlyExtracted, voiceMode)
            withContext(Dispatchers.Main) {
                callback.onResult(result)
            }
        }
    }

    suspend fun processUserInput(
        userInput: String,
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject,
        voiceMode: Boolean = false
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            // LAYER 1: shared thinking engine (reasons first, tries several Gemini models)
            try {
                val geminiResult = callDirectGemini(userInput, conversationHistory, currentlyExtracted, voiceMode)
                if (geminiResult != null && geminiResult.has("ai_reply")) {
                    Log.d(TAG, "Direct Gemini response succeeded")
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct Gemini call failed: ${e.message}")
            }

            // LAYER 2: Backend AI Endpoint Fallback
            try {
                val backendResult = callBackendAi(userInput, conversationHistory, currentlyExtracted)
                if (backendResult != null && backendResult.has("ai_reply")) {
                    Log.d(TAG, "Backend AI response succeeded")
                    return@withContext backendResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Backend AI call failed: ${e.message}")
            }

            // LAYER 3: Intelligent Local Conversational Engine (Offline & resilient fallback)
            Log.d(TAG, "Using intelligent local conversational engine")
            return@withContext processLocally(userInput, currentlyExtracted)
        }
    }

    /**
     * Asks the shared thinking engine. The model reasons about the user's intent, the rules and the
     * fields already filled, then returns strict JSON: ai_reply + extracted_fields + is_complete.
     */
    private suspend fun callDirectGemini(
        userInput: String,
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject,
        voiceMode: Boolean
    ): JSONObject? {
        val turns = conversationHistory.mapNotNull { msg ->
            val content = msg.optString("content", "").trim()
            if (content.isEmpty()) null
            else com.assan.finance.ai.AsaanAiEngine.Turn(
                fromUser = !(msg.optString("role").equals("assistant", true) || msg.optString("role").equals("model", true)),
                text = content
            )
        }
        val raw = com.assan.finance.ai.AsaanAiEngine.generate(
            systemPrompt = buildSystemInstruction(voiceMode),
            history = turns,
            userText = "Currently Extracted Fields: $currentlyExtracted\nUser Input: $userInput",
            json = true,
            thinkingBudget = if (voiceMode) 512 else 1024
        ) ?: return null
        val cleaned = raw.replace("```json", "").replace("```", "").trim()
        val parsed = JSONObject(cleaned.substring(cleaned.indexOf('{').coerceAtLeast(0)))
        if (!parsed.has("ai_reply")) return null
        val merged = mergeExtractedFields(currentlyExtracted, parsed.optJSONObject("extracted_fields"))
        parsed.put("extracted_fields", merged)
        return parsed
    }

    /**
     * Fallback call to the backend server endpoint.
     */
    private suspend fun callBackendAi(
        userInput: String,
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject
    ): JSONObject? {
        val promptBuilder = StringBuilder()
        promptBuilder.append(buildSystemInstruction(false)).append("\n\n")
        promptBuilder.append("Conversation History:\n")
        for (msg in conversationHistory) {
            val role = msg.optString("role", "user")
            val content = msg.optString("content", "")
            promptBuilder.append("$role: $content\n")
        }
        promptBuilder.append("\nCurrently Extracted Fields: $currentlyExtracted\n")
        promptBuilder.append("\nUser: $userInput\n")
        promptBuilder.append("Assistant (Respond strictly in JSON): ")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", promptBuilder.toString())
            .build()

        val response = AssanRetrofitClient.apiService.processVoiceChatText(body)
        val responseText = response.body()?.data?.ai_text ?: ""
        if (responseText.isNotEmpty()) {
            val cleaned = responseText.replace("```json", "").replace("```", "").trim()
            val parsed = JSONObject(cleaned)
            if (parsed.has("ai_reply")) {
                val newExtracted = parsed.optJSONObject("extracted_fields")
                val merged = mergeExtractedFields(currentlyExtracted, newExtracted)
                parsed.put("extracted_fields", merged)
                return parsed
            }
        }
        return null
    }

    /**
     * Ensures message roles strictly alternate between 'user' and 'model' as required by Gemini API.
     */
    private fun buildAlternatingContents(
        conversationHistory: List<JSONObject>,
        currentlyExtracted: JSONObject,
        userInput: String
    ): JSONArray {
        val rawTurns = mutableListOf<Pair<String, String>>()
        for (msg in conversationHistory) {
            val role = msg.optString("role", "user")
            val content = msg.optString("content", "").trim()
            if (content.isNotEmpty()) {
                val geminiRole = if (role.equals("assistant", ignoreCase = true) || role.equals("model", ignoreCase = true)) "model" else "user"
                rawTurns.add(Pair(geminiRole, content))
            }
        }

        val currentTurnText = "Currently Extracted Fields: $currentlyExtracted\nUser Input: $userInput"
        rawTurns.add(Pair("user", currentTurnText))

        val mergedTurns = mutableListOf<Pair<String, StringBuilder>>()
        for ((role, text) in rawTurns) {
            if (mergedTurns.isNotEmpty() && mergedTurns.last().first == role) {
                mergedTurns.last().second.append("\n").append(text)
            } else {
                mergedTurns.add(Pair(role, StringBuilder(text)))
            }
        }

        if (mergedTurns.isNotEmpty() && mergedTurns.first().first == "model") {
            mergedTurns.add(0, Pair("user", StringBuilder("Hello Asaan Assistant")))
        }

        val contentsArray = JSONArray()
        for ((role, sb) in mergedTurns) {
            val partObj = JSONObject().put("text", sb.toString())
            val msgObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(partObj))
            }
            contentsArray.put(msgObj)
        }
        return contentsArray
    }

    private fun buildSystemInstruction(voiceMode: Boolean): String {
        val sampleBanks = validBanksList.filter { !it.startsWith("Select", ignoreCase = true) }.take(25)
        val samplePurposes = validPurposesList.filter { !it.startsWith("Select", ignoreCase = true) }
        val sampleDistricts = validDistrictsList.filter { !it.startsWith("Select", ignoreCase = true) }.take(30)

        return """
            You are the "Asaan Financing Assistant" inside the Financing Application form of the Asaan Finance app (NRSP, Pakistan).
            The user talks to you in English, Roman Urdu or Urdu, by typing or by voice.
            Your purpose is to converse naturally and help the applicant complete, check or change their financing application,
            and to answer their questions about Asaan Financing along the way.

            ${com.assan.finance.ai.AsaanKnowledge.APP_FACTS}

            ${com.assan.finance.ai.AsaanKnowledge.THINKING_RULES}
            Also think about: which fields are already filled, which single detail is most useful to ask next,
            and whether the user is answering your last question, changing a value, or asking something new.

            ${com.assan.finance.ai.AsaanKnowledge.COMMON_RULES}

            ${if (voiceMode) com.assan.finance.ai.AsaanKnowledge.VOICE_RULES else com.assan.finance.ai.AsaanKnowledge.TEXT_RULES}

            === CRITICAL UNDERSTANDING & MODIFICATION RULES ===
            1. HONOR ALL FIELD MODIFICATIONS & CORRECTIONS IMMEDIATELY:
               - The user can CHANGE, UPDATE, or CORRECT any existing detail at any time (e.g. "Bank change kro meezan", "Amount 50000 kar do", "Raqam badlo", "Muddat 6 mahine kr dein", "Mera maqsad karobar hai").
               - You MUST IMMEDIATELY update the corresponding field in 'extracted_fields'!
               - Acknowledge the change warmly in the user's language (e.g. "Ji zaroor! Bank Meezan Bank mein update ho gaya hai. Ab batayein..." or "Sure! Your bank is now Meezan Bank.").
               - NEVER ignore the user's instructions or repeat the previous question as if nothing happened.
            2. ANSWER QUESTIONS WARMLY:
               - If the user asks a question (e.g. about interest/sood, markup rate, documents required, eligibility, or how financing works):
                 * Answer clearly and politely in the user's language, using only the facts above.
                 * Asaan Finance offers interest-free / easy-installment micro-financing; for exact terms refer to the branch or support.
                 * Then warmly invite them to provide the next required detail.
            3. LANGUAGE & GREETING TONE:
               - Reply in the user's language: English -> English, Roman Urdu or Urdu -> polite Pakistani Roman Urdu.
               - If user says 'Salam' / 'Assalam-o-Alaikum' / 'AOA', reply with 'Walaikum Assalam!'.
               - If user says 'Hi' / 'Hello' / 'Hey', reply naturally with 'Hello!' / 'Hi!'.
               - If user directly gives a detail or asks to change something without greeting, respond directly without a forced greeting.
               - NEVER use Devanagari / Hindi script.
               - NEVER use Hindi words ('kripya', 'dhanyawad', 'prapt', 'suvidha', 'jankaari', 'vyavastha'). Use Pakistani Roman Urdu words: 'meherbani', 'shukriya', 'theek hai', 'zaroor', 'jee bilkul', 'darkhwast'.
            
            === AVAILABLE OPTIONS & NORMALIZATION ===
            - bank_name: Match to the available bank in Pakistan from:
              ${sampleBanks.joinToString(", ")}
            - purpose_of_loan: Match to one of:
              ${samplePurposes.joinToString(", ")}
            - Amount limits per purpose (the ONLY valid limits; check the user's amount against the chosen purpose):
              ${productLimitsText.ifBlank { "shown in the form after the purpose is selected" }}
            - district: Match to available districts:
              ${sampleDistricts.joinToString(", ")}
            - loan_amount: Pure integer string without commas or PKR (e.g. "50000", "100000").
              "pachas hazar" = 50000, "ek lakh" = 100000, "dedh lakh" = 150000.
            - CNIC DETAILS COME ONLY FROM THE CNIC SCAN: applicant_name, applicant_cnic, date_of_birth, cnic_issue,
              cnic_expiry, gender, present_address and permanent_address are filled ONLY by scanning the CNIC
              (camera or gallery) in the form. NEVER ask the user to type or say them and NEVER fill or change them
              yourself, even if the user says them. If any of them is missing, politely ask the user to tap
              "Scan CNIC" in the form and scan both sides of the original card with clear pictures.
            - phone_number: 11 digits starting with 03 (e.g. 03001234567).
            - account_number: this is the IBAN number: EXACTLY 24 characters, capital letters and digits, no spaces:
              "PK" + 2 digits + 4-letter bank code + 16 digits (e.g. PK36SCBL0000001123456702). Spoken letters
              ("P K", "pee kay") are letters. If it is not 24 characters in this format, do NOT save it: read back
              what you heard and ask for the 24-character IBAN again.
              Never ask for a branch code.
            - loan_duration: Strictly "6", "12", or "18" (months).
            - source_of_income: Strictly "Business" or "Salary".
            - occupation_type: Strictly "Agriculture" or "Non-Agriculture".
            - nature_of_business: Strictly "New" or "Existing".
            - specific_occupation: one of "Work on Family Farm", "Work on Other Farm", "Share Cropping",
              "Keep Livestock", "Fishing" (Agriculture) or "Food Processing", "Trading", "Handicraft",
              "Garments", "Mechanic", "Carpenter", "Transport", "Wage Labour", "Office Work", "Sewing",
              "Govt Job", "Private Job", "House Wife" (Non-Agriculture).

            === INFER WHAT THE USER ALREADY TOLD YOU (do not ask again) ===
            - Dairy, cattle, buffalo, goats, poultry, crops, seeds, fertiliser, tractor, farming
              -> occupation_type "Agriculture" (livestock work -> specific_occupation "Keep Livestock").
            - Shop, trading, buying and selling, any own business -> source_of_income "Business".
              Job, salary, naukri, employee -> source_of_income "Salary".
            - "Business for N years / N saal se" -> business_duration_years N and nature_of_business "Existing".
              Starting a new business -> nature_of_business "New".
            - "I live in X / X se hoon" -> district (closest match) and, if given, present_address.

            === TODAY, CNIC VALIDITY AND WHAT IS STILL MISSING ===
            - Today's date: ${java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.ENGLISH).format(java.util.Date())}.
            - If cnic_expiry is known, check it: the CNIC must be valid for at least 6 months (6-month financing)
              or 12 months (12 or 18 months). If it is expired or too close to expiry, tell the user politely and
              clearly that they must renew the CNIC first (or choose a shorter duration if 6 months would fit).
            - Required for submission (ask the user only for these): phone_number, marital_status, district, tehsil,
              bank_name, account_number (24-character IBAN, PK...), purpose_of_loan, source_of_income, loan_amount,
              loan_duration, nature_of_business, business_duration_years, business_address, business_city,
              occupation_type. The profile photo and the CNIC scan (which fills the CNIC details) are done in the
              form itself, not in chat.
            - Never say the application is complete while any required field above is empty: instead ask for the
              next missing one (one or two at a time). When everything is filled, ask the user to review the form
              and attach the photos, then press Submit.

            === PRESERVING EXISTING FIELDS ===
            - Keep all fields already present in 'Currently Extracted Fields' unless the user requested to change or overwrite them.
            - Set "is_complete" to true ONLY when loan_amount, loan_duration, and purpose_of_loan are all filled AND the user explicitly confirmed submission (e.g. 'theek hai', 'confirm', 'apply').

            === PAST LOAN HISTORY ===
            $cachedLoanHistory

            YOUR OUTPUT MUST ALWAYS BE STRICTLY VALID JSON AND NOTHING ELSE:
            {
                "ai_reply": "Your reply in the user's language (spoken-style and short in voice mode)",
                "is_complete": false,
                "extracted_fields": {
                    "applicant_name": "",
                    "applicant_cnic": "",
                    "cnic_expiry": "",
                    "cnic_issue": "",
                    "date_of_birth": "",
                    "phone_number": "",
                    "gender": "",
                    "marital_status": "",
                    "present_address": "",
                    "permanent_address": "",
                    "district": "",
                    "tehsil": "",
                    "bank_name": "",
                    "account_number": "",
                    "branch_code": "",
                    "branch_address": "",
                    "purpose_of_loan": "",
                    "source_of_income": "",
                    "loan_amount": "",
                    "loan_duration": "",
                    "business_duration_years": "",
                    "business_address": "",
                    "business_city": "",
                    "occupation_type": "",
                    "specific_occupation": "",
                    "nature_of_business": ""
                }
            }
        """.trimIndent()
    }

    /**
     * Merges extracted fields from AI with existing extracted fields and normalizes values.
     */
    private fun mergeExtractedFields(current: JSONObject, newExtracted: JSONObject?): JSONObject {
        val merged = JSONObject(current.toString())
        if (newExtracted == null) return merged

        val keys = newExtracted.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val raw = newExtracted.opt(key)
            if (raw != null && raw != JSONObject.NULL) {
                val strVal = raw.toString().trim()
                if (strVal.isNotEmpty() && !strVal.equals("null", ignoreCase = true)) {
                    merged.put(key, strVal)
                }
            }
        }

        // Only a complete CNIC (13 digits) / mobile number (11 digits) may reach the form
        val cnicDigits = merged.optString("applicant_cnic", "").filter { it.isDigit() }
        if (cnicDigits.length == 13) {
            merged.put("applicant_cnic", "${cnicDigits.substring(0, 5)}-${cnicDigits.substring(5, 12)}-${cnicDigits.substring(12)}")
        } else if (merged.has("applicant_cnic")) {
            val previous = current.optString("applicant_cnic", "")
            if (previous.isNotEmpty()) merged.put("applicant_cnic", previous) else merged.remove("applicant_cnic")
        }
        // IBAN (account_number) must be a 24-character Pakistani IBAN: PK + 2 digits + 4 letters + 16 digits
        val iban = merged.optString("account_number", "").filter { it.isLetterOrDigit() }.uppercase()
        if (Regex("PK\\d{2}[A-Z]{4}\\d{16}").matches(iban)) {
            merged.put("account_number", iban)
        } else if (merged.has("account_number")) {
            val previous = current.optString("account_number", "")
            if (previous.isNotEmpty()) merged.put("account_number", previous) else merged.remove("account_number")
        }
        merged.remove("branch_code")
        val phoneDigits = merged.optString("phone_number", "").filter { it.isDigit() }
        if (phoneDigits.isNotEmpty() && !(phoneDigits.length == 11 && phoneDigits.startsWith("03"))) {
            val previous = current.optString("phone_number", "")
            if (previous.isNotEmpty()) merged.put("phone_number", previous) else merged.remove("phone_number")
        }

        // Post-normalization for Bank Name
        val currentBank = merged.optString("bank_name", "")
        if (currentBank.isNotEmpty()) {
            val matchedBank = findMatchingBank(currentBank)
            if (matchedBank != null) {
                merged.put("bank_name", matchedBank)
            }
        }

        // Post-normalization for Purpose
        val currentPurpose = merged.optString("purpose_of_loan", "")
        if (currentPurpose.isNotEmpty()) {
            val matchedPurpose = findMatchingPurpose(currentPurpose)
            if (matchedPurpose != null) {
                merged.put("purpose_of_loan", matchedPurpose)
            }
        }

        // Post-normalization for Duration
        val currentDuration = merged.optString("loan_duration", "")
        if (currentDuration.isNotEmpty()) {
            val norm = when {
                currentDuration.contains("6") -> "6"
                currentDuration.contains("18") -> "18"
                currentDuration.contains("12") || currentDuration.contains("1") -> "12"
                else -> currentDuration
            }
            merged.put("loan_duration", norm)
        }

        return merged
    }

    /**
     * Local conversational engine (Tier 3 fallback).
     * Handles changes ("Bank change kro meezan"), FAQs, and auto-fills without crashing or repeating.
     */
    private fun processLocally(userInput: String, currentlyExtracted: JSONObject): JSONObject {
        val (extracted, isComplete, updatedFields) = extractFieldsLocally(userInput, currentlyExtracted)
        val aiReply = generateAssistantReply(userInput, extracted, isComplete, updatedFields)

        val result = JSONObject()
        result.put("ai_reply", aiReply)
        result.put("is_complete", isComplete)
        result.put("extracted_fields", extracted)
        return result
    }

    private fun extractFieldsLocally(
        userInput: String,
        currentlyExtracted: JSONObject
    ): Triple<JSONObject, Boolean, List<String>> {
        val extracted = JSONObject(currentlyExtracted.toString())
        val updatedFields = mutableListOf<String>()
        val lower = userInput.lowercase(Locale.ROOT).trim()

        val idPatterns = Regex("(?<!\\d)\\d{5}[\\s\\-]?\\d{7}[\\s\\-]?\\d(?!\\d)|(?<!\\d)03\\d{2}[\\s\\-]?\\d{7}(?!\\d)")
        val lowerNoIds = idPatterns.replace(lower, " ")
        val inputNoIds = idPatterns.replace(userInput, " ")

        // 1. Bank Extraction / Update (ALWAYS check for bank changes or bank mentions)
        val matchedBank = findMatchingBank(userInput)
        if (matchedBank != null) {
            val oldBank = extracted.optString("bank_name", "")
            if (!oldBank.equals(matchedBank, ignoreCase = true)) {
                extracted.put("bank_name", matchedBank)
                updatedFields.add("bank")
            }
        }

        // 2. Amount Extraction / Update
        val extractedAmount = extractAmountString(lowerNoIds)
        if (extractedAmount != null) {
            val oldAmount = extracted.optString("loan_amount", "")
            if (oldAmount != extractedAmount) {
                extracted.put("loan_amount", extractedAmount)
                updatedFields.add("amount")
            }
        }

        // 3. Duration Extraction / Update
        val extractedDuration = extractDurationString(lower)
        if (extractedDuration != null) {
            val oldDuration = extracted.optString("loan_duration", "")
            if (oldDuration != extractedDuration) {
                extracted.put("loan_duration", extractedDuration)
                updatedFields.add("duration")
            }
        }

        // 4. Purpose Extraction / Update
        val extractedPurpose = findMatchingPurpose(lower)
        if (extractedPurpose != null) {
            val oldPurpose = extracted.optString("purpose_of_loan", "")
            if (!oldPurpose.equals(extractedPurpose, ignoreCase = true)) {
                extracted.put("purpose_of_loan", extractedPurpose)
                updatedFields.add("purpose")
                if (!extracted.has("source_of_income") || extracted.optString("source_of_income").isEmpty()) {
                    extracted.put("source_of_income", "Business")
                }
                if (extractedPurpose.contains("Agriculture", ignoreCase = true) || extractedPurpose.contains("Livestock", ignoreCase = true)) {
                    extracted.put("occupation_type", "Agriculture")
                } else {
                    extracted.put("occupation_type", "Non-Agriculture")
                }
            }
        }

        // 5. Source of Income & Occupation
        if (lower.contains("job") || lower.contains("salary") || lower.contains("naukri") || lower.contains("mulazmat") || lower.contains("private")) {
            extracted.put("source_of_income", "Salary")
            extracted.put("occupation_type", "Non-Agriculture")
            extracted.put("specific_occupation", "Private Job")
            updatedFields.add("income")
        } else if (lower.contains("karobar") || lower.contains("business") || lower.contains("apna kaam") || lower.contains("dukan") || lower.contains("shop")) {
            extracted.put("source_of_income", "Business")
            extracted.put("occupation_type", "Non-Agriculture")
            extracted.put("specific_occupation", "Trading / Buy and Sell")
            updatedFields.add("income")
        }

        // 6. District Matching
        for (dist in validDistrictsList) {
            val dLower = dist.lowercase(Locale.ROOT)
            if (dLower.startsWith("select", ignoreCase = true)) continue
            if (lower.contains(dLower)) {
                extracted.put("district", dist)
                updatedFields.add("district")
                break
            }
        }

        // 7. Tehsil Matching
        for (teh in validTehsilsList) {
            val tLower = teh.lowercase(Locale.ROOT)
            if (tLower.startsWith("select", ignoreCase = true)) continue
            if (lower.contains(tLower)) {
                extracted.put("tehsil", teh)
                updatedFields.add("tehsil")
                break
            }
        }

        // 8. CNIC (13 digits)
        val cnicRegex = Regex("(?<!\\d)(\\d{5}[\\s\\-]?\\d{7}[\\s\\-]?\\d)(?!\\d)")
        val cnicMatch = cnicRegex.find(userInput)
        if (cnicMatch != null) {
            val digits = cnicMatch.value.filter { it.isDigit() }
            if (digits.length == 13) {
                extracted.put("applicant_cnic", "${digits.substring(0, 5)}-${digits.substring(5, 12)}-${digits.substring(12)}")
                updatedFields.add("cnic")
            }
        }

        // 9. Phone
        val phoneRegex = Regex("(?<!\\d)(03\\d{2}[\\s\\-]?\\d{7})(?!\\d)")
        val phoneMatch = phoneRegex.find(userInput)
        if (phoneMatch != null) {
            val digits = phoneMatch.value.filter { it.isDigit() }
            if (digits.length == 11) {
                extracted.put("phone_number", digits)
                updatedFields.add("phone")
            }
        }

        // 10. Account Number & Branch Code
        val mentionsAccount = lower.contains("account") || lower.contains("acc") || lower.contains("khata")
        if (mentionsAccount || !extracted.has("account_number") || extracted.optString("account_number").isEmpty()) {
            // 24-character Pakistani IBAN, also when typed with spaces: "PK36 SCBL 0000 0011 2345 6702"
            val accRegex = Regex("(?i)(?<![A-Z0-9])PK\\s*\\d\\s*\\d\\s*(?:[A-Z]\\s*){4}(?:\\d\\s*){15}\\d(?![A-Z0-9])")
            val accMatch = accRegex.find(inputNoIds)
            if (accMatch != null) {
                val candidate = accMatch.value.filter { it.isLetterOrDigit() }.uppercase()
                val currentCnic = extracted.optString("applicant_cnic", "").replace("-", "")
                if (candidate != currentCnic) {
                    extracted.put("account_number", candidate)
                    updatedFields.add("account_number")
                }
            }
        }

        // Branch code is no longer collected (IBAN identifies the account)

        // Completion criteria
        val isExplicitConfirm = lower.contains("theek hai") || lower.contains("theek h") || lower.contains("confirm") ||
                lower.contains("done") || lower.contains("apply") || lower.contains("haan") ||
                lower.contains("yes") || lower.contains("mukammal") || lower.contains("submit") ||
                lower.contains("sahi hai") || lower.contains("ok")

        val hasAmount = extracted.has("loan_amount") && extracted.optString("loan_amount").isNotEmpty()
        val hasDuration = extracted.has("loan_duration") && extracted.optString("loan_duration").isNotEmpty()
        val hasPurpose = extracted.has("purpose_of_loan") && extracted.optString("purpose_of_loan").isNotEmpty()

        if (hasAmount && hasPurpose && !hasDuration && isExplicitConfirm) {
            extracted.put("loan_duration", "12")
        }

        val isComplete = (hasAmount && hasPurpose && (extracted.has("loan_duration") && extracted.optString("loan_duration").isNotEmpty())) && isExplicitConfirm

        return Triple(extracted, isComplete, updatedFields)
    }

    private fun extractAmountString(lowerNoIds: String): String? {
        val amountRegexLac = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:lakh|lac|laakh)", RegexOption.IGNORE_CASE)
        val amountRegexHazar = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:hazar|hzaar|k\\b|thousand)", RegexOption.IGNORE_CASE)
        val amountRegexPure = Regex("(?:rs\\.?|rupay|pkr|raqam|amount)?\\s*(?<![\\d,])(\\d{1,3}(?:,\\d{3})+|\\d{4,8})(?![\\d,])", RegexOption.IGNORE_CASE)

        val matchLac = amountRegexLac.find(lowerNoIds)
        val matchHazar = amountRegexHazar.find(lowerNoIds)
        val mentionsBankNumbers = Regex("account|acc\\b|branch|code|iban").containsMatchIn(lowerNoIds)
        val matchPure = if (mentionsBankNumbers) null else amountRegexPure.find(lowerNoIds)

        if (matchLac != null) {
            val num = matchLac.groupValues[1].toDoubleOrNull() ?: 1.0
            return (num * 100000).toLong().toString()
        }
        if (matchHazar != null) {
            val num = matchHazar.groupValues[1].toDoubleOrNull() ?: 1.0
            return (num * 1000).toLong().toString()
        }
        if (matchPure != null) {
            val cleanDigits = matchPure.groupValues[1].replace(",", "")
            val num = cleanDigits.toLongOrNull()
            if (num != null && num >= 5000) {
                return num.toString()
            }
        }
        if (lowerNoIds.contains("ek lakh") || lowerNoIds.contains("1 lakh")) return "100000"
        if (lowerNoIds.contains("dedh lakh") || lowerNoIds.contains("1.5 lakh")) return "150000"
        if (lowerNoIds.contains("do lakh") || lowerNoIds.contains("2 lakh")) return "200000"
        if (lowerNoIds.contains("teen lakh") || lowerNoIds.contains("3 lakh")) return "300000"
        if (lowerNoIds.contains("panch lakh") || lowerNoIds.contains("5 lakh")) return "500000"
        if (lowerNoIds.contains("pachas hazar") || lowerNoIds.contains("50 hazar")) return "50000"
        if (lowerNoIds.contains("sath hazar") || lowerNoIds.contains("60 hazar")) return "60000"
        if (lowerNoIds.contains("sattar hazar") || lowerNoIds.contains("70 hazar")) return "70000"
        if (lowerNoIds.contains("assi hazar") || lowerNoIds.contains("80 hazar")) return "80000"
        if (lowerNoIds.contains("nabbey hazar") || lowerNoIds.contains("90 hazar")) return "90000"

        return null
    }

    private fun extractDurationString(lower: String): String? {
        val durationRegexMonth = Regex("(\\d{1,2})\\s*(?:mahine|months?|month|maheene|mah|m\\b)", RegexOption.IGNORE_CASE)
        val durationRegexYear = Regex("(\\d{1,2})\\s*(?:saal|years?|year|yr)", RegexOption.IGNORE_CASE)

        val matchMonth = durationRegexMonth.find(lower)
        val matchYear = durationRegexYear.find(lower)

        if (matchMonth != null) {
            val months = matchMonth.groupValues[1].toIntOrNull() ?: 12
            return when {
                months <= 8 -> "6"
                months <= 14 -> "12"
                else -> "18"
            }
        }
        if (matchYear != null) {
            val years = matchYear.groupValues[1].toIntOrNull() ?: 1
            return if (years <= 1) "12" else "18"
        }
        if (lower.contains("ek saal") || lower.contains("1 saal") || lower.contains("1 year")) return "12"
        if (lower.contains("do saal") || lower.contains("2 saal") || lower.contains("2 year") || lower.contains("dedh saal")) return "18"
        if (lower.contains("che mahine") || lower.contains("6 mahine") || lower.contains("aadha saal") || lower.contains("half year")) return "6"
        if (lower.matches(Regex("^(6|12|18)$"))) return lower
        return null
    }

    private fun generateAssistantReply(
        userInput: String,
        extracted: JSONObject,
        isComplete: Boolean,
        updatedFields: List<String>
    ): String {
        val lower = userInput.lowercase(Locale.ROOT).trim()

        if (isComplete) {
            val amt = extracted.optString("loan_amount")
            val dur = extracted.optString("loan_duration", "12")
            val pur = extracted.optString("purpose_of_loan")
            val bank = extracted.optString("bank_name", "Selected Bank")
            return "Zabardast! Aap ki darj karda maloomat mukammal ho chuki hain:\n\n" +
                    "• Raqam: Rs. $amt\n" +
                    "• Muddat: $dur Mahine\n" +
                    "• Maqsad: $pur\n" +
                    "• Bank: $bank\n\n" +
                    "Application form finalize ho chuka hai. Shukriya!"
        }

        // FAQs & Guidance
        if (lower.contains("sood") || lower.contains("interest") || lower.contains("markup") || lower.contains("riba")) {
            return "Asaan Finance shariah-compliant financing faraham karta hai, jis mein kisi qisam ka sood (interest) ya markup shamil nahin hai. Yeh aasan aqsaat par mabni hai."
        }

        if (lower.contains("documents") || lower.contains("kya chahiye") || lower.contains("requirements") || lower.contains("kaghzat")) {
            return "Financing darkhwast ke liye aap ka CNIC, active mobile number, aur verified bank account number darkar hota hai."
        }

        if (lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("kon ho") || lower.contains("aap kon")) {
            return "Main aap ka Asaan Financing Assistant hoon! Main application bharne aur details update karne mein aap ki madad karta hoon. Batayein main aap ki kya madad karoon?"
        }

        if (lower == "thanks" || lower == "thank you" || lower.contains("shukriya") || lower.contains("jazakallah")) {
            return "Khush aamdeed! Main hamesha aap ki madad ke liye hazir hoon. Agar koi aur detail tabdeel ya check karni ho to batayein."
        }

        val amt = extracted.optString("loan_amount", "")
        val pur = extracted.optString("purpose_of_loan", "")
        val dur = extracted.optString("loan_duration", "")
        val bank = extracted.optString("bank_name", "")

        val isIslamicGreeting = lower == "salam" || lower == "assalam" || lower.contains("assalam") ||
                lower.contains("asalam") || lower.contains("slam") || lower == "aoa"
        val isCasualGreeting = lower == "hi" || lower == "hello" || lower == "hey" ||
                lower.startsWith("hi ") || lower.startsWith("hello ") || lower.startsWith("hey ")

        if (amt.isEmpty() && pur.isEmpty() && updatedFields.isEmpty()) {
            if (isCasualGreeting) {
                return "Hello! Main aap ka Asaan Financing Assistant hoon. Aap ko kitni financing raqam ki zaroorat hai aur kis maqsad ke liye chahiye?"
            }
            if (isIslamicGreeting) {
                return "Walaikum Assalam! Main aap ka Asaan Financing Assistant hoon. Batayein ke aap ko kitni financing raqam ki zaroorat hai aur kis maqsad ke liye?"
            }
        }

        // Dynamic acknowledgment prefix based on what was updated
        val updatePrefix = StringBuilder()
        if (updatedFields.contains("bank")) {
            updatePrefix.append("Ji theek hai! Bank '$bank' mein tabdeel kar diya gaya hai. ")
        }
        if (updatedFields.contains("amount")) {
            updatePrefix.append("Financing amount Rs. $amt darj ho gayi hai. ")
        }
        if (updatedFields.contains("duration")) {
            updatePrefix.append("Muddat $dur mahine darj ho gayi hai. ")
        }
        if (updatedFields.contains("purpose")) {
            updatePrefix.append("Financing ka maqsad '$pur' darj kar liya gaya hai. ")
        }
        if (updatedFields.contains("district")) {
            updatePrefix.append("District darj ho gaya hai. ")
        }

        val nextPrompt = when {
            amt.isEmpty() -> "Batayein ke aap ko kitni raqam (financing amount) ki zaroorat hai? (Maslan: 50,000 ya 1 Lakh)"
            pur.isEmpty() -> "Yeh financing aap kis maqsad (purpose) ke liye lena chahte hain? (Maslan: Karobar, Kheti Baari, ya Maweshi)"
            dur.isEmpty() -> "Yeh raqam aap kitne mahino (duration) mein ada karna chahenge? (6, 12, ya 18 Mahine)"
            bank.isEmpty() -> "Aap kis bank mein account rakhte hain ya financing lena chahenge? (Maslan: Meezan Bank, HBL, UBL waghera)"
            else -> "Aap ki darj karda maloomat note ho chuki hain. Agar koi mazeed tabdeeli karni hai to batayein, ya form finalize karne ke liye 'Confirm' likhein."
        }

        return if (updatePrefix.isNotEmpty()) {
            updatePrefix.toString() + nextPrompt
        } else {
            val greetingPrefix = if (isIslamicGreeting) "Walaikum Assalam! " else if (isCasualGreeting) "Hello! " else ""
            greetingPrefix + nextPrompt
        }
    }

    fun findMatchingBank(input: String): String? {
        if (input.isBlank() || validBanksList.isEmpty()) return null
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Direct equality / contains with items in validBanksList
        for (b in validBanksList) {
            val bLower = b.lowercase(Locale.ROOT).trim()
            if (bLower.isBlank() || bLower.startsWith("select", ignoreCase = true)) continue
            if (lower == bLower || lower.contains(bLower) || bLower.contains(lower)) {
                return b
            }
        }

        // 2. Acronyms & Pakistani Bank Aliases
        val bankKeywords = mapOf(
            "meezan" to listOf("meezan", "mizan"),
            "albarka" to listOf("albarka", "albaraka", "al baraka", "al-baraka"),
            "habib" to listOf("hbl", "habib bank", "habib metropolitan", "habib"),
            "united" to listOf("ubl", "united bank"),
            "mcb" to listOf("mcb", "muslim commercial"),
            "allied" to listOf("allied", "abl"),
            "alfalah" to listOf("alfalah", "al falah", "al-falah"),
            "faysal" to listOf("faysal", "faisal"),
            "askari" to listOf("askari"),
            "national" to listOf("national bank", "nbp"),
            "punjab" to listOf("bank of punjab", "punjab", "bop"),
            "bankislami" to listOf("bankislami", "bank islami"),
            "dubai" to listOf("dubai islamic", "dib"),
            "standard" to listOf("standard chartered", "scb"),
            "js" to listOf("js bank", "js"),
            "soneri" to listOf("soneri"),
            "sindh" to listOf("sindh bank", "sindh"),
            "khushhali" to listOf("khushhali"),
            "nrsp" to listOf("nrsp"),
            "ztbl" to listOf("ztbl", "zarai taraqiati", "zarai")
        )

        for ((key, aliases) in bankKeywords) {
            if (aliases.any { lower.contains(it) }) {
                for (b in validBanksList) {
                    val bLower = b.lowercase(Locale.ROOT)
                    if (bLower.startsWith("select", ignoreCase = true)) continue
                    if (bLower.contains(key) || aliases.any { bLower.contains(it) }) {
                        return b
                    }
                }
            }
        }
        return null
    }

    fun findMatchingPurpose(input: String): String? {
        if (input.isBlank() || validPurposesList.isEmpty()) return null
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Direct contains / equals
        for (p in validPurposesList) {
            val pLower = p.lowercase(Locale.ROOT).trim()
            if (pLower.isBlank() || pLower.startsWith("select", ignoreCase = true)) continue
            if (lower == pLower || lower.contains(pLower) || pLower.contains(lower)) {
                return p
            }
        }

        // 2. Semantic matching
        if (lower.contains("karobar") || lower.contains("business") || lower.contains("dukan") ||
            lower.contains("shop") || lower.contains("trading") || lower.contains("tijarat") ||
            lower.contains("enterprise") || lower.contains("commercial")) {
            return validPurposesList.firstOrNull { it.contains("Enterprise", ignoreCase = true) || it.contains("Tajarat", ignoreCase = true) }
                ?: "Enterprise"
        }

        if (lower.contains("kheti") || lower.contains("zaraat") || lower.contains("agri") ||
            lower.contains("fasal") || lower.contains("crop") || lower.contains("beej") ||
            lower.contains("khad") || lower.contains("kisan") || lower.contains("kashtkari")) {
            return validPurposesList.firstOrNull { it.contains("Agriculture", ignoreCase = true) || it.contains("Agri", ignoreCase = true) }
                ?: "Agriculture Inputs"
        }

        if (lower.contains("maweshi") || lower.contains("livestock") || lower.contains("bakri") ||
            lower.contains("bhens") || lower.contains("cattle") || lower.contains("dairy") ||
            lower.contains("janwar") || lower.contains("gaye")) {
            return validPurposesList.firstOrNull { it.contains("Livestock", ignoreCase = true) }
                ?: "Livestock"
        }

        if (lower.contains("ghar") || lower.contains("house") || lower.contains("makan") ||
            lower.contains("home") || lower.contains("tameer")) {
            return validPurposesList.firstOrNull { it.contains("House", ignoreCase = true) }
                ?: "House improvement"
        }

        if (lower.contains("solar") || lower.contains("tube well")) {
            return validPurposesList.firstOrNull { it.contains("Solar", ignoreCase = true) }
                ?: "Solar Tube wells"
        }

        return null
    }
}
