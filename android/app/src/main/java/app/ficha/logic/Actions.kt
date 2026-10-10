package app.ficha.logic

import app.ficha.data.ActiveExercise
import app.ficha.data.ActiveSet
import app.ficha.data.ActiveWorkout
import app.ficha.data.AppData
import app.ficha.data.Exercise
import app.ficha.data.PR
import app.ficha.data.Program
import app.ficha.data.RestTimer
import app.ficha.data.Routine
import app.ficha.data.RoutineItem
import app.ficha.data.Session
import app.ficha.data.SessionExercise
import app.ficha.data.SetRecord
import app.ficha.data.TemplateRoutine
import app.ficha.data.uid
import kotlin.math.max

/* Mudanças nos dados: funções puras que devolvem um AppData novo (o Store grava). */

fun <T> List<T>.swap(i: Int, j: Int): List<T> {
    if (i !in indices || j !in indices) return this
    val m = toMutableList()
    m[i] = this[j].also { m[j] = this[i] }
    return m
}

fun <T> List<T>.replaceAt(i: Int, v: T): List<T> = toMutableList().also { it[i] = v }
fun <T> List<T>.without(i: Int): List<T> = toMutableList().also { it.removeAt(i) }

/* ---------------- Programas e fichas ---------------- */

fun AppData.updateRoutine(id: String, f: (Routine) -> Routine) = copy(routines = routines.map { if (it.id == id) f(it) else it })
fun AppData.updateProgram(id: String, f: (Program) -> Program) = copy(programs = programs.map { if (it.id == id) f(it) else it })

fun Routine.withItems(items: List<RoutineItem>): Routine {
    val ss = ssClean(items.map { it.ss })
    return copy(items = items.mapIndexed { k, it -> it.copy(ss = ss[k]) })
}

fun Routine.toggleSS(i: Int): Routine {
    val ss = ssToggle(items.map { it.ss }, i)
    return copy(items = items.mapIndexed { k, it -> it.copy(ss = ss[k]) })
}

fun AppData.moveInProgram(id: String, d: Int): AppData {
    val r = routines.find { it.id == id } ?: return this
    val rs = routines.filter { it.programId == r.programId }
    val o = rs.getOrNull(rs.indexOf(r) + d) ?: return this
    return copy(routines = routines.swap(routines.indexOf(r), routines.indexOf(o)))
}

fun AppData.duplicateProgram(id: String): Pair<AppData, String> {
    val p = programs.find { it.id == id } ?: return this to id
    val c = p.copy(id = uid(), name = (p.name.ifBlank { "Programa" }) + " (cópia)")
    val copies = progRoutines(p.id).map { r -> r.copy(id = uid(), programId = c.id, items = r.items.map { it.copy(id = uid()) }) }
    val ps = programs.toMutableList().also { it.add(programs.indexOf(p) + 1, c) }
    return copy(programs = ps, routines = routines + copies) to c.id
}

fun AppData.duplicateRoutine(id: String): Pair<AppData, String> {
    val r = routines.find { it.id == id } ?: return this to id
    val c = r.copy(id = uid(), name = (r.name.ifBlank { "Ficha" }) + " (cópia)", items = r.items.map { it.copy(id = uid()) })
    return copy(routines = routines.toMutableList().also { it.add(routines.indexOf(r) + 1, c) }) to c.id
}

/** Cria um programa a partir de um modelo pronto (ou do assistente). */
fun AppData.createProgram(name: String, defs: List<TemplateRoutine>, useDays: Boolean, pauseOthers: Boolean): Pair<AppData, String> {
    val p = Program(uid(), name, true)
    val newRoutines = defs.map { tr ->
        Routine(
            id = uid(), name = tr.name, days = if (useDays) tr.days else emptyList(), programId = p.id,
            items = tr.items.filter { ex(it.exId) != null }.map { it.copy(id = uid()) },
        )
    }
    val ps = (if (pauseOthers) programs.map { it.copy(active = false) } else programs) + p
    return copy(programs = ps, routines = routines + newRoutines) to p.id
}

