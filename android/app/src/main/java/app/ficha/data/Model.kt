package app.ficha.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.random.Random

/*
 * Os mesmos dados do PWA (localStorage "ficha.v1"), para o backup .json valer nos dois apps.
 * Campos que o app Android ainda não usa (dieta, corpo, fotos…) são guardados à parte pelo Store
 * e voltam intactos no backup exportado.
 */

@Serializable
data class AppData(
    val v: Int = 1,
    val settings: Settings = Settings(),
    val custom: List<Exercise> = emptyList(),
    val programs: List<Program> = emptyList(),
    val routines: List<Routine> = emptyList(),
    /** Do mais recente para o mais antigo. */
    val sessions: List<Session> = emptyList(),
    val active: ActiveWorkout? = null,
    val videos: Map<String, String> = emptyMap(),
    val profile: Profile? = null,
    val body: List<BodyEntry> = emptyList(),
    val bodyGoal: BodyGoal = BodyGoal(),
    val food: FoodData = FoodData(),
    val photos: List<ProgressPhoto> = emptyList(),
    /** Conquistas desbloqueadas (null = ainda não conferidas nesta instalação) */
    @Serializable(AchMapSerializer::class) val ach: Map<String, Long>? = null,
    val coach: CoachData? = null,
    /** Quando os dados mudaram por último (usado na sincronização entre aparelhos) */
    @Serializable(LenientLong::class) val mt: Long = 0,
)

@Serializable
data class Settings(
    @Serializable(LenientInt::class) val rest: Int = 90,
    val sound: Boolean = true,
    /** auto | light | dark | black */
    val theme: String = "auto",
    val accent: String = "limao",
    val keepAwake: Boolean = true,
    val progression: Boolean = true,
    val rir: Boolean = true,
    @Serializable(LenientDouble::class) val bar: Double? = 20.0,
    val plates: List<Double>? = null,
    @Serializable(LenientLong::class) val lastBackup: Long = 0,
    @Serializable(LenientLong::class) val backupSnooze: Long = 0,
    /** Só no Android: cores do papel de parede (Material You) em vez da cor escolhida. */
    val androidDynamic: Boolean = false,
    /** Só no Android: estilo da paleta gerada a partir da cor (tonal | vibrant | expressive). */
    val androidPalette: String = "vibrant",
)

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val group: String = "Funcional",
    val equip: String = "Outro",
    val kind: String = "w",
    val img: String? = null,
    val en: String = "",
    val primary: List<String> = emptyList(),
    val secondary: List<String> = emptyList(),
    val custom: Boolean = false,
)

@Serializable
data class Program(
    val id: String,
    val name: String = "",
    val active: Boolean = true,
)

@Serializable
data class Routine(
    val id: String,
    val name: String = "",
    val days: List<Int> = emptyList(),
    val items: List<RoutineItem> = emptyList(),
    val programId: String? = null,
)

@Serializable
data class RoutineItem(
    val id: String = uid(),
    val exId: String,
    @Serializable(LenientInt::class) val sets: Int = 3,
    @Serializable(LenientString::class) val reps: String = "",
    @Serializable(LenientInt::class) val rest: Int = 90,
    @Serializable(LenientString::class) val note: String = "",
    val ss: String? = null,
)

@Serializable
data class Session(
    val id: String,
    val name: String = "Treino",
    val routineId: String? = null,
    @Serializable(LenientLong::class) val start: Long,
    @Serializable(LenientLong::class) val end: Long,
    @Serializable(LenientString::class) val notes: String = "",
    val exercises: List<SessionExercise> = emptyList(),
    val prs: List<PR> = emptyList(),
    /** Conquistas desbloqueadas neste treino */
    val ach: List<String>? = null,
)

@Serializable
data class SessionExercise(
    val exId: String,
    val name: String = "",
    val kind: String = "w",
    val ss: String? = null,
    val sets: List<SetRecord> = emptyList(),
)

@Serializable
data class SetRecord(
    @Serializable(LenientDouble::class) val a: Double? = null,
    @Serializable(LenientDouble::class) val b: Double? = null,
    val warm: Boolean = false,
    /** drop | fail */
    val t: String? = null,
    @Serializable(LenientDouble::class) val rir: Double? = null,
)

@Serializable
data class PR(
    val exId: String,
    val name: String = "",
    val label: String = "",
    @Serializable(LenientDouble::class) val value: Double? = 0.0,
    val unit: String = "",
)

@Serializable
data class ActiveWorkout(
    val id: String,
    val name: String,
    val routineId: String? = null,
    @Serializable(LenientLong::class) val start: Long,
    @Serializable(LenientString::class) val notes: String = "",
    val rest: RestTimer? = null,
    val exercises: List<ActiveExercise> = emptyList(),
)

@Serializable
data class RestTimer(
    @Serializable(LenientLong::class) val end: Long,
    @Serializable(LenientInt::class) val total: Int,
)

@Serializable
data class ActiveExercise(
    val uid: String = uid(),
    val exId: String,
    val name: String = "Exercício",
    val kind: String = "w",
    @Serializable(LenientString::class) val target: String = "",
    @Serializable(LenientInt::class) val rest: Int = 90,
    @Serializable(LenientString::class) val note: String = "",
    val ss: String? = null,
    val sets: List<ActiveSet> = emptyList(),
)

/** Série do treino em andamento: os campos guardam o texto digitado (como no PWA). */
@Serializable
data class ActiveSet(
    @Serializable(LenientString::class) val a: String = "",
    @Serializable(LenientString::class) val b: String = "",
    val done: Boolean = false,
    val warm: Boolean = false,
    val t: String? = null,
    @Serializable(LenientString::class) val rir: String = "",
)

/** Id curto e único, no mesmo formato do PWA (tempo em base 36 + sufixo aleatório). */
fun uid(): String =
    System.currentTimeMillis().toString(36) + (1..5).map { "0123456789abcdefghijklmnopqrstuvwxyz"[Random.nextInt(36)] }.joinToString("")

/* ---------- Leitura tolerante: o JavaScript às vezes grava número como texto e vice-versa ---------- */

private fun JsonElement.primitiveOrNull(): JsonPrimitive? = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }

object LenientString : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String {
        val p = (decoder as? JsonDecoder)?.decodeJsonElement()?.primitiveOrNull() ?: return ""
        val c = p.content
        // 12.0 → "12" (número inteiro vindo do JSON)
        return if (!p.isString && c.endsWith(".0")) c.dropLast(2) else c
    }
}

object LenientDouble : KSerializer<Double?> {
    override val descriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) encoder.encodeLong(value.toLong()) else encoder.encodeDouble(value)
    }
    override fun deserialize(decoder: Decoder): Double? {
        val p = (decoder as? JsonDecoder)?.decodeJsonElement()?.primitiveOrNull() ?: return null
        return p.doubleOrNull ?: p.content.replace(',', '.').toDoubleOrNull()
    }
}

object LenientInt : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int {
        val p = (decoder as? JsonDecoder)?.decodeJsonElement()?.primitiveOrNull() ?: return 0
        return (p.doubleOrNull ?: p.content.replace(',', '.').toDoubleOrNull() ?: 0.0).toInt()
    }
}

object LenientLong : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long {
        val p = (decoder as? JsonDecoder)?.decodeJsonElement()?.primitiveOrNull() ?: return 0
        return (p.doubleOrNull ?: p.content.toDoubleOrNull() ?: 0.0).toLong()
    }
}
