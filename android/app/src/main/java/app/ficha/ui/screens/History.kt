package app.ficha.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Session
import app.ficha.logic.DAY
import app.ficha.logic.MONTH
import app.ficha.logic.VOL_GROUPS
import app.ficha.logic.VOL_MAIN
import app.ficha.logic.VOL_RANGE
import app.ficha.logic.VOL_SCALE
import app.ficha.logic.addDays
import app.ficha.logic.dateLong
import app.ficha.logic.dateOf
import app.ficha.logic.dateShort
import app.ficha.logic.exName
import app.ficha.logic.fmt
import app.ficha.logic.fmtDur
import app.ficha.logic.fmtInt
import app.ficha.logic.groupSets
import app.ficha.logic.jsDay
import app.ficha.logic.repeatSession
import app.ficha.logic.sessionsInRange
import app.ficha.logic.setCount
import app.ficha.logic.ssInfo
import app.ficha.logic.startOfDay
import app.ficha.logic.startOfWeek
import app.ficha.logic.timeHM
import app.ficha.logic.volume
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.KeyValue
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.ShapeIcon
import app.ficha.ui.components.StatRow
import app.ficha.ui.theme.Fx
import kotlin.random.Random

@Composable
fun HistoryScreen(data: AppData) {
    val app = LocalApp.current
    var limit by rememberSaveable { mutableIntStateOf(40) }
    Screen(title = "Histórico") {
        if (data.sessions.isEmpty()) {
            item { EmptyState(Icons.AutoMirrored.Rounded.ShowChart, "Nenhum treino registrado", "Seus treinos finalizados aparecem aqui, com duração, volume e recordes.") }
            return@Screen
        }
        item { CardBox { Heatmap(data) } }
        item {
            val wk = startOfWeek(System.currentTimeMillis())
            val weekVol = data.sessionsInRange(wk, addDays(wk, 7)).sumOf { it.volume() }
            val recent = data.sessions.take(20)
            val avg = recent.sumOf { it.end - it.start } / recent.size
            Spacer(Modifier.height(12.dp))
            StatRow(listOf(
                data.sessions.size.toString() to "treinos no total",
                (if (weekVol >= 10000) fmt(weekVol / 1000) + "t" else fmtInt(weekVol)) to "kg nesta semana",
                fmtDur(avg) to "duração média",
            ))
        }
        item { AchHistoryCard(data) }
        item { SectionHeader("Séries por músculo") }
        item { MuscleVolume(data) }
        var month = ""
        val shown = data.sessions.take(limit)
        // Agrupa por mês, cada mês uma lista segmentada
        val byMonth = shown.groupBy { s -> dateOf(s.start).let { "${MONTH[it.monthValue - 1]} ${it.year}" } }
        byMonth.forEach { (m, list) ->
            if (m != month) {
                month = m
                item(key = "m-$m") { SectionHeader(m.replaceFirstChar { it.uppercase() }) }
            }
            item(key = "l-$m") {
                SegmentedList(list.map { s -> sessionSeg(data, s) { app.go(Route.Session(s.id)) } })
            }
        }
        val more = data.sessions.size - limit
        if (more > 0) item {
            FilledTonalButton(onClick = { limit += 60 }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(Gutter)) {
                Text("Mostrar treinos mais antigos ($more)")
            }
        }
    }
}

private fun sessionSeg(data: AppData, s: Session, onClick: () -> Unit): Seg {
    val d = dateOf(s.start)
    val vol = s.volume()
    return Seg(
        key = s.id, headline = s.name,
        supporting = "${DAY[d.jsDay()]} ${timeHM(s.start)} · ${fmtDur(s.end - s.start)} · ${s.setCount()} séries" + if (vol > 0) " · ${fmtInt(vol)} kg" else "",
        leading = { ShapeIcon(text = d.dayOfMonth.toString(), shape = MaterialShapes.Cookie6Sided.toShape()) },
        trailing = if (s.prs.isNotEmpty()) ({ Pill("${s.prs.size} PR", Fx.colors.goldContainer, Fx.colors.onGoldContainer) }) else null,
        onClick = onClick,
    )
}

