package app.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Margem lateral das telas. */
val Gutter = 16.dp

/** Título de seção, como nas listas do Android 16. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = Gutter + 4.dp, end = Gutter + 4.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Uma linha de uma lista segmentada (cantos grandes nas pontas, pequenos no meio). */
class Seg(
    val key: Any? = null,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val leading: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val overline: String? = null,
    val supporting: String? = null,
    val supportingColor: Color? = null,
    val headlineColor: Color? = null,
    val headline: String,
)

/** Cor dos itens da lista segmentada; nos painéis (fundo mais claro) fica um tom acima. */
val LocalSegColor = androidx.compose.runtime.compositionLocalOf<Color?> { null }

@Composable
fun SegmentedList(rows: List<Seg>, modifier: Modifier = Modifier.padding(horizontal = Gutter)) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { i, r -> SegRow(r, i, rows.size) }
    }
}

@Composable
fun SegRow(r: Seg, index: Int, count: Int) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val colors = ListItemDefaults.segmentedColors(containerColor = LocalSegColor.current ?: MaterialTheme.colorScheme.surfaceContainer)
    val headline: @Composable () -> Unit = {
        Text(r.headline, maxLines = 2, overflow = TextOverflow.Ellipsis, color = r.headlineColor ?: Color.Unspecified)
    }
    val supporting: (@Composable () -> Unit)? = r.supporting?.let { s ->
        { Text(s, color = r.supportingColor ?: MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    val overline: (@Composable () -> Unit)? = r.overline?.let { s -> { Text(s) } }
    if (r.onClick != null) {
        SegmentedListItem(
            onClick = r.onClick, shapes = shapes, leadingContent = r.leading, trailingContent = r.trailing,
            overlineContent = overline, supportingContent = supporting, onLongClick = r.onLongClick, colors = colors, content = headline,
        )
    } else {
        SegmentedListItem(
            shapes = shapes, leadingContent = r.leading, trailingContent = r.trailing,
            overlineContent = overline, supportingContent = supporting, colors = colors, content = headline,
        )
    }
}

/** Ícone dentro de uma forma do Material Expressive (biscoito, sol, trevo…). */
@Composable
fun ShapeIcon(
    icon: ImageVector? = null,
    text: String? = null,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 40.dp,
) {
    Surface(shape = shape, color = container, contentColor = content, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, Modifier.size(size * 0.5f))
            if (text != null) Text(text, style = MaterialTheme.typography.titleMediumEmphasized, fontWeight = FontWeight.Bold)
        }
    }
}

/** Etiqueta pequena (Pausado, PR, Iniciar…). */
@Composable
fun Pill(
    text: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMediumEmphasized, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), maxLines = 1)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit = {}) {
    Card(
        modifier.fillMaxWidth().padding(horizontal = Gutter),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ShapeIcon(icon, shape = MaterialShapes.Sunny.toShape(), size = 64.dp, container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

/** Três números grandes lado a lado (treinos na semana, no mês…). */
@Composable
fun StatRow(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (value, label) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    Text(value, style = MaterialTheme.typography.headlineSmallEmphasized.copy(fontFeatureSettings = "tnum"), maxLines = 1)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

/** Par chave/valor numa linha (recordes, resumo). */
@Composable
fun KeyValue(key: String, value: String, valueColor: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Rótulo mais apagado que o valor, na cor do fundo em que está (cartão neutro ou colorido)
        Text(key, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = .72f), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum"), color = valueColor)
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = color, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth().padding(horizontal = Gutter)) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Texto pequeno de ajuda abaixo das listas. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier.fillMaxWidth().padding(horizontal = Gutter + 4.dp, vertical = 10.dp),
        lineHeight = 18.sp,
    )
}

@Composable
fun RowScope.Grow(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.weight(1f), content = content)

/** Escolha única como grupo de botões conectados do Material Expressive (substitui o segmentado). */
@Composable
fun ConnectedChoice(options: List<Pair<String, String>>, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (k, label) ->
            androidx.compose.material3.ToggleButton(
                checked = selected == k, onCheckedChange = { onSelect(k) },
                shapes = when (i) {
                    0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.weight(1f),
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
