package app.swisszen.bell

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.swisszen.MainActivity
import app.swisszen.R
import app.swisszen.SwissZenApp
import app.swisszen.data.BellEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the next mindfulness bell with AlarmManager and shows the bell notification. */
object BellAlarms {
    private const val CHANNEL_ID = "bell_v1"
    private const val NOTIFICATION_ID = 7
    const val ACTION_RING = "app.swisszen.bell.RING"
    const val ACTION_ANSWER = "app.swisszen.bell.ANSWER"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_OPEN = "open"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.bell}".toUri()
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.bell_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.bell_channel_desc)
            setSound(sound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60, 120, 60)
        }
        nm.createNotificationChannel(channel)
    }

    private fun ringIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, BellReceiver::class.java).setAction(ACTION_RING),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Recomputes the next ring from the stored config and (re)arms or cancels the alarm. */
    suspend fun reschedule(context: Context) {
        val store = (context.applicationContext as SwissZenApp).container.settings
        val config = store.bellNow()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = ringIntent(context)
        val next = BellSchedule.next(LocalDateTime.now(), config)
        if (next == null) {
            am.cancel(pi)
            store.setNextBell(null)
            return
        }
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact-but-idle-safe alarm: a mindfulness bell does not need to-the-second precision,
        // and this avoids the SCHEDULE_EXACT_ALARM permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        store.setNextBell(at)
    }

    fun notify(context: Context, eventId: Long) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 2,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN, "bell"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val answer = PendingIntent.getBroadcast(
            context, 3,
            Intent(context, BellReceiver::class.java).setAction(ACTION_ANSWER).putExtra(EXTRA_EVENT_ID, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell_notification)
            .setColor(ContextCompat.getColor(context, R.color.swiss_red))
            .setContentTitle(context.getString(R.string.bell_notif_title))
            .setContentText(context.getString(R.string.bell_notif_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.bell_notif_text)))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.bell_notif_action), answer)
            .setAutoCancel(true)
            .setTimeoutAfter(20 * 60 * 1000L)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun dismiss(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as SwissZenApp
        receiverScope.launch {
            try {
                when (intent.action) {
                    BellAlarms.ACTION_RING -> {
                        if (app.container.settings.bellNow().enabled) {
                            val id = app.container.db.bell().insert(BellEvent(rangAt = System.currentTimeMillis()))
                            BellAlarms.notify(context, id)
                        }
                        BellAlarms.reschedule(context)
                    }
                    BellAlarms.ACTION_ANSWER -> {
                        val id = intent.getLongExtra(BellAlarms.EXTRA_EVENT_ID, -1)
                        if (id > 0) app.container.db.bell().markAnswered(id)
                        BellAlarms.dismiss(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                BellAlarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
