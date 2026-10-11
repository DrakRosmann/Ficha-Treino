package app.ficha.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.Arrangement
import app.ficha.ui.Route
import app.ficha.billing.Billing
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ficha.ui.LocalApp
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.max
import kotlin.math.roundToInt

fun online(context: Context): Boolean = context.getSystemService(ConnectivityManager::class.java).activeNetwork != null

/** Reduz a foto (até 1280 px, JPEG) antes de enviar à IA: menos tokens e envio mais rápido. */
fun jpegB64ForAi(b: Bitmap): String {
    val s = minOf(1f, 1280f / max(b.width, b.height))
    val x = if (s < 1f) Bitmap.createScaledBitmap(b, (b.width * s).roundToInt(), (b.height * s).roundToInt(), true) else b
    val out = ByteArrayOutputStream().also { x.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    return Base64.getEncoder().encodeToString(out.toByteArray())
}

/** Convite para o Ficha IA onde há um recurso de IA e a pessoa não assina. */
@Composable
fun AiUpsell(text: String, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null)
                Spacer(Modifier.width(10.dp))
                Text("Recurso do Ficha IA", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { app.go(Route.Plans) }, shapes = ButtonDefaults.shapes()) { Text("Conhecer o Ficha IA") }
        }
    }
}

/** A IA está trabalhando: indicador do Material Expressive e botão de cancelar. */
@Composable
fun AiBusyDialog(title: String, subtitle: String = "Leva alguns segundos", onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { ContainedLoadingIndicator(Modifier.size(72.dp)) },
        title = { Text(title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text(subtitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
    )
}

/** Confere a assinatura e a internet antes de usar a IA. Sem o Ficha IA, [needPlan] mostra os planos. */
fun aiReady(context: Context, toast: (String) -> Unit, needPlan: () -> Unit): String? {
    val t = Billing.state.aiToken
    if (!Billing.state.ai || t == null) { needPlan(); toast("A IA faz parte do plano Ficha IA"); return null }
    if (!online(context)) { toast("Sem internet — a IA precisa de conexão"); return null }
    return t
}
