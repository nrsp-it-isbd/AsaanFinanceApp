package com.assan.finance.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.Observer
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.assan.finance.R
import com.assan.finance.databinding.FragmentHomeBinding
import com.assan.finance.model.DetectionCategory
import java.util.*
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.EditText
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import com.assan.finance.utils.CommonFunctions
import com.assan.finance.apis.RetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import com.assan.finance.activities.loan.LoanApplicationActivity
import com.assan.finance.activities.UserGuideActivity

data class RecentScan(
    val plantName: String,
    val healthStatus: String,
    val timestamp: Long,
    val imageUri: String,
    val categoryKey: String = "plant"
)

/** Category group definition */
data class CategoryGroup(
    val emoji: String,
    val titleEn: String,
    val titleUr: String,
    val color: Long,
    val items: List<DetectionCategory>
)

data class AssanServiceItem(
    val emoji: String,
    val titleEn: String,
    val titleUr: String,
    val descEn: String,
    val descUr: String,
    val color: Long
)

class HomeFragment : Fragment() {

    companion object {
        private var isAmountAnimated = false
        private var lastAnimatedValue = -1
        private var weatherLoadedOnce = false
        private var cachedCityName: String = "Islamabad"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // FIX: banner auto-scroll handler is now owned by the fragment and stopped in onDestroyView.
    // Before, every return to Home started another loop that kept the old views alive (leak) and
    // made the banner scroll faster and faster.
    private val bannerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var bannerRunnable: Runnable? = null

    private lateinit var prefs: SharedPreferences
    private var selectedCategoryForGallery = DetectionCategory.PLANT
    private var isUrdu = false





    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(requireContext(), R.string.auto_image_selected_successful_657, Toast.LENGTH_SHORT).show()
        }
    }

    private val assanServices = listOf(
        AssanServiceItem("💰", "Financing Application", "فنانسنگ کی درخواست", "Apply for agricultural financing", "زرعی قرضوں کے لیے درخواست دیں", 0xFFE91E63),
        AssanServiceItem("📜", "Financing Ledger", "فنانسنگ کا کھاتہ", "View your active financing", "اپنے فعال قرضے دیکھیں", 0xFF00BCD4)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvMarqueeInfo.isSelected = true
        
        prefs = requireContext().getSharedPreferences("detect_prefs", Context.MODE_PRIVATE)
        isUrdu = com.assan.finance.utils.LocaleHelper.getLanguage(requireContext()) == "ur"
        
        updateUserProfile()

        binding.ivProfile.setOnClickListener {
            showProfileDialog()
        }
        
        binding.tvHomeAssanId.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val currentCnic = binding.tvHomeAssanId.text.toString().replace("CNIC: ", "")
            val clip = android.content.ClipData.newPlainText("CNIC", currentCnic)
            clipboard.setPrimaryClip(clip)
            
            val toastMessage = if (isUrdu) "CNIC کاپی ہو گیا ہے!" else "CNIC copied to clipboard!"
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
        }
        
        binding.tvBorrowerNumber.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val currentBorrowerNo = binding.tvBorrowerNumber.text.toString().replace(getString(R.string.str_borrower_no), "").replace("قرض دار نمبر: ", "")
            val clip = android.content.ClipData.newPlainText("Borrower No", currentBorrowerNo)
            clipboard.setPrimaryClip(clip)
            
            val toastMessage = if (isUrdu) "قرض دار نمبر کاپی ہو گیا ہے!" else "Borrower No copied to clipboard!"
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
        }
        
        // Dynamically translate the service button labels based on active locale
        binding.tvApplyHome.text = if (isUrdu) "فنانسنگ کی درخواست" else "Financing Application"
        binding.tvStatusHome.text = if (isUrdu) "فنانسنگ کا کھاتہ" else "Financing Ledger"
        val tvUserGuide = binding.root.findViewById<TextView>(R.id.tvUserGuideHome)
        tvUserGuide?.text = if (isUrdu) "صارف گائیڈ" else "User Guide"

        // Service Buttons Click Listeners
        binding.btnApplyHome.setOnClickListener {
            startActivity(Intent(requireContext(), LoanApplicationActivity::class.java))
        }
        
        binding.btnStatusHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_loanLedgerFragment)
        }
        
        binding.root.findViewById<View>(R.id.btnUserGuideHome)?.setOnClickListener {
            startActivity(Intent(requireContext(), UserGuideActivity::class.java))
        }
        
        binding.fabVoiceChat.setOnClickListener { findNavController().navigate(R.id.action_home_to_voiceChat) }
        
        // Static Promo Buttons
        val btnPromoApply = binding.root.findViewById<View>(R.id.btnPromoApply)
        val cardPromotionBanner = binding.root.findViewById<View>(R.id.cardPromotionBanner)
        
        btnPromoApply?.setOnClickListener {
            val intent = Intent(requireContext(), LoanApplicationActivity::class.java)
            startActivity(intent)
        }
        // Apply only from Asaan Services -> Financing Application: the scheme card and its
        // "Apply Now" button (hidden in the layout) no longer open the form
        cardPromotionBanner?.isClickable = false

        // Promo Banner Setup (Temporary Local)
        setupLocalBanners()
        fetchHomeScreenData()

        // Notifications Click
        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireContext(), com.assan.finance.activities.notifications.NotificationsActivity::class.java)
            startActivity(intent)
        }
        
        // Language Toggle Click
        val currentLang = com.assan.finance.utils.LocaleHelper.getLanguage(requireContext())
        if (currentLang == "ur") {
            binding.tvHomeLangToggle.text = "EN | اردو"
        } else {
            binding.tvHomeLangToggle.text = "اردو | EN"
        }
        binding.tvHomeLangToggle.setOnClickListener {
            val newLang = if (com.assan.finance.utils.LocaleHelper.getLanguage(requireContext()) == "en") "ur" else "en"
            com.assan.finance.utils.LocaleHelper.setLocale(requireContext(), newLang)
            requireActivity().recreate()
        }
        
        // Menu / Logout Click
        binding.ivLogout.setOnClickListener {
            CommonFunctions.showLogoutDialog(requireActivity())
        }
        
        // Profile Click
        binding.ivProfile.setOnClickListener {
            showProfileDialog()
        }
        
        // Language Toggle state
        isUrdu = com.assan.finance.utils.LocaleHelper.getLanguage(requireContext()) == "ur"

        val shimmerHome = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerHome)
        val svHomeContent = binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.svHomeContent)
        val appBarLayout = binding.root.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
        
        // Setup SwipeRefreshLayout
        val swipeRefreshHome = binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome)
        swipeRefreshHome?.setColorSchemeResources(R.color.colorPrimary, R.color.teal_brand)
        swipeRefreshHome?.setOnRefreshListener {
            isAmountAnimated = false // Force re-animate to provide visual feedback
            fetchHomeScreenData()
            checkUnreadNotifications()
            loadHomeData()
        }
        
        // Setup Network Monitor to auto-refresh when internet is back
        val networkMonitor = com.assan.finance.utils.NetworkMonitor(requireContext())
        networkMonitor.isConnected.observe(viewLifecycleOwner, Observer { isConnected ->
            if (isConnected) {
                // If it was previously showing network error or empty, refresh it
                val currentStatus = binding.tvLoanStatusText.text.toString()
                if (currentStatus == "Network Error" || currentStatus == "نیٹ ورک کی خرابی" || 
                    currentStatus == "Loading..." || currentStatus == "لوڈ ہو رہا ہے..." ||
                    binding.tvActiveAmount.text.toString() == "PKR 0") {
                    loadHomeData()
                }
            }
        })
        
        // Load data immediately to sync with bottom nav visibility
        if (isAdded) {
            loadHomeData()
        }

        // Handle double back press to exit from home screen
        val onBackPressedCallback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                CommonFunctions.handleBackPressed(requireActivity())
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }
    
    private fun showLoanCalculatorDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.dialog_loan_calculator)

        val etAmount = dialog.findViewById<EditText>(R.id.etCalcAmount)
        etAmount?.addTextChangedListener(com.assan.finance.utils.NumberTextWatcher(etAmount))
        val etDuration = dialog.findViewById<EditText>(R.id.etCalcDuration)
        val btnCalculate = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCalculate)
        val layoutResults = dialog.findViewById<LinearLayout>(R.id.layoutCalcResults)
        val tvMonthly = dialog.findViewById<TextView>(R.id.tvCalcMonthly)
        val tvTotalMarkup = dialog.findViewById<TextView>(R.id.tvCalcTotalMarkup)
        val tvTotalPayable = dialog.findViewById<TextView>(R.id.tvCalcTotalPayable)

        btnCalculate?.setOnClickListener {
            val amountStr = etAmount?.text?.toString()?.replace(",", "") ?: ""
            val durationStr = etDuration?.text?.toString() ?: ""

            if (amountStr.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(requireContext(), R.string.auto_please_fill_all_fields_658, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val durationMonths = durationStr.toIntOrNull() ?: 0

            if (amount <= 0 || durationMonths <= 0) {
                Toast.makeText(requireContext(), R.string.auto_invalid_amount_or_duratio_659, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 37.5% annual interest
            val annualRate = 37.5 / 100.0
            val totalMarkup = amount * annualRate * (durationMonths / 12.0)
            val totalPayable = amount + totalMarkup
            val monthlyInstallment = totalPayable / durationMonths

            val format = java.text.DecimalFormat("#,###")
            tvMonthly?.text = "PKR " + format.format(monthlyInstallment)
            tvTotalMarkup?.text = "PKR " + format.format(totalMarkup)
            tvTotalPayable?.text = "PKR " + format.format(totalPayable)

            layoutResults?.visibility = View.VISIBLE
        }

        dialog.show()
    }
    
    private fun showProfileDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.dialog_profile)
        
        val sharedManager = SharedManager.getInstance(requireContext())
        
        val ivProfilePic = dialog.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivProfilePic)!!
        val tvName = dialog.findViewById<TextView>(R.id.tvProfileName)!!
        val tvBadge = dialog.findViewById<TextView>(R.id.tvProfileBadge)!!
        val tvAssanId = dialog.findViewById<TextView>(R.id.tvProfileAssanId)!!
        val tvCnic = dialog.findViewById<TextView>(R.id.tvProfileCnic)!!
        val tvPhone = dialog.findViewById<TextView>(R.id.tvProfilePhone)!!
        val tvDob = dialog.findViewById<TextView>(R.id.tvProfileDob)!!
        val tvGender = dialog.findViewById<TextView>(R.id.tvProfileGender)!!
        
        val lblAssanId = dialog.findViewById<TextView>(R.id.lblAssanId)!!
        val lblCnic = dialog.findViewById<TextView>(R.id.lblCnic)!!
        val lblPhone = dialog.findViewById<TextView>(R.id.lblPhone)!!
        val lblDob = dialog.findViewById<TextView>(R.id.lblDob)!!
        val lblGender = dialog.findViewById<TextView>(R.id.lblGender)!!
        
        val btnEdit = dialog.findViewById<TextView>(R.id.btnEditProfile)!!
        val btnClose = dialog.findViewById<TextView>(R.id.btnClose)!!
        
        val name = sharedManager.getString("NAME", "Asaan User")
        val phone = sharedManager.getString("PHONE", "")
        val cnic = sharedManager.getString("CNIC", "")
        val rawDob = sharedManager.getString("DATE_OF_BIRTH", "")
        val dob = if (!com.assan.finance.utils.CommonFunctions.isNullOrEmpty(rawDob)) com.assan.finance.utils.CommonFunctions.formatDateOfBirth(rawDob) else ""
        val gender = sharedManager.getString("GENDER", "1")
        val clientId = sharedManager.getInt("CLIENT_ID", 0)
        
        // Inline helpers for formatting
        val cleanCnic = cnic.replace("-", "")
        val formattedCnic = if (cleanCnic.length == 13) {
            "${cleanCnic.substring(0, 5)}-${cleanCnic.substring(5, 12)}-${cleanCnic.substring(12)}"
        } else {
            cnic
        }
        
        val cleanPhone = phone.replace("-", "").replace(" ", "")
        val formattedPhone = if (cleanPhone.length == 11) {
            "${cleanPhone.substring(0, 4)}-${cleanPhone.substring(4)}"
        } else {
            phone
        }
        
        // Populate static info
        tvName.text = name
        tvAssanId.text = clientId.toString()
        tvCnic.text = if (formattedCnic.isNotEmpty()) formattedCnic else "N/A"
        tvPhone.text = if (formattedPhone.isNotEmpty()) formattedPhone else "N/A"
        tvDob.text = if (dob.isNotEmpty()) dob else "N/A"
        
        // Gender formatting
        val genderFormatted = if (gender == "1") {
            if (isUrdu) "مرد" else "Male"
        } else if (gender == "2") {
            if (isUrdu) "عورت" else "Female"
        } else {
            if (isUrdu) "دیگر" else "Other"
        }
        tvGender.text = genderFormatted
        
        // Image Population
        val userProfileImage = sharedManager.getString("PROFILE_PICTURE", "")
        android.util.Log.d("HomeFragment", "Dialog profile picture path: '$userProfileImage'")
        if (!userProfileImage.isNullOrEmpty()) {
            val fullUrl = com.assan.finance.apis.ApiManager.getFullImageUrl(userProfileImage)
            android.util.Log.d("HomeFragment", "Dialog profile fullUrl: '$fullUrl'")
            com.squareup.picasso.Picasso.get()
                .load(fullUrl)
                .noFade()
                .placeholder(R.drawable.dummy_profile_image)
                .error(R.drawable.dummy_profile_image)
                .into(ivProfilePic, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        android.util.Log.d("HomeFragment", "Successfully loaded dialog profile image")
                    }
                    override fun onError(e: Exception?) {
                        android.util.Log.e("HomeFragment", "Failed to load dialog profile image: $fullUrl", e)
                    }
                })
        } else {
            ivProfilePic.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.dummy_profile_image))
        }
        
        ivProfilePic.setOnClickListener {
            showFullScreenImage(userProfileImage, ivProfilePic.drawable)
        }
        
        // Localization labels
        if (isUrdu) {
            lblAssanId.text = "آسان آئی ڈی"
            lblCnic.text = "شناختی کارڈ نمبر"
            lblPhone.text = "فون نمبر"
            lblDob.text = "تاریخ پیدائش"
            lblGender.text = "جنس"
            tvBadge.text = "آسان ممبر"
            btnClose.text = "بند کریں"
            btnEdit.text = "پروفائل تبدیل کریں"
        } else {
            lblAssanId.text = getString(R.string.str_Asaan_id)
            lblCnic.text = getString(R.string.str_cnic_number_label)
            lblPhone.text = getString(R.string.str_phone_number_label)
            lblDob.text = getString(R.string.str_date_of_birth_label)
            lblGender.text = getString(R.string.str_gender_label)
            tvBadge.text = getString(R.string.str_Asaan_member)
            btnClose.text = getString(R.string.str_close_label)
            btnEdit.text = getString(R.string.str_edit_profile_label)
        }
        
        btnEdit.setOnClickListener {
            dialog.dismiss()
            try {
                findNavController().navigate(R.id.updateProfileFragment)
            } catch (e: Exception) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, com.assan.finance.fragments.UpdateProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showFullScreenImage(imagePath: String?, placeholderDrawable: android.graphics.drawable.Drawable?) {
        try {
            val fullDialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            fullDialog.setContentView(R.layout.dialog_fullscreen_image)

            val photoView = fullDialog.findViewById<com.github.chrisbanes.photoview.PhotoView>(R.id.photoView)
            val ivClose = fullDialog.findViewById<ImageView>(R.id.ivClose)
            val ivRotate = fullDialog.findViewById<ImageView>(R.id.ivRotateFullscreen)

            photoView?.apply {
                maximumScale = 10.0f
                mediumScale = 3.0f
                minimumScale = 1.0f
                if (placeholderDrawable != null) {
                    setImageDrawable(placeholderDrawable)
                } else {
                    setImageResource(R.drawable.dummy_profile_image)
                }
            }

            if (!imagePath.isNullOrEmpty() && photoView != null) {
                val fullUrl = com.assan.finance.apis.ApiManager.getFullImageUrl(imagePath)
                com.squareup.picasso.Picasso.get()
                    .load(fullUrl)
                    .placeholder(photoView.drawable ?: ContextCompat.getDrawable(requireContext(), R.drawable.dummy_profile_image)!!)
                    .error(R.drawable.dummy_profile_image)
                    .into(photoView)
            }

            ivClose?.setOnClickListener {
                fullDialog.dismiss()
            }

            ivRotate?.setOnClickListener {
                photoView?.let { pv ->
                    val drawable = pv.drawable
                    if (drawable is BitmapDrawable) {
                        val bmp = drawable.bitmap
                        if (bmp != null) {
                            val matrix = Matrix().apply { postRotate(90f) }
                            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                            pv.setImageBitmap(rotated)
                        }
                    }
                }
            }

            fullDialog.show()
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error showing full screen profile image", e)
        }
    }

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assan.finance.NEW_NOTIFICATION") {
                checkUnreadNotifications()
            }
        }
    }

    private fun updateUserProfile() {
        val sharedManager = SharedManager.getInstance(requireContext())
        // FIX: a hard-coded test name ("Muhammad Ibrahim") was shown when NAME wasn't saved yet
        val name = sharedManager.getString(getString(R.string.lbl_name_upper), sharedManager.getString("NAME", ""))
        val borrowerNo = sharedManager.getString("BORROWER_NO", "")
        val cnic = sharedManager.getString(getString(R.string.lbl_cnic_upper), sharedManager.getString("CNIC", "")) ?: ""

        binding.tvHomeUserName.text = name
        binding.tvHomeAssanId.text = "CNIC: $cnic"
        binding.tvHomeAssanId.visibility = View.VISIBLE

        binding.tvBorrowerNumber.text = if (isUrdu) "قرض دار نمبر: ${if(borrowerNo.isNullOrEmpty()) "N/A" else borrowerNo}" else "Borrower No: ${if(borrowerNo.isNullOrEmpty()) "N/A" else borrowerNo}"
        binding.tvBorrowerNumber.visibility = View.VISIBLE

        val userProfileImage = sharedManager.getString("PROFILE_PICTURE", "")
        android.util.Log.d("HomeFragment", "Top bar profile picture path: '$userProfileImage'")
        if (!userProfileImage.isNullOrEmpty()) {
            binding.ivProfile.setPadding(0, 0, 0, 0)
            val fullUrl = com.assan.finance.apis.ApiManager.getFullImageUrl(userProfileImage)
            android.util.Log.d("HomeFragment", "Top bar profile fullUrl: '$fullUrl'")
            com.squareup.picasso.Picasso.get()
                .load(fullUrl)
                .noFade()
                .placeholder(R.drawable.dummy_profile_image)
                .error(R.drawable.dummy_profile_image)
                .into(binding.ivProfile, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        android.util.Log.d("HomeFragment", "Successfully loaded top bar profile image")
                    }
                    override fun onError(e: Exception?) {
                        android.util.Log.e("HomeFragment", "Failed to load top bar profile image: $fullUrl", e)
                    }
                })
        } else {
            val padding = (8 * resources.displayMetrics.density).toInt()
            binding.ivProfile.setPadding(padding, padding, padding, padding)
            binding.ivProfile.setImageResource(R.drawable.ic_user)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserProfile()
        checkUnreadNotifications()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(notificationReceiver, android.content.IntentFilter("com.assan.finance.NEW_NOTIFICATION"))
    }

    override fun onPause() {
        super.onPause()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(notificationReceiver)
    }

    private fun checkUnreadNotifications() {
        val sharedManager = SharedManager.getInstance(requireContext())
        val hasLocalUnread = sharedManager.getBoolean("HAS_UNREAD_NOTIFICATION", false)
        val badge = view?.findViewById<TextView>(R.id.tvNotificationBadge)
        
        if (hasLocalUnread) {
            badge?.visibility = View.VISIBLE
            badge?.text = "1+"
        }
        
        val borrowerId = sharedManager.getString("BORROWER_NO", "") ?: ""
        val cnic = sharedManager.getString("CNIC", "") ?: ""
        
        if (borrowerId.isNotEmpty() || cnic.isNotEmpty()) {
            val retrofitClient = RetrofitClient(com.assan.finance.network.assan.AssanApiService::class.java, requireContext())
            retrofitClient.apiService.getNotifications(borrowerId, cnic).enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelNotifications> {
                override fun onResponse(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelNotifications>, response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelNotifications>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success && body.data != null) {
                            val unreadCount = body.data.count { !it.isRead }
                            if (unreadCount > 0) {
                                badge?.visibility = View.VISIBLE
                                badge?.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                                sharedManager.saveBoolean("HAS_UNREAD_NOTIFICATION", true)
                            } else {
                                badge?.visibility = View.GONE
                                sharedManager.saveBoolean("HAS_UNREAD_NOTIFICATION", false)
                            }
                        }
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelNotifications>, t: Throwable) {}
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerRunnable?.let { bannerHandler.removeCallbacks(it) }
        bannerRunnable = null
        _binding = null
    }

    private fun setupLocalBanners() {
        val localBanners = listOf(
            R.drawable.promo_banner_1,
            R.drawable.promo_banner_2,
            R.drawable.promo_banner_3
        )
        val vpPromoBanner = binding.root.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpPromoBanner)
        if (vpPromoBanner != null) {
            vpPromoBanner.adapter = BannerAdapter(localBanners)
            
            // Auto-scroll
            bannerRunnable?.let { bannerHandler.removeCallbacks(it) }
            val runnable = object : Runnable {
                override fun run() {
                    if (_binding != null && vpPromoBanner.adapter != null && isAdded) {
                        var nextItem = vpPromoBanner.currentItem + 1
                        if (nextItem >= localBanners.size) nextItem = 0
                        vpPromoBanner.setCurrentItem(nextItem, true)
                        bannerHandler.postDelayed(this, 3000)
                    }
                }
            }
            bannerRunnable = runnable
            bannerHandler.postDelayed(runnable, 3000)
        }
    }

    // dots setup removed
    inner class BannerAdapter(private val images: List<Int>) : androidx.recyclerview.widget.RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {
        inner class BannerViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val ivBannerImage = view.findViewById<ImageView>(R.id.ivBannerImage)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
            return BannerViewHolder(view)
        }
        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(images[position])
                .centerCrop()
                .into(holder.ivBannerImage)
            holder.itemView.setOnClickListener {
                // Future: Open relevant link / ad for Assan Finance.
            }
        }
        override fun getItemCount() = images.size
    }

    private fun fetchHomeScreenData() {
        val retrofitClient = RetrofitClient(com.assan.finance.apis.ApiService::class.java, requireContext())
        retrofitClient.apiService.homeScreenData.enqueue(object : retrofit2.Callback<com.assan.finance.responseModel.ResponseModelHomeScreenData> {
            override fun onResponse(
                call: retrofit2.Call<com.assan.finance.responseModel.ResponseModelHomeScreenData>,
                response: retrofit2.Response<com.assan.finance.responseModel.ResponseModelHomeScreenData>
            ) {
                // FIX: response can arrive after the user left Home → binding is null → crash
                if (_binding == null) return
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    data?.uiConfig?.let { uiConfig ->
                        val tvMarqueeInfo = binding.root.findViewById<TextView>(R.id.tvMarqueeInfo)
                        val tvPromoTitle = binding.root.findViewById<TextView>(R.id.tvPromoTitle)
                        val tvPromoDesc = binding.root.findViewById<TextView>(R.id.tvPromoDesc)
                        
                        tvMarqueeInfo?.text = uiConfig.marqueeText
                        tvPromoTitle?.text = uiConfig.loanSchemeTitle
                        tvPromoDesc?.text = uiConfig.loanSchemeDesc
                        
                        tvMarqueeInfo?.isSelected = true
                    }
                    /* Temporary override for local banners
                    data?.sliderImages?.let { images ->
                        if (images.isNotEmpty()) {
                            val imageUrls = images.map { it.imageUrl }
                            val vpPromoBanner = binding.root.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpPromoBanner)
                            if (vpPromoBanner != null) {
                                vpPromoBanner.adapter = BannerAdapter(imageUrls)
                                
                                // Auto-scroll
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                val runnable = object : Runnable {
                                    override fun run() {
                                        if (vpPromoBanner.adapter != null && isAdded) {
                                            var nextItem = vpPromoBanner.currentItem + 1
                                            if (nextItem >= imageUrls.size) nextItem = 0
                                            vpPromoBanner.setCurrentItem(nextItem, true)
                                            handler.postDelayed(this, 3000)
                                        }
                                    }
                                }
                                handler.postDelayed(runnable, 3000)
                            }
                        }
                    }
                    */
                }
            }
            override fun onFailure(call: retrofit2.Call<com.assan.finance.responseModel.ResponseModelHomeScreenData>, t: Throwable) {
                android.util.Log.e("HomeFragment", "Failed to fetch home screen data: ${t.message}")
                if (_binding != null) {
                    CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
                }
            }
        })
    }

    private fun loadHomeData() {
        if (!isAdded) return
        val sharedManager = SharedManager.getInstance(requireContext())
        val cnic = sharedManager.getString("CNIC", "") ?: ""
        
        val shimmerHome = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerHome)
        val svHomeContent = binding.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.svHomeContent)
        val appBarLayout = binding.root.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)

        binding.tvLoanCycleTitle.text = if (isUrdu) "فنانسنگ سائیکل" else "FINANCING CYCLE"
        binding.tvLoanCycleText.text = "—"
        binding.tvActiveAmount.text = "PKR 0"
        binding.tvLoanStatusText.text = if (isUrdu) "لوڈ ہو رہا ہے..." else "Loading..."

        // Show shimmer
        appBarLayout?.visibility = View.VISIBLE
        svHomeContent?.visibility = View.GONE
        shimmerHome?.visibility = View.VISIBLE
        shimmerHome?.startShimmer()

        val retrofitClient = RetrofitClient(com.assan.finance.network.assan.AssanApiService::class.java, requireContext())
        val call = retrofitClient.apiService.getLedgerByCnic(cnic)
        
        call.enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelAssanLedger> {
            override fun onResponse(
                call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelAssanLedger>,
                response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelAssanLedger>
            ) {
                if (_binding == null) return
                
                shimmerHome?.stopShimmer()
                binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome)?.isRefreshing = false
                shimmerHome?.visibility = View.GONE
                appBarLayout?.visibility = View.VISIBLE
                svHomeContent?.visibility = View.VISIBLE
                
                if (response.isSuccessful && response.body()?.success == true && response.body()?.records?.isNotEmpty() == true) {
                    val records = response.body()?.records!!
                    val firstRecord = records[0]
                    
                    if (!firstRecord.borrowerNo.isNullOrEmpty()) {
                        val sharedManager = SharedManager.getInstance(requireContext())
                        sharedManager.saveString("BORROWER_NO", firstRecord.borrowerNo!!)
                    }
                    
                    binding.tvBorrowerNumber.text = if (isUrdu) "قرض دار نمبر: ${firstRecord.borrowerNo ?: "N/A"}" else "Borrower No: ${firstRecord.borrowerNo ?: "N/A"}"
                    
                    val loanAmount = firstRecord.amount
                    var totalRecPa = 0.0
                    for (rec in records) {
                        totalRecPa += rec.recPa
                    }
                    val finalAmount = loanAmount.toInt()

                    if (!isAmountAnimated || lastAnimatedValue != finalAmount) {
                        isAmountAnimated = true
                        lastAnimatedValue = finalAmount
                        val animator = android.animation.ValueAnimator.ofInt(0, finalAmount)
                        animator.duration = 1500
                        animator.addUpdateListener { animation ->
                            val animatedValue = animation.animatedValue as Int
                            _binding?.tvActiveAmount?.text = "PKR ${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(animatedValue)}"
                        }
                        animator.start()
                    } else {
                        binding.tvActiveAmount.text = "PKR ${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(finalAmount)}"
                    }
                    
                    val outstanding = loanAmount - totalRecPa

                    // FIX: the ledger has one row per receipt, so the same installment can appear twice.
                    // Take each installment's due once, in order, and compare with what was recovered.
                    val schedule = records.filter { it.installmentNo > 0 }
                        .distinctBy { it.installmentNo }
                        .sortedBy { it.installmentNo }
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    var cumulativeDue = 0.0
                    var dueTillToday = 0.0
                    var nextDue: com.assan.finance.model.assan.AssanLedgerRecord? = null
                    for (inst in schedule) {
                        cumulativeDue += inst.dueTotal
                        val instDate = inst.dueDate?.take(10) ?: ""
                        if (instDate.isNotEmpty() && instDate <= today) dueTillToday += inst.dueTotal
                        // first installment not fully covered by the total recovered so far
                        if (nextDue == null && cumulativeDue > totalRecPa + 1) nextDue = inst
                    }
                    val isOverdue = outstanding > 0 && dueTillToday > totalRecPa + 1

                    if (firstRecord.loanStatus != null) {
                        when (firstRecord.loanStatus.lowercase(java.util.Locale.US)) {
                            "applied", "0" -> binding.tvLoanStatusText.text = "Applied"
                            "sanction", "sanctioned", "1" -> binding.tvLoanStatusText.text = "Sanction"
                            "disbursed", "2" -> binding.tvLoanStatusText.text = "Disbursed"
                            else -> binding.tvLoanStatusText.text = if (isOverdue) "Overdue" else if (outstanding > 0) "Active" else "Settled"
                        }
                    } else {
                        binding.tvLoanStatusText.text = if (isOverdue) "Overdue" else if (outstanding > 0) "Active" else "Settled"
                    }

                    // FIX: showed installment 1's date (22-Jun) even after it was paid; show the next unpaid one
                    val rawDate = if (outstanding <= 0) null else (nextDue?.dueDate ?: firstRecord.dueDate)
                    val dateString = rawDate?.split("T")?.get(0)
                    if (!dateString.isNullOrEmpty()) {
                        try {
                            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val outputFormat = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.US)
                            val date = inputFormat.parse(dateString)
                            binding.tvDueDate?.text = if (date != null) outputFormat.format(date) else dateString
                        } catch (e: Exception) {
                            binding.tvDueDate?.text = dateString
                        }
                    } else {
                        binding.tvDueDate?.text = "N/A"
                    }
                    
                    // FIX: "Repeat Cycle" came from a phone-wide flag set whenever ANY application was submitted
                    // on this phone; it was not this borrower's cycle. The ledger API has no cycle field yet,
                    // so show "First Cycle" until the backend sends the real cycle.
                    binding.tvLoanCycleTitle.text = if (isUrdu) "فنانسنگ سائیکل" else "FINANCING CYCLE"
                    binding.tvLoanCycleText.text = if (isUrdu) "پہلا سائیکل" else "First Cycle"
                } else {
                    binding.tvActiveAmount.text = "PKR 0"
                    binding.tvLoanStatusText.text = if (isUrdu) "کوئی فعال فنانسنگ نہیں" else "No active financing"
                    // FIX: the layout's sample due date ("18 Sep, 2024") and cycle stayed visible with no financing
                    binding.tvDueDate?.text = "—"
                    binding.tvLoanCycleText.text = "—"
                }
            }

            override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelAssanLedger>, t: Throwable) {
                if (_binding == null) return
                CommonFunctions.handleApiFailure(requireContext(), binding.root, t)
                shimmerHome?.stopShimmer()
                binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshHome)?.isRefreshing = false
                shimmerHome?.visibility = View.GONE
                appBarLayout?.visibility = View.VISIBLE
                svHomeContent?.visibility = View.VISIBLE
                binding.tvLoanStatusText.text = "-"
            }
        })


    }

}
