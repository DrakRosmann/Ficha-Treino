package app.ficha.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import app.ficha.data.ProgressPhoto
import app.ficha.data.uid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fotos do progresso: ficam só neste aparelho (pasta privada do app), como foto grande (até 1440 px)
 * e miniatura (360 px). Nada vai para a internet nem para a IA; o backup só as inclui se você escolher.
 */
object PhotoStore {
    val POSES = listOf("frente" to "Frente", "lado" to "Lado", "costas" to "Costas")
    fun poseName(p: String) = POSES.find { it.first == p }?.second ?: p

    private lateinit var dir: File
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun init(context: Context) {
        dir = File(context.filesDir, "progresso").apply { mkdirs() }
    }

    private fun file(id: String, thumb: Boolean) = File(dir, if (thumb) "${id}_t.jpg" else "$id.jpg")

    fun cached(id: String, thumb: Boolean): Bitmap? = cache.get("$id:$thumb")

    suspend fun load(id: String, thumb: Boolean): Bitmap? = withContext(Dispatchers.IO) {
        cache.get("$id:$thumb") ?: file(id, thumb).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }?.also { cache.put("$id:$thumb", it) }
    }

    private fun scaled(src: Bitmap, maxSide: Int): Bitmap {
        val s = minOf(1f, maxSide.toFloat() / max(src.width, src.height))
        return if (s >= 1f) src else Bitmap.createScaledBitmap(src, (src.width * s).roundToInt(), (src.height * s).roundToInt(), true)
    }

    private fun jpeg(b: Bitmap, q: Int) = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, q, it) }.toByteArray()

    /** Guarda a foto (já virada certo) e devolve o registro dela. */
    suspend fun save(bitmap: Bitmap, t: Long, pose: String): ProgressPhoto = withContext(Dispatchers.IO) {
        val id = "pp" + uid()
        val full = jpeg(scaled(bitmap, 1440), 85)
        val thumb = jpeg(scaled(bitmap, 360), 78)
        file(id, false).writeBytes(full)
        file(id, true).writeBytes(thumb)
        ProgressPhoto(id, t, pose, (full.size + thumb.size) / 1024)
    }

    /** Abre uma foto da galeria respeitando a rotação gravada pela câmera (EXIF). */
    suspend fun decode(context: Context, uri: Uri, maxSide: Int = 2400): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: return@runCatching null
            val rot = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            if (rot == 0) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
        }.getOrNull()
    }

    fun delete(id: String) {
        file(id, false).delete()
        file(id, true).delete()
        cache.remove("$id:true")
        cache.remove("$id:false")
    }

    fun wipe() {
        dir.listFiles()?.forEach { it.delete() }
        cache.evictAll()
    }

    /** { id: [foto, miniatura] } em base64, para o arquivo de backup (mesmo formato do PWA). */
    suspend fun exportData(ids: Collection<String>): Map<String, Pair<String, String>> = withContext(Dispatchers.IO) {
        val enc = Base64.getEncoder()
        ids.mapNotNull { id ->
            val f = file(id, false)
            val t = file(id, true)
            if (!f.exists()) null else id to (enc.encodeToString(f.readBytes()) to enc.encodeToString(if (t.exists()) t.readBytes() else f.readBytes()))
        }.toMap()
    }

    suspend fun importData(data: Map<String, Pair<String, String>>) = withContext(Dispatchers.IO) {
        val dec = Base64.getDecoder()
        for ((id, p) in data) {
            runCatching {
                file(id, false).writeBytes(dec.decode(p.first))
                file(id, true).writeBytes(dec.decode(p.second))
                cache.remove("$id:true")
                cache.remove("$id:false")
            }
        }
    }

    fun totalKb(photos: List<ProgressPhoto>) = photos.sumOf { it.kb }
}
