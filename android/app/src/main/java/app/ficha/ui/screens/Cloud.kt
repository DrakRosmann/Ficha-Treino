package app.ficha.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ficha.data.AppData
import app.ficha.data.Foods
import app.ficha.logic.DAY_LONG
import app.ficha.logic.daysLabel
import app.ficha.logic.isScheduled
import app.ficha.logic.relDay
import app.ficha.logic.timeHM
import app.ficha.sync.CloudCrypto
import app.ficha.sync.CloudSync
import app.ficha.sync.Reminders
import app.ficha.timer.RestNotifier
import app.ficha.ui.LocalApp
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

private fun ago(t: Long): String {
    if (t == 0L) return "ainda não"
    val s = (System.currentTimeMillis() - t) / 1000
    return when {
        s < 60 -> "agora há pouco"
        s < 3600 -> "há ${Math.round(s / 60.0)} min"
        else -> "${relDay(t).lowercase()} às ${timeHM(t)}"
    }
}

@Composable
fun CloudScreen(data: AppData) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(CloudSync.url) }
    var code by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var cfg by remember { mutableStateOf(Reminders.config(context)) }
    val on = CloudSync.on
    val saveCfg = { c: Reminders.Config -> cfg = c; Reminders.save(context, c) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { saveCfg(cfg.copy(on = true)); app.toast("Lembretes ativados") }
        else app.toast("Notificações bloqueadas. Libere nas configurações do Android → Apps → Ficha → Notificações")
    }
    LaunchedEffect(url) { CloudSync.setUrl(url) }

    Screen(title = "Nuvem e lembretes", subtitle = "Entre aparelhos e lembretes", back = true, large = false) {
        item { SectionHeader("Servidor") }
        item {
            CardBox {
                OutlinedTextField(
                    url, { url = it.trim() }, label = { Text("Endereço do seu servidor") }, placeholder = { Text("https://ficha-sync.seu-nome.workers.dev") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    FilledTonalButton(onClick = { CloudSync.setUrl(url); scope.launch { app.toast(CloudSync.ping()) } }, shapes = ButtonDefaults.shapes()) { Text("Testar conexão") }
                    OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CloudSync.HELP))) } }, shapes = ButtonDefaults.shapes()) { Text("Como criar (grátis)") }
                }
                Text(
                    "O Ficha não tem servidor próprio: você cria o seu na Cloudflare, de graça, em uns 5 minutos, e só você usa. É o mesmo servidor do app web: os dois sincronizam entre si.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        item { SectionHeader("Sincronização") }
        item {
            when {
                CloudSync.url.isEmpty() -> Hint("Configure o servidor acima para sincronizar.")
                !on -> CardBox {
                    Text("Mantenha os mesmos treinos, fichas, dieta e medidas no celular, no tablet ou no app web — e não perca nada se trocar de aparelho.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch {
                            val c = CloudSync.create()
                            if (c == null) app.toast(CloudSync.error) else code = c to true
                        }
                    }, enabled = !CloudSync.busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Começar a sincronizar") }
                    OutlinedButton(onClick = {
                        app.prompt("Entrar com um código", "", "Código de sincronização", "XXXX-XXXX-XXXX-XXXX-XXXX", "Entrar") { v ->
                            scope.launch {
                                val err = CloudSync.join(v)
                                app.toast(err ?: "Dados sincronizados ✓")
                            }
                        }
                    }, enabled = !CloudSync.busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Já tenho um código") }
                    Text("Ao entrar com um código, os dados deste aparelho são juntados com os da nuvem (nada é apagado).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    val err = CloudSync.error
                    SegmentedList(listOf(
                        Seg(
                            headline = if (err.isNotEmpty()) "Não sincronizou" else "Sincronização ativa",
                            supporting = if (CloudSync.busy) "Sincronizando…" else err.ifEmpty { "Sincronizado ${ago(CloudSync.lastAt)}" },
                            supportingColor = if (err.isNotEmpty()) MaterialTheme.colorScheme.error else null,
                            trailing = {
                                if (err.isNotEmpty()) Pill("Erro", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                                else Pill("Ativa", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            },
                        ),
                        Seg(headline = "Sincronizar agora", onClick = { scope.launch { CloudSync.sync(pull = true); app.toast(CloudSync.error.ifEmpty { "Sincronizado ✓" }) } }),
                        Seg(headline = "Código de sincronização", supporting = "Use para entrar nos outros aparelhos", onClick = { CloudSync.code?.let { code = it to false } }),
                        Seg(headline = "Desligar neste aparelho", onClick = {
                            app.confirm("Desligar a sincronização?", "Os dados continuam neste aparelho e na nuvem. Para voltar, use o código.", "Desligar") { CloudSync.off() }
                        }),
                        Seg(headline = "Apagar meus dados da nuvem", headlineColor = MaterialTheme.colorScheme.error, onClick = {
                            app.confirm(
                                "Apagar os dados da nuvem?",
                                "Apaga a cópia do servidor e desliga a sincronização neste aparelho. Os dados deste aparelho continuam aqui. Desligue também nos outros aparelhos, senão eles enviam tudo de novo.",
                                "Apagar da nuvem", danger = true,
                            ) { scope.launch { val e = CloudSync.wipeRemote(); app.toast(e ?: "Dados apagados da nuvem") } }
                        }),
                    ))
                }
            }
            Hint("Criptografia de ponta a ponta: os dados saem do aparelho embaralhados com o seu código, e o servidor não consegue ler. As fotos do progresso e o treino em andamento ficam só em cada aparelho.")
        }
        item { SectionHeader("Lembretes") }
        item {
            SegmentedList(listOf(Seg(
                headline = "Notificações neste aparelho", supporting = if (cfg.on) "Ativadas" else "Desligadas",
                trailing = {
                    Switch(cfg.on, { v ->
                        if (!v) saveCfg(cfg.copy(on = false))
                        else if (Build.VERSION.SDK_INT >= 33 && !RestNotifier.canNotify(context)) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else saveCfg(cfg.copy(on = true))
                    })
                },
            )))
        }
        if (cfg.on) item {
            val R = cfg.rem
            val tDays = data.routines.filter { it.items.isNotEmpty() && data.isScheduled(it) }.flatMap { it.days }.distinct()
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RemCard("Treino do dia", if (tDays.isNotEmpty()) "${daysLabel(tDays)} · pelos dias das fichas" else "Defina os dias nas fichas para receber", R.treino.on, { saveCfg(cfg.copy(rem = R.copy(treino = R.treino.copy(on = it)))) }) {
                    TimeRow("Horário", R.treino.hm) { saveCfg(cfg.copy(rem = R.copy(treino = R.treino.copy(hm = it)))) }
                }
                RemCard("Beber água", "Para quando bater a meta do dia", R.agua.on, { saveCfg(cfg.copy(rem = R.copy(agua = R.agua.copy(on = it)))) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownField("Das", (6..12).map { "$it" to "${it}h" }, R.agua.from.toString(), { saveCfg(cfg.copy(rem = R.copy(agua = R.agua.copy(from = it.toInt())))) }, Modifier.weight(1f))
                        DropdownField("Às", (16..23).map { "$it" to "${it}h" }, R.agua.to.toString(), { saveCfg(cfg.copy(rem = R.copy(agua = R.agua.copy(to = it.toInt())))) }, Modifier.weight(1f))
                        DropdownField("A cada", (1..4).map { "$it" to "${it}h" }, R.agua.every.toString(), { saveCfg(cfg.copy(rem = R.copy(agua = R.agua.copy(every = it.toInt())))) }, Modifier.weight(1f))
                    }
                }
                RemCard("Registrar refeições", "Não avisa a refeição já registrada", R.meals.on, { saveCfg(cfg.copy(rem = R.copy(meals = R.meals.copy(on = it)))) }) {
                    Foods.MEALS.forEachIndexed { m, n ->
                        TimeRow(n, R.meals.hm.getOrElse(m) { "12:00" }) { v -> saveCfg(cfg.copy(rem = R.copy(meals = R.meals.copy(hm = R.meals.hm.toMutableList().also { it[m] = v })))) }
                    }
                }
                RemCard("Pesagem semanal", "Não avisa se já pesou nos últimos dias", R.peso.on, { saveCfg(cfg.copy(rem = R.copy(peso = R.peso.copy(on = it)))) }) {
                    DropdownField("Dia", DAY_LONG.mapIndexed { i, d -> "$i" to d }, R.peso.day.toString(), { saveCfg(cfg.copy(rem = R.copy(peso = R.peso.copy(day = it.toInt())))) })
                    TimeRow("Horário", R.peso.hm) { saveCfg(cfg.copy(rem = R.copy(peso = R.peso.copy(hm = it)))) }
                }
                RemCard("Backup semanal", if (CloudSync.on) "Com a sincronização ativa, seus dados já ficam na nuvem" else "Lembra de salvar o arquivo de backup", R.backup.on, { saveCfg(cfg.copy(rem = R.copy(backup = R.backup.copy(on = it)))) }) {
                    DropdownField("Dia", DAY_LONG.mapIndexed { i, d -> "$i" to d }, R.backup.day.toString(), { saveCfg(cfg.copy(rem = R.copy(backup = R.backup.copy(day = it.toInt())))) })
                    TimeRow("Horário", R.backup.hm) { saveCfg(cfg.copy(rem = R.copy(backup = R.backup.copy(hm = it)))) }
                }
                FilledTonalButton(onClick = { Reminders.test(context); app.toast("Enviada — deve chegar em instantes") }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter)) { Text("Enviar notificação de teste") }
            }
        }
        item {
            Hint("Os lembretes são agendados neste aparelho, no fuso dele (${ZoneId.systemDefault().id}), sem precisar do servidor. O que você já fez no dia não é lembrado.")
        }
    }
    code?.let { (c, fresh) -> CodeSheet(c, fresh) { code = null } }
}

