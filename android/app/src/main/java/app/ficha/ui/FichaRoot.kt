package app.ficha.ui

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.ficha.FichaApp
import app.ficha.MainActivity
import app.ficha.data.AppData
import app.ficha.logic.addRest
import app.ficha.logic.clock
import app.ficha.logic.stats
import app.ficha.timer.RestNotifier
import app.ficha.ui.screens.AchievementsScreen
import app.ficha.ui.screens.AssistantScreen
import app.ficha.ui.screens.BodyScreen
import app.ficha.ui.screens.CameraScreen
import app.ficha.ui.screens.CloudScreen
import app.ficha.ui.screens.CoachScreen
import app.ficha.ui.screens.DietScreen
import app.ficha.ui.screens.ExerciseScreen
import app.ficha.ui.screens.PhotosScreen
import app.ficha.ui.screens.ExercisesScreen
import app.ficha.ui.screens.HistoryScreen
import app.ficha.ui.screens.ProgramScreen
import app.ficha.ui.screens.RoutineScreen
import app.ficha.ui.screens.RoutinesScreen
import app.ficha.ui.screens.SessionScreen
import app.ficha.ui.screens.SettingsScreen
import app.ficha.ui.screens.TemplateScreen
import app.ficha.ui.screens.TemplatesScreen
import app.ficha.ui.screens.TodayScreen
import app.ficha.ui.screens.WorkoutScreen
import app.ficha.ui.theme.FichaTheme
import kotlinx.coroutines.delay

private class Tab(val route: Route, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab(Route.Today, "Hoje", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Route.Routines, "Fichas", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Rounded.Assignment),
    Tab(Route.Diet, "Dieta", Icons.Outlined.Restaurant, Icons.Rounded.Restaurant),
    Tab(Route.History, "Histórico", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Tab(Route.Body, "Corpo", Icons.Outlined.Accessibility, Icons.Rounded.Accessibility),
)

/** Hora atual que se atualiza sozinha (cronômetros). */
@Composable
fun rememberNow(intervalMs: Long = 500): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(intervalMs)
    }
}

@Composable
fun FichaRoot() {
    val store = FichaApp.store
    val data = store.value
    FichaTheme(data.settings) {
        val backStack = rememberNavBackStack(Route.Today)
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val app = remember { AppController(store, backStack, snackbar, scope, context) }
        val top = backStack.lastOrNull()
        val inWorkout = top == Route.Workout
        val fullScreen = inWorkout || top is Route.Camera

        CompositionLocalProvider(LocalApp provides app) {
            WorkoutEffects(data, app)
            AppDialogs(app)
            // Na raiz de outra aba, voltar leva para Hoje (como nos apps do Android)
            BackHandler(enabled = backStack.size == 1 && backStack[0] != Route.Today) { app.tab(Route.Today) }

            Scaffold(
                contentWindowInsets = WindowInsets(0),
                snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = if (data.active != null) 88.dp else 0.dp)) },
                bottomBar = {
                    AnimatedVisibility(
                        !fullScreen,
                        enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
                        exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + fadeOut(),
                    ) {
                        ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            val root = backStack.firstOrNull()
                            TABS.forEach { t ->
                                val selected = root == t.route
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = { if (selected && backStack.size > 1) app.replace(t.route) else app.tab(t.route) },
                                    icon = { Icon(if (selected) t.selectedIcon else t.icon, null) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    }
                },
            ) { pad ->
                // A barra de abas já cobre a barra do sistema: as telas não somam esse recuo de novo
                val bottom = PaddingValues(bottom = pad.calculateBottomPadding())
                Box(Modifier.padding(bottom).consumeWindowInsets(bottom).fillMaxSize()) {
                    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    NavDisplay(
                        backStack = backStack,
                        onBack = { app.back() },
                        transitionSpec = {
                            if (app.tabSwitch) fadeIn(effects) togetherWith fadeOut(effects)
                            else (slideInHorizontally(spatial) { it / 4 } + fadeIn(effects)) togetherWith (slideOutHorizontally(spatial) { -it / 8 } + fadeOut(effects))
                        },
                        popTransitionSpec = {
                            (slideInHorizontally(spatial) { -it / 8 } + fadeIn(effects)) togetherWith (slideOutHorizontally(spatial) { it / 4 } + fadeOut(effects))
                        },
                        entryProvider = entryProvider<NavKey> {
                            entry<Route.Today> { TodayScreen(data) }
                            entry<Route.Routines> { RoutinesScreen(data) }
                            entry<Route.History> { HistoryScreen(data) }
                            entry<Route.Settings> { SettingsScreen(data) }
                            entry<Route.Program> { ProgramScreen(data, it.id) }
                            entry<Route.Routine> { RoutineScreen(data, it.id) }
                            entry<Route.Templates> { TemplatesScreen(data) }
                            entry<Route.Template> { TemplateScreen(data, it.id) }
                            entry<Route.Exercises> { ExercisesScreen(data) }
                            entry<Route.Exercise> { ExerciseScreen(data, it.id) }
                            entry<Route.Session> { SessionScreen(data, it.id) }
                            entry<Route.Workout> { WorkoutScreen(data) }
                            entry<Route.Diet> { DietScreen(data) }
                            entry<Route.Body> { BodyScreen(data) }
                            entry<Route.Photos> { PhotosScreen(data) }
                            entry<Route.Camera> { CameraScreen(data, it.t, it.pose) }
                            entry<Route.Assistant> { AssistantScreen(data) }
                            entry<Route.Coach> { CoachScreen(data) }
                            entry<Route.Achievements> { AchievementsScreen(data) }
                            entry<Route.Cloud> { CloudScreen(data) }
                        },
                    )
                    if (top !is Route.Camera) Dock(data, inWorkout, Modifier.align(Alignment.BottomCenter).let { if (inWorkout) it.navigationBarsPadding() else it })
                }
            }
        }
    }
}

