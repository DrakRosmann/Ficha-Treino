package app.ficha.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.Routine
import app.ficha.logic.DAY
import app.ficha.logic.DAY_MS
import app.ficha.logic.addDays
import app.ficha.logic.backupDue
import app.ficha.logic.clock
import app.ficha.logic.dateLong
import app.ficha.logic.dateOf
import app.ficha.logic.daysLabel
import app.ficha.logic.exKind
import app.ficha.logic.exName
import app.ficha.logic.fmtDur
import app.ficha.logic.fmtInt
import app.ficha.logic.isScheduled
import app.ficha.logic.jsDay
import app.ficha.logic.millis
import app.ficha.logic.nextInProgram
import app.ficha.logic.programOf
import app.ficha.logic.progRoutines
import app.ficha.logic.relDay
import app.ficha.logic.sessionsInRange
import app.ficha.logic.setCount
import app.ficha.logic.startFromRoutine
import app.ficha.logic.startOfDay
import app.ficha.logic.startOfWeek
import app.ficha.logic.startWorkout
import app.ficha.logic.stats
import app.ficha.logic.streakWeeks
import app.ficha.logic.targetUnit
import app.ficha.logic.unbackedSessions
import app.ficha.logic.volume
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.StatRow
import app.ficha.ui.rememberNow
import java.time.LocalDate

