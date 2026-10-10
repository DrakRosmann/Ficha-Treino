package app.ficha.data

import android.content.Context
import app.ficha.logic.norm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlin.math.roundToInt

/**
 * Banco de alimentos da Dieta: Tabela TACO (4ª ed., NEPA/UNICAMP) com micronutrientes e itens comuns de academia.
 * Valores por 100 g. Ids: "t<n>" (TACO), "x<n>" (extras) e "c…" (criados pelo usuário).
 */
object Foods {
    lateinit var groups: List<String>; private set
    lateinit var all: List<Food>; private set
    lateinit var byId: Map<String, Food>; private set
    private lateinit var normNames: Map<String, String>

    val MEALS = listOf("Café da manhã", "Almoço", "Lanche", "Jantar")
    fun mealShort(m: Int) = MEALS[m].replace("Café da manhã", "Café")

    // Porções caseiras por tipo de alimento (gramas aproximadas), pelo nome sem acento
    private val UNIT_RULES: List<Pair<Regex, List<Portion>>> = listOf(
        "^arroz\\b.*cozid" to listOf(Portion("colher de sopa", 25.0), Portion("escumadeira", 80.0)),
        "^feijao\\b.*cozid" to listOf(Portion("concha", 100.0), Portion("colher de sopa", 20.0)),
        "^ovo\\b.*inteiro" to listOf(Portion("unidade", 50.0)),
        "^ovo\\b.*clara" to listOf(Portion("unidade", 30.0)),
        "^ovo\\b.*gema" to listOf(Portion("unidade", 18.0)),
        "^pao\\b.*frances" to listOf(Portion("unidade", 50.0)),
        "^pao\\b.*forma" to listOf(Portion("fatia", 25.0)),
        "^pao\\b" to listOf(Portion("fatia", 30.0)),
        "^banana\\b" to listOf(Portion("unidade", 70.0)),
        "^maca\\b" to listOf(Portion("unidade", 130.0)),
        "^(laranja|tangerina|pera|pessego|kiwi|caqui)\\b" to listOf(Portion("unidade", 140.0)),
        "^(mamao|melao|melancia|abacaxi|manga)\\b" to listOf(Portion("fatia", 120.0)),
        "^(morango|uva|acerola)\\b" to listOf(Portion("xícara", 140.0)),
        "^(leite|iogurte|bebida lactea)\\b" to listOf(Portion("copo de 200 ml", 206.0)),
        "^cafe\\b.*infus" to listOf(Portion("xícara", 50.0)),
        "^(suco|refrigerante|agua de coco|cerveja|bebida)" to listOf(Portion("copo de 200 ml", 200.0), Portion("lata de 350 ml", 350.0)),
        "^(aveia|farelo|farinha)\\b" to listOf(Portion("colher de sopa", 15.0)),
        "^(azeite|oleo)\\b" to listOf(Portion("colher de sopa", 13.0), Portion("colher de chá", 4.0)),
        "^(manteiga|margarina)\\b" to listOf(Portion("colher de chá", 5.0), Portion("colher de sopa", 15.0)),
        "^acucar\\b" to listOf(Portion("colher de chá", 5.0), Portion("colher de sopa", 12.0)),
        "^(mel|doce|geleia)\\b" to listOf(Portion("colher de sopa", 20.0)),
        "^queijo\\b" to listOf(Portion("fatia", 20.0)),
        "^requeijao\\b" to listOf(Portion("colher de sopa", 15.0)),
        "^(frango|carne|peixe|tilapia|salmao|atum|porco|lombo|file|bife|figado|peru|pescada|merluza|sardinha)\\b" to listOf(Portion("filé", 100.0), Portion("pedaço", 60.0)),
        "^(batata|mandioca|aipim|inhame|cara|abobora)\\b" to listOf(Portion("pedaço", 100.0), Portion("colher de sopa", 25.0)),
        "^(macarrao|massa|lasanha|nhoque)\\b" to listOf(Portion("pegador", 110.0), Portion("prato raso", 200.0)),
        "^cuscuz\\b" to listOf(Portion("fatia", 100.0)),
        "^tapioca\\b" to listOf(Portion("unidade", 90.0)),
        "^(alface|rucula|agriao|couve|espinafre|repolho|acelga)\\b" to listOf(Portion("xícara", 20.0)),
        "^tomate\\b" to listOf(Portion("unidade", 100.0), Portion("fatia", 15.0)),
        "^(cenoura|beterraba|pepino|abobrinha|chuchu|vagem|quiabo)\\b" to listOf(Portion("colher de sopa", 15.0), Portion("unidade", 100.0)),
        "^(brocolis|couve-flor|couve flor)\\b" to listOf(Portion("ramo", 30.0)),
        "^(amendoim|castanha|noz|nozes|amendoa|macadamia|pistache)" to listOf(Portion("punhado", 30.0)),
        "^(biscoito|bolacha)\\b" to listOf(Portion("unidade", 7.0)),
        "^(bolo|torta|pizza)\\b" to listOf(Portion("fatia", 80.0)),
    ).map { (re, u) -> Regex(re) to u }

