package app.ficha.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/* Dieta, corpo, perfil, fotos, conquistas e treinador — os mesmos campos do PWA (S.food, S.body…). */

@Serializable
data class FoodData(
    /** Chave AAAA-MM-DD */
    val days: Map<String, FoodDay> = emptyMap(),
    val custom: List<Food> = emptyList(),
    val fav: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    val goal: DietGoal? = null,
    val tdee: Tdee? = null,
    val tdeeNote: TdeeNote? = null,
    val meals: List<SavedMeal> = emptyList(),
)

@Serializable
data class FoodDay(
    val e: List<FoodEntry> = emptyList(),
    /** Água em ml */
    @Serializable(LenientInt::class) val w: Int = 0,
)

/** Um item registrado: macros já calculados para a quantidade [gr]. */
@Serializable
data class FoodEntry(
    val id: String = uid(),
    @Serializable(LenientInt::class) val m: Int = 0,
    val n: String = "",
    val ref: String? = null,
    @Serializable(LenientDouble::class) val gr: Double? = 0.0,
    @Serializable(LenientDouble::class) val q: Double? = null,
    val u: String? = null,
    @Serializable(LenientDouble::class) val k: Double? = 0.0,
    @Serializable(LenientDouble::class) val p: Double? = 0.0,
    @Serializable(LenientDouble::class) val c: Double? = 0.0,
    @Serializable(LenientDouble::class) val f: Double? = 0.0,
    @Serializable(LenientDouble::class) val fi: Double? = 0.0,
    /** Micronutrientes (TACO): sódio, cálcio, ferro, potássio, magnésio, vitamina C (mg) */
    val mi: List<Double>? = null,
    val ai: Boolean? = null,
)

/** Alimento (valores por 100 g). Porções caseiras em [u]: [rótulo, gramas]. */
@Serializable
data class Food(
    val id: String,
    val n: String,
    @Serializable(LenientDouble::class) val k: Double? = 0.0,
    @Serializable(LenientDouble::class) val p: Double? = 0.0,
    @Serializable(LenientDouble::class) val c: Double? = 0.0,
    @Serializable(LenientDouble::class) val f: Double? = 0.0,
    @Serializable(LenientDouble::class) val fi: Double? = 0.0,
    @Serializable(PortionListSerializer::class) val u: List<Portion> = emptyList(),
    val src: String? = null,
    val code: String? = null,
    /** Grupo da TACO (só dos alimentos embutidos) */
    val gr: Int? = null,
    val mi: List<Double>? = null,
)

data class Portion(val label: String, val g: Double)

/** Porções no formato do PWA: [["colher de sopa", 25], …] */
object PortionListSerializer : KSerializer<List<Portion>> {
    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor
    override fun serialize(encoder: Encoder, value: List<Portion>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonArray(value.map { JsonArray(listOf(JsonPrimitive(it.label), JsonPrimitive(it.g))) }))
    }
    override fun deserialize(decoder: Decoder): List<Portion> {
        val e = (decoder as JsonDecoder).decodeJsonElement() as? JsonArray ?: return emptyList()
        return e.mapNotNull { x ->
            val a = x as? JsonArray ?: return@mapNotNull null
            val label = (a.getOrNull(0) as? JsonPrimitive)?.content ?: return@mapNotNull null
            val g = (a.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            Portion(label, g)
        }
    }
}

@Serializable
data class DietGoal(
    /** perder | manter | ganhar */
    val obj: String = "manter",
    @Serializable(LenientDouble::class) val rate: Double? = 0.5,
    @Serializable(LenientDouble::class) val act: Double? = 1.55,
    @Serializable(LenientDouble::class) val prot: Double? = 2.0,
    val manual: Macros? = null,
)

@Serializable
data class Macros(
    @Serializable(LenientDouble::class) val k: Double? = 0.0,
    @Serializable(LenientDouble::class) val p: Double? = 0.0,
    @Serializable(LenientDouble::class) val c: Double? = 0.0,
    @Serializable(LenientDouble::class) val f: Double? = 0.0,
)

@Serializable
data class Tdee(
    @Serializable(LenientDouble::class) val v: Double? = null,
    @Serializable(LenientLong::class) val at: Long = 0,
    val src: String = "formula",
)

@Serializable
data class TdeeNote(
    @Serializable(LenientDouble::class) val from: Double? = 0.0,
    @Serializable(LenientDouble::class) val to: Double? = 0.0,
    @Serializable(LenientLong::class) val at: Long = 0,
)

@Serializable
data class SavedMeal(val id: String, val n: String, val items: List<MealItem> = emptyList())

