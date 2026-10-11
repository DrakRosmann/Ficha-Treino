package app.ficha.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ficha.BuildConfig
import app.ficha.ads.Ads
import app.ficha.billing.Billing
import app.ficha.billing.Offer
import app.ficha.billing.Plan
import app.ficha.logic.dateShort
import app.ficha.logic.timeHM
import app.ficha.ui.LocalApp
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.Pill
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.ShapeIcon
import app.ficha.ui.findActivity
import app.ficha.ui.rememberNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

private val SUPPORTER_PERKS = listOf(
    "Nenhum anúncio no app",
    "Ajuda a manter o Ficha e a trazer novidades",
)
private val AI_PERKS = listOf(
    "Treinador IA: relatório da semana e conversa sobre treino e dieta",
    "Montar meu treino com IA, entendendo o que você escrever",
    "Dieta por foto do prato, por descrição e leitura de rótulos",
    "Nenhum anúncio no app",
)

@Composable
fun PlansScreen() {
    val app = LocalApp.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val ent = Billing.state
    LaunchedEffect(Unit) { Billing.refresh() }

    Screen(title = "Assinatura", subtitle = "Sem anúncios e IA", back = true) {
        item { Hint("O Ficha é grátis e continua completo sem assinatura: fichas, treinos, histórico, dieta, corpo, fotos, nuvem e lembretes. Os planos tiram os anúncios e liberam a IA.") }
        if (Billing.pending) item { CardBox(color = MaterialTheme.colorScheme.secondaryContainer) { Text("Pagamento em análise. O plano ativa assim que a Play Store aprovar.") } }
        if (Billing.available == false && Billing.debugPlan == null) item {
            CardBox(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text("As assinaturas precisam da Play Store com uma conta Google neste aparelho.")
            }
        }

        item {
            PlanCard(
                icon = Icons.Rounded.AutoAwesome, name = "Ficha IA", tagline = "A IA no seu treino e na sua dieta, sem anúncios",
                perks = AI_PERKS, current = ent.plan == Plan.Ai, offers = Billing.offers[Billing.AI].orEmpty(),
                container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer,
                onBuy = { o -> activity?.let { Billing.buy(it, o) } }, onManage = { Billing.manage(context, Billing.AI) },
            ) { if (ent.ai) AiUsage(ent.aiToken!!) }
        }
        item {
            PlanCard(
                icon = Icons.Rounded.Favorite, name = "Apoiador", tagline = "Sem anúncios e o seu apoio ao app",
                perks = SUPPORTER_PERKS, current = ent.plan == Plan.Supporter, offers = Billing.offers[Billing.SUPPORTER].orEmpty(),
                container = MaterialTheme.colorScheme.secondaryContainer, content = MaterialTheme.colorScheme.onSecondaryContainer,
                onBuy = { o -> activity?.let { Billing.buy(it, o) } }, onManage = { Billing.manage(context, Billing.SUPPORTER) },
                included = ent.plan == Plan.Ai,
            )
        }

        if (ent.plan == Plan.Free && Ads.ready && activity != null) {
            item { SectionHeader("Sem anúncios por um dia") }
            item {
                val now by rememberNow(30_000)
                var busy by remember { mutableStateOf(false) }
                CardBox {
                    if (Ads.adFreeUntil > now) {
                        Text("Sem anúncios até ${relDayTime(Ads.adFreeUntil)}. Obrigado por assistir!", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Text("Assista a um vídeo curto e fique 24 horas sem anúncios.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                busy = true
                                Ads.showRewarded(activity) { ok, err ->
                                    busy = false
                                    if (ok) app.toast("Pronto: 24 horas sem anúncios") else err?.let(app::toast)
                                }
                            },
                            enabled = !busy, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Carregando o vídeo…" else "Assistir e tirar os anúncios")
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { Billing.refresh(); app.toast("Conferindo suas assinaturas…") }) { Text("Restaurar compras") }
                TextButton(onClick = { Billing.manage(context) }) { Text("Gerenciar no Google Play") }
            }
        }
        item {
            Hint(
                "As assinaturas são cobradas pela sua conta do Google Play e renovam sozinhas pelo mesmo preço até você cancelar. " +
                    "Cancele quando quiser em Google Play → Assinaturas, até 24 horas antes da renovação; o plano continua até o fim do período pago. " +
                    "Trocar de plano aproveita o tempo já pago. A IA tem um limite de uso justo por mês, que renova todo dia 1º.",
            )
        }

        if (BuildConfig.DEBUG) {
            item { SectionHeader("Teste (só na build de debug)") }
            item {
                CardBox {
                    Text("Simula um plano sem a Play Store. No Ficha IA, a IA usa o servidor local com o token de teste.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    ConnectedChoice(
                        listOf("real" to "Play Store", "Free" to "Grátis", "Supporter" to "Apoiador", "Ai" to "IA"),
                        Billing.debugPlan?.name ?: "real",
                    ) { Billing.simulate(if (it == "real") null else Plan.valueOf(it)) }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    icon: ImageVector,
    name: String,
    tagline: String,
    perks: List<String>,
    current: Boolean,
    offers: List<Offer>,
    container: Color,
    content: Color,
    onBuy: (Offer) -> Unit,
    onManage: () -> Unit,
    included: Boolean = false,
    extra: @Composable () -> Unit = {},
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(icon, shape = MaterialShapes.Cookie9Sided.toShape(), size = 48.dp, container = content, content = container)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.headlineSmallEmphasized)
                    Text(tagline, style = MaterialTheme.typography.bodyMedium)
                }
                if (current) Pill("Seu plano", content, container)
                else if (included) Pill("Incluído", content.copy(alpha = .15f), content)
            }
            perks.forEach { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(20.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(p, style = MaterialTheme.typography.bodyLarge)
                }
            }
            extra()
            when {
                current -> OutlinedButton(onClick = onManage, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Gerenciar assinatura") }
                included -> {}
                offers.isEmpty() -> Text(
                    if (Billing.available == null) "Carregando preços…" else "Preços indisponíveis agora.",
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> offers.forEachIndexed { i, o ->
                    val label = "${o.price}${Billing.perPeriod(o.period)}" + if (o.trialDays > 0) " · ${o.trialDays} dias grátis" else ""
                    if (i == 0) Button(
                        onClick = { onBuy(o) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = content, contentColor = container),
                    ) { Text("Assinar · $label") }
                    else OutlinedButton(onClick = { onBuy(o) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
            }
        }
    }
}

/** Quanto do limite de IA do mês já foi usado (o servidor calcula pelos tokens). */
@Composable
private fun AiUsage(token: String) {
    val used by produceState<Double?>(null, token) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(BuildConfig.SERVER.trimEnd('/') + "/v1/ai/usage").openConnection() as HttpURLConnection
                c.connectTimeout = 10_000; c.readTimeout = 10_000
                c.setRequestProperty("Authorization", "Bearer $token")
                if (c.responseCode != 200) null
                else Json.parseToJsonElement(c.inputStream.bufferedReader().use { it.readText() }).jsonObject["used"]?.jsonPrimitive?.doubleOrNull
            }.getOrNull()
        }
    }
    val u = used ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Uso da IA neste mês: ${(u * 100).toInt().coerceIn(0, 100)}%", style = MaterialTheme.typography.labelLarge)
        LinearWavyProgressIndicator(progress = { u.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun relDayTime(t: Long) = "${dateShort(t)}, ${timeHM(t)}"
