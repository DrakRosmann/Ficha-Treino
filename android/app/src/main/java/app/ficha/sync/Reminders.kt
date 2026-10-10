package app.ficha.sync

import android.annotation.SuppressLint
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
import app.ficha.FichaApp
import app.ficha.MainActivity
import app.ficha.R
import app.ficha.data.AppData
import app.ficha.data.Foods
import app.ficha.logic.DAY_MS
import app.ficha.logic.addDays
import app.ficha.logic.dayKey
import app.ficha.logic.fmt
import app.ficha.logic.isScheduled
import app.ficha.logic.sessionsInRange
import app.ficha.logic.startOfDay
import app.ficha.logic.waterGoal
import app.ficha.timer.RestNotifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Lembretes por notificação, agendados no próprio aparelho (no Android não precisam do servidor).
 * As mesmas opções do PWA: treino do dia (pelos dias das fichas), água, refeições, pesagem semanal e backup.
 * O que você já fez no dia não é lembrado.
 */
object Reminders {
    private const val PREFS = "lembretes"
    private const val CHANNEL = "lembretes"
    const val EXTRA_ROUTE = "rota"

    @Serializable data class Treino(val on: Boolean = true, val hm: String = "08:00")
    @Serializable data class Agua(val on: Boolean = false, val from: Int = 9, val to: Int = 21, val every: Int = 3)
    @Serializable data class Meals(val on: Boolean = false, val hm: List<String> = listOf("08:00", "12:30", "16:00", "20:00"))
    @Serializable data class Weekly(val on: Boolean = true, val day: Int = 1, val hm: String = "07:30")
    @Serializable data class Rem(
        val treino: Treino = Treino(),
        val agua: Agua = Agua(),
        val meals: Meals = Meals(),
        val peso: Weekly = Weekly(),
        val backup: Weekly = Weekly(on = false, day = 0, hm = "20:00"),
    )
    @Serializable data class Config(val on: Boolean = false, val rem: Rem = Rem())

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun config(context: Context): Config = runCatching {
        json.decodeFromString(Config.serializer(), context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("cfg", null)!!)
    }.getOrDefault(Config())