fun newItemFor(ex: Exercise, defaultRest: Int) = RoutineItem(
    id = uid(), exId = ex.id,
    sets = if (ex.kind == "c") 1 else 3,
    reps = when (ex.kind) { "s" -> "30"; "c" -> "20"; else -> "10-12" },
    rest = if (ex.kind == "c") 0 else defaultRest,
)

/* ---------------- Treino em andamento ---------------- */

fun AppData.makeActiveExercise(exId: String, sets: Int, reps: String = "", rest: Int = settings.rest, note: String = "", ss: String? = null): ActiveExercise {
    val ex = ex(exId)
    return ActiveExercise(
        uid = uid(), exId = exId, name = ex?.name ?: "Exercício", kind = ex?.kind ?: "w",
        target = reps, rest = rest, note = note, ss = ss,
        sets = List(max(1, sets)) { ActiveSet() },
    )
}

fun AppData.startWorkout(name: String, routineId: String?, exercises: List<ActiveExercise>) =
    copy(active = ActiveWorkout(id = uid(), name = name, routineId = routineId, start = System.currentTimeMillis(), exercises = exercises))

fun AppData.startFromRoutine(r: Routine) =
    startWorkout(r.name.ifBlank { "Treino" }, r.id, r.items.map { makeActiveExercise(it.exId, it.sets, it.reps, it.rest, it.note, it.ss) })

fun AppData.repeatSession(s: Session): AppData {
    val routine = routines.find { it.id == s.routineId }
    return startWorkout(s.name, s.routineId, s.exercises.map { e ->
        val it = routine?.items?.find { i -> i.exId == e.exId }
        val n = makeActiveExercise(e.exId, e.sets.size, it?.reps ?: "", it?.rest ?: settings.rest, it?.note ?: "", e.ss)
        n.copy(sets = n.sets.mapIndexed { i, x -> x.copy(warm = e.sets.getOrNull(i)?.warm ?: false) })
    })
}

fun AppData.updateActive(f: (ActiveWorkout) -> ActiveWorkout) = active?.let { copy(active = f(it)) } ?: this

fun ActiveWorkout.updateEx(x: Int, f: (ActiveExercise) -> ActiveExercise) =
    if (x in exercises.indices) copy(exercises = exercises.replaceAt(x, f(exercises[x]))) else this

fun ActiveWorkout.withExercises(list: List<ActiveExercise>): ActiveWorkout {
    val ss = ssClean(list.map { it.ss })
    return copy(exercises = list.mapIndexed { k, e -> e.copy(ss = ss[k]) })
}

fun ActiveWorkout.toggleSS(x: Int): ActiveWorkout {
    val ss = ssToggle(exercises.map { it.ss }, x)
    return copy(exercises = exercises.mapIndexed { k, e -> e.copy(ss = ss[k]) })
}

class WorkoutStats(val total: Int, val done: Int, val vol: Double) {
    val p get() = if (total > 0) done.toFloat() / total else 0f
}

fun ActiveWorkout.stats(): WorkoutStats = WorkoutStats(
    total = exercises.sumOf { it.sets.size },
    done = exercises.sumOf { e -> e.sets.count { it.done } },
    vol = exercises.sumOf { e -> e.sets.filter { it.done && !it.warm }.sumOf { if (e.kind == "w") (num(it.a) ?: 0.0) * (num(it.b) ?: 0.0) else 0.0 } },
)

/** Resultado de marcar uma série: o que mostrar e para onde ir depois. */
class ToggleResult(
    val data: AppData,
    val message: String? = null,
    /** Campo que faltou preencher (a ou b) */
    val missing: Char? = null,
    /** Exercício para onde rolar (supersérie) */
    val scrollTo: Int? = null,
    val completed: Boolean = false,
)

