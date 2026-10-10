package app.ficha.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ficha.ai.AiError
import app.ficha.ai.COACH_SUGGEST
import app.ficha.ai.CoachAI
import app.ficha.data.AppData
import app.ficha.data.ChatMsg
import app.ficha.data.CoachData
import app.ficha.data.CoachReport
import app.ficha.logic.dateShort
import app.ficha.logic.millis
import app.ficha.logic.relDay
import app.ficha.logic.timeHM
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.AiKeySheet
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.ShapeIcon
import app.ficha.ui.components.aiReady
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Conversa em andamento (sobrevive a sair e voltar para a tela). */
private object CoachRun {
    var kind by mutableStateOf<String?>(null) // report | chat
    var text by mutableStateOf("")
    var job: Job? = null
}

@Composable
fun CoachScreen(data: AppData) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val C = data.coach ?: CoachData()
    var keySheet by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val setCoach = { f: (CoachData) -> CoachData -> app.update { it.copy(coach = f(it.coach ?: CoachData())) } }

    fun report() {
        if (CoachRun.kind != null) return
        val k = aiReady(context, app::toast) { keySheet = true } ?: return
        CoachRun.kind = "report"
        CoachRun.job = app.scope.launch {
            try {
                val r = CoachAI.report(k, app.data)
                setCoach { it.copy(report = CoachReport(System.currentTimeMillis(), CoachAI.weekKey(), r)) }
            } catch (e: Exception) {
                if (e !is CancellationException) app.toast((e as? AiError)?.message ?: "Não foi possível gerar o relatório agora")
            } finally {
                CoachRun.kind = null
            }
        }
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || CoachRun.kind != null) return
        val k = aiReady(context, app::toast) { keySheet = true } ?: return
        q = ""
        setCoach { it.copy(chat = (it.chat + ChatMsg("u", t, System.currentTimeMillis())).takeLast(40)) }
        CoachRun.kind = "chat"
        CoachRun.text = ""
        CoachRun.job = app.scope.launch {
            try {
                val out = CoachAI.chat(k, app.data, (app.data.coach?.chat ?: emptyList()).takeLast(20)) { d -> CoachRun.text += d }
                setCoach { it.copy(chat = it.chat + ChatMsg("a", out.trim().ifEmpty { "(sem resposta)" }, System.currentTimeMillis())) }
            } catch (e: Exception) {
                if (CoachRun.text.isNotBlank()) setCoach { it.copy(chat = it.chat + ChatMsg("a", CoachRun.text.trim() + "\n\n*(resposta interrompida)*", System.currentTimeMillis())) }
                else if (e !is CancellationException) app.toast((e as? AiError)?.message ?: "Não foi possível usar a IA agora")
            } finally {
                CoachRun.kind = null
                CoachRun.text = ""
            }
        }
    }

    val running = CoachRun.kind
    LaunchedEffect(C.chat.size, CoachRun.text.length / 200) {
        val n = list.layoutInfo.totalItemsCount
        if (n > 0 && C.chat.isNotEmpty()) list.animateScrollToItem(n - 1)
    }

    Screen(
        title = "Treinador IA", subtitle = "Com base nos seus registros", back = true, large = false, listState = list,
        actions = {
            if (C.chat.isNotEmpty()) TextButton(onClick = {
                app.confirm("Limpar conversa?", "As mensagens com o treinador são apagadas. O relatório da semana fica.", "Limpar", danger = true) { setCoach { it.copy(chat = emptyList()) } }
            }) { Text("Limpar") }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        q, { q = it }, placeholder = { Text("Pergunte ao treinador…") }, maxLines = 5, enabled = running == null,
                        shape = RoundedCornerShape(28.dp), modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    if (running == "chat") FilledIconButton(onClick = { CoachRun.job?.cancel() }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Stop, "Parar") }
                    else FilledIconButton(onClick = { send(q) }, enabled = q.isNotBlank() && running == null, shapes = IconButtonDefaults.shapes()) { Icon(Icons.AutoMirrored.Rounded.Send, "Enviar") }
                }
            }
        },
    ) {
        item {
            val rep = C.report
            when {
                running == "report" -> CardBox {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(Modifier.size(64.dp))
                        Text("Analisando sua semana…", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(top = 8.dp))
                        Text("Treinos, cargas, dieta e peso. Leva alguns segundos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                rep != null -> ReportCard(rep)
                else -> CardBox {
                    Text("Relatório da semana", style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        "A IA lê seus treinos, cargas, volume por músculo, dieta e peso, compara com as semanas anteriores e diz o que ajustar.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (running != "report") {
                val fresh = rep?.week == CoachAI.weekKey()
                val b: @Composable () -> Unit = { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.size(8.dp)); Text(if (rep != null) "Gerar relatório de novo" else "Gerar relatório da semana") }
                if (fresh) FilledTonalButton(onClick = { report() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 10.dp)) { b() }
                else Button(onClick = { report() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 10.dp)) { b() }
            }
        }
        item { SectionHeader("Conversa") }
        if (C.chat.isEmpty() && running != "chat") item { Hint("Pergunte qualquer coisa sobre o seu treino, dieta ou evolução. A IA vê os seus registros.") }
        C.chat.forEachIndexed { i, m -> item(key = "m$i-${m.at}") { Bubble(m.r == "u", m.x) } }
        if (running == "chat") item { Bubble(false, CoachRun.text.ifEmpty { "…" }) }
        if (C.chat.isEmpty()) item {
            FlowRow(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                COACH_SUGGEST.forEach { s -> AssistChip(onClick = { send(s) }, label = { Text(s) }) }
            }
        }
        item {
            Hint("Usa o Claude com a sua chave da API (cobrança por uso na sua conta da Anthropic). Ao gerar ou perguntar, um resumo dos seus registros é enviado à Anthropic. Não substitui um profissional de educação física, nutricionista ou médico.")
        }
    }
    if (keySheet) AiKeySheet({ keySheet = false })
}

@Composable
private fun ReportCard(rep: CoachReport) {
    val d = rep.data
    val week = runCatching { LocalDate.parse(rep.week).millis() }.getOrDefault(rep.at)
    CardBox {
        Text("RELATÓRIO · SEMANA DE ${dateShort(week)} · gerado ${relDay(rep.at).lowercase()} ${timeHM(rep.at)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(d.resumo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        @Composable
        fun block(title: String, items: List<String>, color: Color, on: Color) {
            if (items.isEmpty()) return
            Surface(color = color, contentColor = on, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
                    items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp)) }
                }
            }
        }
        block("O que foi bem", d.destaques, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        block("Atenção", d.atencao, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        if (d.recomendacoes.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Para a próxima semana", style = MaterialTheme.typography.titleSmallEmphasized)
                d.recomendacoes.forEach { r ->
                    Text(r.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    Text(r.detalhe, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        block("Metas da semana", d.metas_semana, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun Bubble(me: Boolean, text: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp), contentAlignment = if (me) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (me) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (me) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (me) 24.dp else 6.dp, bottomEnd = if (me) 6.dp else 24.dp),
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { if (me) Text(text) else MdText(text) }
        }
    }
}

/** Texto com formatação simples (negrito, itálico, código, listas e títulos), como o mdLite do PWA. */
@Composable
fun MdText(src: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (raw in src.split('\n')) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            val bullet = Regex("^\\s*[-*•]\\s+(.*)$").find(line)
            val numbered = Regex("^\\s*(\\d+)[.)]\\s+(.*)$").find(line)
            val heading = Regex("^#{1,4}\\s+(.*)$").find(line)
            when {
                bullet != null -> Row { Text("•  "); Text(inline(bullet.groupValues[1])) }
                numbered != null -> Row { Text("${numbered.groupValues[1]}.  "); Text(inline(numbered.groupValues[2])) }
                heading != null -> Text(inline(heading.groupValues[1]), style = MaterialTheme.typography.titleSmallEmphasized)
                else -> Text(inline(line))
            }
        }
    }
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val re = Regex("\\*\\*(.+?)\\*\\*|\\*(?!\\s)(.+?)\\*|`([^`]+)`")
    for (m in re.findAll(s)) {
        append(s.substring(i, m.range.first))
        when {
            m.groupValues[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            m.groupValues[2].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[2]) }
            else -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(m.groupValues[3]) }
        }
        i = m.range.last + 1
    }
    append(s.substring(i))
}

/** Banner do treinador na tela Hoje. */
@Composable
fun CoachTodayBanner(data: AppData) {
    val app = LocalApp.current
    val rep = data.coach?.report
    val fresh = rep != null && rep.week == CoachAI.weekKey()
    val sub = when {
        fresh -> "Relatório desta semana · ${rep.data.metas_semana.firstOrNull() ?: "veja as metas"}"
        data.sessions.isNotEmpty() -> "Gere o relatório da semana ou pergunte sobre seu treino e dieta"
        else -> "Tire dúvidas de treino e dieta com base nos seus dados"
    }
    Spacer(Modifier.height(10.dp))
    Surface(onClick = { app.go(Route.Coach) }, color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeIcon(Icons.Rounded.AutoAwesome, shape = MaterialShapes.Sunny.toShape(), size = 44.dp, container = MaterialTheme.colorScheme.tertiary, content = MaterialTheme.colorScheme.onTertiary)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Treinador IA", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

