package app.ficha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.data.SetRecord
import app.ficha.logic.PT_BR
import app.ficha.logic.Suggestion
import app.ficha.logic.allEx
import app.ficha.logic.dateLong
import app.ficha.logic.ex
import app.ficha.logic.exHistory
import app.ficha.logic.filterEx
import app.ficha.logic.fmt
import app.ficha.logic.fmtSet
import app.ficha.logic.metricsFor
import app.ficha.logic.relDay
import app.ficha.logic.setMark
import app.ficha.logic.suggestNext
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ChartPoint
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.ExerciseDemo
import app.ficha.ui.components.ExerciseMuscles
import app.ficha.ui.components.ExerciseThumb
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.KeyValue
import app.ficha.ui.components.LineChart
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegRow
import app.ficha.ui.theme.Fx
import java.text.Collator

@Composable
fun ExercisesScreen(data: AppData) {
    val app = LocalApp.current
    var q by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val all = remember(data.custom) { data.allEx() }
    val done = remember(data.sessions) { data.sessions.flatMap { s -> s.exercises.map { it.exId } }.toSet() }
    val collator = remember { Collator.getInstance(PT_BR) }
    val groups = remember(q, group, all) {
        val f = filterEx(all, q, group)
        Catalog.groups.map { g -> g to f.filter { it.group == g }.sortedWith(compareBy(collator) { it.name }) }.filter { it.second.isNotEmpty() }
    }
    Screen(
        title = "Exercícios", subtitle = "${all.size} exercícios", back = true,
        actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "Novo exercício") } },
    ) {
        item {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Rounded.Close, "Limpar") } },
                placeholder = { Text("Buscar em português ou inglês") }, shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("") + Catalog.groups) { g -> FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g.ifEmpty { "Todos" }) }) }
            }
        }
        if (groups.isEmpty()) item {
            EmptyState(Icons.Rounded.Search, "Nada encontrado", "Tente outro termo ou crie um exercício novo.") {
                FilledTonalButton(onClick = { creating = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Criar exercício") }
            }
        }
        groups.forEach { (g, list) ->
            stickyHeader(key = "h-$g") {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(start = Gutter + 4.dp, end = Gutter + 4.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(g, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    Text(list.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            list.forEachIndexed { i, e ->
                item(key = e.id) {
                    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 2.dp)) {
                        SegRow(
                            Seg(
                                headline = e.name,
                                supporting = e.equip + (if (e.custom) " · personalizado" else "") + if (e.id in done) " · com histórico" else "",
                                leading = { ExerciseThumb(e, 52.dp) },
                                onClick = { app.go(Route.Exercise(e.id)) },
                            ),
                            i, list.size,
                        )
                    }
                }
            }
        }
    }
    if (creating) ExerciseFormSheet(null, initialName = q.trim(), initialGroup = group.ifEmpty { Catalog.groups[0] }, onDismiss = { creating = false }) {
        creating = false
        app.toast("Exercício salvo")
    }
}

/** Série como etiqueta pequena (número/tipo, carga × reps, RIR). */
@Composable
fun SetPill(kind: String, s: SetRecord, mark: String) {
    val special = s.t != null
    Surface(
        color = when {
            s.warm -> MaterialTheme.colorScheme.surfaceContainerHigh
            special -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = when {
            s.warm -> MaterialTheme.colorScheme.onSurfaceVariant
            special -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(mark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
            Text(fmtSet(kind, s), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
            if (s.rir != null) Text(" · RIR ${if (s.rir >= 5) "5+" else fmt(s.rir, 0)}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SetPills(kind: String, sets: List<SetRecord>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var n = 0
        sets.forEach { s -> SetPill(kind, s, setMark(s.warm, s.t) ?: (++n).toString()) }
    }
}

/** Caixa da progressão automática: o que fazer hoje / no próximo treino e por quê. */
@Composable
fun SuggestionBox(sg: Suggestion, prefix: String = "Hoje: ", modifier: Modifier = Modifier) {
    val (bg, fg) = when (sg.type) {
        "up" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "down" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "keep" -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("${sg.icon} ${if (sg.type == "first") "" else prefix}${sg.text}", style = MaterialTheme.typography.titleSmallEmphasized)
            Text(sg.why, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = .85f))
        }
    }
}

@Composable
fun ExerciseScreen(data: AppData, id: String) {
    val app = LocalApp.current
    val ex = data.ex(id)
    val hist = remember(data.sessions, id) { data.exHistory(id) }
    if (ex == null && hist.isEmpty()) {
        LaunchedEffect(Unit) { app.leave(Route.Exercise(id)) }
        return
    }
    val kind = ex?.kind ?: hist[0].kind
    val name = ex?.name ?: hist[0].sess.exercises.first { it.exId == id }.name
    val metrics = metricsFor(kind)
    var metricId by rememberSaveable(id) { mutableStateOf(metrics[0].id) }
    val metric = metrics.find { it.id == metricId } ?: metrics[0]
    var editing by remember { mutableStateOf(false) }
    var calc by remember { mutableStateOf<Triple<String, Double?, Double?>?>(null) }
    Screen(
        title = name, subtitle = ex?.let { "${it.group} · ${it.equip}" } ?: "Exercício removido", back = true, large = false,
        actions = { if (ex?.custom == true) IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Editar") } },
    ) {
        if (ex != null) item {
            Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseDemo(ex)
                ExerciseMuscles(ex)
                VideoActions(data, id)
            }
        }
        item { SectionHeader("Seus registros") }
        if (hist.isEmpty()) {
            item { EmptyState(Icons.AutoMirrored.Rounded.ShowChart, "Sem registros ainda", "Quando você fizer este exercício num treino, a evolução aparece aqui.") }
        } else {
            val work = hist.map { h -> h.sess.start to h.sets.filter { !it.warm } }.filter { it.second.isNotEmpty() }
            item {
                CardBox(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    KeyValue("Treinos com este exercício", hist.size.toString())
                    metrics.forEach { m ->
                        val best = work.maxOfOrNull { m.f(it.second) } ?: 0.0
                        if (best > 0) KeyValue("Recorde · ${m.label}", "${fmt(best)} ${m.unit}", Fx.colors.gold)
                    }
                    KeyValue("Última vez", relDay(hist.last().sess.start))
                }
            }
            if (data.settings.progression) {
                val target = data.routines.flatMap { it.items }.find { it.exId == id }?.reps ?: ""
                val sg = data.suggestNext(id, target)
                if (sg != null && sg.type != "first") item { SuggestionBox(sg, "Próximo treino: ", Modifier.padding(horizontal = Gutter, vertical = 10.dp)) }
            }
            if (kind == "w") item {
                val top = work.lastOrNull()?.second?.maxByOrNull { it.a ?: 0.0 }
                Row(Modifier.padding(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { calc = Triple("plates", top?.a, null) }, shapes = ButtonDefaults.shapes()) { Text("Anilhas") }
                    FilledTonalButton(onClick = { calc = Triple("warm", top?.a, null) }, shapes = ButtonDefaults.shapes()) { Text("Aquecimento") }
                    FilledTonalButton(onClick = { calc = Triple("rm", top?.a, top?.b) }, shapes = ButtonDefaults.shapes()) { Text("1RM") }
                }
            }
            item { SectionHeader("Evolução") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(metrics) { m -> FilterChip(selected = m.id == metric.id, onClick = { metricId = m.id }, label = { Text(m.label) }) }
                }
                Spacer(Modifier.height(8.dp))
                CardBox { LineChart(work.map { ChartPoint(it.first, metric.f(it.second)) }, metric.unit) }
            }
            item { SectionHeader("Histórico") }
            val recent = hist.asReversed().take(30)
            recent.forEachIndexed { i, h ->
                item(key = "h-${h.sess.id}") {
                    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 2.dp)) {
                        Surface(
                            onClick = { app.go(Route.Session(h.sess.id)) },
                            shape = segmentShape(i, recent.size), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(dateLong(h.sess.start), style = MaterialTheme.typography.titleSmallEmphasized)
                                Spacer(Modifier.height(8.dp))
                                SetPills(kind, h.sets)
                            }
                        }
                    }
                }
            }
        }
        if (ex?.custom == true) item {
            Button(
                onClick = {
                    app.confirm("Excluir exercício?", "“${ex.name}” sairá das fichas. O histórico de treinos é mantido.", "Excluir", danger = true) {
                        app.back()
                        app.update { d -> d.copy(custom = d.custom.filter { it.id != id }, routines = d.routines.map { r -> r.copy(items = r.items.filter { it.exId != id }) }) }
                    }
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth().padding(Gutter),
            ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Excluir exercício") }
        }
    }
    if (editing && ex != null) ExerciseFormSheet(ex, onDismiss = { editing = false }) { editing = false; app.toast("Exercício salvo") }
    calc?.let { (m, w, r) -> CalculatorSheet(data, m, w, r) { calc = null } }
}

/** Cantos da lista segmentada (grandes nas pontas, pequenos no meio). */
fun segmentShape(i: Int, n: Int): RoundedCornerShape {
    val big = 24.dp
    val small = 6.dp
    return RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == n - 1) big else small, bottomEnd = if (i == n - 1) big else small,
    )
}
