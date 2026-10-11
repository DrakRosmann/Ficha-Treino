package app.ficha.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ficha.billing.Billing
import app.ficha.data.AppData
import app.ficha.ui.components.Pill
import app.ficha.ui.components.ShapeIcon

/** Boas-vindas: só na primeira vez que o app abre (quem já tem fichas ou treinos não vê). */
object Welcome {
    private const val KEY = "welcome_done"

    fun needed(context: Context, data: AppData): Boolean {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY, false)) return false
        if (data.routines.isNotEmpty() || data.programs.isNotEmpty() || data.sessions.isNotEmpty()) {
            done(context)
            return false
        }
        return true
    }

    fun done(context: Context) {
        context.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

/** Primeira tela: montar o treino (grátis), montar com IA (assinatura) ou só conhecer o app. */
@Composable
fun WelcomeScreen(onBuild: () -> Unit, onAi: () -> Unit, onSkip: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hasAi = Billing.state.ai
    Surface(color = cs.surface, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Enter(0) { Hero() }
                Spacer(Modifier.height(28.dp))
                Enter(1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bem-vindo ao Ficha", style = MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Seu treino, sua dieta e sua evolução no mesmo lugar. Como você quer começar?",
                            style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Enter(2) {
                    Option(
                        icon = Icons.Rounded.Checklist, title = "Montar meu treino",
                        text = "Responda algumas perguntas sobre objetivo, dias e equipamentos e escolha entre 3 programas. Grátis.",
                        container = cs.primaryContainer, content = cs.onPrimaryContainer, accent = cs.primary, onAccent = cs.onPrimary,
                        onClick = onBuild,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Enter(3) {
                    Option(
                        icon = Icons.Rounded.AutoAwesome, title = "Montar com IA",
                        text = if (hasAi) "Você já tem o Ficha IA: conte o que quer e a IA monta o seu programa."
                        else "A IA monta o treino a partir do que você escrever, analisa a sua semana e ajuda na dieta.",
                        container = cs.tertiaryContainer, content = cs.onTertiaryContainer, accent = cs.tertiary, onAccent = cs.onTertiary,
                        badge = if (hasAi) "Seu plano" else "Ficha IA",
                        onClick = onAi,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Enter(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedButton(onClick = onSkip, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("Continuar sem montar treino", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Seus dados ficam salvos no aparelho. Dá para montar o treino depois, na aba Fichas.",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** Halteres sobre formas do Material Expressive que giram devagar. */
@Composable
private fun Hero() {
    val cs = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "boas-vindas")
    val a by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "giro")
    val b by spin.animateFloat(1f, 1.06f, infiniteRepeatable(tween(2_400), RepeatMode.Reverse), label = "pulso")
    Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(196.dp).graphicsLayer { rotationZ = -a / 2; scaleX = b; scaleY = b }
                .clip(MaterialShapes.SoftBurst.toShape()).background(cs.tertiaryContainer.copy(alpha = .7f)),
        )
        Box(
            Modifier.size(140.dp).graphicsLayer { rotationZ = a }
                .clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary),
        )
        Icon(Icons.Rounded.FitnessCenter, null, Modifier.size(64.dp), tint = cs.onPrimary)
    }
}

@Composable
private fun Option(
    icon: ImageVector,
    title: String,
    text: String,
    container: Color,
    content: Color,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
    badge: String? = null,
) {
    Surface(
        onClick = onClick, color = container, contentColor = content, shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeIcon(icon, shape = MaterialShapes.Cookie9Sided.toShape(), size = 56.dp, container = accent, content = onAccent)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.weight(1f, fill = false))
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Pill(badge, accent, onAccent)
                    }
                }
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

/** Entrada em sequência: cada bloco aparece um pouco depois do anterior. */
@Composable
private fun Enter(i: Int, content: @Composable ColumnScope.() -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        state,
        enter = fadeIn(tween(420, delayMillis = 90 * i)) + slideInVertically(tween(520, delayMillis = 90 * i)) { it / 3 },
        content = { Column(horizontalAlignment = Alignment.CenterHorizontally, content = content) },
    )
}
