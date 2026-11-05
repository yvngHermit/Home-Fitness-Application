package com.example.homefitness

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FitnessMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveToken(token)
    }

    private fun saveToken(token: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("fcmToken" to token),
                com.google.firebase.firestore.SetOptions.merge()
            )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val prefs = getSharedPreferences("home_fitness_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", false)) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new Home Fitness update."

        val channelId = "home_fitness_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = getString(R.string.notification_channel_description)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Android 13+ notification permission has not been granted.
        }
    }
}
