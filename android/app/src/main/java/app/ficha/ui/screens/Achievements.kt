package app.ficha.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ficha.data.AppData
import app.ficha.logic.ACH
import app.ficha.logic.ACH_BY_ID
import app.ficha.logic.ACH_GROUPS
import app.ficha.logic.Ach
import app.ficha.logic.TIER
import app.ficha.logic.TIER_NAME
import app.ficha.logic.achProgressText
import app.ficha.logic.achStats
import app.ficha.logic.dateLong
import app.ficha.share.ShareImage
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.rememberPhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Selo da conquista: círculo com as cores do nível e o emoji. */
@Composable
fun AchBadge(a: Ach, on: Boolean, size: Dp = 64.dp) {
    val (c1, c2) = TIER[a.t]!!
    Box(
        Modifier.size(size).alpha(if (on) 1f else .3f).clip(CircleShape).background(Brush.linearGradient(listOf(Color(c1), Color(c2)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size * .8f).clip(CircleShape).background(Color(0x2E000000)))
        Text(a.e, fontSize = (size.value * .45f).sp)
    }
}

@Composable
fun AchievementsScreen(data: AppData) {
    var open by remember { mutableStateOf<String?>(null) }
    val st = remember(data) { data.achStats() }
    val got = ACH.count { data.ach?.get(it.id) != null }
    Screen(title = "Conquistas", back = true) {
        item {
            Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { got / ACH.size.toFloat() }, modifier = Modifier.weight(1f).height(10.dp), drawStopIndicator = {})
                Text("  $got de ${ACH.size}", style = MaterialTheme.typography.titleSmallEmphasized)
            }
        }
        ACH_GROUPS.forEachIndexed { gi, g ->
            item { SectionHeader(g) }
            item {
                Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACH.filter { it.g == gi }.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { a ->
                                val on = data.ach?.get(a.id) != null
                                val (c, goal) = a.p(st)
                                Surface(onClick = { open = a.id }, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        AchBadge(a, on, 56.dp)
                                        Text(a.n, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                                        if (!on) LinearProgressIndicator(
                                            progress = { (c / goal).toFloat().coerceIn(0f, 1f) }, drawStopIndicator = {},
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
    open?.let { AchSheet(data, it) { open = null } }
}

@Composable
fun AchSheet(data: AppData, id: String, onDismiss: () -> Unit) {
    val a = ACH_BY_ID[id] ?: return
    val on = data.ach?.get(id)
    var share by remember { mutableStateOf(false) }
    Sheet(onDismiss, "Conquista") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            AchBadge(a, on != null, 120.dp)
            Text(TIER_NAME[a.t], color = Color(TIER[a.t]!!.second), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text(a.n, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
            Text(a.d, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(
                if (on != null) (if (on > 1) "Desbloqueada em ${dateLong(on)}" else "Desbloqueada") else achProgressText(a, data.achStats()),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp),
            )
            if (on != null) Button(onClick = { share = true }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartilhar")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (share) ShareSheet(data, "ach", id) { share = false }
}

/** Bloco das conquistas no Histórico. */
@Composable
fun AchHistoryCard(data: AppData) {
    val app = LocalApp.current
    val achMap = data.ach ?: return
    val got = ACH.filter { achMap[it.id] != null }
    val recent = got.sortedByDescending { achMap[it.id] }.take(4)
    val st = remember(data) { data.achStats() }
    val next = ACH.filter { achMap[it.id] == null }.map { it to (it.p(st).let { (c, g) -> minOf(1.0, c / g) }) }.maxByOrNull { it.second }
    Spacer(Modifier.height(12.dp))
    Surface(onClick = { app.go(Route.Achievements) }, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                (recent.ifEmpty { ACH.take(4) }).forEach { AchBadge(it, recent.isNotEmpty(), 38.dp) }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("Conquistas", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    "${got.size} de ${ACH.size}" + (next?.let { " · próxima: ${it.first.n} (${Math.round(it.second * 100)}%)" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

/** Conquistas desbloqueadas (no resumo do treino e na sessão). */
@Composable
fun AchBadgesRow(data: AppData, ids: List<String>) {
    var open by remember { mutableStateOf<String?>(null) }
    val list = ids.mapNotNull { ACH_BY_ID[it] }
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    Surface(onClick = { open = a.id }, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AchBadge(a, true, 52.dp)
                            Text(a.n, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    open?.let { AchSheet(data, it) { open = null } }
}

/** Imagem para os Stories: cartão, sobre uma foto sua ou adesivo com fundo transparente. */
@Composable
fun ShareSheet(data: AppData, kind: String, id: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    var mode by remember { mutableStateOf("card") }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var img by remember { mutableStateOf<Bitmap?>(null) }
    val picker = rememberPhotoPicker(onError = app::toast) { b -> photo = b; mode = "photo" }
    LaunchedEffect(mode, photo) {
        img = null
        img = withContext(Dispatchers.Default) {
            if (kind == "session") data.sessions.find { it.id == id }?.let { ShareImage.session(context, data, it, mode, photo) }
            else ACH_BY_ID[id]?.let { ShareImage.achievement(context, data, it, mode, photo) }
        }
    }
    val name = if (kind == "session") "ficha-treino.png" else "ficha-conquista.png"
    Sheet(onDismiss, if (kind == "session") "Compartilhar treino" else "Compartilhar conquista") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            ConnectedChoice(listOf("card" to "Cartão", "photo" to "Com foto", "sticker" to "Adesivo"), mode) { v ->
                if (v == "photo" && photo == null) picker.gallery() else mode = v
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth(0.62f).aspectRatio(1080f / 1920f).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(20.dp))
                    .background(if (mode == "sticker") Brush.linearGradient(listOf(Color(0xFF6B7280), Color(0xFF374151))) else Brush.linearGradient(listOf(Color.Black, Color.Black))),
                contentAlignment = Alignment.Center,
            ) {
                val b = img
                if (b == null) LoadingIndicator() else Image(b.asImageBitmap(), "Prévia da imagem", Modifier.fillMaxSize())
            }
            Text(
                "Formato dos Stories (1080 × 1920). O adesivo tem fundo transparente para colar sobre a sua foto.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            Button(onClick = { img?.let { ShareImage.share(context, it, name) } }, enabled = img != null, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(8.dp)); Text("Compartilhar")
            }
            OutlinedButton(onClick = {
                img?.let { app.toast(if (ShareImage.saveToGallery(context, it, "ficha-${System.currentTimeMillis()}.png")) "Imagem salva na galeria (Imagens/Ficha)" else "Não foi possível salvar a imagem") }
            }, enabled = img != null, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Salvar imagem")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
