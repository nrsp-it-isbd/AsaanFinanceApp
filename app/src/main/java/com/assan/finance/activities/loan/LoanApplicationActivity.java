package com.assan.finance.activities.loan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.assan.finance.R;
import com.assan.finance.apis.ApiService;
import com.assan.finance.apis.RetrofitClient;
import com.assan.finance.base.BaseActivity;
import com.assan.finance.dataModels.DataModelLoanData;
import com.assan.finance.model.assan.AssanBank;
import com.assan.finance.model.assan.AssanDistrict;
import com.assan.finance.model.assan.AssanTehsil;
import com.assan.finance.model.assan.ResponseModelAssanBanks;
import com.assan.finance.model.assan.ResponseModelAssanDistricts;
import com.assan.finance.model.assan.ResponseModelAssanTehsils;
import com.assan.finance.network.assan.AssanRetrofitClient;
import com.assan.finance.responseModel.ResponseModelLoanApplication;
import com.assan.finance.responseModel.ResponseModelProfileUpdate;
import com.assan.finance.sharedPreferences.SharedManager;
import com.assan.finance.sqlitedb.SqliteDatabase;
import com.assan.finance.utils.CommonFunctions;
import com.assan.finance.utils.Constants;
import com.assan.finance.utils.CnicRotationHelper;
import com.assan.finance.utils.SearchableSpinner;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@android.annotation.SuppressLint("InflateParams")
public class LoanApplicationActivity extends BaseActivity {

    static final int CAMERA_REQUEST_CODE = 100;
    static final int BACK_CAMERA_REQUEST_CODE = 2;
    private Uri photoUri;
    private File photoFile;
    private boolean isFrontScanned = false;
    private boolean isBackScanned = false;
    private String tempFrontPath = "";
    private String tempBackPath = "";

    // Voice Assistant Variables
    private String voiceApplicantName = "";
    private String voiceApplicantCnic = "";
    private String voiceCnicExpiry = "";
    private String voiceCnicIssue = "";
    private String voiceDob = "";
    private String voicePhone = "";
    private String voiceGender = "";
    private String voiceMaritalStatus = "";
    private String voicePresentAddress = "";
    private String voicePermanentAddress = "";
    private String voiceDistrict = "";
    private String voiceTehsil = "";
    private String voiceBankName = "";
    private String voiceAccountNo = "";
    private String voiceBranchCode = "";
    private String voiceBranchAddress = "";
    private String voicePurposeOfLoan = "";
    private String voiceSourceOfIncome = "";
    private String voiceAmount = "";
    private String voiceDuration = "";
    private String voiceBusinessDuration = "";
    private String voiceBusinessAddress = "";
    private String voiceBusinessCity = "";
    private String voiceOccupationType = "";
    private String voiceSpecificOccupation = "";
    private SpeechRecognizer speechRecognizer;
    private android.speech.tts.TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private Runnable onTtsDoneRunnable;
    // Form assistant voice: natural Gemini voice (phone TTS fallback) + live voice-mode flag
    private com.assan.finance.ai.VoiceReplyPlayer assistantVoicePlayer;
    private com.assan.finance.ai.ListeningSoundMuter assistantBeepMuter;
    private boolean[] assistantVoiceModeRef;
    private String voiceNatureOfBusiness = ""; // "New" / "Existing" from the form assistant
    private final List<com.google.gson.JsonObject> voiceConversationHistory = new ArrayList<>();
    private com.assan.finance.utils.LoanAssistantAiHelper aiHelper;
    // Latest assistant answer (typed or spoken), for the "Report answer" button
    private String lastAssistantReply = "";
    private boolean isAutoFilling = false;
    // FIX: Spinner.setSelection() dispatches onItemSelected asynchronously (after isAutoFilling
    // is already reset), which wiped the auto-filled account number / branch code. This flag
    // tells the bank listener to skip exactly one reset caused by the assistant's auto-fill.
    private boolean skipNextBankReset = false;

    final List<String> districtNames = new ArrayList<>();
    final List<String> cityNames = new ArrayList<>();
    final List<String> districtIDs = new ArrayList<>();
    final List<String> tehsilNames = new ArrayList<>();
    final List<String> tehsilIDs = new ArrayList<>();
    final List<String> fieldUnitIDs = new ArrayList<>();
    final List<String> regionIDs = new ArrayList<>();
    int isMarried = 0;
    String currentImagePath;
    String currentImageName;
    String currentImagePath1;
    String currentBackImageName;

    private String cnicFrontBase64 = "";
    private Uri cnicFrontUri = null;
    private Uri cnicBackUri = null;
    private String cnicBackBase64 = "";
    private ImageView ivCnicFront, ivCnicBack;
    private androidx.cardview.widget.CardView cardCnicFront, cardCnicBack;
    private ImageView ivViewCnicFront, ivViewCnicBack;
    private ImageView ivRotateCnicFront, ivRotateCnicBack;
    private boolean isPickingCnicFront = true;
    private int pickingImageType = 0; // 0 = cnicFront, 1 = cnicBack, 2 = profile Picture
    private String frontOcrDate = "";
    private String backOcrDate = "";
    public final int REQUEST_IMAGE_CODE_CAMERA = 101;
    public final int REQUEST_IMAGE_CODE_GALLERY = 102;
    public final int REQUEST_IMAGE_CODE_CAMERA_PROFILE = 103;
    public final int REQUEST_IMAGE_CODE_GALLERY_PROFILE = 104;
    private String profilePictureBase64 = "";
    private de.hdodenhof.circleimageview.CircleImageView ivProfilePicture;

    SearchableSpinner spinnerPurposeOfLoan;
    SearchableSpinner spinnerTehsilName;
    SearchableSpinner spinnerDistrictName;
    SearchableSpinner spinnerBusinessCityName;
    RadioGroup rgOccupationType;
    RadioGroup rgAgricultureOccupation;
    RadioGroup rgNonAgricultureOccupation;
    LinearLayout llAgricultureOptions;
    LinearLayout llNonAgricultureOptions;
    Spinner spinnerDurationOfLoan;
    String businessCityName;
    String pendingTehsilSelection = null;
    SearchableSpinner spinnerBankName;
    private com.google.android.material.button.MaterialButton submitLoanApplicationButton;
    RadioButton radioButtonSingle;
    RadioButton radioButtonMarried;
    EditText etPresentAddress;
    EditText etBankAccNumber;
    EditText etBankBranch;
    EditText etBankBranchCode;
    TextView tvBankAccNumberCounter, tvBankBranchCodeCounter;
    int currentBankAccNumberLimit = 14;
    int currentBankBranchCodeLimit = 6;
    EditText etLoanAmount;
    RadioGroup radioGroupSourceOfIncome;
    LinearLayout llSourceOfIncome;
    EditText etBusinessAddress;
    EditText etHowOldBusiness;
    EditText etPermanentAddress;
    TextView txtClientIdLoan;
    EditText etApplicantName;
    EditText etApplicantCnic;
    EditText etApplicantCnicExpiry;
    EditText etApplicantCnicIssue;
    EditText etApplicantDOB;
    EditText etApplicantPhone;
    RadioGroup radioGroupGender;
    TextView heading;
    String LoanPurpose = "";
    SqliteDatabase mSqliteDatabase;
    View btnBack;
    RadioGroup radioGroupMaritalStatus;
    RadioGroup radioGroupNatureOfBusiness;
    SharedManager sharedManager;
    List<AssanDistrict> districts;
    View parentLayout;
    String applicantName = "";
    String applicantCnic = "";
    String applicantDOB = "";
    String applicantPhone = "";
    String applicantGender = "";
    String applicantCnicExpiry = "";
    NestedScrollView SVParent;
    AppCompatCheckBox termsAndConditionsCheckbox;
    String selectedOccupation = "";

    String LoanFor = "";
    String NatureOfBusiness = "";
    String SourceOfIncome = "";

    TextView spinner_error_purpose_of_loan;
    List<String> productNames = new ArrayList<>();
    List<String> minLoanAmountList = new ArrayList<>();
    List<String> maxLoanAmountList = new ArrayList<>();
    List<String> bankNames = new ArrayList<>();
    TextView txtloanLimit;

    AppCompatRadioButton radioButtonNatureOfBusinessNew, radioButtonNatureOfBusinessExisting;
    List<String> accountNoFormats = new ArrayList<>();
    List<String> branchCodes = new ArrayList<>();

    // CNIC Scanner Views
    ImageView btnScanCnicUnified;
    TextView tvUnifiedScanStatus;

    public static void saveCompressedImageToScopedStorage(File file) {
        try {

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 3;

            FileInputStream inputStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();
            final int REQUIRED_SIZE = 100;
            int scale = 1;
            while (o.outWidth / scale / 5 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 5 >= REQUIRED_SIZE) {
                scale *= 5;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);

            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        } catch (Exception e) {
        }
    }

