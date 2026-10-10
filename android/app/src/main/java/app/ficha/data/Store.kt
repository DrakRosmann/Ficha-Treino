package app.ficha.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.time.Instant

/**
 * Guarda os dados num arquivo JSON no aparelho, no mesmo formato do PWA.
 * Várias mudanças seguidas viram uma gravação só (depois de um instante); ao sair do app, grava na hora.
 */
class Store(context: Context) {
    private val file = File(context.filesDir, "ficha.json")
    private val prevFile = File(context.filesDir, "ficha.prev.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    /** Estado do Compose: as telas leem direto daqui e redesenham quando muda (sem atraso ao digitar). */
    var value: AppData by mutableStateOf(AppData())
        private set

    /** Partes do JSON que o app Android ainda não usa (dieta, corpo, fotos…): voltam intactas no backup. */
    private var extras = JsonObject(emptyMap())
    private var settingsExtras = JsonObject(emptyMap())

    init {
        try {
            if (file.exists()) applyJson(json.parseToJsonElement(file.readText()).jsonObject)
        } catch (e: Exception) {
            Log.e("Ficha", "Não foi possível ler os dados", e)
            // Guarda o arquivo com problema para não perder nada
            file.copyTo(File(file.parentFile, "ficha.broken.json"), overwrite = true)
        }
    }

    /** Chamado depois de cada mudança (a nuvem usa para enviar). */
    var onChange: (() -> Unit)? = null

    @Synchronized
    fun update(f: (AppData) -> AppData) {
        val before = value
        value = f(value)
        scheduleSave()
        if (value !== before) onChange?.invoke()
    }

    /** O JSON completo (com as partes que o app não conhece), no formato do PWA. */
    fun snapshot(): JsonObject = toJson(value)

    /** Troca tudo por este JSON (usado pela sincronização). */
    @Synchronized
    fun replace(o: JsonObject) {
        applyJson(o)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            writeNow()
        }
    }

    /** Grava já (ao minimizar ou fechar o app). [now]: grava nesta thread, antes de voltar. */
    fun flush(now: Boolean = false) {
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            if (now) writeNow() else scope.launch { writeNow() }
        }
    }

    @Synchronized
    private fun writeNow() {
        val tmp = File(file.parentFile, "ficha.json.tmp")
        tmp.writeText(json.encodeToString(JsonObject.serializer(), toJson(value)))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    private fun toJson(d: AppData): JsonObject {
        val typed = json.encodeToJsonElement(AppData.serializer(), d).jsonObject
        val settings = JsonObject(settingsExtras + typed["settings"]!!.jsonObject)
        return JsonObject(extras + typed + ("settings" to settings))
    }

    private fun applyJson(o: JsonObject) {
        val d = json.decodeFromJsonElement(AppData.serializer(), migrate(o))
        val known = AppData.serializer().descriptor.let { desc -> (0 until desc.elementsCount).map(desc::getElementName).toSet() }
        extras = JsonObject(o.filterKeys { it !in known && it != "app" && it != "exportedAt" && it != "photoData" })
        val knownSettings = Settings.serializer().descriptor.let { desc -> (0 until desc.elementsCount).map(desc::getElementName).toSet() }
        settingsExtras = JsonObject(((o["settings"] as? JsonObject) ?: JsonObject(emptyMap())).filterKeys { it !in knownSettings })
        value = d.copy(sessions = d.sessions.sortedByDescending { it.start })
    }

    /** Dados antigos do PWA (sem programas): as fichas existentes viram o programa "Meu treino". */
    private fun migrate(o: JsonObject): JsonObject {
        if (o["programs"] is JsonArray) return o
        val routines = (o["routines"] as? JsonArray) ?: return o
        if (routines.isEmpty()) return JsonObject(o + ("programs" to JsonArray(emptyList())))
        val pid = uid()
        val program = JsonObject(mapOf("id" to JsonPrimitive(pid), "name" to JsonPrimitive("Meu treino"), "active" to JsonPrimitive(true)))
        val moved = JsonArray(routines.map { r -> JsonObject((r as JsonObject) + ("programId" to JsonPrimitive(pid))) })
        return JsonObject(o + ("programs" to JsonArray(listOf(program))) + ("routines" to moved))
    }

    /* ---------------- Backup ---------------- */

    /** Arquivo de backup; com [photoData] ({ id: [foto, miniatura] } em base64) inclui as fotos, como o PWA. */
    fun exportJson(photoData: Map<String, Pair<String, String>>? = null): String {
        val ph = photoData?.takeIf { it.isNotEmpty() }?.let { m ->
            mapOf("photoData" to JsonObject(m.mapValues { (_, v) -> JsonArray(listOf(JsonPrimitive(v.first), JsonPrimitive(v.second))) }))
        } ?: emptyMap()
        val o = JsonObject(mapOf("app" to JsonPrimitive("ficha"), "exportedAt" to JsonPrimitive(Instant.now().toString())) + toJson(value) + ph)
        return (if (ph.isEmpty()) pretty else json).encodeToString(JsonObject.serializer(), o)
    }

    class Preview(val routines: Int, val sessions: Int, val photos: Int, val raw: JsonObject) {
        /** Fotos do progresso que vieram no arquivo. */
        val photoData: Map<String, Pair<String, String>>
            get() = (raw["photoData"] as? JsonObject)?.mapNotNull { (id, v) ->
                val a = v as? JsonArray ?: return@mapNotNull null
                val full = (a.getOrNull(0) as? JsonPrimitive)?.content ?: return@mapNotNull null
                id to (full to ((a.getOrNull(1) as? JsonPrimitive)?.content ?: full))
            }?.toMap() ?: emptyMap()
    }

    /** Confere se o texto é um backup do Ficha (do PWA ou deste app). */
    fun readBackup(text: String): Preview? = try {
        val o = json.parseToJsonElement(text).jsonObject
        val r = o["routines"] as? JsonArray
        val s = o["sessions"] as? JsonArray
        if (r == null || s == null) null else Preview(r.size, s.size, (o["photoData"] as? JsonObject)?.size ?: 0, o)
    } catch (e: Exception) {
        null
    }

    /** Substitui os dados pelos do backup, guardando uma cópia dos atuais para desfazer. */
    fun importBackup(p: Preview) {
        prevFile.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(toJson(value) + ("_at" to JsonPrimitive(System.currentTimeMillis())))))
        applyJson(JsonObject(p.raw - "photoData"))
        scheduleSave()
        onChange?.invoke()
    }

    /** Data da cópia de antes da última importação (válida por 30 dias), ou null. */
    fun previousCopyAt(): Long? = try {
        if (!prevFile.exists()) null
        else (json.parseToJsonElement(prevFile.readText()).jsonObject["_at"] as? JsonPrimitive)?.content?.toLongOrNull()
            ?.takeIf { System.currentTimeMillis() - it < 30L * 86_400_000 }
    } catch (e: Exception) {
        null
    }

    fun undoImport(): Boolean {
        if (previousCopyAt() == null) return false
        applyJson(JsonObject(json.parseToJsonElement(prevFile.readText()).jsonObject - "_at"))
        prevFile.delete()
        scheduleSave()
        return true
    }

    fun wipe() {
        extras = JsonObject(emptyMap())
        settingsExtras = JsonObject(emptyMap())
        prevFile.delete()
        update { AppData() }
    }

    /** Valores guardados pelo PWA que o app ainda não usa, para mostrar contagens em Ajustes. */
    fun extra(key: String): JsonElement? = extras[key]

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
        private val pretty = Json(json) { prettyPrint = true }
    }
}
