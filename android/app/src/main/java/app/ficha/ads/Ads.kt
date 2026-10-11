package app.ficha.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.ficha.BuildConfig
import app.ficha.billing.Billing
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Anúncios do plano grátis (AdMob), pensados para não atrapalhar: banner no rodapé, um card nativo nas listas e um
 * vídeo opcional que dá 24 h sem anúncios. Nunca no treino nem na câmera, e nada no primeiro dia de uso.
 * Antes de carregar qualquer anúncio, pede o consentimento onde a lei exige (UMP: GDPR/LGPD).
 */
object Ads {
    /** Primeiro dia sem anúncios: a pessoa conhece o app antes. */
    private const val GRACE_MS = 24 * 3600_000L

    /** O prêmio do vídeo. */
    const val REWARD_MS = 24 * 3600_000L

    private lateinit var prefs: SharedPreferences
    private var firstRun = 0L
    private var starting = false
    private lateinit var consent: ConsentInformation

    /** SDK iniciado e consentimento resolvido: pode pedir anúncios. */
    var ready by mutableStateOf(false)
        private set

    /** Fim do período sem anúncios ganho com o vídeo. */
    var adFreeUntil by mutableLongStateOf(0L)
        private set

    /** A lei pede um botão para rever o consentimento (Ajustes → Privacidade dos anúncios). */
    var privacyRequired by mutableStateOf(false)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("anuncios", Context.MODE_PRIVATE)
        firstRun = prefs.getLong("first", 0L).takeIf { it > 0 } ?: System.currentTimeMillis().also { prefs.edit().putLong("first", it).apply() }
        adFreeUntil = prefs.getLong("free_until", 0L)
        consent = UserMessagingPlatform.getConsentInformation(context)
    }

    /** Mostrar anúncios agora? */
    fun visible(now: Long = System.currentTimeMillis()): Boolean =
        ready && !Billing.state.noAds && now >= adFreeUntil && now - firstRun >= GRACE_MS

    /** Consentimento e início do SDK (uma vez; só para quem está no plano grátis). */
    fun start(activity: Activity) {
        if (starting || ready || Billing.state.noAds) return
        starting = true
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                begin(activity)
            }
        }, { begin(activity) })
        // Mesmo antes de a resposta chegar, a escolha da última vez já vale
        if (consent.canRequestAds()) begin(activity)
    }

    private fun begin(context: Context) {
        if (ready || !consent.canRequestAds()) return
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context.applicationContext)
            launch(Dispatchers.Main) { ready = true }
        }
    }

    fun privacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) {}

    /** Vídeo com prêmio: carrega e mostra; [done] recebe se ganhou as 24 h. */
    fun showRewarded(activity: Activity, done: (Boolean, String?) -> Unit) {
        RewardedAd.load(activity, BuildConfig.AD_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                var earned = false
                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() = done(earned, null)
                    override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) = done(false, "O vídeo não abriu. Tente de novo.")
                }
                ad.show(activity) {
                    earned = true
                    adFreeUntil = System.currentTimeMillis() + REWARD_MS
                    prefs.edit().putLong("free_until", adFreeUntil).apply()
                }
            }

            override fun onAdFailedToLoad(e: LoadAdError) = done(false, "Nenhum vídeo disponível agora. Tente mais tarde.")
        })
    }
}

/**
 * Banner fixo no rodapé, discreto: até 600 dp de largura e 60 dp de altura (centralizado em telas largas).
 * Só ocupa espaço depois que o anúncio carrega.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val w = minOf(maxWidth.value, 600f).toInt()
        // Altura real só se sabe depois de carregar (o adaptável escolhe até 60 dp)
        var height by remember(w) { mutableStateOf(0) }
        val size = remember(w) { AdSize.getInlineAdaptiveBannerAdSize(w, 60) }
        val view = remember(w) {
            AdView(context).apply {
                adUnitId = BuildConfig.AD_BANNER
                setAdSize(size)
                adListener = object : AdListener() {
                    override fun onAdLoaded() { height = adSize?.height?.coerceIn(0, 60) ?: 50 }
                    override fun onAdFailedToLoad(e: LoadAdError) { Log.i("FichaAds", "banner: ${e.message}") }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(view) { onDispose { view.destroy() } }
        AndroidView({ view }, Modifier.width(w.dp).height(height.dp))
    }
}

/**
 * Um card de anúncio nativo no meio da lista, com o visual do app: imagem, título, texto e botão.
 * Some se não houver anúncio. [onRemove] abre os planos.
 */
@Composable
fun NativeAdCard(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var alive = true
        AdLoader.Builder(context, BuildConfig.AD_NATIVE)
            .forNativeAd { a -> if (alive) ad = a else a.destroy() }
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
            .loadAd(AdRequest.Builder().build())
        onDispose { alive = false; ad?.destroy() }
    }
    val a = ad ?: return
    val cs = MaterialTheme.colorScheme
    val colors = NativeColors(cs.onSurface, cs.onSurfaceVariant, cs.primary, cs.onPrimary, cs.tertiaryContainer, cs.onTertiaryContainer)
    Surface(color = cs.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Box {
            AndroidView(
                factory = { buildNativeView(it, colors) },
                update = { it.bind(a) },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 44.dp),
            )
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRemove) { Text("Remover anúncios") }
            }
        }
    }
}

private class NativeColors(val title: Color, val text: Color, val button: Color, val onButton: Color, val badge: Color, val onBadge: Color)

private class NativeHolder(val root: NativeAdView, val media: MediaView, val title: TextView, val body: TextView, val cta: Button)

private fun dp(context: Context, v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).roundToInt()

private fun buildNativeView(context: Context, c: NativeColors): NativeAdView {
    val root = NativeAdView(context)
    val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val media = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(context, 120), dp(context, 120))
        clipToOutline = true
        background = GradientDrawable().apply { cornerRadius = dp(context, 16).toFloat(); setColor(c.badge.toArgb()) }
    }
    val col = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(context, 14) }
    }
    val badge = TextView(context).apply {
        text = "Anúncio"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(c.onBadge.toArgb())
        setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2))
        background = GradientDrawable().apply { cornerRadius = dp(context, 8).toFloat(); setColor(c.badge.toArgb()) }
    }
    val title = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(c.title.toArgb()); typeface = Typeface.DEFAULT_BOLD
        maxLines = 2; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(context, 6), 0, 0)
    }
    val body = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setTextColor(c.text.toArgb()); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        setPadding(0, dp(context, 2), 0, dp(context, 8))
    }
    val cta = Button(context).apply {
        isAllCaps = false; setTextColor(c.onButton.toArgb()); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        minHeight = dp(context, 40); minimumHeight = dp(context, 40); stateListAnimator = null
        setPadding(dp(context, 18), 0, dp(context, 18), 0)
        background = GradientDrawable().apply { cornerRadius = dp(context, 20).toFloat(); setColor(c.button.toArgb()) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 40))
    }
    col.addView(badge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    col.addView(title); col.addView(body); col.addView(cta)
    row.addView(media); row.addView(col)
    root.addView(row)
    root.mediaView = media; root.headlineView = title; root.bodyView = body; root.callToActionView = cta
    root.tag = NativeHolder(root, media, title, body, cta)
    return root
}

private fun NativeAdView.bind(ad: NativeAd) {
    val h = tag as NativeHolder
    h.title.text = ad.headline
    h.body.text = ad.body
    h.body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    h.cta.text = ad.callToAction ?: "Saiba mais"
    h.cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    h.media.mediaContent = ad.mediaContent
    setNativeAd(ad)
}
