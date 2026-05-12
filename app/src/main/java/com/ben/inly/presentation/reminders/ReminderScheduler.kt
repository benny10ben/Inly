package com.ben.inly.presentation.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles scheduling and canceling background alarms for task reminders.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Sets up a system alarm that will wake up the app and trigger the ReminderReceiver.
     * Includes fallback logic for newer Android versions to prevent crashes if exact alarm permissions are denied.
     */
    fun schedule(blockId: String, noteTitle: String, text: String, timestamp: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("block_id", blockId)
            putExtra("note_title", noteTitle)
            putExtra("block_text", text)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            blockId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ requires explicit permission to fire alarms at an exact millisecond.
        // If the user denied it, this falls back to a standard alarm to ensure the notification still delivers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timestamp,
                pendingIntent
            )
        }
    }

    fun cancel(blockId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            blockId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}