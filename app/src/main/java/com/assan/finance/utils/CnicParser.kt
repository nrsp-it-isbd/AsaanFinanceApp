package com.assan.finance.utils

import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern

object CnicParser {

    fun parseCnicText(visionText: Text): CnicData {
        val cnicData = CnicData()
        val blocks = visionText.textBlocks
        val lines = mutableListOf<String>()

        for (block in blocks) {
            for (line in block.lines) {
                lines.add(line.text.trim())
            }
        }

        // Patterns
        val cnicPattern = Pattern.compile("\\b\\d{5}-\\d{7}-\\d{1}\\b")
        val datePattern = Pattern.compile("\\b\\d{2}[./-]\\d{2}[./-]\\d{4}\\b")

        // Noise words to ignore for English names
        val noiseWords = listOf("pakistan", "national identity card", "islamic republic", "identity", "card", "signature", "holder", "gender", "country", "stay", "authority")

        var nextIsName = false
        var nextIsFatherName = false

        for (i in lines.indices) {
            val line = lines[i]
            val lowerLine = line.lowercase()

            // Filter out common header noise
            var isNoise = false
            for (noise in noiseWords) {
                if (lowerLine.contains(noise) && line.length < 30) {
                    isNoise = true
                    break
                }
            }
            if (isNoise) continue

            // 1. Extract CNIC Number
            val cnicMatcher = cnicPattern.matcher(line)
            if (cnicMatcher.find() && cnicData.cnicNumber.isEmpty()) {
                cnicData.cnicNumber = cnicMatcher.group()
                continue
            }

            // 2. Extract Expiry Date
            val dateMatcher = datePattern.matcher(line)
            if (dateMatcher.find()) {
                // Usually 3 dates: DOB, DOI, DOE
                if (i > 0 && lines[i - 1].lowercase().contains("expiry")) {
                    cnicData.expiryDate = dateMatcher.group()
                } else {
                    cnicData.expiryDate = dateMatcher.group() // Keep updating, last date is usually expiry
                }
                continue
            }

            // 3. Name (English)
            if (nextIsName) {
                // Ensure only English alphabets and valid length
                if (!lowerLine.contains("name") && line.matches(Regex("^[a-zA-Z\\s\\.]+$")) && line.length > 2) {
                    cnicData.nameEn = line
                    nextIsName = false
                }
                continue
            }
            if (lowerLine == "name" || lowerLine == "name:" || lowerLine.startsWith("name ")) {
                nextIsName = true
                val split = lowerLine.split("name")
                if (split.size > 1 && split.last().trim().length > 2) {
                    // Extract from same line if it's like "Name Asif Nawaz"
                    cnicData.nameEn = line.substring(line.lowercase().indexOf("name") + 4).replace(":", "").trim()
                    nextIsName = false
                }
                continue
            }

            // 4. Father Name (English)
            if (nextIsFatherName) {
                if (!lowerLine.contains("name") && line.matches(Regex("^[a-zA-Z\\s\\.]+$")) && line.length > 2) {
                    cnicData.fatherNameEn = line
                    nextIsFatherName = false
                }
                continue
            }
            if (lowerLine == "father name" || lowerLine == "husband name" || lowerLine.startsWith("father name") || lowerLine.startsWith("husband name")) {
                nextIsFatherName = true
                continue
            }
        }

        return cnicData
    }
}

