package com.assan.finance
import com.assan.finance.R

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.assan.finance.databinding.ActivityMainBinding
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import com.assan.finance.sharedPreferences.SharedManager
import com.assan.finance.activities.loan.LoanApplicationActivity

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.assan.finance.utils.LocaleHelper.onAttach(newBase))
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager
    private val UPDATE_REQUEST_CODE = 1001

    private val installStateUpdatedListener = com.google.android.play.core.install.InstallStateUpdatedListener { state ->
        if (state.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
            val snackbar = com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                R.string.auto_an_update_has_just_been_d_616,
                com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction("RESTART") { appUpdateManager.completeUpdate() }
            snackbar.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     //   window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkForAppUpdate()
        // APK shared on WhatsApp / Google Drive: Play's update check cannot see it, so check our own version file
        com.assan.finance.utils.SideloadUpdateChecker.check(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        val sharedManager = SharedManager.getInstance(this)
        val isLogin = sharedManager.getBoolean(com.assan.finance.utils.Constants.isLogin, false)

        // Determine start destination based on login state
        if (savedInstanceState == null) {
            if (isLogin) {
                com.assan.finance.utils.Constants.CLIENT_ID = sharedManager.getInt("CLIENT_ID", 0)
                navGraph.setStartDestination(R.id.homeFragment)
            } else {
                navGraph.setStartDestination(R.id.assanLogin)
            }
            navController.graph = navGraph
        } else {
            // After configuration change (like Dark Mode toggle), restore CLIENT_ID if needed
            if (isLogin) {
                com.assan.finance.utils.Constants.CLIENT_ID = sharedManager.getInt("CLIENT_ID", 0)
            }
        }

        // Setup Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Only wire nav-graph fragments; handle action items manually
        bottomNav.setupWithNavController(navController)

        // Handle non-nav-graph action items (Apply & Services)
        bottomNav.setOnItemSelectedListener { item ->
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_right)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.slide_out_right)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .build()

            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment, null, navOptions)
                    true
                }
                R.id.updateProfileFragment -> {
                    navController.navigate(R.id.updateProfileFragment, null, navOptions)
                    true
                }
                R.id.feedbackFragment -> {
                    navController.navigate(R.id.feedbackFragment, null, navOptions)
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment, null, navOptions)
                    true
                }
                else -> false
            }
        }

        // Hide Bottom Nav on auth/form screens
        val bottomNavBorder = findViewById<View>(R.id.bottomNavBorder)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showNav = when (destination.id) {
                R.id.homeFragment,
                R.id.loanLedgerFragment,
                R.id.settingsFragment,
                R.id.updateProfileFragment,
                R.id.changePasswordFragment,
                R.id.aboutUsFragment,
                R.id.termsConditionsFragment,
                R.id.feedbackFragment,
                R.id.voiceChatFragment -> true
                else -> false
            }
            if (!showNav) {
                bottomNav.animate().cancel()
                bottomNavBorder?.animate()?.cancel()
                bottomNav.visibility = View.GONE
                bottomNavBorder?.visibility = View.GONE
            } else if (bottomNav.visibility != View.VISIBLE) {
                // FIX: after login the bar appeared at once while the login screen was still sliding out.
                // Show it only after the 300 ms screen transition, with a short fade.
                listOfNotNull(bottomNav, bottomNavBorder).forEach { v ->
                    v.animate().cancel()
                    v.alpha = 0f
                    v.visibility = View.VISIBLE
                    v.animate().alpha(1f).setStartDelay(300).setDuration(200).start()
                }
            }
        }

        // Set status bar color to match primary dark
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.primary_dark)
        
        // Request Notification Permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Global Network Monitor
        val networkMonitor = com.assan.finance.utils.NetworkMonitor(this)
        var networkSnackbar: com.google.android.material.snackbar.Snackbar? = null
        var wasDisconnected = false
        
        networkMonitor.isConnected.observe(this) { isConnected ->
            if (!isConnected) {
                wasDisconnected = true
                networkSnackbar = com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.auto_no_internet_connection_617, com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE)
                networkSnackbar?.setBackgroundTint(Color.parseColor("#F44336")) // Red
                networkSnackbar?.setTextColor(Color.WHITE)
                networkSnackbar?.show()
            } else {
                networkSnackbar?.dismiss()
                if (wasDisconnected) {
                    val successSnackbar = com.google.android.material.snackbar.Snackbar.make(binding.root, R.string.auto_back_online_618, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    successSnackbar.setBackgroundTint(Color.parseColor("#4CAF50")) // Green
                    successSnackbar.setTextColor(Color.WHITE)
                    successSnackbar.show()
                    wasDisconnected = false
                }
            }
        }
        
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val logIdStr = intent?.getStringExtra("logId")
        if (!logIdStr.isNullOrEmpty()) {
            try {
                val logId = logIdStr.toInt()
                com.assan.finance.network.assan.AssanRetrofitClient.apiService.markNotificationRead(logId).enqueue(object : retrofit2.Callback<com.assan.finance.model.assan.ResponseModelLogin> {
                    override fun onResponse(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, response: retrofit2.Response<com.assan.finance.model.assan.ResponseModelLogin>) {
                        Log.d("FCM", "Notification marked as read: ${response.isSuccessful}")
                    }
                    override fun onFailure(call: retrofit2.Call<com.assan.finance.model.assan.ResponseModelLogin>, t: Throwable) {
                        Log.e("FCM", "Failed to mark notification as read: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                Log.e("FCM", "Invalid logId format")
            }
        }

        // Navigate to Notifications Screen if clicked from tray
        val openNotifications = intent?.getBooleanExtra("openNotifications", false) == true
        val clickedFromTray = intent?.extras?.containsKey("google.message_id") == true
        
        val hasCnic = intent?.extras?.containsKey("cnic") == true
        val hasMessage = intent?.extras?.containsKey("message") == true
        val hasLogId = intent?.extras?.containsKey("logId") == true
        
        if (openNotifications || clickedFromTray || hasCnic || hasMessage || hasLogId) {
            val navIntent = Intent(this, com.assan.finance.activities.notifications.NotificationsActivity::class.java)
            startActivity(navIntent)
            
            // Remove extras so we don't trigger it again on rotation
            intent?.removeExtra("cnic")
            intent?.removeExtra("message")
            intent?.removeExtra("logId")
            intent?.removeExtra("openNotifications")
            intent?.removeExtra("google.message_id")
        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                    val snackbar = com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        R.string.auto_an_update_has_just_been_d_616,
                        com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
                    )
                    snackbar.setAction("RESTART") { appUpdateManager.completeUpdate() }
                    snackbar.show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.e("InAppUpdate", "Update flow failed! Result code: $resultCode")
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        com.assan.finance.AssanFinanceApp.resetInactivityTimer(this)
        return super.dispatchTouchEvent(ev)
    }
}
