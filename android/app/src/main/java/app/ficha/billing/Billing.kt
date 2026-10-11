package app.ficha.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ficha.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class Plan { Free, Supporter, Ai }

/** O plano ativo. [aiToken] é o token da compra do Ficha IA, que o servidor confere no Google Play. */
data class Entitlement(val plan: Plan = Plan.Free, val aiToken: String? = null) {
    val noAds get() = plan != Plan.Free
    val ai get() = plan == Plan.Ai && aiToken != null
}

/** Um plano básico de uma assinatura (mensal, anual…) com o preço da Play Store. */
class Offer(val productId: String, val price: String, val period: String, val trialDays: Int, val offerToken: String, val details: ProductDetails)

/**
 * Assinaturas pelo Google Play: Apoiador (sem anúncios) e Ficha IA (IA + sem anúncios).
 * O plano fica guardado no aparelho para abrir certo mesmo sem internet; a Play Store confirma a cada abertura.
 */
object Billing : PurchasesUpdatedListener {
    const val SUPPORTER = "ficha_apoio"
    const val AI = "ficha_ia"

    /** Token aceito pelo servidor local de testes (DEV_TOKENS); só na build de debug. */
    private const val DEBUG_TOKEN = "dev-token"

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var client: BillingClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var purchases: List<Purchase> = emptyList()

    var state by mutableStateOf(Entitlement())
        private set

    /** Preços da Play Store por produto; vazio enquanto carrega ou se a loja não estiver disponível. */
    var offers by mutableStateOf<Map<String, List<Offer>>>(emptyMap())
        private set

    /** null: conectando; false: sem Play Store / sem conta Google neste aparelho. */
    var available by mutableStateOf<Boolean?>(null)
        private set

    /** Pagamento aguardando aprovação (boleto, Pix…). */
    var pending by mutableStateOf(false)
        private set

    /** Só na build de debug: simula um plano para testar as telas e o servidor local. */
    var debugPlan by mutableStateOf<Plan?>(null)
        private set

    private val messages = Channel<String>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("plano", Context.MODE_PRIVATE)
        if (BuildConfig.DEBUG) debugPlan = prefs.getString("debug", null)?.let { runCatching { Plan.valueOf(it) }.getOrNull() }
        real = Entitlement(
            runCatching { Plan.valueOf(prefs.getString("plan", "Free")!!) }.getOrDefault(Plan.Free),
            prefs.getString("token", null),
        )
        publish()
        client = BillingClient.newBuilder(app)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                available = r.responseCode == BillingClient.BillingResponseCode.OK
                if (available == true) scope.launch { loadOffers(); refresh() }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private var real = Entitlement()

    private fun publish() {
        val d = debugPlan
        state = if (d != null) Entitlement(d, if (d == Plan.Ai) DEBUG_TOKEN else null) else real
    }

    fun simulate(p: Plan?) {
        if (!BuildConfig.DEBUG) return
        debugPlan = p
        prefs.edit().apply { if (p == null) remove("debug") else putString("debug", p.name) }.apply()
        publish()
    }

    /** Confere as assinaturas ativas na Play Store (ao abrir o app e depois de comprar). */
    fun refresh() {
        if (available != true) return
        scope.launch {
            val r = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
            if (r.billingResult.responseCode == BillingClient.BillingResponseCode.OK) handle(r.purchasesList)
        }
    }

