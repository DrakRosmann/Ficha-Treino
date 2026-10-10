package app.ficha.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Diálogos de confirmação e de texto pedidos pelas telas (app.confirm / app.prompt). */
@Composable
fun AppDialogs(app: AppController) {
    app.confirmRequest?.let { r ->
        AlertDialog(
            onDismissRequest = { app.confirmRequest = null },
            title = { Text(r.title) },
            text = if (r.text.isNotEmpty()) ({ Text(r.text) }) else null,
            confirmButton = {
                TextButton(
                    onClick = { app.confirmRequest = null; r.onOk() },
                    colors = if (r.danger) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors(),
                ) { Text(r.ok) }
            },
            dismissButton = { TextButton(onClick = { app.confirmRequest = null }) { Text("Cancelar") } },
        )
    }
    app.promptRequest?.let { r ->
        var v by remember(r) { mutableStateOf(TextFieldValue(r.initial, TextRange(0, r.initial.length))) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(r) { focus.requestFocus() }
        AlertDialog(
            onDismissRequest = { app.promptRequest = null },
            title = { Text(r.title) },
            text = {
                OutlinedTextField(
                    value = v, onValueChange = { v = it }, singleLine = true,
                    label = if (r.label.isNotEmpty()) ({ Text(r.label) }) else null,
                    placeholder = if (r.hint.isNotEmpty()) ({ Text(r.hint) }) else null,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            },
            confirmButton = { TextButton(onClick = { app.promptRequest = null; r.onOk(v.text) }) { Text(r.ok) } },
            dismissButton = { TextButton(onClick = { app.promptRequest = null }) { Text("Cancelar") } },
        )
    }
}
