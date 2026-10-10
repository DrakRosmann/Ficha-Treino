package app.ficha.sync

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ficha.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CloudError(message: String) : Exception(message)

/* ================= Criptografia (igual ao cloud.js do PWA) =================
   Os dados saem do aparelho embaralhados com o código de sincronização (AES-GCM, chave por PBKDF2);
   o servidor só guarda um arquivo que não consegue ler. */
object CloudCrypto {
    const val ALPHA = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // sem 0/O, 1/I/L
    private val rnd = SecureRandom()

    fun newCode(): String {
        val sb = StringBuilder()
        val buf = ByteArray(32)
        while (sb.length < 20) {
            rnd.nextBytes(buf)
            for (b in buf) {
                val v = b.toInt() and 0xFF
                if (v < 248 && sb.length < 20) sb.append(ALPHA[v % 31])
            }
        }
        return sb.toString()
    }

    fun fmtCode(c: String) = c.chunked(4).joinToString("-")
    fun cleanCode(s: String) = s.uppercase().filter { it in ALPHA }

    fun sha256hex(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    class Keys(val code: String, val key: SecretKey, val id: String, val auth: String)

    private var cache: Keys? = null

    fun keys(code: String): Keys {
        cache?.let { if (it.code == code) return it }
        val spec = PBEKeySpec(code.toCharArray(), "ficha-sync-v1".toByteArray(), 150_000, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Keys(code, SecretKeySpec(raw, "AES"), sha256hex("ficha-id:$code"), sha256hex("ficha-auth:$code")).also { cache = it }
    }

    private val b64e = Base64.getEncoder()
    private val b64d = Base64.getDecoder()

    fun seal(text: String, k: Keys): String {
        val z = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(text.toByteArray()) } }.toByteArray()
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, k.key, GCMParameterSpec(128, iv)) }
        return "g1:${b64e.encodeToString(iv)}:${b64e.encodeToString(c.doFinal(z))}"
    }

    fun unseal(str: String, k: Keys): String {
        val parts = str.split(':')
        if (parts.size != 3) throw CloudError("Dados da nuvem em formato desconhecido")
        val data = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, k.key, GCMParameterSpec(128, b64d.decode(parts[1]))) }.doFinal(b64d.decode(parts[2]))
        } catch (e: Exception) {
            throw CloudError("Não foi possível abrir os dados da nuvem com este código")
        }
        val plain = if (parts[0] == "g1") GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() } else data
        return String(plain)
    }
}

/* ================= Mesclagem entre aparelhos =================
   Três vias: compara o que mudou aqui e na nuvem desde a última sincronização (a "base").
   Mudou só de um lado → fica a mudança. Mudou dos dois → vale a versão do aparelho que mexeu por último.
   Excluir de um lado e não mexer do outro → excluído. (null = não existe) */
object Merge {
    private fun J(x: JsonElement?) = x?.toString()
    private fun isObj(x: JsonElement?) = x is JsonObject
    private fun idList(vararg arrs: JsonElement?) = arrs.all { a -> a is JsonArray && a.all { it is JsonObject && it["id"] != null && it["id"] !is kotlinx.serialization.json.JsonNull } }
    private fun idOf(x: JsonElement) = (x as JsonObject)["id"].toString()

    fun any(b: JsonElement?, l: JsonElement?, r: JsonElement?, newerL: Boolean): JsonElement? {
        val sl = J(l)
        val sr = J(r)
        val sb = J(b)
        if (sl == sr) return l
        if (sl == sb) return r // só a nuvem mudou (inclusive excluir)
        if (sr == sb) return l // só este aparelho mudou
        if (l == null) return r // editar vence excluir
        if (r == null) return l
        if (isObj(l) && isObj(r)) return obj(b, l, r, newerL)
        if (idList(l, r)) return list(b, l, r, newerL)
        return if (newerL) l else r
    }

    fun obj(b0: JsonElement?, l: JsonElement?, r: JsonElement?, newerL: Boolean, fields: Map<String, (JsonElement?, JsonElement?, JsonElement?) -> JsonElement?> = emptyMap()): JsonElement? {
        val b = b0 as? JsonObject ?: JsonObject(emptyMap())
        if (l !is JsonObject || r !is JsonObject) return any(b, l, r, newerL)
        val out = LinkedHashMap<String, JsonElement>()
        for (k in (l.keys + r.keys)) {
            val v = fields[k]?.invoke(b[k], l[k], r[k]) ?: if (fields.containsKey(k)) null else any(b[k], l[k], r[k], newerL)
            if (v != null) out[k] = v
        }
        return JsonObject(out)
    }