@Composable
fun TodayScreen(data: AppData) {
    val app = LocalApp.current
    val now = System.currentTimeMillis()
    val dow = LocalDate.now().jsDay()
    val sched = data.routines.filter { data.isScheduled(it) }
    val today = sched.filter { dow in it.days }
    // Programas ativos sem dia fixo: sugere a próxima ficha na ordem
    val seq = data.programs.filter { it.active }
        .map { it to data.progRoutines(it.id) }
        .filter { (_, rs) -> rs.isNotEmpty() && rs.all { r -> r.days.isEmpty() } }
        .mapNotNull { (p, _) -> data.nextInProgram(p)?.let { p to it } }
    val heroIds = (today.map { it.id } + seq.map { it.second.id }).toSet()
    val others = sched.filter { it.id !in heroIds }
    val doneToday = data.sessionsInRange(startOfDay(now), addDays(startOfDay(now), 1))
    val title = when {
        data.active != null -> "Treino em andamento"
        data.routines.isEmpty() -> "Vamos montar seu treino"
        doneToday.isNotEmpty() -> "Treino feito hoje"
        today.isNotEmpty() || seq.isNotEmpty() -> "Dia de treinar"
        else -> "Dia de descanso"
    }
    val start = { r: Routine ->
        if (r.items.isEmpty()) app.toast("Adicione exercícios à ficha primeiro")
        else app.startWorkout { it.startFromRoutine(r) }
    }

    Screen(title = title, subtitle = dateLong(now)) {
        item { WeekStrip(data, sched) }

        val a = data.active
        if (a != null) item {
            val t by rememberNow(1000)
            val st = a.stats()
            HeroCard(label = "EM ANDAMENTO · ${clock((t - a.start) / 1000.0)}", title = a.name) {
                Text("${st.done} de ${st.total} séries concluídas", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                LinearWavyProgressIndicator(progress = { st.p }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(onClick = { app.go(Route.Workout) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continuar treino", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else if (data.routines.isEmpty()) item {
            Spacer(Modifier.height(8.dp))
            EmptyState(
                Icons.AutoMirrored.Rounded.ListAlt, "Nenhuma ficha ainda",
                "Crie suas fichas de treino escolhendo os exercícios de cada dia, ou comece com um programa pronto (PPL, Upper/Lower, ABC…) e ajuste do seu jeito.",
            ) {
                Button(onClick = { app.go(Route.Assistant) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Montar meu treino")
                }
                OutlinedButton(onClick = { app.go(Route.Templates) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Rounded.ListAlt, null); Spacer(Modifier.width(8.dp)); Text("Ver modelos prontos")
                }
                OutlinedButton(onClick = { app.tab(Route.Routines) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Criar minha ficha")
                }
            }
        } else {
            today.forEach { r ->
                val p = data.programOf(r)
                item(key = "today-${r.id}") {
                    RoutineHero(data, r, done = doneToday.any { it.routineId == r.id }, label = "FICHA DE HOJE" + (p?.let { " · " + it.name.uppercase() } ?: ""), onStart = { start(r) })
                }
            }
            seq.forEach { (p, r) ->
                item(key = "seq-${r.id}") { RoutineHero(data, r, done = false, label = "PRÓXIMA FICHA · ${p.name.uppercase()}", onStart = { start(r) }) }
            }
        }

        if (data.sessions.isNotEmpty() || data.routines.isNotEmpty()) item {
            val wk = startOfWeek(now)
            val monthStart = LocalDate.now().withDayOfMonth(1).millis()
            Spacer(Modifier.height(12.dp))
            StatRow(
                listOf(
                    data.sessionsInRange(wk, addDays(wk, 7)).size.toString() to "nesta semana",
                    data.sessions.count { it.start >= monthStart }.toString() to "neste mês",
                    data.streakWeeks(now).toString() to "semanas seguidas",
                ),
            )
        }

        if (a == null && data.routines.isNotEmpty()) {
            if (others.isNotEmpty()) {
                item { SectionHeader(if (today.isNotEmpty() || seq.isNotEmpty()) "Outras fichas" else "Escolha uma ficha") }
                item {
                    SegmentedList(others.map { r ->
                        val p = data.programOf(r)
                        Seg(
                            key = r.id, headline = r.name.ifBlank { "Sem nome" },
                            supporting = (p?.let { it.name + " · " } ?: "") + "${daysLabel(r.days)} · ${r.items.size} exercícios",
                            trailing = { Pill("Iniciar", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) },
                            onClick = { start(r) },
                        )
                    })
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { app.startWorkout { it.startWorkout("Treino livre", null, emptyList()) } }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Treino livre")
                    }
                    FilledTonalButton(onClick = { app.tab(Route.Routines) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.ListAlt, null); Spacer(Modifier.width(6.dp)); Text("Ver fichas")
                    }
                }
            }
        }

        item { DietTodayCard(data) { app.tab(Route.Diet) } }
        if (a == null) item { CoachTodayBanner(data) }

        if (a == null && data.backupDue()) item {
            val n = data.unbackedSessions()
            Spacer(Modifier.height(8.dp))
            CardBox(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Faça um backup", style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            "$n treino${if (n > 1) "s ainda não estão salvos" else " ainda não está salvo"} fora deste aparelho.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { app.tab(Route.Settings) }, shapes = ButtonDefaults.shapes()) { Text("Salvar backup") }
                    OutlinedButton(onClick = {
                        app.update { it.copy(settings = it.settings.copy(backupSnooze = System.currentTimeMillis() + 3 * DAY_MS)) }
                    }, shapes = ButtonDefaults.shapes()) { Text("Depois") }
                }
            }
        }

        data.sessions.firstOrNull()?.let { last ->
            item { SectionHeader("Último treino") }
            item {
                val vol = last.volume()
                SegmentedList(listOf(Seg(
                    key = last.id, headline = last.name,
                    supporting = "${relDay(last.start)} · ${fmtDur(last.end - last.start)} · ${last.setCount()} séries" + if (vol > 0) " · ${fmtInt(vol)} kg" else "",
                    onClick = { app.go(Route.Session(last.id)) },
                )))
            }
        }
    }
}

/** A semana (segunda a domingo) com os dias planejados, treinados e o dia de hoje. */
@Composable
private fun WeekStrip(data: AppData, sched: List<Routine>) {
    val now = System.currentTimeMillis()
    val wk = startOfWeek(now)
    val todayStart = startOfDay(now)
    val cookie = MaterialShapes.Cookie6Sided.toShape()
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val t = addDays(wk, i)
            val d = dateOf(t)
            val plan = sched.any { d.jsDay() in it.days }
            val done = data.sessionsInRange(t, addDays(t, 1)).isNotEmpty()
            val isToday = t == todayStart
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    DAY[d.jsDay()], style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(6.dp))
                val container = when {
                    done -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                }
                val content = when {
                    done -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Surface(color = container, contentColor = content, shape = if (done) cookie else CircleShape, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
                        else Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmallEmphasized)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Surface(color = if (plan) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, shape = CircleShape, modifier = Modifier.size(5.dp)) {}
            }
        }
    }
}

@Composable
fun HeroCard(label: String, title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun RoutineHero(data: AppData, r: Routine, done: Boolean, label: String, onStart: () -> Unit) {
    val app = LocalApp.current
    HeroCard(label = if (done) "CONCLUÍDO HOJE ✓" else label, title = r.name.ifBlank { "Sem nome" }) {
        if (r.items.isEmpty()) Text("Ficha vazia — adicione exercícios.", style = MaterialTheme.typography.bodyMedium)
        r.items.take(6).forEach { it ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(data.exName(it.exId), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Text("${it.sets} × ${it.reps.ifBlank { "—" }}${targetUnit(data.exKind(it.exId)).trim().let { u -> if (u.isEmpty()) "" else " $u" }}", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
            }
        }
        val n = r.items.size - 6
        if (n > 0) Text("+ $n exercício${if (n > 1) "s" else ""}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (r.items.isNotEmpty()) Button(
                onClick = onStart, shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f).height(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (done) "Treinar de novo" else "Iniciar treino", style = MaterialTheme.typography.titleMedium)
            }
            else Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { app.go(Route.Routine(r.id)) }, shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(ButtonDefaults.MediumContainerHeight),
            ) { Icon(Icons.Rounded.Edit, "Editar ficha") }
        }
    }
}
