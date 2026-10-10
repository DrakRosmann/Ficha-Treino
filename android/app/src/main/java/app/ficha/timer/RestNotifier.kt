package app.ficha.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.ficha.FichaApp
import app.ficha.MainActivity
import app.ficha.R
import app.ficha.data.ActiveWorkout
import app.ficha.logic.addRest
import app.ficha.logic.clock
import app.ficha.logic.stats

/**
 * Descanso e treino em andamento fora do app:
 * - notificação contínua com o cronômetro (contagem regressiva no descanso). No Android 16+ ela pede para
 *   virar "Live Update" (chip na barra de status, como as Live Activities do iPhone);
 * - alarme exato no fim do descanso, com som e vibração, mesmo com a tela apagada;
 * - botões −15 s, +15 s e Pular direto na notificação.
 */
object RestNotifier {
    private const val CH_ONGOING = "treino"
    private const val CH_ALARM = "descanso_fim"
    private const val CH_ALARM_QUIET = "descanso_fim_vibrar"
    private const val ID_ONGOING = 1
    private const val ID_ALARM = 2

    const val ACTION_REST_END = "app.ficha.REST_END"
    const val ACTION_REST_ADD = "app.ficha.REST_ADD"
    const val ACTION_REST_SKIP = "app.ficha.REST_SKIP"
    const val EXTRA_OPEN_WORKOUT = "abrir_treino"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ONGOING, context.getString(R.string.channel_rest), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_rest_desc)
                setShowBadge(false)
            },
        )
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 120, 220, 120, 400)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM_QUIET, "Fim do descanso (só vibrar)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Usado quando o som do descanso está desligado em Ajustes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 120, 220, 120, 400)
                setSound(null, null)
            },
        )
    }

    fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Deixa notificação e alarme iguais ao estado do treino. */
    fun sync(context: Context, active: ActiveWorkout?, sound: Boolean) {
        val nm = NotificationManagerCompat.from(context)
        val rest = active?.rest?.takeIf { it.end > System.currentTimeMillis() }
        if (rest != null) scheduleAlarm(context, rest.end, sound) else cancelAlarm(context)
        if (active == null) {
            nm.cancel(ID_ONGOING)
            nm.cancel(ID_ALARM)
            return
        }
        if (rest != null) nm.cancel(ID_ALARM)
        post(context, ID_ONGOING, if (rest != null) restNotification(context, active, rest.end, rest.total) else workoutNotification(context, active))
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, n: android.app.Notification) {
        if (canNotify(context)) NotificationManagerCompat.from(context).notify(id, n)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(EXTRA_OPEN_WORKOUT, true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, action: String, delta: Int = 0, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context, code,
        Intent(context, RestAlarmReceiver::class.java).setAction(action).putExtra("d", delta),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun base(context: Context, active: ActiveWorkout) = NotificationCompat.Builder(context, CH_ONGOING)
        .setSmallIcon(R.drawable.ic_stat_ficha)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(openApp(context))
        .setRequestPromotedOngoing(true)

    private fun restNotification(context: Context, active: ActiveWorkout, end: Long, total: Int): android.app.Notification {
        val left = ((end - System.currentTimeMillis()) / 1000.0)
        return base(context, active)
            .setContentTitle("Descanso")
            .setContentText(active.name)
            .setWhen(end)
            .setChronometerCountDown(true)
            .setShortCriticalText(clock(left))
            .addAction(0, "−15 s", action(context, ACTION_REST_ADD, -15, 11))
            .addAction(0, "+15 s", action(context, ACTION_REST_ADD, 15, 12))
            .addAction(0, "Pular", action(context, ACTION_REST_SKIP, code = 13))
            .build()
    }

    private fun workoutNotification(context: Context, active: ActiveWorkout): android.app.Notification {
        val st = active.stats()
        return base(context, active)
            .setContentTitle(active.name)
            .setContentText("${st.done} de ${st.total} séries")
            .setWhen(active.start)
            .setChronometerCountDown(false)
            .setProgress(st.total.coerceAtLeast(1), st.done, false)
            .build()
    }

    /** O descanso acabou: aviso com som/vibração e a notificação volta a mostrar o treino. */
    fun restEnded(context: Context, sound: Boolean) {
        val active = FichaApp.store.value.active
        if (active != null) {
            post(
                context, ID_ALARM,
                NotificationCompat.Builder(context, if (sound) CH_ALARM else CH_ALARM_QUIET)
                    .setSmallIcon(R.drawable.ic_stat_ficha)
                    .setContentTitle("Descanso terminado")
                    .setContentText("Próxima série!")
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setTimeoutAfter(20_000)
                    .setContentIntent(openApp(context))
                    .build(),
            )
            post(context, ID_ONGOING, workoutNotification(context, active))
        }
    }

    private fun alarmIntent(context: Context, sound: Boolean) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, RestAlarmReceiver::class.java).setAction(ACTION_REST_END).putExtra("som", sound),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun scheduleAlarm(context: Context, at: Long, sound: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context, sound)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, true))
    }
}

/** Recebe o alarme do fim do descanso e os botões da notificação. */
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = FichaApp.store
        when (intent.action) {
            RestNotifier.ACTION_REST_END -> {
                val rest = store.value.active?.rest
                // O descanso pode ter mudado (+15 s) depois que o alarme foi marcado
                if (rest != null && rest.end - System.currentTimeMillis() > 1500) {
                    RestNotifier.sync(context, store.value.active, store.value.settings.sound)
                    return
                }
                store.update { d -> d.active?.let { d.copy(active = it.copy(rest = null)) } ?: d }
                RestNotifier.restEnded(context, intent.getBooleanExtra("som", true))
                store.flush(now = true)
            }
            RestNotifier.ACTION_REST_ADD -> {
                store.update { d -> d.active?.let { d.copy(active = it.addRest(intent.getIntExtra("d", 0))) } ?: d }
                RestNotifier.sync(context, store.value.active, store.value.settings.sound)
                store.flush(now = true)
            }
            RestNotifier.ACTION_REST_SKIP -> {
                store.update { d -> d.active?.let { d.copy(active = it.copy(rest = null)) } ?: d }
                RestNotifier.sync(context, store.value.active, store.value.settings.sound)
                store.flush(now = true)
            }
        }
    }
}