    @SuppressLint({"MissingInflatedId", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_application);
        // new FetchUserDataTask().execute();

        textToSpeech = new android.speech.tts.TextToSpeech(this, status -> {
            android.util.Log.d("LoanTTS", "TTS init status: " + status);
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                // Try ur-PK first (Urdu Pakistan). Falls back to en-IN (Indian English)
                // which pronounces Roman Urdu/Hinglish far more naturally than en-US.
                java.util.Locale locale = new java.util.Locale("ur", "PK");
                int result = textToSpeech.setLanguage(locale);
                if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                    android.util.Log.d("LoanTTS", "ur-PK not supported, falling back to en-IN");
                    locale = new java.util.Locale("en", "IN");
                    result = textToSpeech.setLanguage(locale);
                    if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                        android.util.Log.d("LoanTTS", "en-IN not supported, falling back to en-US");
                        locale = java.util.Locale.US;
                        textToSpeech.setLanguage(locale);
                    }
                }

                // Natural speech rate and pitch
                textToSpeech.setSpeechRate(0.95f);
                textToSpeech.setPitch(1.0f);

                // Select the best quality voice available for this locale
                final java.util.Locale finalLocale = locale;
                try {
                    java.util.Set<android.speech.tts.Voice> voices = textToSpeech.getVoices();
                    if (voices != null) {
                        android.speech.tts.Voice bestVoice = null;
                        int bestQuality = -1;

                        for (android.speech.tts.Voice voice : voices) {
                            if (!voice.getLocale().getLanguage().equals(finalLocale.getLanguage()))
                                continue;
                            String nameLower = voice.getName().toLowerCase(java.util.Locale.getDefault());
                            if (nameLower.contains("female")) continue;
                            int quality = voice.getQuality();
                            if (voice.isNetworkConnectionRequired()) quality += 200;
                            else quality += 50;
                            if (nameLower.contains("male")) quality += 100;
                            if (nameLower.contains("high") || nameLower.contains("premium") || nameLower.contains("enhanced"))
                                quality += 150;
                            if (quality > bestQuality) {
                                bestQuality = quality;
                                bestVoice = voice;
                            }
                        }

                        if (bestVoice != null) {
                            textToSpeech.setVoice(bestVoice);
                            android.util.Log.d("LoanTTS", "Selected voice: " + bestVoice.getName() + " quality=" + bestQuality);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoanTTS", "Voice selection error", e);
                }

                // IMPORTANT: Set utterance listener BEFORE setting ttsReady=true
                textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        android.util.Log.d("LoanTTS", "TTS onStart: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        android.util.Log.d("LoanTTS", "TTS onDone: " + utteranceId);
                        if ("AssistantReply".equals(utteranceId) && onTtsDoneRunnable != null) {
                            runOnUiThread(onTtsDoneRunnable);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        android.util.Log.e("LoanTTS", "TTS onError: " + utteranceId);
                        if ("AssistantReply".equals(utteranceId) && onTtsDoneRunnable != null) {
                            runOnUiThread(onTtsDoneRunnable);
                        }
                    }
                });

                ttsReady = true;
                android.util.Log.d("LoanTTS", "TTS is ready.");
            } else {
                android.util.Log.e("LoanTTS", "TTS initialization FAILED with status: " + status);
            }
        });

        initialization();
        setToolBar();
        setViews();

        // Removed prefillUserData() as per user request: do not prefill from preferences
        // Removed auto-popup of showFillMethodChoiceDialog() as per user request
        android.view.View btnVoiceAssistant = findViewById(R.id.btnVoiceAssistant);
        if (btnVoiceAssistant != null) {
            btnVoiceAssistant.setOnClickListener(v -> showVoiceAssistantDialog());
        }
    }

    private void prefillUserData() {
        // Disabled as per user request: do not pre-fill form fields from preferences when navigating to financing form
    }

    private void getAllDistricts() {
        String cachedDistricts = sharedManager.getString("CACHE_DISTRICTS", "");
        if (!cachedDistricts.isEmpty()) {
            try {
                ResponseModelAssanDistricts responseModelDistricts = new Gson().fromJson(cachedDistricts, ResponseModelAssanDistricts.class);
                populateDistrictsList(responseModelDistricts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        retrofit2.Call<ResponseModelAssanDistricts> call = AssanRetrofitClient.INSTANCE.getApiService().getDistricts();

        call.enqueue(new retrofit2.Callback<ResponseModelAssanDistricts>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseModelAssanDistricts> call, @NonNull retrofit2.Response<ResponseModelAssanDistricts> response) {
                if (response.isSuccessful()) {
                    ResponseModelAssanDistricts responseModelDistricts = response.body();
                    if (responseModelDistricts != null) {
                        sharedManager.saveString("CACHE_DISTRICTS", new Gson().toJson(responseModelDistricts));
                        populateDistrictsList(responseModelDistricts);
                    }
                } else if (cachedDistricts.isEmpty()) {
                    CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_network_error));
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseModelAssanDistricts> call, @NonNull Throwable t) {
                if (cachedDistricts.isEmpty()) {
                    CommonFunctions.handleApiFailure(LoanApplicationActivity.this, parentLayout, t);
                }
            }
        });
    }

    private void populateDistrictsList(ResponseModelAssanDistricts responseModelDistricts) {
        districtNames.clear();
        cityNames.clear();
        districtIDs.clear();
        if (responseModelDistricts != null) {
            districts = responseModelDistricts.getDistricts();
            if (districts != null) {
                districtNames.add("Select District");
                cityNames.add("Select District");
                districtIDs.add("");
                for (AssanDistrict district : districts) {
                    if (!CommonFunctions.isNullOrEmpty(district.getDistrictName())) {
                        districtNames.add(district.getDistrictName());
                        cityNames.add(district.getDistrictName());
                        districtIDs.add(district.getDistrictId());
                    }
                }
            }

            if (!CommonFunctions.isNullOrEmpty(districtNames)) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanApplicationActivity.this, android.R.layout.simple_spinner_item, districtNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDistrictName.setTitle(getString(R.string.hint_select_district));
                spinnerBusinessCityName.setTitle(getString(R.string.hint_select_city));
                spinnerDistrictName.setAdapter(adapter);
                spinnerBusinessCityName.setAdapter(adapter);

                String targetDistrict = !CommonFunctions.isNullOrEmpty(voiceDistrict) ? voiceDistrict : "";
                if (!targetDistrict.isEmpty()) {
                    for (int i = 0; i < districtNames.size(); i++) {
                        if (districtNames.get(i).equalsIgnoreCase(targetDistrict)) {
                            spinnerDistrictName.setSelection(i);
                            spinnerBusinessCityName.setSelection(i);
                            break;
                        }
                    }
                }

                String targetTehsil = !CommonFunctions.isNullOrEmpty(voiceTehsil) ? voiceTehsil : "";
                if (!targetTehsil.isEmpty()) {
                    pendingTehsilSelection = targetTehsil;
                }
            } else {
                CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_district_not_found));
            }
        }
    }

    private void getAllTehsils(String DISTRICT_ID) {
        String cachedTehsils = sharedManager.getString("CACHE_TEHSILS_" + DISTRICT_ID, "");
        if (!cachedTehsils.isEmpty()) {
            try {
                ResponseModelAssanTehsils responseData = new Gson().fromJson(cachedTehsils, ResponseModelAssanTehsils.class);
                populateTehsilsList(responseData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        retrofit2.Call<ResponseModelAssanTehsils> call = AssanRetrofitClient.INSTANCE.getApiService().getTehsils(DISTRICT_ID);
        call.enqueue(new retrofit2.Callback<ResponseModelAssanTehsils>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseModelAssanTehsils> call, @NonNull retrofit2.Response<ResponseModelAssanTehsils> response) {
                if (response.isSuccessful()) {
                    ResponseModelAssanTehsils responseData = response.body();
                    if (responseData != null && responseData.getSuccess()) {
                        sharedManager.saveString("CACHE_TEHSILS_" + DISTRICT_ID, new Gson().toJson(responseData));
                        populateTehsilsList(responseData);
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseModelAssanTehsils> call, Throwable t) {
                if (cachedTehsils.isEmpty()) {
                    CommonFunctions.handleApiFailure(LoanApplicationActivity.this, parentLayout, t);
                }
            }
        });

    }

    private void populateTehsilsList(ResponseModelAssanTehsils responseData) {
        tehsilNames.clear();
        tehsilIDs.clear();
        fieldUnitIDs.clear();
        regionIDs.clear();
        if (responseData != null && responseData.getSuccess()) {
            List<AssanTehsil> tehsils = responseData.getTehsils();
            if (tehsils != null) {
                tehsilNames.add("Select Tehsil");
                tehsilIDs.add("");
                fieldUnitIDs.add("");
                regionIDs.add("");
                for (AssanTehsil tehsil : tehsils) {
                    if (!CommonFunctions.isNullOrEmpty(tehsil.getTehsilName())) {
                        tehsilNames.add(tehsil.getTehsilName());
                        tehsilIDs.add(tehsil.getTehsilId() != null ? tehsil.getTehsilId() : "");
                        fieldUnitIDs.add(tehsil.getFieldUnitId() != null ? tehsil.getFieldUnitId() : "");
                        regionIDs.add(tehsil.getRegionId() != null ? tehsil.getRegionId() : "");
                    }
                }
            }

            if (!CommonFunctions.isNullOrEmpty(tehsilNames)) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanApplicationActivity.this, android.R.layout.simple_spinner_item, tehsilNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTehsilName.setTitle(getString(R.string.hint_select_tehsil));
                spinnerTehsilName.setAdapter(adapter);

                if (pendingTehsilSelection != null && !pendingTehsilSelection.isEmpty()) {
                    for (int i = 0; i < spinnerTehsilName.getAdapter().getCount(); i++) {
                        if (spinnerTehsilName.getItemAtPosition(i).toString().equalsIgnoreCase(pendingTehsilSelection)) {
                            spinnerTehsilName.setSelection(i);
                            break;
                        }
                    }
                    pendingTehsilSelection = null;
                }

                if (aiHelper != null) {
                    aiHelper.initialize("No previous financing history found.", districtNames, tehsilNames, bankNames, productNames);
                    aiHelper.setProductLimits(buildProductLimitsText());
                }
            } else {
                tehsilNames.clear();
                tehsilIDs.clear();
                tehsilNames.add("");
                tehsilIDs.add("");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanApplicationActivity.this, android.R.layout.simple_spinner_item, tehsilNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTehsilName.setAdapter(adapter);
                CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_no_tehsil_found));
            }
        }
    }

    private int getRequiredCnicValidityMonths() {
        if (spinnerDurationOfLoan != null && spinnerDurationOfLoan.getSelectedItem() != null) {
            String selectedDuration = spinnerDurationOfLoan.getSelectedItem().toString();
            if (selectedDuration.contains("6")) {
                return 6;
            } else if (selectedDuration.contains("12") || selectedDuration.contains("18")) {
                return 12;
            }
        }
        return 6;
    }

    private void setSpinners() {
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Select Financing Duration", "6 Months", "12 Months", "18 Months"});
        spinnerDurationOfLoan.setAdapter(durationAdapter);
        spinnerDurationOfLoan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && etApplicantCnicExpiry != null) {
                    String expiryStr = etApplicantCnicExpiry.getText().toString().trim();
                    if (!CommonFunctions.isNullOrEmpty(expiryStr)) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                            java.util.Date expiryDate = sdf.parse(expiryStr);
                            if (expiryDate != null) {
                                String selectedDuration = parent.getItemAtPosition(position).toString();
                                int reqMonths = selectedDuration.contains("6") ? 6 : 12;
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.add(java.util.Calendar.MONTH, reqMonths);
                                if (expiryDate.before(cal.getTime())) {
                                    String msg = (reqMonths == 6) ? getString(R.string.error_cnic_expired_message_6) : getString(R.string.error_cnic_expired_message_12);
                                    CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", getString(R.string.error_cnic_expired_title), msg, false);
                                    spinnerDurationOfLoan.setSelection(0);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> occupationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                "Select Occupation", "Work on Family Farm", "Work on Other's Farm", "Share Cropping", "Keep Livestock", "Fishing", "Food Processing", "Trading", "Handicrafts", "Garments / Stitching", "Other"
        });


        spinnerPurposeOfLoan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LoanPurpose = parent.getItemAtPosition(position).toString();
                if (LoanPurpose.equalsIgnoreCase("House improvement")) {
                    NatureOfBusiness = "";
                    etHowOldBusiness.setText("");
                    etBusinessAddress.setText("");
                    businessCityName = "";

                } else {

                    SourceOfIncome = "";
                }

                if (txtloanLimit != null) {
                    if (position > 0) {
                        txtloanLimit.setVisibility(View.VISIBLE);
                        try {
                            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
                            String minFormatted = df.format(Double.parseDouble(minLoanAmountList.get(position)));
                            String maxFormatted = df.format(Double.parseDouble(maxLoanAmountList.get(position)));
                            txtloanLimit.setText("PKR " + minFormatted + " - " + maxFormatted);
                        } catch (Exception e) {
                            txtloanLimit.setText("PKR " + minLoanAmountList.get(position) + " - " + maxLoanAmountList.get(position));
                        }
                    } else {
                        txtloanLimit.setVisibility(View.GONE);
                    }
                }
                if (position > 0) {
                    if (llSourceOfIncome != null) llSourceOfIncome.setVisibility(View.VISIBLE);
                } else {
                    if (llSourceOfIncome != null) llSourceOfIncome.setVisibility(View.GONE);
                }
                Constants.MINIMUM_LOAN_AMOUNT = CommonFunctions.convertToInteger(minLoanAmountList.get(position));
                Constants.MAXIMUM_LOAN_AMOUNT = CommonFunctions.convertToInteger(maxLoanAmountList.get(position));
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerDistrictName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Log.e("DistrictID::", districtIDs.get(position));
                    getAllTehsils(districtIDs.get(position));
                } else {
                    tehsilNames.clear();
                    tehsilIDs.clear();
                    tehsilNames.add("Select Tehsil");
                    tehsilIDs.add("");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanApplicationActivity.this, android.R.layout.simple_spinner_item, tehsilNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTehsilName.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerBusinessCityName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                businessCityName = cityNames.get(position);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerBankName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (isAutoFilling || skipNextBankReset) {
                    skipNextBankReset = false;
                    boolean isBankSelected = position > 0;
                    if (etBankAccNumber != null) etBankAccNumber.setEnabled(isBankSelected);
                    if (etBankBranchCode != null) etBankBranchCode.setEnabled(isBankSelected);
                    if (etBankBranch != null) etBankBranch.setEnabled(isBankSelected);
                    return;
                }
                String selectedBank = (String) parentView.getItemAtPosition(position);
                etBankBranchCode.setText("");
                etBankAccNumber.setText("");
                etBankBranch.setText("");

                boolean isBankSelected = position > 0;
                etBankAccNumber.setEnabled(isBankSelected);
                etBankBranchCode.setEnabled(isBankSelected);
                etBankBranch.setEnabled(isBankSelected);

                View parentAccNumber = (View) etBankAccNumber.getParent();
                View parentBranchCode = (View) etBankBranchCode.getParent();
                View parentBranch = (View) etBankBranch.getParent();
                if (parentAccNumber != null) parentAccNumber.setEnabled(isBankSelected);
                if (parentBranchCode != null) parentBranchCode.setEnabled(isBankSelected);
                if (parentBranch != null) parentBranch.setEnabled(isBankSelected);

                // String branchCodeFormat = CommonFunctions.getBranchCodeFormat(selectedBank);
                if (branchCodes != null && position > 0) {
                    currentBankBranchCodeLimit = branchCodes.get(position).length();
                    // etBankBranchCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(currentBankBranchCodeLimit)});
                    // etBankBranchCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)}); /* Limit removed */
                    etBankBranchCode.setFilters(new InputFilter[]{});
                    if (tvBankBranchCodeCounter != null) {
                        // tvBankBranchCodeCounter.setVisibility(View.VISIBLE);
                        // tvBankBranchCodeCounter.setText("0/" + currentBankBranchCodeLimit);
                    }
                } else if (tvBankBranchCodeCounter != null) {
                    tvBankBranchCodeCounter.setVisibility(View.GONE);
                }
                // String accountNumberFormat = CommonFunctions.getAccountNumberFormat(selectedBank);
                if (accountNoFormats != null && position > 0) {
                    currentBankAccNumberLimit = accountNoFormats.get(position).length();
                    // etBankAccNumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(currentBankAccNumberLimit)}); /* Limit removed */
                    if (tvBankAccNumberCounter != null) {
                        // tvBankAccNumberCounter.setVisibility(View.VISIBLE);
                        // tvBankAccNumberCounter.setText("0/" + currentBankAccNumberLimit);
                    }
                } else if (tvBankAccNumberCounter != null) {
                    tvBankAccNumberCounter.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void getAllBanks() {
        String cachedBanks = sharedManager.getString("CACHE_BANKS", "");
        if (!cachedBanks.isEmpty()) {
            try {
                ResponseModelAssanBanks responseData = new Gson().fromJson(cachedBanks, ResponseModelAssanBanks.class);
                populateBanksList(responseData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        retrofit2.Call<ResponseModelAssanBanks> call = AssanRetrofitClient.INSTANCE.getApiService().getBanks();
        call.enqueue(new Callback<ResponseModelAssanBanks>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<ResponseModelAssanBanks> call, @NonNull Response<ResponseModelAssanBanks> response) {
                if (response.isSuccessful()) {
                    ResponseModelAssanBanks responseBody = response.body();
                    if (responseBody != null) {
                        sharedManager.saveString("CACHE_BANKS", new Gson().toJson(responseBody));
                        populateBanksList(responseBody);
                    } else if (cachedBanks.isEmpty()) {
                        CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_no_bank_channels));
                    }
                } else if (cachedBanks.isEmpty()) {
                    CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_failed_retrieve_banks));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelAssanBanks> call, @NonNull Throwable t) {
                if (cachedBanks.isEmpty()) {
                    CommonFunctions.handleApiFailure(LoanApplicationActivity.this, parentLayout, t);
                }
            }
        });
    }

    private void populateBanksList(ResponseModelAssanBanks responseBody) {
        List<AssanBank> bankChannels = responseBody.getRecords();
        if (bankChannels != null && !bankChannels.isEmpty()) {
            List<AssanBank> activeBanks = new ArrayList<>();
            for (AssanBank b : bankChannels) {
                if (b.isActive()) activeBanks.add(b);
            }

            bankNames.clear();
            accountNoFormats.clear();
            branchCodes.clear();

            bankNames.add("Select Bank");
            accountNoFormats.add("");
            branchCodes.add("");

            for (AssanBank channel : activeBanks) {
                if (channel.getBankName() != null) {
                    bankNames.add(channel.getBankName());
                } else {
                    bankNames.add("Unknown Bank");
                }

                String accountFormat = channel.getAcNoFormat();
                if (accountFormat == null || accountFormat.isEmpty()) {
                    accountFormat = "BBBBBBAAAAAAAAAAAAAA";
                }
                int countA = (int) accountFormat.chars().filter(ch -> ch == 'A').count();
                if (countA == 0) {
                    if (channel.getAcDigitBkp() != null && channel.getAcDigitBkp() > 0) {
                        countA = channel.getAcDigitBkp();
                    } else {
                        countA = 14;
                    }
                }

                accountNoFormats.add(new String(new char[countA]).replace(' ', 'A'));

                int countB = (int) accountFormat.chars().filter(ch -> ch == 'B').count();
                if (countB == 0) countB = 6;

                branchCodes.add(new String(new char[countB]).replace(' ', 'B'));
            }

            setBanksOnSpinner(bankNames);

        } else {
            CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_no_bank_channels));
        }
    }


    private void setBanksOnSpinner(List<String> bankNames) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanApplicationActivity.this, android.R.layout.simple_spinner_item, bankNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBankName.setTitle(getString(R.string.hint_select_bank));
        spinnerBankName.setAdapter(adapter);

        String targetBank = !CommonFunctions.isNullOrEmpty(voiceBankName) ? voiceBankName : "";
        if (!targetBank.isEmpty()) {
            for (int i = 0; i < bankNames.size(); i++) {
                if (bankNames.get(i).equalsIgnoreCase(targetBank) || bankNames.get(i).toLowerCase().contains(targetBank.toLowerCase())) {
                    spinnerBankName.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setViews() {
        applicantName = "";
        applicantCnic = "";
        applicantDOB = "";
        applicantPhone = "";
        applicantGender = "";
        applicantCnicExpiry = "";

        if (!CommonFunctions.isNullOrZero(Constants.CLIENT_ID)) {
            txtClientIdLoan.setText(CommonFunctions.convertToString(Constants.CLIENT_ID));
        }

        if (!CommonFunctions.isNullOrEmpty(applicantCnic)) etApplicantCnic.setText(applicantCnic);

        // Preload profile picture if already saved in SharedManager / user profile
        String savedProfilePic = sharedManager != null ? sharedManager.getString("PROFILE_PICTURE", "") : "";
        if (!CommonFunctions.isNullOrEmpty(savedProfilePic)) {
            profilePictureBase64 = savedProfilePic;
            if (savedProfilePic.startsWith("http://") || savedProfilePic.startsWith("https://") || savedProfilePic.startsWith("/uploads/") || savedProfilePic.startsWith("uploads/")) {
                String fullUrl = com.assan.finance.apis.ApiManager.getFullImageUrl(savedProfilePic);
                com.squareup.picasso.Picasso.get()
                        .load(fullUrl)
                        .placeholder(R.drawable.dummy_profile_image)
                        .error(R.drawable.dummy_profile_image)
                        .into(ivProfilePicture);
            } else {
                try {
                    byte[] decoded = android.util.Base64.decode(savedProfilePic, android.util.Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bmp != null) ivProfilePicture.setImageBitmap(bmp);
                } catch (Exception ignored) {}
            }
        }

        setSpinners();
        getAllDistricts();
        getAllBanks();
        radioGroupMaritalStatus.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == R.id.radioButtonSingle) {
                isMarried = 0;
            } else if (checkedId == R.id.radioButtonMarried) {
                isMarried = 1;
            }
        });

        submitLoanApplicationButton.setOnClickListener(v -> {
            if (validateInputFields()) {
                if (CommonFunctions.isNetworkAvailable(this)) {
                    submitLoanApplication();
                } else {
                    CommonFunctions.showSnakeBar(parentLayout, getString(R.string.msg_network_error));
                }
            }
        });

        termsAndConditionsCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    submitLoanApplicationButton.setEnabled(true);
                } else {
                    submitLoanApplicationButton.setEnabled(false);
                }
            }
        });
        setSpannableText();
    }

    private boolean validateInputFields() {
        int loanAmount = 0;
        try {
            if (etLoanAmount.getText() != null && !etLoanAmount.getText().toString().isEmpty()) {
                String rawAmount = etLoanAmount.getText().toString().replaceAll(",", "");
                loanAmount = CommonFunctions.convertToInteger(rawAmount);
            }
        } catch (Exception e) {
        }


        // Profile Picture
        if (CommonFunctions.isNullOrEmpty(profilePictureBase64)) {
            CommonFunctions.showToast(LoanApplicationActivity.this, "Please select a profile picture");
            CommonFunctions.scrollViewToVisible(findViewById(R.id.ivProfilePicture));
            return false;
        }

        // CNIC scan is mandatory (camera or gallery): both pictures and the details read from them
        if (CommonFunctions.isNullOrEmpty(cnicFrontBase64) || CommonFunctions.isNullOrEmpty(cnicBackBase64)) {
            CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", "CNIC Scan Required",
                    "Please scan both sides (front and back) of your original CNIC using the camera or the gallery.", false);
            CommonFunctions.scrollViewToVisible(btnScanCnicUnified != null ? btnScanCnicUnified : cardCnicFront);
            return false;
        }
        if (isEmptyText(etApplicantName) || isEmptyText(etApplicantCnic) || isEmptyText(etApplicantDOB)
                || isEmptyText(etApplicantCnicIssue) || isEmptyText(etApplicantCnicExpiry)
                || isEmptyText(etPresentAddress) || isEmptyText(etPermanentAddress)
                || radioGroupGender.getCheckedRadioButtonId() == -1) {
            CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", "CNIC Details Missing",
                    "Some details could not be read from your CNIC pictures. Please scan your CNIC again with clear, well-lit pictures.", false);
            CommonFunctions.scrollViewToVisible(btnScanCnicUnified != null ? btnScanCnicUnified : cardCnicFront);
            return false;
        }

        // Personal Info
        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etApplicantName, "Full Name"))
            return false;
        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etApplicantCnic, getString(R.string.str_cnic_number_label)))
            return false;
        if (etApplicantCnic.getText().toString().trim().length() != 15) {
            CommonFunctions.showInlineError(LoanApplicationActivity.this, etApplicantCnic, "Please enter a valid 13-digit CNIC");
            CommonFunctions.scrollViewToVisible(etApplicantCnic);
            return false;
        }

        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etApplicantCnicExpiry, "CNIC Expiry Date")) {
            return false;
        } else {
            String expiryStr = etApplicantCnicExpiry.getText().toString().trim();
            
            // Calculate actual max date from front and back OCR dates
            String silentOcrExpiryDate = "";
            try {
                java.text.SimpleDateFormat uiSdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                java.util.Date frontD = frontOcrDate.isEmpty() ? null : uiSdf.parse(frontOcrDate);
                java.util.Date backD = backOcrDate.isEmpty() ? null : uiSdf.parse(backOcrDate);
                
                if (frontD != null && backD != null) {
                    silentOcrExpiryDate = frontD.after(backD) ? frontOcrDate : backOcrDate;
                } else if (frontD != null) {
                    silentOcrExpiryDate = frontOcrDate;
                } else if (backD != null) {
                    silentOcrExpiryDate = backOcrDate;
                }
            } catch (Exception ignored) {}

            // If we silently found a date via OCR, compare it
            if (!silentOcrExpiryDate.isEmpty()) {
                if (!silentOcrExpiryDate.equals(expiryStr)) {
                    CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", "Invalid Expiry Date", "The Expiry Date you entered does not match the date on your CNIC picture. Please enter the correct date.", false);
                    CommonFunctions.scrollViewToVisible(etApplicantCnicExpiry);
                    return false;
                }
            }

            if (!CommonFunctions.isNullOrEmpty(expiryStr)) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                    java.util.Date expiryDate = sdf.parse(expiryStr);
                    java.util.Date currentDate = new java.util.Date();
                    
                    if (expiryDate != null) {
                        int requiredMonths = getRequiredCnicValidityMonths();
                        
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.add(java.util.Calendar.MONTH, requiredMonths);
                        
                        if (expiryDate.before(cal.getTime())) {
                            String msg = requiredMonths == 6 ? getString(R.string.error_cnic_expired_message_6) : getString(R.string.error_cnic_expired_message_12);
                            CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", getString(R.string.error_cnic_expired_title), msg, false);
                            CommonFunctions.scrollViewToVisible(etApplicantCnicExpiry);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etApplicantDOB, getString(R.string.str_date_of_birth_label)))
            return false;
        // Applicant must be 18 to 60 years old today
        if (!CommonFunctions.isAgeAllowed(etApplicantDOB.getText().toString().trim())) {
            CommonFunctions.showAgeRestrictionDialog(LoanApplicationActivity.this, false, etApplicantDOB.getText().toString().trim());
            CommonFunctions.scrollViewToVisible(etApplicantDOB);
            return false;
        }

        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etApplicantPhone, getString(R.string.str_phone_number_label)))
            return false;
        if (etApplicantPhone.getText().toString().trim().length() != 12) {
            CommonFunctions.showInlineError(LoanApplicationActivity.this, etApplicantPhone, "Please enter a valid 11-digit Phone Number");
            CommonFunctions.scrollViewToVisible(etApplicantPhone);
            return false;
        }
        if (radioGroupGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(LoanApplicationActivity.this, getString(R.string.msg_select_gender), Toast.LENGTH_SHORT).show();
            CommonFunctions.scrollViewToVisible(radioGroupGender);
            return false;
        } else {
            int selectedId = radioGroupGender.getCheckedRadioButtonId();
            if (selectedId == R.id.radioButtonMale) {
                applicantGender = "M";
            } else if (selectedId == R.id.radioButtonFemale) {
                applicantGender = "F";
            } else {
                applicantGender = "O";
            }
        }

        if (radioGroupMaritalStatus.getCheckedRadioButtonId() == -1) {
            CommonFunctions.showToast(LoanApplicationActivity.this, getString(R.string.msg_select_marital_status));
            CommonFunctions.scrollViewToVisible(radioGroupMaritalStatus);
            return false;
        }

        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etPresentAddress, "Present Address"))
            return false;
        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etPermanentAddress, "Permanent Address"))
            return false;

        // Loan Details
        if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerDistrictName, spinner_error_purpose_of_loan, "Please select District Name"))
            return false;
        if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerTehsilName, spinner_error_purpose_of_loan, "Please select Tehsil Name"))
            return false;
        if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerBankName, spinner_error_purpose_of_loan, "Please select Bank Name"))
            return false;
        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etBankAccNumber, "IBAN Number"))
            return false;
        // Pakistani IBAN: 24 characters = PK + 2 check digits + 4-letter bank code + 16-digit account
        if (!etBankAccNumber.getText().toString().trim().toUpperCase(java.util.Locale.ROOT).matches("PK\\d{2}[A-Z]{4}\\d{16}")) {
            CommonFunctions.showInlineError(LoanApplicationActivity.this, etBankAccNumber, "IBAN must be 24 characters, starting with PK (e.g. PK36SCBL0000001123456702)");
            CommonFunctions.scrollViewToVisible(etBankAccNumber);
            return false;
        }
//        if (etBankAccNumber.getText().toString().trim().length() != currentBankAccNumberLimit) {
//            CommonFunctions.showInlineError(LoanApplicationActivity.this, etBankAccNumber, "Account Number must be exactly " + currentBankAccNumberLimit + " digits");
//            return false;
//        }

        // Branch code is hidden and no longer required (IBAN identifies the account)
        //        if (etBankBranchCode.getText().toString().trim().length() != currentBankBranchCodeLimit) {
