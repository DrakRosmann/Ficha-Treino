package app.ficha.logic

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** 0 = Dom … 6 = Sáb (como no JavaScript). */
val DAY = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
val DAY_LONG = listOf("Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado")
val MONTH = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
/** Semana começando na segunda. */
val WEEK_ORDER = listOf(1, 2, 3, 4, 5, 6, 0)
val REST_OPTIONS = listOf(0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)

const val DAY_MS = 86_400_000L

/** Texto digitado → número ("12,5" vale 12.5). */
fun num(v: String?): Double? {
    if (v.isNullOrBlank()) return null
    return v.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

private val nfCache = HashMap<Int, NumberFormat>()

/** Número com até [d] casas decimais, no formato brasileiro (1.234,5). */
fun fmt(n: Double?, d: Int = 1): String {
    if (n == null) return ""
    val f = nfCache.getOrPut(d) { NumberFormat.getNumberInstance(PT_BR).apply { maximumFractionDigits = d; minimumFractionDigits = 0 } }
    return f.format(if (abs(n) < 1e-9) 0.0 else n)
}

fun fmt(n: Int): String = fmt(n.toDouble(), 0)
fun fmtInt(n: Double?): String = fmt((n ?: 0.0).roundToLong().toDouble(), 0)

/** Número para preencher campos: sem separador de milhar, vírgula decimal, até 2 casas. */
fun fmtIn(n: Double): String {
    val r = Math.round(n * 100) / 100.0
    return (if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()).replace('.', ',')
}

fun clock(secIn: Double): String {
    val sec = max(0, Math.round(secIn).toInt())
    val h = sec / 3600
    val m = sec % 3600 / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun fmtDur(ms: Long): String {
    val m = Math.round(ms / 60000.0)
    val h = m / 60
    return if (h > 0) "${h}h%02d".format(m % 60) else "$m min"
}

fun fmtRest(s: Int): String = when {
    s <= 0 -> "sem descanso"
    s < 60 -> "${s}s"
    else -> clock(s.toDouble())
}

val zone: ZoneId get() = ZoneId.systemDefault()
fun dateOf(t: Long): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
fun LocalDate.millis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
/** 0 = Dom … 6 = Sáb */
fun LocalDate.jsDay(): Int = dayOfWeek.value % 7

fun startOfDay(t: Long): Long = dateOf(t).millis()
fun startOfWeek(t: Long): Long = dateOf(t).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).millis()
fun addDays(t: Long, n: Int): Long = dateOf(t).plusDays(n.toLong()).millis()

fun dateShort(t: Long): String = dateOf(t).let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }
fun dateLong(t: Long): String = dateOf(t).let { "${DAY_LONG[it.jsDay()]}, ${it.dayOfMonth} de ${MONTH[it.monthValue - 1]}" }
fun timeHM(t: Long): String = Instant.ofEpochMilli(t).atZone(zone).let { "%02d:%02d".format(it.hour, it.minute) }

fun relDay(t: Long): String {
    val diff = ChronoUnit.DAYS.between(dateOf(t), LocalDate.now(zone))
    return when {
        diff == 0L -> "Hoje"
        diff == 1L -> "Ontem"
        diff in 2..6 -> "Há $diff dias"
        else -> dateShort(t)
    }
}

fun daysLabel(days: List<Int>): String = when {
    days.isEmpty() -> "Sem dia fixo"
    days.size == 7 -> "Todos os dias"
    else -> WEEK_ORDER.filter { it in days }.joinToString(" · ") { DAY[it] }
}

/** Letra da ficha na ordem do programa (A, B, C…). */
fun letter(i: Int): String = ('A' + i % 26).toString()

fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

/** Busca sem acento e sem maiúsculas. */
fun norm(s: String): String = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "").lowercase(PT_BR)
