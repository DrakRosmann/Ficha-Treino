package app.ficha.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.ficha.data.AppData
import app.ficha.data.Settings
import app.ficha.logic.REST_OPTIONS
import app.ficha.logic.backupLabel
import app.ficha.logic.dateShort
import app.ficha.logic.fmtRest
import app.ficha.timer.RestNotifier
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.theme.ACCENTS
import app.ficha.ui.theme.PALETTES
import app.ficha.ui.theme.THEMES
import app.ficha.ui.theme.isDarkTheme
import app.ficha.ui.theme.schemeFromSeed
import kotlinx.serialization.json.JsonArray
import java.io.File
import java.time.LocalDate

@Composable
fun SettingsScreen(data: AppData) {
    val app = LocalApp.current
    val context = LocalContext.current
    val s = data.settings
    val set = { f: (Settings) -> Settings -> app.update { it.copy(settings = f(it.settings)) } }
    var calc by remember { mutableStateOf(false) }
    val backupName = "ficha-backup-${LocalDate.now()}.json"

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(app.store.exportJson().toByteArray()) }
            set { it.copy(lastBackup = System.currentTimeMillis(), backupSnooze = 0) }
            app.toast("Backup salvo")
        }.onFailure { app.toast("Não foi possível salvar o backup") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }.getOrNull()
            val p = text?.let { app.store.readBackup(it) }
            if (p == null) app.toast("Esse arquivo não é um backup do Ficha")
            else app.confirm(
                "Importar backup?",
                "O arquivo tem ${p.routines} fichas e ${p.sessions} treinos${if (p.photos > 0) " (as ${p.photos} fotos do progresso ainda não são importadas no Android)" else ""}. Os dados atuais deste aparelho serão substituídos.",
                "Importar", danger = true,
            ) {
                app.store.importBackup(p)
                app.toast("Backup importado — dá para desfazer em Ajustes")
                app.tab(Route.Today)
            }
        }
    }
    val shareBackup = {
        runCatching {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val f = File(dir, backupName).apply { writeText(app.store.exportJson()) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
            context.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Backup do Ficha",
            ))
            set { it.copy(lastBackup = System.currentTimeMillis(), backupSnooze = 0) }
        }.onFailure { app.toast("Não foi possível compartilhar o backup") }
    }

    Screen(title = "Ajustes") {
        item { SectionHeader("Aparência") }
        item {
            CardBox {
                Label("Tema")
                ConnectedChoice(THEMES, s.theme) { k -> set { it.copy(theme = k) } }
                if (Build.VERSION.SDK_INT >= 31) {
                    Spacer(Modifier.height(12.dp))
                    SegmentedList(listOf(Seg(
                        headline = "Cores do papel de parede", supporting = "Material You: usa as cores do sistema em vez da cor escolhida abaixo",
                        trailing = { Switch(s.androidDynamic, { v -> set { it.copy(androidDynamic = v) } }) },
                        onClick = { set { it.copy(androidDynamic = !it.androidDynamic) } },
                    )), Modifier)
                }
                if (!s.androidDynamic || Build.VERSION.SDK_INT < 31) {
                    Label("Paleta")
                    ConnectedChoice(PALETTES, s.androidPalette) { k -> set { it.copy(androidPalette = k) } }
                    Label("Cor (a paleta é gerada a partir dela)")
                    val dark = isDarkTheme(s)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 5) {
                        ACCENTS.forEach { a ->
                            val on = s.accent == a.id
                            val sch = remember(a.id, dark, s.androidPalette) { schemeFromSeed(a.seed, dark, s.androidPalette, a.id == "mono") }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                                Surface(
                                    onClick = { set { it.copy(accent = a.id) } },
                                    shape = if (on) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(52.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Prévia como no Android: primária em cima, secundária e terciária embaixo
                                        Canvas(Modifier.size(40.dp)) {
                                            val r = size.minDimension / 2
                                            drawArc(sch.primary, 180f, 180f, true, Offset.Zero, Size(r * 2, r * 2))
                                            drawArc(sch.secondaryContainer, 90f, 90f, true, Offset.Zero, Size(r * 2, r * 2))
                                            drawArc(sch.tertiaryContainer, 0f, 90f, true, Offset.Zero, Size(r * 2, r * 2))
                                        }
                                        if (on) Surface(color = sch.primaryContainer, contentColor = sch.onPrimaryContainer, shape = CircleShape, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Rounded.Check, null, Modifier.padding(3.dp))
                                        }
                                    }
                                }
                                Text(a.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Treino") }
        item {
            var restOpen by remember { mutableStateOf(false) }
            SegmentedList(listOf(
                Seg(headline = "Descanso padrão", supporting = "${fmtRest(s.rest)} · usado em treino livre e exercícios novos", onClick = { restOpen = true }, trailing = {
                    androidx.compose.material3.DropdownMenu(restOpen, { restOpen = false }) {
                        REST_OPTIONS.filter { it > 0 }.forEach { r ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(fmtRest(r)) }, onClick = { restOpen = false; set { it.copy(rest = r) } })
                        }
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                }),
                toggle("Som ao fim do descanso", "Alarme quando o descanso acaba (sem som: só vibra)", s.sound) { v -> set { it.copy(sound = v) } },
                toggle("Sugerir carga e repetições", "Progressão automática: o app sugere quanto fazer em cada exercício com base no último treino", s.progression) { v -> set { it.copy(progression = v) } },
                toggle("Registrar RIR", "Repetições na reserva: quantas ainda sobravam ao fim da série. Opcional; deixa a progressão mais precisa", s.rir) { v -> set { it.copy(rir = v) } },
                toggle("Manter a tela ligada no treino", "A tela não apaga sozinha enquanto há um treino em andamento", s.keepAwake) { v -> set { it.copy(keepAwake = v) } },
                Seg(headline = "Calculadoras", supporting = "Anilhas, aquecimento e 1RM", onClick = { calc = true }, trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }),
                Seg(
                    headline = "Notificações do descanso",
                    supporting = if (RestNotifier.canNotify(context)) "Ativas: o descanso aparece na tela bloqueada e toca com a tela apagada" else "Desativadas — toque para permitir",
                    onClick = {
                        context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                    trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) },
                ),
            ))
        }

        item { SectionHeader("Seus dados") }
        item {
            val prev = app.store.previousCopyAt()
            SegmentedList(buildList {
                add(Seg(headline = "Exportar backup", supporting = data.backupLabel(), onClick = { exportLauncher.launch(backupName) }, trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }))
                add(Seg(headline = "Compartilhar backup", supporting = "Enviar o arquivo para o Drive, e-mail ou outro app", onClick = { shareBackup() }, trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }))
                add(Seg(headline = "Importar backup", supporting = "Do PWA ou deste app · substitui os dados atuais", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }))
                if (prev != null) add(Seg(headline = "Desfazer a última importação", supporting = "Volta aos dados de antes de importar (${dateShort(prev)})", onClick = {
                    app.confirm("Desfazer a importação?", "Volta aos dados que estavam neste aparelho antes de importar.", "Desfazer importação", danger = true) {
                        if (app.store.undoImport()) { app.toast("Dados anteriores restaurados"); app.tab(Route.Today) }
                    }
                }))
                add(Seg(headline = "Modelos de treino prontos", supporting = "PPL, Upper/Lower, ABC, ABCDE, em casa e mais", onClick = { app.go(Route.Templates) }, trailing = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) }))
                add(Seg(headline = "Apagar todos os dados", headlineColor = MaterialTheme.colorScheme.error, onClick = {
                    app.confirm("Apagar tudo?", "Fichas, histórico e exercícios personalizados serão apagados deste aparelho. Isso não pode ser desfeito.", "Apagar tudo", danger = true) {
                        app.store.wipe(); app.toast("Dados apagados"); app.tab(Route.Today)
                    }
                }))
            })
            val keep = listOf("food" to "dieta", "body" to "medidas", "photos" to "fotos").mapNotNull { (k, l) ->
                val n = (app.store.extra(k) as? JsonArray)?.size ?: if (k == "food" && app.store.extra(k) != null) 1 else 0
                if (n > 0) l else null
            }
            Hint(
                "${data.programs.size} programas · ${data.routines.size} fichas · ${data.sessions.size} treinos · ${data.custom.size} exercícios personalizados. " +
                    "Tudo fica salvo só neste aparelho: exporte um backup de vez em quando. O arquivo é o mesmo do app web (PWA), então dá para levar os dados de um para o outro." +
                    if (keep.isNotEmpty()) " Os dados de ${keep.joinToString(", ")} vindos do PWA ficam guardados e voltam no backup." else "",
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Ficha para Android · versão 1.0\nFotos e músculos: free-exercise-db (domínio público)\nMapa muscular: react-body-highlighter (MIT)\nFonte: Google Sans Flex (OFL)",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
        }
    }

    if (calc) CalculatorSheet(data) { calc = false }
}

private fun toggle(title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) =
    Seg(headline = title, supporting = sub, trailing = { Switch(on, set) }, onClick = { set(!on) })

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp))
}
