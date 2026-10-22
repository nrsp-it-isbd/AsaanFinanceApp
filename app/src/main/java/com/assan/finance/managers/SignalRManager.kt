package com.assan.finance.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.assan.finance.R
import com.assan.finance.activities.notifications.NotificationsActivity
import com.assan.finance.model.assan.NotificationPayload
import com.assan.finance.sharedPreferences.SharedManager
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState

object SignalRManager {
    private const val TAG = "SignalRManager"
    private const val HUB_URL = "http://103.172.140.103:999/hubs/notifications"
    private var hubConnection: HubConnection? = null
    private const val CHANNEL_ID = "assan_notifications"

    fun connect(context: Context) {
        val sharedManager = SharedManager.getInstance(context)
        val cnic = sharedManager.getString("CNIC", "") ?: ""
        val borrowerId = sharedManager.getString("BORROWER_NO", "") ?: ""

        if (cnic.isEmpty() && borrowerId.isEmpty()) {
            Log.d(TAG, "Cannot connect to SignalR: User not logged in (No CNIC or BorrowerId)")
            return
        }

        if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
            Log.d(TAG, "SignalR is already connected")
            return
        }

        hubConnection = HubConnectionBuilder
            .create(HUB_URL)
            .build()

        hubConnection?.on("NotificationReceived", { payload: NotificationPayload ->
            Log.d(TAG, "Notification Received: ${payload.message}")
            showNotification(context, payload)
            
            // Set flag so home screen knows there is a new unread notification
            sharedManager.saveBoolean("HAS_UNREAD_NOTIFICATION", true)
            
            // Broadcast so HomeFragment updates dynamically if open
            val intent = Intent("com.assan.finance.NEW_NOTIFICATION")
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
        }, NotificationPayload::class.java)

        hubConnection?.start()?.subscribe({
            Log.d(TAG, "SignalR Connected successfully")
            hubConnection?.invoke("Subscribe", cnic, borrowerId)
        }, { error ->
            Log.e(TAG, "SignalR Connection failed", error)
        })
    }

    fun disconnect() {
        if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
            hubConnection?.stop()
            Log.d(TAG, "SignalR Disconnected")
        }
        hubConnection = null
    }

    private fun showNotification(context: Context, payload: NotificationPayload) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for financing updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            payload.logId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Asaan Finance")
            .setContentText(payload.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(payload.logId.toInt(), builder.build())
    }
}