    /** Listas de registros com id: junta por id; a ordem segue o lado que reordenou. */
    private fun list(b0: JsonElement?, l0: JsonElement?, r0: JsonElement?, newerL: Boolean): JsonElement? {
        val b = b0 as? JsonArray ?: JsonArray(emptyList())
        val l = l0 as? JsonArray ?: JsonArray(emptyList())
        val r = r0 as? JsonArray ?: JsonArray(emptyList())
        if (!idList(b, l, r)) return any(b, l, r, newerL)
        val bm = b.associateBy(::idOf)
        val lm = l.associateBy(::idOf)
        val rm = r.associateBy(::idOf)
        fun moved(a: JsonArray, m: Map<String, JsonElement>) =
            a.map(::idOf).filter { it in bm } != b.map(::idOf).filter { it in m }
        val lFirst = moved(l, lm) || !moved(r, rm)
        val (p, q) = if (lFirst) l to r else r to l
        val pIds = p.map(::idOf)
        val out = mutableListOf<JsonElement>()
        for (id in pIds + q.map(::idOf).filter { it !in pIds.toSet() }) {
            any(bm[id], lm[id], rm[id], newerL)?.let { out += it }
        }
        return JsonArray(out)
    }

    /** Ajustes que seguem a conta; os outros (tema, estilo, tela acesa…) são de cada aparelho */
    private val SYNC_SETTINGS = listOf("rest", "sound", "progression", "rir", "bar", "accent", "timerShortcut")

    private fun mt(o: JsonObject?) = (o?.get("mt") as? JsonPrimitive)?.longOrNull ?: 0

    fun state(B: JsonObject?, L: JsonObject, R: JsonObject): JsonObject {
        val nl = mt(L) >= mt(R)
        fun sorted(cmp: Comparator<JsonElement>): (JsonElement?, JsonElement?, JsonElement?) -> JsonElement? = { b, l, r ->
            val v = any(b, l, r, nl)
            if (v is JsonArray) JsonArray(v.sortedWith(cmp)) else v
        }
        fun num(e: JsonElement, k: String) = ((e as? JsonObject)?.get(k) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
        return obj(
            B, L, R, nl,
            mapOf(
                "sessions" to sorted(compareByDescending { num(it, "start") }),
                "body" to sorted(compareBy { num(it, "t") }),
                "settings" to { b, l, r ->
                    val out = LinkedHashMap((l as? JsonObject) ?: emptyMap())
                    for (k in SYNC_SETTINGS) {
                        val v = any((b as? JsonObject)?.get(k), (l as? JsonObject)?.get(k), (r as? JsonObject)?.get(k), nl)
                        if (v == null) out.remove(k) else out[k] = v
                    }
                    JsonObject(out)
                },
                "ach" to { _, l, r ->
                    if (l == null && r == null) null
                    else {
                        val out = LinkedHashMap((r as? JsonObject) ?: emptyMap())
                        for ((k, v) in (l as? JsonObject) ?: emptyMap()) {
                            val a = (out[k] as? JsonPrimitive)?.longOrNull
                            val bv = (v as? JsonPrimitive)?.longOrNull ?: 0
                            out[k] = if (a != null && a != 0L) JsonPrimitive(minOf(a, bv)) else v
                        }
                        JsonObject(out)
                    }
                },
                "active" to { _, _, _ -> null },
                "photos" to { _, _, _ -> null },
                "mt" to { _, _, _ -> JsonPrimitive(maxOf(mt(L), mt(R))) },
            ),
        ) as JsonObject
    }
}

/* ================= Sincronização ================= */
object CloudSync {
    private const val PREFS = "nuvem"
    const val HELP = "https://github.com/DrakRosmann/Ficha-Treino/blob/main/server/README.md"

    private lateinit var app: Context
    private lateinit var store: Store
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var pushJob: Job? = null
    private var pulledAt = 0L
    @Volatile private var applying = false

    /** Estado para a tela: sincronizando, erro e quando sincronizou por último. */
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf(""); private set
    var lastAt by mutableStateOf(0L); private set

    private val prefs get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Muda quando o endereço ou o código mudam, para a tela se atualizar. */
    private var rev by mutableIntStateOf(0)
    val url: String get() = rev.let { prefs.getString("url", "") ?: "" }
    val code: String? get() = rev.let { prefs.getString("code", null) }
    val on: Boolean get() = url.isNotEmpty() && !code.isNullOrEmpty()

    fun init(context: Context, s: Store) {
        app = context.applicationContext
        store = s
        lastAt = prefs.getLong("at", 0)
        store.onChange = { if (!applying) soon() }
        if (on) scope.launch { delay(300); sync(pull = true) }
    }

    fun setUrl(v0: String) {
        var v = v0.trim().trimEnd('/')
        if (v.isNotEmpty() && !Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(v)) v = "https://$v"
        if (v != url) { prefs.edit().putString("url", v).remove("v").apply(); rev++ }
    }

