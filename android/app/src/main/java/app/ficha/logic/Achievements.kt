package app.ficha.logic

import app.ficha.data.AppData
import java.time.LocalDate
import kotlin.math.max

/* ================= Conquistas (port do share.js) =================
   tier: 1 bronze · 2 prata · 3 ouro · 4 especial. p(st) → [atual, meta] para a barra de progresso. */

val ACH_GROUPS = listOf("Treinos", "Constância", "Força", "Recordes", "Dieta", "Corpo")

class Ach(val id: String, val g: Int, val e: String, val t: Int, val n: String, val d: String, val p: (AchStats) -> Pair<Double, Double>)

class AchStats(
    val n: Double, val vol: Double, val prs: Double, val ss: Double, val early: Double, val late: Double,
    val streak: Double, val perfect: Double, val bench: Double, val squat: Double, val dead: Double,
    val dietRun: Double, val protRun: Double, val waterRun: Double, val body: Double, val photoDays: Double,
)

val ACH = listOf(
    Ach("w1", 0, "🏁", 1, "Primeiro treino", "Registrou o primeiro treino") { it.n to 1.0 },
    Ach("w10", 0, "💪", 1, "10 treinos", "Dez treinos registrados") { it.n to 10.0 },
    Ach("w50", 0, "🔥", 2, "50 treinos", "Cinquenta treinos registrados") { it.n to 50.0 },
    Ach("w100", 0, "💯", 3, "100 treinos", "Cem treinos registrados") { it.n to 100.0 },
    Ach("w250", 0, "🏛️", 4, "250 treinos", "Duzentos e cinquenta treinos") { it.n to 250.0 },
    Ach("v10", 0, "🧱", 1, "10 toneladas", "10.000 kg levantados no total") { it.vol / 1000 to 10.0 },
    Ach("v100", 0, "🏗️", 2, "100 toneladas", "100.000 kg levantados no total") { it.vol / 1000 to 100.0 },
    Ach("v1000", 0, "🌋", 4, "1.000 toneladas", "Um milhão de quilos levantados") { it.vol / 1000 to 1000.0 },
    Ach("ss1", 0, "🔗", 1, "Sem pausa", "Fez uma supersérie ou circuito") { it.ss to 1.0 },
    Ach("early", 0, "🌅", 1, "Madrugador", "Começou um treino antes das 6h30") { it.early to 1.0 },
    Ach("late", 0, "🌙", 1, "Coruja", "Começou um treino depois das 22h") { it.late to 1.0 },
    Ach("st4", 1, "📅", 1, "1 mês firme", "4 semanas seguidas treinando") { it.streak to 4.0 },
    Ach("st12", 1, "🗓️", 2, "3 meses firme", "12 semanas seguidas treinando") { it.streak to 12.0 },
    Ach("st26", 1, "⛰️", 3, "Meio ano", "26 semanas seguidas treinando") { it.streak to 26.0 },
    Ach("st52", 1, "👑", 4, "Um ano inteiro", "52 semanas seguidas treinando") { it.streak to 52.0 },
    Ach("pw", 1, "✅", 2, "Semana perfeita", "Treinou em todos os dias planejados da semana") { it.perfect to 1.0 },
    Ach("bench1", 2, "🏋️", 2, "Supino com o próprio peso", "Supino reto com barra com carga igual ao seu peso") { it.bench to 1.0 },
    Ach("squat15", 2, "🦵", 3, "Agachamento 1,5×", "Agachamento livre com 1,5 vez o seu peso") { it.squat to 1.5 },
    Ach("dead2", 2, "⚡", 3, "Terra 2×", "Levantamento terra com o dobro do seu peso") { it.dead to 2.0 },
    Ach("pr1", 3, "⭐", 1, "Primeiro recorde", "Bateu um recorde pessoal") { it.prs to 1.0 },
    Ach("pr10", 3, "🌟", 2, "10 recordes", "Dez recordes pessoais") { it.prs to 10.0 },
    Ach("pr50", 3, "🏆", 3, "50 recordes", "Cinquenta recordes pessoais") { it.prs to 50.0 },
    Ach("d7", 4, "🥗", 1, "Uma semana na dieta", "7 dias seguidos registrando a alimentação") { it.dietRun to 7.0 },
    Ach("d30", 4, "🍽️", 3, "Um mês na dieta", "30 dias seguidos registrando a alimentação") { it.dietRun to 30.0 },
    Ach("prot7", 4, "🥩", 2, "Proteína em dia", "7 dias seguidos batendo a meta de proteína") { it.protRun to 7.0 },
    Ach("water7", 4, "💧", 1, "Hidratado", "7 dias seguidos bebendo a meta de água") { it.waterRun to 7.0 },
    Ach("body10", 5, "⚖️", 1, "De olho na balança", "Dez registros de medidas") { it.body to 10.0 },
    Ach("photo3", 5, "📸", 1, "Antes e depois", "Fotos do progresso em 3 datas") { it.photoDays to 3.0 },
)
val ACH_BY_ID = ACH.associateBy { it.id }

