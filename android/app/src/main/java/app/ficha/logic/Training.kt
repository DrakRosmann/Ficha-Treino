package app.ficha.logic

import app.ficha.data.ActiveExercise
import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.data.Exercise
import app.ficha.data.Program
import app.ficha.data.Routine
import app.ficha.data.Session
import app.ficha.data.SetRecord
import app.ficha.data.uid
import kotlin.math.max
import kotlin.math.min

/* ================= Exercícios ================= */

fun AppData.ex(id: String): Exercise? = Catalog.byId[id] ?: custom.find { it.id == id }
fun AppData.exName(id: String, fb: String? = null): String = ex(id)?.name ?: fb ?: "Exercício removido"
fun AppData.exKind(id: String, fb: String? = null): String = ex(id)?.kind ?: fb ?: "w"
fun AppData.allEx(): List<Exercise> = Catalog.exercises + custom

/** Músculos principais e secundários; sem cadastro (ex.: personalizados), usa os do grupo. */
fun musclesOf(ex: Exercise?): Pair<List<String>, List<String>> {
    if (ex == null) return emptyList<String>() to emptyList()
    if (ex.primary.isNotEmpty()) return ex.primary to ex.secondary
    val g = Catalog.body.groupMuscles[ex.group] ?: return emptyList<String>() to emptyList()
    val (p, s) = g.split('|').let { it[0] to it.getOrElse(1) { "" } }
    return p.split(',').filter { it.isNotEmpty() } to s.split(',').filter { it.isNotEmpty() }
}

fun filterEx(list: List<Exercise>, q: String, group: String?): List<Exercise> {
    val nq = norm(q.trim())
    return list.filter { e -> (group.isNullOrEmpty() || e.group == group) && (nq.isEmpty() || norm("${e.name} ${e.equip} ${e.en}").contains(nq)) }
}

/* ================= Volume, recordes e métricas ================= */

fun setVol(kind: String, s: SetRecord): Double = if (kind == "w" && !s.warm) (s.a ?: 0.0) * (s.b ?: 0.0) else 0.0

/** 1RM estimado pela fórmula de Epley. */
fun e1rm(w: Double?, r: Double?): Double {
    if (w == null || r == null || w == 0.0 || r == 0.0) return 0.0
    return if (r == 1.0) w else w * (1 + r / 30)
}

fun Session.volume(): Double = exercises.sumOf { e -> e.sets.sumOf { setVol(e.kind, it) } }
fun Session.setCount(): Int = exercises.sumOf { it.sets.size }

private fun List<SetRecord>.maxOf0(f: (SetRecord) -> Double) = if (isEmpty()) 0.0 else maxOf(f)

class Metric(val id: String, val label: String, val unit: String, val f: (List<SetRecord>) -> Double)

val METRICS: Map<String, List<Metric>> = mapOf(
    "w" to listOf(
        Metric("max", "Carga máxima", "kg") { ss -> ss.maxOf0 { it.a ?: 0.0 } },
        Metric("rm", "1RM estimado", "kg") { ss -> ss.maxOf0 { e1rm(it.a, it.b) } },
        Metric("vol", "Volume", "kg") { ss -> ss.sumOf { (it.a ?: 0.0) * (it.b ?: 0.0) } },
    ),
    "bw" to listOf(
        Metric("reps", "Reps máximas", "reps") { ss -> ss.maxOf0 { it.b ?: 0.0 } },
        Metric("tot", "Total de reps", "reps") { ss -> ss.sumOf { it.b ?: 0.0 } },
        Metric("extra", "Carga extra", "kg") { ss -> ss.maxOf0 { it.a ?: 0.0 } },
    ),
    "s" to listOf(
        Metric("max", "Tempo máximo", "s") { ss -> ss.maxOf0 { it.a ?: 0.0 } },
        Metric("tot", "Tempo total", "s") { ss -> ss.sumOf { it.a ?: 0.0 } },
    ),
    "c" to listOf(
        Metric("min", "Tempo", "min") { ss -> ss.sumOf { it.a ?: 0.0 } },
        Metric("km", "Distância", "km") { ss -> ss.sumOf { it.b ?: 0.0 } },
    ),
)

fun metricsFor(kind: String) = METRICS[kind] ?: METRICS.getValue("w")

class HistEntry(val sess: Session, val kind: String, val sets: List<SetRecord>)

