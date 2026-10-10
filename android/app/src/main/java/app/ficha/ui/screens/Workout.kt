package app.ficha.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.data.ActiveExercise
import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.logic.REST_OPTIONS
import app.ficha.logic.SET_TYPES
import app.ficha.logic.addSet
import app.ficha.logic.clock
import app.ficha.logic.commitWorkout
import app.ficha.logic.ex
import app.ficha.logic.fmtInt
import app.ficha.logic.fmtRest
import app.ficha.logic.fmtSet
import app.ficha.logic.lastSets
import app.ficha.logic.loadStep
import app.ficha.logic.makeActiveExercise
import app.ficha.logic.num
import app.ficha.logic.placeholder
import app.ficha.logic.replaceAt
import app.ficha.logic.routineChanged
import app.ficha.logic.setMark
import app.ficha.logic.setType
import app.ficha.logic.setTypeOf
import app.ficha.logic.ssInfo
import app.ficha.logic.stats
import app.ficha.logic.suggestNext
import app.ficha.logic.swap
import app.ficha.logic.toggleSS
import app.ficha.logic.toggleSet
import app.ficha.logic.updateActive
import app.ficha.logic.updateEx
import app.ficha.logic.withExercises
import app.ficha.logic.withWarmup
import app.ficha.logic.without
import app.ficha.ui.AppController
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.ExercisePic
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.KeyValue
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.ShapeIcon
import app.ficha.ui.rememberNow
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(data: AppData) {
    val app = LocalApp.current
    val a = data.active
    if (a == null) {
        LaunchedEffect(Unit) { app.leave(Route.Workout) }
        return
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf<Int?>(null) }
    var menuFor by remember { mutableStateOf<Int?>(null) }
    var typeFor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var howTo by remember { mutableStateOf<String?>(null) }
    var rirHelp by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var calc by remember { mutableStateOf<Double?>(null) }
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { app.back() }) { Icon(Icons.Rounded.KeyboardArrowDown, "Minimizar") } },
                title = {
                    Text(
                        a.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { app.prompt("Nome do treino", a.name) { n -> if (n.isNotBlank()) app.update { it.updateActive { w -> w.copy(name = n.trim()) } } } },
                    )
                },
                actions = {
                    Button(onClick = { finishing = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(end = 8.dp)) { Text("Finalizar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { pad ->
        LazyColumn(
            Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
        ) {
            item(key = "meta") { WorkoutMeta(data) }
            if (a.exercises.isEmpty()) item { EmptyState(Icons.Rounded.Add, "Treino livre", "Adicione os exercícios conforme for treinando.") }
            val ssList = a.exercises.map { it.ss }
            a.exercises.forEachIndexed { x, ex ->
                item(key = ex.uid) {
                    ExerciseCard(
                        data, ex, x, ssList,
                        onToggle = { i ->
                            focus.clearFocus()
                            val wasDone = ex.sets[i].done
                            val res = app.data.toggleSet(x, i)
                            app.update { res.data }
                            if (!wasDone && res.missing == null) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            else if (wasDone) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            res.message?.let(app::toast)
                            res.scrollTo?.let { k -> scope.launch { listState.animateScrollToItem(k + 1) } }
                        },
                        onMenu = { menuFor = x },
                        onType = { i -> typeFor = x to i },
                        onHowTo = { howTo = ex.exId },
                        onRirHelp = { rirHelp = true },
                    )
                }
            }
            item(key = "foot") {
                Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { adding = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar exercício", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = a.notes, onValueChange = { v -> app.update { it.updateActive { w -> w.copy(notes = v) } } },
                        label = { Text("Anotações do treino") }, placeholder = { Text("Como foi o treino? Energia, dores, ajustes…") },
                        minLines = 3, modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            app.confirm("Descartar treino?", "Nada deste treino será salvo no histórico.", "Descartar", danger = true) {
                                app.update { it.copy(active = null) }
                                app.leave(Route.Workout)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Descartar treino") }
                }
            }
        }
    }

    if (adding) ExercisePickerSheet(data, onDismiss = { adding = false }) { ids ->
        app.update { d ->
            d.updateActive { w ->
                w.copy(exercises = w.exercises + ids.map { id ->
                    val e = d.ex(id)
                    d.makeActiveExercise(id, if (e?.kind == "c") 1 else 3, "", if (e?.kind == "c") 0 else d.settings.rest)
                })
            }
        }
        scope.launch { listState.animateScrollToItem((app.data.active?.exercises?.size ?: 1)) }
    }
    replacing?.let { x ->
        ExercisePickerSheet(data, multi = false, onDismiss = { replacing = null }) { (id) ->
            app.update { d ->
                d.updateActive { w ->
                    val old = w.exercises[x]
                    w.copy(exercises = w.exercises.replaceAt(x, d.makeActiveExercise(id, old.sets.size, old.target, old.rest).copy(ss = old.ss)))
                }
            }
        }
    }
    menuFor?.let { x -> a.exercises.getOrNull(x)?.let { ex -> ExerciseMenuSheet(data, x, ex, onDismiss = { menuFor = null }, onReplace = { replacing = x }, onHowTo = { howTo = ex.exId }, onCalc = { w -> calc = w }) } }
    typeFor?.let { (x, i) -> a.exercises.getOrNull(x)?.takeIf { i in it.sets.indices }?.let { ex -> SetTypeSheet(ex, x, i) { typeFor = null } } }
    howTo?.let { HowToSheet(data, it, { howTo = null }) }
    if (rirHelp) RirHelpSheet { rirHelp = false }
    if (finishing) FinishSheet(data) { finishing = false }
    calc?.let { w -> CalculatorSheet(data, "plates", w, null) { calc = null } }
}

@Composable
private fun WorkoutMeta(data: AppData) {
    val a = data.active ?: return
    val now by rememberNow(1000)
    val st = a.stats()
    val progress by animateFloatAsState(st.p, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "progresso")
    Column(Modifier.padding(horizontal = Gutter, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Meta(Icons.Rounded.Timer, clock((now - a.start) / 1000.0))
            Meta(Icons.Rounded.Check, "${st.done}/${st.total} séries")
            if (st.vol > 0) Meta(Icons.Rounded.LocalFireDepartment, "${fmtInt(st.vol)} kg")
        }
        Spacer(Modifier.height(12.dp))
        LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
private fun ExerciseCard(
    data: AppData,
    ex: ActiveExercise,
    x: Int,
    ssList: List<String?>,
    onToggle: (Int) -> Unit,
    onMenu: () -> Unit,
    onType: (Int) -> Unit,
    onHowTo: () -> Unit,
    onRirHelp: () -> Unit,
) {
    val app = LocalApp.current
    val k = Catalog.kinds[ex.kind] ?: Catalog.kinds.getValue("w")
    val def = data.ex(ex.exId)
    val allDone = ex.sets.isNotEmpty() && ex.sets.all { it.done }
    val sg = remember(data.sessions, ex.exId, ex.target, data.settings.progression) { if (data.settings.progression) data.suggestNext(ex.exId, ex.target) else null }
    val last = remember(data.sessions, ex.exId) { data.lastSets(ex.exId) }
    val step = loadStep(def)
    val rir = data.settings.rir && (ex.kind == "w" || ex.kind == "bw")
    val ssi = ssInfo(ssList, x)
    val container by animateColorAsState(
        if (allDone) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "card",
    )
    if (ssi != null && ssi.first) Text(
        "${ssi.g.name} ${ssi.g.letter} · faça uma série de cada, sem descanso, e descanse no fim da rodada",
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(start = Gutter + 4.dp, end = Gutter, top = 12.dp, bottom = 2.dp),
    )
    Surface(color = container, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
        Column(Modifier.padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box {
                    ExercisePic(def, Modifier.size(76.dp).clickable(onClick = onHowTo), phase = x)
                    if (allDone) ShapeIcon(
                        Icons.Rounded.Check, shape = MaterialShapes.SoftBurst.toShape(), size = 30.dp,
                        container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    if (ssi != null) Pill(ssi.badge, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.padding(bottom = 4.dp))
                    Text(
                        ex.name, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { app.go(Route.Exercise(ex.exId)) },
                    )
                    val unit = when (ex.kind) { "s" -> "s"; "c" -> " min"; else -> "" }
                    Text(
                        "${ex.sets.count { !it.warm }} × ${ex.target.ifBlank { "—" }}$unit · descanso ${fmtRest(ex.rest)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, "Opções do exercício") }
            }
            if (ex.note.isNotBlank()) Text(
                ex.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, end = 4.dp),
            )
            if (sg != null && !allDone) SuggestionBox(sg, modifier = Modifier.padding(top = 10.dp, end = 4.dp))
            Spacer(Modifier.height(10.dp))
            // Cabeçalho da tabela de séries
            Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val st = MaterialTheme.typography.labelMedium
                val c = MaterialTheme.colorScheme.onSurfaceVariant
                Text("Série", style = st, color = c, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                Text("Anterior", style = st, color = c, modifier = Modifier.weight(1.15f), textAlign = TextAlign.Center)
                Text(k.a, style = st, color = c, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (k.b != null) Text(k.b, style = st, color = c, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (rir) Text("RIR", style = st, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(48.dp).clickable(onClick = onRirHelp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(52.dp))
            }
            var wn = 0
            var wi = 0
            ex.sets.forEachIndexed { i, s ->
                val label = setMark(s.warm, s.t) ?: (++wn).toString()
                val ref = if (s.warm) null else last.getOrNull(wi++)
                val bg by animateColorAsState(
                    if (s.done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else androidx.compose.ui.graphics.Color.Transparent,
                    MaterialTheme.motionScheme.fastEffectsSpec(), label = "serie",
                )
                Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Row(Modifier.padding(vertical = 4.dp, horizontal = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                        SetNumber(label, setTypeOf(s.warm, s.t)) { onType(i) }
                        Text(
                            ref?.let { fmtSet(ex.kind, it) } ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.15f), textAlign = TextAlign.Center, maxLines = 1,
                        )
                        NumField(s.a, placeholder(ex, i, 'a', sg, last, step), Modifier.weight(1f).padding(horizontal = 3.dp)) { v ->
                            app.update { d -> d.updateActive { w -> w.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, e.sets[i].copy(a = v))) } } }
                        }
                        if (k.b != null) NumField(s.b, placeholder(ex, i, 'b', sg, last, step), Modifier.weight(1f).padding(horizontal = 3.dp)) { v ->
                            app.update { d -> d.updateActive { w -> w.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, e.sets[i].copy(b = v))) } } }
                        }
                        if (rir) Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                            if (!s.warm) RirPicker(s.rir) { v -> app.update { d -> d.updateActive { w -> w.updateEx(x) { e -> e.copy(sets = e.sets.replaceAt(i, e.sets[i].copy(rir = v))) } } } }
                        }
                        CheckButton(s.done) { onToggle(i) }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { app.update { d -> d.updateActive { w -> w.updateEx(x) { it.addSet() } } } }, shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraSmallContainerHeight),
                ) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Série") }
                if (ex.sets.size > 1) OutlinedButton(
                    onClick = { app.update { d -> d.updateActive { w -> w.updateEx(x) { it.copy(sets = it.sets.dropLast(1)) } } } }, shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraSmallContainerHeight),
                ) { Icon(Icons.Rounded.Remove, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Remover série") }
            }
        }
    }
}

/** Número da série (ou A, D, F para aquecimento, drop set e até a falha); toque troca o tipo. */
@Composable
private fun SetNumber(label: String, type: String, onClick: () -> Unit) {
    val (bg, fg) = when (type) {
        "warm" -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        "drop" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "fail" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, color = bg, contentColor = fg, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Campo numérico compacto da tabela de séries; ao focar, seleciona tudo. */
@Composable
fun NumField(value: String, placeholder: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var tfv by remember { mutableStateOf(TextFieldValue(value)) }
    val shown = if (tfv.text == value) tfv else TextFieldValue(value, TextRange(value.length))
    val focus = LocalFocusManager.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(12.dp), modifier = modifier.height(40.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            if (value.isEmpty()) Text(
                placeholder, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f), textAlign = TextAlign.Center, maxLines = 1,
            )
            BasicTextField(
                value = shown,
                onValueChange = { n ->
                    val f = n.text.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                    tfv = n.copy(text = f)
                    if (f != value) onChange(f)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) tfv = TextFieldValue(value, TextRange(0, value.length)) },
            )
        }
    }
}

@Composable
private fun RirPicker(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(44.dp, 40.dp)) {
            Text(
                when (value) { "" -> "–"; "5" -> "5+"; else -> value }, style = MaterialTheme.typography.titleMedium,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(open, { open = false }) {
            listOf("" to "– (sem RIR)", "0" to "0 · falha", "1" to "1", "2" to "2", "3" to "3", "4" to "4", "5" to "5+").forEach { (v, l) ->
                DropdownMenuItem(text = { Text(l) }, onClick = { open = false; onChange(v) })
            }
        }
    }
}

/** Botão de concluir a série: vira uma forma cheia com ✓ e dá um pulinho (mola). */
@Composable
private fun CheckButton(done: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (done) 1f else 0.92f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "check",
    )
    val shape = if (done) MaterialShapes.Cookie4Sided.toShape() else CircleShape
    Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick, shape = shape,
            color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(42.dp).scale(scale),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, "Concluir série", Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun SetTypeSheet(ex: ActiveExercise, x: Int, i: Int, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val cur = setTypeOf(ex.sets[i].warm, ex.sets[i].t)
    Sheet(onDismiss, "Tipo de série") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            SegmentedList(SET_TYPES.map { t ->
                Seg(
                    headline = t.label, supporting = t.desc,
                    leading = { SetNumber(t.mark ?: "1", t.id) {} },
                    trailing = if (cur == t.id) ({ Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }) else null,
                    onClick = {
                        app.update { d -> d.updateActive { w -> w.updateEx(x) { it.setType(i, t.id) } } }
                        onDismiss()
                    },
                )
            }, Modifier)
            if (ex.sets.size > 1) OutlinedButton(
                onClick = { app.update { d -> d.updateActive { w -> w.updateEx(x) { it.copy(sets = it.sets.without(i)) } } }; onDismiss() },
                shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Remover esta série") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RirHelpSheet(onDismiss: () -> Unit) {
    Sheet(onDismiss, "RIR: repetições na reserva") {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text("Quantas repetições você ainda conseguiria fazer ao terminar a série, com boa execução.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            listOf("0" to "Falha: não sairia mais nenhuma", "1–2" to "Bem perto da falha (o ideal para hipertrofia)", "3–4" to "Moderado: sobrou um pouco", "5+" to "Leve: sobrou bastante")
                .forEach { (v, t) -> KeyValue(t, v) }
            Text(
                "É opcional. Quando você registra, a progressão automática acerta melhor o próximo passo: se sobrou muito, ela sobe mais a carga. Dá para esconder em Ajustes → Treino.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ExerciseMenuSheet(
    data: AppData,
    x: Int,
    ex: ActiveExercise,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
    onHowTo: () -> Unit,
    onCalc: (Double?) -> Unit,
) {
    val app = LocalApp.current
    val list = data.active?.exercises ?: return
    val upd = { f: (ActiveExercise) -> ActiveExercise -> app.update { d -> d.updateActive { w -> w.updateEx(x, f) } } }
    Sheet(onDismiss, ex.name) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text("Descanso entre séries", style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                REST_OPTIONS.forEach { s -> FilterChip(selected = ex.rest == s, onClick = { upd { it.copy(rest = s) } }, label = { Text(if (s == 0) "Nenhum" else fmtRest(s)) }) }
            }
            Spacer(Modifier.height(12.dp))
            val rows = buildList {
                if (x < list.size - 1) {
                    val linked = ex.ss != null && list[x + 1].ss == ex.ss
                    add(Seg(
                        headline = if (linked) "Separar do próximo exercício" else "Fazer supersérie com o próximo", supporting = list[x + 1].name,
                        leading = { Icon(Icons.Rounded.Link, null) },
                        onClick = { app.update { d -> d.updateActive { it.toggleSS(x) } }; onDismiss() },
                    ))
                }
                if (x > 0) add(Seg(headline = "Mover para cima", leading = { Icon(Icons.Rounded.KeyboardArrowUp, null) }, onClick = {
                    app.update { d -> d.updateActive { w -> w.withExercises(w.exercises.swap(x, x - 1)) } }; onDismiss()
                }))
                if (x < list.size - 1) add(Seg(headline = "Mover para baixo", leading = { Icon(Icons.Rounded.KeyboardArrowDown, null) }, onClick = {
                    app.update { d -> d.updateActive { w -> w.withExercises(w.exercises.swap(x, x + 1)) } }; onDismiss()
                }))
                if (ex.kind == "w") {
                    add(Seg(headline = "Adicionar aquecimento", supporting = "Séries leves calculadas pela carga de trabalho", leading = { Icon(Icons.Rounded.LocalFireDepartment, null) }, onClick = {
                        val (nd, msg) = app.data.withWarmup(x)
                        app.update { nd }
                        app.toast(msg)
                        onDismiss()
                    }))
                    add(Seg(headline = "Calculadora de anilhas", leading = { Icon(Icons.Rounded.Calculate, null) }, onClick = {
                        val i0 = ex.sets.indexOfFirst { !it.warm && !it.done }.coerceAtLeast(0)
                        val sg = data.suggestNext(ex.exId, ex.target)
                        val w = num(ex.sets.getOrNull(i0)?.a) ?: num(placeholder(ex, i0, 'a', sg, data.lastSets(ex.exId), loadStep(data.ex(ex.exId))))
                        onDismiss(); onCalc(w)
                    }))
                }
                add(Seg(headline = "Ver execução (foto e vídeo)", leading = { Icon(Icons.Rounded.PlayCircle, null) }, onClick = { onDismiss(); onHowTo() }))
                add(Seg(headline = "Substituir exercício", leading = { Icon(Icons.Rounded.SwapHoriz, null) }, onClick = { onDismiss(); onReplace() }))
                add(Seg(headline = "Ver evolução", leading = { Icon(Icons.AutoMirrored.Rounded.ShowChart, null) }, onClick = { onDismiss(); app.go(Route.Exercise(ex.exId)) }))
                add(Seg(
                    headline = "Remover do treino", headlineColor = MaterialTheme.colorScheme.error,
                    leading = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { app.update { d -> d.updateActive { w -> w.withExercises(w.exercises.without(x)) } }; onDismiss() },
                ))
            }
            SegmentedList(rows, Modifier)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Finalizar: confirma, salva no histórico (com recordes) e mostra o resumo. */
@Composable
private fun FinishSheet(data: AppData, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val a = data.active ?: return
    val done = a.exercises.sumOf { e -> e.sets.count { it.done } }
    val pending = a.exercises.sumOf { e -> e.sets.count { !it.done } }
    if (done == 0) {
        LaunchedEffect(Unit) {
            onDismiss()
            app.confirm("Nenhuma série concluída", "Marque as séries com ✓ conforme for treinando. Deseja descartar este treino?", "Descartar treino", danger = true) {
                app.update { it.copy(active = null) }
                app.leave(Route.Workout)
            }
        }
        return
    }
    val changed = remember { data.routineChanged() }
    var updRoutine by remember { mutableStateOf(false) }
    Sheet(onDismiss, "Finalizar treino?") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(
                "$done série${if (done > 1) "s" else ""} concluída${if (done > 1) "s" else ""}" +
                    if (pending > 0) " · $pending não marcada${if (pending > 1) "s" else ""} ${if (pending > 1) "serão descartadas" else "será descartada"}." else ".",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp),
            )
            if (changed != null) {
                Spacer(Modifier.height(12.dp))
                SegmentedList(listOf(Seg(
                    headline = "Atualizar a ficha", supporting = "Salvar exercícios e nº de séries de hoje em “${changed.name}”",
                    trailing = { Switch(updRoutine, { updRoutine = it }) }, onClick = { updRoutine = !updRoutine },
                )), Modifier)
            }
            Button(
                onClick = { finish(app, updRoutine); onDismiss() }, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(ButtonDefaults.MediumContainerHeight),
            ) { Text("Finalizar e salvar", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun finish(app: AppController, updateRoutine: Boolean) {
    val (nd, sess) = app.data.commitWorkout(updateRoutine) ?: return
    app.update { nd }
    app.summaryFor = sess.id
    app.replace(Route.Session(sess.id))
}