    fun save(context: Context, c: Config) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("cfg", json.encodeToString(Config.serializer(), c)).apply()
        reschedule(context)
    }

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Treino do dia, água, refeições, pesagem e backup"
            },
        )
    }

    /** Um lembrete: dias da semana (0 = dom), horário, texto e a aba que abre. */
    class Item(val id: String, val kind: String, val days: List<Int>, val hm: String, val title: String, val body: String, val route: String)

    private fun hm(h: Double) = "%02d:%02d".format(h.toInt(), Math.round(h % 1 * 60).toInt())

    fun items(d: AppData, R: Rem): List<Item> {
        val out = mutableListOf<Item>()
        val all = (0..6).toList()
        if (R.treino.on) {
            val sched = d.routines.filter { it.items.isNotEmpty() && d.isScheduled(it) }
            for (day in all) {
                val rs = sched.filter { day in it.days }
                if (rs.isEmpty()) continue
                val n = rs.sumOf { it.items.size }
                out += Item("treino$day", "treino", listOf(day), R.treino.hm, "Dia de treinar 💪", "${rs.joinToString(" + ") { it.name.ifBlank { "Treino" } }} · $n exercício${if (n > 1) "s" else ""}", "hoje")
            }
        }
        if (R.agua.on) {
            var h = R.agua.from
            while (h <= R.agua.to) {
                out += Item("agua$h", "agua", all, hm(h.toDouble()), "Hora de beber água 💧", "Meta de hoje: ${fmt(d.waterGoal() / 1000.0, 1)} L. Registre na Dieta.", "dieta")
                h += maxOf(1, R.agua.every)
            }
        }
        if (R.meals.on) R.meals.hm.forEachIndexed { m, t ->
            if (t.isNotEmpty()) out += Item("meal$m", "meal$m", all, t, "${Foods.MEALS[m]} 🍽️", "Registre o que você comeu na Dieta.", "dieta")
        }
        if (R.peso.on) out += Item("peso", "peso", listOf(R.peso.day), R.peso.hm, "Pesagem da semana ⚖️", "De manhã, em jejum e depois de ir ao banheiro. Registre no Corpo.", "corpo")
        if (R.backup.on) out += Item("backup", "backup", listOf(R.backup.day), R.backup.hm, "Backup da semana", "Salve um backup dos seus treinos (Ajustes → Exportar backup).", "ajustes")
        return out
    }

    /** O que já foi feito hoje: não lembra disso. */
    private fun doneToday(context: Context, d: AppData): Set<String> {
        val t0 = startOfDay(System.currentTimeMillis())
        val kinds = mutableSetOf<String>()
        if (d.sessionsInRange(t0, addDays(t0, 1)).isNotEmpty()) kinds += "treino"
        d.food.days[dayKey(t0)]?.let { day ->
            (0..3).forEach { m -> if (day.e.any { it.m == m }) kinds += "meal$m" }
            if (day.w >= d.waterGoal()) kinds += "agua"
        }
        if (d.body.any { it["peso"] != null && it.t >= addDays(t0, -6) }) kinds += "peso"
        if (System.currentTimeMillis() - d.settings.lastBackup < 6 * DAY_MS || CloudSync.on) kinds += "backup"
        return kinds
    }

    /** Próximo horário de cada lembrete (a partir de agora). */
    private fun nextTimes(items: List<Item>, from: LocalDateTime): List<Pair<Item, LocalDateTime>> = items.mapNotNull { it ->
        val t = runCatching { LocalTime.parse(it.hm) }.getOrNull() ?: return@mapNotNull null
        (0..7).asSequence().map { k -> from.toLocalDate().plusDays(k.toLong()).atTime(t) }
            .firstOrNull { dt -> dt.isAfter(from) && (dt.dayOfWeek.value % 7) in it.days }?.let { dt -> it to dt }
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 50, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Agenda o próximo lembrete (um alarme só; ao tocar, agenda o seguinte). */
    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        val c = config(context)
        am.cancel(pi)
        if (!c.on) return
        val next = nextTimes(items(FichaApp.store.value, c.rem), LocalDateTime.now()).minByOrNull { it.second } ?: return
        val at = next.second.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Hora de algum lembrete: avisa os que vencem agora (e não foram feitos) e agenda o próximo. */
    @SuppressLint("MissingPermission")
    fun fire(context: Context) {
        val c = config(context)
        if (c.on) {
            val d = FichaApp.store.value
            val now = LocalDateTime.now()
            val today = LocalDate.now()
            val done = doneToday(context, d)
            val due = items(d, c.rem).filter { it ->
                val t = runCatching { LocalTime.parse(it.hm) }.getOrNull() ?: return@filter false
                val dt = today.atTime(t)
                (today.dayOfWeek.value % 7) in it.days && !dt.isAfter(now) && dt.isAfter(now.minusMinutes(60))
            }
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            for (it in due) {
                val key = "last_${it.id}"
                if (prefs.getString(key, "") == today.toString()) continue
                prefs.edit().putString(key, today.toString()).apply()
                if (it.kind in done || !RestNotifier.canNotify(context)) continue
                val open = PendingIntent.getActivity(
                    context, it.id.hashCode(),
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(EXTRA_ROUTE, it.route),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                NotificationManagerCompat.from(context).notify(
                    "lembrete", it.id.hashCode(),
                    NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_ficha)
                        .setContentTitle(it.title).setContentText(it.body).setStyle(NotificationCompat.BigTextStyle().bigText(it.body))
                        .setAutoCancel(true).setContentIntent(open).build(),
                )
            }
        }
        reschedule(context)
    }

    @SuppressLint("MissingPermission")
    fun test(context: Context) {
        NotificationManagerCompat.from(context).notify(
            "lembrete", 1,
            NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_ficha)
                .setContentTitle("Ficha").setContentText("As notificações estão funcionando ✓").setAutoCancel(true).build(),
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED -> Reminders.reschedule(context)
            else -> Reminders.fire(context)
        }
    }
}