/** Histórico de um exercício, do mais antigo para o mais recente. */
fun AppData.exHistory(exId: String): List<HistEntry> = sessions.mapNotNull { s ->
    val exs = s.exercises.filter { it.exId == exId }
    if (exs.isEmpty()) null else HistEntry(s, exs[0].kind, exs.flatMap { it.sets })
}.sortedBy { it.sess.start }

fun AppData.lastSets(exId: String): List<SetRecord> = exHistory(exId).lastOrNull()?.sets?.filter { !it.warm } ?: emptyList()

fun fmtSet(kind: String, a: Double?, b: Double?): String = when (kind) {
    "w" -> "${fmt(a ?: 0.0)} × ${fmt(b ?: 0.0, 0)}"
    "bw" -> if (a != null && a != 0.0) "+${fmt(a)} × ${fmt(b ?: 0.0, 0)}" else "${fmt(b ?: 0.0, 0)} reps"
    "s" -> "${fmt(a ?: 0.0, 0)}s"
    "c" -> "${fmt(a ?: 0.0)} min" + if (b != null && b != 0.0) " · ${fmt(b, 2)} km" else ""
    else -> ""
}

fun fmtSet(kind: String, s: SetRecord) = fmtSet(kind, s.a, s.b)

/** Unidade do alvo de repetições conforme o tipo (segundos, minutos ou nada). */
fun targetUnit(kind: String) = when (kind) { "s" -> " s"; "c" -> " min"; else -> "" }

/* ================= Tipos de série ================= */

class SetType(val id: String, val label: String, val desc: String, val mark: String?)

val SET_TYPES = listOf(
    SetType("", "Normal", "Conta no volume, nos recordes e na progressão", null),
    SetType("warm", "Aquecimento", "Não conta no volume nem nos recordes", "A"),
    SetType("drop", "Drop set", "Reduz a carga e continua sem descanso; conta no volume", "D"),
    SetType("fail", "Até a falha", "Série levada até não conseguir mais nenhuma repetição", "F"),
)

fun setTypeOf(warm: Boolean, t: String?): String = if (warm) "warm" else t ?: ""
fun setMark(warm: Boolean, t: String?): String? = SET_TYPES.find { it.id == setTypeOf(warm, t) }?.mark

/* ================= Programas ================= */

fun AppData.programOf(r: Routine): Program? = r.programId?.let { id -> programs.find { it.id == id } }
fun AppData.progRoutines(pid: String): List<Routine> = routines.filter { it.programId == pid }

/** Fichas que valem para a tela Hoje: avulsas e as de programas ativos. */
fun AppData.isScheduled(r: Routine): Boolean = programOf(r)?.active ?: true

/** Próxima ficha na ordem do programa, a partir do último treino feito nele. */
fun AppData.nextInProgram(p: Program): Routine? {
    val rs = progRoutines(p.id)
    if (rs.isEmpty()) return null
    val ids = rs.map { it.id }.toSet()
    val last = sessions.find { it.routineId in ids } ?: return rs[0]
    return rs[(rs.indexOfFirst { it.id == last.routineId } + 1) % rs.size]
}

fun AppData.sessionsInRange(a: Long, b: Long) = sessions.filter { it.start in a until b }

fun AppData.streakWeeks(now: Long = System.currentTimeMillis()): Int {
    var wk = startOfWeek(now)
    var n = 0
    if (sessionsInRange(wk, addDays(wk, 7)).isEmpty()) wk = addDays(wk, -7)
    while (sessionsInRange(wk, addDays(wk, 7)).isNotEmpty()) {
        n++
        wk = addDays(wk, -7)
    }
    return n
}

/* ================= Superséries e circuitos =================
   Exercícios seguidos com o mesmo `ss` formam um grupo: 2 = supersérie, 3 ou mais = circuito.
   No treino, o descanso só vem depois do último exercício do grupo. */

class SSGroup(val start: Int, val end: Int, val letter: String) {
    val size get() = end - start + 1
    val name get() = if (size > 2) "Circuito" else "Supersérie"
}

class SSInfo(val g: SSGroup, val pos: Int) {
    val first get() = pos == 1
    val last get() = pos == g.size
    val badge get() = "${g.letter}$pos · ${g.name}"
}

