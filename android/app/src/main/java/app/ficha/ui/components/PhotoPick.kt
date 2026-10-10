package app.ficha.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.ficha.photos.PhotoStore
import kotlinx.coroutines.launch
import java.io.File

/** Tirar foto com a câmera do sistema ou escolher da galeria; devolve a imagem já virada certo. */
class PhotoPicker(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberPhotoPicker(onError: (String) -> Unit = {}, onBitmap: (Bitmap) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val deliver = { uri: Uri ->
        scope.launch {
            val b = PhotoStore.decode(context, uri)
            if (b != null) onBitmap(b) else onError("Não foi possível abrir a foto")
        }
    }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pending?.let(Uri::parse)
        if (ok && u != null) deliver(u)
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) deliver(uri) }
    val launchCamera = {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, "foto-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
        pending = uri.toString()
        take.launch(uri)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchCamera() else onError("Sem permissão da câmera — escolha uma foto da galeria")
    }
    return remember {
        PhotoPicker(
            camera = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
                else perm.launch(Manifest.permission.CAMERA)
            },
            gallery = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}
