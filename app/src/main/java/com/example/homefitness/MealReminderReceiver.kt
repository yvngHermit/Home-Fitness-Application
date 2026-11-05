package com.example.homefitness

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

object MealReminderScheduler {
    private const val CHANNEL = "home_fitness_updates"
    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("home_fitness_settings", Context.MODE_PRIVATE)
        schedule(context, "Breakfast", prefs.getString("breakfast_time", "07:00") ?: "07:00", 1001)
        schedule(context, "Lunch", prefs.getString("lunch_time", "13:00") ?: "13:00", 1002)
        schedule(context, "Dinner", prefs.getString("dinner_time", "18:00") ?: "18:00", 1003)
        if (prefs.getBoolean("workout_reminders_enabled", false)) {
            schedule(context, "Workout", prefs.getString("workout_reminder_time", "17:00") ?: "17:00", 1004)
        } else cancel(context, 1004)
    }

    private fun schedule(context: Context, label: String, time: String, id: Int) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (label != "Workout") add(Calendar.MINUTE, -60)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, MealReminderReceiver::class.java).putExtra("label", label).putExtra("time", time)
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    private fun cancel(context: Context, id: Int) {
        val intent = Intent(context, MealReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            MealReminderScheduler.scheduleAll(context)
            return
        }
        val prefs = context.getSharedPreferences("home_fitness_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", false)) return
        val label = intent.getStringExtra("label") ?: return
        val time = intent.getStringExtra("time") ?: ""
        if (label == "Workout" && !prefs.getBoolean("workout_reminders_enabled", false)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel("home_fitness_updates", "Home Fitness updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val title = if (label == "Workout") "Workout reminder" else "$label reminder"
        val message = if (label == "Workout") "It's time for your planned workout." else "Don't forget to prepare your $label. Your meal is scheduled for $time."
        val notification = NotificationCompat.Builder(context, "home_fitness_updates")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(label.hashCode(), notification) } catch (_: SecurityException) {}
    }
}
