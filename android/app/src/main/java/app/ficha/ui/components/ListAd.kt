package app.ficha.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ficha.ads.Ads
import app.ficha.ads.NativeAdCard
import app.ficha.ui.LocalApp
import app.ficha.ui.Route

/** Um anúncio nativo no meio da lista (só no plano grátis). */
fun LazyListScope.listAd(key: String) {
    if (!Ads.visible()) return
    item(key = "ad-$key") {
        val app = LocalApp.current
        NativeAdCard(onRemove = { app.go(Route.Plans) }, Modifier.padding(horizontal = Gutter, vertical = 8.dp))
    }
}
