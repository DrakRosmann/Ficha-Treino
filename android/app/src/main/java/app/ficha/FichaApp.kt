package app.ficha

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.ficha.data.Catalog
import app.ficha.data.Foods
import app.ficha.data.Store
import app.ficha.photos.PhotoStore
import app.ficha.sync.CloudSync
import app.ficha.sync.Reminders
import app.ficha.timer.RestNotifier

class FichaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Catalog.load(this)
        Foods.load(this)
        PhotoStore.init(this)
        store = Store(this)
        RestNotifier.createChannels(this)
        Reminders.createChannel(this)
        CloudSync.init(this, store)
        Reminders.reschedule(this)
        // Ao minimizar ou fechar o app, grava os dados na hora (e envia para a nuvem)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                store.flush()
                CloudSync.onBackground()
                Reminders.reschedule(this@FichaApp)
            }

            override fun onStart(owner: LifecycleOwner) = CloudSync.onForeground()
        })
    }

    companion object {
        lateinit var store: Store
            private set
    }
}