fun AppData.toggleSet(x: Int, i: Int): ToggleResult {
    val a = active ?: return ToggleResult(this)
    val ex = a.exercises.getOrNull(x) ?: return ToggleResult(this)
    val s = ex.sets.getOrNull(i) ?: return ToggleResult(this)
    if (s.done) return ToggleResult(updateActive { it.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, s.copy(done = false))) } })

    // Campos vazios recebem a sugestão mostrada (progressão, último treino ou série anterior)
    val k = app.ficha.data.Catalog.kinds[ex.kind]
    val exDef = ex(ex.exId)
    val sg = if (settings.progression) suggestNext(ex.exId, ex.target) else null
    val last = lastSets(ex.exId)
    val step = loadStep(exDef)
    var ns = s
    if (ns.a.isEmpty()) ns = ns.copy(a = placeholder(ex, i, 'a', sg, last, step))
    if (ns.b.isEmpty() && k?.b != null) ns = ns.copy(b = placeholder(ex, i, 'b', sg, last, step))
    val need = if (ex.kind == "w" || ex.kind == "bw") 'b' else 'a'
    val nv = num(if (need == 'a') ns.a else ns.b)
    if (nv == null || nv <= 0) {
        val withFill = updateActive { it.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, ns)) } }
        val msg = when {
            need == 'b' -> "Informe as repetições"
            ex.kind == "s" -> "Informe o tempo em segundos"
            else -> "Informe os minutos"
        }
        return ToggleResult(withFill, msg, missing = need)
    }
    if (ex.kind == "w" && ns.a.isEmpty()) ns = ns.copy(a = "0")
    ns = ns.copy(done = true)
    var w = a.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, ns)) }
    val list = w.exercises
    val next = list[x].sets.getOrNull(i + 1)
    val dropNext = next != null && next.t == "drop" && !next.done
    val ssi = ssInfo(list.map { it.ss }, x)
    val completed = list[x].sets.all { it.done }
    fun pending(k: Int) = list[k].sets.any { !it.done }
    if (ssi != null && !dropNext) {
        // Supersérie/circuito: sem descanso até o último exercício da rodada; depois volta para o primeiro com séries
        var k2 = x + 1
        while (k2 <= ssi.g.end && !pending(k2)) k2++
        val goTo = if (k2 <= ssi.g.end) k2 else (ssi.g.start..ssi.g.end).find { pending(it) }
        if (k2 > ssi.g.end) w = w.startRest(ex.rest)
        val msg = goTo?.let { if (k2 <= ssi.g.end) "Agora: ${list[it].name}" else "Descanse · depois ${list[it].name}" }
        return ToggleResult(copy(active = w), msg, scrollTo = goTo, completed = completed)
    }
    if (!dropNext) w = w.startRest(ex.rest) // drop set vem sem descanso
    return ToggleResult(copy(active = w), completed = completed)
}

fun ActiveWorkout.startRest(sec: Int): ActiveWorkout =
    if (sec <= 0) this else copy(rest = RestTimer(end = System.currentTimeMillis() + sec * 1000L, total = sec))

fun ActiveWorkout.addRest(deltaSec: Int): ActiveWorkout {
    val r = rest ?: return this
    val end = r.end + deltaSec * 1000L
    return if (end <= System.currentTimeMillis()) copy(rest = null) else copy(rest = RestTimer(end, max(1, r.total + deltaSec)))
}

fun ActiveExercise.setType(i: Int, v: String): ActiveExercise {
    val s = sets.getOrNull(i) ?: return this
    var n = s.copy(warm = v == "warm", t = if (v == "drop" || v == "fail") v else null)
    if (v == "fail" && n.rir.isEmpty()) n = n.copy(rir = "0")
    if (n.warm) n = n.copy(rir = "")
    return copy(sets = sets.replaceAt(i, n))
}

fun ActiveExercise.addSet(): ActiveExercise {
    val last = sets.lastOrNull()
    return copy(sets = sets + ActiveSet(a = last?.a ?: "", b = last?.b ?: ""))
}

/** Séries leves de aquecimento antes das séries de trabalho (calculadas pela carga). */
fun AppData.withWarmup(x: Int): Pair<AppData, String> {
    val a = active ?: return this to ""
    val ex = a.exercises[x]
    val def = ex(ex.exId)
    val i0 = ex.sets.indexOfFirst { !it.warm }
    val sg = if (settings.progression) suggestNext(ex.exId, ex.target) else null
    val w = if (i0 < 0) null else num(ex.sets[i0].a) ?: num(placeholder(ex, i0, 'a', sg, lastSets(ex.exId), loadStep(def)))
    if (w == null || w <= 0) return this to "Informe a carga da primeira série"
    val bar = if (def?.equip == "Barra") settings.bar ?: 20.0 else 0.0
    val sets = warmupSets(w, bar, loadStep(def))
    if (sets.isEmpty()) return this to "Carga leve demais para aquecimento"
    val warm = sets.map { ActiveSet(a = fmtIn(it.w), b = it.r.toString(), warm = true) }
    val nd = updateActive { it.updateEx(x) { e -> e.copy(sets = warm + e.sets.filter { s -> !s.warm || s.done }) } }
    return nd to "${sets.size} séries de aquecimento até ${fmt(w, 2)} kg"
}