fun ssGroups(ss: List<String?>): List<SSGroup> {
    val out = mutableListOf<SSGroup>()
    var i = 0
    var n = 0
    while (i < ss.size) {
        var j = i
        if (ss[i] != null) while (j + 1 < ss.size && ss[j + 1] == ss[i]) j++
        if (j > i) out += SSGroup(i, j, ('A' + n++).toString())
        i = j + 1
    }
    return out
}

fun ssInfo(ss: List<String?>, i: Int): SSInfo? = ssGroups(ss).find { i in it.start..it.end }?.let { SSInfo(it, i - it.start + 1) }

/** Remove marcas soltas (grupo de um só). */
fun ssClean(ss: List<String?>): List<String?> = ss.mapIndexed { k, s ->
    if (s != null && ss.getOrNull(k - 1) != s && ss.getOrNull(k + 1) != s) null else s
}

private fun newSs() = "s" + uid().takeLast(6)

/** Junta o item i com o próximo (e o grupo dele, se houver) ou separa os dois. */
fun ssToggle(ssIn: List<String?>, i: Int): List<String?> {
    if (i < 0 || i + 1 >= ssIn.size) return ssIn
    val ss = ssIn.toMutableList()
    val a = ss[i]
    val b = ss[i + 1]
    if (a != null && a == b) {
        val nid = newSs()
        var k = i + 1
        while (k < ss.size && ss[k] == a) ss[k++] = nid
    } else {
        val id = a ?: newSs().also { ss[i] = it }
        ss[i + 1] = id
        var k = i + 2
        while (b != null && k < ss.size && ss[k] == b) ss[k++] = id
    }
    return ssClean(ss)
}

/* ================= Progressão automática =================
   Dupla progressão por regras: fez o topo da faixa de repetições em todas as séries → sobe a carga e
   recomeça no começo da faixa; ficou dentro da faixa → mesma carga, +1 rep; ficou abaixo da faixa
   duas vezes seguidas → reduz ~10%. O RIR, quando registrado, deixa o passo maior se sobrou muito. */

val DEFAULT_RANGE = 8 to 12

fun repRange(target: String?): Pair<Int, Int>? {
    val t = target ?: ""
    Regex("(\\d+)\\s*(?:-|–|a|até)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(t)?.let {
        val x = it.groupValues[1].toInt()
        val y = it.groupValues[2].toInt()
        return min(x, y) to max(x, y)
    }
    val n = Regex("\\d+").find(t)?.value?.toInt() ?: return null
    return if (n > 0) n to n else null
}

/** Menor salto de carga de cada equipamento (kg). */
fun loadStep(ex: Exercise?): Double = when (ex?.equip) {
    "Halteres" -> 2.0
    "Kettlebell" -> 4.0
    "Máquina", "Polia" -> 5.0
    else -> 2.5
}

fun roundTo(v: Double, step: Double): Double = Math.round(v / step) * step

class SugSet(val a: Double?, val b: Double?)

/** type: up | reps | keep | down | first */
class Suggestion(val type: String, val sets: List<SugSet>, val text: String, val why: String) {
    val icon get() = when (type) { "up" -> "↑"; "reps" -> "+"; "keep" -> "="; "down" -> "↓"; else -> "•" }
}

private fun progSets(sets: List<SetRecord>) = sets.filter { !it.warm && it.t != "drop" }

