package app.ficha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ficha.data.Catalog
import app.ficha.data.Exercise
import app.ficha.logic.musclesOf

/** Nível de destaque de cada região do desenho. */
enum class MuscleLevel { Pri, Sec, L1, L2, L3 }

@Composable
fun BodyMapFigures(levels: Map<String, MuscleLevel>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val base = cs.onSurface.copy(alpha = .08f)
    val idle = cs.onSurface.copy(alpha = .14f)
    fun mix(k: Float) = cs.primary.copy(alpha = k).compositeOver(cs.surfaceContainerHighest)
    val colors = mapOf(
        MuscleLevel.Pri to cs.primary, MuscleLevel.Sec to mix(.42f),
        MuscleLevel.L1 to mix(.32f), MuscleLevel.L2 to mix(.65f), MuscleLevel.L3 to cs.primary,
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Catalog.body.front to "Frente", Catalog.body.back to "Costas").forEach { (fig, label) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val paths = remember(fig) { fig.map { (name, polys) -> name to polys.map(::polygon) } }
                // O desenho de costas desce até y≈220 (as panturrilhas passam do viewBox 100×200)
                Box(Modifier.height(228.dp).aspectRatio(100f / 222f)) {
                    Canvas(Modifier.matchParentSize()) {
                        scale(size.width / 100f, size.height / 222f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                            for ((name, ps) in paths) {
                                val c = if (name == "head" || name == "knees") base else levels[name]?.let(colors::get) ?: idle
                                ps.forEach { drawPath(it, c) }
                            }
                        }
                    }
                }
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
            }
        }
    }
}

private fun polygon(pts: FloatArray) = Path().apply {
    moveTo(pts[0], pts[1])
    var i = 2
    while (i + 1 < pts.size) {
        lineTo(pts[i], pts[i + 1])
        i += 2
    }
    close()
}

/** Mapa com os músculos de um exercício: principais em destaque, secundários mais claros. */
@Composable
fun ExerciseMuscles(ex: Exercise?, modifier: Modifier = Modifier) {
    val (p, s) = musclesOf(ex)
    if (p.isEmpty()) return
    val levels = HashMap<String, MuscleLevel>()
    for (c in s) for (r in Catalog.body.regions[c].orEmpty()) levels[r] = MuscleLevel.Sec
    for (c in p) for (r in Catalog.body.regions[c].orEmpty()) levels[r] = MuscleLevel.Pri
    val names = { list: List<String> -> list.joinToString(", ") { Catalog.muscles[it] ?: it } }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            BodyMapFigures(levels)
            Spacer(Modifier.height(12.dp))
            LegendKey(MaterialTheme.colorScheme.primary, "Principais", names(p))
            if (s.isNotEmpty()) LegendKey(MaterialTheme.colorScheme.primary.copy(alpha = .42f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHighest), "Secundários", names(s))
        }
    }
}

@Composable
fun LegendKey(color: Color, title: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 4.dp).size(12.dp).let { it }) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Mapa de calor: séries por músculo nos últimos dias. */
@Composable
fun MuscleHeat(load: Map<String, Double>, modifier: Modifier = Modifier) {
    val best = HashMap<String, Double>()
    for ((c, v) in load) for (r in Catalog.body.regions[c].orEmpty()) best[r] = maxOf(best[r] ?: 0.0, v)
    val levels = best.mapNotNull { (r, v) ->
        when {
            v >= 10 -> r to MuscleLevel.L3
            v >= 5 -> r to MuscleLevel.L2
            v > 0 -> r to MuscleLevel.L1
            else -> null
        }
    }.toMap()
    val color = animateColorAsState(MaterialTheme.colorScheme.primary, label = "cor").value
    Column(modifier) {
        BodyMapFigures(levels)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            listOf(.32f to "1–4", .65f to "5–9", 1f to "10+ séries").forEach { (k, t) ->
                Surface(color = color.copy(alpha = k).compositeOver(MaterialTheme.colorScheme.surfaceContainerHighest), shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                Spacer(Modifier.width(6.dp))
                Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}
