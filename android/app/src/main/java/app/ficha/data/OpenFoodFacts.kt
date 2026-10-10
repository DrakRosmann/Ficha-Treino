package app.ficha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Produtos por código de barras no Open Food Facts (base aberta e colaborativa, ODbL). */
object OpenFoodFacts {
    /** Produto encontrado; [missing] = sem tabela nutricional na base. */
    class Product(val food: Food, val missing: Boolean)

    suspend fun product(code: String): Product? = withContext(Dispatchers.IO) {
        val c = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,product_name_pt,brands,nutriments,serving_quantity,serving_size")
            .openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", "Ficha Android - github.com/DrakRosmann/Ficha-Treino")
        if (c.responseCode == 404) return@withContext null
        if (c.responseCode !in 200..299) throw IllegalStateException("http ${c.responseCode}")
        val d = Json.parseToJsonElement(c.inputStream.bufferedReader().use { it.readText() }).jsonObject
        if ((d["status"] as? JsonPrimitive)?.intOrNull != 1) return@withContext null
        val p = d["product"] as? JsonObject ?: return@withContext null
        val nm = p["nutriments"] as? JsonObject ?: JsonObject(emptyMap())
        fun s(k: String) = (p[k] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        fun n(k: String) = (nm[k] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.replace(',', '.').toDoubleOrNull() }
        val name = listOfNotNull(s("product_name_pt") ?: s("product_name"), s("brands")?.substringBefore(',')?.trim()).joinToString(" · ").ifEmpty { "Produto $code" }
        val kcal = n("energy-kcal_100g") ?: n("energy_100g")?.let { it / 4.184 }
        fun g(k: String) = Math.round((n(k) ?: 0.0) * 10) / 10.0
        val serving = (p["serving_quantity"] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }
        Product(
            Food(
                id = "c" + uid(), n = name, code = code, src = "Open Food Facts",
                k = (kcal ?: 0.0).roundToInt().toDouble(), p = g("proteins_100g"), c = g("carbohydrates_100g"), f = g("fat_100g"), fi = g("fiber_100g"),
                u = if (serving != null && serving > 0) listOf(Portion("porção", serving)) else emptyList(),
            ),
            missing = kcal == null,
        )
    }
}
