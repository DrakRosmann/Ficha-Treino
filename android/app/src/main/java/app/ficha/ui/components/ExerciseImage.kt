package app.ficha.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.ficha.data.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Fotos dos exercícios embutidas no app (assets/img), com cache em memória. */
object AssetImages {
    private val cache = object : LruCache<String, ImageBitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cached(path: String): ImageBitmap? = cache.get(path)

    suspend fun load(context: Context, path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(path) ?: runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()?.also { cache.put(path, it) }
    }
}

@Composable
fun rememberAssetImage(path: String?): ImageBitmap? {
    val context = LocalContext.current
    var img by remember(path) { mutableStateOf(path?.let { AssetImages.cached(it) }) }
    LaunchedEffect(path) {
        if (img == null && path != null) img = AssetImages.load(context, path)
    }
    return img
}

/** Desenha um recorte da imagem preenchendo a área (como object-fit: cover). */
private fun DrawScope.drawCover(img: ImageBitmap, srcX: Int, srcW: Int, alpha: Float = 1f) {
    val srcH = img.height
    val target = size.width / size.height
    val src = srcW.toFloat() / srcH
    val (cw, ch) = if (src > target) (srcH * target).roundToInt() to srcH else srcW to (srcW / target).roundToInt()
    val ox = srcX + (srcW - cw) / 2
    val oy = (srcH - ch) / 2
    drawImage(
        img, srcOffset = IntOffset(ox, oy), srcSize = IntSize(cw, ch),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = alpha,
    )
}

@Composable
private fun Placeholder(size: Dp?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f), modifier = Modifier.size((size ?: 40.dp) * 0.45f))
    }
}

/** Miniatura das listas (posição inicial). */
@Composable
fun ExerciseThumb(ex: Exercise?, size: Dp = 56.dp, shape: Shape = RoundedCornerShape(14.dp)) {
    val img = rememberAssetImage(ex?.img?.let { "img/thumb/$it.webp" })
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = shape, modifier = Modifier.size(size)) {
        if (img == null) Placeholder(size)
        else Canvas(Modifier.fillMaxSize()) { drawCover(img, 0, img.width) }
    }
}

/**
 * Foto da execução alternando a posição inicial e a final (as duas ficam lado a lado no arquivo).
 * [phase] desencontra a animação de fotos vizinhas.
 */
@Composable
fun ExercisePic(ex: Exercise?, modifier: Modifier = Modifier, phase: Int = 0, shape: Shape = RoundedCornerShape(20.dp)) {
    val img = rememberAssetImage(ex?.img?.let { "img/ex/$it.webp" })
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = shape, modifier = modifier) {
        if (img == null) Placeholder(null)
        else {
            val second by alternate(phase)
            Canvas(Modifier.fillMaxSize()) {
                val half = img.width / 2
                drawCover(img, 0, half)
                if (second > 0f) drawCover(img, half, half, second)
            }
        }
    }
}

/** 0 → 1 → 0: mostra a primeira foto, troca suavemente para a segunda e volta. */
@Composable
private fun alternate(phase: Int) = rememberInfiniteTransition("alterna").animateFloat(
    initialValue = 0f, targetValue = 0f,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 3200
            0f at 0 using LinearEasing
            0f at 1200 using LinearEasing
            1f at 1600 using LinearEasing
            1f at 2800 using LinearEasing
            0f at 3200
        },
        repeatMode = RepeatMode.Restart,
        initialStartOffset = StartOffset((phase % 4) * 800),
    ),
    label = "foto",
)

/** Foto grande da execução, com as etiquetas "1 · Início" e "2 · Fim". */
@Composable
fun ExerciseDemo(ex: Exercise?, modifier: Modifier = Modifier) {
    val img = rememberAssetImage(ex?.img?.let { "img/ex/$it.webp" })
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth().aspectRatio(1.5f),
    ) {
        if (ex?.img == null) {
            Box(contentAlignment = Alignment.Center) {
                Text("Sem foto para este exercício.\nVeja um vídeo abaixo.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (img != null) {
            val second by alternate(0)
            Box {
                Canvas(Modifier.fillMaxSize()) {
                    val half = img.width / 2
                    drawCover(img, 0, half)
                    if (second > 0f) drawCover(img, half, half, second)
                }
                val label = if (second < .5f) "1 · Início" else "2 · Fim"
                Text(
                    label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        .clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = .92f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}
