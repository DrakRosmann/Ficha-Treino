package app.ficha.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Dados embutidos no app (assets/data, gerados a partir dos .js do PWA por tools/convert_data.py). */
object Catalog {
    lateinit var groups: List<String>; private set
    lateinit var equipment: List<String>; private set
    lateinit var kinds: Map<String, Kind>; private set
    lateinit var muscles: Map<String, String>; private set
    lateinit var exercises: List<Exercise>; private set
    lateinit var byId: Map<String, Exercise>; private set
    lateinit var templates: List<Template>; private set
    lateinit var body: BodyMap; private set

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun load(context: Context) {
        if (::exercises.isInitialized) return
        val ex = json.decodeFromString<ExerciseFile>(context.read("data/exercises.json"))
        groups = ex.groups
        equipment = ex.equipment
        kinds = ex.kinds
        muscles = ex.muscles
        exercises = ex.exercises
        byId = exercises.associateBy { it.id }
        templates = json.decodeFromString(context.read("data/templates.json"))
        body = BodyMap.parse(json.decodeFromString<JsonObject>(context.read("data/body.json")))
    }

    private fun Context.read(path: String) = assets.open(path).bufferedReader().use { it.readText() }
}

@Serializable
private data class ExerciseFile(
    val groups: List<String>,
    val equipment: List<String>,
    val kinds: Map<String, Kind>,
    val muscles: Map<String, String>,
    val exercises: List<Exercise>,
)

/** Como registrar: a = primeiro campo (kg, +kg, seg, min), b = segundo (reps, km) ou nenhum. */
@Serializable
data class Kind(val label: String, val a: String, val b: String? = null)

@Serializable
data class Template(
    val id: String,
    val name: String,
    val level: String,
    val freq: String,
    val place: String,
    val desc: String,
    val routines: List<TemplateRoutine>,
)

@Serializable
data class TemplateRoutine(val name: String, val days: List<Int> = emptyList(), val items: List<RoutineItem>)

/** Desenho do corpo (frente e costas) do mapa muscular, adaptado de react-body-highlighter (MIT). */
class BodyMap(
    val front: List<Pair<String, List<FloatArray>>>,
    val back: List<Pair<String, List<FloatArray>>>,
    val regions: Map<String, List<String>>,
    val groupMuscles: Map<String, String>,
) {
    companion object {
        fun parse(o: JsonObject): BodyMap {
            fun figure(a: JsonArray) = a.map { region ->
                val r = region.jsonArray
                r[0].jsonPrimitive.content to r[1].jsonArray.map { poly ->
                    poly.jsonPrimitive.content.trim().split(Regex("\\s+")).map { it.toFloat() }.toFloatArray()
                }
            }
            return BodyMap(
                front = figure(o["front"]!!.jsonArray),
                back = figure(o["back"]!!.jsonArray),
                regions = (o["regions"] as JsonObject).mapValues { (_, v) -> v.jsonArray.map { it.jsonPrimitive.content } },
                groupMuscles = (o["groupMuscles"] as JsonObject).mapValues { (_, v) -> v.jsonPrimitive.content },
            )
        }
    }
}
