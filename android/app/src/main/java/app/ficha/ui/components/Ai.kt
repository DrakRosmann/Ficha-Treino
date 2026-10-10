package app.ficha.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ficha.ai.Claude
import app.ficha.ui.LocalApp
import app.ficha.ui.screens.Sheet
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

/** Chave da API da Anthropic: salva só neste aparelho (fica fora do backup). */
@Composable
fun AiKeySheet(onDismiss: () -> Unit, onSaved: (() -> Unit)? = null) {
    val app = LocalApp.current
    val context = LocalContext.current
    val has = Claude.key(context).isNotEmpty()
    var v by remember { mutableStateOf("") }
    Sheet(onDismiss, "Chave da API da Anthropic") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Para usar a IA, crie uma chave em platform.claude.com (Settings → API keys) e cole aqui. O uso é cobrado pela Anthropic na sua conta — cada geração custa em torno de US$ 0,15 a 0,40.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.claude.com"))) } }) { Text("Abrir platform.claude.com") }
            OutlinedTextField(
                v, { v = it.trim() }, singleLine = true, label = { Text("Chave (começa com sk-ant-)") },
                placeholder = { Text(if (has) "Chave salva — cole outra para trocar" else "sk-ant-…") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "A chave fica salva só neste aparelho e não vai para o backup. Os dados usados pela IA são enviados à Anthropic.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                when {
                    v.isEmpty() -> app.toast("Cole a chave da API")
                    !v.startsWith("sk-ant-") -> app.toast("A chave deve começar com sk-ant-")
                    else -> {
                        Claude.setKey(context, v)
                        app.toast("Chave salva neste aparelho")
                        onDismiss()
                        onSaved?.invoke()
                    }
                }
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Salvar") }
            if (has) OutlinedButton(
                onClick = { Claude.setKey(context, ""); app.toast("Chave removida"); onDismiss() }, shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth(),
            ) { Text("Remover chave") }
            Spacer(Modifier.height(12.dp))
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

/** Confere chave e internet antes de usar a IA. Sem chave, pede a chave. */
fun aiReady(context: Context, toast: (String) -> Unit, askKey: () -> Unit): String? {
    val k = Claude.key(context)
    if (k.isEmpty()) { askKey(); toast("Salve a chave da API e tente de novo"); return null }
    if (!online(context)) { toast("Sem internet — a IA precisa de conexão"); return null }
    return k
}
