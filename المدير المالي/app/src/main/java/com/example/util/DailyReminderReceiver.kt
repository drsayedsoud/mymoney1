package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "daily_finance_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكير كتابة البيانات المالية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "قناة تنبيهات التذكير اليومي لتسجيل المصاريف اليومية"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notifyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using safe standard system notification icon
            .setContentTitle("المدير المالي الذكي 📝")
            .setContentText("الساعة الآن 11 بالليل! لا تنسى تسجيل مصروفاتك ودخلك اليومي لمتابعة ميزانيتك الذكية.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4001, notification)
    }
}
