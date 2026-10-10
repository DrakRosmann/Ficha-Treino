package app.ficha.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ficha.data.AppData
import app.ficha.data.ProgressPhoto
import app.ficha.logic.DAY_MS
import app.ficha.logic.bodyNear
import app.ficha.logic.dateLong
import app.ficha.logic.dateShort
import app.ficha.logic.fmt
import app.ficha.logic.millis
import app.ficha.logic.dateOf
import app.ficha.logic.startOfDay
import app.ficha.photos.PhotoStore
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.EmptyState
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.rememberPhotoPicker
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

/** Foto do progresso carregada do aparelho. */
@Composable
fun PhotoImage(id: String, thumb: Boolean, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    var bmp by remember(id, thumb) { mutableStateOf<Bitmap?>(PhotoStore.cached(id, thumb)) }
    LaunchedEffect(id, thumb) { if (bmp == null) bmp = PhotoStore.load(id, thumb) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = scale) }
    }
}

fun AppData.photosOf(pose: String? = null) = photos.filter { pose == null || it.pose == pose }.sortedBy { it.t }

private fun pd(v: Double, unit: String, dec: Int = 1) = "${if (v > 0) "+" else if (v < 0) "−" else ""}${fmt(abs(v), dec)}$unit"

fun AppData.photoCompareFacts(a: ProgressPhoto, b: ProgressPhoto): String {
    val out = mutableListOf("${Math.round((b.t - a.t) / DAY_MS.toDouble())} dias")
    for ((f, label, unit) in listOf(Triple("peso", "peso", " kg"), Triple("gordura", "gordura", " pts"), Triple("cintura", "cintura", " cm"), Triple("braco", "braço", " cm"))) {
        val va = bodyNear(a.t, f)
        val vb = bodyNear(b.t, f)
        if (va != null && vb != null) out += "$label ${pd(vb - va, unit)}"
    }
    return out.joinToString(" · ")
}

fun AppData.nextPose(t: Long): String {
    val have = photos.filter { it.t == t }.map { it.pose }.toSet()
    return PhotoStore.POSES.firstOrNull { it.first !in have }?.first ?: PhotoStore.POSES[0].first
}

