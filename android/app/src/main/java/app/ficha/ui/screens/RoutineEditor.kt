package app.ficha.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Program
import app.ficha.data.Routine
import app.ficha.data.RoutineItem
import app.ficha.data.uid
import app.ficha.logic.DAY
import app.ficha.logic.REST_OPTIONS
import app.ficha.logic.WEEK_ORDER
import app.ficha.logic.duplicateRoutine
import app.ficha.logic.ex
import app.ficha.logic.exKind
import app.ficha.logic.exName
import app.ficha.logic.fmtRest
import app.ficha.logic.newItemFor
import app.ficha.logic.programOf
import app.ficha.logic.ssInfo
import app.ficha.logic.startFromRoutine
import app.ficha.logic.swap
import app.ficha.logic.toggleSS
import app.ficha.logic.updateRoutine
import app.ficha.logic.withItems
import app.ficha.logic.without
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.ExercisePic
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader

@Composable
fun RoutineScreen(data: AppData, id: String) {
    val app = LocalApp.current
    val r = data.routines.find { it.id == id }
    if (r == null) {
        LaunchedEffect(Unit) { app.leave(Route.Routine(id)) }
        return
    }
    val prog = data.programOf(r)
    var name by remember(id) { mutableStateOf(r.name) }
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var howTo by remember { mutableStateOf<String?>(null) }
    val edit = { f: (Routine) -> Routine -> app.update { it.updateRoutine(id, f) } }

    Screen(
        title = r.name.ifBlank { "Ficha" }, subtitle = prog?.name ?: "Ficha avulsa", back = true, large = false,
        actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Mais opções") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Duplicar ficha") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                    menu = false
                    val (nd, nid) = app.data.duplicateRoutine(id)
                    app.update { nd }
                    app.replace(Route.Routine(nid))
                })
                DropdownMenuItem(text = { Text("Excluir ficha", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = {
                    menu = false
                    app.confirm("Excluir ficha?", "“${r.name.ifBlank { "Sem nome" }}” será removida. Os treinos já registrados continuam no histórico.", "Excluir", danger = true) {
                        app.back()
                        app.update { d -> d.copy(routines = d.routines.filter { it.id != id }) }
                    }
                })
            }
        },
    ) {
        item {
            OutlinedTextField(
                value = name, onValueChange = { v -> name = v.take(60); edit { it.copy(name = name) } },
                label = { Text("Nome da ficha") }, placeholder = { Text("Ex.: A — Peito e tríceps") }, singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            Spacer(Modifier.height(12.dp))
            val newProg = "__new"
            DropdownField(
                "Programa",
                listOf("" to "Nenhum (ficha avulsa)") + data.programs.map { it.id to it.name.ifBlank { "Sem nome" } } + (newProg to "+ Novo programa…"),
                r.programId ?: "",
                { v ->
                    if (v == newProg) {
                        app.prompt("Novo programa", "Meu treino", "Nome do programa", ok = "Criar") { n ->
                            if (n.isNotBlank()) {
                                val p = Program(uid(), n.trim(), true)
                                app.update { d -> d.copy(programs = d.programs + p).updateRoutine(id) { it.copy(programId = p.id) } }
                                app.toast("Ficha movida para o programa")
                            }
                        }
                    } else {
                        edit { it.copy(programId = v.ifEmpty { null }) }
                        app.toast(if (v.isNotEmpty()) "Ficha movida para o programa" else "Ficha agora é avulsa")
                    }
                },
                Modifier.padding(horizontal = Gutter),
            )
        }
        item { SectionHeader("Dias da semana") }
        item { DayToggles(r.days) { d -> edit { it.copy(days = if (d in it.days) it.days - d else it.days + d) } } }
        item { SectionHeader("Exercícios", trailing = r.items.size.toString()) }
        if (r.items.isEmpty()) item { EmptyState(Icons.Rounded.FitnessCenter, "Ficha vazia", "Adicione os exercícios deste treino.") }
        val ssList = r.items.map { it.ss }
        r.items.forEachIndexed { i, it ->
            item(key = it.id) {
                val ssi = ssInfo(ssList, i)
                if (ssi != null && ssi.first) Text(
                    "${ssi.g.name} ${ssi.g.letter} · sem descanso entre os exercícios; o descanso do último vale para a rodada",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = Gutter + 4.dp, end = Gutter, top = 8.dp, bottom = 4.dp),
                )
                RoutineItemCard(
                    data, it, i, r.items.size, ssi?.badge,
                    onChange = { n -> edit { rr -> rr.copy(items = rr.items.map { x -> if (x.id == n.id) n else x }) } },
                    onMove = { d -> edit { rr -> rr.withItems(rr.items.swap(i, i + d)) } },
                    onRemove = { edit { rr -> rr.withItems(rr.items.without(i)) } },
                    onHowTo = { howTo = it.exId },
                )
                if (i < r.items.size - 1) {
                    val linked = it.ss != null && r.items[i + 1].ss == it.ss
                    TextButton(onClick = { edit { rr -> rr.toggleSS(i) } }, modifier = Modifier.padding(horizontal = Gutter)) {
                        Icon(if (linked) Icons.Rounded.LinkOff else Icons.Rounded.Link, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (linked) "Em supersérie · separar" else "Juntar em supersérie")
                    }
                } else Spacer(Modifier.height(8.dp))
            }
        }
        item {
            Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { picking = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar exercícios")
                }
                if (r.items.isNotEmpty()) Button(
                    onClick = { app.startWorkout { it.startFromRoutine(r) } }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Iniciar treino", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (picking) ExercisePickerSheet(data, onDismiss = { picking = false }) { ids ->
        edit { rr -> rr.copy(items = rr.items + ids.mapNotNull { exId -> app.data.ex(exId)?.let { newItemFor(it, app.data.settings.rest) } }) }
    }
    howTo?.let { HowToSheet(data, it, { howTo = null }) }
}

/** Seg a Dom como um grupo de botões conectados do Material Expressive. */
@Composable
fun DayToggles(days: List<Int>, onToggle: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        WEEK_ORDER.forEachIndexed { i, d ->
            ToggleButton(
                checked = d in days, onCheckedChange = { onToggle(d) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    WEEK_ORDER.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.weight(1f).semantics { role = Role.Checkbox },
            ) { Text(DAY[d], maxLines = 1, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun RoutineItemCard(
    data: AppData,
    it: RoutineItem,
    i: Int,
    count: Int,
    ssBadge: String?,
    onChange: (RoutineItem) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onHowTo: () -> Unit,
) {
    val ex = data.ex(it.exId)
    val kind = data.exKind(it.exId)
    Surface(
        color = if (ssBadge != null) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ExercisePic(ex, Modifier.size(88.dp).clickable(onClick = onHowTo), phase = i)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    if (ssBadge != null) Pill(ssBadge, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.padding(bottom = 4.dp))
                    Text("${i + 1}. ${data.exName(it.exId)}", style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (ex != null) Text("${ex.group} · ${ex.equip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    IconButton(onClick = { onMove(-1) }, enabled = i > 0, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.KeyboardArrowUp, "Subir") }
                    IconButton(onClick = { onMove(1) }, enabled = i < count - 1, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.KeyboardArrowDown, "Descer") }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Delete, "Remover", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var sets by remember(it.id) { mutableStateOf(it.sets.toString()) }
                OutlinedTextField(
                    sets, { v -> sets = v.filter(Char::isDigit).take(2); sets.toIntOrNull()?.let { n -> onChange(it.copy(sets = n.coerceIn(1, 20))) } },
                    label = { Text("Séries") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(0.8f),
                )
                var reps by remember(it.id) { mutableStateOf(it.reps) }
                OutlinedTextField(
                    reps, { v -> reps = v.take(12); onChange(it.copy(reps = reps)) },
                    label = { Text(when (kind) { "s" -> "Segundos"; "c" -> "Minutos"; else -> "Reps" }) }, placeholder = { Text("8-12") },
                    singleLine = true, textStyle = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f),
                )
                DropdownField(
                    "Descanso", REST_OPTIONS.map { s -> s.toString() to (if (s == 0) "—" else fmtRest(s)) }, it.rest.toString(),
                    { v -> onChange(it.copy(rest = v.toInt())) }, Modifier.weight(1.2f),
                )
            }
            var note by remember(it.id) { mutableStateOf(it.note) }
            OutlinedTextField(
                note, { v -> note = v; onChange(it.copy(note = v)) },
                placeholder = { Text("Observação (ex.: banco no 3)") }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
