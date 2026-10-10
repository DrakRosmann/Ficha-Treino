package app.ficha.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.ficha.data.AppData
import app.ficha.logic.dateShort
import app.ficha.photos.PhotoStore
import app.ficha.ui.LocalApp
import app.ficha.ui.Route
import app.ficha.ui.components.ConnectedChoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(data: AppData, t: Long, pose0: String) {
    val app = LocalApp.current
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) { app.toast("Sem acesso à câmera. Permita a câmera ou escolha uma foto."); app.leave(Route.Camera(t, pose0)) }
    }
    LaunchedEffect(Unit) { if (!granted) perm.launch(Manifest.permission.CAMERA) }

    var pose by remember { mutableStateOf(pose0) }
    var front by remember { mutableStateOf(false) }
    var timer by remember { mutableIntStateOf(3) }
    var ghost by remember { mutableFloatStateOf(.35f) }
    var count by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val prev = data.photosOf(pose).lastOrNull { it.t <= t }

    LaunchedEffect(granted, front) {
        if (!granted) return@LaunchedEffect
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(owner, if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        }.onFailure { app.toast("Não foi possível abrir a câmera") }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() } }
    }

    fun shootNow() {
        capture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val raw = image.toBitmap()
                val m = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                    if (front) postScale(-1f, 1f) // salva como aparece na tela
                }
                image.close()
                val bmp = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                val p = pose
                scope.launch {
                    flash.snapTo(1f)
                    launch { flash.animateTo(0f, tween(450)) }
                    val ph = PhotoStore.save(bmp, t, p)
                    app.update { it.copy(photos = it.photos + ph) }
                    app.toast("Foto de ${PhotoStore.poseName(p).lowercase()} salva")
                    busy = false
                    val have = app.data.photos.filter { it.t == t }.map { it.pose }.toSet()
                    if (PhotoStore.POSES.all { it.first in have }) app.leave(Route.Camera(t, pose0))
                    else pose = app.data.nextPose(t)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                busy = false
                app.toast("Não foi possível salvar a foto")
            }
        })
    }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { app.leave(Route.Camera(t, pose0)) }) { Icon(Icons.Rounded.Close, "Fechar", tint = Color.White) }
            Text("${PhotoStore.poseName(pose)} · ${dateShort(t)}", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { front = !front }) { Icon(Icons.Rounded.Cameraswitch, if (front) "Câmera traseira" else "Câmera frontal", tint = Color.White) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(0.75f).clip(RoundedCornerShape(24.dp))) {
                AndroidView({ previewView }, Modifier.fillMaxSize().graphicsLayer { scaleX = if (front) -1f else 1f })
                if (prev != null) PhotoImage(prev.id, false, Modifier.fillMaxSize().alpha(ghost), ContentScale.Crop)
                // Grade dos terços
                Canvas(Modifier.fillMaxSize()) {
                    val c = Color.White.copy(alpha = .35f)
                    for (k in 1..2) {
                        drawLine(c, Offset(size.width * k / 3, 0f), Offset(size.width * k / 3, size.height), 1.dp.toPx())
                        drawLine(c, Offset(0f, size.height * k / 3), Offset(size.width, size.height * k / 3), 1.dp.toPx())
                    }
                }
                if (count > 0) Text(count.toString(), color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectedChoice(PhotoStore.POSES, pose) { pose = it }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Sem timer", 3 to "3 s", 10 to "10 s").forEach { (s, l) ->
                    FilterChip(
                        selected = timer == s, onClick = { timer = s }, label = { Text(l) },
                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White),
                    )
                }
            }
            if (prev != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Guia", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(12.dp))
                Slider(ghost, { ghost = it }, valueRange = 0f..0.7f, modifier = Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LargeFloatingActionButton(onClick = {
                    if (busy || !granted) return@LargeFloatingActionButton
                    busy = true
                    if (timer == 0) shootNow()
                    else scope.launch {
                        for (n in timer downTo 1) { count = n; delay(1000) }
                        count = 0
                        shootNow()
                    }
                }) { Icon(Icons.Rounded.CameraAlt, "Tirar foto", Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
