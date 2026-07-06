package com.tertiaryinfotech.jingangjing.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tertiaryinfotech.jingangjing.AppStore
import com.tertiaryinfotech.jingangjing.MainActivity
import com.tertiaryinfotech.jingangjing.R
import com.tertiaryinfotech.jingangjing.data.SutraData
import java.util.Calendar

/**
 * Schedules an optional daily reminder that surfaces 每日一偈 (verse of the day)
 * as a local notification — no server, no account, fully on-device.
 * Mirrors the iOS NotificationManager.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "daily-sutra-reminder"
    private const val REQUEST_CODE = 1001

    /** (Re)schedule the repeating daily reminder at the given time. */
    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        ensureChannel(context)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "每日一偈", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "每天定时推送一句金刚经名句"
            },
        )
    }
}

/** Posts the 每日一偈 notification when the daily alarm fires. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.ensureChannel(context)
        val verse = SutraData.verseFor()
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("每日一偈 · 金刚经")
            .setContentText(verse.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(verse.text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(1, notification) }
    }
}

/** Re-arms the daily reminder after a reboot (alarms don't survive restarts). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val (enabled, hour, minute) = AppStore.reminderPrefs(context)
        if (enabled) ReminderScheduler.scheduleDaily(context, hour, minute)
    }
}
