package app.ficha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.BodyEntry
import app.ficha.data.BodyGoal
import app.ficha.data.Catalog
import app.ficha.data.Profile
import app.ficha.data.uid
import app.ficha.logic.BODY_CHART_ORDER
import app.ficha.logic.BODY_FIELDS
import app.ficha.logic.BODY_RANGES
import app.ficha.logic.BODY_SHORT
import app.ficha.logic.DAY_MS
import app.ficha.logic.betterOf
import app.ficha.logic.bmeta
import app.ficha.logic.bodyEntries
import app.ficha.logic.bodyHeight
import app.ficha.logic.bval
import app.ficha.logic.dateLong
import app.ficha.logic.dateOf
import app.ficha.logic.dateShort
import app.ficha.logic.deltaGood
import app.ficha.logic.earliestOf
import app.ficha.logic.fmt
import app.ficha.logic.fmtDelta
import app.ficha.logic.fmtIn
import app.ficha.logic.imcLabel
import app.ficha.logic.latestOf
import app.ficha.logic.millis
import app.ficha.logic.muscleLoad
import app.ficha.logic.num
import app.ficha.logic.relDay
import app.ficha.logic.sex
import app.ficha.logic.startOfDay
import app.ficha.ui.LocalApp
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ChartPoint
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.LineChart
import app.ficha.ui.components.MuscleHeat
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.ShapeIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun BodyScreen(data: AppData) {
    val app = LocalApp.current
    var metric by rememberSaveable { mutableStateOf("peso") }
    var range by rememberSaveable { mutableStateOf("all") }
    var limit by rememberSaveable { mutableIntStateOf(30) }
    var form by remember { mutableStateOf<Pair<Boolean, BodyEntry?>?>(null) }
    val all = data.bodyEntries()
    val openNew = { form = true to data.body.find { it.t == startOfDay(System.currentTimeMillis()) } }
    val goal = data.bodyGoal.peso
    val editGoal = {
        app.prompt("Meta de peso (kg)", goal?.let { fmtIn(it) } ?: "", "Meta de peso", "Deixe vazio para remover") { v ->
            val n = num(v)
            app.update { it.copy(bodyGoal = BodyGoal(if (n != null && n > 0) n else null)) }
        }
    }
    Screen(title = "Corpo", actions = { IconButton(onClick = openNew) { Icon(Icons.Rounded.Add, "Registrar medidas") } }) {
        if (all.isEmpty()) item {
            EmptyState(Icons.Rounded.Accessibility, "Acompanhe sua evolução", "Registre peso, medidas e composição corporal (gordura, massa muscular…) de tempos em tempos. Os gráficos mostram a sua evolução.") {
                Button(onClick = openNew, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Registrar medidas") }
            }
        } else {
            val last = all.last()
            val first = all.first()
            item {
                Text(
                    "Último registro: ${if (relDay(last.t) == "Hoje") "hoje" else dateShort(last.t)}" + if (all.size > 1) " · variação desde ${dateShort(first.t)}" else "",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Gutter + 4.dp, vertical = 4.dp),
                )
                val rows = listOf("peso", "gordura", "navy", "musculo", "magra", "imc", "rcq").mapNotNull { k ->
                    val L = data.latestOf(k, all) ?: return@mapNotNull null
                    if (k == "navy" && data.latestOf("gordura", all) != null) return@mapNotNull null
                    val m = bmeta(k)
                    val d = L.first - (data.earliestOf(k, all) ?: L.first)
                    Seg(
                        headline = m.name + if (k == "navy") " (pelas medidas)" else "",
                        supporting = if (k == "imc") imcLabel(L.first) else null,
                        trailing = { ValueDelta("${fmt(L.first, m.dec)}${if (m.unit.isNotEmpty()) " " + m.unit else ""}", if (all.size > 1) d else null, m.dec, data.deltaGood(k, d)) },
                        onClick = { metric = k },
                    )
                } + Seg(
                    headline = "Meta de peso",
                    trailing = {
                        if (goal != null) {
                            val lw = data.latestOf("peso", all)?.first
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${fmt(goal)} kg", style = MaterialTheme.typography.titleSmallEmphasized)
                                if (lw != null) Text("faltam ${fmt(kotlin.math.abs(lw - goal))} kg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else Text("Definir", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    },
                    onClick = editGoal,
                )
                SegmentedList(rows)
            }
            // Gráfico
            val avail = BODY_CHART_ORDER.filter { k -> all.any { data.bval(k, it) != null } }
            val cur = if (metric in avail) metric else avail.firstOrNull() ?: "peso"
            item { SectionHeader("Evolução") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(avail) { k -> FilterChip(selected = k == cur, onClick = { metric = k }, label = { Text(bmeta(k).name) }) }
                }
                Spacer(Modifier.height(8.dp))
                val m = bmeta(cur)
                val since = if (range == "all") 0L else startOfDay(System.currentTimeMillis()) - range.toLong() * DAY_MS
                val pts = all.filter { it.t >= since }.mapNotNull { e -> data.bval(cur, e)?.let { ChartPoint(e.t, it) } }
                CardBox {
                    ConnectedChoice(BODY_RANGES, range) { range = it }
                    Spacer(Modifier.height(12.dp))
                    LineChart(
                        pts, m.unit, goal = if (cur == "peso") goal else null, better = data.betterOf(cur), dec = m.dec, maxPoints = 200, byTime = true,
                        empty = if (pts.isNotEmpty()) "Registre mais uma vez para ver a linha de evolução." else "Sem registros neste período.",
                    )
                }
                if (cur == "navy") Hint("Estimativa pelo método da Marinha dos EUA (pescoço, cintura${if (data.sex() == "f") ", quadril" else ""} e altura). Serve para comparar a sua própria evolução.")
            }
            // Medidas
            val meds = BODY_FIELDS.filter { it.section == "med" && data.latestOf(it.key, all) != null }
            if (meds.isNotEmpty()) {
                item { SectionHeader("Medidas") }
                item {
                    SegmentedList(meds.map { f ->
                        val L = data.latestOf(f.key, all)!!
                        val d = L.first - (data.earliestOf(f.key, all) ?: L.first)
                        Seg(headline = f.name, trailing = { ValueDelta("${fmt(L.first, f.dec)} ${f.unit}", if (all.size > 1) d else null, f.dec, data.deltaGood(f.key, d)) }, onClick = { metric = f.key })
                    })
                }
            }
        }
        item { PhotosCorpoSection(data) }
        item { SectionHeader("Músculos treinados", trailing = "últimos 7 dias") }
        item {
            val load = data.muscleLoad(7)
            CardBox {
                MuscleHeat(load)
                val top = load.entries.sortedByDescending { it.value }
                if (top.isEmpty()) Text("Nenhum treino nos últimos 7 dias.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
                    top.forEach { (c, v) ->
                        Pill(
                            "${Catalog.muscles[c] ?: c} ${fmt(v, 1)}",
                            if (v >= 5) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            if (v >= 5) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (all.isNotEmpty()) {
            item { SectionHeader("Registros", trailing = all.size.toString()) }
            item {
                SegmentedList(all.reversed().take(limit).map { e ->
                    val parts = listOfNotNull(
                        e["peso"]?.let { "${fmt(it)} kg" }, e["gordura"]?.let { "${fmt(it)}% gordura" },
                        e["musculo"]?.let { "${fmt(it)} kg músculo" }, e["cintura"]?.let { "cintura ${fmt(it)}" },
                    )
                    val nMed = BODY_FIELDS.count { it.section == "med" && e[it.key] != null }
                    Seg(
                        key = e.id, headline = dateLong(e.t),
                        supporting = (if (parts.isNotEmpty()) parts.joinToString(" · ") else "$nMed medidas") + if (nMed > 0 && parts.isNotEmpty()) " · $nMed medidas" else "",
                        leading = { ShapeIcon(text = dateOf(e.t).dayOfMonth.toString(), shape = MaterialShapes.Cookie6Sided.toShape()) },
                        onClick = { form = false to e },
                    )
                })
                if (all.size > limit) FilledTonalButton(onClick = { limit += 60 }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(Gutter)) {
                    Text("Mostrar registros mais antigos (${all.size - limit})")
                }
            }
        }
        item { Hint("Dica: meça sempre nas mesmas condições — de manhã, em jejum e depois de ir ao banheiro. Variações de 1–2 kg de um dia para o outro são normais (água e alimentação).") }
    }
    form?.let { (_, e) -> BodyFormSheet(data, e) { form = null } }
}

/** Valor com a variação desde o primeiro registro (verde se melhorou, vermelho se piorou). */
@Composable
fun ValueDelta(value: String, delta: Double?, dec: Int, good: Boolean?) {
    Column(horizontalAlignment = Alignment.End) {
        Text(value, style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum"))
        if (delta != null) Text(
            fmtDelta(delta, dec), style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = when (good) { true -> app.ficha.ui.theme.LocalFichaColors.current.success; false -> MaterialTheme.colorScheme.error; null -> MaterialTheme.colorScheme.onSurfaceVariant },
        )
    }
}

fun LocalDate.toMillisUtc() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun utcMillisToLocal(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

/** Seletor de data (até hoje). */
@Composable
fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    // O campo de texto consome o toque; abre o calendário ao soltar o dedo nele
    val src = remember { MutableInteractionSource() }
    LaunchedEffect(src) { src.interactions.collect { if (it is PressInteraction.Release) open = true } }
    OutlinedTextField(
        dateLong(date.millis()), {}, readOnly = true, label = { Text(label) },
        trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Rounded.CalendarMonth, "Escolher data") } },
        interactionSource = src, modifier = Modifier.fillMaxWidth(),
    )
    if (open) {
        val today = LocalDate.now()
        val st = rememberDatePickerState(
            initialSelectedDateMillis = date.toMillisUtc(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !utcMillisToLocal(utcTimeMillis).isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { st.selectedDateMillis?.let { onChange(utcMillisToLocal(it)) }; open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(st) }
    }
}

@Composable
private fun BodyFormSheet(data: AppData, entry: BodyEntry?, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val vals = remember { mutableStateMapOf<String, String>().apply { entry?.v?.forEach { (k, v) -> put(k, fmtIn(v)) } } }
    var date by remember { mutableStateOf(entry?.t?.let { dateOf(it) } ?: LocalDate.now()) }
    var nota by remember { mutableStateOf(entry?.nota ?: "") }
    val pr = data.profile ?: Profile()
    var altura by remember { mutableStateOf(data.bodyHeight()?.let { fmt(it, 0) } ?: "") }
    var sexo by remember { mutableStateOf(pr.sexo) }
    val needWho = data.bodyHeight() == null || pr.sexo.isEmpty()
    var how by remember { mutableStateOf(false) }
    Sheet(onDismiss, if (entry != null) "Editar registro" else "Registrar medidas") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            DateField("Data", date) { date = it }
            if (needWho) {
                Spacer(Modifier.height(12.dp))
                CardBox(Modifier.padding(horizontal = 0.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Text("Altura e sexo servem para calcular o IMC e estimar a gordura pelas medidas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(altura, { altura = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, label = { Text("Altura (cm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    ConnectedChoice(listOf("f" to "Feminino", "m" to "Masculino"), sexo) { sexo = it }
                }
            }
            @Composable
            fun grid(section: String) {
                BODY_FIELDS.filter { it.section == section }.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { f ->
                            val unit = if (f.unit.isNotEmpty() && f.unit != "nível" && f.unit != "cm") " (${f.unit})" else ""
                            OutlinedTextField(
                                vals[f.key] ?: "", { v -> vals[f.key] = v.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                                label = { Text((BODY_SHORT[f.key] ?: f.name) + unit, maxLines = 1) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Label2("Peso e composição")
            grid("comp")
            Label2("Medidas (cm)")
            grid("med")
            TextButton(onClick = { how = !how }) {
                Text("Como medir"); Icon(if (how) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            AnimatedVisibility(how) {
                Column {
                    BODY_FIELDS.filter { it.tip.isNotEmpty() }.forEach { f ->
                        Text("• ${f.name}: ${f.tip}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Text("Use fita métrica sem apertar a pele e meça sempre do mesmo lado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
            OutlinedTextField(nota, { nota = it.take(140) }, label = { Text("Observação") }, placeholder = { Text("Ex.: depois de uma semana de dieta") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Button(onClick = {
                val v = vals.mapNotNull { (k, s) -> num(s)?.takeIf { it >= 0 }?.let { k to it } }.toMap()
                if (v.isEmpty()) { app.toast("Preencha pelo menos uma medida"); return@Button }
                val t = date.millis()
                app.update { d ->
                    var p = d.profile ?: Profile()
                    num(altura)?.let { p = p.copy(altura = fmtIn(it)) }
                    if (sexo.isNotEmpty()) p = p.copy(sexo = sexo)
                    val same = d.body.find { it.t == t && it.id != entry?.id }
                    val body = when {
                        // Junta com o registro que já existe nesse dia
                        same != null -> d.body.filter { it.id != entry?.id }.map { if (it === same) it.copy(v = it.v + v, nota = nota.ifBlank { it.nota }) else it }
                        entry != null -> d.body.map { if (it.id == entry.id) BodyEntry(entry.id, t, nota.ifBlank { null }, v) else it }
                        else -> d.body + BodyEntry(uid(), t, nota.ifBlank { null }, v)
                    }
                    d.copy(profile = if (needWho) p else d.profile, body = body)
                }
                app.toast("Medidas salvas")
                onDismiss()
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) { Text("Salvar", style = MaterialTheme.typography.titleMedium) }
            if (entry != null) OutlinedButton(onClick = {
                app.confirm("Excluir registro?", "As medidas deste dia saem dos gráficos.", "Excluir", danger = true) {
                    app.update { d -> d.copy(body = d.body.filter { it.id != entry.id }) }
                    onDismiss()
                }
            }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Excluir registro")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
