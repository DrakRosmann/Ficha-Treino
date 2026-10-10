package app.ficha.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ficha.logic.dateShort
import app.ficha.logic.fmt
import kotlin.math.abs
import kotlin.math.max

class ChartPoint(val t: Long, val v: Double)

/**
 * Gráfico de linha com área, valor atual e variação desde o primeiro ponto.
 * [better]: "up" (subir é bom), "down" (descer é bom) ou null (neutro).
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    unit: String,
    modifier: Modifier = Modifier,
    goal: Double? = null,
    better: String? = "up",
    dec: Int = 1,
    maxPoints: Int = 24,
    byTime: Boolean = false,
    empty: String = "Faça este exercício em pelo menos 2 treinos para ver o gráfico.",
) {
    if (points.size < 2) {
        Text(empty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(8.dp))
        return
    }
    val pts = points.takeLast(maxPoints)
    val cs = MaterialTheme.colorScheme
    val lastV = pts.last().v
    val diff = lastV - pts.first().v
    val good = when (better) { "up" -> diff >= 0; "down" -> diff <= 0; else -> false }
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt(lastV, dec), style = MaterialTheme.typography.headlineMediumEmphasized.copy(fontFeatureSettings = "tnum"))
            Spacer(Modifier.width(6.dp))
            Text(unit, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Pill(
                "${if (diff >= 0) "+" else "−"}${fmt(abs(diff), dec)} $unit",
                container = if (good) cs.primaryContainer else cs.surfaceContainerHighest,
                content = if (good) cs.onPrimaryContainer else cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        val measurer = rememberTextMeasurer()
        val labelStyle = TextStyle(fontSize = 11.sp, color = cs.onSurfaceVariant, fontFeatureSettings = "tnum")
        val progress = remember(pts) { Animatable(0f) }
        LaunchedEffect(pts) { progress.animateTo(1f, tween(900)) }
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val pl = 44.dp.toPx()
            val pr = 10.dp.toPx()
            val pt = 12.dp.toPx()
            val pb = 22.dp.toPx()
            val w = size.width
            val h = size.height
            val vs = pts.map { it.v } + listOfNotNull(goal)
            var lo = vs.min()
            var hi = vs.max()
            if (lo == hi) { lo -= 1; hi += 1 }
            val pad = (hi - lo) * 0.12
            lo = max(0.0, lo - pad); hi += pad
            val t0 = pts.first().t
            val t1 = pts.last().t
            fun x(i: Int): Float = if (byTime && t1 > t0) pl + ((pts[i].t - t0).toFloat() / (t1 - t0)) * (w - pl - pr)
            else pl + i * (w - pl - pr) / (pts.size - 1)
            fun y(v: Double): Float = (pt + (1 - (v - lo) / (hi - lo)) * (h - pt - pb)).toFloat()
            // Grade e rótulos
            listOf(lo, (lo + hi) / 2, hi).forEach { v ->
                drawLine(cs.outlineVariant.copy(alpha = .6f), Offset(pl, y(v)), Offset(w - pr, y(v)), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                val txt = measurer.measure(fmt(v, if (hi - lo < 10) max(1, dec) else 0), labelStyle)
                drawText(txt, topLeft = Offset(pl - 6.dp.toPx() - txt.size.width, y(v) - txt.size.height / 2f))
            }
            if (goal != null) {
                drawLine(cs.tertiary, Offset(pl, y(goal)), Offset(w - pr, y(goal)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                val txt = measurer.measure("meta ${fmt(goal, dec)}", labelStyle.copy(color = cs.tertiary))
                drawText(txt, topLeft = Offset(w - pr - txt.size.width, y(goal) - txt.size.height - 2.dp.toPx()))
            }
            val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.v)) else lineTo(x(i), y(p.v)) } }
            val k = progress.value
            // Área sob a linha
            val area = Path().apply {
                addPath(line)
                lineTo(x(pts.size - 1), h - pb)
                lineTo(pl, h - pb)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(cs.primary.copy(alpha = .28f * k), cs.primary.copy(alpha = 0f)), startY = pt, endY = h - pb))
            // A linha se desenha da esquerda para a direita
            val measure = PathMeasure().apply { setPath(line, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * k, partial, true)
            drawPath(partial, cs.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (pts.size <= 40) pts.forEachIndexed { i, p ->
                if (i < pts.size - 1) drawCircle(cs.primary, 3.dp.toPx() * k, Offset(x(i), y(p.v)))
            }
            drawCircle(cs.surfaceContainer, 7.dp.toPx() * k, Offset(x(pts.size - 1), y(lastV)))
            drawCircle(cs.primary, 5.dp.toPx() * k, Offset(x(pts.size - 1), y(lastV)))
            val first = measurer.measure(dateShort(t0), labelStyle)
            drawText(first, topLeft = Offset(pl, h - first.size.height.toFloat()))
            val lastT = measurer.measure(dateShort(t1), labelStyle)
            drawText(lastT, topLeft = Offset(w - pr - lastT.size.width, h - lastT.size.height.toFloat()))
        }
    }
}