/** Seção da aba Corpo */
@Composable
fun PhotosCorpoSection(data: AppData) {
    val app = LocalApp.current
    var adding by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val all = data.photosOf()
    SectionHeader("Fotos do progresso", trailing = if (all.isNotEmpty()) "${all.size}" else null)
    CardBox {
        if (all.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoStore.POSES.forEach { (p, l) -> PosePlaceholder(l, Modifier.weight(1f)) { adding = p } }
            }
            Text(
                "Tire fotos de frente, de lado e de costas a cada 2 a 4 semanas. A balança não mostra tudo: as fotos mostram a mudança no corpo. Elas ficam só neste aparelho.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp),
            )
            Button(onClick = { adding = data.nextPose(startOfDay(System.currentTimeMillis())) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Tirar as primeiras fotos")
            }
        } else {
            val last = all.maxOf { it.t }
            val w = data.bodyNear(last, "peso")
            Text("Últimas fotos: ${dateShort(last)}${w?.let { " · ${fmt(it)} kg" } ?: ""}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoStore.POSES.forEach { (p, l) ->
                    val x = all.find { it.t == last && it.pose == p }
                    if (x != null) PhotoImage(x.id, true, Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(16.dp)).clickable { viewing = x.id })
                    else PosePlaceholder(l, Modifier.weight(1f)) { adding = p }
                }
            }
            val front = data.photosOf("frente")
            if (front.size >= 2) {
                Spacer(Modifier.height(12.dp))
                Surface(onClick = { app.go(Route.Photos) }, color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PhotoImage(front.first().id, true, Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(10.dp)))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(horizontal = 6.dp))
                        PhotoImage(front.last().id, true, Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Antes e depois", style = MaterialTheme.typography.titleSmallEmphasized)
                            Text(data.photoCompareFacts(front.first(), front.last()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { adding = data.nextPose(startOfDay(System.currentTimeMillis())) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Adicionar fotos")
                }
                if (all.size > 1) FilledTonalButton(onClick = { app.go(Route.Photos) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Comparar") }
            }
        }
    }
    adding?.let { PhotoAddSheet(data, it) { adding = null } }
    viewing?.let { PhotoViewSheet(data, it, onDismiss = { viewing = null }) }
}

@Composable
private fun PosePlaceholder(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(16.dp), modifier = modifier.aspectRatio(0.75f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Galeria e comparação */
@Composable
fun PhotosScreen(data: AppData) {
    var pose by rememberSaveable { mutableStateOf("frente") }
    var aId by rememberSaveable { mutableStateOf<String?>(null) }
    var bId by rememberSaveable { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf("slide") }
    var adding by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val list = data.photosOf(pose)
    Screen(
        title = "Fotos do progresso", back = true,
        actions = { IconButton(onClick = { adding = pose }) { Icon(Icons.Rounded.Add, "Adicionar") } },
    ) {
        item {
            ConnectedChoice(PhotoStore.POSES.map { (p, l) -> p to (l + data.photosOf(p).size.let { if (it > 0) " · $it" else "" }) }, pose, Modifier.padding(horizontal = Gutter)) { pose = it }
            Spacer(Modifier.height(12.dp))
        }
        if (list.isEmpty()) item {
            EmptyState(
                Icons.Rounded.CameraAlt, "Nenhuma foto de ${PhotoStore.poseName(pose).lowercase()}",
                "Use sempre o mesmo lugar, a mesma luz e a mesma distância. A câmera com guia mostra a foto anterior por cima para você se posicionar igual.",
            ) { Button(onClick = { adding = pose }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Adicionar foto") } }
        } else if (list.size >= 2) item {
            val a = list.find { it.id == aId } ?: list.first()
            val b = list.find { it.id == bId && it.id != a.id } ?: list.last()
            CardBox {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField("Antes", list.map { it.id to dateShort(it.t) + yearSuffix(it.t) }, a.id, { aId = it }, Modifier.weight(1f))
                    DropdownField("Depois", list.map { it.id to dateShort(it.t) + yearSuffix(it.t) }, b.id, { bId = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                ConnectedChoice(listOf("slide" to "Deslizar", "side" to "Lado a lado"), mode) { mode = it }
                Spacer(Modifier.height(12.dp))
                if (mode == "slide") CompareSlider(a, b) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(a, b).forEach { p ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            PhotoImage(p.id, false, Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp)))
                            Text(dateShort(p.t), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text(data.photoCompareFacts(a, b), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
            }
        } else item { Hint("Adicione outra foto de ${PhotoStore.poseName(pose).lowercase()} em outra data para comparar.") }
        if (list.isNotEmpty()) {
            item { SectionHeader("Linha do tempo") }
            item {
                Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.reversed().chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { p ->
                                Column(Modifier.weight(1f)) {
                                    PhotoImage(p.id, true, Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp)).clickable { viewing = p.id })
                                    val w = data.bodyNear(p.t, "peso")
                                    Text("${dateShort(p.t)}${w?.let { " · ${fmt(it)} kg" } ?: ""}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            Hint("${data.photos.size} fotos · ${fmt(PhotoStore.totalKb(data.photos) / 1024.0, 1)} MB neste aparelho. As fotos não vão para a internet; o backup só as inclui se você escolher.")
        }
    }
    adding?.let { PhotoAddSheet(data, it) { adding = null } }
    viewing?.let { id ->
        PhotoViewSheet(data, id, onDismiss = { viewing = null }) { other ->
            val p = data.photos.find { it.id == id } ?: return@PhotoViewSheet
            pose = p.pose
            if (other.t < p.t) { aId = other.id; bId = p.id } else { aId = p.id; bId = other.id }
            viewing = null
        }
    }
}

private fun yearSuffix(t: Long) = dateOf(t).year.let { if (it != LocalDate.now().year) "/$it" else "" }

/** Antes e depois deslizando: arraste na foto; a de antes aparece até a linha. */
@Composable
private fun CompareSlider(a: ProgressPhoto, b: ProgressPhoto) {
    var pos by remember { mutableFloatStateOf(.5f) }
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) { detectTapGestures { pos = (it.x / size.width).coerceIn(0f, 1f) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { ch, _ -> pos = (ch.position.x / size.width).coerceIn(0f, 1f) } },
    ) {
        PhotoImage(b.id, false, Modifier.fillMaxSize())
        PhotoImage(a.id, false, Modifier.fillMaxSize().drawWithContent { clipRect(right = size.width * pos) { this@drawWithContent.drawContent() } })
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val x = size.width * pos
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 3.dp.toPx())
            drawCircle(Color.White, 14.dp.toPx(), androidx.compose.ui.geometry.Offset(x, size.height / 2))
        }
        Text(dateShort(a.t), color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0x66000000), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Text(dateShort(b.t), color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color(0x66000000), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Adicionar fotos: data, pose, câmera com guia ou galeria. */
@Composable
fun PhotoAddSheet(data: AppData, pose0: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now()) }
    var pose by remember { mutableStateOf(pose0) }
    val t = date.millis()
    val done = data.photos.filter { it.t == t }
    val prev = data.photosOf(pose).lastOrNull { it.t < t }
    val picker = rememberPhotoPicker(onError = app::toast) { bmp ->
        val p = pose
        scope.launch {
            val ph = PhotoStore.save(bmp, t, p)
            app.update { it.copy(photos = it.photos + ph) }
            app.toast("Foto de ${PhotoStore.poseName(p).lowercase()} salva")
            pose = app.data.nextPose(t)
        }
    }
    Sheet(onDismiss, "Fotos do progresso") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            DateField("Data", date) { date = it; pose = data.nextPose(it.millis()) }
            Label2("Pose")
            ConnectedChoice(PhotoStore.POSES.map { (p, l) -> p to ((if (done.any { it.pose == p }) "✓ " else "") + l) }, pose) { pose = it }
            if (done.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                PhotoStore.POSES.forEach { (p, l) ->
                    val x = done.find { it.pose == p }
                    if (x != null) PhotoImage(x.id, true, Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(14.dp)))
                    else PosePlaceholder(l, Modifier.weight(1f)) { pose = p }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onDismiss(); app.go(Route.Camera(t, pose)) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                Icon(Icons.Rounded.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Câmera com guia", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { picker.gallery() }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Escolher da galeria")
            }
            if (prev != null) Text(
                "A câmera com guia mostra por cima, transparente, a foto de ${PhotoStore.poseName(pose).lowercase()} de ${dateShort(prev.t)}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
            )
            Label2("Para comparar bem")
            listOf(
                "Mesmo lugar, mesma luz e mesma distância", "De manhã, em jejum, com roupas parecidas",
                "Celular na altura do umbigo, reto; use o timer e apoie o celular", "Corpo relaxado, na mesma postura",
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text(if (done.isNotEmpty()) "Concluir" else "Cancelar") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Ver uma foto: anterior/próxima, comparar e excluir. */
@Composable
fun PhotoViewSheet(data: AppData, id0: String, onDismiss: () -> Unit, onCompare: ((ProgressPhoto) -> Unit)? = null) {
    val app = LocalApp.current
    var id by remember { mutableStateOf(id0) }
    val p = data.photos.find { it.id == id } ?: return
    val list = data.photosOf(p.pose)
    val i = list.indexOfFirst { it.id == id }
    val w = data.bodyNear(p.t, "peso")
    val bf = data.bodyNear(p.t, "gordura")
    Sheet(onDismiss, "${PhotoStore.poseName(p.pose)} · ${dateShort(p.t)}") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            PhotoImage(p.id, false, Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(24.dp)), ContentScale.Fit)
            Text(
                dateLong(p.t) + (w?.let { " · ${fmt(it)} kg" } ?: "") + (bf?.let { " · ${fmt(it)}% gordura" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                FilledTonalButton(onClick = { id = list[i - 1].id }, enabled = i > 0, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("‹ Anterior") }
                FilledTonalButton(onClick = { id = list[i + 1].id }, enabled = i < list.size - 1, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Próxima ›") }
            }
            if (list.size > 1) OutlinedButton(onClick = {
                val other = if (list.first().id == p.id) list.last() else list.first()
                if (onCompare != null) onCompare(other) else { onDismiss(); app.go(Route.Photos) }
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Comparar com a ${if (i == 0) "última" else "primeira"}") }
            OutlinedButton(onClick = {
                app.confirm("Excluir esta foto?", "Ela será apagada deste aparelho.", "Excluir", danger = true) {
                    app.update { d -> d.copy(photos = d.photos.filter { it.id != p.id }) }
                    PhotoStore.delete(p.id)
                    app.toast("Foto excluída")
                    onDismiss()
                }
            }, shapes = ButtonDefaults.shapes(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Excluir foto")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