//            CommonFunctions.showInlineError(LoanApplicationActivity.this, etBankBranchCode, "Branch Code must be exactly " + currentBankBranchCodeLimit + " digits");
//            CommonFunctions.scrollViewToVisible(etBankBranchCode);
//            return false;
//        }
//        if (etBankBranchCode.getText().toString().trim().length() != 4) {
//            CommonFunctions.showInlineError(LoanApplicationActivity.this, etBankBranchCode, "Branch Code must be exactly 4 digits");
//            CommonFunctions.scrollViewToVisible(etBankBranchCode);
//            return false;
//        }
        // if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etBankBranch, "Branch Address")) return false;

        if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerPurposeOfLoan, spinner_error_purpose_of_loan, "Please select Purpose of Financing"))
            return false;
        int selectedIncomeId = radioGroupSourceOfIncome.getCheckedRadioButtonId();
        if (selectedIncomeId == -1) {
            Toast.makeText(this, getString(R.string.msg_select_income_source), Toast.LENGTH_SHORT).show();
            CommonFunctions.scrollViewToVisible(radioGroupSourceOfIncome);
            return false;
        }

        if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etLoanAmount, "Financing Amount"))
            return false;
        if (loanAmount < Constants.MINIMUM_LOAN_AMOUNT || loanAmount > Constants.MAXIMUM_LOAN_AMOUNT) {
            if (txtloanLimit != null)
                txtloanLimit.setHint("Financing Limit: " + Constants.MINIMUM_LOAN_AMOUNT + " - " + Constants.MAXIMUM_LOAN_AMOUNT);
            etLoanAmount.setError("Financing Limit: " + Constants.MINIMUM_LOAN_AMOUNT + " - " + Constants.MAXIMUM_LOAN_AMOUNT);
            etLoanAmount.requestFocus();
            if (SVParent != null) {
                SVParent.post(() -> {
                    android.graphics.Rect rect = new android.graphics.Rect(0, 0, etLoanAmount.getWidth(), etLoanAmount.getHeight());
                    SVParent.offsetDescendantRectToMyCoords(etLoanAmount, rect);
                    SVParent.smoothScrollTo(0, rect.top - 50); // subtract a bit for padding
                });
            }
            return false;
        }

        if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerDurationOfLoan, spinner_error_purpose_of_loan, "Please select Duration of Loan"))
            return false;

        // Additional Info
        if (!LoanPurpose.equalsIgnoreCase("House improvement")) {
            if (radioGroupNatureOfBusiness != null && radioGroupNatureOfBusiness.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, getString(R.string.msg_select_business_nature), Toast.LENGTH_SHORT).show();
                CommonFunctions.scrollViewToVisible(radioGroupNatureOfBusiness);
                return false;
            }
        }

        if (!LoanPurpose.equalsIgnoreCase("House improvement")) {
            if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etHowOldBusiness, "Business Duration (Years)"))
                return false;
            if (!CommonFunctions.validateEditText(LoanApplicationActivity.this, SVParent, etBusinessAddress, "Complete Business Address"))
                return false;
            if (!CommonFunctions.validateSpinner(LoanApplicationActivity.this, spinnerBusinessCityName, spinner_error_purpose_of_loan, "Please select City of Business"))
                return false;
        } else {
            // Optional fields for house improvement
        }
        int selectedOccupationType = rgOccupationType.getCheckedRadioButtonId();
        if (selectedOccupationType == -1) {
            Toast.makeText(this, getString(R.string.msg_select_occupation_type), Toast.LENGTH_SHORT).show();
            CommonFunctions.scrollViewToVisible(rgOccupationType);
            return false;
        }

        if (selectedOccupationType == R.id.rbAgriculture) {
            if (rgAgricultureOccupation.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, getString(R.string.msg_select_agri_occupation), Toast.LENGTH_SHORT).show();
                CommonFunctions.scrollViewToVisible(rgAgricultureOccupation);
                return false;
            }
        } else if (selectedOccupationType == R.id.rbNonAgriculture) {
            if (rgNonAgricultureOccupation.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, getString(R.string.msg_select_non_agri_occupation), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }


    private void setSpannableText() {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(getString(R.string.str_i_have_read_and_accept_the));
        String termsAndConditions = getString(R.string.str_terms_amp_conditions);
        int start = spannableStringBuilder.length();
        spannableStringBuilder.append(termsAndConditions);
        int end = spannableStringBuilder.length();
        spannableStringBuilder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermsDialog();
            }
        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsAndConditionsCheckbox.setText(spannableStringBuilder);
        termsAndConditionsCheckbox.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showTermsDialog() {
        Dialog dialog = new Dialog(LoanApplicationActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_terms_conditions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.horizontalMargin = 0.04f;
            dialog.getWindow().setAttributes(params);
        }

        dialog.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        TextView termsTextView = dialog.findViewById(R.id.termsTextView);
        termsTextView.setText(R.string.termsAndConditionsUrdu);

        dialog.show();
    }

    private void setToolBar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(view -> CommonFunctions.onBackFinish(this));
        }
        heading.setText(getResources().getString(R.string.loan_application));
    }

    private void initialization() {
        termsAndConditionsCheckbox = findViewById(R.id.termsAndConditionsCheckbox);
        SVParent = findViewById(R.id.SVParent);
        radioGroupMaritalStatus = findViewById(R.id.radioGroupMaritalStatus);
        parentLayout = findViewById(android.R.id.content);
        // initProgressBar removed
        mSqliteDatabase = new SqliteDatabase(this);
        btnBack = findViewById(R.id.btnBack);
        heading = findViewById(R.id.heading);
        sharedManager = SharedManager.getInstance(this);
        txtClientIdLoan = findViewById(R.id.tvclientidloan);

        setupProfilePicturePicker();

        // Personal Info
        etApplicantName = findViewById(R.id.etApplicantName);
        etApplicantCnic = findViewById(R.id.etApplicantCnic);
        etApplicantCnicExpiry = findViewById(R.id.etApplicantCnicExpiry);
        etApplicantCnicIssue = findViewById(R.id.etApplicantCnicIssue);
        etApplicantDOB = findViewById(R.id.etApplicantDOB);
        etApplicantPhone = findViewById(R.id.etApplicantPhone);

        ivCnicFront = findViewById(R.id.ivCnicFront);
        ivCnicBack = findViewById(R.id.ivCnicBack);
        cardCnicFront = findViewById(R.id.cardCnicFront);
        cardCnicBack = findViewById(R.id.cardCnicBack);
        ivViewCnicFront = findViewById(R.id.ivViewCnicFront);
        ivViewCnicBack = findViewById(R.id.ivViewCnicBack);
        ivRotateCnicFront = findViewById(R.id.ivRotateCnicFront);
        ivRotateCnicBack = findViewById(R.id.ivRotateCnicBack);

        if (ivRotateCnicFront != null) {
            ivRotateCnicFront.setOnClickListener(v -> {
                if (cnicFrontUri != null) {
                    Uri rotatedUri = CnicRotationHelper.rotateUriBy90(LoanApplicationActivity.this, cnicFrontUri);
                    if (rotatedUri != null) {
                        cnicFrontUri = rotatedUri;
                        ivCnicFront.setImageURI(null);
                        ivCnicFront.setImageURI(rotatedUri);
                        cnicFrontBase64 = CommonFunctions.convertToBase64(rotatedUri, LoanApplicationActivity.this);
                        runSilentOcrForExpiry(rotatedUri, true);
                    }
                }
            });
        }

        if (ivRotateCnicBack != null) {
            ivRotateCnicBack.setOnClickListener(v -> {
                if (cnicBackUri != null) {
                    Uri rotatedUri = CnicRotationHelper.rotateUriBy90(LoanApplicationActivity.this, cnicBackUri);
                    if (rotatedUri != null) {
                        cnicBackUri = rotatedUri;
                        ivCnicBack.setImageURI(null);
                        ivCnicBack.setImageURI(rotatedUri);
                        cnicBackBase64 = CommonFunctions.convertToBase64(rotatedUri, LoanApplicationActivity.this);
                        runSilentOcrForExpiry(rotatedUri, false);
                    }
                }
            });
        }

        cardCnicFront.setOnClickListener(v -> {
            isPickingCnicFront = true;
            pickingImageType = 0;
            showCnicImageSourceDialog();
        });

        cardCnicBack.setOnClickListener(v -> {
            isPickingCnicFront = false;
            pickingImageType = 1;
            showCnicImageSourceDialog();
        });

        if (ivViewCnicFront != null) {
            ivViewCnicFront.setOnClickListener(v -> {
                showFullScreenImage(cnicFrontBase64);
            });
        }

        if (ivViewCnicBack != null) {
            ivViewCnicBack.setOnClickListener(v -> {
                showFullScreenImage(cnicBackBase64);
            });
        }

        // Setup Auto Formatting
        CommonFunctions.addCNICFormatting(etApplicantCnic);
        CommonFunctions.addPhoneNumberFormatting(etApplicantPhone, this);

        // Date Pickers
        etApplicantDOB.setOnClickListener(v -> CommonFunctions.showDatePickerDialog(this, etApplicantDOB, true));

        etApplicantCnicIssue.setOnClickListener(v -> CommonFunctions.showDatePickerDialog(this, etApplicantCnicIssue, true));

        etApplicantCnicExpiry.setOnClickListener(v -> CommonFunctions.showDatePickerDialog(this, etApplicantCnicExpiry, false));

        radioGroupGender = findViewById(R.id.radioGroupGender);
        if (radioGroupGender != null) {
            radioGroupGender.clearCheck();
        }
        radioButtonSingle = findViewById(R.id.radioButtonSingle);
        radioButtonMarried = findViewById(R.id.radioButtonMarried);
        etPresentAddress = findViewById(R.id.etPresentAddress);
        etPermanentAddress = findViewById(R.id.etPermanentAddress);
        // CNIC scan is mandatory: these fields are filled only from the scan (camera or gallery), never typed
        lockCnicFieldsUntilScanned();

        // Loan Details
        spinnerDistrictName = findViewById(R.id.spinnerdistrictname);
        spinnerTehsilName = findViewById(R.id.spinnertehsilname);
        spinnerBankName = findViewById(R.id.spinnerbankname);
        etBankAccNumber = findViewById(R.id.etBankAccNumber);
        etBankBranchCode = findViewById(R.id.etBankBranchCode);
        etBankBranch = findViewById(R.id.etBankBranch);

        etBankAccNumber.setEnabled(false);
        etBankBranchCode.setEnabled(false);
        etBankBranch.setEnabled(false);

        View parentAccNumber = (View) etBankAccNumber.getParent();
        View parentBranchCode = (View) etBankBranchCode.getParent();
        View parentBranch = (View) etBankBranch.getParent();
        if (parentAccNumber != null) parentAccNumber.setEnabled(false);
        if (parentBranchCode != null) parentBranchCode.setEnabled(false);
        if (parentBranch != null) parentBranch.setEnabled(false);
        tvBankAccNumberCounter = findViewById(R.id.tvBankAccNumberCounter);
        tvBankBranchCodeCounter = findViewById(R.id.tvBankBranchCodeCounter);

        // IBAN: capital letters and digits only, max 24. Done with an input filter: the old
        // textAllCaps + android:digits combination kept the cursor stuck while typing.
        etBankAccNumber.setFilters(new android.text.InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    StringBuilder clean = new StringBuilder(end - start);
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) clean.append(c);
                        else if (c >= 'a' && c <= 'z') clean.append(Character.toUpperCase(c));
                    }
                    return clean.toString().contentEquals(source.subSequence(start, end)) ? null : clean;
                },
                new android.text.InputFilter.LengthFilter(24)
        });

        etBankAccNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvBankAccNumberCounter != null && tvBankAccNumberCounter.getVisibility() == View.VISIBLE) {
                    int remaining = currentBankAccNumberLimit - s.length();
                    // tvBankAccNumberCounter.setText(remaining + " limit");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        etBankBranchCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvBankBranchCodeCounter != null && tvBankBranchCodeCounter.getVisibility() == View.VISIBLE) {
                    int remaining = currentBankBranchCodeLimit - s.length();
                    // tvBankBranchCodeCounter.setText(remaining + " limit");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        spinnerPurposeOfLoan = findViewById(R.id.spinnerPurposeofLoan);
        radioGroupSourceOfIncome = findViewById(R.id.radioGroupSourceOfIncome);
        llSourceOfIncome = findViewById(R.id.llSourceOfIncome);
        txtloanLimit = findViewById(R.id.txtloanLimit);
        etLoanAmount = findViewById(R.id.etLoanAmount);
        etLoanAmount.addTextChangedListener(new com.assan.finance.utils.NumberTextWatcher(etLoanAmount));
        spinnerDurationOfLoan = findViewById(R.id.spinnerDurationOfLoan);
        radioGroupNatureOfBusiness = findViewById(R.id.radioGroupNatureOfBusiness);

        if (etApplicantCnic != null) {
            etApplicantCnic.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String cnic = s.toString().trim().replace("-", "");
                    if (cnic.length() == 13 && radioGroupGender != null) {
                        try {
                            int lastDigit = Character.getNumericValue(cnic.charAt(12));
                            if (lastDigit % 2 != 0) {
                                radioGroupGender.check(R.id.radioButtonMale);
                            } else {
                                radioGroupGender.check(R.id.radioButtonFemale);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        // Additional Info
        etHowOldBusiness = findViewById(R.id.etHowOldBusiness);
        etBusinessAddress = findViewById(R.id.etBusinessAddress);
        spinnerBusinessCityName = findViewById(R.id.spinnerBusinessCityName);
        rgOccupationType = findViewById(R.id.rgOccupationType);
        rgAgricultureOccupation = findViewById(R.id.rgAgricultureOccupation);
        rgNonAgricultureOccupation = findViewById(R.id.rgNonAgricultureOccupation);
        llAgricultureOptions = findViewById(R.id.llAgricultureOptions);
        llNonAgricultureOptions = findViewById(R.id.llNonAgricultureOptions);

        rgOccupationType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbAgriculture) {
                    llAgricultureOptions.setVisibility(View.VISIBLE);
                    llNonAgricultureOptions.setVisibility(View.GONE);
                    rgNonAgricultureOccupation.clearCheck();
                } else if (checkedId == R.id.rbNonAgriculture) {
                    llAgricultureOptions.setVisibility(View.GONE);
                    llNonAgricultureOptions.setVisibility(View.VISIBLE);
                    rgAgricultureOccupation.clearCheck();
                }
            }
        });

        submitLoanApplicationButton = findViewById(R.id.submitButton);
        btnScanCnicUnified = findViewById(R.id.btnScanCnicUnified);
        tvUnifiedScanStatus = findViewById(R.id.tvUnifiedScanStatus);

        formatData();
        getLoanMinMax();

        // The instance variable btnScanCnicUnified is already assigned.
        // We will just use it.
        final TextView tvUnifiedScanStatus = findViewById(R.id.tvUnifiedScanStatus);

        if (btnScanCnicUnified != null) {
            btnScanCnicUnified.setOnClickListener(v -> {
                Intent intent = new Intent(LoanApplicationActivity.this, com.assan.finance.activities.CnicScannerActivity.class);
                cnicScannerLauncher.launch(intent);
            });
        }

        // TEMPORARY trial button: Google ML Kit Document Scanner (the old scanner above is unchanged)
        View btnScanCnicMlKit = findViewById(R.id.btnScanCnicMlKit);
        if (btnScanCnicMlKit != null) btnScanCnicMlKit.setOnClickListener(v -> startMlKitCnicScan());

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

/*    @SuppressLint("StaticFieldLeak")
    public class FetchUserDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result = new StringBuilder();
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(ApiManager.BASE_URL + "getdata_registration_form.php?clientid=" + Constants.CLIENT_ID);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }
                bufferedReader.close();
                inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();


            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && !result.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray registrationData = jsonObject.getJSONArray("APP_REGISTRATION");

                    if (registrationData.length() > 0) {
                        JSONObject data = registrationData.getJSONObject(0);
                        applicantName = data.getString("NAME");
                        applicantCnic = data.getString("CNIC");
                        String rawDob = data.getString("DATE_OF_BIRTH");
                        applicantDOB = !CommonFunctions.isNullOrEmpty(rawDob) ? CommonFunctions.formatDateOfBirth(rawDob) : "";
                        etApplicantName.setText(applicantName);
                        etApplicantCnic.setText(applicantCnic);
                        etApplicantDOB.setText(applicantDOB);
                        
                    }
                } catch (JSONException e) {
                    
                    e.printStackTrace();

                }
            }
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            CommonFunctions.onBackFinish(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void formatData() {
    }

    public void submitLoanApplication() {
        if (Constants.CLIENT_ID == 0) {
            Constants.CLIENT_ID = SharedManager.getInstance(this).getInt("CLIENT_ID", 0);
        }
        if (Constants.CLIENT_ID == 0) {
            Toast.makeText(this, getString(R.string.msg_session_expired), Toast.LENGTH_LONG).show();
            return;
        }

        // Ensure string variables have the latest user input before submission
        if (etApplicantName != null) applicantName = etApplicantName.getText().toString().trim();
        if (etApplicantCnic != null)
            applicantCnic = etApplicantCnic.getText().toString().trim().replace("-", "");
        if (etApplicantDOB != null) applicantDOB = etApplicantDOB.getText().toString().trim();
        if (etApplicantCnicExpiry != null)
            applicantCnicExpiry = etApplicantCnicExpiry.getText().toString().trim();
        String applicantCnicIssue = "";
        if (etApplicantCnicIssue != null)
            applicantCnicIssue = etApplicantCnicIssue.getText().toString().trim();
        if (radioGroupGender != null && radioGroupGender.getCheckedRadioButtonId() != -1) {
            int selectedId = radioGroupGender.getCheckedRadioButtonId();
            if (selectedId == R.id.radioButtonMale) {
                applicantGender = "M";
            } else if (selectedId == R.id.radioButtonFemale) {
                applicantGender = "F";
            } else {
                applicantGender = "O";
            }
        }

        int checkedType = rgOccupationType.getCheckedRadioButtonId();
        if (checkedType == R.id.rbAgriculture) {
            RadioButton selectedRb = findViewById(rgAgricultureOccupation.getCheckedRadioButtonId());
            selectedOccupation = selectedRb != null ? selectedRb.getText().toString() : "";
        } else if (checkedType == R.id.rbNonAgriculture) {
            RadioButton selectedRb = findViewById(rgNonAgricultureOccupation.getCheckedRadioButtonId());
            selectedOccupation = selectedRb != null ? selectedRb.getText().toString() : "";
        } else {
            selectedOccupation = "";
        }

        // Map UI labels to backend expected values
        switch (selectedOccupation) {
            case "Work on Other Farm":
                selectedOccupation = "Work on Other's Farm";
                break;
            case "Share Cropping on Farm":
                selectedOccupation = "Share Cropping";
                break;
            case "Keep other's Livestock":
                selectedOccupation = "Keep Livestock";
                break;
            case "Fishing Business":
                selectedOccupation = "Fishing";
                break;
            case "Food Processing & Vending":
                selectedOccupation = "Food Processing";
                break;
            case "Trading / Buy and Sell":
                selectedOccupation = "Trading";
                break;
            case "Garments":
                selectedOccupation = "Garments / Stitching";
                break;
            case "Mechanic":
            case "Carpentry / Metal Work":
            case "Transport Service":
            case "Wage Earner / Construction":
            case "Office Work":
            case "Sewing":
            case "Government Employee":
            case "Private Job":
            case "House Wife":
                selectedOccupation = "Other";
                break;
        }

        if (radioGroupNatureOfBusiness != null && radioGroupNatureOfBusiness.getCheckedRadioButtonId() != -1) {
            RadioButton selectedRb = findViewById(radioGroupNatureOfBusiness.getCheckedRadioButtonId());
            if (selectedRb != null) NatureOfBusiness = selectedRb.getText().toString();
        } else {
            NatureOfBusiness = "";
        }

        // FIX: Removed the client-side "Proscribed Persons" simulation. It blocked real applicants
        // based on keywords in their name (e.g. "banned") or a hard-coded CNIC and told them they
        // were on the national Proscribed Persons List. AML/CFT screening must be done on the
        // server against the official NACTA list, never with hard-coded rules in the app.


        CommonFunctions.showButtonLoader(submitLoanApplicationButton, getString(R.string.str_submitting_feedback));

        // Create RequestBody instances for each form field
        String selectedTehsilId = "";
        String selectedRegionId = "";
        String selectedFieldUnitId = "";
        int tehsilPos = spinnerTehsilName.getSelectedItemPosition();
        if (tehsilPos >= 0 && tehsilPos < tehsilIDs.size()) {
            selectedTehsilId = tehsilIDs.get(tehsilPos);
            selectedRegionId = regionIDs.get(tehsilPos);
            selectedFieldUnitId = fieldUnitIDs.get(tehsilPos);
        }
        RequestBody tehsilIdRequestBody = RequestBody.create(selectedTehsilId, MediaType.parse("text/plain"));
        RequestBody regionIdRequestBody = RequestBody.create(selectedRegionId, MediaType.parse("text/plain"));
        RequestBody fieldUnitIdRequestBody = RequestBody.create(selectedFieldUnitId, MediaType.parse("text/plain"));
        String businessDurationStr = Objects.requireNonNull(etHowOldBusiness.getText()).toString().trim();
        String businessDurationNum = businessDurationStr.replaceAll("[^0-9]", "");
        if (businessDurationNum.isEmpty()) businessDurationNum = "0";
        RequestBody howOldBusinessRequestBody = RequestBody.create(businessDurationNum, MediaType.parse("text/plain"));
        RequestBody presentAddressRequestBody = RequestBody.create(Objects.requireNonNull(etPresentAddress.getText()).toString().trim(), MediaType.parse("text/plain"));
        RequestBody permanentAddressRequestBody = RequestBody.create(Objects.requireNonNull(etPermanentAddress.getText()).toString().trim(), MediaType.parse("text/plain"));
        String selectedDistrictId = "";
        int districtPos = spinnerDistrictName.getSelectedItemPosition();
        if (districtPos >= 0 && districtPos < districtIDs.size())
            selectedDistrictId = districtIDs.get(districtPos);
        RequestBody districtIdRequestBody = RequestBody.create(selectedDistrictId, MediaType.parse("text/plain"));
        RequestBody bankAccountNumberRequestBody = RequestBody.create(Objects.requireNonNull(etBankAccNumber.getText()).toString().trim(), MediaType.parse("text/plain"));
        RequestBody bankNameRequestBody = RequestBody.create(spinnerBankName.getSelectedItem().toString().trim(), MediaType.parse("text/plain"));
        RequestBody bankBranchCodeRequestBody = RequestBody.create(Objects.requireNonNull(etBankBranchCode.getText()).toString().trim(), MediaType.parse("text/plain"));

        // Remove commas from loan amount
        String rawAmount = etLoanAmount.getText().toString().replaceAll(",", "");
        RequestBody loanAmountRequestBody = RequestBody.create(rawAmount.trim(), MediaType.parse("text/plain"));

        RequestBody occupationRequestBody = RequestBody.create(Objects.requireNonNull(selectedOccupation), MediaType.parse("text/plain"));
        String durationStr = spinnerDurationOfLoan.getSelectedItem().toString().trim();
        String durationNum = durationStr.replaceAll("[^0-9]", "");
        if (durationNum.isEmpty()) durationNum = "0";
        RequestBody durationOfLoanRequestBody = RequestBody.create(durationNum, MediaType.parse("text/plain"));
        RequestBody businessAddressRequestBody = RequestBody.create(Objects.requireNonNull(etBusinessAddress.getText()).toString().trim(), MediaType.parse("text/plain"));
        // FIX: businessCityName was null (NPE) if the city spinner never fired, and was sent as
        // "Select District" for House-improvement applications. Read it from the spinner instead.
        String businessCityToSend = "";
        int cityPos = spinnerBusinessCityName != null ? spinnerBusinessCityName.getSelectedItemPosition() : -1;
        if (cityPos > 0 && cityPos < cityNames.size()) {
            businessCityToSend = cityNames.get(cityPos);
        }
        RequestBody businessCityNameRequestBody = RequestBody.create(businessCityToSend, MediaType.parse("text/plain"));
        RequestBody applicantNameRequestBody = RequestBody.create(Objects.requireNonNull(applicantName), MediaType.parse("text/plain"));
        RequestBody applicantCNICRequestBody = RequestBody.create(Objects.requireNonNull(applicantCnic), MediaType.parse("text/plain"));
        RequestBody loanUsedByNameRequestBody = RequestBody.create("", MediaType.parse("text/plain"));
        RequestBody loanUsedByCNICRequestBody = RequestBody.create("", MediaType.parse("text/plain"));
        RequestBody purposeOfLoanRequestBody = RequestBody.create(Objects.requireNonNull(LoanPurpose).trim(), MediaType.parse("text/plain"));
        int selectedIncomeId = radioGroupSourceOfIncome.getCheckedRadioButtonId();
        if (selectedIncomeId != -1) {
            RadioButton selectedRb = findViewById(selectedIncomeId);
            SourceOfIncome = selectedRb.getText().toString();
        } else {
            SourceOfIncome = "";
        }

        RequestBody sourceOfIncomeRequestBody = RequestBody.create(SourceOfIncome, MediaType.parse("text/plain"));
        String borrowerNo = sharedManager.getString("BORROWER_NO", "");
        RequestBody borrowerNumberRequestBody = RequestBody.create(borrowerNo, MediaType.parse("text/plain"));
        if (Constants.CLIENT_ID == 0) {
            Constants.CLIENT_ID = SharedManager.getInstance(this).getInt("CLIENT_ID", 0);
        }

        // DEBUG: Show the exact CLIENT_ID we are about to send
        //  android.widget.Toast.makeText(this, "DEBUG CLIENT_ID: " + Constants.CLIENT_ID, android.widget.Toast.LENGTH_LONG).show();

        RequestBody clientIDRequestBody = RequestBody.create(CommonFunctions.convertToString(Constants.CLIENT_ID), MediaType.parse("text/plain"));

        String phoneStr = etApplicantPhone != null && etApplicantPhone.getText() != null ? etApplicantPhone.getText().toString().trim().replace("-", "").replace(" ", "") : "";
        RequestBody phoneRequestBody = RequestBody.create(phoneStr, MediaType.parse("text/plain"));

        RequestBody dateOfBirthRequestBody = RequestBody.create(CommonFunctions.formatDateForServer(Objects.requireNonNull(applicantDOB)), MediaType.parse("text/plain"));
        RequestBody maritalStatusRequestBody = RequestBody.create(String.valueOf(isMarried), MediaType.parse("text/plain"));
        RequestBody bankBranchAddressRequestBody = RequestBody.create(Objects.requireNonNull(etBankBranch.getText()).toString().trim(), MediaType.parse("text/plain"));
        RequestBody natureOfBusinessRequestBody = RequestBody.create(NatureOfBusiness, MediaType.parse("text/plain"));
        RequestBody genderRequestBody = RequestBody.create(applicantGender != null ? applicantGender : "", MediaType.parse("text/plain"));
        RequestBody cnicExpiryRequestBody = RequestBody.create(CommonFunctions.formatDateForServer(applicantCnicExpiry != null ? applicantCnicExpiry : ""), MediaType.parse("text/plain"));
        RequestBody cnicIssueRequestBody = RequestBody.create(CommonFunctions.formatDateForServer(applicantCnicIssue != null ? applicantCnicIssue : ""), MediaType.parse("text/plain"));
        RequestBody cnicFrontRequestBody = RequestBody.create(cnicFrontBase64 != null ? cnicFrontBase64 : "", MediaType.parse("text/plain"));
        RequestBody cnicBackRequestBody = RequestBody.create(cnicBackBase64 != null ? cnicBackBase64 : "", MediaType.parse("text/plain"));
        // FIX: profilePictureBase64 is preloaded from SharedManager and can hold the server path
        // (e.g. "/uploads/profile_pictures/12_x.jpg") instead of base64. The backend rejects that as
        // "not valid base64", so send it empty — the backend then keeps the existing picture.
        String profilePicToSend = profilePictureBase64 != null ? profilePictureBase64.trim() : "";
        if (profilePicToSend.startsWith("http://") || profilePicToSend.startsWith("https://") || profilePicToSend.startsWith("/uploads/") || profilePicToSend.startsWith("uploads/")) {
            profilePicToSend = "";
        }
        RequestBody profilePictureRequestBody = RequestBody.create(profilePicToSend, MediaType.parse("text/plain"));

        // Log the parameters (debug builds only — these lines contain CNIC, phone and bank account numbers)
        if (com.assan.finance.BuildConfig.DEBUG) {
        Log.d("USMAN_LOAN_FORM", "--- START API REQUEST LOGS ---");
        Log.d("USMAN_LOAN_FORM", "API Endpoint: " + com.assan.finance.apis.ApiManager.BASE_URL + "Loan/submit");
        Log.d("USMAN_LOAN_FORM", "Tehsil ID: " + selectedTehsilId);
        Log.d("USMAN_LOAN_FORM", "District ID: " + selectedDistrictId);
        Log.d("USMAN_LOAN_FORM", "Region ID: " + selectedRegionId);
        Log.d("USMAN_LOAN_FORM", "Field Unit ID: " + selectedFieldUnitId);
        Log.d("USMAN_LOAN_FORM", "How Old Business: " + etHowOldBusiness.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Present Address: " + etPresentAddress.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Permanent Address: " + etPermanentAddress.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Borrower No: " + borrowerNo);
        Log.d("USMAN_LOAN_FORM", "Bank Account Number: " + etBankAccNumber.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Bank Name: " + spinnerBankName.getSelectedItem().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Bank Branch Code: " + etBankBranchCode.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Loan Amount: " + etLoanAmount.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Occupation: " + selectedOccupation);
        Log.d("USMAN_LOAN_FORM", "Duration Of Loan: " + spinnerDurationOfLoan.getSelectedItem().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Business Address: " + etBusinessAddress.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Business City Name: " + businessCityToSend);
        Log.d("USMAN_LOAN_FORM", "Applicant Name: " + applicantName);
        Log.d("USMAN_LOAN_FORM", "Applicant CNIC: " + applicantCnic);
        Log.d("USMAN_LOAN_FORM", "Loan Used By Name: " + "");
        Log.d("USMAN_LOAN_FORM", "Loan Used By CNIC: " + "");
        Log.d("USMAN_LOAN_FORM", "Purpose Of Loan: " + LoanPurpose.trim());
        Log.d("USMAN_LOAN_FORM", "Source Of Income: " + SourceOfIncome);
        Log.d("USMAN_LOAN_FORM", "Client ID: " + Constants.CLIENT_ID);
        Log.d("USMAN_LOAN_FORM", "Date Of Birth: " + applicantDOB);
        Log.d("USMAN_LOAN_FORM", "Marital Status: " + isMarried);
        Log.d("USMAN_LOAN_FORM", "Bank Branch Address: " + etBankBranch.getText().toString().trim());
        Log.d("USMAN_LOAN_FORM", "Nature Of Business: " + NatureOfBusiness);
        Log.d("USMAN_LOAN_FORM", "Gender: " + applicantGender);
        Log.d("USMAN_LOAN_FORM", "CNIC Issue Date: " + applicantCnicIssue);
        Log.d("USMAN_LOAN_FORM", "CNIC Expiry Date: " + applicantCnicExpiry);
        Log.d("USMAN_LOAN_FORM", "Phone: " + phoneStr);
        Log.d("USMAN_LOAN_FORM", "Profile Picture Image Length: " + (profilePictureBase64 != null ? profilePictureBase64.length() : 0));
        Log.d("USMAN_LOAN_FORM", "CNIC Front Image Length: " + (cnicFrontBase64 != null ? cnicFrontBase64.length() : 0));
        Log.d("USMAN_LOAN_FORM", "CNIC Back Image Length: " + (cnicBackBase64 != null ? cnicBackBase64.length() : 0));
        Log.d("USMAN_LOAN_FORM", "--- END API REQUEST LOGS ---");
        }

        // Make the API call
        RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, this);
        ApiService apiService = retrofitClient.getApiService();
        Call<ResponseModelLoanApplication> call = apiService.submitLoanForm(
                tehsilIdRequestBody,
                districtIdRequestBody,
                regionIdRequestBody,
                fieldUnitIdRequestBody,
                profilePictureRequestBody,
                applicantNameRequestBody,
                applicantCNICRequestBody,
                loanUsedByNameRequestBody,
                loanUsedByCNICRequestBody,
                dateOfBirthRequestBody,
                maritalStatusRequestBody,
                presentAddressRequestBody,
                permanentAddressRequestBody,
                bankNameRequestBody,
                bankAccountNumberRequestBody,
                bankBranchCodeRequestBody,
                bankBranchAddressRequestBody,
                howOldBusinessRequestBody,
                businessAddressRequestBody,
                businessCityNameRequestBody,
                occupationRequestBody,
                purposeOfLoanRequestBody,
                loanAmountRequestBody,
                durationOfLoanRequestBody,
                clientIDRequestBody,
                borrowerNumberRequestBody,
                phoneRequestBody,
                sourceOfIncomeRequestBody,
                natureOfBusinessRequestBody,
                genderRequestBody,
                cnicExpiryRequestBody,
                cnicIssueRequestBody,
                cnicFrontRequestBody,
                cnicBackRequestBody
        );
        call.enqueue(new Callback<ResponseModelLoanApplication>() {
            @Override
            public void onResponse(@NonNull Call<ResponseModelLoanApplication> call, @NonNull Response<ResponseModelLoanApplication> response) {
                CommonFunctions.hideButtonLoader(submitLoanApplicationButton);
                if (response.isSuccessful() && response.body() != null) {
                    ResponseModelLoanApplication responseModel = response.body();
                    String status = responseModel.getStatus();
                    String message = responseModel.getMessage();
                    boolean success = responseModel.isSuccess() || 
                                      (status != null && (status.equalsIgnoreCase("Success") || status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("New"))) ||
                                      (message != null && message.toLowerCase().contains("submitted successfully"));
                    String tokenNumber = responseModel.getTokenNumber();

                    Log.d("USMAN_LOAN_FORM", "API Response Success: " + success + ", Message: " + message);

                    if (success) {
                        handleSuccessStatus(status, message, tokenNumber, responseModel.getProfilePicture());
                    } else {
                        // Sanitize backend messages to prevent technical jargon in the UI
                        String safeMessage = CommonFunctions.getProfessionalErrorMessage(message);

                        // Display the error returned by the API
                        CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", "Submission Error", safeMessage, false);
                        submitLoanApplicationButton.setEnabled(true);
                    }
                } else {
                    Log.e("USMAN_LOAN_FORM", "API Response Unsuccessful: Code " + response.code());
                    CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", "Submission Failed", "Something went wrong. Please try again.", false);
                    submitLoanApplicationButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseModelLoanApplication> call, @NonNull Throwable t) {
                Log.e("USMAN_LOAN_FORM", "API Request Failure: " + t.getMessage());
                CommonFunctions.hideButtonLoader(submitLoanApplicationButton);
                CommonFunctions.handleApiFailure(LoanApplicationActivity.this, parentLayout, t);
                submitLoanApplicationButton.setEnabled(true);
            }
        });
    }

    private void handleSuccessStatus(String status, String message, String tokenNumber, String savedProfilePicUrl) {
        // FIX: removed the phone-wide "is_repeat_loan_cycle" flag set here; it made Home show
        // "Repeat Cycle" for every user on this phone after any submission. Clear old values.
        getSharedPreferences("detect_prefs", MODE_PRIVATE).edit().remove("is_repeat_loan_cycle").apply();

        // FIX: a null message from the server produced "null\n\nYour Transaction Token..."
        String finalMessage = message != null ? message : "Your Financing Application has been submitted.";
        if (finalMessage.toLowerCase().contains("submitted successfully")) {
            finalMessage = "Your Financing Application Submitted Successfully, Now wait for approval.";
        }
        
        // Transaction token is hidden for now (not needed yet). Set SHOW_TRANSACTION_TOKEN to true to show it again.
        final boolean SHOW_TRANSACTION_TOKEN = false;
        if (SHOW_TRANSACTION_TOKEN && tokenNumber != null && !tokenNumber.isEmpty()) {
            finalMessage = finalMessage + "\n\nYour Transaction Token Number is:\n" + tokenNumber + "\n\nPlease show this at the branch.";
        }

        if (status == null) {
            status = "Success";
        }

        switch (status) {
            case "Sanction", "Disbursed", "Cancel", "Write-Off", "Rejected", "Suspended", "Disclosed" ->
                    CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "warning", "Already Applied", finalMessage, true);
            default -> {
                CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "success", "Financing Application", finalMessage, true);
            }
        }
        submitLoanApplicationButton.setEnabled(true);

        // Immediately update client profile picture in local cache so Home and Drawer show it
        // Prefer the server path returned by Loan/submit; fall back to the picked base64.
        if (!CommonFunctions.isNullOrEmpty(savedProfilePicUrl)) {
            sharedManager.saveString("PROFILE_PICTURE", savedProfilePicUrl);
        } else if (!CommonFunctions.isNullOrEmpty(profilePictureBase64)) {
            sharedManager.saveString("PROFILE_PICTURE", profilePictureBase64);
        }

        // FIX: removed the extra Profile/update upload that ran here. Loan/submit already saves
        // the picture, and the second upload could overwrite ASSAN_CLIENT.PROFILE_PICTURE.
    }

    private void syncProfilePictureToBackend(String cnic, String name, String phone, String dob, String profilePicBase64) {
        if (CommonFunctions.isNullOrEmpty(profilePicBase64) || CommonFunctions.isNullOrEmpty(cnic)) {
            return;
        }
        // If it's already an existing server file URL, no need to re-upload
        if (profilePicBase64.startsWith("http://") || profilePicBase64.startsWith("https://") || profilePicBase64.startsWith("/uploads/") || profilePicBase64.startsWith("uploads/")) {
            return;
        }

        try {
            RequestBody cnicBody = RequestBody.create(CommonFunctions.removeDashes(cnic), MediaType.parse("text/plain"));
            RequestBody nameBody = RequestBody.create(name != null ? name : "", MediaType.parse("text/plain"));
            RequestBody phoneBody = RequestBody.create(CommonFunctions.removeDashes(phone != null ? phone : ""), MediaType.parse("text/plain"));
            RequestBody dobBody = RequestBody.create(CommonFunctions.formatDateForServer(dob != null ? dob : ""), MediaType.parse("text/plain"));
            RequestBody picBody = RequestBody.create(profilePicBase64.trim(), MediaType.parse("text/plain"));
            RequestBody clientIdBody = RequestBody.create(String.valueOf(Constants.CLIENT_ID), MediaType.parse("text/plain"));

            RetrofitClient<ApiService> retrofitClient = new RetrofitClient<>(ApiService.class, this);
            ApiService apiService = retrofitClient.getApiService();
            Call<ResponseModelProfileUpdate> call = apiService.updateProfile(
                    nameBody, cnicBody, phoneBody, dobBody, picBody, clientIdBody
            );
            call.enqueue(new Callback<ResponseModelProfileUpdate>() {
                @Override
                public void onResponse(@NonNull Call<ResponseModelProfileUpdate> call, @NonNull Response<ResponseModelProfileUpdate> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ResponseModelProfileUpdate body = response.body();
                        Log.d("USMAN_LOAN_FORM", "Profile picture sync success: " + body.isSuccess());
                        if (body.getUpdatedRecord() != null && !CommonFunctions.isNullOrEmpty(body.getUpdatedRecord().getProfilePicture())) {
                            sharedManager.saveString("PROFILE_PICTURE", body.getUpdatedRecord().getProfilePicture());
                        }
                    } else {
                        Log.w("USMAN_LOAN_FORM", "Profile picture sync response code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseModelProfileUpdate> call, @NonNull Throwable t) {
                    Log.e("USMAN_LOAN_FORM", "Profile picture sync failed: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("USMAN_LOAN_FORM", "Error initiating profile picture sync", e);
        }
    }

    private void setupProfilePicturePicker() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        View layoutSection = findViewById(R.id.layoutProfilePictureSection);
        View framePicture = findViewById(R.id.frameProfilePicture);
        View tvTap = findViewById(R.id.tvTapToSelectPhoto);

        View.OnClickListener listener = v -> {
            pickingImageType = 2;
            CommonFunctions.showImageSourceDialog(this);
        };

        if (ivProfilePicture != null) ivProfilePicture.setOnClickListener(listener);
        if (layoutSection != null) layoutSection.setOnClickListener(listener);
        if (framePicture != null) framePicture.setOnClickListener(listener);
        if (tvTap != null) tvTap.setOnClickListener(listener);
    }


    private String saveImageToScopedStorage(Bitmap bitmap) {
        File imageFile = createImageFile();
        try {
            OutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
            Toast.makeText(this, R.string.auto_image_saved_to_scoped_sto_627, Toast.LENGTH_SHORT).show();

            currentImageName = imageFile.getName();
            currentBackImageName = imageFile.getName();
            return imageFile.getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File createImageFile() {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }

    private void compressBitmapToFile(Bitmap bitmap) {
        File file = new File(getExternalFilesDir(null), "compressed_image.jpg");
        try {
            // FileOutputStream works on every Android version (Files/Path need Android 8+, so this was null on 7.x)
            OutputStream os = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, os);
            os.flush();
            os.close();
            saveCompressedImageToScopedStorage(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populatePurposeOfLoanData(List<DataModelLoanData> loanDataList) {
        productNames.clear();
        minLoanAmountList.clear();
        maxLoanAmountList.clear();
        java.util.Set<String> uniqueProducts = new java.util.HashSet<>();
        uniqueProducts.add("Select Purpose of Financing");
        productNames.add("Select Purpose of Financing");
        minLoanAmountList.add("0");
        maxLoanAmountList.add("0");

        for (DataModelLoanData data : loanDataList) {
            String name = data.getProductName();
            if (name != null && !uniqueProducts.contains(name)) {
                uniqueProducts.add(name);
                productNames.add(name);
                minLoanAmountList.add(data.getLoanMinLimit());
                maxLoanAmountList.add(data.getLoanMaxLimit());
            }
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPurposeOfLoan.setTitle(getString(R.string.hint_select_purpose));
        spinnerPurposeOfLoan.setAdapter(adapter);
    }

    private void getLoanMinMax() {
        String cachedProducts = sharedManager.getString("CACHE_PRODUCTS", "");
        if (!cachedProducts.isEmpty()) {
            try {
                com.assan.finance.model.assan.ResponseModelAssanProducts responseData = new Gson().fromJson(cachedProducts, com.assan.finance.model.assan.ResponseModelAssanProducts.class);
                if (responseData != null && responseData.getSuccess()) {
                    List<DataModelLoanData> loanDataList = responseData.getRecords();
                    if (loanDataList != null) {
                        populatePurposeOfLoanData(loanDataList);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Call<com.assan.finance.model.assan.ResponseModelAssanProducts> call = com.assan.finance.network.assan.AssanRetrofitClient.INSTANCE.getApiService().getProducts();
        call.enqueue(new Callback<com.assan.finance.model.assan.ResponseModelAssanProducts>() {
            @Override
            public void onResponse(@NonNull Call<com.assan.finance.model.assan.ResponseModelAssanProducts> call, @NonNull Response<com.assan.finance.model.assan.ResponseModelAssanProducts> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    sharedManager.saveString("CACHE_PRODUCTS", new Gson().toJson(response.body()));
                    if (cachedProducts.isEmpty()) {
                        List<DataModelLoanData> loanDataList = response.body().getRecords();
                        if (loanDataList != null) {
                            populatePurposeOfLoanData(loanDataList);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.assan.finance.model.assan.ResponseModelAssanProducts> call, @NonNull Throwable t) {
                Log.e("MainActivity", "Failed to fetch data: " + t.getMessage());
                if (cachedProducts.isEmpty()) {
                    CommonFunctions.handleApiFailure(LoanApplicationActivity.this, parentLayout, t);
                }
            }
        });
    }

    private void showVoiceAssistantDialog() {
        voiceConversationHistory.clear();

        // Start only from what is already in the form (typed or CNIC-scanned). No profile fallbacks:
        // stale profile values (e.g. an old CNIC expiry date) were filled in and triggered "CNIC expired"
        // although the user had not entered a CNIC.
        voiceApplicantName = (etApplicantName != null && !etApplicantName.getText().toString().trim().isEmpty())
                ? etApplicantName.getText().toString().trim() : "";

        voiceApplicantCnic = (etApplicantCnic != null && !etApplicantCnic.getText().toString().trim().isEmpty())
                ? etApplicantCnic.getText().toString().trim() : "";

        voiceCnicExpiry = (etApplicantCnicExpiry != null && !etApplicantCnicExpiry.getText().toString().trim().isEmpty())
                ? etApplicantCnicExpiry.getText().toString().trim() : "";

        voiceCnicIssue = (etApplicantCnicIssue != null && !etApplicantCnicIssue.getText().toString().trim().isEmpty())
                ? etApplicantCnicIssue.getText().toString().trim() : "";

        voiceDob = (etApplicantDOB != null && !etApplicantDOB.getText().toString().trim().isEmpty())
                ? etApplicantDOB.getText().toString().trim() : "";

        voicePhone = (etApplicantPhone != null && !etApplicantPhone.getText().toString().trim().isEmpty())
                ? etApplicantPhone.getText().toString().trim() : "";

        if (radioGroupGender != null && radioGroupGender.getCheckedRadioButtonId() != -1) {
            int gId = radioGroupGender.getCheckedRadioButtonId();
            if (gId == R.id.radioButtonMale) voiceGender = "Male";
            else if (gId == R.id.radioButtonFemale) voiceGender = "Female";
            else voiceGender = "Other";
        } else {
            // FIX: profile stores GENDER as "1"/"2" (see HomeFragment), which the auto-fill treated
            // as "Other". Map it properly and never default to "Male".
            String g = ""; // not from the profile: only what is in the form or said in chat
            if (g == null) g = "";
            if (g.equals("1") || g.equalsIgnoreCase("M") || g.equalsIgnoreCase("Male")) voiceGender = "Male";
            else if (g.equals("2") || g.equalsIgnoreCase("F") || g.equalsIgnoreCase("Female")) voiceGender = "Female";
            else voiceGender = "";
        }

        if (radioGroupMaritalStatus != null && radioGroupMaritalStatus.getCheckedRadioButtonId() != -1) {
            int mId = radioGroupMaritalStatus.getCheckedRadioButtonId();
            voiceMaritalStatus = (mId == R.id.radioButtonMarried) ? "Married" : "Single";
        } else {
            String m = "";
            if (m == null) m = "";
            // FIX: unknown marital status stays empty so the assistant asks for it (was forced to "Single")
            voiceMaritalStatus = m.equalsIgnoreCase("Married") ? "Married" : (m.equalsIgnoreCase("Single") ? "Single" : "");
        }

        voicePresentAddress = (etPresentAddress != null && !etPresentAddress.getText().toString().trim().isEmpty())
                ? etPresentAddress.getText().toString().trim() : "";

        voicePermanentAddress = (etPermanentAddress != null && !etPermanentAddress.getText().toString().trim().isEmpty())
                ? etPermanentAddress.getText().toString().trim() : "";
        if (voicePermanentAddress.isEmpty()) voicePermanentAddress = voicePresentAddress;

        if (spinnerDistrictName != null && spinnerDistrictName.getSelectedItemPosition() > 0) {
            voiceDistrict = spinnerDistrictName.getSelectedItem().toString();
        } else {
            voiceDistrict = "";
        }

        if (spinnerTehsilName != null && spinnerTehsilName.getSelectedItemPosition() > 0) {
            voiceTehsil = spinnerTehsilName.getSelectedItem().toString();
        } else {
            voiceTehsil = "";
        }

        // FIX (critical): the assistant used to pre-fill a dummy bank (first in list), account
        // number "01234567890123", branch code "0101" and "<District> Branch". These passed
        // validation and could be submitted as real disbursement details. Only use real values.
        if (spinnerBankName != null && spinnerBankName.getSelectedItemPosition() > 0) {
            voiceBankName = spinnerBankName.getSelectedItem().toString();
        } else {
            voiceBankName = "";
        }

        if (etBankAccNumber != null && !etBankAccNumber.getText().toString().trim().isEmpty()) {
            voiceAccountNo = etBankAccNumber.getText().toString().trim();
        } else {
            String savedAcc = "";
            voiceAccountNo = savedAcc != null ? savedAcc : "";
        }

        if (etBankBranchCode != null && !etBankBranchCode.getText().toString().trim().isEmpty()) {
            voiceBranchCode = etBankBranchCode.getText().toString().trim();
        } else {
            voiceBranchCode = "";
        }

        if (etBankBranch != null && !etBankBranch.getText().toString().trim().isEmpty()) {
            voiceBranchAddress = etBankBranch.getText().toString().trim();
        } else {
            voiceBranchAddress = "";
        }

        if (spinnerPurposeOfLoan != null && spinnerPurposeOfLoan.getSelectedItemPosition() > 0) {
            voicePurposeOfLoan = spinnerPurposeOfLoan.getSelectedItem().toString();
        }

        if (radioGroupSourceOfIncome != null && radioGroupSourceOfIncome.getCheckedRadioButtonId() != -1) {
            voiceSourceOfIncome = (radioGroupSourceOfIncome.getCheckedRadioButtonId() == R.id.rbIncomeSalary) ? "Salary" : "Business";
        }

        if (etLoanAmount != null && !etLoanAmount.getText().toString().trim().isEmpty()) {
            voiceAmount = etLoanAmount.getText().toString().trim().replace(",", "");
        }

        if (spinnerDurationOfLoan != null && spinnerDurationOfLoan.getSelectedItemPosition() > 0) {
            voiceDuration = spinnerDurationOfLoan.getSelectedItem().toString();
        }

        if (etHowOldBusiness != null && !etHowOldBusiness.getText().toString().trim().isEmpty()) {
            voiceBusinessDuration = etHowOldBusiness.getText().toString().trim();
        }

        // FIX: no more invented business address ("Main Market") or city — ask the user instead.
        if (etBusinessAddress != null && !etBusinessAddress.getText().toString().trim().isEmpty()) {
            voiceBusinessAddress = etBusinessAddress.getText().toString().trim();
        } else {
            voiceBusinessAddress = "";
        }

        if (spinnerBusinessCityName != null && spinnerBusinessCityName.getSelectedItemPosition() > 0) {
            voiceBusinessCity = spinnerBusinessCityName.getSelectedItem().toString();
        } else {
            voiceBusinessCity = "";
        }

        if (rgOccupationType != null && rgOccupationType.getCheckedRadioButtonId() != -1) {
            voiceOccupationType = (rgOccupationType.getCheckedRadioButtonId() == R.id.rbAgriculture) ? "Agriculture" : "Non-Agriculture";
        }

        if (radioGroupNatureOfBusiness != null && radioGroupNatureOfBusiness.getCheckedRadioButtonId() != -1) {
            voiceNatureOfBusiness = radioGroupNatureOfBusiness.getCheckedRadioButtonId() == R.id.radioButtonNatureOfBusinessExisting ? "Existing" : "New";
        }

        String currentCnic = SharedManager.getInstance(this).getString("CNIC", "");
        String loanHistoryText = "No previous financing history found."; // In a real scenario, fetch from DB/API based on CNIC

        aiHelper = new com.assan.finance.utils.LoanAssistantAiHelper();
        aiHelper.initialize(loanHistoryText, districtNames, tehsilNames, bankNames, productNames);
        aiHelper.setProductLimits(buildProductLimitsText());

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loan_assistant, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.92);
            int height = (int) (metrics.heightPixels * 0.50f); // 50% height
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.5f);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        LinearLayout layoutChatLog = view.findViewById(R.id.layoutChatLog);
        ScrollView chatScrollView = view.findViewById(R.id.chatScrollView);
        TextView tvLiveStatusSummary = view.findViewById(R.id.tvLiveStatusSummary);
        ImageButton btnMic = view.findViewById(R.id.btnMic);
        btnMic.setVisibility(View.VISIBLE); // voice-to-voice is enabled again
        if (assistantVoicePlayer != null) assistantVoicePlayer.release();
        assistantVoicePlayer = new com.assan.finance.ai.VoiceReplyPlayer(this);
        if (assistantBeepMuter == null) assistantBeepMuter = new com.assan.finance.ai.ListeningSoundMuter(this);
        TextView tvMicInstructions = view.findViewById(R.id.tvMicInstructions);
        EditText etManualText = view.findViewById(R.id.etManualText);
        ImageButton btnSendText = view.findViewById(R.id.btnSendText);
        tvLiveStatusSummary.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        tvLiveStatusSummary.setFocusable(false);
        tvLiveStatusSummary.setFocusableInTouchMode(false);
        chatScrollView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        chatScrollView.setFocusable(false);
        chatScrollView.setFocusableInTouchMode(false);
        layoutChatLog.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        layoutChatLog.setFocusable(false);
        layoutChatLog.setFocusableInTouchMode(false);
        btnSendText.setFocusable(false);
        btnSendText.setFocusableInTouchMode(false);
        ImageView btnDismiss = view.findViewById(R.id.btnDismiss);

        android.widget.LinearLayout layoutInputText = view.findViewById(R.id.layoutInputText);
        android.widget.LinearLayout layoutVoiceRecording = view.findViewById(R.id.layoutVoiceRecording);
        ImageView ivRecordingDot = view.findViewById(R.id.ivRecordingDot);
        TextView tvVoiceStatus = view.findViewById(R.id.tvVoiceStatus);

        btnDismiss.setOnClickListener(v -> {
            if (speechRecognizer != null) speechRecognizer.destroy();
            dialog.dismiss();
        });

        // Report the latest assistant answer (typed or spoken) - Google Play AI content policy
        View btnReportAnswer = view.findViewById(R.id.btnReportAnswer);
        if (btnReportAnswer != null) {
            btnReportAnswer.setOnClickListener(v -> com.assan.finance.ai.AiContentReporter.showReportDialog(
                    this, com.assan.finance.ai.AiContentReporter.SOURCE_FORM, lastAssistantReply));
        }

        Runnable updateStatusSummary = () -> {
            StringBuilder sb = new StringBuilder("📋 Extracted Information:\n");
            int count = 0;
            if (!voiceAmount.isEmpty()) {
                sb.append("Amount: ").append(voiceAmount).append(" | ");
                count++;
            }
            if (!voicePurposeOfLoan.isEmpty()) {
                sb.append("Purpose: ").append(voicePurposeOfLoan).append(" | ");
                count++;
            }
            if (!voiceApplicantName.isEmpty()) {
                sb.append("Name: ").append(voiceApplicantName).append(" | ");
                count++;
            }
            if (!voiceApplicantCnic.isEmpty()) {
                sb.append("CNIC: ").append(voiceApplicantCnic).append(" | ");
                count++;
            }
            if (!voiceOccupationType.isEmpty()) {
                sb.append("Occ Type: ").append(voiceOccupationType).append(" | ");
                count++;
            }
            if (!voiceBankName.isEmpty()) {
                sb.append("Bank: ").append(voiceBankName).append(" | ");
                count++;
            }
            if (!voiceDistrict.isEmpty()) {
                sb.append("District: ").append(voiceDistrict).append(" | ");
                count++;
            }
            if (!voiceSourceOfIncome.isEmpty()) {
                sb.append("Income: ").append(voiceSourceOfIncome).append(" | ");
                count++;
            }
            if (!voiceDuration.isEmpty()) {
                sb.append("Duration: ").append(voiceDuration).append(" | ");
                count++;
            }

            if (count == 0) {
                sb.append("Assistant is listening...");
            } else {
                sb.setLength(sb.length() - 3);
            }
            tvLiveStatusSummary.setText(sb.toString());
        };

        updateStatusSummary.run();

        // ═══════════════════════════════════════════════════
        //  VOICE MODE STATE MACHINE
        // ═══════════════════════════════════════════════════
        final boolean[] isVoiceModeActive = {false};
        assistantVoiceModeRef = isVoiceModeActive;
        final boolean[] userHasSpoken = {false};
        final String[] lastPartialResult = {""};
        final long SILENCE_TIMEOUT_MS = 2000; // numbers (CNIC, phone) are dictated in groups with pauses

        final android.os.Handler silenceHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // Create recognizer intent
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L);
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, new ArrayList<>(java.util.Arrays.asList("ur-PK", "en-US")));
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED);
        }

        // Token of the current recognizer: callbacks from a destroyed one (late error 11/5) are ignored
        final int[] recogToken = {0};

        // ── Helper: destroy and recreate recognizer ──
        Runnable destroyRecognizer = () -> {
            recogToken[0]++;
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.stopListening();
                    speechRecognizer.cancel();
                    speechRecognizer.destroy();
                }
            } catch (Exception ignored) {
            }
            speechRecognizer = null;
        };

        // ── Helper: start blinking dot ──
        Runnable startDotBlink = () -> {
            ivRecordingDot.clearAnimation();
            android.view.animation.AlphaAnimation blinkAnimation = new android.view.animation.AlphaAnimation(1.0f, 0.0f);
            blinkAnimation.setDuration(500);
            blinkAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
            blinkAnimation.setRepeatCount(android.view.animation.Animation.INFINITE);
            blinkAnimation.setRepeatMode(android.view.animation.Animation.REVERSE);
            ivRecordingDot.startAnimation(blinkAnimation);
        };

        // Forward-declared runnables (using arrays for lambda capture)
        final Runnable[] startListeningRef = new Runnable[1];
        final int[] emptyListens = {0}; // listens that ended without speech (recognizer timeout / no match)
        final long[] silenceSince = {0L}; // when the current stretch of silence began
        final long MAX_SILENCE_MS = 20000; // ChatGPT-like: keep listening through pauses, give up after 20 s of silence
        final Runnable[] listenAgainOrStopRef = new Runnable[1];
        final Runnable[] finishListeningRef = new Runnable[1];
        final Runnable silenceRunnable = () -> {
            if (isVoiceModeActive[0] && userHasSpoken[0]) {
                finishListeningRef[0].run();
            }
        };

        // ── finishListening: stop recognizer, process speech ──
        finishListeningRef[0] = () -> {
            silenceHandler.removeCallbacksAndMessages(null);
            destroyRecognizer.run();

            assistantBeepMuter.restore(); // the answer will be spoken: sound back on
            // Urdu digits (۱۲۳) from the recogniser -> 0-9 so CNIC / phone numbers are understood
            String text = com.assan.finance.ai.AsaanAiEngine.INSTANCE.normalizeSpokenText(lastPartialResult[0]).trim();
            if (text.isEmpty()) {
                listenAgainOrStopRef[0].run();
                return;
            }
            emptyListens[0] = 0;
            silenceSince[0] = 0L;

            // STATE: Thinking
            tvVoiceStatus.setText(getString(R.string.msg_thinking));
            ivRecordingDot.clearAnimation();

            // Process through AI (voice mode = true → no chat bubble, AI speaks result)
            processAssistantInputText(text, layoutChatLog, chatScrollView, dialog, updateStatusSummary, true, layoutVoiceRecording, ivRecordingDot, tvVoiceStatus, etManualText);
        };

        // ── startListening: create fresh recognizer, start ──
        startListeningRef[0] = () -> {
            if (!isVoiceModeActive[0]) return;

            userHasSpoken[0] = false;
            lastPartialResult[0] = "";
            silenceHandler.removeCallbacksAndMessages(null);

            tvVoiceStatus.setText(getString(R.string.msg_listening));
            startDotBlink.run();

            // FIX: reuse one recognizer while listening. Destroying + recreating it on every restart unbound
            // Google's speech service under the new instance (error 11 "disconnected"), so voice mode
            // switched off by itself. A new one is created only after it was destroyed (turn sent / exit).
            if (speechRecognizer != null) {
                try {
                    speechRecognizer.cancel();
                } catch (Exception ignored) {
                }
            } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(LoanApplicationActivity.this);
            final int myToken = recogToken[0];
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                }

                @Override
                public void onBeginningOfSpeech() {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    if (userHasSpoken[0]) {
                        silenceHandler.removeCallbacks(silenceRunnable);
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    if (rmsdB > 2.0f && userHasSpoken[0]) {
                        silenceHandler.removeCallbacks(silenceRunnable);
                        silenceHandler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS);
                    }
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                }

                @Override
                public void onEndOfSpeech() {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    if (userHasSpoken[0]) {
                        silenceHandler.removeCallbacks(silenceRunnable);
                        silenceHandler.postDelayed(silenceRunnable, 500);
                    }
                }

                @Override
                public void onError(int error) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    silenceHandler.removeCallbacks(silenceRunnable);
                    if (!isVoiceModeActive[0]) return;

                    if (lastPartialResult[0].trim().length() > 0 && userHasSpoken[0]) {
                        finishListeningRef[0].run();
                    } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        emptyListens[0] = 99;
                        listenAgainOrStopRef[0].run();
                    } else {
                        // FIX: no-match / timeout / busy used to switch voice mode off instantly
                        // (it looked like the mic "closed by itself"). Keep listening instead.
                        listenAgainOrStopRef[0].run();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    silenceHandler.removeCallbacks(silenceRunnable);
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty() && !matches.get(0).trim().isEmpty()) {
                        lastPartialResult[0] = matches.get(0);
                        userHasSpoken[0] = true;
                    }
                    if (!lastPartialResult[0].trim().isEmpty()) {
                        finishListeningRef[0].run();
                    } else if (isVoiceModeActive[0]) {
                        listenAgainOrStopRef[0].run();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                    ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty() && !partial.get(0).isEmpty()) {
                        lastPartialResult[0] = partial.get(0);
                        userHasSpoken[0] = true;
                        emptyListens[0] = 0;
                        silenceSince[0] = 0L;
                        // DO NOT show text anywhere — voice mode is invisible

                        silenceHandler.removeCallbacks(silenceRunnable);
                        silenceHandler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS);
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    if (myToken != recogToken[0]) return; // stale recognizer
                }
            });

            }
            assistantBeepMuter.mute(); // silence the recognizer's start/stop tone while listening
            speechRecognizer.startListening(recognizerIntent);
        };

        // Silence / no match: listen again; only after ~4 silent tries (long silence) go back to typing
        listenAgainOrStopRef[0] = () -> {
            if (!isVoiceModeActive[0]) return;
            emptyListens[0]++;
            if (silenceSince[0] == 0L) silenceSince[0] = System.currentTimeMillis();
            boolean fatal = emptyListens[0] >= 99;
            if (!fatal && System.currentTimeMillis() - silenceSince[0] < MAX_SILENCE_MS) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isVoiceModeActive[0] && dialog.isShowing()
                            && (assistantVoicePlayer == null || !assistantVoicePlayer.isSpeaking())) {
                        startListeningRef[0].run();
                    }
                }, 700); // a short gap: recreating the recognizer instantly made the speech service disconnect
            } else {
                emptyListens[0] = 0;
                silenceSince[0] = 0L;
                assistantBeepMuter.restore();
                isVoiceModeActive[0] = false;
                silenceHandler.removeCallbacksAndMessages(null);
                destroyRecognizer.run();
                layoutVoiceRecording.setVisibility(View.GONE);
                layoutInputText.setVisibility(View.VISIBLE);
                btnSendText.setImageResource(R.drawable.ic_arrow_right);
                ivRecordingDot.clearAnimation();
            }
        };

        // ═══════════════════════════════════════════════════
        //  TTS → auto-restart listening after AI finishes speaking
        // ═══════════════════════════════════════════════════
        // Voice-to-voice loop like ChatGPT voice: after the assistant speaks, listen for the next answer
        onTtsDoneRunnable = () -> {
            if (isVoiceModeActive[0] && dialog.isShowing()) {
                silenceSince[0] = 0L; // silence is counted from the end of the assistant's answer
                startListeningRef[0].run();
            }
        };

        // Leaves voice mode and returns to typing
        final Runnable exitVoiceMode = () -> {
            isVoiceModeActive[0] = false;
            silenceHandler.removeCallbacksAndMessages(null);
            destroyRecognizer.run();
            assistantBeepMuter.restore();
            if (assistantVoicePlayer != null) assistantVoicePlayer.stop();
            layoutVoiceRecording.setVisibility(View.GONE);
            layoutInputText.setVisibility(View.VISIBLE);
            btnSendText.setImageResource(R.drawable.ic_arrow_right);
            ivRecordingDot.clearAnimation();
            btnSendText.setImageResource(R.drawable.ic_arrow_right);
        };

        // ═══════════════════════════════════════════════════
        //  BUTTON HANDLERS
        // ═══════════════════════════════════════════════════

        // ── Mic Button: enter voice mode ──
        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
                return;
            }

            isVoiceModeActive[0] = true;
            layoutInputText.setVisibility(View.GONE);
            layoutVoiceRecording.setVisibility(View.VISIBLE);
            btnSendText.setImageResource(R.drawable.ic_close); // tap to end the voice conversation
            emptyListens[0] = 0;
            silenceSince[0] = 0L;
            if (assistantVoicePlayer != null) assistantVoicePlayer.stop();
            startListeningRef[0].run();
        });


        // ── Tap on voice bar: interrupt AI speaking → start listening ──
        layoutVoiceRecording.setOnClickListener(v -> {
            if (assistantVoicePlayer != null && assistantVoicePlayer.isSpeaking()) {
                assistantVoicePlayer.stop();   // interrupt the assistant and speak again
                startListeningRef[0].run();
            }
        });

        // ═══════════════════════════════════════════════════
        //  TEXT MODE HANDLERS (completely separate)
        // ═══════════════════════════════════════════════════

        btnSendText.setOnClickListener(v -> {
            if (isVoiceModeActive[0]) {
                exitVoiceMode.run(); // in voice mode this button ends the voice conversation
            } else {
                String text = etManualText.getText().toString().trim();
                if (!text.isEmpty()) {
                    etManualText.setText("");
                    keepAssistantInputFocus(etManualText);
                    addUserChatBubble(layoutChatLog, text, chatScrollView, etManualText);
                    processAssistantInputText(text, layoutChatLog, chatScrollView, dialog, updateStatusSummary, false, layoutVoiceRecording, ivRecordingDot, tvVoiceStatus, etManualText);
                } else {
                    keepAssistantInputFocus(etManualText);
                }
            }
        });

        // ── Also send on keyboard "done"/"send" ──
        etManualText.setOnEditorActionListener((tv, actionId, event) -> {
            String text = etManualText.getText().toString().trim();
            if (!text.isEmpty()) {
                etManualText.setText("");
                keepAssistantInputFocus(etManualText);
                addUserChatBubble(layoutChatLog, text, chatScrollView, etManualText);
                processAssistantInputText(text, layoutChatLog, chatScrollView, dialog, updateStatusSummary, false, layoutVoiceRecording, ivRecordingDot, tvVoiceStatus, etManualText);
            } else {
                keepAssistantInputFocus(etManualText);
            }
            return true;
        });

        // ── Dismiss → cleanup & auto-fill ──
        btnDismiss.setOnClickListener(v -> {
            isVoiceModeActive[0] = false;
            silenceHandler.removeCallbacksAndMessages(null);
            destroyRecognizer.run();
            applyExtractedFieldsToForm();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            isVoiceModeActive[0] = false;
            silenceHandler.removeCallbacksAndMessages(null);
            destroyRecognizer.run();
            if (assistantBeepMuter != null) assistantBeepMuter.restore();
            if (assistantVoicePlayer != null) {
                assistantVoicePlayer.release();
                assistantVoicePlayer = null;
            }
            applyExtractedFieldsToForm();
        });

        dialog.setOnShowListener(d -> {
            keepAssistantInputFocus(etManualText);
            if (layoutChatLog.getChildCount() == 0) {
                String welcome;
                if (!voiceApplicantName.isEmpty() && !voiceAmount.isEmpty() && !voiceBankName.isEmpty()) {
                    welcome = "Salam " + voiceApplicantName + "! Main aap ka Asaan Financing Assistant hoon. Financing amount Rs. " + voiceAmount + " (" + voiceBankName + ") darj hai. Yeh financing aap kis maqsad ke liye lena chahte hain, ya koi detail tabdeel karni hai?";
                } else if (!voiceApplicantName.isEmpty()) {
                    welcome = "Salam " + voiceApplicantName + "! Main aap ka Asaan Financing Assistant hoon. Batayein ke aap ko kitni financing raqam darkar hai aur kis maqsad ke liye?";
                } else {
                    welcome = "Assalam-o-Alaikum! Main aap ka Asaan Financing Assistant hoon. Financing application ke liye batayein ke aap ko kitni raqam aur kis maqsad ke liye chahiye?";
                }
                addAssistantChatBubble(layoutChatLog, welcome, chatScrollView, etManualText);
            }
        });
        dialog.show();
        keepAssistantInputFocus(etManualText);
    }

    /** "Livestock: Rs. 10,000 - 750,000; Enterprise: ..." from the products loaded in the form. */
    private String buildProductLimitsText() {
        StringBuilder sb = new StringBuilder();
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        for (int i = 0; i < productNames.size(); i++) {
            String name = productNames.get(i);
            if (name == null || name.trim().isEmpty() || name.toLowerCase().startsWith("select")) continue;
            if (i >= minLoanAmountList.size() || i >= maxLoanAmountList.size()) continue;
            try {
                sb.append(name).append(": Rs. ")
                        .append(df.format(Double.parseDouble(minLoanAmountList.get(i)))).append(" - ")
                        .append(df.format(Double.parseDouble(maxLoanAmountList.get(i)))).append("; ");
            } catch (Exception ignored) {
            }
        }
        return sb.toString().trim();
    }

    private void addUserChatBubble(LinearLayout layoutChatLog, String text, ScrollView chatScrollView, EditText etManualText) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.END;
        params.setMargins(0, 0, 0, 8);
        tv.setLayoutParams(params);
        tv.setBackgroundResource(R.drawable.bg_pill_light_green);
        tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
        tv.setPadding(12, 8, 12, 8);
        tv.setTextColor(Color.parseColor("#1B5E20"));
        tv.setTextSize(13);
        tv.setText(text);
        tv.setFocusable(false);
        tv.setFocusableInTouchMode(false);
        layoutChatLog.addView(tv);
        chatScrollView.post(() -> {
            chatScrollView.fullScroll(View.FOCUS_DOWN);
            keepAssistantInputFocus(etManualText);
        });
    }

    private TextView addAssistantChatBubble(LinearLayout layoutChatLog, String text, ScrollView chatScrollView, EditText etManualText) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.START;
        params.setMargins(0, 0, 0, 8);
        tv.setLayoutParams(params);
        tv.setBackgroundResource(R.drawable.bg_pill_light_green);
        tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        tv.setPadding(12, 8, 12, 8);
        tv.setTextColor(Color.parseColor("#334155"));
        tv.setTextSize(13);
        tv.setText(text);
        tv.setFocusable(false);
        tv.setFocusableInTouchMode(false);
        // Long-press an answer to report it (Google Play AI content policy)
        tv.setOnLongClickListener(v -> {
            String shown = tv.getText().toString();
            if (!shown.equals(getString(R.string.str_thinking))) {
                com.assan.finance.ai.AiContentReporter.showReportDialog(
                        this, com.assan.finance.ai.AiContentReporter.SOURCE_FORM, shown);
            }
            return true;
        });
        layoutChatLog.addView(tv);
        chatScrollView.post(() -> {
            chatScrollView.fullScroll(View.FOCUS_DOWN);
            keepAssistantInputFocus(etManualText);
        });
        return tv;
    }

    private void keepAssistantInputFocus(EditText etManualText) {
        if (etManualText == null) return;
        etManualText.requestFocus();
        etManualText.post(() -> {
            etManualText.requestFocus();
            etManualText.setSelection(etManualText.getText().length());
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etManualText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
        etManualText.postDelayed(() -> {
            etManualText.requestFocus();
            etManualText.setSelection(etManualText.getText().length());
        }, 100);
    }

    private void processAssistantInputText(String text, LinearLayout layoutChatLog, ScrollView chatScrollView, android.app.Dialog assistantDialog, Runnable updateStatusSummary, boolean isVoiceMode, android.widget.LinearLayout layoutVoiceRecording, ImageView ivRecordingDot, TextView tvVoiceStatus, EditText etManualText) {
        if (aiHelper == null) return;

        // VOICE -> VOICE: no chat bubbles; the status bar shows "Thinking..." and the reply is spoken.
        // TEXT -> TEXT: a "Thinking..." bubble is replaced by the written reply.
        // Keep a reference to the indicator and remove exactly that view.
        final TextView typingIndicator = isVoiceMode ? null
                : addAssistantChatBubble(layoutChatLog, getString(R.string.str_thinking), chatScrollView, etManualText);

        // Convert List<com.google.gson.JsonObject> to List<org.json.JSONObject>
        List<org.json.JSONObject> history = new ArrayList<>();
        for (com.google.gson.JsonObject h : voiceConversationHistory) {
            try {
                history.add(new org.json.JSONObject(h.toString()));
            } catch (Exception ignored) {
            }
        }

        // Build currently extracted JSON
        org.json.JSONObject currentlyExtracted = new org.json.JSONObject();
        try {
            if (!voiceApplicantName.isEmpty())
                currentlyExtracted.put("applicant_name", voiceApplicantName);
            if (!voiceApplicantCnic.isEmpty())
                currentlyExtracted.put("applicant_cnic", voiceApplicantCnic);
            if (!voiceCnicExpiry.isEmpty()) currentlyExtracted.put("cnic_expiry", voiceCnicExpiry);
            if (!voiceDob.isEmpty()) currentlyExtracted.put("date_of_birth", voiceDob);
            if (!voicePhone.isEmpty()) currentlyExtracted.put("phone_number", voicePhone);
            if (!voiceGender.isEmpty()) currentlyExtracted.put("gender", voiceGender);
            if (!voiceMaritalStatus.isEmpty())
                currentlyExtracted.put("marital_status", voiceMaritalStatus);
            if (!voicePresentAddress.isEmpty())
                currentlyExtracted.put("present_address", voicePresentAddress);
            if (!voicePermanentAddress.isEmpty())
                currentlyExtracted.put("permanent_address", voicePermanentAddress);
            if (!voiceDistrict.isEmpty()) currentlyExtracted.put("district", voiceDistrict);
            if (!voiceTehsil.isEmpty()) currentlyExtracted.put("tehsil", voiceTehsil);
            if (!voiceBankName.isEmpty()) currentlyExtracted.put("bank_name", voiceBankName);
            if (!voiceAccountNo.isEmpty()) currentlyExtracted.put("account_number", voiceAccountNo);
            if (!voiceBranchCode.isEmpty()) currentlyExtracted.put("branch_code", voiceBranchCode);
            if (!voiceBranchAddress.isEmpty())
                currentlyExtracted.put("branch_address", voiceBranchAddress);
            if (!voicePurposeOfLoan.isEmpty())
                currentlyExtracted.put("purpose_of_loan", voicePurposeOfLoan);
            if (!voiceSourceOfIncome.isEmpty())
                currentlyExtracted.put("source_of_income", voiceSourceOfIncome);
            if (!voiceAmount.isEmpty()) currentlyExtracted.put("loan_amount", voiceAmount);
            if (!voiceDuration.isEmpty()) currentlyExtracted.put("loan_duration", voiceDuration);
            if (!voiceBusinessDuration.isEmpty())
                currentlyExtracted.put("business_duration_years", voiceBusinessDuration);
            if (!voiceBusinessAddress.isEmpty())
                currentlyExtracted.put("business_address", voiceBusinessAddress);
            if (!voiceBusinessCity.isEmpty())
                currentlyExtracted.put("business_city", voiceBusinessCity);
            if (!voiceOccupationType.isEmpty())
                currentlyExtracted.put("occupation_type", voiceOccupationType);
            if (!voiceSpecificOccupation.isEmpty())
                currentlyExtracted.put("specific_occupation", voiceSpecificOccupation);
            if (!voiceNatureOfBusiness.isEmpty())
                currentlyExtracted.put("nature_of_business", voiceNatureOfBusiness);
        } catch (Exception ignored) {
        }

        if (com.assan.finance.BuildConfig.DEBUG) Log.d("FormAssistant", (isVoiceMode ? "[VOICE]" : "[TEXT]") + " heard: " + text);
        aiHelper.processUserInputAsync(text, history, currentlyExtracted, isVoiceMode, new com.assan.finance.utils.LoanAssistantAiHelper.AiResultCallback() {
            @Override
            public void onResult(@NonNull org.json.JSONObject response) {
                if (com.assan.finance.BuildConfig.DEBUG) Log.d("FormAssistant", "reply: " + response.optString("ai_reply") + " | fields: " + response.optJSONObject("extracted_fields"));
                if (typingIndicator != null) layoutChatLog.removeView(typingIndicator);

                String aiReply = com.assan.finance.ai.AsaanAiEngine.INSTANCE.polishReply(
                        response.optString("ai_reply", "Maaf kijiye, dobara koshish karein."));
                boolean isComplete = response.optBoolean("is_complete", false);

                org.json.JSONObject extracted = response.optJSONObject("extracted_fields");
                if (extracted != null) {
                    if (extracted.has("applicant_name") && !extracted.optString("applicant_name").isEmpty())
                        voiceApplicantName = extracted.optString("applicant_name");
                    if (extracted.has("applicant_cnic") && !extracted.optString("applicant_cnic").isEmpty())
                        voiceApplicantCnic = extracted.optString("applicant_cnic");
                    if (extracted.has("cnic_expiry") && !extracted.optString("cnic_expiry").isEmpty())
                        voiceCnicExpiry = extracted.optString("cnic_expiry");
                    if (extracted.has("cnic_issue") && !extracted.optString("cnic_issue").isEmpty())
                        voiceCnicIssue = extracted.optString("cnic_issue");
                    if (extracted.has("date_of_birth") && !extracted.optString("date_of_birth").isEmpty())
                        voiceDob = extracted.optString("date_of_birth");
                    if (extracted.has("phone_number") && !extracted.optString("phone_number").isEmpty())
                        voicePhone = extracted.optString("phone_number");
                    if (extracted.has("gender") && !extracted.optString("gender").isEmpty())
                        voiceGender = extracted.optString("gender");
                    if (extracted.has("marital_status") && !extracted.optString("marital_status").isEmpty())
                        voiceMaritalStatus = extracted.optString("marital_status");
                    if (extracted.has("present_address") && !extracted.optString("present_address").isEmpty())
                        voicePresentAddress = extracted.optString("present_address");
                    if (extracted.has("permanent_address") && !extracted.optString("permanent_address").isEmpty())
                        voicePermanentAddress = extracted.optString("permanent_address");
                    if (extracted.has("district") && !extracted.optString("district").isEmpty()) {
                        String newVoiceDistrict = extracted.optString("district");
                        if (!voiceDistrict.equalsIgnoreCase(newVoiceDistrict)) {
                            voiceDistrict = newVoiceDistrict;
                            int districtIndex = districtNames.indexOf(voiceDistrict);
                            if (districtIndex >= 0 && districtIndex < districtIDs.size()) {
                                getAllTehsils(String.valueOf(districtIDs.get(districtIndex)));
                            }
                        }
                    }
                    if (extracted.has("tehsil") && !extracted.optString("tehsil").isEmpty())
                        voiceTehsil = extracted.optString("tehsil");
                    if (extracted.has("bank_name") && !extracted.optString("bank_name").isEmpty())
                        voiceBankName = extracted.optString("bank_name");
                    if (extracted.has("account_number") && !extracted.optString("account_number").isEmpty())
                        voiceAccountNo = extracted.optString("account_number");
                    if (extracted.has("branch_code") && !extracted.optString("branch_code").isEmpty())
                        voiceBranchCode = extracted.optString("branch_code");
                    if (extracted.has("branch_address") && !extracted.optString("branch_address").isEmpty())
                        voiceBranchAddress = extracted.optString("branch_address");
                    if (extracted.has("purpose_of_loan") && !extracted.optString("purpose_of_loan").isEmpty())
                        voicePurposeOfLoan = extracted.optString("purpose_of_loan");
                    if (extracted.has("source_of_income") && !extracted.optString("source_of_income").isEmpty())
                        voiceSourceOfIncome = extracted.optString("source_of_income");
                    if (extracted.has("loan_amount") && !extracted.optString("loan_amount").isEmpty())
                        voiceAmount = extracted.optString("loan_amount");
                    if (extracted.has("loan_duration") && !extracted.optString("loan_duration").isEmpty())
                        voiceDuration = extracted.optString("loan_duration");
                    if (extracted.has("business_duration_years") && !extracted.optString("business_duration_years").isEmpty())
                        voiceBusinessDuration = extracted.optString("business_duration_years");
                    if (extracted.has("business_address") && !extracted.optString("business_address").isEmpty())
                        voiceBusinessAddress = extracted.optString("business_address");
                    if (extracted.has("business_city") && !extracted.optString("business_city").isEmpty())
                        voiceBusinessCity = extracted.optString("business_city");
                    if (extracted.has("occupation_type") && !extracted.optString("occupation_type").isEmpty())
                        voiceOccupationType = extracted.optString("occupation_type");
                    if (extracted.has("specific_occupation") && !extracted.optString("specific_occupation").isEmpty())
                        voiceSpecificOccupation = extracted.optString("specific_occupation");
                    if (extracted.has("nature_of_business") && !extracted.optString("nature_of_business").isEmpty())
                        voiceNatureOfBusiness = extracted.optString("nature_of_business");
                }

                updateStatusSummary.run();
                applyExtractedFieldsToForm();

                lastAssistantReply = aiReply;
                boolean stillInVoiceMode = isVoiceMode && assistantVoiceModeRef != null && assistantVoiceModeRef[0];
                if (stillInVoiceMode && assistantVoicePlayer != null && assistantDialog != null && assistantDialog.isShowing()) {
                    // VOICE -> VOICE: speak the reply only (no text bubble)
                    assistantVoicePlayer.speak(aiReply, new com.assan.finance.ai.VoiceReplyPlayer.Listener() {
                        @Override
                        public void onSpeakingStarted() {
                            tvVoiceStatus.setText(getString(R.string.msg_speaking));
                            ivRecordingDot.clearAnimation();
                        }

                        @Override
                        public void onSpeakingDone() {
                            if (onTtsDoneRunnable != null) onTtsDoneRunnable.run();
                        }
                    });
                } else {
                    // TEXT -> TEXT
                    addAssistantChatBubble(layoutChatLog, aiReply, chatScrollView, etManualText);
                    keepAssistantInputFocus(etManualText);
                }

                // Update conversation history
                com.google.gson.JsonObject userHist = new com.google.gson.JsonObject();
                userHist.addProperty("role", "user");
                userHist.addProperty("content", text);
                voiceConversationHistory.add(userHist);

                com.google.gson.JsonObject aiHist = new com.google.gson.JsonObject();
                aiHist.addProperty("role", "assistant");
                aiHist.addProperty("content", aiReply);
                voiceConversationHistory.add(aiHist);

                if (isComplete) {
                    applyExtractedFieldsToForm();
                    chatScrollView.postDelayed(() -> {
                        if (assistantDialog != null && assistantDialog.isShowing()) {
                            assistantDialog.dismiss();
                        }
                        applyExtractedFieldsToForm();
                        // FIX: the form is only filled with what the user actually provided — don't claim it is complete
                        Toast.makeText(LoanApplicationActivity.this, "Form filled with the details you provided. Please review and complete any remaining fields.", Toast.LENGTH_LONG).show();
                        if (SVParent != null && etLoanAmount != null) {
                            SVParent.post(() -> {
                                android.graphics.Rect rect = new android.graphics.Rect(0, 0, etLoanAmount.getWidth(), etLoanAmount.getHeight());
                                SVParent.offsetDescendantRectToMyCoords(etLoanAmount, rect);
                                SVParent.smoothScrollTo(0, Math.max(0, rect.top - 100));
                            });
                        }
                    }, 1500);
                }
            }
        });
    }

    private void showReviewAndEditDialog() {
        BottomSheetDialog reviewDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loan_review, null);
        reviewDialog.setContentView(view);

        EditText etReviewApplicantName = view.findViewById(R.id.etReviewApplicantName);
        EditText etReviewApplicantCnic = view.findViewById(R.id.etReviewApplicantCnic);
        EditText etReviewCnicExpiry = view.findViewById(R.id.etReviewCnicExpiry);
        EditText etReviewDob = view.findViewById(R.id.etReviewDob);
        EditText etReviewPhone = view.findViewById(R.id.etReviewPhone);
        EditText etReviewGender = view.findViewById(R.id.etReviewGender);
        EditText etReviewMaritalStatus = view.findViewById(R.id.etReviewMaritalStatus);
        EditText etReviewPresentAddress = view.findViewById(R.id.etReviewPresentAddress);
        EditText etReviewPermanentAddress = view.findViewById(R.id.etReviewPermanentAddress);
        EditText etReviewDistrict = view.findViewById(R.id.etReviewDistrict);
        EditText etReviewTehsil = view.findViewById(R.id.etReviewTehsil);
        EditText etReviewBankName = view.findViewById(R.id.etReviewBankName);
        EditText etReviewAccountNo = view.findViewById(R.id.etReviewAccountNo);
        EditText etReviewBranchCode = view.findViewById(R.id.etReviewBranchCode);
        EditText etReviewBranchAddress = view.findViewById(R.id.etReviewBranchAddress);
        EditText etReviewPurposeOfLoan = view.findViewById(R.id.etReviewPurposeOfLoan);
        EditText etReviewSourceOfIncome = view.findViewById(R.id.etReviewSourceOfIncome);
        EditText etReviewAmount = view.findViewById(R.id.etReviewAmount);
        EditText etReviewDuration = view.findViewById(R.id.etReviewDuration);
        EditText etReviewBusinessDuration = view.findViewById(R.id.etReviewBusinessDuration);
        EditText etReviewBusinessAddress = view.findViewById(R.id.etReviewBusinessAddress);
        EditText etReviewBusinessCity = view.findViewById(R.id.etReviewBusinessCity);
        EditText etReviewOccupationType = view.findViewById(R.id.etReviewOccupationType);
        EditText etReviewSpecificOccupation = view.findViewById(R.id.etReviewSpecificOccupation);

        com.google.android.material.button.MaterialButton btnCancelReview = view.findViewById(R.id.btnCancelReview);
        com.google.android.material.button.MaterialButton btnConfirmReview = view.findViewById(R.id.btnConfirmReview);

        // Pre-populate fields
        if (etReviewApplicantName != null)
            etReviewApplicantName.setText(voiceApplicantName.isEmpty() ? etApplicantName.getText().toString() : voiceApplicantName);
        if (etReviewApplicantCnic != null) {
            CommonFunctions.addCNICFormatting(etReviewApplicantCnic);
            etReviewApplicantCnic.setText(voiceApplicantCnic.isEmpty() ? etApplicantCnic.getText().toString() : voiceApplicantCnic);
        }
        if (etReviewCnicExpiry != null)
            etReviewCnicExpiry.setText(voiceCnicExpiry.isEmpty() ? etApplicantCnicExpiry.getText().toString() : voiceCnicExpiry);
        if (etReviewDob != null)
            etReviewDob.setText(voiceDob.isEmpty() ? etApplicantDOB.getText().toString() : voiceDob);
        if (etReviewPhone != null)
            etReviewPhone.setText(voicePhone.isEmpty() ? etApplicantPhone.getText().toString() : voicePhone);
        if (etReviewGender != null) etReviewGender.setText(voiceGender);
        if (etReviewMaritalStatus != null) etReviewMaritalStatus.setText(voiceMaritalStatus);
        if (etReviewPresentAddress != null)
            etReviewPresentAddress.setText(voicePresentAddress.isEmpty() ? etPresentAddress.getText().toString() : voicePresentAddress);
        if (etReviewPermanentAddress != null)
            etReviewPermanentAddress.setText(voicePermanentAddress.isEmpty() ? etPermanentAddress.getText().toString() : voicePermanentAddress);
        if (etReviewDistrict != null) etReviewDistrict.setText(voiceDistrict);
        if (etReviewTehsil != null) etReviewTehsil.setText(voiceTehsil);
        if (etReviewBankName != null) etReviewBankName.setText(voiceBankName);
        if (etReviewAccountNo != null) etReviewAccountNo.setText(voiceAccountNo);
        if (etReviewBranchCode != null) etReviewBranchCode.setText(voiceBranchCode);
        if (etReviewBranchAddress != null) etReviewBranchAddress.setText(voiceBranchAddress);
        if (etReviewPurposeOfLoan != null) etReviewPurposeOfLoan.setText(voicePurposeOfLoan);
        if (etReviewSourceOfIncome != null) etReviewSourceOfIncome.setText(voiceSourceOfIncome);
        if (etReviewAmount != null) etReviewAmount.setText(voiceAmount);
        if (etReviewDuration != null) etReviewDuration.setText(voiceDuration);
        if (etReviewBusinessDuration != null)
            etReviewBusinessDuration.setText(voiceBusinessDuration);
        if (etReviewBusinessAddress != null) etReviewBusinessAddress.setText(voiceBusinessAddress);
        if (etReviewBusinessCity != null) etReviewBusinessCity.setText(voiceBusinessCity);
        if (etReviewOccupationType != null) etReviewOccupationType.setText(voiceOccupationType);
        if (etReviewSpecificOccupation != null)
            etReviewSpecificOccupation.setText(voiceSpecificOccupation);

        btnCancelReview.setOnClickListener(v -> reviewDialog.dismiss());

        btnConfirmReview.setOnClickListener(v -> {
            // Apply changes to the main form
            String finalName = etReviewApplicantName != null ? etReviewApplicantName.getText().toString().trim() : "";
            String finalCnic = etReviewApplicantCnic != null ? etReviewApplicantCnic.getText().toString().trim() : "";
            String finalCnicExpiry = etReviewCnicExpiry != null ? etReviewCnicExpiry.getText().toString().trim() : "";
            String finalDob = etReviewDob != null ? etReviewDob.getText().toString().trim() : "";
            String finalPhone = etReviewPhone != null ? etReviewPhone.getText().toString().trim() : "";
            String finalGender = etReviewGender != null ? etReviewGender.getText().toString().trim() : "";
            String finalMaritalStatus = etReviewMaritalStatus != null ? etReviewMaritalStatus.getText().toString().trim() : "";
            String finalPresentAddr = etReviewPresentAddress != null ? etReviewPresentAddress.getText().toString().trim() : "";
            String finalPermanentAddr = etReviewPermanentAddress != null ? etReviewPermanentAddress.getText().toString().trim() : "";
            String finalDistrict = etReviewDistrict != null ? etReviewDistrict.getText().toString().trim() : "";
            String finalTehsil = etReviewTehsil != null ? etReviewTehsil.getText().toString().trim() : "";
            String finalBank = etReviewBankName != null ? etReviewBankName.getText().toString().trim() : "";
            String finalAccount = etReviewAccountNo != null ? etReviewAccountNo.getText().toString().trim() : "";
            String finalBranchCode = etReviewBranchCode != null ? etReviewBranchCode.getText().toString().trim() : "";
            String finalBranchAddr = etReviewBranchAddress != null ? etReviewBranchAddress.getText().toString().trim() : "";
            String finalPurpose = etReviewPurposeOfLoan != null ? etReviewPurposeOfLoan.getText().toString().trim() : "";
            String finalSourceOfIncome = etReviewSourceOfIncome != null ? etReviewSourceOfIncome.getText().toString().trim() : "";
            String finalAmount = etReviewAmount != null ? etReviewAmount.getText().toString().trim() : "";
            String finalDuration = etReviewDuration != null ? etReviewDuration.getText().toString().trim() : "";
            String finalBusinessDuration = etReviewBusinessDuration != null ? etReviewBusinessDuration.getText().toString().trim() : "";
            String finalBusinessAddr = etReviewBusinessAddress != null ? etReviewBusinessAddress.getText().toString().trim() : "";
            String finalBusinessCity = etReviewBusinessCity != null ? etReviewBusinessCity.getText().toString().trim() : "";
            String finalOccupationType = etReviewOccupationType != null ? etReviewOccupationType.getText().toString().trim() : "";
            String finalSpecificOcc = etReviewSpecificOccupation != null ? etReviewSpecificOccupation.getText().toString().trim() : "";

            if (etApplicantName != null) etApplicantName.setText(finalName);
            if (etApplicantCnic != null) etApplicantCnic.setText(finalCnic);
            if (etApplicantCnicExpiry != null) etApplicantCnicExpiry.setText(finalCnicExpiry);
            if (etApplicantDOB != null) etApplicantDOB.setText(finalDob);
            if (etApplicantPhone != null) etApplicantPhone.setText(finalPhone);
            if (etPresentAddress != null) etPresentAddress.setText(finalPresentAddr);
            if (etPermanentAddress != null) etPermanentAddress.setText(finalPermanentAddr);
            if (etBankAccNumber != null) etBankAccNumber.setText(finalAccount);
            if (etBankBranchCode != null) etBankBranchCode.setText(finalBranchCode);
            if (etBankBranch != null) etBankBranch.setText(finalBranchAddr);
            if (etLoanAmount != null) etLoanAmount.setText(finalAmount);
            if (etHowOldBusiness != null) etHowOldBusiness.setText(finalBusinessDuration);
            if (etBusinessAddress != null) etBusinessAddress.setText(finalBusinessAddr);

            // Radio Groups using findViewById
            RadioGroup rgg = findViewById(R.id.radioGroupGender);
            if (rgg != null) {
                if (finalGender.equalsIgnoreCase("Male")) rgg.check(R.id.radioButtonMale);
                else if (finalGender.equalsIgnoreCase("Female")) rgg.check(R.id.radioButtonFemale);
                else if (finalGender.equalsIgnoreCase("Other")) rgg.check(R.id.radioButtonOther);
            }

            RadioGroup rgm = findViewById(R.id.radioGroupMaritalStatus);
            if (rgm != null) {
                if (finalMaritalStatus.equalsIgnoreCase("Single"))
                    rgm.check(R.id.radioButtonSingle);
                else if (finalMaritalStatus.equalsIgnoreCase("Married"))
                    rgm.check(R.id.radioButtonMarried);
            }

            RadioGroup rgs = findViewById(R.id.radioGroupSourceOfIncome);
            if (rgs != null) {
                if (finalSourceOfIncome.equalsIgnoreCase("Salary")) rgs.check(R.id.rbIncomeSalary);
                else if (finalSourceOfIncome.equalsIgnoreCase("Business"))
                    rgs.check(R.id.rbIncomeBusiness);
            }

            RadioGroup rgo = findViewById(R.id.rgOccupationType);
            RadioGroup rgAgri = findViewById(R.id.rgAgricultureOccupation);
            RadioGroup rgNonAgri = findViewById(R.id.rgNonAgricultureOccupation);

            if (rgo != null && finalOccupationType.equalsIgnoreCase("Agriculture")) {
                rgo.check(R.id.rbAgriculture);
                if (rgAgri != null) {
                    if (finalSpecificOcc.equalsIgnoreCase("Work on Family Farm"))
                        rgAgri.check(R.id.rbAgri1);
                    else if (finalSpecificOcc.equalsIgnoreCase("Work on Other Farm"))
                        rgAgri.check(R.id.rbAgri2);
                    else if (finalSpecificOcc.equalsIgnoreCase("Share Cropping on Farm"))
                        rgAgri.check(R.id.rbAgri3);
                    else if (finalSpecificOcc.equalsIgnoreCase("Keep other's Livestock"))
                        rgAgri.check(R.id.rbAgri4);
                    else if (finalSpecificOcc.equalsIgnoreCase("Fishing Business"))
                        rgAgri.check(R.id.rbAgri5);
                }
            } else if (rgo != null && finalOccupationType.equalsIgnoreCase("Non-Agriculture")) {
                rgo.check(R.id.rbNonAgriculture);
                if (rgNonAgri != null) {
                    if (finalSpecificOcc.equalsIgnoreCase("Food Processing & Vending"))
                        rgNonAgri.check(R.id.rbNonAgri1);
                    else if (finalSpecificOcc.equalsIgnoreCase("Trading / Buy and Sell"))
                        rgNonAgri.check(R.id.rbNonAgri2);
                    else if (finalSpecificOcc.equalsIgnoreCase("Handicrafts"))
                        rgNonAgri.check(R.id.rbNonAgri3);
                    else if (finalSpecificOcc.equalsIgnoreCase("Garments"))
                        rgNonAgri.check(R.id.rbNonAgri4);
                    else if (finalSpecificOcc.equalsIgnoreCase("Mechanic"))
                        rgNonAgri.check(R.id.rbNonAgri5);
                    else if (finalSpecificOcc.equalsIgnoreCase("Carpentry / Metal Work"))
                        rgNonAgri.check(R.id.rbNonAgri6);
                    else if (finalSpecificOcc.equalsIgnoreCase("Transport Service"))
                        rgNonAgri.check(R.id.rbNonAgri7);
                    else if (finalSpecificOcc.equalsIgnoreCase("Wage Earner / Construction"))
                        rgNonAgri.check(R.id.rbNonAgri8);
                    else if (finalSpecificOcc.equalsIgnoreCase("Office Work"))
                        rgNonAgri.check(R.id.rbNonAgri9);
                    else if (finalSpecificOcc.equalsIgnoreCase("Sewing"))
                        rgNonAgri.check(R.id.rbNonAgri10);
                    else if (finalSpecificOcc.equalsIgnoreCase("Government Employee"))
                        rgNonAgri.check(R.id.rbNonAgri11);
                    else if (finalSpecificOcc.equalsIgnoreCase("Private Job"))
                        rgNonAgri.check(R.id.rbNonAgri12);
                    else if (finalSpecificOcc.equalsIgnoreCase("House Wife"))
                        rgNonAgri.check(R.id.rbNonAgri13);
                }
            }

            // Spinners
            if (spinnerDistrictName != null && !finalDistrict.isEmpty() && spinnerDistrictName.getAdapter() != null) {
                pendingTehsilSelection = finalTehsil;
                for (int i = 0; i < spinnerDistrictName.getAdapter().getCount(); i++) {
                    if (spinnerDistrictName.getItemAtPosition(i).toString().equalsIgnoreCase(finalDistrict)) {
                        spinnerDistrictName.setSelection(i);
                        break;
                    }
                }
            } else if (spinnerTehsilName != null && !finalTehsil.isEmpty() && spinnerTehsilName.getAdapter() != null) {
                for (int i = 0; i < spinnerTehsilName.getAdapter().getCount(); i++) {
                    if (spinnerTehsilName.getItemAtPosition(i).toString().equalsIgnoreCase(finalTehsil)) {
                        spinnerTehsilName.setSelection(i);
                        break;
                    }
                }
            }

            if (spinnerBankName != null && !finalBank.isEmpty() && spinnerBankName.getAdapter() != null) {
                for (int i = 0; i < spinnerBankName.getAdapter().getCount(); i++) {
                    if (spinnerBankName.getItemAtPosition(i).toString().toLowerCase().contains(finalBank.toLowerCase())) {
                        spinnerBankName.setSelection(i);
                        break;
                    }
                }
            }

            if (spinnerPurposeOfLoan != null && !finalPurpose.isEmpty() && spinnerPurposeOfLoan.getAdapter() != null) {
                for (int i = 0; i < spinnerPurposeOfLoan.getAdapter().getCount(); i++) {
                    if (spinnerPurposeOfLoan.getItemAtPosition(i).toString().toLowerCase().contains(finalPurpose.toLowerCase())) {
                        spinnerPurposeOfLoan.setSelection(i);
                        break;
                    }
                }
            }

            if (spinnerDurationOfLoan != null && !finalDuration.isEmpty() && spinnerDurationOfLoan.getAdapter() != null) {
                for (int i = 0; i < spinnerDurationOfLoan.getAdapter().getCount(); i++) {
                    if (spinnerDurationOfLoan.getItemAtPosition(i).toString().contains(finalDuration)) {
                        spinnerDurationOfLoan.setSelection(i);
                        break;
                    }
                }
            }

            if (spinnerBusinessCityName != null && !finalBusinessCity.isEmpty() && spinnerBusinessCityName.getAdapter() != null) {
                for (int i = 0; i < spinnerBusinessCityName.getAdapter().getCount(); i++) {
                    if (spinnerBusinessCityName.getItemAtPosition(i).toString().equalsIgnoreCase(finalBusinessCity)) {
                        spinnerBusinessCityName.setSelection(i);
                        break;
                    }
                }
            }

            reviewDialog.dismiss();
            Toast.makeText(this, R.string.auto_loan_details_successfully_628, Toast.LENGTH_SHORT).show();
        });

        reviewDialog.show();
    }

    private int findMatchingProductIndex(String purpose) {
        if (productNames == null || productNames.isEmpty() || purpose == null || purpose.trim().isEmpty()) {
            return -1;
        }
        String pLower = purpose.toLowerCase().trim();

        // 1. Direct contains or equals match
        for (int i = 1; i < productNames.size(); i++) {
            String pName = productNames.get(i).toLowerCase();
            if (pName.equalsIgnoreCase(pLower) || pName.contains(pLower) || pLower.contains(pName)) {
                return i;
            }
        }

        // 2. Semantic keywords match
        // Karobar / Business / Shop / Enterprise / Trading
        if (pLower.contains("karobar") || pLower.contains("business") || pLower.contains("dukan") ||
                pLower.contains("shop") || pLower.contains("trading") || pLower.contains("tijarat") ||
                pLower.contains("enterprise") || pLower.contains("commercial")) {
            for (int i = 1; i < productNames.size(); i++) {
                String pName = productNames.get(i).toLowerCase();
                if (pName.contains("enterprise") || pName.contains("tajarat")) {
                    return i;
                }
            }
        }

        // Agriculture / Kheti / Zaraat
        if (pLower.contains("kheti") || pLower.contains("zaraat") || pLower.contains("agri") ||
                pLower.contains("fasal") || pLower.contains("crop") || pLower.contains("beej") ||
                pLower.contains("khad") || pLower.contains("kisan")) {
            for (int i = 1; i < productNames.size(); i++) {
                String pName = productNames.get(i).toLowerCase();
                if (pName.contains("agriculture input") || pName.contains("agriculture implement") || pName.contains("agri")) {
                    return i;
                }
            }
        }

        // Livestock / Maweshi
        if (pLower.contains("maweshi") || pLower.contains("livestock") || pLower.contains("bakri") ||
                pLower.contains("bhens") || pLower.contains("cattle") || pLower.contains("dairy") ||
                pLower.contains("janwar")) {
            for (int i = 1; i < productNames.size(); i++) {
                String pName = productNames.get(i).toLowerCase();
                if (pName.contains("livestock")) {
                    return i;
                }
            }
        }

        // House / Ghar
        if (pLower.contains("ghar") || pLower.contains("house") || pLower.contains("makan") ||
                pLower.contains("home") || pLower.contains("tameer")) {
            for (int i = 1; i < productNames.size(); i++) {
                String pName = productNames.get(i).toLowerCase();
                if (pName.contains("house improvement") || pName.contains("house")) {
                    return i;
                }
            }
        }

        // Solar
        if (pLower.contains("solar") || pLower.contains("tube well")) {
            for (int i = 1; i < productNames.size(); i++) {
                String pName = productNames.get(i).toLowerCase();
                if (pName.contains("solar")) {
                    return i;
                }
            }
        }

        // FIX: no match → don't silently pick the first product; let the user choose
        return -1;
    }

    private int findMatchingBankIndex(String bankName) {
        if (spinnerBankName == null || spinnerBankName.getAdapter() == null || bankName == null || bankName.trim().isEmpty()) {
            return -1;
        }
        String bLower = bankName.toLowerCase().trim();

        // 1. Direct contains or equals match
        for (int i = 1; i < spinnerBankName.getAdapter().getCount(); i++) {
            String item = spinnerBankName.getItemAtPosition(i).toString().toLowerCase();
            if (item.equalsIgnoreCase(bLower) || item.contains(bLower) || bLower.contains(item)) {
                return i;
            }
        }

        // 2. Acronym & alias matches
        String[][] bankKeywords = {
            {"meezan", "mizan"},
            {"albarka", "albaraka", "al baraka", "al-baraka"},
            {"habib", "hbl", "habib bank"},
            {"united", "ubl", "united bank"},
            {"mcb", "muslim commercial"},
            {"allied", "abl"},
            {"alfalah", "al falah", "al-falah"},
            {"faysal", "faisal"},
            {"askari"},
            {"national bank", "nbp"},
            {"punjab", "bop"},
            {"bankislami", "bank islami"},
            {"dubai islamic", "dib"},
            {"standard chartered", "scb"},
            {"js bank", "js"},
            {"soneri"},
            {"sindh bank", "sindh"},
            {"khushhali"},
            {"nrsp"},
            {"ztbl", "zarai taraqiati", "zarai"}
        };

        for (String[] group : bankKeywords) {
            boolean matchesInput = false;
            for (String kw : group) {
                if (bLower.contains(kw)) {
                    matchesInput = true;
                    break;
                }
            }
            if (matchesInput) {
                for (int i = 1; i < spinnerBankName.getAdapter().getCount(); i++) {
                    String item = spinnerBankName.getItemAtPosition(i).toString().toLowerCase();
                    for (String kw : group) {
                        if (item.contains(kw)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public void applyExtractedFieldsToForm() {
        isAutoFilling = true;
        try {
            // 1. Applicant Personal Details (fields locked by the CNIC scan are never overwritten)
            if (!CommonFunctions.isNullOrEmpty(voiceApplicantName) && etApplicantName != null && etApplicantName.isEnabled()) {
                etApplicantName.setText(voiceApplicantName);
            }
            if (!CommonFunctions.isNullOrEmpty(voiceApplicantCnic) && etApplicantCnic != null && etApplicantCnic.isEnabled()) {
                etApplicantCnic.setText(voiceApplicantCnic);
            }
            if (!CommonFunctions.isNullOrEmpty(voiceCnicExpiry) && etApplicantCnicExpiry != null && etApplicantCnicExpiry.isEnabled()) {
                etApplicantCnicExpiry.setText(CommonFunctions.formatDateOfBirth(voiceCnicExpiry));
            }
            if (!CommonFunctions.isNullOrEmpty(voiceCnicIssue) && etApplicantCnicIssue != null && etApplicantCnicIssue.isEnabled()) {
                etApplicantCnicIssue.setText(CommonFunctions.formatDateOfBirth(voiceCnicIssue));
            }
            if (!CommonFunctions.isNullOrEmpty(voiceDob) && etApplicantDOB != null && etApplicantDOB.isEnabled()) {
                etApplicantDOB.setText(CommonFunctions.formatDateOfBirth(voiceDob));
            }
            if (!CommonFunctions.isNullOrEmpty(voicePhone) && etApplicantPhone != null && etApplicantPhone.isEnabled()) {
                etApplicantPhone.setText(voicePhone);
            }
            if (radioGroupGender != null && !CommonFunctions.isNullOrEmpty(voiceGender)
                    && (radioGroupGender.getChildCount() == 0 || radioGroupGender.getChildAt(0).isEnabled())) {
                if (voiceGender.equalsIgnoreCase("Male") || voiceGender.equalsIgnoreCase("M")) {
                    radioGroupGender.check(R.id.radioButtonMale);
                    applicantGender = "M";
                } else if (voiceGender.equalsIgnoreCase("Female") || voiceGender.equalsIgnoreCase("F")) {
                    radioGroupGender.check(R.id.radioButtonFemale);
                    applicantGender = "F";
                } else {
                    radioGroupGender.check(R.id.radioButtonOther);
                    applicantGender = "O";
                }
            }
            if (radioGroupMaritalStatus != null && !CommonFunctions.isNullOrEmpty(voiceMaritalStatus)) {
                if (voiceMaritalStatus.equalsIgnoreCase("Married")) {
                    radioGroupMaritalStatus.check(R.id.radioButtonMarried);
                    isMarried = 1;
                } else {
                    radioGroupMaritalStatus.check(R.id.radioButtonSingle);
                    isMarried = 0;
                }
            }
            if (!CommonFunctions.isNullOrEmpty(voicePresentAddress) && etPresentAddress != null && etPresentAddress.isEnabled()) {
                etPresentAddress.setText(voicePresentAddress);
            }
            if (!CommonFunctions.isNullOrEmpty(voicePermanentAddress) && etPermanentAddress != null && etPermanentAddress.isEnabled()) {
                etPermanentAddress.setText(voicePermanentAddress);
            }

            // 2. District & Tehsil
            String targetDist = !CommonFunctions.isNullOrEmpty(voiceDistrict) ? voiceDistrict : "";
            String targetTehsil = !CommonFunctions.isNullOrEmpty(voiceTehsil) ? voiceTehsil : "";
            if (!CommonFunctions.isNullOrEmpty(targetTehsil)) {
                pendingTehsilSelection = targetTehsil;
            }
            if (!CommonFunctions.isNullOrEmpty(targetDist) && spinnerDistrictName != null && spinnerDistrictName.getAdapter() != null) {
                for (int i = 0; i < spinnerDistrictName.getAdapter().getCount(); i++) {
                    if (spinnerDistrictName.getItemAtPosition(i).toString().equalsIgnoreCase(targetDist)) {
                        spinnerDistrictName.setSelection(i);
                        break;
                    }
                }
            }
            // FIX (critical): removed the fallbacks that silently picked the FIRST district / tehsil /
            // bank, a dummy account number, branch code, branch address, business duration, nature
            // of business, income source, occupation and loan duration. The auto-fill also runs when
            // the assistant dialog is merely closed, so these invented values could be submitted.
            // Only values the user actually gave (or the form already had) are applied now.

            if (!CommonFunctions.isNullOrEmpty(targetTehsil) && spinnerTehsilName != null && spinnerTehsilName.getAdapter() != null) {
                for (int i = 0; i < spinnerTehsilName.getAdapter().getCount(); i++) {
                    if (spinnerTehsilName.getItemAtPosition(i).toString().equalsIgnoreCase(targetTehsil)) {
                        spinnerTehsilName.setSelection(i);
                        break;
                    }
                }
            }

            // 3. Bank & Account Details
            if (spinnerBankName != null && spinnerBankName.getAdapter() != null) {
                int matchedBankIndex = findMatchingBankIndex(voiceBankName);
                if (matchedBankIndex > 0 && matchedBankIndex != spinnerBankName.getSelectedItemPosition()) {
                    skipNextBankReset = true; // the async onItemSelected must not wipe the fields below
                    spinnerBankName.setSelection(matchedBankIndex);
                }
            }

            boolean bankChosen = spinnerBankName != null && spinnerBankName.getSelectedItemPosition() > 0;
            boolean bankWillBeChosen = bankChosen || skipNextBankReset;
            if (etBankAccNumber != null && bankWillBeChosen) {
                etBankAccNumber.setEnabled(true);
                View p = (View) etBankAccNumber.getParent();
                if (p != null) p.setEnabled(true);
                if (!CommonFunctions.isNullOrEmpty(voiceAccountNo)) {
                    etBankAccNumber.setText(voiceAccountNo);
                }
            }
            if (etBankBranchCode != null && bankWillBeChosen) {
                etBankBranchCode.setEnabled(true);
                View p = (View) etBankBranchCode.getParent();
                if (p != null) p.setEnabled(true);
                if (!CommonFunctions.isNullOrEmpty(voiceBranchCode)) {
                    etBankBranchCode.setText(voiceBranchCode);
                }
            }
            if (etBankBranch != null && bankWillBeChosen) {
                etBankBranch.setEnabled(true);
                View p = (View) etBankBranch.getParent();
                if (p != null) p.setEnabled(true);
                if (!CommonFunctions.isNullOrEmpty(voiceBranchAddress)) {
                    etBankBranch.setText(voiceBranchAddress);
                }
            }

            // 4. Purpose of Loan (Smart matching against productNames)
            if (!CommonFunctions.isNullOrEmpty(voicePurposeOfLoan) && spinnerPurposeOfLoan != null && spinnerPurposeOfLoan.getAdapter() != null) {
                int matchedProductIndex = findMatchingProductIndex(voicePurposeOfLoan);
                if (matchedProductIndex > 0) {
                    spinnerPurposeOfLoan.setSelection(matchedProductIndex);
                    LoanPurpose = productNames.get(matchedProductIndex);
                    if (llSourceOfIncome != null) llSourceOfIncome.setVisibility(View.VISIBLE);
                    if (matchedProductIndex < minLoanAmountList.size() && matchedProductIndex < maxLoanAmountList.size()) {
                        Constants.MINIMUM_LOAN_AMOUNT = CommonFunctions.convertToInteger(minLoanAmountList.get(matchedProductIndex));
                        Constants.MAXIMUM_LOAN_AMOUNT = CommonFunctions.convertToInteger(maxLoanAmountList.get(matchedProductIndex));
                        if (txtloanLimit != null) {
                            txtloanLimit.setVisibility(View.VISIBLE);
                            try {
                                java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
                                String minFormatted = df.format(Double.parseDouble(minLoanAmountList.get(matchedProductIndex)));
                                String maxFormatted = df.format(Double.parseDouble(maxLoanAmountList.get(matchedProductIndex)));
                                txtloanLimit.setText("PKR " + minFormatted + " - " + maxFormatted);
                            } catch (Exception ignored) {
                                txtloanLimit.setText("PKR " + minLoanAmountList.get(matchedProductIndex) + " - " + maxLoanAmountList.get(matchedProductIndex));
                            }
                        }
                    }
                }
            }

            // 5. Source of Income
            if (!CommonFunctions.isNullOrEmpty(voiceSourceOfIncome) && radioGroupSourceOfIncome != null) {
                if (llSourceOfIncome != null) llSourceOfIncome.setVisibility(View.VISIBLE);
                if (voiceSourceOfIncome.toLowerCase().contains("sal") || voiceSourceOfIncome.toLowerCase().contains("job") || voiceSourceOfIncome.toLowerCase().contains("naukri")) {
                    radioGroupSourceOfIncome.check(R.id.rbIncomeSalary);
                    SourceOfIncome = "Salary";
                } else {
                    radioGroupSourceOfIncome.check(R.id.rbIncomeBusiness);
                    SourceOfIncome = "Business";
                }
            } else if (spinnerPurposeOfLoan != null && spinnerPurposeOfLoan.getSelectedItemPosition() > 0) {
                if (llSourceOfIncome != null) llSourceOfIncome.setVisibility(View.VISIBLE);
            }

            // 6. Loan Amount
            if (!CommonFunctions.isNullOrEmpty(voiceAmount) && etLoanAmount != null) {
                String cleanAmount = voiceAmount.replaceAll("[^0-9]", "");
                if (!cleanAmount.isEmpty()) {
                    etLoanAmount.setText(cleanAmount);
                }
            }

            // 7. Loan Duration
            if (!CommonFunctions.isNullOrEmpty(voiceDuration) && spinnerDurationOfLoan != null && spinnerDurationOfLoan.getAdapter() != null) {
                String durLower = voiceDuration.toLowerCase();
                int durationPos = -1; // FIX: no silent default — only set when the user's answer matches
                if (durLower.matches(".*\\b18\\b.*") || durLower.contains("2 saal") || durLower.contains("2 year") || durLower.contains("dedh saal")) {
                    durationPos = 3; // "18 Months"
                } else if (durLower.matches(".*\\b12\\b.*") || durLower.contains("1 saal") || durLower.contains("1 year") || durLower.contains("ek saal")) {
                    durationPos = 2; // "12 Months"
                } else if (durLower.matches(".*\\b6\\b.*") || durLower.contains("che mahine") || durLower.contains("chhe mahine")) {
                    durationPos = 1; // "6 Months"
                }
                if (durationPos > 0) spinnerDurationOfLoan.setSelection(durationPos);
            }

            // 8. Nature of Business, Duration & City
            String nature = voiceNatureOfBusiness;
            if (CommonFunctions.isNullOrEmpty(nature) && !CommonFunctions.isNullOrEmpty(voiceBusinessDuration)) {
                String yrs = voiceBusinessDuration.replaceAll("[^0-9]", "");
                if (!yrs.isEmpty() && Integer.parseInt(yrs) > 0) nature = "Existing"; // running for years = existing business
            }
            if (!CommonFunctions.isNullOrEmpty(nature) && radioGroupNatureOfBusiness != null) {
                radioGroupNatureOfBusiness.check(nature.equalsIgnoreCase("New")
                        ? R.id.radioButtonNatureOfBusinessNew : R.id.radioButtonNatureOfBusinessExisting);
            }
            if (!CommonFunctions.isNullOrEmpty(voiceBusinessDuration) && etHowOldBusiness != null) {
                String years = voiceBusinessDuration.replaceAll("[^0-9]", "");
                if (!years.isEmpty()) etHowOldBusiness.setText(years);
            }
            if (!CommonFunctions.isNullOrEmpty(voiceBusinessAddress) && etBusinessAddress != null) {
                etBusinessAddress.setText(voiceBusinessAddress);
            }
            if (!CommonFunctions.isNullOrEmpty(voiceBusinessCity) && spinnerBusinessCityName != null && spinnerBusinessCityName.getAdapter() != null) {
                for (int i = 0; i < spinnerBusinessCityName.getAdapter().getCount(); i++) {
                    if (spinnerBusinessCityName.getItemAtPosition(i).toString().equalsIgnoreCase(voiceBusinessCity)) {
                        spinnerBusinessCityName.setSelection(i);
                        break;
                    }
                }
            }

            // 9. Occupation Type & Specific Occupation
            if (!CommonFunctions.isNullOrEmpty(voiceOccupationType) && rgOccupationType != null) {
                if (voiceOccupationType.equalsIgnoreCase("Agriculture")) {
                    rgOccupationType.check(R.id.rbAgriculture);
                    if (llAgricultureOptions != null) llAgricultureOptions.setVisibility(View.VISIBLE);
                    if (llNonAgricultureOptions != null) llNonAgricultureOptions.setVisibility(View.GONE);
                    if (rgAgricultureOccupation != null) {
                        int agriId = -1; // FIX: only tick a specific occupation the user actually mentioned
                        String spec = voiceSpecificOccupation.toLowerCase();
                        if (spec.contains("family farm") || spec.contains("own farm") || spec.contains("apni zameen")) agriId = R.id.rbAgri1;
                        else if (spec.contains("other farm")) agriId = R.id.rbAgri2;
                        else if (spec.contains("share")) agriId = R.id.rbAgri3;
                        else if (spec.contains("livestock") || spec.contains("maweshi") || spec.contains("cattle")) agriId = R.id.rbAgri4;
                        else if (spec.contains("fish")) agriId = R.id.rbAgri5;
                        if (agriId != -1) rgAgricultureOccupation.check(agriId);
                    }
                } else {
                    rgOccupationType.check(R.id.rbNonAgriculture);
                    if (llAgricultureOptions != null) llAgricultureOptions.setVisibility(View.GONE);
                    if (llNonAgricultureOptions != null) llNonAgricultureOptions.setVisibility(View.VISIBLE);
                    if (rgNonAgricultureOccupation != null) {
                        int nonAgriId = -1; // FIX: no default "Trading" when the user didn't say it
                        String spec = voiceSpecificOccupation.toLowerCase();
                        if (spec.contains("food")) nonAgriId = R.id.rbNonAgri1;
                        else if (spec.contains("trading") || spec.contains("buy") || spec.contains("karobar") || spec.contains("business") || spec.contains("dukan")) nonAgriId = R.id.rbNonAgri2;
                        else if (spec.contains("handicraft")) nonAgriId = R.id.rbNonAgri3;
                        else if (spec.contains("garment")) nonAgriId = R.id.rbNonAgri4;
                        else if (spec.contains("mechanic")) nonAgriId = R.id.rbNonAgri5;
                        else if (spec.contains("carpent")) nonAgriId = R.id.rbNonAgri6;
                        else if (spec.contains("transport")) nonAgriId = R.id.rbNonAgri7;
                        else if (spec.contains("wage") || spec.contains("construct")) nonAgriId = R.id.rbNonAgri8;
                        else if (spec.contains("office")) nonAgriId = R.id.rbNonAgri9;
                        else if (spec.contains("sewing")) nonAgriId = R.id.rbNonAgri10;
                        else if (spec.contains("govt")) nonAgriId = R.id.rbNonAgri11;
                        else if (spec.contains("job") || spec.contains("private") || spec.contains("salary")) nonAgriId = R.id.rbNonAgri12;
                        else if (spec.contains("wife") || spec.contains("house")) nonAgriId = R.id.rbNonAgri13;
                        if (nonAgriId != -1) rgNonAgricultureOccupation.check(nonAgriId);
                    }
                }
            }
            // FIX: removed the branch that guessed occupation type/occupation from the loan purpose.
        } catch (Exception e) {
            android.util.Log.e("LoanAutoFill", "Error auto-filling form: " + e.getMessage(), e);
        } finally {
            isAutoFilling = false;
        }
    }


    private void showFillMethodChoiceDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_fill_method_choice, null);
        builder.setView(view);
        android.app.AlertDialog choiceDialog = builder.create();
        if (choiceDialog.getWindow() != null) {
            choiceDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        com.google.android.material.button.MaterialButton btnVoiceChoice = view.findViewById(R.id.btnVoiceChoice);
        com.google.android.material.button.MaterialButton btnManualChoice = view.findViewById(R.id.btnManualChoice);

        btnVoiceChoice.setOnClickListener(v -> {
            choiceDialog.dismiss();
            showVoiceAssistantDialog();
        });

        btnManualChoice.setOnClickListener(v -> {
            choiceDialog.dismiss();
        });

        choiceDialog.show();
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> cnicScannerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String frontPath = data.getStringExtra("FRONT_IMAGE_PATH");
                    String backPath = data.getStringExtra("BACK_IMAGE_PATH");
                    String cnic = data.getStringExtra("CNIC_NUMBER");
                    String presentAddress = data.getStringExtra("PRESENT_ADDRESS");
                    String permanentAddr = data.getStringExtra("PERMANENT_ADDRESS");
                    String name = data.getStringExtra("NAME_EN");
                    String rawDob = data.getStringExtra("DATE_OF_BIRTH");
                    String dob = !CommonFunctions.isNullOrEmpty(rawDob) ? CommonFunctions.formatDateOfBirth(rawDob) : "";
                    String rawExpiry = data.getStringExtra("EXPIRY_DATE");
                    String expiry = !CommonFunctions.isNullOrEmpty(rawExpiry) ? CommonFunctions.formatDateOfBirth(rawExpiry) : "";

                    if (!CommonFunctions.isNullOrEmpty(expiry)) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                            java.util.Date expiryDate = sdf.parse(expiry);
                            java.util.Date currentDate = new java.util.Date();
                            
                            if (expiryDate != null) {
                                int requiredMonths = getRequiredCnicValidityMonths();
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.add(java.util.Calendar.MONTH, requiredMonths);
                                
                                if (expiryDate.before(cal.getTime())) {
                                    String msg = requiredMonths == 6 ? getString(R.string.error_cnic_expired_message_6) : getString(R.string.error_cnic_expired_message_12);
                                    CommonFunctions.showPremiumFeedbackDialog(LoanApplicationActivity.this, "error", getString(R.string.error_cnic_expired_title), msg, false);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


                    // Store scanned image paths + mark scan flags so validateInputFields() passes.
                    currentImagePath = frontPath;
                    currentImagePath1 = backPath;
                    tempFrontPath = frontPath != null ? frontPath : "";
                    tempBackPath = backPath != null ? backPath : "";
                    isFrontScanned = frontPath != null && !frontPath.isEmpty();
                    isBackScanned = backPath != null && !backPath.isEmpty();
                    
                    if (isFrontScanned) frontOcrDate = "";
                    if (isBackScanned) backOcrDate = "";

                    if (isFrontScanned) {
                        File frontFile = new File(frontPath);
                        if (frontFile.exists()) {
                            Uri frontUri = Uri.fromFile(frontFile);
                            if (ivCnicFront != null) ivCnicFront.setImageURI(frontUri);
                            cnicFrontUri = frontUri;
                            cnicFrontBase64 = CommonFunctions.convertToBase64(frontUri, LoanApplicationActivity.this);
                            View placeholder = findViewById(R.id.llCnicFrontPlaceholder);
                            if (placeholder != null) placeholder.setVisibility(View.GONE);
                            if (ivViewCnicFront != null) ivViewCnicFront.setVisibility(View.VISIBLE);
                            if (ivRotateCnicFront != null) ivRotateCnicFront.setVisibility(View.VISIBLE);
                        }
                    }

                    if (isBackScanned) {
                        File backFile = new File(backPath);
                        if (backFile.exists()) {
                            Uri backUri = Uri.fromFile(backFile);
                            if (ivCnicBack != null) ivCnicBack.setImageURI(backUri);
                            cnicBackUri = backUri;
                            cnicBackBase64 = CommonFunctions.convertToBase64(backUri, LoanApplicationActivity.this);
                            View placeholder = findViewById(R.id.llCnicBackPlaceholder);
                            if (placeholder != null) placeholder.setVisibility(View.GONE);
                            if (ivViewCnicBack != null) ivViewCnicBack.setVisibility(View.VISIBLE);
                            if (ivRotateCnicBack != null) ivRotateCnicBack.setVisibility(View.VISIBLE);
                        }
                    }

                    // Every field filled from the CNIC scan is locked so the user cannot change it.
                    // Fields the scan could not read stay editable so the user can type them.
                    if (!CommonFunctions.isNullOrEmpty(name)) {
                        etApplicantName.setText(name);
                        applicantName = name;
                        lockScannedField(etApplicantName);
                    }
                    if (!CommonFunctions.isNullOrEmpty(cnic)) {
                        etApplicantCnic.setText(cnic);
                        applicantCnic = cnic;
                        lockScannedField(etApplicantCnic);
                    }
                    if (!CommonFunctions.isNullOrEmpty(dob)) {
                        etApplicantDOB.setText(dob);
                        applicantDOB = dob;
                        lockScannedField(etApplicantDOB);
                        if (!CommonFunctions.isAgeAllowed(dob)) {
                            CommonFunctions.showAgeRestrictionDialog(LoanApplicationActivity.this, false, dob);
                        }
                    }
                    if (!CommonFunctions.isNullOrEmpty(expiry)) {
                        etApplicantCnicExpiry.setText(expiry);
                        applicantCnicExpiry = expiry;
                        lockScannedField(etApplicantCnicExpiry);
                    }

                    String rawIssue = data.getStringExtra("ISSUE_DATE");
                    String issue = !CommonFunctions.isNullOrEmpty(rawIssue) ? CommonFunctions.formatDateOfBirth(rawIssue) : "";
                    if (!CommonFunctions.isNullOrEmpty(issue) && etApplicantCnicIssue != null) {
                        etApplicantCnicIssue.setText(issue);
                        lockScannedField(etApplicantCnicIssue);
                    }

                    // Gender: from the scanned card, otherwise already set from the CNIC number's last digit
                    String scannedGender = data.getStringExtra("GENDER");
                    if (radioGroupGender != null) {
                        if (!CommonFunctions.isNullOrEmpty(scannedGender)) {
                            String g = scannedGender.trim().toUpperCase(java.util.Locale.ROOT);
                            if (g.startsWith("M")) radioGroupGender.check(R.id.radioButtonMale);
                            else if (g.startsWith("F")) radioGroupGender.check(R.id.radioButtonFemale);
                        }
                        if (radioGroupGender.getCheckedRadioButtonId() != -1
                                && (!CommonFunctions.isNullOrEmpty(scannedGender) || !CommonFunctions.isNullOrEmpty(cnic))) {
                            for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                                lockScannedField(radioGroupGender.getChildAt(i));
                            }
                        }
                    }

                    if (!CommonFunctions.isNullOrEmpty(presentAddress) && etPresentAddress != null) {
                        etPresentAddress.setText(presentAddress);
                        lockScannedField(etPresentAddress);
                    }
                    if (!CommonFunctions.isNullOrEmpty(permanentAddr) && etPermanentAddress != null) {
                        etPermanentAddress.setText(permanentAddr);
                        lockScannedField(etPermanentAddress);
                    }

                    if (isFrontScanned && frontPath != null && sharedManager != null) {
                        sharedManager.saveString("CACHED_CNIC_FRONT_PATH", frontPath);
                    }
                    if (isBackScanned && backPath != null && sharedManager != null) {
                        sharedManager.saveString("CACHED_CNIC_BACK_PATH", backPath);
                    }

                    if (data.getBooleanExtra("EXTRACTION_FAILED", false)) {
                        // Photos were captured but the AI service could not read them (quota / server issue)
                        Toast.makeText(this, "CNIC details could not be read. Please scan your CNIC again with clear pictures.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.auto_cnic_scanned_successfully_629, Toast.LENGTH_SHORT).show();
                    }

                    final TextView tvUnifiedScanStatus = findViewById(R.id.tvUnifiedScanStatus);
                    if (tvUnifiedScanStatus != null) {
                        tvUnifiedScanStatus.setVisibility(View.VISIBLE);
                        tvUnifiedScanStatus.setText(R.string.auto_scan_completed_626);
                        tvUnifiedScanStatus.setTextColor(Color.parseColor("#10B981"));
                    }
                }
            });

    private void showFullScreenImage(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) return;
        try {
            byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (decodedByte == null) return;

            final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.dialog_fullscreen_image);
            
            com.github.chrisbanes.photoview.PhotoView photoView = dialog.findViewById(R.id.photoView);
            ImageView ivClose = dialog.findViewById(R.id.ivClose);
            ImageView ivRotate = dialog.findViewById(R.id.ivRotateFullscreen);

            final Bitmap[] currentBitmap = new Bitmap[]{decodedByte};
            photoView.setImageBitmap(currentBitmap[0]);
            ivClose.setOnClickListener(v -> dialog.dismiss());

            if (ivRotate != null) {
                ivRotate.setOnClickListener(v -> {
                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.postRotate(90);
                    Bitmap rotated = Bitmap.createBitmap(currentBitmap[0], 0, 0, currentBitmap[0].getWidth(), currentBitmap[0].getHeight(), matrix, true);
                    currentBitmap[0] = rotated;
                    photoView.setImageBitmap(rotated);
                });
            }
            
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    
        
    private void showCnicImageSourceDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setContentView(R.layout.dialog_image_source);
        
        android.view.View cameraView = dialog.findViewById(R.id.openCamera);
        android.view.View galleryView = dialog.findViewById(R.id.openGallery);
        android.view.View cancelView = dialog.findViewById(R.id.btnCancelDialog);
        
        if (cancelView != null) {
            cancelView.setOnClickListener(v -> dialog.dismiss());
        }
        
        cameraView.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(LoanApplicationActivity.this, com.assan.finance.activities.CnicScannerActivity.class);
            cnicScannerLauncher.launch(intent);
        });
        
        galleryView.setOnClickListener(v -> {
            dialog.dismiss();
            com.assan.finance.utils.CommonFunctions.selectImageFromGallery(LoanApplicationActivity.this, REQUEST_IMAGE_CODE_GALLERY);
        });
        
        dialog.show();
    }

        private void triggerGeminiExtraction(Uri front, Uri back) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(LoanApplicationActivity.this);
        progressDialog.setMessage("Extracting CNIC details with AI...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        com.assan.finance.utils.CnicExtractionHelper.INSTANCE.extractWithGemini(
            this,
            front,
            back,
            jsonObject -> {
                if (jsonObject != null) {
                    try {
                        String cnicNumber = jsonObject.optString("cnicNumber", "");
                        String nameEn = jsonObject.optString("nameEn", "");
                        String dateOfBirth = jsonObject.optString("dateOfBirth", "");
                        String issueDate = jsonObject.optString("issueDate", "");
                        String expiryDate = jsonObject.optString("expiryDate", "");
                        String presentAddress = jsonObject.optString("presentAddress", "");
                        String permanentAddress = jsonObject.optString("permanentAddress", "");
                        String gender = jsonObject.optString("gender", "");

                        if (!cnicNumber.isEmpty() && etApplicantCnic != null) {
                            etApplicantCnic.setText(cnicNumber);
                            etApplicantCnic.setEnabled(false);
                            etApplicantCnic.setFocusable(false);
                            android.view.View parent = (android.view.View) etApplicantCnic.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        if (!nameEn.isEmpty() && etApplicantName != null) {
                            etApplicantName.setText(nameEn);
                            etApplicantName.setEnabled(false);
                            etApplicantName.setFocusable(false);
                            android.view.View parent = (android.view.View) etApplicantName.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        if (!dateOfBirth.isEmpty() && etApplicantDOB != null) {
                            String formatted = com.assan.finance.utils.CommonFunctions.formatDateOfBirth(dateOfBirth);
                            etApplicantDOB.setText(!formatted.isEmpty() ? formatted : dateOfBirth);
                            etApplicantDOB.setEnabled(false);
                            etApplicantDOB.setFocusable(false);
                            etApplicantDOB.setClickable(false);
                            android.view.View parent = (android.view.View) etApplicantDOB.getParent();
                            if (parent != null) parent.setEnabled(false);
                            if (!com.assan.finance.utils.CommonFunctions.isAgeAllowed(dateOfBirth)) {
                                com.assan.finance.utils.CommonFunctions.showAgeRestrictionDialog(LoanApplicationActivity.this, false, dateOfBirth);
                            }
                        }
                        if (!issueDate.isEmpty() && etApplicantCnicIssue != null) {
                            String formatted = com.assan.finance.utils.CommonFunctions.formatDateOfBirth(issueDate);
                            etApplicantCnicIssue.setText(!formatted.isEmpty() ? formatted : issueDate);
                            etApplicantCnicIssue.setEnabled(false);
                            etApplicantCnicIssue.setFocusable(false);
                            etApplicantCnicIssue.setClickable(false);
                            android.view.View parent = (android.view.View) etApplicantCnicIssue.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        if (!expiryDate.isEmpty() && etApplicantCnicExpiry != null) {
                            String formatted = com.assan.finance.utils.CommonFunctions.formatDateOfBirth(expiryDate);
                            etApplicantCnicExpiry.setText(!formatted.isEmpty() ? formatted : expiryDate);
                            etApplicantCnicExpiry.setEnabled(false);
                            etApplicantCnicExpiry.setFocusable(false);
                            etApplicantCnicExpiry.setClickable(false);
                            android.view.View parent = (android.view.View) etApplicantCnicExpiry.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        
                        if (!presentAddress.isEmpty() && etPresentAddress != null) {
                            etPresentAddress.setText(presentAddress);
                            etPresentAddress.setEnabled(false);
                            etPresentAddress.setFocusable(false);
                            android.view.View parent = (android.view.View) etPresentAddress.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        if (!permanentAddress.isEmpty() && etPermanentAddress != null) {
                            etPermanentAddress.setText(permanentAddress);
                            etPermanentAddress.setEnabled(false);
                            etPermanentAddress.setFocusable(false);
                            android.view.View parent = (android.view.View) etPermanentAddress.getParent();
                            if (parent != null) parent.setEnabled(false);
                        }
                        
                        if (gender != null && gender.trim().length() > 0 && radioGroupGender != null) {
                            String gUpper = gender.trim().toUpperCase();
                            if (gUpper.startsWith("M")) {
                                radioGroupGender.check(R.id.radioButtonMale);
                                for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                                    radioGroupGender.getChildAt(i).setEnabled(false);
                                }
                            } else if (gUpper.startsWith("F")) {
                                radioGroupGender.check(R.id.radioButtonFemale);
                                for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                                    radioGroupGender.getChildAt(i).setEnabled(false);
                                }
                            }
                        }
                        // Gender already set from the scanned CNIC number's last digit: lock it too
                        if (!cnicNumber.isEmpty() && radioGroupGender != null && radioGroupGender.getCheckedRadioButtonId() != -1) {
                            for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                                lockScannedField(radioGroupGender.getChildAt(i));
                            }
                        }
                        
                        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                        android.widget.Toast.makeText(LoanApplicationActivity.this, "CNIC details extracted successfully!", android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    }
                } else {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    android.widget.Toast.makeText(LoanApplicationActivity.this, "CNIC details could not be read. Please scan your CNIC again with clear pictures.", android.widget.Toast.LENGTH_LONG).show();
                }
                return kotlin.Unit.INSTANCE;
            },
            error -> {
                if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                android.widget.Toast.makeText(LoanApplicationActivity.this, error, android.widget.Toast.LENGTH_LONG).show();
                return kotlin.Unit.INSTANCE;
            }
        );
    }

    /**
     * Makes a field filled from the CNIC scan read-only: no typing, no date picker, no selection.
     * Its value is still read normally when the form is submitted.
     */
    // ===== TEMPORARY trial: Google ML Kit Document Scanner =====
    // Finds the card edges, crops away the background and straightens the card. The two scanned pages
    // (front, back) then go through the same Gemini extraction as the gallery path. Remove this block,
    // the button in activity_loan_application.xml and its listener if the trial is not kept.
    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> mlKitCnicScanLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult scan =
                        com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                if (scan == null || scan.getPages() == null || scan.getPages().size() < 2) {
                    Toast.makeText(this, "Please scan both sides of the CNIC: first the front, then the back.", Toast.LENGTH_LONG).show();
                    return;
                }
                Uri front = scan.getPages().get(0).getImageUri();
                Uri back = scan.getPages().get(1).getImageUri();
                showMlKitScannedSide(front, true);
                showMlKitScannedSide(back, false);
                runSilentOcrForExpiry(front, true);
                runSilentOcrForExpiry(back, false);
                triggerGeminiExtraction(cnicFrontUri, cnicBackUri);
            });

    private void startMlKitCnicScan() {
        com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions options =
                new com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(2)
                        .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build();
        Toast.makeText(this, "Scan the FRONT of your CNIC first, then the BACK.", Toast.LENGTH_LONG).show();
        com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
                .getStartScanIntent(this)
                .addOnSuccessListener(intentSender -> mlKitCnicScanLauncher.launch(
                        new androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Google document scanner is not available on this phone. Please use Scan CNIC instead.", Toast.LENGTH_LONG).show());
    }

    private void showMlKitScannedSide(Uri uri, boolean isFront) {
        ImageView target = isFront ? ivCnicFront : ivCnicBack;
        if (target != null) {
            target.setImageURI(null);
            target.setImageURI(uri);
        }
        String base64 = CommonFunctions.convertToBase64(uri, LoanApplicationActivity.this);
        if (isFront) {
            cnicFrontUri = uri;
            cnicFrontBase64 = base64;
            isFrontScanned = true;
        } else {
            cnicBackUri = uri;
            cnicBackBase64 = base64;
            isBackScanned = true;
        }
        View placeholder = findViewById(isFront ? R.id.llCnicFrontPlaceholder : R.id.llCnicBackPlaceholder);
        if (placeholder != null) placeholder.setVisibility(View.GONE);
        View viewIcon = isFront ? ivViewCnicFront : ivViewCnicBack;
        if (viewIcon != null) viewIcon.setVisibility(View.VISIBLE);
        View rotateIcon = isFront ? ivRotateCnicFront : ivRotateCnicBack;
        if (rotateIcon != null) rotateIcon.setVisibility(View.VISIBLE);
    }
    // ===== end of TEMPORARY trial =====

    /** Locks every CNIC-derived field at start; the CNIC scan (camera or gallery) is the only way to fill them. */
    private void lockCnicFieldsUntilScanned() {
        lockScannedField(etApplicantName);
        lockScannedField(etApplicantCnic);
        lockScannedField(etApplicantDOB);
        lockScannedField(etApplicantCnicIssue);
        lockScannedField(etApplicantCnicExpiry);
        lockScannedField(etPresentAddress);
        lockScannedField(etPermanentAddress);
        if (radioGroupGender != null) {
            for (int i = 0; i < radioGroupGender.getChildCount(); i++) {
                lockScannedField(radioGroupGender.getChildAt(i));
            }
        }
    }

    private static boolean isEmptyText(EditText field) {
        return field == null || field.getText() == null || field.getText().toString().trim().isEmpty();
    }

    private void lockScannedField(View field) {
        if (field == null) return;
        field.setEnabled(false);
        field.setFocusable(false);
        field.setFocusableInTouchMode(false);
        field.setClickable(false);
        field.setLongClickable(false);
        if (field.getParent() instanceof View) {
            ((View) field.getParent()).setEnabled(false);
        }
    }

    private void runSilentOcrForExpiry(Uri uri, boolean isFront) {
        if (isFront) frontOcrDate = ""; else backOcrDate = "";
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String fullText = visionText.getText();
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
                        java.util.regex.Matcher m = p.matcher(fullText);
                        java.util.List<java.util.Date> dates = new java.util.ArrayList<>();
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.ENGLISH);
                        while (m.find()) {
                            try {
                                java.util.Date d = sdf.parse(m.group());
                                if (d != null) dates.add(d);
                            } catch (Exception ignored) {}
                        }
                        if (!dates.isEmpty()) {
                            java.util.Collections.sort(dates);
                            java.util.Date maxDate = dates.get(dates.size() - 1);
                            
                            java.text.SimpleDateFormat uiSdf = new java.text.SimpleDateFormat(com.assan.finance.utils.Constants.UI_DATE_PATTERN, java.util.Locale.ENGLISH);
                            String newlyFound = uiSdf.format(maxDate);
                            
                            if (isFront) {
                                frontOcrDate = newlyFound;
                            } else {
                                backOcrDate = newlyFound;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("USMAN_LOAN_FORM", "Silent OCR failed", e);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CODE_CAMERA || requestCode == REQUEST_IMAGE_CODE_GALLERY) {
            if (pickingImageType == 2) {
                Bitmap directBitmap = null;
                if (data != null && data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                    directBitmap = (Bitmap) data.getExtras().get("data");
                }

                Uri selectedImageUri = CommonFunctions.handleImageActivityResultProfileImage(
                        requestCode, resultCode, data, this,
                        REQUEST_IMAGE_CODE_CAMERA, REQUEST_IMAGE_CODE_GALLERY, ivProfilePicture);

                if (selectedImageUri != null) {
                    ivProfilePicture.setImageURI(null);
                    ivProfilePicture.setImageURI(selectedImageUri);
                    profilePictureBase64 = CommonFunctions.convertToBase64(selectedImageUri, this);
                }

                // Fallback to direct in-memory Bitmap if URI conversion returned null or empty
                if (CommonFunctions.isNullOrEmpty(profilePictureBase64) && directBitmap != null) {
                    ivProfilePicture.setImageBitmap(directBitmap);
                    profilePictureBase64 = CommonFunctions.convertToBase64(directBitmap, this);
                }

                if (!CommonFunctions.isNullOrEmpty(profilePictureBase64)) {
                    Log.d("USMAN_LOAN_FORM", "Profile picture selected successfully. Base64 length: " + profilePictureBase64.length());
                    CommonFunctions.showToast(LoanApplicationActivity.this, "Profile picture selected successfully");
                } else {
                    Log.e("USMAN_LOAN_FORM", "Profile picture conversion failed!");
                    CommonFunctions.showToast(LoanApplicationActivity.this, "Could not load selected photo. Please try again.");
                }
            } else {
                ImageView targetView = isPickingCnicFront ? ivCnicFront : ivCnicBack;
                Uri selectedImageUri = CommonFunctions.handleImageActivityResultProfileImage(
                        requestCode, resultCode, data, this,
                        REQUEST_IMAGE_CODE_CAMERA, REQUEST_IMAGE_CODE_GALLERY, targetView);

                if (selectedImageUri != null) {
                    // Pre-rotate with EXIF so UCrop opens with the true upright orientation
                    Uri sourceForCrop = CnicRotationHelper.prepareForCrop(this, selectedImageUri);
                    Uri destinationUri = Uri.fromFile(new java.io.File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
                    
                    com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
                    options.setFreeStyleCropEnabled(true);
                    options.setShowCropGrid(true);
                    options.setShowCropFrame(true);
                    options.setHideBottomControls(false);
                    options.setAllowedGestures(com.yalantis.ucrop.UCropActivity.ALL, com.yalantis.ucrop.UCropActivity.ALL, com.yalantis.ucrop.UCropActivity.ALL);
                    options.setAspectRatioOptions(0,
                            new com.yalantis.ucrop.model.AspectRatio("CNIC Card (85:54)", 85.6f, 53.98f),
                            new com.yalantis.ucrop.model.AspectRatio("16:9", 16f, 9f),
                            new com.yalantis.ucrop.model.AspectRatio("Original", com.yalantis.ucrop.view.CropImageView.DEFAULT_ASPECT_RATIO, com.yalantis.ucrop.view.CropImageView.DEFAULT_ASPECT_RATIO)
                    );
                    
                    com.yalantis.ucrop.UCrop.of(sourceForCrop, destinationUri)
                            .withOptions(options)
                            .withMaxResultSize(1200, 1200)
                            .start(LoanApplicationActivity.this);
                }
            }
        } else if (requestCode == com.yalantis.ucrop.UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = com.yalantis.ucrop.UCrop.getOutput(data);
            if (resultUri != null) {
                final boolean pickingFront = isPickingCnicFront;
                android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(LoanApplicationActivity.this);
                progressDialog.setMessage("Straightening CNIC image...");
                progressDialog.setCancelable(false);
                try {
                    progressDialog.show();
                } catch (Exception ignored) {}

                CnicRotationHelper.straightenCnic(LoanApplicationActivity.this, resultUri, pickingFront, straightenedUri -> {
                    try {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                    } catch (Exception ignored) {}

                    ImageView targetView = pickingFront ? ivCnicFront : ivCnicBack;
                    if (targetView != null) {
                        targetView.setImageURI(null);
                        targetView.setImageURI(straightenedUri);
                    }
                    runSilentOcrForExpiry(straightenedUri, pickingFront);
                    if (pickingFront) {
                        cnicFrontUri = straightenedUri;
                        cnicFrontBase64 = CommonFunctions.convertToBase64(straightenedUri, LoanApplicationActivity.this);
                        View placeholder = findViewById(R.id.llCnicFrontPlaceholder);
                        if (placeholder != null) placeholder.setVisibility(View.GONE);
                        if (ivViewCnicFront != null) ivViewCnicFront.setVisibility(View.VISIBLE);
                        if (ivRotateCnicFront != null) ivRotateCnicFront.setVisibility(View.VISIBLE);
                    } else {
                        cnicBackUri = straightenedUri;
                        cnicBackBase64 = CommonFunctions.convertToBase64(straightenedUri, LoanApplicationActivity.this);
                        View placeholder = findViewById(R.id.llCnicBackPlaceholder);
                        if (placeholder != null) placeholder.setVisibility(View.GONE);
                        if (ivViewCnicBack != null) ivViewCnicBack.setVisibility(View.VISIBLE);
                        if (ivRotateCnicBack != null) ivRotateCnicBack.setVisibility(View.VISIBLE);
                    }
                        
                    if (cnicFrontUri != null && cnicBackUri != null) {
                        triggerGeminiExtraction(cnicFrontUri, cnicBackUri);
                    }
                });
            }
        }
    }

    private Uri autoRotateIfVertical(Uri imageUri) {
        return imageUri;
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        // Never leave the phone's sounds muted if the screen closes while the assistant is listening
        if (assistantBeepMuter != null) assistantBeepMuter.restore();
        if (assistantVoicePlayer != null) {
            assistantVoicePlayer.release();
            assistantVoicePlayer = null;
        }
    }
}
