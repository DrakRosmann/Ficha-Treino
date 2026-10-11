package app.ficha.ui

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.ficha.data.AppData
import app.ficha.data.Store
import app.ficha.logic.ACH_BY_ID
import app.ficha.logic.achCheck
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Telas do app. As abas são as raízes da pilha; o resto empilha por cima. */
@Serializable
sealed interface Route : NavKey {
    @Serializable data object Today : Route
    @Serializable data object Routines : Route
    @Serializable data object History : Route
    @Serializable data object Settings : Route
    @Serializable data class Program(val id: String) : Route
    @Serializable data class Routine(val id: String) : Route
    @Serializable data object Templates : Route
    @Serializable data class Template(val id: String) : Route
    @Serializable data object Exercises : Route
    @Serializable data class Exercise(val id: String) : Route
    @Serializable data class Session(val id: String) : Route
    @Serializable data object Workout : Route
    @Serializable data object Diet : Route
    @Serializable data object Body : Route
    @Serializable data object Photos : Route
    @Serializable data class Camera(val t: Long, val pose: String) : Route
    @Serializable data object Assistant : Route
    @Serializable data object Coach : Route
    @Serializable data object Achievements : Route
    @Serializable data object Cloud : Route
    @Serializable data object Plans : Route
}

class ConfirmRequest(val title: String, val text: String, val ok: String, val danger: Boolean, val onOk: () -> Unit)

class PromptRequest(val title: String, val initial: String, val label: String, val hint: String, val ok: String, val onOk: (String) -> Unit)

/** Estado compartilhado entre o controle da raiz e os de cada tela. */
private class UiState {
    var tabSwitch = false
    var achJob: Job? = null
    var confirmRequest by mutableStateOf<ConfirmRequest?>(null)
    var promptRequest by mutableStateOf<PromptRequest?>(null)
    var summaryFor by mutableStateOf<String?>(null)
}

/**
 * Ações comuns das telas: dados, navegação, diálogos e avisos.
 * Cada tela recebe uma cópia com a sua rota em [origin]: em dois painéis, navegar a partir do
 * painel da esquerda troca o da direita em vez de empilhar por cima dele.
 */
class AppController private constructor(
    val store: Store,
    val backStack: NavBackStack<NavKey>,
    val snackbar: SnackbarHostState,
    val scope: CoroutineScope,
    val context: Context,
    private val ui: UiState,
    private val origin: NavKey?,
) {
    constructor(store: Store, backStack: NavBackStack<NavKey>, snackbar: SnackbarHostState, scope: CoroutineScope, context: Context) :
        this(store, backStack, snackbar, scope, context, UiState(), null) {
        // Dados de antes das conquistas: marca as já cumpridas em silêncio
        if (data.ach == null) store.update { it.achCheck().first }
    }

    /** O controle usado pela tela [key]. */
    fun scoped(key: NavKey) = AppController(store, backStack, snackbar, scope, context, ui, key)

    val data: AppData get() = store.value

    /** A última troca de tela foi pela barra de abas (animação diferente). */
    val tabSwitch: Boolean get() = ui.tabSwitch

    fun update(f: (AppData) -> AppData) {
        store.update(f)
        achSoon()
    }

    /** Depois das mudanças, confere as conquistas (no meio do treino, o resumo final mostra). */
    private fun achSoon() {
        ui.achJob?.cancel()
        ui.achJob = scope.launch {
            delay(1200)
            if (data.active != null) return@launch
            val (nd, fresh) = data.achCheck()
            if (nd === data) return@launch
            store.update { nd }
            if (fresh.isNotEmpty()) {
                val a = ACH_BY_ID.getValue(fresh[0])
                toast(if (fresh.size > 1) "${a.e} ${fresh.size} conquistas novas! Veja no Histórico" else "${a.e} Conquista: ${a.n}")
            }
        }
    }

    /** Tira da pilha o que está acima da tela que pediu a navegação (no celular, ela já é a de cima). */
    private fun trimToOrigin() {
        val i = origin?.let { backStack.lastIndexOf(it) } ?: return
        if (i >= 0) while (backStack.size > i + 1) backStack.removeAt(backStack.lastIndex)
    }

    fun go(r: Route) {
        ui.tabSwitch = false
        trimToOrigin()
        if (backStack.lastOrNull() != r) backStack.add(r)
    }

    fun back() {
        ui.tabSwitch = false
        trimToOrigin()
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** A tela [r] ficou sem dados (apagada, treino finalizado): sai dela se ainda estiver à vista. */
    fun leave(r: Route) {
        if (origin == r) trimToOrigin()
        if (backStack.lastOrNull() == r) back()
    }

    /** Volta até a tela [r] se ela estiver na pilha; senão, troca a de cima por ela. */
    fun replace(r: Route) {
        ui.tabSwitch = false
        val i = backStack.indexOf(r)
        if (i >= 0) {
            while (backStack.size > i + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            trimToOrigin()
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            backStack.add(r)
        }
    }

    fun tab(r: Route) {
        if (backStack.size == 1 && backStack[0] == r) return
        ui.tabSwitch = true
        backStack.add(r)
        while (backStack.size > 1) backStack.removeAt(0)
    }

    var confirmRequest by ui::confirmRequest
    var promptRequest by ui::promptRequest

    /** Treino recém-finalizado: a tela da sessão mostra o resumo (com confete) uma vez. */
    var summaryFor by ui::summaryFor

    fun confirm(title: String, text: String = "", ok: String = "Confirmar", danger: Boolean = false, onOk: () -> Unit) {
        confirmRequest = ConfirmRequest(title, text, ok, danger, onOk)
    }

    fun prompt(title: String, initial: String = "", label: String = "", hint: String = "", ok: String = "Salvar", onOk: (String) -> Unit) {
        promptRequest = PromptRequest(title, initial, label, hint, ok, onOk)
    }

    /** Começa um treino; se já houver um em andamento, pergunta antes de descartar. */
    fun startWorkout(f: (AppData) -> AppData) {
        val cur = data.active
        if (cur != null) {
            confirm("Já existe um treino em andamento", "Deseja descartar “${cur.name}” e começar outro?", "Descartar e começar", danger = true) {
                update { f(it.copy(active = null)) }
                go(Route.Workout)
            }
        } else {
            update(f)
            go(Route.Workout)
        }
    }

    fun toast(msg: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    fun toastAction(msg: String, action: String, onAction: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(msg, action, withDismissAction = false, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalApp = staticCompositionLocalOf<AppController> { error("AppController ausente") }

/** A Activity por trás de um Context da interface (compras e anúncios precisam dela). */
fun android.content.Context.findActivity(): android.app.Activity? {
    var c = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}