    private suspend fun loadOffers() {
        val products = listOf(SUPPORTER, AI).map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }
        val r = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build())
        offers = r.productDetailsList.orEmpty().associate { pd ->
            pd.productId to pd.subscriptionOfferDetails.orEmpty()
                .groupBy { it.basePlanId }
                // Por plano básico: a oferta com teste grátis (a Play só manda as que a pessoa pode usar) ou o preço normal
                .map { (_, list) -> list.maxBy { o -> o.pricingPhases.pricingPhaseList.count { it.priceAmountMicros == 0L } } }
                .map { o ->
                    val phases = o.pricingPhases.pricingPhaseList
                    val base = phases.last()
                    val trial = phases.firstOrNull { it.priceAmountMicros == 0L }?.billingPeriod
                    Offer(pd.productId, base.formattedPrice, base.billingPeriod, trial?.let(::days) ?: 0, o.offerToken, pd)
                }
                .sortedBy { days(it.period) }
        }
    }

    private suspend fun handle(list: List<Purchase>) {
        val done = list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        pending = list.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        for (p in done) if (!p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
        purchases = done
        val ai = done.firstOrNull { AI in it.products }
        val sup = done.firstOrNull { SUPPORTER in it.products }
        real = when {
            ai != null -> Entitlement(Plan.Ai, ai.purchaseToken)
            sup != null -> Entitlement(Plan.Supporter)
            else -> Entitlement()
        }
        prefs.edit().putString("plan", real.plan.name).putString("token", real.aiToken).apply()
        publish()
    }

    override fun onPurchasesUpdated(r: BillingResult, list: MutableList<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val before = real.plan
                // A lista da compra traz só o que acabou de mudar: confere tudo de novo
                val q = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
                handle(if (q.billingResult.responseCode == BillingClient.BillingResponseCode.OK) q.purchasesList else list.orEmpty())
                when {
                    pending -> messages.send("Pagamento em análise — o plano ativa assim que for aprovado")
                    real.plan != before && real.plan == Plan.Ai -> messages.send("Ficha IA ativado. Obrigado por apoiar! ✨")
                    real.plan != before && real.plan == Plan.Supporter -> messages.send("Obrigado pelo apoio! Os anúncios foram removidos ❤️")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> { refresh(); messages.trySend("Você já tem este plano") }
            else -> messages.trySend("Não foi possível concluir a compra (${r.debugMessage.ifBlank { "código ${r.responseCode}" }})")
        }
    }

    /** Abre a compra da Play Store. Trocar de plano mantém o tempo já pago (proporcional). */
    fun buy(activity: Activity, offer: Offer) {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(offer.details).setOfferToken(offer.offerToken)
        val params = BillingFlowParams.newBuilder()
        purchases.firstOrNull { offer.productId !in it.products && (SUPPORTER in it.products || AI in it.products) }?.let { old ->
            // Subir para a IA: cobra a diferença agora. Voltar para Apoiador: vale na próxima renovação.
            val mode = if (offer.productId == AI) BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.ReplacementMode.CHARGE_PRORATED_PRICE
            else BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.ReplacementMode.DEFERRED
            product.setSubscriptionProductReplacementParams(
                BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.newBuilder()
                    .setOldProductId(old.products.first()).setReplacementMode(mode).build(),
            )
            params.setSubscriptionUpdateParams(BillingFlowParams.SubscriptionUpdateParams.newBuilder().setOldPurchaseToken(old.purchaseToken).build())
        }
        val r = client.launchBillingFlow(activity, params.setProductDetailsParamsList(listOf(product.build())).build())
        if (r.responseCode != BillingClient.BillingResponseCode.OK && r.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            messages.trySend("A Play Store não abriu a compra agora. Tente de novo.")
        }
    }

    /** Página de assinaturas da Play Store (cancelar, trocar a forma de pagamento). */
    fun manage(context: Context, productId: String? = null) {
        val u = "https://play.google.com/store/account/subscriptions" + (productId?.let { "?sku=$it&package=${context.packageName}" } ?: "")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** "P1M" → 30, "P1Y" → 365, "P1W" → 7, "P3D" → 3 (aproximado; só para ordenar e mostrar o teste grátis). */
    fun days(iso: String): Int {
        val m = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(iso) ?: return 0
        val (y, mo, w, d) = m.destructured
        return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
    }

    /** "/mês", "/ano", "/3 meses"… */
    fun perPeriod(iso: String): String = when (iso) {
        "P1W" -> "/semana"
        "P1M" -> "/mês"
        "P3M" -> "/3 meses"
        "P6M" -> "/6 meses"
        "P1Y" -> "/ano"
        else -> "/${days(iso)} dias"
    }
}
