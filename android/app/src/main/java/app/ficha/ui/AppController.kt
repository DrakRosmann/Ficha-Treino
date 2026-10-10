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
}

class ConfirmRequest(val title: String, val text: String, val ok: String, val danger: Boolean, val onOk: () -> Unit)

class PromptRequest(val title: String, val initial: String, val label: String, val hint: String, val ok: String, val onOk: (String) -> Unit)

/** Ações comuns das telas: dados, navegação, diálogos e avisos. */
class AppController(
    val store: Store,
    val backStack: NavBackStack<NavKey>,
    val snackbar: SnackbarHostState,
    val scope: CoroutineScope,
    val context: Context,
) {
    val data: AppData get() = store.value

    init {
        // Dados de antes das conquistas: marca as já cumpridas em silêncio
        if (data.ach == null) store.update { it.achCheck().first }
    }

    /** A última troca de tela foi pela barra de abas (animação diferente). */
    var tabSwitch = false
        private set

    fun update(f: (AppData) -> AppData) {
        store.update(f)
        achSoon()
    }

    private var achJob: Job? = null

    /** Depois das mudanças, confere as conquistas (no meio do treino, o resumo final mostra). */
    private fun achSoon() {
        achJob?.cancel()
        achJob = scope.launch {
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

    fun go(r: Route) {
        tabSwitch = false
        if (backStack.lastOrNull() != r) backStack.add(r)
    }

    fun back() {
        tabSwitch = false
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** A tela [r] ficou sem dados (apagada, treino finalizado): sai dela se ainda for a de cima. */
    fun leave(r: Route) {
        if (backStack.lastOrNull() == r) back()
    }

    /** Volta até a tela [r] se ela estiver na pilha; senão, troca a de cima por ela. */
    fun replace(r: Route) {
        tabSwitch = false
        val i = backStack.indexOf(r)
        if (i >= 0) {
            while (backStack.size > i + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            backStack.add(r)
        }
    }

    fun tab(r: Route) {
        if (backStack.size == 1 && backStack[0] == r) return
        tabSwitch = true
        backStack.add(r)
        while (backStack.size > 1) backStack.removeAt(0)
    }

    var confirmRequest by mutableStateOf<ConfirmRequest?>(null)
    var promptRequest by mutableStateOf<PromptRequest?>(null)

    /** Treino recém-finalizado: a tela da sessão mostra o resumo (com confete) uma vez. */
    var summaryFor by mutableStateOf<String?>(null)

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
