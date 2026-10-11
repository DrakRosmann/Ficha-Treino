package app.ficha.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import app.ficha.ui.components.ShapeIcon

/** Largura da janela: celular; celular deitado ou tablet pequeno; dobrável aberto ou tablet. */
enum class WindowWidth { Compact, Medium, Expanded }

val LocalWindowWidth = compositionLocalOf { WindowWidth.Compact }

@Composable
fun rememberWindowWidth(): WindowWidth {
    val w = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return when {
        w >= 840.dp -> WindowWidth.Expanded
        w >= 600.dp -> WindowWidth.Medium
        else -> WindowWidth.Compact
    }
}

/** Largura máxima do conteúdo numa coluna só: em telas largas, mais que isso fica ruim de ler. */
val MaxContentWidth = 720.dp

/** Recuo de cada lado para centralizar o conteúdo numa área de largura [width]. */
fun sideGap(width: Dp): Dp = ((width - MaxContentWidth) / 2).coerceAtLeast(0.dp)

/** O que mostrar no painel da direita enquanto nada da lista foi escolhido. */
class EmptyPane(val icon: ImageVector, val title: String, val text: String)

/** Metadados de uma tela que pode dividir a tela em dois painéis. */
class Pane(val empty: EmptyPane? = null) {
    companion object {
        const val KEY = "ficha.pane"

        /** A rota da tela, para a lista saber qual item está aberto ao lado. */
        const val ROUTE = "ficha.route"
    }
}

/** Na lista da esquerda: a tela aberta no painel da direita (para destacar o item dela). */
val LocalOpenDetail = compositionLocalOf<NavKey?> { null }

/**
 * Dois painéis no dobrável aberto e no tablet: a tela de cima fica à direita e a de baixo dela à
 * esquerda, quando as duas aceitam painéis (lista → detalhe → detalhe do detalhe…). Uma lista
 * sozinha mostra um aviso à direita. O resto continua em tela cheia.
 */
class TwoPaneStrategy(private val enabled: Boolean) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!enabled) return null
        val top = entries.last()
        val pane = top.metadata[Pane.KEY] as? Pane ?: return null
        val below = entries.getOrNull(entries.lastIndex - 1)
        return when {
            below?.metadata?.get(Pane.KEY) is Pane -> TwoPaneScene(below, top, null, entries.dropLast(1))
            pane.empty != null -> TwoPaneScene(top, null, pane.empty, entries.dropLast(1))
            else -> null
        }
    }
}

internal class TwoPaneScene(
    val list: NavEntry<NavKey>,
    val detail: NavEntry<NavKey>?,
    val empty: EmptyPane?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    // A mesma lista à esquerda é a mesma cena: trocar o detalhe não anima a tela inteira
    override val key: Any = "2:${list.contentKey}"
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)

    override val content: @Composable () -> Unit = {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val listWidth = (maxWidth * .44f).coerceIn(320.dp, 440.dp)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(listWidth).fillMaxHeight()) {
                    CompositionLocalProvider(LocalOpenDetail provides detail?.metadata?.get(Pane.ROUTE) as? NavKey) { list.Content() }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    key(detail?.contentKey) {
                        Appear {
                            if (detail != null) detail.Content()
                            else if (empty != null) EmptyPaneContent(empty)
                        }
                    }
                }
            }
        }
    }

    override fun equals(other: Any?) =
        other is TwoPaneScene && other.key == key && other.entries == entries && other.previousEntries == previousEntries

    override fun hashCode() = (key.hashCode() * 31 + entries.hashCode()) * 31 + previousEntries.hashCode()
}

/** Entrada suave do conteúdo novo do painel da direita. */
@Composable
private fun Appear(content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(Unit) { p.animateTo(1f, spec) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = p.value; translationX = (1 - p.value) * 24.dp.toPx() }) { content() }
}

@Composable
private fun EmptyPaneContent(e: EmptyPane) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ShapeIcon(
                e.icon, shape = MaterialShapes.Cookie9Sided.toShape(), size = 96.dp,
                container = MaterialTheme.colorScheme.secondaryContainer, content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(20.dp))
            Text(e.title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(e.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
