package app.ficha.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ficha.ai.AiError
import app.ficha.ai.AssistantAI
import app.ficha.data.AppData
import app.ficha.data.Profile
import app.ficha.logic.ASST_OPTS
import app.ficha.logic.AsstOption
import app.ficha.logic.asstBmi
import app.ficha.logic.asstGenerateLocal
import app.ficha.logic.daysLabel
import app.ficha.logic.ex
import app.ficha.logic.exKind
import app.ficha.logic.exName
import app.ficha.logic.fmt
import app.ficha.logic.fmtRest
import app.ficha.logic.letter
import app.ficha.logic.optLabel
import app.ficha.logic.targetUnit
import app.ficha.ui.LocalApp
import app.ficha.ui.components.AiBusyDialog
import app.ficha.ui.components.AiKeySheet
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ExerciseThumb
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.aiReady
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Resultado do assistente (fica enquanto o app está aberto, como no PWA). */
private object AsstState {
    var opts by mutableStateOf<List<AsstOption>>(emptyList())
    var source by mutableStateOf("local")
    var notes by mutableStateOf("")
    var seed by mutableIntStateOf(0)
    var dropped by mutableIntStateOf(0)
    var results by mutableStateOf(false)
}

private const val DISCLAIMER = "Sugestão automática: não substitui a avaliação de um profissional de educação física. Com dor, lesão ou condição de saúde, procure orientação médica antes de treinar."

