package app.ficha.logic

import app.ficha.data.AppData
import app.ficha.data.DietGoal
import app.ficha.data.FoodData
import app.ficha.data.FoodDay
import app.ficha.data.FoodEntry
import app.ficha.data.Macros
import app.ficha.data.SavedMeal
import app.ficha.data.Tdee
import app.ficha.data.TdeeNote
import app.ficha.data.toMealItem
import app.ficha.data.uid
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* ================= Dieta: metas, gasto e registros (port do nutrition.js) ================= */

class Activity(val v: Double, val name: String, val desc: String)

val ACTIVITY = listOf(
    Activity(1.2, "Sedentário", "pouco movimento no dia"),
    Activity(1.375, "Leve", "treina 1 a 3 vezes por semana"),
    Activity(1.55, "Moderado", "treina 3 a 5 vezes"),
    Activity(1.725, "Alto", "treina 6 ou 7 vezes"),
    Activity(1.9, "Muito alto", "treino pesado + trabalho físico"),
)
val GOALS = listOf("perder" to "Perder gordura", "manter" to "Manter", "ganhar" to "Ganhar massa")
/** % do peso por semana */
val RATES = mapOf("perder" to listOf(0.25, 0.5, 0.75, 1.0), "ganhar" to listOf(0.1, 0.25, 0.5))
val PROT = listOf(1.6, 1.8, 2.0, 2.2)

/** Micronutrientes da TACO: [sódio, cálcio, ferro, potássio, magnésio, vitamina C] em mg. Sódio é limite, não meta. */
class Micro(val key: String, val name: String, val idx: Int, val limit: Boolean, val dec: Int, val ref: (String) -> Double)

val MICROS = listOf(
    Micro("na", "Sódio", 0, true, 0) { 2000.0 },
    Micro("k", "Potássio", 3, false, 0) { 3510.0 },
    Micro("ca", "Cálcio", 1, false, 0) { 1000.0 },
    Micro("fe", "Ferro", 2, false, 1) { if (it == "f") 18.0 else 8.0 },
    Micro("mg", "Magnésio", 4, false, 0) { if (it == "f") 320.0 else 420.0 },
    Micro("vc", "Vitamina C", 5, false, 1) { if (it == "f") 75.0 else 90.0 },
)

fun isoDate(t: Long): String = dateOf(t).toString()
fun dayKey(t: Long) = isoDate(t)

fun AppData.dayOf(key: String): FoodDay = food.days[key] ?: FoodDay()

class Totals(val k: Double, val p: Double, val c: Double, val f: Double, val fi: Double)

fun FoodDay.totals() = Totals(e.sumOf { it.k ?: 0.0 }, e.sumOf { it.p ?: 0.0 }, e.sumOf { it.c ?: 0.0 }, e.sumOf { it.f ?: 0.0 }, e.sumOf { it.fi ?: 0.0 })
fun AppData.dayTotals(key: String) = dayOf(key).totals()

/** Refeição pelo horário: café até 10h30, almoço até 15h, lanche até 18h30, jantar depois. */
fun mealNow(): Int {
    val t = LocalTime.now()
    val h = t.hour + t.minute / 60.0
    return if (h < 10.5) 0 else if (h < 15) 1 else if (h < 18.5) 2 else 3
}

fun AppData.lastBody(field: String): Double? = body.filter { it[field] != null }.maxByOrNull { it.t }?.get(field)
fun AppData.dietWeight(): Double? = lastBody("peso") ?: num(profile?.peso)
/** Altura em cm (aceita metros também). */
fun AppData.bodyHeight(): Double? = num(profile?.altura)?.let { if (it > 3) it else it * 100 }
fun AppData.sex(): String = profile?.sexo ?: ""

/** Meta salva com os padrões do PWA. */
fun DietGoal?.resolved() = DietGoal(
    obj = this?.obj ?: "manter", rate = this?.rate ?: 0.5, act = this?.act ?: 1.55, prot = this?.prot ?: 2.0, manual = this?.manual,
)