@Composable
private fun RemCard(title: String, sub: String, on: Boolean, onToggle: (Boolean) -> Unit, content: @Composable () -> Unit) {
    CardBox(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(on, onToggle)
        }
        if (on) Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Horário com o seletor de relógio do Material. */
@Composable
private fun TimeRow(label: String, hm: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = { open = true }, shapes = ButtonDefaults.shapes()) { Text(hm, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum")) }
    }
    if (open) {
        val t = runCatching { LocalTime.parse(hm) }.getOrDefault(LocalTime.of(8, 0))
        val st = rememberTimePickerState(t.hour, t.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { onChange("%02d:%02d".format(st.hour, st.minute)); open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CodeSheet(code: String, fresh: Boolean, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val shown = CloudCrypto.fmtCode(code)
    Sheet(onDismiss, if (fresh) "Guarde seu código" else "Código de sincronização") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(shown, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            Text(
                (if (fresh) "Pronto: seus dados estão na nuvem. " else "") + "Para usar em outro aparelho, abra o Ficha nele, vá em Ajustes → Nuvem e lembretes → Já tenho um código e digite este código.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Ele é a chave dos seus dados: sem ele ninguém consegue ler — nem você, se perder. Guarde no gerenciador de senhas ou nas notas. Não compartilhe com outras pessoas.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = {
                runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Código de sincronização do Ficha: $shown"), "Guardar código")) }
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartilhar / guardar") }
            OutlinedButton(onClick = {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Código do Ficha", shown))
                app.toast("Código copiado")
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copiar código") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