@Composable
fun AssistantScreen(data: AppData) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = data.profile ?: Profile()
    var keySheet by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf<Job?>(null) }
    var phase by remember { mutableStateOf("Preparando…") }
    var secs by remember { mutableIntStateOf(0) }
    var use by remember { mutableStateOf<AsstOption?>(null) }
    var howTo by remember { mutableStateOf<String?>(null) }
    val setP = { f: (Profile) -> Profile -> app.update { it.copy(profile = f(it.profile ?: Profile())) } }

    fun local() {
        AsstState.opts = app.data.asstGenerateLocal(app.data.profile ?: Profile(), 0)
        AsstState.source = "local"; AsstState.notes = ""; AsstState.seed = 0; AsstState.dropped = 0; AsstState.results = true
    }

    fun ai() {
        val k = aiReady(context, app::toast) { keySheet = true } ?: return
        phase = "Preparando…"
        secs = 0
        running = scope.launch {
            val tick = launch { while (true) { delay(1000); secs++ } }
            try {
                val r = AssistantAI.generate(k, app.data, app.data.profile ?: Profile()) { thinking, chars ->
                    phase = if (thinking) "Analisando seu perfil e escolhendo a divisão…" else "Escrevendo as fichas… ($chars caracteres)"
                }
                AsstState.opts = r.opts; AsstState.source = "ai"; AsstState.notes = r.notes; AsstState.dropped = r.dropped; AsstState.results = true
            } catch (e: Exception) {
                if (e !is CancellationException) app.confirm("Não deu certo", (e as? AiError)?.message ?: "Não foi possível usar a IA agora", "Ver opções sem IA") { local() }
            } finally {
                tick.cancel()
                running = null
            }
        }
    }

    if (AsstState.results && AsstState.opts.isNotEmpty()) {
        val bmi = asstBmi(p)
        Screen(
            title = "Suas opções", subtitle = if (AsstState.source == "ai") "Montado com IA (Claude)" else "Assistente de treino", back = true, large = false,
            actions = { TextButton(onClick = { AsstState.results = false }) { Text("Ajustar respostas") } },
        ) {
            item {
                Hint(listOfNotNull(optLabel("objetivo", p.objetivo), optLabel("nivel", p.nivel), "${p.dias}x por semana", "${p.tempo} min", optLabel("local", p.local), bmi?.let { "IMC ${fmt(it)}" }).joinToString(" · "))
                if (AsstState.source == "ai" && AsstState.notes.isNotBlank()) CardBox(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Recomendações", style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(AsstState.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (AsstState.dropped > 0) Hint("${AsstState.dropped} exercício(s) sugerido(s) pela IA não estavam no catálogo e foram removidos.")
            }
            AsstState.opts.forEachIndexed { i, op -> item(key = "op$i-${op.name}") { OptionCard(data, op, i, onUse = { use = op }, onHowTo = { howTo = it }) } }
            item {
                Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (AsstState.source == "local") {
                        FilledTonalButton(onClick = {
                            AsstState.seed++
                            AsstState.opts = app.data.asstGenerateLocal(app.data.profile ?: Profile(), AsstState.seed)
                            app.toast("Nova variação de exercícios")
                        }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Outra variação de exercícios") }
                        FilledTonalButton(onClick = { ai() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Montar com IA (Claude)") }
                    } else {
                        FilledTonalButton(onClick = { ai() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Gerar de novo com IA") }
                        FilledTonalButton(onClick = { local() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Rounded.ListAlt, null); Spacer(Modifier.width(8.dp)); Text("Ver opções sem IA") }
                    }
                }
                Hint(DISCLAIMER)
            }
        }
    } else {
        Screen(title = "Montar meu treino", subtitle = "Assistente de treino", back = true) {
            item { Hint("Responda e receba 3 opções de programa para escolher. Depois dá para mudar tudo.") }
            item {
                CardBox {
                    ChipField("Objetivo", "objetivo", listOf(p.objetivo)) { v -> setP { it.copy(objetivo = v) } }
                    ChipField("Experiência com treino", "nivel", listOf(p.nivel)) { v -> setP { it.copy(nivel = v) } }
                    ChipField("Dias por semana", "dias", listOf(p.dias.toString())) { v -> setP { it.copy(dias = v.toInt()) } }
                    ChipField("Tempo por treino (minutos)", "tempo", listOf(p.tempo.toString())) { v -> setP { it.copy(tempo = v.toInt()) } }
                    ChipField("Onde vai treinar", "local", listOf(p.local)) { v -> setP { it.copy(local = v) } }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                CardBox {
                    Text("Sobre você (opcional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileField("Peso (kg)", p.peso, Modifier.weight(1f)) { v -> setP { it.copy(peso = v) } }
                        ProfileField("Altura (cm)", p.altura, Modifier.weight(1f)) { v -> setP { it.copy(altura = v) } }
                        ProfileField("Idade", p.idade, Modifier.weight(1f)) { v -> setP { it.copy(idade = v) } }
                    }
                    asstBmi(p)?.let { Text("IMC ${fmt(it)} · usado só para escolher exercícios mais seguros", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                    ChipField("Sexo", "sexo", listOf(p.sexo)) { v -> setP { it.copy(sexo = v) } }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                CardBox {
                    ChipField("Quer dar prioridade a algum grupo?", "foco", p.foco) { v -> setP { it.copy(foco = if (v in it.foco) it.foco - v else it.foco + v) } }
                    ChipField("Restrições ou dores", "restr", p.restr) { v -> setP { it.copy(restr = if (v in it.restr) it.restr - v else it.restr + v) } }
                    var obs by remember { mutableStateOf(p.obs) }
                    OutlinedTextField(
                        obs, { v -> obs = v.take(600); setP { it.copy(obs = obs) } }, label = { Text("Outras observações (usadas pela IA)") },
                        placeholder = { Text("Ex.: hérnia de disco, já treino há 1 ano, prefiro máquinas, não gosto de agachamento…") },
                        minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
            item {
                Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { local() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                        Icon(Icons.AutoMirrored.Rounded.ListAlt, null); Spacer(Modifier.width(8.dp)); Text("Ver opções", style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(onClick = { ai() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Montar com IA (Claude)")
                    }
                }
                Hint("“Ver opções” usa regras de treino, funciona sem internet e é grátis. “Montar com IA” usa o Claude, entende as suas observações em texto livre e precisa de internet e de uma chave da API da Anthropic (paga por uso).")
                Hint(DISCLAIMER)
            }
        }
    }
    if (keySheet) AiKeySheet({ keySheet = false }) { ai() }
    running?.let { job -> AiBusyDialog("Montando seu treino com IA", "$phase\n$secs s") { job.cancel(); running = null } }
    use?.let { op -> UseProgramSheet(data, op.programName, op.sessions) { use = null } }
    howTo?.let { HowToSheet(data, it, { howTo = null }) }
}

@Composable
private fun ChipField(label: String, field: String, selected: List<String>, onToggle: (String) -> Unit) {
    Label2(label)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ASST_OPTS.getValue(field).forEach { (v, l) -> FilterChip(selected = v in selected, onClick = { onToggle(v) }, label = { Text(l) }) }
    }
}

@Composable
private fun ProfileField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    var v by remember { mutableStateOf(value) }
    OutlinedTextField(
        v, { x -> v = x.filter { it.isDigit() || it == ',' || it == '.' }.take(5); onChange(v) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
    )
}

@Composable
private fun OptionCard(data: AppData, op: AsstOption, i: Int, onUse: () -> Unit, onHowTo: (String) -> Unit) {
    var open by remember { mutableStateOf(i == 0) }
    Surface(
        color = if (i == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (i == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (i == 0) Pill("Recomendada", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) else Pill("Opção ${i + 1}")
                Spacer(Modifier.width(8.dp))
                Text(op.sub, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }
            Text(op.name, style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.padding(vertical = 8.dp))
            op.why.forEach { w -> Text("• $w", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
            TextButton(onClick = { open = !open }, colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)) {
                Text("Ver as ${op.sessions.size} fichas"); Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            AnimatedVisibility(open) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    op.sessions.forEachIndexed { k, s ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface, shape = MaterialTheme.shapes.large) {
                            Column(Modifier.padding(12.dp)) {
                                Row {
                                    Text("${letter(k)} · ${s.name}", style = MaterialTheme.typography.titleSmallEmphasized, modifier = Modifier.weight(1f))
                                    Text(if (s.days.isNotEmpty()) daysLabel(s.days) else "Sem dia fixo", style = MaterialTheme.typography.labelSmall)
                                }
                                s.items.forEach { it ->
                                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(onClick = { onHowTo(it.exId) }, color = androidx.compose.ui.graphics.Color.Transparent) { ExerciseThumb(data.ex(it.exId), 44.dp) }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(data.exName(it.exId), style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${it.sets} × ${it.reps}${targetUnit(data.exKind(it.exId))}" + (if (it.rest > 0) " · ${fmtRest(it.rest)}" else "") + (if (it.note.isNotBlank()) " · ${it.note}" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = onUse, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("Usar esta opção")
            }
        }
    }
}