    private var version: Long?
        get() = if (prefs.contains("v")) prefs.getLong("v", 0) else null
        set(x) = prefs.edit().apply { if (x == null) remove("v") else putLong("v", x) }.apply()

    /* ---------- HTTP ---------- */
    class Res(val status: Int, val j: JsonObject)

    suspend fun api(method: String, path: String, body: JsonObject? = null, auth: String? = null): Res = withContext(Dispatchers.IO) {
        try {
            val c = URL(url.trimEnd('/') + path).openConnection() as HttpURLConnection
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 60_000
            c.useCaches = false
            c.setRequestProperty("Content-Type", "application/json")
            if (auth != null) c.setRequestProperty("Authorization", "Bearer $auth")
            if (body != null) {
                c.doOutput = true
                c.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val status = c.responseCode
            val text = (if (status >= 400) c.errorStream else c.inputStream)?.bufferedReader()?.use { it.readText() } ?: ""
            Res(status, runCatching { Json.parseToJsonElement(text).jsonObject }.getOrDefault(JsonObject(emptyMap())))
        } catch (e: Exception) {
            Log.w("FichaSync", "$method $path falhou", e)
            throw CloudError("Sem conexão com o servidor")
        }
    }

    fun httpErr(r: Res) = CloudError(
        when (r.status) {
            403 -> "O servidor recusou o código"
            413 -> "Dados grandes demais para o servidor"
            else -> "Erro do servidor (${r.status}${(r.j["message"] as? JsonPrimitive)?.content?.let { ": $it" } ?: ""})"
        },
    )

    /* ---------- Base (última versão em comum) ---------- */
    private val baseFile get() = File(app.filesDir, "sync_base.json")
    private var base: String? = null
    private var baseCmp: String = ""

    private fun loadBase(id: String) {
        if (base != null) return
        val o = runCatching { Json.parseToJsonElement(baseFile.readText()).jsonObject }.getOrNull()
        base = if (o != null && (o["id"] as? JsonPrimitive)?.content == id) (o["s"] as? JsonPrimitive)?.content ?: "" else ""
        baseCmp = base!!.takeIf { it.isNotEmpty() }?.let { cmp(Json.parseToJsonElement(it).jsonObject) } ?: ""
    }

    private fun setBase(id: String, o: JsonObject?) {
        base = o?.toString() ?: ""
        baseCmp = o?.let { cmp(it) } ?: ""
        if (o == null) baseFile.delete()
        else baseFile.writeText(buildJsonObject { put("id", id); put("s", base) }.toString())
    }

    private fun forgetBase() {
        base = null
        baseCmp = ""
        baseFile.delete()
    }

    /** O que vai para a nuvem: tudo menos o treino em andamento e as fotos. */
    private fun syncable(o: JsonObject) = JsonObject(o - "active" - "photos" - "app" - "exportedAt")
    /** Para comparar, ignora a data de modificação e a ordem das chaves */
    private fun cmp(o: JsonObject): String = canon(JsonObject(o + ("mt" to JsonPrimitive(0))))
    private fun canon(e: JsonElement): String = when (e) {
        is JsonObject -> e.entries.sortedBy { it.key }.joinToString(",", "{", "}") { "\"${it.key}\":${canon(it.value)}" }
        is JsonArray -> e.joinToString(",", "[", "]") { canon(it) }
        else -> e.toString()
    }

    private fun mergeIn(remote: JsonObject) {
        val local = store.snapshot()
        val merged = Merge.state(base?.takeIf { it.isNotEmpty() }?.let { Json.parseToJsonElement(it).jsonObject }, syncable(local), remote).toMutableMap()
        local["active"]?.let { merged["active"] = it }
        local["photos"]?.let { merged["photos"] = it }
        if (local["ach"] != null && merged["ach"] == null) merged["ach"] = local["ach"]!!
        val m = JsonObject(merged)
        if (cmp(m) == cmp(local)) return
        applying = true
        try { store.replace(m) } finally { applying = false }
    }

    /** pull: busca a nuvem antes; push: envia direto e só busca se houver conflito */
    suspend fun sync(pull: Boolean = false) {
        if (!on) return
        mutex.withLock {
            busy = true
            try {
                val k = withContext(Dispatchers.Default) { CloudCrypto.keys(code!!) }
                loadBase(k.id)
                var remote: Pair<Long, String?>? = null
                if (pull || version == null) {
                    val r = api("GET", "/v1/data/${k.id}", null, k.auth)
                    remote = when (r.status) {
                        200 -> ((r.j["v"] as? JsonPrimitive)?.longOrNull ?: 0) to (r.j["d"] as? JsonPrimitive)?.content
                        404 -> 0L to null
                        else -> throw httpErr(r)
                    }
                    pulledAt = System.currentTimeMillis()
                }
                for (tries in 0 until 4) {
                    if (remote != null && remote.first != version) {
                        val d = remote.second
                        if (d != null) {
                            val rs = Json.parseToJsonElement(CloudCrypto.unseal(d, k)).jsonObject
                            withContext(Dispatchers.Main) { mergeIn(rs) }
                            setBase(k.id, rs)
                        } else setBase(k.id, null)
                        version = remote.first
                    }
                    remote = null
                    val cur = syncable(store.snapshot())
                    if (cmp(cur) == baseCmp) break // nada novo para enviar
                    applying = true
                    try { withContext(Dispatchers.Main) { store.update { it.copy(mt = System.currentTimeMillis()) } } } finally { applying = false }
                    val data = syncable(store.snapshot())
                    val body = buildJsonObject { put("base", version ?: 0); put("d", CloudCrypto.seal(data.toString(), k)) }
                    val r = api("PUT", "/v1/data/${k.id}", body, k.auth)
                    if (r.status == 200) {
                        version = (r.j["v"] as? JsonPrimitive)?.longOrNull
                        setBase(k.id, data)
                        break
                    }
                    if (r.status == 409) {
                        remote = ((r.j["v"] as? JsonPrimitive)?.longOrNull ?: 0) to (r.j["d"] as? JsonPrimitive)?.content
                        continue
                    }
                    throw httpErr(r)
                }
                lastAt = System.currentTimeMillis()
                prefs.edit().putLong("at", lastAt).apply()
                error = ""
            } catch (e: CloudError) {
                error = e.message ?: "Falha ao sincronizar"
            } catch (e: Exception) {
                error = "Falha ao sincronizar"
            } finally {
                busy = false
            }
        }
    }

    /** Depois de cada mudança: envia alguns segundos depois da última. */
    fun soon() {
        if (!on) return
        pushJob?.cancel()
        pushJob = scope.launch { delay(5000); sync() }
    }

    /** App minimizado: envia já o que estava esperando. */
    fun onBackground() {
        if (on && pushJob?.isActive == true) {
            pushJob?.cancel()
            scope.launch { sync() }
        }
    }

    fun onForeground() {
        if (on && System.currentTimeMillis() - pulledAt > 20_000) scope.launch { sync(pull = true) }
    }

    /* ---------- Ações da tela ---------- */
    suspend fun ping(): String {
        if (url.isEmpty()) return "Digite o endereço do servidor"
        return try {
            val r = api("GET", "/v1/ping")
            if (r.status == 200 && (r.j["app"] as? JsonPrimitive)?.content == "ficha") "Conectado ao servidor ✓"
            else (r.j["message"] as? JsonPrimitive)?.content ?: "O endereço respondeu, mas não é um servidor do Ficha (${r.status})"
        } catch (e: CloudError) {
            e.message ?: "Sem conexão com o servidor"
        }
    }

    /** Começa a sincronizar com um código novo (devolve o código, ou null com o erro em [error]). */
    suspend fun create(): String? {
        val c = CloudCrypto.newCode()
        prefs.edit().putString("code", c).remove("v").putLong("at", 0).apply(); rev++
        forgetBase()
        error = ""
        sync(pull = true)
        if (error.isNotEmpty()) {
            prefs.edit().remove("code").remove("v").apply(); rev++
            return null
        }
        return c
    }

    /** Entra com um código existente: confere se abre os dados antes de juntar. */
    suspend fun join(raw: String): String? {
        val c = CloudCrypto.cleanCode(raw)
        if (c.length != 20) return "O código tem 20 letras e números"
        try {
            val k = withContext(Dispatchers.Default) { CloudCrypto.keys(c) }
            val r = api("GET", "/v1/data/${k.id}", null, k.auth)
            if (r.status == 404) return "Nenhum dado com este código. Confira as letras."
            if (r.status != 200) throw httpErr(r)
            CloudCrypto.unseal((r.j["d"] as JsonPrimitive).content, k)
        } catch (e: CloudError) {
            return e.message
        }
        prefs.edit().putString("code", c).remove("v").putLong("at", 0).apply(); rev++
        forgetBase()
        error = ""
        sync(pull = true)
        return error.ifEmpty { null }
    }

    fun off() {
        prefs.edit().remove("code").remove("v").apply(); rev++
        forgetBase()
    }

    suspend fun wipeRemote(): String? {
        val c = code ?: return null
        return try {
            val k = CloudCrypto.keys(c)
            val r = api("DELETE", "/v1/data/${k.id}", null, k.auth)
            if (r.status != 200) throw httpErr(r)
            off()
            null
        } catch (e: CloudError) {
            e.message
        }
    }
}
