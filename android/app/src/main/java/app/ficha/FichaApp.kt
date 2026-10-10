package app.ficha

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.ficha.data.Catalog
import app.ficha.data.Store
import app.ficha.timer.RestNotifier

class FichaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Catalog.load(this)
        store = Store(this)
        RestNotifier.createChannels(this)
        // Ao minimizar ou fechar o app, grava os dados na hora
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = store.flush()
        })
    }

    companion object {
        lateinit var store: Store
            private set
    }
}
