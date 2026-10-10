package app.ficha.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.ficha.R
import app.ficha.data.AppData
import app.ficha.data.Session
import app.ficha.data.SessionExercise
import app.ficha.logic.ACH_BY_ID
import app.ficha.logic.Ach
import app.ficha.logic.TIER
import app.ficha.logic.TIER_NAME
import app.ficha.logic.dateLong
import app.ficha.logic.exName
import app.ficha.logic.fmt
import app.ficha.logic.fmtDur
import app.ficha.logic.fmtInt
import app.ficha.logic.setCount
import app.ficha.logic.timeHM
import app.ficha.logic.volume
import app.ficha.ui.theme.accentOf
import java.io.File

/** Imagem do treino ou da conquista no formato dos Stories (port do share.js). mode: card | photo | sticker */
object ShareImage {
    const val W = 1080
    const val H = 1920

    private var base: Typeface? = null
    private fun font(context: Context, weight: Int): Typeface {
        val b = base ?: (ResourcesCompat.getFont(context, R.font.google_sans_flex) ?: Typeface.DEFAULT).also { base = it }
        return if (Build.VERSION.SDK_INT >= 28) Typeface.create(b, weight, false) else Typeface.create(b, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Cor de destaque viva (a do tema escuro do PWA). */
    private fun accent(d: AppData): Int = if (d.settings.accent == "mono") 0xFFF2F3EF.toInt() else accentOf(d.settings.accent).seed.toInt()

    private fun paint(context: Context, weight: Int, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = font(context, weight)
        textSize = size
        this.color = color
        textAlign = align
    }

    private fun shadow(p: Paint, on: Boolean) {
        if (on) p.setShadowLayer(18f, 0f, 2f, 0x8C000000.toInt()) else p.clearShadowLayer()
    }

    private fun wrap(p: Paint, text: String, maxW: Float, maxLines: Int): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        for (w in text.split(Regex("\\s+"))) {
            val t = if (line.isEmpty()) w else "$line $w"
            if (p.measureText(t) > maxW && line.isNotEmpty()) { lines += line; line = w } else line = t
        }
        if (line.isNotEmpty()) lines += line
        if (lines.size > maxLines) {
            val keep = lines.take(maxLines).toMutableList()
            var last = keep[maxLines - 1]
            while (p.measureText("$last…") > maxW && last.isNotEmpty()) last = last.dropLast(1)
            keep[maxLines - 1] = "$last…"
            return keep
        }
        return lines
    }

    private fun fit(p: Paint, text: String, maxW: Float): String {
        if (p.measureText(text) <= maxW) return text
        var t = text
        while (t.isNotEmpty() && p.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }

    private fun withAlpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a shl 24)

    private fun background(c: Canvas, mode: String, photo: Bitmap?, acc: Int) {
        if (mode == "sticker") { c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); return }
        if (mode == "photo" && photo != null) {
            val s = maxOf(W.toFloat() / photo.width, H.toFloat() / photo.height)
            val w = photo.width * s
            val h = photo.height * s
            c.drawBitmap(photo, null, RectF((W - w) / 2, (H - h) / 2, (W + w) / 2, (H + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, H.toFloat(), intArrayOf(0x73000000, 0x0D000000, 0x59000000, 0xE0000000.toInt()), floatArrayOf(0f, .3f, .5f, 1f), Shader.TileMode.CLAMP)
            })
            return
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF15161B.toInt(), 0xFF07080A.toInt(), Shader.TileMode.CLAMP) })
        fun glow(x: Float, y: Float, r: Float, a: Int) = c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
            shader = RadialGradient(x, y, r, withAlpha(acc, a), withAlpha(acc, 0), Shader.TileMode.CLAMP)
        })
        glow(W * .95f, 80f, 900f, 0x55)
        glow(-80f, H * .9f, 800f, 0x22)
    }

    private fun footer(context: Context, c: Canvas, y: Float, mode: String) {
        c.drawText("Registrado no Ficha", W / 2f, y, paint(context, 600, 30f, if (mode == "card") 0x73FFFFFF else 0xCCFFFFFF.toInt(), Paint.Align.CENTER))
    }

    private fun badge(context: Context, c: Canvas, a: Ach, cx: Float, cy: Float, r: Float) {
        val (c1, c2) = TIER[a.t]!!
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, c1.toInt(), c2.toInt(), Shader.TileMode.CLAMP) })
        c.drawCircle(cx, cy, r * .8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2E000000 })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = r * .95f; textAlign = Paint.Align.CENTER }
        val fm = p.fontMetrics
        c.drawText(a.e, cx, cy - (fm.ascent + fm.descent) / 2, p)
    }

    private fun bestSetText(e: SessionExercise): String {
        val w = e.sets.filter { !it.warm }
        if (w.isEmpty()) return ""
        return when (e.kind) {
            "w" -> {
                val b = w.reduce { x, y -> if ((y.a ?: 0.0) > (x.a ?: 0.0) || ((y.a ?: 0.0) == (x.a ?: 0.0) && (y.b ?: 0.0) > (x.b ?: 0.0))) y else x }
                if ((b.a ?: 0.0) > 0) "${fmt(b.a)} kg × ${fmt(b.b ?: 0.0, 0)}" else "${fmt(b.b ?: 0.0, 0)} reps"
            }
            "bw" -> {
                val ex = w.maxOf { it.a ?: 0.0 }
                "${fmt(w.maxOf { it.b ?: 0.0 }, 0)} reps${if (ex > 0) " · +${fmt(ex)} kg" else ""}"
            }
            "s" -> "${fmt(w.maxOf { it.a ?: 0.0 }, 0)} s"
            else -> {
                val km = w.sumOf { it.b ?: 0.0 }
                "${fmt(w.sumOf { it.a ?: 0.0 })} min${if (km > 0) " · ${fmt(km)} km" else ""}"
            }
        }
    }

    fun session(context: Context, d: AppData, s: Session, mode: String, photo: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val acc = accent(d)
        val X = 96f
        val Wd = W - X * 2
        val sticker = mode == "sticker"
        val onPhoto = mode == "photo"
        val sh = sticker || onPhoto
        background(c, mode, photo, acc)
        val vol = s.volume()
        val prKeys = s.prs.map { it.exId }.toSet()
        val exs = s.exercises.filter { e -> e.sets.any { !it.warm } }
        val rows = exs.take(if (sticker) 4 else if (onPhoto) 5 else 8)
        val ach = (s.ach ?: emptyList()).mapNotNull { ACH_BY_ID[it] }
        val titleP = paint(context, 800, 100f, Color.WHITE)
        val titleLines = wrap(titleP, s.name, Wd, 2)
        val blockH = 34 + 160 + titleLines.size * 108 + 30 + 240 + rows.size * 96 + (if (exs.size > rows.size) 60 else 0) + (if (ach.isNotEmpty() && !sticker) 200 else 0)
        var y = 34f + if (onPhoto) H - 210f - blockH else maxOf(150f, (H - blockH) / 2f - if (sticker) 0f else 40f)

        paint(context, 800, 34f, acc).also { shadow(it, sh); c.drawText("FICHA · TREINO CONCLUÍDO", X, y, it) }
        paint(context, 500, 36f, 0xB8FFFFFF.toInt()).also { shadow(it, sh); c.drawText("${dateLong(s.start)} · ${timeHM(s.start)}", X, y + 54, it) }
        y += 160
        shadow(titleP, sh)
        titleLines.forEach { c.drawText(it, X, y, titleP); y += 108 }
        y += 30
        // Números
        val stats = listOf(
            fmtDur(s.end - s.start) to "duração",
            s.setCount().toString() to "séries",
            (if (vol > 0) (if (vol >= 10000) fmt(vol / 1000, 1) + " t" else fmtInt(vol)) else "—") to (if (vol > 0) (if (vol >= 10000) "de volume" else "kg de volume") else "volume"),
        )
        if (!sh) c.drawRoundRect(RectF(X - 8, y, X + Wd + 8, y + 190), 44f, 44f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x0FFFFFFF })
        stats.forEachIndexed { i, (v, l) ->
            val cx = X + Wd / 6 + i * Wd / 3
            paint(context, 800, 72f, if (i == 0) acc else Color.WHITE, Paint.Align.CENTER).also { shadow(it, sh); c.drawText(v, cx, y + 104, it) }
            paint(context, 600, 30f, 0x9EFFFFFF.toInt(), Paint.Align.CENTER).also { shadow(it, sh); c.drawText(l, cx, y + 150, it) }
        }
        y += 240
        // Exercícios
        rows.forEachIndexed { i, e ->
            val best = bestSetText(e)
            val pr = e.exId in prKeys
            val top = y + i * 96
            if (!sh && i > 0) c.drawRect(X, top - 20, X + Wd, top - 18, Paint().apply { color = 0x14FFFFFF })
            val bp = paint(context, 700, 40f, 0xBFFFFFFF.toInt(), Paint.Align.RIGHT).also { shadow(it, sh) }
            val bw = bp.measureText(best)
            c.drawText(best, X + Wd, top + 44, bp)
            val prW = if (pr) 92f else 0f
            val np = paint(context, 600, 42f, Color.WHITE).also { shadow(it, sh) }
            val nm = fit(np, d.exName(e.exId, e.name), Wd - bw - prW - 40)
            c.drawText(nm, X, top + 44, np)
            if (pr) {
                val nx = X + np.measureText(nm) + 18
                c.drawRoundRect(RectF(nx, top + 8, nx + 74, top + 54), 23f, 23f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD43B.toInt() })
                c.drawText("PR", nx + 17, top + 41, paint(context, 800, 28f, 0xFF231A00.toInt()))
            }
        }
        y += rows.size * 96
        if (exs.size > rows.size) {
            val n = exs.size - rows.size
            paint(context, 600, 34f, 0x99FFFFFF.toInt()).also { shadow(it, sh); c.drawText("+ $n exercício${if (n > 1) "s" else ""}", X, y + 30, it) }
            y += 60
        }
        // Conquistas deste treino
        if (ach.isNotEmpty() && !sticker) {
            y += 40
            paint(context, 700, 32f, 0xB3FFFFFF.toInt()).also { shadow(it, onPhoto); c.drawText(if (ach.size > 1) "CONQUISTAS DESBLOQUEADAS" else "CONQUISTA DESBLOQUEADA", X, y, it) }
            y += 30
            val cols = minOf(3, ach.size)
            ach.take(3).forEachIndexed { i, a ->
                val cx = X + 56 + i * (Wd / cols)
                val cy = y + 70
                badge(context, c, a, cx, cy, 56f)
                val p = paint(context, 700, 34f, Color.WHITE).also { shadow(it, onPhoto) }
                c.drawText(fit(p, a.n, Wd / cols - 140), cx + 76, cy + 12, p)
            }
        }
        footer(context, c, H - 90f, mode)
        return bmp
    }

    fun achievement(context: Context, d: AppData, a: Ach, mode: String, photo: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        background(c, mode, photo, accent(d))
        val sticker = mode == "sticker"
        val onPhoto = mode == "photo"
        val sh = sticker || onPhoto
        val cy = if (onPhoto) H - 760f else H / 2f - 240
        val (c1, _) = TIER[a.t]!!
        if (!sh) c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
            shader = RadialGradient(W / 2f, cy, 520f, withAlpha(c1.toInt(), 0x55), withAlpha(c1.toInt(), 0), Shader.TileMode.CLAMP)
        })
        badge(context, c, a, W / 2f, cy, 190f)
        paint(context, 800, 36f, c1.toInt(), Paint.Align.CENTER).also { shadow(it, sh); c.drawText("CONQUISTA · ${TIER_NAME[a.t].uppercase()}", W / 2f, cy + 300, it) }
        val np = paint(context, 800, 92f, Color.WHITE, Paint.Align.CENTER).also { shadow(it, sh) }
        val nameLines = wrap(np, a.n, W - 160f, 2)
        nameLines.forEachIndexed { i, l -> c.drawText(l, W / 2f, cy + 410 + i * 100, np) }
        val dp = paint(context, 500, 42f, 0xB8FFFFFF.toInt(), Paint.Align.CENTER).also { shadow(it, sh) }
        val lines = wrap(dp, a.d, W - 220f, 3)
        val base = cy + 410 + (nameLines.size - 1) * 100 + 90
        lines.forEachIndexed { i, l -> c.drawText(l, W / 2f, base + i * 56, dp) }
        val whenT = d.ach?.get(a.id) ?: 0
        if (whenT > 1) paint(context, 600, 34f, 0x80FFFFFF.toInt(), Paint.Align.CENTER).also { shadow(it, sh); c.drawText(dateLong(whenT), W / 2f, base + lines.size * 56 + 40, it) }
        footer(context, c, H - 90f, mode)
        return bmp
    }

    private fun png(bmp: Bitmap) = java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /** Abre o menu de compartilhar do Android (Instagram, WhatsApp…). */
    fun share(context: Context, bmp: Bitmap, name: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, name).apply { writeBytes(png(bmp)) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Compartilhar",
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Salva na galeria (Imagens/Ficha). */
    fun saveToGallery(context: Context, bmp: Bitmap, name: String): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ficha")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use { it.write(png(bmp)) }
        true
    }.getOrDefault(false)
}