/** Gasto pela fórmula: Katch-McArdle (com % de gordura) ou Mifflin-St Jeor; TMB da balança, se houver. */
fun AppData.formulaTDEE(goal: DietGoal? = food.goal): Double? {
    // Valores fora do plausível (erro de digitação) contam como não preenchidos
    val w = dietWeight()?.takeIf { it in 25.0..350.0 } ?: return null
    val h = bodyHeight()?.takeIf { it in 100.0..250.0 }
    val age = num(profile?.idade)?.takeIf { it in 10.0..110.0 }
    val bf = lastBody("gordura")?.takeIf { it in 2.0..70.0 }
    var bmr = when {
        bf != null -> 370 + 21.6 * w * (1 - bf / 100)
        h != null && age != null -> 10 * w + 6.25 * h - 5 * age + when (sex()) { "m" -> 5.0; "f" -> -161.0; else -> -78.0 }
        else -> 22 * w
    }
    val tmb = lastBody("tmb")
    if (tmb != null && bf == null) bmr = tmb
    return (bmr * (goal.resolved().act ?: 1.55)).roundToInt().toDouble()
}

class Slope(val perDay: Double, val n: Int, val span: Double)

/** Tendência do peso: inclinação (regressão linear) das pesagens do período, em kg por dia. */
fun AppData.weightSlope(days: Int = 28): Slope? {
    val from = addDays(startOfDay(System.currentTimeMillis()), -days)
    val pts = body.filter { it["peso"] != null && it.t >= from }.map { it.t / DAY_MS.toDouble() to it["peso"]!! }
    if (pts.size < 3) return null
    val span = pts.maxOf { it.first } - pts.minOf { it.first }
    if (span < 10) return null
    val mx = pts.sumOf { it.first } / pts.size
    val my = pts.sumOf { it.second } / pts.size
    val nu = pts.sumOf { (x, y) -> (x - mx) * (y - my) }
    val de = pts.sumOf { (x, _) -> (x - mx) * (x - mx) }
    return if (de != 0.0) Slope(nu / de, pts.size, span) else null
}

class Adaptive(val v: Double?, val src: String, val logged: Int, val weighIns: Int, val perWeek: Double? = null)

/**
 * Gasto adaptativo (como no MacroFactor): o que você comeu menos a energia da variação do peso, nas últimas 4 semanas.
 * 1 kg de peso corporal ≈ 7.700 kcal. Mistura com a fórmula enquanto há poucos dados.
 */
fun AppData.adaptiveTDEE(): Adaptive {
    val prior = formulaTDEE()
    val end = startOfDay(System.currentTimeMillis())
    val start = addDays(end, -28)
    val logged = mutableListOf<Double>()
    var t = start
    while (t < end) {
        val k = dayTotals(dayKey(t)).k
        if (k >= 600) logged += k
        t = addDays(t, 1)
    }
    val sl = weightSlope(28)
    val weighIns = sl?.n ?: body.count { it["peso"] != null && it.t >= start }
    if (logged.size < 10 || sl == null) return Adaptive(prior, "formula", logged.size, weighIns)
    val obs = logged.average() - sl.perDay * 7700
    val w = min(1.0, logged.size / 21.0) * min(1.0, sl.n / 6.0)
    val v = max(1200.0, min(5000.0, if (prior != null) prior * (1 - w) + obs * w else obs)).roundToInt().toDouble()
    return Adaptive(v, "dados", logged.size, weighIns, sl.perDay * 7)
}

fun AppData.currentTDEE(): Double? = food.tdee?.v ?: formulaTDEE()