/** Cores do selo (claro, escuro) por nível */
val TIER = listOf(null, 0xFFE7A774 to 0xFF9A5B2E, 0xFFE9EEF3 to 0xFF8D99A6, 0xFFFFE07A to 0xFFC8961C, 0xFFC9B6FF to 0xFF6C4BD8)
val TIER_NAME = listOf("", "Bronze", "Prata", "Ouro", "Especial")

/** Maior sequência de dias seguidos (chaves AAAA-MM-DD). */
fun runOfDays(keys: Collection<String>): Int {
    val ds = keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSortedSet()
    var best = 0
    var cur = 0
    var prev: LocalDate? = null
    for (d in ds) {
        cur = if (prev != null && prev.plusDays(1) == d) cur + 1 else 1
        best = max(best, cur)
        prev = d
    }
    return best
}

private fun AppData.bestLoad(exId: String): Double {
    var m = 0.0
    for (s in sessions) for (e in s.exercises) if (e.exId == exId) for (x in e.sets) if (!x.warm && (x.b ?: 0.0) >= 1) m = max(m, x.a ?: 0.0)
    return m
}

fun AppData.achStats(): AchStats {
    var vol = 0.0
    var prs = 0.0
    var ss = 0.0
    var early = 0.0
    var late = 0.0
    val weeks = sortedSetOf<Long>()
    for (s in sessions) {
        vol += s.volume()
        prs += s.prs.size
        if (s.exercises.any { it.ss != null }) ss = 1.0
        val z = java.time.Instant.ofEpochMilli(s.start).atZone(zone)
        val h = z.hour + z.minute / 60.0
        if (h < 6.5 && h >= 3) early = 1.0
        if (h >= 22) late = 1.0
        weeks += startOfWeek(s.start)
    }
    // Semanas seguidas (a maior sequência)
    var best = 0
    var cur = 0
    var prev: Long? = null
    for (w in weeks) {
        cur = if (prev != null && addDays(prev, 7) == w) cur + 1 else 1
        best = max(best, cur)
        prev = w
    }
    // Semana perfeita: todos os dias planejados (2 ou mais) feitos, nas últimas 12 semanas
    val plan = routines.filter { isScheduled(it) }.flatMap { it.days }.toSet()
    var perfect = 0.0
    if (plan.size >= 2) {
        val today = startOfDay(System.currentTimeMillis())
        for (k in 0 until 12) {
            val wk = addDays(startOfWeek(today), -7 * k)
            val days = plan.map { d -> addDays(wk, (d + 6) % 7) }
            if (days.all { t -> t <= today && sessionsInRange(t, addDays(t, 1)).isNotEmpty() }) { perfect = 1.0; break }
        }
    }
    // Força relativa ao peso atual
    val bw = dietWeight()
    val days = food.days
    val tg = dietTargets()
    val wGoal = waterGoal()
    return AchStats(
        n = sessions.size.toDouble(), vol = vol, prs = prs, ss = ss, early = early, late = late,
        streak = best.toDouble(), perfect = perfect,
        bench = if (bw != null) bestLoad("supino-reto-barra") / bw else 0.0,
        squat = if (bw != null) bestLoad("agachamento") / bw else 0.0,
        dead = if (bw != null) bestLoad("terra") / bw else 0.0,
        dietRun = runOfDays(days.filter { it.value.e.isNotEmpty() }.keys).toDouble(),
        protRun = if (tg != null) runOfDays(days.filter { (_, d) -> d.e.sumOf { it.p ?: 0.0 } >= (tg.p ?: 0.0) * 0.95 }.keys).toDouble() else 0.0,
        waterRun = runOfDays(days.filter { it.value.w >= wGoal }.keys).toDouble(),
        body = body.size.toDouble(),
        photoDays = photos.map { startOfDay(it.t) }.toSet().size.toDouble(),
    )
}

fun achDone(a: Ach, st: AchStats): Boolean = a.p(st).let { (c, g) -> c >= g }

/** Marca as novas conquistas. Na primeira vez (dados de antes desta versão), marca em silêncio (valor 1). */
fun AppData.achCheck(): Pair<AppData, List<String>> {
    val st = achStats()
    val first = ach == null
    val cur = (ach ?: emptyMap()).toMutableMap()
    val fresh = mutableListOf<String>()
    for (a in ACH) if (cur[a.id] == null && achDone(a, st)) {
        cur[a.id] = if (first) 1 else System.currentTimeMillis()
        if (!first) fresh += a.id
    }
    return if (first || fresh.isNotEmpty()) copy(ach = cur) to fresh else this to emptyList()
}

fun achProgressText(a: Ach, st: AchStats): String {
    val (c, g) = a.p(st)
    if (a.id in listOf("bench1", "squat15", "dead2")) return if (c > 0) "Seu melhor hoje: ${fmt(c, 2)}× o seu peso" else "Registre o exercício e o seu peso no Corpo"
    if (a.id.startsWith("v")) return "${fmt(c, if (c < 10) 1 else 0)} de ${fmtInt(g)} toneladas"
    if (g == 1.0) return "Ainda não"
    return "${fmtInt(minOf(c, g))} de ${fmtInt(g)}"
}
