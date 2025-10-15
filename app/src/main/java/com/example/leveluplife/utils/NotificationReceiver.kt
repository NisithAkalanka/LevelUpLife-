package com.example.leveluplife.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.leveluplife.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "hydration_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a proper icon
            .setContentTitle("Hydration Quest!")
            .setContentText("Time to refill your HP! 💧 Drink some water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}