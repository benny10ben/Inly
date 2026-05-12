package com.ben.inly.presentation.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Listens for alarms triggered by the system and pushes the actual notification to the user.
 * Runs independently in the background, even if the app is completely closed.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteTitle = intent.getStringExtra("note_title") ?: "Reminder"
        val blockText = intent.getStringExtra("block_text") ?: "You have a task to check."
        val notificationId = intent.getStringExtra("block_id")?.hashCode() ?: 1

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "inly_reminders"

        // Modern Android requires notifications to be assigned to a specific channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Swap this with your actual app icon resource later!
            .setContentTitle(noteTitle)
            .setContentText(blockText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}