/** Item de uma refeição pronta (um registro sem id e sem refeição). */
@Serializable
data class MealItem(
    val n: String = "",
    val ref: String? = null,
    @Serializable(LenientDouble::class) val gr: Double? = 0.0,
    @Serializable(LenientDouble::class) val q: Double? = null,
    val u: String? = null,
    @Serializable(LenientDouble::class) val k: Double? = 0.0,
    @Serializable(LenientDouble::class) val p: Double? = 0.0,
    @Serializable(LenientDouble::class) val c: Double? = 0.0,
    @Serializable(LenientDouble::class) val f: Double? = 0.0,
    @Serializable(LenientDouble::class) val fi: Double? = 0.0,
    val mi: List<Double>? = null,
    val ai: Boolean? = null,
) {
    fun toEntry(m: Int) = FoodEntry(id = uid(), m = m, n = n, ref = ref, gr = gr, q = q, u = u, k = k, p = p, c = c, f = f, fi = fi, mi = mi, ai = ai)
}

fun FoodEntry.toMealItem() = MealItem(n, ref, gr, q, u, k, p, c, f, fi, mi, ai)

/** Perfil (compartilhado entre o assistente de treino, a dieta e o corpo). */
@Serializable
data class Profile(
    val objetivo: String = "massa",
    val nivel: String = "iniciante",
    @Serializable(LenientInt::class) val dias: Int = 3,
    @Serializable(LenientInt::class) val tempo: Int = 60,
    val local: String = "academia",
    val sexo: String = "",
    @Serializable(LenientString::class) val idade: String = "",
    @Serializable(LenientString::class) val peso: String = "",
    @Serializable(LenientString::class) val altura: String = "",
    val foco: List<String> = emptyList(),
    val restr: List<String> = emptyList(),
    @Serializable(LenientString::class) val obs: String = "",
)

/** Registro do corpo: data, observação e os valores medidos (peso, gordura, cintura…). */
@Serializable(BodyEntrySerializer::class)
data class BodyEntry(val id: String, val t: Long, val nota: String? = null, val v: Map<String, Double> = emptyMap()) {
    operator fun get(k: String): Double? = v[k]
}

object BodyEntrySerializer : KSerializer<BodyEntry> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor
    override fun serialize(encoder: Encoder, value: BodyEntry) {
        val m = LinkedHashMap<String, JsonElement>()
        m["id"] = JsonPrimitive(value.id)
        m["t"] = JsonPrimitive(value.t)
        value.v.forEach { (k, x) -> m[k] = JsonPrimitive(x) }
        if (!value.nota.isNullOrEmpty()) m["nota"] = JsonPrimitive(value.nota)
        (encoder as JsonEncoder).encodeJsonElement(JsonObject(m))
    }
    override fun deserialize(decoder: Decoder): BodyEntry {
        val o = (decoder as JsonDecoder).decodeJsonElement() as JsonObject
        val v = HashMap<String, Double>()
        for ((k, x) in o) {
            if (k == "id" || k == "t" || k == "nota" || x is JsonNull) continue
            (x as? JsonPrimitive)?.doubleOrNull?.let { v[k] = it }
        }
        return BodyEntry(
            id = (o["id"] as? JsonPrimitive)?.content ?: uid(),
            t = (o["t"] as? JsonPrimitive)?.longOrNull ?: (o["t"] as? JsonPrimitive)?.doubleOrNull?.toLong() ?: 0,
            nota = (o["nota"] as? JsonPrimitive)?.content,
            v = v,
        )
    }
}

@Serializable
data class BodyGoal(@Serializable(LenientDouble::class) val peso: Double? = null)

/** Foto do progresso (a imagem fica em arquivos no aparelho). */
@Serializable
data class ProgressPhoto(
    val id: String,
    @Serializable(LenientLong::class) val t: Long,
    /** frente | lado | costas */
    val pose: String = "frente",
    @Serializable(LenientInt::class) val kb: Int = 0,
)

@Serializable
data class CoachData(val report: CoachReport? = null, val chat: List<ChatMsg> = emptyList())

@Serializable
data class CoachReport(@Serializable(LenientLong::class) val at: Long, val week: String, val data: CoachReportData)

@Serializable
data class CoachReportData(
    val resumo: String = "",
    val destaques: List<String> = emptyList(),
    val atencao: List<String> = emptyList(),
    val recomendacoes: List<CoachRec> = emptyList(),
    val metas_semana: List<String> = emptyList(),
)

@Serializable
data class CoachRec(val titulo: String = "", val detalhe: String = "")

/** Mensagem da conversa: r = "u" (usuário) ou "a" (treinador). */
@Serializable
data class ChatMsg(val r: String, val x: String, @Serializable(LenientLong::class) val at: Long = 0)

/** Conquistas: id → quando desbloqueou (1 = já tinha antes desta versão). */
object AchMapSerializer : KSerializer<Map<String, Long>> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Map<String, Long>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonObject(value.mapValues { JsonPrimitive(it.value) }))
    }
    override fun deserialize(decoder: Decoder): Map<String, Long> {
        val o = (decoder as JsonDecoder).decodeJsonElement() as? JsonObject ?: return emptyMap()
        return o.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.let { p -> (p.longOrNull ?: p.doubleOrNull?.toLong())?.let { k to it } } }.toMap()
    }
}