fun AppData.suggestNext(exId: String, target: String?): Suggestion? {
    val ex = ex(exId)
    val kind = ex?.kind ?: return null
    if (kind == "c") return null
    val hist = exHistory(exId).map { progSets(it.sets) }.filter { it.isNotEmpty() }
    if (hist.isEmpty()) {
        return if (kind == "s") null else Suggestion(
            "first", emptyList(), "Primeira vez",
            "Escolha uma carga em que sobrem 2 ou 3 repetições no fim de cada série. A partir do próximo treino o app sugere a evolução.",
        )
    }
    val last = hist.last()
    val prev = hist.getOrNull(hist.size - 2)
    val rirs = last.mapNotNull { it.rir }
    val easy = rirs.isNotEmpty() && rirs.sum() / rirs.size >= 3
    if (kind == "s") {
        val add = if (easy) 10 else 5
        return Suggestion("reps", last.map { SugSet((it.a ?: 0.0) + add, null) }, "+$add s por série", "Tente segurar $add segundos a mais que no último treino")
    }
    val (lo, hi) = repRange(target) ?: DEFAULT_RANGE
    val inc = if (easy) 2 else 1
    val incTxt = if (inc == 1) "1 rep" else "2 reps"
    val w = last.maxOf { it.a ?: 0.0 }
    // Peso corporal sem carga extra: progride nas repetições
    if (kind == "bw" && w == 0.0) {
        val minR = last.minOf { it.b ?: 0.0 }
        if (minR >= hi) return Suggestion(
            "up", last.map { SugSet(null, (it.b ?: 0.0) + 1) }, "$hi+ reps",
            "Você passou de $hi reps em todas as séries: continue somando reps ou adicione carga (+kg) ou uma variação mais difícil",
        )
        return Suggestion(
            "reps", last.map { SugSet(null, min(hi.toDouble(), (it.b ?: 0.0) + inc)) }, "${fmt(min(hi.toDouble(), minR + inc), 0)} reps",
            "Tente $incTxt a mais por série (faixa $lo–$hi)",
        )
    }
    val step = if (kind == "bw") 2.5 else loadStep(ex)
    val kg = { v: Double -> if (kind == "bw") "+${fmt(v, 2)} kg" else "${fmt(v, 2)} kg" }
    val top = last.filter { (it.a ?: 0.0) == w }
    val topMin = top.minOf { it.b ?: 0.0 }
    if (topMin >= hi) {
        val a = w + step * inc
        return Suggestion(
            "up", last.map { if ((it.a ?: 0.0) == w) SugSet(a, lo.toDouble()) else SugSet(it.a ?: 0.0, it.b) }, "${kg(a)} × $lo",
            "Você fez ${if (hi == lo) "$hi" else "$hi+"} reps em todas as séries com ${kg(w)}: suba a carga${if (hi > lo) " e recomece em $lo reps" else ""}",
        )
    }
    if (topMin < lo) {
        val prevTop = prev?.filter { (it.a ?: 0.0) == w } ?: emptyList()
        if (prevTop.isNotEmpty() && prevTop.minOf { it.b ?: 0.0 } < lo) {
            val a = max(if (kind == "bw") 0.0 else step, roundTo(w * 0.9, step))
            return Suggestion(
                "down", last.map { SugSet(min(a, it.a ?: 0.0), lo.toDouble()) }, "${kg(a)} × $lo",
                "Duas vezes abaixo de $lo reps com ${kg(w)}: reduza um pouco a carga e volte a progredir",
            )
        }
        return Suggestion(
            "keep", last.map { SugSet(it.a ?: 0.0, max(lo.toDouble(), it.b ?: 0.0)) }, "${kg(w)} × $lo",
            "Ficou abaixo de $lo reps: mantenha a carga e busque $lo reps",
        )
    }
    return Suggestion(
        "reps", last.map { SugSet(it.a ?: 0.0, min(hi.toDouble(), (it.b ?: 0.0) + inc)) }, "${kg(w)} × ${fmt(min(hi.toDouble(), topMin + inc), 0)}",
        "Mantenha a carga e tente $incTxt a mais por série (faixa $lo–$hi)",
    )
}

/**
 * Valor sugerido para um campo vazio do treino: a progressão automática, senão o último treino,
 * senão a série anterior. Drop set: ~20% menos carga que a série anterior.
 */
fun placeholder(ex: ActiveExercise, i: Int, field: Char, sg: Suggestion?, last: List<SetRecord>, step: Double): String {
    val s = ex.sets[i]
    fun value(k: Int) = if (field == 'a') ex.sets[k].a else ex.sets[k].b
    if (s.t == "drop" && i > 0) {
        val pv = value(i - 1).ifEmpty { placeholder(ex, i - 1, field, sg, last, step) }
        val n = num(pv)
        if (field != 'a' || n == null) return pv
        return fmtIn(max(step, roundTo(n * 0.8, step)))
    }
    if (!s.warm) {
        val wi = ex.sets.take(i).count { !it.warm }
        val ref = sg?.sets?.takeIf { it.isNotEmpty() }?.let { it.getOrNull(wi) ?: it.last() }
        val rv = if (field == 'a') ref?.a else ref?.b
        if (rv != null) return fmtIn(rv)
        val old = last.getOrNull(wi) ?: last.lastOrNull()
        val ov = if (field == 'a') old?.a else old?.b
        if (ov != null) return fmtIn(ov)
    }
    for (j in i - 1 downTo 0) if (value(j).isNotEmpty()) return value(j)
    return ""
}

