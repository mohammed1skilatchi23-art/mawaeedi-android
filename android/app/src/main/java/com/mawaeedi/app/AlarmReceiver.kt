package com.mawaeedi.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("alarm_id", intent.getStringExtra("alarm_id") ?: "default")
            putExtra("title", intent.getStringExtra("title") ?: "منبه مواعيدي")
        }
        context.startForegroundService(serviceIntent)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AlarmScheduler.rescheduleSavedAlarms(context)
        }
    }
}


class AlarmSoundService : android.app.Service() {
    private var player: android.media.MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val channelId = "mawaeedi_alarms"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(android.app.NotificationChannel(channelId, "منبهات مواعيدي", android.app.NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.mawaeedi.app.R.mipmap.ic_launcher)
            .setContentTitle("منبه مواعيدي")
            .setContentText("حان وقت الموعد")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)
        player = android.media.MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)?.apply {
            isLooping = true
            start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        player?.stop()
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}

object ExactAlarmHelper {
    fun canSchedule(context: Context): Boolean = android.os.Build.VERSION.SDK_INT < 31 ||
        (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
}

fun scheduleExactAlarm(context: Context, triggerAtMillis: Long, id: String, title: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarm_id", id)
        putExtra("title", title)
    }
    val pending = android.app.PendingIntent.getBroadcast(context, id.hashCode(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
    if (android.os.Build.VERSION.SDK_INT >= 23) alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
    else alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
} 