/** Uma vez por semana o gasto é recalculado com os seus dados (devolve null se nada mudou). */
fun AppData.dietCheckIn(): AppData? {
    val D = food
    if (D.goal == null || (D.tdee != null && System.currentTimeMillis() - D.tdee.at < 7 * DAY_MS)) return null
    val a = adaptiveTDEE()
    val v = a.v ?: return null
    val old = D.tdee?.v
    val note = if (old != null && abs(old - v) >= 50) TdeeNote(old, v, System.currentTimeMillis()) else D.tdeeNote
    return copy(food = D.copy(tdee = Tdee(v, System.currentTimeMillis(), a.src), tdeeNote = note))
}

/** Metas do dia (calorias e macros) pelo objetivo, ritmo, gasto e peso. */
fun AppData.dietTargets(tdeeIn: Double? = null, goalIn: DietGoal? = food.goal): Macros? {
    if (goalIn == null) return null
    val g = goalIn.resolved()
    if (g.manual != null) return g.manual
    val tdee = tdeeIn ?: currentTDEE() ?: return null
    val w = dietWeight() ?: return null
    val rate = g.rate ?: 0.5
    var k = tdee
    if (g.obj == "perder") k = max(if (sex() == "m") 1500.0 else 1200.0, tdee - w * rate / 100 * 7700 / 7)
    if (g.obj == "ganhar") k = tdee + w * rate / 100 * 7700 / 7
    k = Math.round(k / 10) * 10.0
    val p = Math.round(w * (g.prot ?: 2.0)).toDouble()
    val f = Math.round(max(0.6 * w, k * 0.25 / 9)).toDouble()
    val c = max(0.0, Math.round((k - p * 4 - f * 9) / 4).toDouble())
    return Macros(k, p, c, f)
}

/** Meta de água: 35 ml por kg, arredondada a 50 ml. */
fun AppData.waterGoal(): Int = (Math.round((dietWeight() ?: 70.0) * 35 / 50) * 50).toInt()

class DayMicros(val tot: DoubleArray, val cover: Double, val fi: Double)

fun AppData.dayMicros(key: String): DayMicros {
    val e = dayOf(key).e
    val tot = DoubleArray(6)
    for (x in e) x.mi?.forEachIndexed { i, v -> if (i < 6) tot[i] += v }
    val kAll = e.sumOf { it.k ?: 0.0 }
    val kKnown = e.filter { it.mi != null }.sumOf { it.k ?: 0.0 }
    return DayMicros(tot, if (kAll > 0) kKnown / kAll else 0.0, e.sumOf { it.fi ?: 0.0 })
}

/* ---------------- Mudanças ---------------- */

fun AppData.updateFood(f: (FoodData) -> FoodData) = copy(food = f(food))

fun FoodData.withDay(key: String, f: (FoodDay) -> FoodDay): FoodData = copy(days = days + (key to f(days[key] ?: FoodDay())))

fun FoodData.pushRecent(id: String?): FoodData = if (id == null) this else copy(recent = (listOf(id) + recent.filter { it != id }).take(30))

fun FoodData.addEntry(key: String, entry: FoodEntry): FoodData =
    withDay(key) { it.copy(e = it.e + entry.copy(id = uid())) }.pushRecent(entry.ref)

fun FoodData.replaceEntry(key: String, entry: FoodEntry): FoodData =
    withDay(key) { d -> d.copy(e = d.e.map { if (it.id == entry.id) entry else it }) }

fun FoodData.deleteEntry(key: String, id: String): FoodData = withDay(key) { d -> d.copy(e = d.e.filter { it.id != id }) }

fun FoodData.toggleFav(id: String): FoodData = copy(fav = if (id in fav) fav - id else listOf(id) + fav)

fun FoodData.saveMeal(name: String, items: List<FoodEntry>): FoodData =
    copy(meals = meals + SavedMeal("m" + uid(), name, items.map { it.toMealItem() }))

/** Nome curto para mensagens ("Arroz, tipo 1, cozido" → "Arroz"). */
fun shortFoodName(n: String) = n.substringBefore(',')
