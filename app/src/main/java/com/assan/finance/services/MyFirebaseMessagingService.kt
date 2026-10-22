package com.assan.finance.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.assan.finance.MainActivity
import com.assan.finance.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.assan.finance.sharedPreferences.SharedManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message Received: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Asaan Finance"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""
        val logId = remoteMessage.data["logId"] ?: "" // To be sent to backend later when tapped

        val sharedManager = SharedManager.getInstance(this)
        sharedManager.saveBoolean("HAS_UNREAD_NOTIFICATION", true)
        
        val intent = Intent("com.assan.finance.NEW_NOTIFICATION")
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        
        val isPopupsEnabled = sharedManager.getBoolean("NOTIFICATION_POPUPS", true)

        if (isPopupsEnabled) {
            showNotification(title, message, logId)
        } else {
            Log.d("FCM", "Notification received but popups are disabled in settings")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token Generated: $token")
        // If the user is logged in, you should send this token to the backend.
        // For now, it will also be fetched during the login process.
    }

    private fun showNotification(title: String, message: String, logId: String) {
        val channelId = "assan_finance_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("logId", logId)
            putExtra("openNotifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bell) // Use a solid vector drawable
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Asaan Finance Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
