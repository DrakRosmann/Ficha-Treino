package app.ficha.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.data.Exercise
import app.ficha.data.uid
import app.ficha.logic.BARS
import app.ficha.logic.PLATES
import app.ficha.logic.PLATE_COLORS
import app.ficha.logic.RM_REPS
import app.ficha.logic.allEx
import app.ficha.logic.e1rm
import app.ficha.logic.ex
import app.ficha.logic.filterEx
import app.ficha.logic.fmt
import app.ficha.logic.fmtIn
import app.ficha.logic.num
import app.ficha.logic.platesFor
import app.ficha.logic.rmLoad
import app.ficha.logic.roundTo
import app.ficha.logic.warmupSets
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.ExerciseDemo
import app.ficha.ui.components.ExerciseMuscles
import app.ficha.ui.components.ExerciseThumb
import app.ficha.ui.components.KeyValue
import kotlinx.coroutines.launch
import java.net.URLEncoder

/** Painel padrão: ocupa a altura toda quando precisa e tem título com botão de fechar. */
@Composable
fun Sheet(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbar = LocalApp.current.snackbar
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box {
            Column {
                if (title != null) {
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val scope = rememberCoroutineScope()
                        IconButton(onClick = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }) { Icon(Icons.Rounded.Close, "Fechar") }
                    }
                }
                androidx.compose.runtime.CompositionLocalProvider(app.ficha.ui.components.LocalSegColor provides MaterialTheme.colorScheme.surfaceContainerHigh) { content() }
            }
            // O painel fica numa janela por cima do app: os avisos precisam aparecer aqui também
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}

fun youtubeUrl(q: String) = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(q, "UTF-8")