/** Mapa de frequência: as últimas 17 semanas, uma coluna por semana (seg → dom). */
@Composable
private fun Heatmap(data: AppData) {
    val weeks = 17
    val today = startOfDay(System.currentTimeMillis())
    val first = addDays(startOfWeek(today), -(weeks - 1) * 7)
    val counts = remember(data.sessions) { data.sessions.groupingBy { startOfDay(it.start) }.eachCount() }
    val cs = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (weeks - 1)) / weeks
        for (i in 0 until weeks * 7) {
            val t = addDays(first, i)
            val c = counts[t] ?: 0
            val col = i / 7
            val row = i % 7
            val tl = Offset(col * (cell + gap), row * (cell + gap))
            val color = when {
                t > today -> cs.surfaceContainerHighest.copy(alpha = .4f)
                c > 0 -> cs.primary
                else -> cs.surfaceContainerHighest
            }
            drawRoundRect(color, tl, Size(cell, cell), CornerRadius(cell * .28f))
            if (t == today) drawRoundRect(cs.onSurface, tl, Size(cell, cell), CornerRadius(cell * .28f), style = Stroke(1.5.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(dateShort(first), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("Seg → Dom por coluna", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("hoje", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

/** Séries por músculo na semana, comparadas com a faixa de 10 a 20 séries dos estudos de hipertrofia. */
@Composable
private fun MuscleVolume(data: AppData) {
    var range by rememberSaveable { mutableStateOf("week") }
    val wk = startOfWeek(System.currentTimeMillis())
    val vals = remember(data.sessions, range) {
        when (range) {
            "last" -> data.groupSets(addDays(wk, -7), wk)
            "avg" -> data.groupSets(addDays(wk, -28), wk).map { it / 4 }.toDoubleArray()
            else -> data.groupSets(wk, addDays(wk, 7))
        }
    }
    val (lo, hi) = VOL_RANGE
    Column(Modifier.padding(horizontal = Gutter)) {
        ConnectedChoice(listOf("week" to "Esta semana", "last" to "Anterior", "avg" to "Média 4 sem."), range) { range = it }
        Spacer(Modifier.height(10.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(16.dp)) {
                val cs = MaterialTheme.colorScheme
                VOL_GROUPS.forEachIndexed { i, (name, _) ->
                    val v = vals[i]
                    if (i >= VOL_MAIN && v <= 0) return@forEachIndexed
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp), maxLines = 1)
                        Canvas(Modifier.weight(1f).height(14.dp)) {
                            val w = size.width
                            val h = size.height
                            fun px(x: Double) = (minOf(x, VOL_SCALE) / VOL_SCALE * w).toFloat()
                            drawRoundRect(cs.surfaceContainerHighest, Offset.Zero, Size(w, h), CornerRadius(h / 2))
                            drawRect(cs.primary.copy(alpha = .14f), Offset(px(lo.toDouble()), 0f), Size(px(hi.toDouble()) - px(lo.toDouble()), h))
                            if (v > 0) {
                                val color = when {
                                    v < lo -> cs.tertiary
                                    v <= hi -> cs.primary
                                    else -> cs.error
                                }
                                drawRoundRect(color, Offset.Zero, Size(maxOf(h, px(v)), h), CornerRadius(h / 2))
                            }
                        }
                        Text(fmt(v, 1), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend(cs.primary.copy(alpha = .3f), "faixa: $lo–$hi séries")
                    Legend(cs.tertiary, "abaixo")
                    Legend(cs.error, "acima")
                }
                Text(
                    "Para hipertrofia, os estudos indicam $lo a $hi séries por músculo por semana. Séries de aquecimento não contam e músculos secundários contam meia série" +
                        if (range == "week") ". A semana atual ainda está em andamento." else ".",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(3.dp), modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SessionScreen(data: AppData, id: String) {
    val app = LocalApp.current
    val s = data.sessions.find { it.id == id }
    if (s == null) {
        LaunchedEffect(Unit) { app.leave(Route.Session(id)) }
        return
    }
    val vol = s.volume()
    var menu by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    val celebrate = remember { app.summaryFor == id }
    LaunchedEffect(Unit) { if (app.summaryFor == id) app.summaryFor = null }
    val prKeys = s.prs.map { it.exId }.toSet()
    Box {
        Screen(
            title = s.name, subtitle = "${dateLong(s.start)} · ${timeHM(s.start)}", back = true, large = false,
            actions = {
                IconButton(onClick = { share = true }) { Icon(Icons.Rounded.Share, "Compartilhar") }
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opções") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Renomear") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = {
                        menu = false
                        app.prompt("Nome do treino", s.name) { n -> if (n.isNotBlank()) app.update { d -> d.copy(sessions = d.sessions.map { if (it.id == id) it.copy(name = n.trim()) else it }) } }
                    })
                    DropdownMenuItem(text = { Text("Excluir do histórico", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = {
                        menu = false
                        app.confirm("Excluir treino?", "Ele sai do histórico e dos gráficos de evolução.", "Excluir", danger = true) {
                            app.back()
                            app.update { d -> d.copy(sessions = d.sessions.filter { it.id != id }) }
                        }
                    })
                }
            },
        ) {
            if (celebrate) item { Celebration(s) }
            item {
                if (!celebrate) StatRow(listOf(fmtDur(s.end - s.start) to "duração", s.setCount().toString() to "séries", (if (vol > 0) fmtInt(vol) else "—") to "kg de volume"))
                if (celebrate) FilledTonalButton(onClick = { share = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp)) {
                    Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartilhar nos Stories")
                }
            }
            val ach = s.ach.orEmpty()
            if (ach.isNotEmpty()) {
                item { SectionHeader(if (celebrate) (if (ach.size > 1) "Conquistas desbloqueadas" else "Conquista desbloqueada") else "Conquistas deste treino") }
                item { AchBadgesRow(data, ach) }
            }
            if (s.prs.isNotEmpty()) {
                item { SectionHeader("Recordes pessoais") }
                item {
                    CardBox(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        s.prs.forEach { p -> KeyValue("${p.name} · ${p.label}", "${fmt(p.value)} ${p.unit}", Fx.colors.gold) }
                    }
                }
            }
            item { SectionHeader("Exercícios") }
            val ss = s.exercises.map { it.ss }
            s.exercises.forEachIndexed { i, e ->
                item(key = "e-$i") {
                    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 2.dp)) {
                        Surface(
                            onClick = { app.go(Route.Exercise(e.exId)) }, shape = segmentShape(i, s.exercises.size),
                            color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ssInfo(ss, i)?.let { Pill(it.badge, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) }
                                    Text(data.exName(e.exId, e.name), style = MaterialTheme.typography.titleSmallEmphasized, modifier = Modifier.weight(1f, fill = false))
                                    if (e.exId in prKeys) Pill("PR", Fx.colors.goldContainer, Fx.colors.onGoldContainer)
                                }
                                Spacer(Modifier.height(8.dp))
                                SetPills(e.kind, e.sets)
                            }
                        }
                    }
                }
            }
            if (s.notes.isNotBlank()) {
                item { SectionHeader("Anotações") }
                item { CardBox { Text(s.notes, style = MaterialTheme.typography.bodyLarge) } }
            }
            item {
                Button(
                    onClick = { app.startWorkout { it.repeatSession(s) } }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().padding(Gutter).height(ButtonDefaults.MediumContainerHeight),
                ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Repetir este treino", style = MaterialTheme.typography.titleMedium) }
            }
        }
        if (celebrate) Confetti(Modifier.fillMaxSize(), if (s.prs.isNotEmpty() || !s.ach.isNullOrEmpty()) 140 else 80)
    }
    if (share) ShareSheet(data, "session", id) { share = false }
}

/** Resumo no fim do treino: troféu, números que sobem até o valor e recordes. */
@Composable
private fun Celebration(s: Session) {
    val vol = s.volume()
    val k = remember { Animatable(0f) }
    LaunchedEffect(Unit) { k.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ShapeIcon(
            Icons.Rounded.EmojiEvents, shape = MaterialShapes.VerySunny.toShape(), size = 88.dp,
            container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text("Treino concluído!", style = MaterialTheme.typography.headlineMediumEmphasized, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
    }
    StatRow(listOf(
        fmtDur(s.end - s.start) to "duração",
        (s.setCount() * k.value).toInt().toString() to "séries",
        (if (vol > 0) fmtInt(vol * k.value) else "—") to "kg de volume",
    ))
}

/** Confete caindo (comemoração no fim do treino). */
@Composable
private fun Confetti(modifier: Modifier, n: Int) {
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, Fx.colors.gold, cs.tertiary, Color(0xFF4DA3FF), Color(0xFFFF6FAE), Color(0xFF34D399), Color.White)
    val parts = remember {
        List(n) {
            floatArrayOf(
                (Random.nextFloat() - .5f) * 60f, // x0 (dp)
                (Random.nextFloat() - .5f) * 1.1f, // dx (fração da largura)
                120f + Random.nextFloat() * 220f, // subida (dp)
                (Random.nextFloat() - .5f) * 900f, // rotação
                6f + Random.nextFloat() * 5f, 8f + Random.nextFloat() * 8f, // tamanho
                Random.nextFloat() * .12f, // atraso
                .75f + Random.nextFloat() * .45f, // duração relativa
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3000, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val y0 = h * .32f
        parts.forEachIndexed { i, p ->
            val k = ((t.value - p[6]) / p[7]).coerceIn(0f, 1f)
            if (k <= 0f || k >= 1f) return@forEachIndexed
            // sobe rápido e cai (parábola), abrindo para os lados
            val x = w / 2 + p[0].dp.toPx() + p[1] * w * k
            val up = p[2].dp.toPx()
            val y = y0 - up * 4 * k * (1 - k) * 1.2f + (h - y0 + 60) * k * k
            rotate(p[3] * k, Offset(x, y)) {
                drawRect(colors[i % colors.size].copy(alpha = if (k > .85f) (1 - k) / .15f else 1f), Offset(x, y), Size(p[4].dp.toPx(), p[5].dp.toPx()))
            }
        }
    }
}