/** A ficha mudou no treino (exercícios ou nº de séries)? Então dá para atualizar a ficha ao finalizar. */
fun AppData.routineChanged(): Routine? {
    val a = active ?: return null
    val r = routines.find { it.id == a.routineId } ?: return null
    val sig1 = r.items.joinToString("|") { "${it.exId}:${it.sets}" }
    val sig2 = a.exercises.filter { e -> e.sets.any { it.done } }.joinToString("|") { e -> "${e.exId}:${e.sets.count { !it.warm }}" }
    return if (sig1 != sig2) r else null
}

/** Finaliza o treino: vira uma sessão do histórico, com os recordes pessoais detectados. */
fun AppData.commitWorkout(updateRoutine: Boolean): Pair<AppData, Session>? {
    val a = active ?: return null
    val exercises = a.exercises.map { e ->
        SessionExercise(
            exId = e.exId, name = e.name, kind = e.kind, ss = e.ss,
            sets = e.sets.filter { it.done }.map { s ->
                SetRecord(
                    a = num(s.a), b = if (e.kind == "s") null else num(s.b), warm = s.warm, t = s.t,
                    rir = if (s.warm) null else num(s.rir),
                )
            },
        )
    }.filter { it.sets.isNotEmpty() }
    // Recordes pessoais (comparados com os treinos anteriores)
    val prs = mutableListOf<PR>()
    for (e in exercises) {
        val prev = exHistory(e.exId).map { h -> h.sets.filter { !it.warm } }.filter { it.isNotEmpty() }
        val cur = e.sets.filter { !it.warm }
        if (prev.isEmpty() || cur.isEmpty()) continue
        for (m in metricsFor(e.kind)) {
            if (m.id == "vol" || m.id == "tot" || m.id == "km") continue
            val best = prev.maxOf { m.f(it) }
            val now = m.f(cur)
            if (now > best && now > 0) prs += PR(e.exId, e.name, m.label, now, m.unit)
        }
    }
    val sess = Session(
        id = a.id, name = a.name, routineId = a.routineId, start = a.start, end = System.currentTimeMillis(),
        notes = a.notes.trim(), exercises = exercises, prs = prs,
    )
    var d = this
    val routine = routines.find { it.id == a.routineId }
    if (updateRoutine && routine != null) {
        val items = a.exercises.filter { e -> e.sets.any { it.done } }.map { e ->
            val old = routine.items.find { it.exId == e.exId }
            RoutineItem(
                id = old?.id ?: uid(), exId = e.exId, sets = e.sets.count { !it.warm }.coerceAtLeast(1),
                reps = e.target.ifEmpty { old?.reps ?: "" }, rest = e.rest, note = e.note, ss = e.ss,
            )
        }
        d = d.updateRoutine(routine.id) { it.withItems(items) }
    }
    d = d.copy(sessions = (listOf(sess) + d.sessions).sortedByDescending { it.start }, active = null)
    return d to sess
}

/* ---------------- Backup ---------------- */

fun AppData.unbackedSessions(): Int = sessions.count { it.end > settings.lastBackup }

fun AppData.backupDue(now: Long = System.currentTimeMillis()): Boolean =
    unbackedSessions() >= 3 && now - settings.lastBackup > 7 * DAY_MS && now > settings.backupSnooze

fun AppData.backupLabel(): String {
    val t = settings.lastBackup
    if (t == 0L) return "Nenhum backup feito ainda"
    val n = unbackedSessions()
    return "Último: ${relDay(t).lowercase(PT_BR)}" + if (n > 0) " · ${plural(n, "treino", "treinos")} depois dele" else ""
}