/** Notificação, alarme, tela ligada e permissão de notificação enquanto há um treino. */
@Composable
private fun WorkoutEffects(data: AppData, app: AppController) {
    val context = LocalContext.current
    val active = data.active
    val st = active?.stats()
    val key = active?.let { "${it.id}|${it.name}|${it.rest?.end}|${st?.done}/${st?.total}" }
    LaunchedEffect(key, data.settings.sound) { RestNotifier.sync(context, active, data.settings.sound) }

    // Fim do descanso com o app aberto: aviso na tela (o som vem da notificação; sem permissão, toca aqui)
    val rest = active?.rest
    LaunchedEffect(rest?.end) {
        if (rest == null) return@LaunchedEffect
        delay((rest.end - System.currentTimeMillis()).coerceAtLeast(0))
        val cur = app.store.value.active?.rest
        if (cur != null && cur.end == rest.end) {
            app.update { d -> d.active?.let { d.copy(active = it.copy(rest = null)) } ?: d }
            app.toast("Descanso terminado — próxima série!")
            if (!RestNotifier.canNotify(context) && app.data.settings.sound) {
                runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90).startTone(ToneGenerator.TONE_PROP_BEEP2, 600) }
            }
        }
    }

    val view = LocalView.current
    val keepOn = active != null && data.settings.keepAwake
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    // Pede a permissão de notificação no primeiro treino (para o descanso tocar com a tela apagada)
    var asked by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) RestNotifier.sync(context, app.data.active, app.data.settings.sound)
    }
    LaunchedEffect(active?.id) {
        if (active != null && !asked && Build.VERSION.SDK_INT >= 33 && !RestNotifier.canNotify(context)) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val open by MainActivity.openWorkout.collectAsStateWithLifecycle()
    LaunchedEffect(open) { if (open > 0 && app.data.active != null) app.go(Route.Workout) }
    val route by MainActivity.openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(route) {
        when (route?.substringBefore('#')) {
            "hoje" -> app.tab(Route.Today)
            "dieta" -> app.tab(Route.Diet)
            "corpo" -> app.tab(Route.Body)
            "ajustes" -> app.go(Route.Settings)
        }
    }
}

/** Barra flutuante do descanso e do treino em andamento (acima da barra de abas). */
@Composable
private fun Dock(data: AppData, inWorkout: Boolean, modifier: Modifier) {
    val app = LocalApp.current
    val a = data.active
    val now by rememberNow(250)
    val rest = a?.rest?.takeIf { it.end > now }
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(
            rest != null,
            enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
            exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + fadeOut(),
        ) {
            val r = rest ?: a?.rest
            if (r != null) {
                val left = ((r.end - now) / 1000.0).coerceAtLeast(0.0)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.extraLarge, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(
                                progress = { (left / r.total.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .18f),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Descanso", style = MaterialTheme.typography.labelMedium)
                            Text(clock(left), style = MaterialTheme.typography.headlineSmallEmphasized.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold)
                        }
                        val small = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraSmallContainerHeight)
                        FilledTonalButton(onClick = { app.update { d -> d.copy(active = d.active?.addRest(-15)) } }, contentPadding = small, shapes = ButtonDefaults.shapes()) { Text("−15") }
                        Spacer(Modifier.width(4.dp))
                        FilledTonalButton(onClick = { app.update { d -> d.copy(active = d.active?.addRest(15)) } }, contentPadding = small, shapes = ButtonDefaults.shapes()) { Text("+15") }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = { app.update { d -> d.copy(active = d.active?.copy(rest = null)) } }, contentPadding = small, shapes = ButtonDefaults.shapes()) { Text("Pular") }
                    }
                }
            }
        }
        AnimatedVisibility(
            a != null && !inWorkout,
            enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
            exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + fadeOut(),
        ) {
            if (a != null) {
                Surface(
                    onClick = { app.go(Route.Workout) },
                    color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = MaterialTheme.shapes.extraLarge,
                    shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Treino em andamento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${a.name} · ${clock((now - a.start) / 1000.0)}", style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(onClick = { app.go(Route.Workout) }, shapes = ButtonDefaults.shapes()) { Text("Abrir") }
                    }
                }
            }
        }
    }
}
