package app.ficha.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.data.Program
import app.ficha.data.Routine
import app.ficha.data.TemplateRoutine
import app.ficha.data.uid
import app.ficha.logic.allEx
import app.ficha.logic.createProgram
import app.ficha.logic.daysLabel
import app.ficha.logic.duplicateProgram
import app.ficha.logic.ex
import app.ficha.logic.exKind
import app.ficha.logic.exName
import app.ficha.logic.isScheduled
import app.ficha.logic.jsDay
import app.ficha.logic.letter
import app.ficha.logic.moveInProgram
import app.ficha.logic.nextInProgram
import app.ficha.logic.programOf
import app.ficha.logic.progRoutines
import app.ficha.logic.startFromRoutine
import app.ficha.logic.targetUnit
import app.ficha.logic.updateProgram
import app.ficha.ui.AppController
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.ExerciseThumb
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.ShapeIcon
import java.time.LocalDate

/** Letra da ficha numa forma "biscoito" (destacada quando é a do dia / a próxima). */
@Composable
fun LetterBadge(i: Int, on: Boolean) {
    ShapeIcon(
        text = letter(i), shape = MaterialShapes.Cookie9Sided.toShape(),
        container = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        content = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun Banner(icon: ImageVector, title: String, text: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = MaterialTheme.shapes.extraLarge,
        color = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeIcon(
                icon, shape = MaterialShapes.Sunny.toShape(), size = 48.dp,
                container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer,
                content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(text, style = MaterialTheme.typography.bodySmall, color = if (primary) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

/** Cria uma ficha vazia e abre para editar. */
fun newRoutine(app: AppController, programId: String?) {
    val r = Routine(id = uid(), programId = programId)
    app.update { it.copy(routines = it.routines + r) }
    app.go(Route.Routine(r.id))
}

/** Cria um programa vazio e abre para editar. */
fun newProgram(app: AppController) {
    val p = Program(uid(), "", true)
    app.update { it.copy(programs = it.programs + p) }
    app.go(Route.Program(p.id))
}

@Composable
fun RoutinesScreen(data: AppData) {
    val app = LocalApp.current
    var fabOpen by rememberSaveable { mutableStateOf(false) }
    Screen(
        title = "Fichas",
        fab = {
            FloatingActionButtonMenu(
                expanded = fabOpen,
                button = {
                    ToggleFloatingActionButton(checked = fabOpen, onCheckedChange = { fabOpen = it }) {
                        val icon = if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add
                        Icon(rememberVectorPainter(icon), "Criar", Modifier.animateIcon({ checkedProgress }))
                    }
                },
            ) {
                FloatingActionButtonMenuItem(onClick = { fabOpen = false; newProgram(app) }, icon = { Icon(Icons.Rounded.Folder, null) }, text = { Text("Novo programa") })
                FloatingActionButtonMenuItem(onClick = { fabOpen = false; newRoutine(app, null) }, icon = { Icon(Icons.AutoMirrored.Rounded.ListAlt, null) }, text = { Text("Nova ficha avulsa") })
                FloatingActionButtonMenuItem(onClick = { fabOpen = false; app.go(Route.Templates) }, icon = { Icon(Icons.Rounded.Folder, null) }, text = { Text("Usar um modelo pronto") })
                FloatingActionButtonMenuItem(onClick = { fabOpen = false; app.go(Route.Assistant) }, icon = { Icon(Icons.Rounded.AutoAwesome, null) }, text = { Text("Montar meu treino") })
            }
        },
    ) {
        item {
            Banner(Icons.Rounded.AutoAwesome, "Montar meu treino", "Responda algumas perguntas e escolha entre 3 opções", true) { app.go(Route.Assistant) }
            Banner(Icons.AutoMirrored.Rounded.ListAlt, "Modelos prontos", "PPL, Upper/Lower, ABC, ABCDE, em casa e mais", false) { app.go(Route.Templates) }
            Banner(Icons.Rounded.FitnessCenter, "Exercícios", "${data.allEx().size} exercícios com foto da execução, músculos e sua evolução", false) { app.go(Route.Exercises) }
        }
        if (data.routines.isEmpty() && data.programs.isEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                EmptyState(
                    Icons.AutoMirrored.Rounded.ListAlt, "Nenhuma ficha ainda",
                    "Uma ficha é a lista de exercícios de um dia de treino. Um programa agrupa várias fichas — por exemplo “Meu treino” com Push, Pull e Legs.",
                ) {
                    Button(onClick = { newProgram(app) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(8.dp)); Text("Criar programa")
                    }
                    OutlinedButton(onClick = { newRoutine(app, null) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Criar ficha avulsa")
                    }
                }
            }
            return@Screen
        }
        if (data.programs.isNotEmpty()) {
            item { SectionHeader("Programas") }
            item {
                SegmentedList(data.programs.map { p ->
                    val rs = data.progRoutines(p.id)
                    Seg(
                        key = p.id, headline = p.name.ifBlank { "Sem nome" },
                        supporting = if (rs.isEmpty()) "Nenhuma ficha" else rs.joinToString(" · ") { it.name.ifBlank { "Sem nome" } },
                        leading = {
                            ShapeIcon(
                                Icons.Rounded.Folder, shape = MaterialShapes.Clover4Leaf.toShape(),
                                container = if (p.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                content = if (p.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailing = if (p.active) null else ({ Pill("Pausado") }),
                        onClick = { app.go(Route.Program(p.id)) },
                    )
                })
            }
        }
        val loose = data.routines.filter { data.programOf(it) == null }
        if (loose.isNotEmpty()) {
            item { SectionHeader(if (data.programs.isNotEmpty()) "Fichas avulsas" else "Fichas") }
            item {
                val dow = LocalDate.now().jsDay()
                SegmentedList(loose.mapIndexed { i, r ->
                    Seg(
                        key = r.id, headline = r.name.ifBlank { "Sem nome" }, supporting = "${daysLabel(r.days)} · ${r.items.size} exercícios",
                        leading = { LetterBadge(i, data.isScheduled(r) && dow in r.days) },
                        onClick = { app.go(Route.Routine(r.id)) },
                    )
                })
            }
        }
        item { Hint("Toque num programa para ver as fichas dele. Use o botão + para criar programas e fichas.", center = true) }
    }
}

@Composable
fun ProgramScreen(data: AppData, id: String) {
    val app = LocalApp.current
    val p = data.programs.find { it.id == id }
    if (p == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { app.leave(Route.Program(id)) }
        return
    }
    val rs = data.progRoutines(id)
    val next = data.nextInProgram(p)
    var name by remember(id) { mutableStateOf(p.name) }
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var bringing by remember { mutableStateOf(false) }
    val loose = data.routines.filter { data.programOf(it) == null }
    Screen(
        title = p.name.ifBlank { "Programa" }, subtitle = "Programa", back = true, large = false,
        actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Mais opções") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Duplicar programa") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                    menu = false
                    val (nd, nid) = data.duplicateProgram(id)
                    app.update { nd }
                    app.replace(Route.Program(nid))
                })
                DropdownMenuItem(text = { Text("Excluir programa", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menu = false; deleting = true })
            }
        },
    ) {
        item {
            OutlinedTextField(
                value = name, onValueChange = { v -> name = v.take(60); app.update { it.updateProgram(id) { pp -> pp.copy(name = name) } } },
                label = { Text("Nome do programa") }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            Spacer(Modifier.height(12.dp))
            SegmentedList(listOf(Seg(
                headline = "Programa ativo", supporting = if (p.active) "As fichas aparecem na tela Hoje" else "Pausado — não aparece na tela Hoje",
                trailing = { Switch(checked = p.active, onCheckedChange = { v -> app.update { it.updateProgram(id) { pp -> pp.copy(active = v) } } }) },
                onClick = { app.update { it.updateProgram(id) { pp -> pp.copy(active = !pp.active) } } },
            )))
            if (next != null && next.items.isNotEmpty()) {
                Button(
                    onClick = { app.startWorkout { it.startFromRoutine(next) } }, shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp).height(ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp))
                    Text("Iniciar próxima: ${next.name.ifBlank { "Sem nome" }}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
        item { SectionHeader("Fichas", trailing = rs.size.toString()) }
        item {
            if (rs.isEmpty()) EmptyState(Icons.AutoMirrored.Rounded.ListAlt, "Programa vazio", "Crie as fichas deste programa — por exemplo Push, Pull e Legs, ou A, B e C.")
            else SegmentedList(rs.mapIndexed { i, r ->
                Seg(
                    key = r.id, headline = r.name.ifBlank { "Sem nome" }, supporting = "${daysLabel(r.days)} · ${r.items.size} exercícios",
                    leading = { LetterBadge(i, next?.id == r.id) },
                    trailing = {
                        Row {
                            IconButton(onClick = { app.update { it.moveInProgram(r.id, -1) } }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Subir") }
                            IconButton(onClick = { app.update { it.moveInProgram(r.id, 1) } }, enabled = i < rs.size - 1) { Icon(Icons.Rounded.KeyboardArrowDown, "Descer") }
                        }
                    },
                    onClick = { app.go(Route.Routine(r.id)) },
                )
            })
        }
        item {
            Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { newRoutine(app, id) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Nova ficha neste programa")
                }
                if (loose.isNotEmpty()) OutlinedButton(onClick = { bringing = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(8.dp)); Text("Trazer fichas avulsas")
                }
            }
            Hint(
                if (rs.any { it.days.isNotEmpty() }) "As fichas aparecem na tela Hoje nos dias marcados. Para treinar em sequência (A → B → C…) sem dia fixo, desmarque os dias de todas as fichas."
                else "Sem dias fixos: a tela Hoje sugere a próxima ficha na ordem (A → B → C…), seguindo o último treino feito. Marque dias nas fichas se preferir dias fixos.",
            )
        }
    }
    if (deleting) {
        val n = rs.size
        Sheet({ deleting = false }, "Excluir programa?") {
            Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "“${p.name.ifBlank { "Sem nome" }}” tem $n ficha${if (n == 1) "" else "s"}. Os treinos já registrados continuam no histórico.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp),
                )
                if (n > 0) OutlinedButton(onClick = {
                    deleting = false
                    app.back()
                    app.update { d -> d.copy(routines = d.routines.map { if (it.programId == id) it.copy(programId = null) else it }, programs = d.programs.filter { it.id != id }) }
                }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Excluir e manter as fichas como avulsas") }
                Button(onClick = {
                    deleting = false
                    app.back()
                    app.update { d -> d.copy(routines = d.routines.filter { it.programId != id }, programs = d.programs.filter { it.id != id }) }
                }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError), modifier = Modifier.fillMaxWidth()) {
                    Text(if (n > 0) "Excluir programa e fichas" else "Excluir programa")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (bringing) {
        Sheet({ bringing = false }, "Trazer fichas avulsas") {
            val list = data.routines.filter { data.programOf(it) == null }
            if (list.isEmpty()) bringing = false
            SegmentedList(list.map { r ->
                Seg(
                    key = r.id, headline = r.name.ifBlank { "Sem nome" }, supporting = "${daysLabel(r.days)} · ${r.items.size} exercícios",
                    trailing = { Pill("Mover", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = {
                        // vai para o fim da lista para ficar por último no programa
                        app.update { d -> d.copy(routines = d.routines.filter { it.id != r.id } + r.copy(programId = id)) }
                        app.toast("Ficha movida")
                    },
                )
            }, Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp))
        }
    }
}

/* ================= Modelos prontos ================= */

private val TPL_FILTERS = listOf("" to "Todos", "Iniciante" to "Iniciante", "Intermediário" to "Intermediário", "Avançado" to "Avançado", "casa" to "Em casa")

@Composable
fun TemplatesScreen(data: AppData) {
    val app = LocalApp.current
    var filter by rememberSaveable { mutableStateOf("") }
    val list = Catalog.templates.filter { filter.isEmpty() || it.level == filter || it.place == filter }
    Screen(title = "Modelos prontos", subtitle = "Escolha, veja as fichas e adicione aos seus", back = true) {
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TPL_FILTERS) { (v, l) -> FilterChip(selected = filter == v, onClick = { filter = v }, label = { Text(l) }) }
            }
        }
        list.forEach { t ->
            item(key = t.id) {
                Surface(
                    onClick = { app.go(Route.Template(t.id)) }, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(t.level, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            Pill(t.freq)
                            if (t.place == "casa") Pill("Em casa", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(t.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            t.routines.forEachIndexed { i, r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ShapeIcon(text = letter(i), size = 26.dp, shape = MaterialShapes.Cookie6Sided.toShape())
                                    Spacer(Modifier.width(6.dp))
                                    Text(r.name, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (list.isEmpty()) item { EmptyState(Icons.AutoMirrored.Rounded.ListAlt, "Nenhum modelo", "Tente outro filtro.") }
    }
}

@Composable
fun TemplateScreen(data: AppData, id: String) {
    val app = LocalApp.current
    val t = Catalog.templates.find { it.id == id } ?: return
    var howTo by remember { mutableStateOf<String?>(null) }
    var using by remember { mutableStateOf(false) }
    Screen(
        title = t.name, subtitle = "${t.level} · ${t.freq}", back = true,
        fab = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { using = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Usar este programa") },
            )
        },
    ) {
        item { Text(t.desc, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Gutter + 4.dp)) }
        t.routines.forEachIndexed { i, r ->
            item { SectionHeader("${letter(i)} · ${r.name}", trailing = if (r.days.isNotEmpty()) daysLabel(r.days) else "Sem dia fixo") }
            item {
                SegmentedList(r.items.map { it ->
                    val ex = data.ex(it.exId)
                    Seg(
                        headline = data.exName(it.exId), supporting = "${it.sets} × ${it.reps}${targetUnit(data.exKind(it.exId))}",
                        leading = { ExerciseThumb(ex, 48.dp) },
                        onClick = { howTo = it.exId },
                    )
                })
            }
        }
    }
    howTo?.let { HowToSheet(data, it, { howTo = null }) }
    if (using) UseProgramSheet(data, t.name, t.routines, onDismiss = { using = false })
}

/** Confirmação ao criar um programa a partir de um modelo (ou do assistente). */
@Composable
fun UseProgramSheet(data: AppData, name: String, defs: List<TemplateRoutine>, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val hasDays = defs.any { it.days.isNotEmpty() }
    val othersActive = data.programs.count { it.active }
    var useDays by remember { mutableStateOf(true) }
    var only by remember { mutableStateOf(true) }
    Sheet(onDismiss, "Usar “$name”") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(
                "Cria o programa com ${defs.size} ficha${if (defs.size > 1) "s" else ""}. Depois você pode trocar exercícios, séries e dias.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            val rows = buildList {
                if (hasDays) add(Seg(
                    headline = "Usar os dias sugeridos",
                    supporting = defs.joinToString(" · ") { "${it.name.substringBefore(" — ")}: ${if (it.days.isNotEmpty()) daysLabel(it.days) else "livre"}" },
                    trailing = { Switch(useDays, { useDays = it }) }, onClick = { useDays = !useDays },
                ))
                if (othersActive > 0) add(Seg(
                    headline = "Pausar os outros programas", supporting = "A tela Hoje passa a mostrar só este programa",
                    trailing = { Switch(only, { only = it }) }, onClick = { only = !only },
                ))
            }
            if (rows.isNotEmpty()) SegmentedList(rows, Modifier)
            if (hasDays) Hint("Sem dias fixos, o app sugere a próxima ficha na ordem a cada treino.")
            Button(onClick = {
                val (nd, pid) = app.data.createProgram(name, defs, useDays || !hasDays, only && othersActive > 0)
                app.update { nd }
                onDismiss()
                app.toast("Programa criado — ajuste como quiser")
                app.go(Route.Program(pid))
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(ButtonDefaults.MediumContainerHeight)) {
                Text("Criar programa", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
