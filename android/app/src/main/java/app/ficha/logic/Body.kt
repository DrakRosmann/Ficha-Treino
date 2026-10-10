package app.ficha.logic

import app.ficha.data.AppData
import app.ficha.data.BodyEntry
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/* ================= Corpo: medidas e composição corporal (port do body.js) ================= */

/** [campo, nome, unidade, seção (comp | med), melhor direção (up | down | goal | null), casas, dica de como medir] */
class BodyField(val key: String, val name: String, val unit: String, val section: String, val better: String?, val dec: Int, val tip: String)

val BODY_FIELDS = listOf(
    BodyField("peso", "Peso", "kg", "comp", "goal", 1, ""),
    BodyField("gordura", "Gordura", "%", "comp", "down", 1, "Da balança de bioimpedância ou avaliação física"),
    BodyField("musculo", "Massa muscular", "kg", "comp", "up", 1, ""),
    BodyField("agua", "Água", "%", "comp", null, 1, ""),
    BodyField("visceral", "Gordura visceral", "nível", "comp", "down", 0, ""),
    BodyField("ossea", "Massa óssea", "kg", "comp", null, 1, ""),
    BodyField("tmb", "Metabolismo basal", "kcal", "comp", null, 0, ""),
    BodyField("pescoco", "Pescoço", "cm", "med", null, 1, "Logo abaixo do pomo de adão"),
    BodyField("ombros", "Ombros", "cm", "med", "up", 1, "Volta completa na parte mais larga dos ombros"),
    BodyField("peito", "Peito", "cm", "med", "up", 1, "Na altura dos mamilos, ao fim de uma expiração"),
    BodyField("braco", "Braço", "cm", "med", "up", 1, "Meio do braço, contraído"),
    BodyField("antebraco", "Antebraço", "cm", "med", "up", 1, "Parte mais grossa, braço estendido"),
    BodyField("cintura", "Cintura", "cm", "med", "down", 1, "Parte mais fina do tronco, sem contrair"),
    BodyField("abdomen", "Abdômen", "cm", "med", "down", 1, "Na altura do umbigo, sem contrair"),
    BodyField("quadril", "Quadril", "cm", "med", null, 1, "Parte mais larga dos glúteos, pés juntos"),
    BodyField("coxa", "Coxa", "cm", "med", "up", 1, "Logo abaixo do glúteo"),
    BodyField("panturrilha", "Panturrilha", "cm", "med", "up", 1, "Parte mais grossa, em pé"),
)
val BF = BODY_FIELDS.associateBy { it.key }
/** Rótulos curtos do formulário */
val BODY_SHORT = mapOf("musculo" to "Músculo", "visceral" to "Visceral", "ossea" to "Ossos", "tmb" to "TMB")

class BodyMeta(val name: String, val unit: String, val better: String?, val dec: Int)

/** Estimativa de % de gordura pelo método da Marinha dos EUA (fita métrica). */
fun AppData.navyFat(e: BodyEntry): Double? {
    val h = bodyHeight() ?: return null
    val n = e["pescoco"] ?: return null
    return when (sex()) {
        "m" -> {
            val w = e["abdomen"] ?: e["cintura"] ?: return null
            if (w > n) 495 / (1.0324 - 0.19077 * log10(w - n) + 0.15456 * log10(h)) - 450 else null
        }
        "f" -> {
            val w = e["cintura"] ?: return null
            val q = e["quadril"] ?: return null
            if (w + q > n) 495 / (1.29579 - 0.35004 * log10(w + q - n) + 0.221 * log10(h)) - 450 else null
        }
        else -> null
    }
}

/** Métricas calculadas */
val BODY_DERIVED = mapOf(
    "imc" to BodyMeta("IMC", "", null, 1),
    "magra" to BodyMeta("Massa magra", "kg", "up", 1),
    "gordkg" to BodyMeta("Massa de gordura", "kg", "down", 1),
    "navy" to BodyMeta("Gordura estimada", "%", "down", 1),
    "rcq" to BodyMeta("Cintura / quadril", "", "down", 2),
)

val BODY_CHART_ORDER = listOf(
    "peso", "gordura", "navy", "musculo", "magra", "gordkg", "imc", "cintura", "abdomen", "quadril", "peito", "ombros",
    "braco", "antebraco", "coxa", "panturrilha", "pescoco", "rcq", "agua", "visceral", "ossea", "tmb",
)

val BODY_RANGES = listOf("30" to "30 dias", "90" to "3 meses", "365" to "1 ano", "all" to "Tudo")

fun bmeta(k: String): BodyMeta = BF[k]?.let { BodyMeta(it.name, it.unit, it.better, it.dec) } ?: BODY_DERIVED.getValue(k)

fun AppData.bval(k: String, e: BodyEntry): Double? = if (BF.containsKey(k)) e[k] else when (k) {
    "imc" -> bodyHeight()?.let { h -> e["peso"]?.let { it / (h / 100).pow(2) } }
    "magra" -> e["peso"]?.let { p -> e["gordura"]?.let { p * (1 - it / 100) } }
    "gordkg" -> e["peso"]?.let { p -> e["gordura"]?.let { p * it / 100 } }
    "navy" -> navyFat(e)
    "rcq" -> e["cintura"]?.let { c -> e["quadril"]?.let { c / it } }
    else -> null
}

fun AppData.bodyEntries(): List<BodyEntry> = body.sortedBy { it.t }

fun imcLabel(v: Double) = when {
    v < 18.5 -> "abaixo do peso"
    v < 25 -> "peso normal"
    v < 30 -> "sobrepeso"
    else -> "obesidade"
}

fun fmtDelta(v: Double?, dec: Int): String =
    if (v == null || abs(v) < 10.0.pow(-dec) / 2) "=" else "${if (v > 0) "+" else "−"}${fmt(abs(v), dec)}"

/** Direção boa da métrica; para o peso depende da meta (perder ou ganhar). */
fun AppData.betterOf(k: String): String? {
    val b = bmeta(k).better
    if (b != "goal") return b
    val g = bodyGoal.peso ?: return null
    val first = bodyEntries().firstOrNull { it["peso"] != null } ?: return null
    return if (g < first["peso"]!!) "down" else "up"
}

/** true = melhorou, false = piorou, null = neutro */
fun AppData.deltaGood(k: String, d: Double): Boolean? {
    val b = betterOf(k) ?: return null
    if (d == 0.0) return null
    return (b == "up") == (d > 0)
}

/** Último valor de uma métrica (e o registro dele). */
fun AppData.latestOf(k: String, all: List<BodyEntry> = bodyEntries()): Pair<Double, BodyEntry>? {
    for (i in all.indices.reversed()) bval(k, all[i])?.let { return it to all[i] }
    return null
}

fun AppData.earliestOf(k: String, all: List<BodyEntry> = bodyEntries()): Double? {
    for (e in all) bval(k, e)?.let { return it }
    return null
}

/** Registro do corpo mais próximo de uma data (até 4 dias de diferença). */
fun AppData.bodyNear(t: Long, field: String): Double? =
    body.filter { it[field] != null && abs(it.t - t) <= 4 * DAY_MS }.minByOrNull { abs(it.t - t) }?.get(field)