/** Execução do exercício: foto animada, músculos, vídeos e atalho para a evolução. */
@Composable
fun HowToSheet(data: AppData, exId: String, onDismiss: () -> Unit, showEvolution: Boolean = true, footer: (@Composable ColumnScope.() -> Unit)? = null) {
    val app = LocalApp.current
    val ex = data.ex(exId)
    Sheet(onDismiss, ex?.name ?: "Exercício") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            ExerciseDemo(ex)
            if (ex != null) Text(
                "${ex.group} · ${ex.equip}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 10.dp),
            )
            ExerciseMuscles(ex)
            Spacer(Modifier.height(12.dp))
            VideoActions(data, exId)
            if (showEvolution) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onDismiss(); app.go(Route.Exercise(exId)) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.ShowChart, null); Spacer(Modifier.width(8.dp)); Text("Evolução e histórico")
                }
            }
            if (footer != null) {
                Spacer(Modifier.height(12.dp))
                footer()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun VideoActions(data: AppData, exId: String) {
    val app = LocalApp.current
    val context = LocalContext.current
    val ex = data.ex(exId)
    val name = ex?.name ?: "exercício"
    val mine = data.videos[exId]
    val open = { url: String -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mine != null) Button(onClick = { open(mine) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Meu vídeo")
        }
        FilledTonalButton(onClick = { open(youtubeUrl("como fazer $name")) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Ver vídeos da execução")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (!ex?.en.isNullOrBlank()) TextButton(onClick = { open(youtubeUrl("${ex.en} exercise")) }) { Text("Buscar em inglês") } else Spacer(Modifier)
            TextButton(onClick = {
                app.prompt(
                    "Meu vídeo", mine ?: "", "Link do vídeo", "https://…",
                ) { v ->
                    val t = v.trim()
                    if (t.isNotEmpty() && !Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(t)) {
                        app.toast("Link inválido — ele deve começar com https://")
                    } else {
                        app.update { d -> d.copy(videos = if (t.isEmpty()) d.videos - exId else d.videos + (exId to t)) }
                        app.toast(if (t.isEmpty()) "Vídeo removido" else "Vídeo salvo")
                    }
                }
            }) {
                Icon(Icons.Rounded.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(if (mine != null) "Trocar meu vídeo" else "Adicionar meu vídeo")
            }
        }
    }
}

/** Seletor de exercícios com busca (português ou inglês) e filtro por grupo. */
@Composable
fun ExercisePickerSheet(
    data: AppData,
    multi: Boolean = true,
    onDismiss: () -> Unit,
    onDone: (List<String>) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    val sel = remember { mutableStateListOf<String>() }
    var preview by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val all = remember(data.custom) { data.allEx() }
    val list = remember(q, group, all) {
        val f = filterEx(all, q, group)
        Catalog.groups.flatMap { g -> f.filter { it.group == g }.sortedWith(compareBy(java.text.Collator.getInstance(app.ficha.logic.PT_BR)) { it.name }) }
    }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val finish = { ids: List<String> -> scope.launch { state.hide() }.invokeOnCompletion { onDismiss(); onDone(ids) } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box {
            Column(Modifier.fillMaxHeight(0.94f).imePadding()) {
                Text(
                    if (multi) "Adicionar exercícios" else "Escolher exercício", style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Rounded.Close, "Limpar") } },
                    placeholder = { Text("Buscar exercício") },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("") + Catalog.groups) { g ->
                        FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g.ifEmpty { "Todos" }) })
                    }
                }
                val listState = rememberLazyListState()
                // A lista tem chaves: sem isso, ao digitar ela fica presa num item que mudou de lugar
                LaunchedEffect(q, group) { listState.scrollToItem(0) }
                LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
                    var lastGroup = ""
                    list.forEach { e ->
                        if (e.group != lastGroup) {
                            lastGroup = e.group
                            val g = e.group
                            stickyHeader(key = "h-$g") {
                                Text(
                                    g, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .padding(horizontal = 24.dp, vertical = 8.dp),
                                )
                            }
                        }
                        item(key = e.id) {
                            val on = e.id in sel
                            ListItem(
                                onClick = {
                                    if (!multi) finish(listOf(e.id))
                                    else if (on) sel.remove(e.id) else sel.add(e.id)
                                },
                                leadingContent = { ExerciseThumb(e, 52.dp) },
                                supportingContent = { Text(e.equip + if (e.custom) " · personalizado" else "") },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { preview = e.id }) { Icon(Icons.Rounded.OpenInFull, "Ver execução", Modifier.size(20.dp)) }
                                        if (multi) Icon(
                                            if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                                            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                                content = { Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                    if (list.isEmpty()) item {
                        Text("Nada encontrado. Não achou? Crie o exercício.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                    }
                    item {
                        OutlinedButton(onClick = { creating = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp))
                            Text("Criar exercício" + if (q.isNotBlank()) " “${q.trim()}”" else "")
                        }
                    }
                }
                if (multi) {
                    HorizontalDivider()
                    Button(
                        onClick = { if (sel.isNotEmpty()) finish(sel.toList()) }, enabled = sel.isNotEmpty(), shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding().height(ButtonDefaults.MediumContainerHeight),
                    ) {
                        Text(if (sel.isEmpty()) "Selecione os exercícios" else "Adicionar ${sel.size} exercício${if (sel.size > 1) "s" else ""}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            SnackbarHost(LocalApp.current.snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
    preview?.let { id ->
        HowToSheet(data, id, onDismiss = { preview = null }, showEvolution = false) {
            val on = id in sel
            Button(onClick = {
                preview = null
                if (!multi) finish(listOf(id)) else if (on) sel.remove(id) else sel.add(id)
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Text(if (!multi) "Escolher" else if (on) "Desmarcar" else "Selecionar")
            }
        }
    }
    if (creating) {
        ExerciseFormSheet(null, initialName = q.trim(), initialGroup = group.ifEmpty { Catalog.groups[0] }, onDismiss = { creating = false }) { ex ->
            creating = false
            if (!multi) finish(listOf(ex.id)) else {
                sel.add(ex.id); q = ""; group = ex.group
            }
        }
    }
}

@Composable
fun DropdownField(label: String, options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.find { it.first == value }?.second ?: value, onValueChange = {}, readOnly = true, singleLine = true,
            label = { Text(label) },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (k, v) -> DropdownMenuItem(text = { Text(v) }, onClick = { onChange(k); open = false }) }
        }
    }
}

/** Criar ou editar um exercício personalizado. */
@Composable
fun ExerciseFormSheet(ex: Exercise?, initialName: String = "", initialGroup: String = "Peito", onDismiss: () -> Unit, onSaved: (Exercise) -> Unit) {
    val app = LocalApp.current
    var name by remember { mutableStateOf(ex?.name ?: initialName) }
    var group by remember { mutableStateOf(ex?.group ?: initialGroup) }
    var equip by remember { mutableStateOf(ex?.equip ?: "Halteres") }
    var kind by remember { mutableStateOf(ex?.kind ?: "w") }
    Sheet(onDismiss, if (ex != null) "Editar exercício" else "Novo exercício") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(60) }, label = { Text("Nome") }, placeholder = { Text("Ex.: Remada articulada") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            DropdownField("Grupo muscular", Catalog.groups.map { it to it }, group, { group = it })
            DropdownField("Equipamento", Catalog.equipment.map { it to it }, equip, { equip = it })
            DropdownField("Como registrar", Catalog.kinds.map { (k, v) -> k to v.label }, kind, { kind = it })
            Button(onClick = {
                if (name.isBlank()) { app.toast("Dê um nome ao exercício"); return@Button }
                val saved = (ex ?: Exercise(id = "c-" + uid(), name = "", custom = true)).copy(name = name.trim(), group = group, equip = equip, kind = kind, custom = true)
                app.update { d -> d.copy(custom = if (ex != null) d.custom.map { if (it.id == ex.id) saved else it } else d.custom + saved) }
                onSaved(saved)
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) { Text("Salvar", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ================= Calculadoras: anilhas, aquecimento e 1RM ================= */

@Composable
fun CalculatorSheet(data: AppData, mode0: String = "plates", w0: Double? = null, r0: Double? = null, onDismiss: () -> Unit) {
    val app = LocalApp.current
    var mode by remember { mutableStateOf(mode0) }
    var w by remember { mutableStateOf(fmtIn(w0?.takeIf { it > 0 } ?: 60.0)) }
    var r by remember { mutableStateOf(fmtIn(r0?.takeIf { it > 0 } ?: 5.0)) }
    val bar = data.settings.bar ?: 20.0
    val avail = (data.settings.plates ?: PLATES).sortedDescending()
    Sheet(onDismiss, "Calculadoras") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            ConnectedChoice(listOf("plates" to "Anilhas", "warm" to "Aquecimento", "rm" to "1RM"), mode) { mode = it }
            Spacer(Modifier.height(16.dp))
            @Composable
            fun stepField(label: String, v: String, step: Double, set: (String) -> Unit, modifier: Modifier) {
                Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { set(fmtIn(maxOf(0.0, (num(v) ?: 0.0) - step))) }, shapes = ButtonDefaults.shapes()) { Text("−") }
                    OutlinedTextField(
                        v, { set(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, label = { Text(label) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    FilledTonalButton(onClick = { set(fmtIn((num(v) ?: 0.0) + step)) }, shapes = ButtonDefaults.shapes()) { Text("+") }
                }
            }
            if (mode == "rm") {
                stepField("Carga (kg)", w, 2.5, { w = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                stepField("Repetições", r, 1.0, { r = it }, Modifier.fillMaxWidth())
            } else {
                stepField(if (mode == "plates") "Peso total na barra (kg)" else "Carga de trabalho (kg)", w, 2.5, { w = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Barra", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ConnectedChoice(BARS.map { (v, l) -> v.toString() to l }, bar.toString()) { v -> app.update { it.copy(settings = it.settings.copy(bar = v.toDouble())) } }
            }
            if (mode == "plates") {
                Spacer(Modifier.height(12.dp))
                Text("Anilhas disponíveis", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PLATES.forEach { p ->
                        FilterChip(selected = p in avail, onClick = {
                            val next = if (p in avail) avail - p else avail + p
                            if (next.isEmpty()) app.toast("Deixe pelo menos uma anilha")
                            else app.update { it.copy(settings = it.settings.copy(plates = next.sortedDescending())) }
                        }, label = { Text(fmt(p, 2)) })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val wv = num(w) ?: 0.0
            when (mode) {
                "plates" -> {
                    val res = platesFor(wv, bar, avail)
                    if (res == null) Text("O peso total precisa ser pelo menos o da barra (${fmt(bar)} kg).", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        Barbell(bar, res.plates)
                        Spacer(Modifier.height(12.dp))
                        Text("Cada lado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (res.plates.isEmpty()) "nenhuma anilha" else res.plates.joinToString(" + ") { fmt(it, 2) } + " kg",
                            style = MaterialTheme.typography.headlineSmallEmphasized,
                        )
                        if (res.rest > 0) Text(
                            "Não fecha exato com essas anilhas: monta ${fmt(bar + 2 * res.plates.sum(), 2)} kg (faltam ${fmt(res.rest * 2, 2)} kg).",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                "warm" -> {
                    val sets = warmupSets(wv, bar, 2.5)
                    if (sets.isEmpty()) Text("Informe a carga de trabalho.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        sets.forEachIndexed { i, s ->
                            val res = platesFor(s.w, bar, avail)
                            val per = if (res != null && res.plates.isNotEmpty()) "cada lado: ${res.plates.joinToString(" + ") { fmt(it, 2) }}" else if (bar > 0) "só a barra" else ""
                            KeyValue("${i + 1}ª · ${Math.round(s.p * 100)}%" + if (per.isNotEmpty()) "  ·  $per" else "", "${fmt(s.w, 2)} kg × ${s.r}")
                        }
                        KeyValue("Séries de trabalho", "${fmt(wv, 2)} kg")
                        Text("Descanse cerca de 1 minuto entre as séries de aquecimento. Elas não contam no volume nem nos recordes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val rm = e1rm(wv, (num(r) ?: 0.0).let { Math.round(it).toDouble() })
                    if (rm == 0.0) Text("Informe a carga e as repetições.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        Text("1RM estimado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${fmt(rm, 1)} kg", style = MaterialTheme.typography.displaySmallEmphasized)
                        Spacer(Modifier.height(8.dp))
                        RM_REPS.forEach { n ->
                            KeyValue("$n rep${if (n > 1) "s" else ""}  ·  ${Math.round(rmLoad(100.0, n))}%", "${fmt(if (n == 1) rm else roundTo(rmLoad(rm, n), 0.5), 1)} kg")
                        }
                        Text("Estimativa pela fórmula de Epley; é mais precisa com séries de até 10 repetições.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Desenho da barra com as anilhas de um lado (as maiores junto do colar). */
@Composable
private fun Barbell(bar: Double, plates: List<Double>) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val cy = size.height / 2
        val unit = 1.dp.toPx()
        // eixo
        drawRoundRect(cs.outline, topLeft = androidx.compose.ui.geometry.Offset(0f, cy - 4 * unit), size = androidx.compose.ui.geometry.Size(size.width, 8 * unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4 * unit))
        // colar
        val collarX = size.width * 0.62f
        drawRoundRect(cs.onSurfaceVariant, topLeft = androidx.compose.ui.geometry.Offset(collarX, cy - 14 * unit), size = androidx.compose.ui.geometry.Size(10 * unit, 28 * unit), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3 * unit))
        var x = collarX - 3 * unit
        for (p in plates.sortedDescending()) {
            val h = (34 + p * 2.2).toFloat() * unit
            val w = (if (p >= 10) 13 else if (p >= 5) 9 else 7) * unit
            x -= w + 2 * unit
            drawRoundRect(Color(PLATE_COLORS[p] ?: 0xFF999999), topLeft = androidx.compose.ui.geometry.Offset(x, cy - h / 2), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3 * unit))
        }
    }
    if (bar > 0) Text("Barra de ${fmt(bar)} kg", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
}