    fun unitsFor(name: String): List<Portion> {
        val n = norm(name)
        return UNIT_RULES.firstOrNull { it.first.containsMatchIn(n) }?.second ?: emptyList()
    }

    fun load(context: Context) {
        if (::all.isInitialized) return
        val o = Json.parseToJsonElement(context.assets.open("data/foods.json").bufferedReader().use { it.readText() }) as JsonObject
        groups = o["groups"]!!.jsonArray.map { (it as JsonPrimitive).content }
        fun d(a: JsonArray, i: Int) = (a.getOrNull(i) as? JsonPrimitive)?.doubleOrNull ?: 0.0
        val taco = o["taco"]!!.jsonArray.map { x ->
            val a = x.jsonArray
            val n = (a[1] as JsonPrimitive).content
            Food(
                id = "t" + (a[0] as JsonPrimitive).content, n = n, gr = (a[2] as JsonPrimitive).intOrNull,
                k = d(a, 3), p = d(a, 4), c = d(a, 5), f = d(a, 6), fi = d(a, 7),
                mi = (8..13).map { d(a, it) }, u = unitsFor(n), src = "TACO",
            )
        }
        val extra = o["extra"]!!.jsonArray.map { x ->
            val a = x.jsonArray
            val u = (a.getOrNull(8) as? JsonArray)?.let { p -> listOf(Portion((p[0] as JsonPrimitive).content, (p[1] as JsonPrimitive).doubleOrNull ?: 100.0)) } ?: emptyList()
            Food(
                id = "x" + (a[0] as JsonPrimitive).content, n = (a[1] as JsonPrimitive).content, gr = (a[2] as JsonPrimitive).intOrNull,
                k = d(a, 3), p = d(a, 4), c = d(a, 5), f = d(a, 6), fi = d(a, 7), u = u, src = "média de rótulos",
            )
        }
        all = taco + extra
        byId = all.associateBy { it.id }
        normNames = all.associate { it.id to norm(it.n) }
    }

    fun get(data: FoodData, id: String?): Food? = id?.let { byId[it] ?: data.custom.find { c -> c.id == id } }

    /** Busca por palavras (sem acento), favorecendo começo de palavra, favoritos e recentes. */
    fun search(data: FoodData, q: String): List<Food> {
        val toks = norm(q).split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
        if (toks.isEmpty()) return emptyList()
        val fav = data.fav.toSet()
        val recent = data.recent.toSet()
        val out = ArrayList<Pair<Double, Food>>()
        for (f in data.custom + all) {
            val nn = normNames[f.id] ?: norm(f.n)
            var score = 0.0
            var ok = true
            for (t in toks) {
                val i = nn.indexOf(t)
                if (i < 0) { ok = false; break }
                score += if (i == 0) 3.0 else if (nn[i - 1] in " ,(/-") 2.0 else 0.5
            }
            if (!ok) continue
            score -= nn.length / 40.0
            if (Regex("\\bcru[as]?\\b").containsMatchIn(nn)) score -= 0.8
            if (f.id in fav) score += 2
            if (f.id in recent) score += 1
            if (f.id.startsWith("c")) score += 0.5
            out += score to f
        }
        return out.sortedByDescending { it.first }.take(60).map { it.second }
    }

    /** Macros de uma quantidade em gramas, arredondados como no PWA. */
    fun entryFor(f: Food, g: Double, m: Int, q: Double?, u: String?): FoodEntry {
        val r = g / 100
        fun r1(v: Double?) = Math.round((v ?: 0.0) * r * 10) / 10.0
        return FoodEntry(
            id = uid(), m = m, n = f.n, ref = f.id.ifEmpty { null }, gr = Math.round(g * 10) / 10.0, q = q, u = u,
            k = ((f.k ?: 0.0) * r).roundToInt().toDouble(), p = r1(f.p), c = r1(f.c), f = r1(f.f), fi = r1(f.fi),
            mi = f.mi?.map { Math.round(it * r * 10) / 10.0 },
        )
    }

    /** "colher de sopa" → "colheres de sopa" (só a primeira palavra). */
    fun unitLabel(q: Double?, u: String): String {
        if (q == null || q <= 1) return u
        val first = u.substringBefore(' ')
        val rest = u.removePrefix(first)
        val pl = when {
            first.endsWith("ão") -> first.dropLast(2) + "ões"
            first.endsWith("r") || first.endsWith("z") -> first + "es"
            first.endsWith("l") -> first.dropLast(1) + "is"
            else -> first + "s"
        }
        return pl + rest
    }
}
