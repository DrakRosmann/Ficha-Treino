package app.ficha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.ficha.sync.Reminders
import app.ficha.timer.RestNotifier
import app.ficha.ui.FichaRoot
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { FichaRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(Reminders.EXTRA_ROUTE)?.let {
            intent.removeExtra(Reminders.EXTRA_ROUTE)
            openRoute.value = "$it#${System.nanoTime()}"
        }
        if (intent?.getBooleanExtra(RestNotifier.EXTRA_OPEN_WORKOUT, false) == true) {
            intent.removeExtra(RestNotifier.EXTRA_OPEN_WORKOUT)
            openWorkout.value++
        }
    }

    companion object {
        /** Toque na notificação do treino: a tela do treino abre (o valor muda a cada toque). */
        val openWorkout = MutableStateFlow(0)

        /** Toque num lembrete: abre a aba dele ("dieta#…"; o sufixo faz cada toque contar). */
        val openRoute = MutableStateFlow<String?>(null)
    }
}
