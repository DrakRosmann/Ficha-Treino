package app.ficha.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.ui.LocalApp

/** Espaço no fim das listas para o dock (descanso / treino em andamento) não cobrir nada. */
val BottomSpace = 120.dp

/**
 * Tela com barra de título grande do Material Expressive, que encolhe ao rolar.
 * [back] mostra a seta de voltar; o conteúdo é uma LazyColumn.
 */
@Composable
fun Screen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    back: Boolean = false,
    large: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    bottomBar: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val app = LocalApp.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val nav: @Composable () -> Unit = {
        if (back) IconButton(onClick = { app.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") }
    }
    val titleC: @Composable () -> Unit = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    val subC: (@Composable () -> Unit)? = subtitle?.let { s -> { Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (large) LargeFlexibleTopAppBar(title = titleC, subtitle = subC, navigationIcon = nav, actions = actions, scrollBehavior = scroll, colors = colors)
            else MediumFlexibleTopAppBar(title = titleC, subtitle = subC, navigationIcon = nav, actions = actions, scrollBehavior = scroll, colors = colors)
        },
        floatingActionButton = fab,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { pad ->
        Box(Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = BottomSpace + pad.calculateBottomPadding()),
                content = content,
            )
        }
    }
}
