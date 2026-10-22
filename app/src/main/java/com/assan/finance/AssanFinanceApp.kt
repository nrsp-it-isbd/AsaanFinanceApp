package com.assan.finance

import android.app.Application


/**
 * Application class — initializes RetrofitClient with SSL certificate
 * support once, before any Activity or Fragment is created.
 */
class AssanFinanceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Retrofit with the bundled SSL certificate (off the main thread: faster app start)
        com.assan.finance.network.assan.AssanRetrofitClient.initAsync(this)
        
        // Start SignalR for real-time notifications
        com.assan.finance.managers.SignalRManager.connect(this)
        
        val sharedManager = com.assan.finance.sharedPreferences.SharedManager.getInstance(this)
        if (sharedManager.getBoolean("DARK_MODE", false)) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {
                val sharedManager = com.assan.finance.sharedPreferences.SharedManager.getInstance(this@AssanFinanceApp)
                if (sharedManager.getBoolean("KEEP_SCREEN_ON", true)) {
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    companion object {
        private var inactivityHandler: android.os.Handler? = null
        private var logoutRunnable: Runnable? = null
        private const val INACTIVITY_TIMEOUT = 3 * 60 * 1000L // 3 minutes

        fun resetInactivityTimer(context: android.content.Context) {
            val sharedManager = com.assan.finance.sharedPreferences.SharedManager.getInstance(context)
            if (!sharedManager.getBoolean("AUTO_LOGOUT_ENABLED", false)) {
                cancelInactivityTimer()
                return
            }

            if (inactivityHandler == null) {
                inactivityHandler = android.os.Handler(android.os.Looper.getMainLooper())
                logoutRunnable = Runnable {
                    val mgr = com.assan.finance.sharedPreferences.SharedManager.getInstance(context)
                    mgr.clearPreferences()
                    com.assan.finance.utils.CommonFunctions.deletePushToken(context)
                    com.assan.finance.managers.SignalRManager.disconnect()
                    val intent = android.content.Intent(context, com.assan.finance.MainActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            }
            inactivityHandler?.removeCallbacks(logoutRunnable!!)
            inactivityHandler?.postDelayed(logoutRunnable!!, INACTIVITY_TIMEOUT)
        }

        fun cancelInactivityTimer() {
            if (inactivityHandler != null && logoutRunnable != null) {
                inactivityHandler?.removeCallbacks(logoutRunnable!!)
            }
        }
    }
}