/* ================= Volume semanal por músculo =================
   Séries válidas (sem aquecimento) por grupo muscular; músculo secundário conta meia série. */

val VOL_GROUPS = listOf(
    "Peito" to listOf("pei"), "Costas" to listOf("dor", "mei"), "Ombros" to listOf("omb"), "Bíceps" to listOf("bic"),
    "Tríceps" to listOf("tri"), "Quadríceps" to listOf("qua"), "Posteriores" to listOf("pos"), "Glúteos" to listOf("glu"),
    "Panturrilhas" to listOf("pan"), "Abdômen" to listOf("abd"), "Trapézio" to listOf("tra"), "Antebraços" to listOf("ant"),
    "Lombar" to listOf("lom"), "Adutores" to listOf("adu"), "Abdutores" to listOf("abu"), "Pescoço" to listOf("pes"),
)
const val VOL_MAIN = 10
val VOL_RANGE = 10 to 20
const val VOL_SCALE = 25.0

fun AppData.groupSets(from: Long, to: Long): DoubleArray {
    val out = DoubleArray(VOL_GROUPS.size)
    for (sess in sessions) {
        if (sess.start < from || sess.start >= to) continue
        for (e in sess.exercises) {
            val ex = ex(e.exId)
            if (e.kind == "c" || ex?.group == "Alongamento") continue
            val n = e.sets.count { !it.warm }
            if (n == 0) continue
            val (p, s) = musclesOf(ex)
            VOL_GROUPS.forEachIndexed { i, (_, codes) ->
                if (codes.any { it in p }) out[i] += n.toDouble()
                else if (codes.any { it in s }) out[i] += n / 2.0
            }
        }
    }
    return out
}

/** Séries por músculo nos últimos [days] dias (principal conta 1 por série, secundário 0,5). */
fun AppData.muscleLoad(days: Int): Map<String, Double> {
    val since = startOfDay(System.currentTimeMillis()) - (days - 1) * DAY_MS
    val load = HashMap<String, Double>()
    for (sess in sessions) {
        if (sess.start < since) continue
        for (e in sess.exercises) {
            val n = e.sets.count { !it.warm }
            if (n == 0) continue
            val (p, s) = musclesOf(ex(e.exId))
            for (c in p) load[c] = (load[c] ?: 0.0) + n
            for (c in s) load[c] = (load[c] ?: 0.0) + n / 2.0
        }
    }
    return load
}

/* ================= Calculadoras ================= */

val PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
val PLATE_COLORS = mapOf(25.0 to 0xFFD8392F, 20.0 to 0xFF2F6BD6, 15.0 to 0xFFE5B315, 10.0 to 0xFF2E9B57, 5.0 to 0xFFECECE6, 2.5 to 0xFFB9473D, 1.25 to 0xFFA8ADB4)
val BARS = listOf(20.0 to "20 kg", 15.0 to "15 kg", 10.0 to "10 kg", 0.0 to "Sem barra")

class PlateResult(val plates: List<Double>, val rest: Double)

/** Anilhas de cada lado para chegar ao peso total (a sobra > 0 quando não fecha exato). */
fun platesFor(total: Double, bar: Double, avail: List<Double>): PlateResult? {
    var side = (total - bar) / 2
    if (side < 0 || side.isNaN()) return null
    val out = mutableListOf<Double>()
    for (p in avail.sortedDescending()) while (side >= p - 1e-9) {
        out += p
        side -= p
    }
    return PlateResult(out, Math.round(side * 1000) / 1000.0)
}

class WarmSet(val w: Double, val r: Int, val p: Double)

fun warmupSets(w: Double, bar: Double, step: Double): List<WarmSet> {
    if (w <= 0) return emptyList()
    val scheme = if (w < 40) listOf(0.5 to 10, 0.75 to 5) else listOf(0.4 to 10, 0.6 to 5, 0.8 to 3)
    val out = mutableListOf<WarmSet>()
    for ((p, r) in scheme) {
        val x = max(bar, roundTo(w * p, step))
        if (x >= w || (out.isNotEmpty() && out.last().w == x)) continue
        out += WarmSet(x, r, p)
    }
    return out
}

val RM_REPS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15)
fun rmLoad(rm: Double, r: Int): Double = if (r == 1) rm else rm / (1 + r / 30.0)
