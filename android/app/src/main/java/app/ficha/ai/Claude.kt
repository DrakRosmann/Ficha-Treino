package app.ficha.ai

import android.content.Context
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.InternalServerException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.helpers.BetaMessageAccumulator
import com.anthropic.models.beta.messages.BetaBase64ImageSource
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral
import com.anthropic.models.beta.messages.BetaContentBlockParam
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/** Erro da IA com mensagem pronta para mostrar ao usuário. */
class AiError(message: String) : Exception(message)

/**
 * Chamadas ao Claude (Anthropic) com a chave do próprio usuário, salva só neste aparelho.
 * Sempre em streaming; com `fallbacks: "default"`, se o modelo recusar o pedido a API tenta de novo
 * num modelo alternativo recomendado.
 */
object Claude {
    const val MODEL = "claude-opus-5-5"
    private const val PREFS = "segredos"
    private const val KEY = "anthropic_key"

    fun key(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun setKey(context: Context, k: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply { if (k.isBlank()) remove(KEY) else putString(KEY, k.trim()) }.apply()
    }

    /** Um pedaço de mensagem: texto ou foto (JPEG em base64). */
    sealed interface Part {
        class Text(val text: String) : Part
        class Image(val jpegBase64: String) : Part
    }

    class Msg(val user: Boolean, val parts: List<Part>)

    /**
     * Envia e espera a resposta completa (texto). [onText] recebe cada pedaço do texto, [onThinking] avisa quando o
     * modelo começa a pensar. Cancelar a coroutine interrompe a conexão.
     */
    suspend fun run(
        key: String,
        system: String,
        messages: List<Msg>,
        effort: String,
        maxTokens: Long,
        schema: JsonObject? = null,
        cache: Boolean = false,
        onText: ((String) -> Unit)? = null,
        onThinking: (() -> Unit)? = null,
    ): String = withContext(Dispatchers.IO) {
        val client = AnthropicOkHttpClient.builder().apiKey(key).maxRetries(1).timeout(Duration.ofMinutes(10)).build()
        try {
            val out = BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.of(effort))
            if (schema != null) {
                out.format(
                    BetaJsonOutputFormat.builder()
                        .schema(BetaJsonOutputFormat.Schema.builder().putAllAdditionalProperties(schema.mapValues { JsonValue.from(toJava(it.value)) }).build())
                        .build(),
                )
            }
            val b = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(maxTokens)
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .system(system)
                .outputConfig(out.build())
            if (cache) b.cacheControl(BetaCacheControlEphemeral.builder().build())
            for (m in messages) {
                val blocks = m.parts.map { p ->
                    when (p) {
                        is Part.Text -> BetaContentBlockParam.ofText(p.text)
                        is Part.Image -> BetaContentBlockParam.ofImage(
                            BetaBase64ImageSource.builder().mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG).data(p.jpegBase64).build(),
                        )
                    }
                }
                b.addMessage(
                    BetaMessageParam.builder()
                        .role(if (m.user) BetaMessageParam.Role.USER else BetaMessageParam.Role.ASSISTANT)
                        .content(BetaMessageParam.Content.ofBetaContentBlockParams(blocks))
                        .build(),
                )
            }
            val acc = BetaMessageAccumulator.create()
            val stream = client.beta().messages().createStreaming(b.build())
            // Cancelou (botão parar, saiu da tela): fecha a conexão para o laço abaixo terminar
            val handle = coroutineContext.job.invokeOnCompletion { if (it != null) runCatching { stream.close() } }
            try {
                stream.use { s ->
                    s.stream().forEach { ev ->
                        acc.accumulate(ev)
                        ev.contentBlockStart().ifPresent { st -> if (st.contentBlock().isThinking()) onThinking?.invoke() }
                        ev.contentBlockDelta().flatMap { it.delta().text() }.ifPresent { onText?.invoke(it.text()) }
                    }
                }
            } finally {
                handle.dispose()
            }
            coroutineContext.ensureActive()
            val msg = acc.message()
            when (msg.stopReason().orElse(null)) {
                BetaStopReason.REFUSAL -> throw AiError("A IA não conseguiu atender este pedido. Tente de outro jeito.")
                BetaStopReason.MAX_TOKENS -> if (schema != null) throw AiError("A resposta ficou incompleta. Tente de novo.")
                else -> {}
            }
            msg.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiError) {
            throw e
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            throw friendly(e)
        } finally {
            client.close()
        }
    }

    /** Erros da API em mensagens para o usuário. */
    private fun friendly(e: Exception): AiError = when (e) {
        is UnauthorizedException -> AiError("Chave da API inválida. Confira a chave em Ajustes → Inteligência artificial.")
        is PermissionDeniedException -> AiError("Esta chave não tem permissão para usar o modelo. Confira sua conta na Anthropic.")
        is RateLimitException -> AiError("Limite de uso atingido. Espere um pouco e tente de novo.")
        is BadRequestException -> AiError("A Anthropic recusou a solicitação: ${e.message}")
        is InternalServerException -> AiError("O serviço da IA está ocupado agora. Tente de novo em instantes.")
        is AnthropicServiceException -> AiError("Erro da IA (${e.statusCode()}): ${e.message}")
        is AnthropicIoException -> AiError("Sem conexão com a internet (a IA precisa de internet).")
        else -> AiError("Não foi possível usar a IA agora (${e.javaClass.simpleName}).")
    }

    /** JSON do kotlinx → Map/List/valores do Java, para o JsonValue do SDK. */
    private fun toJava(e: JsonElement): Any? = when (e) {
        is JsonNull -> null
        is JsonObject -> e.mapValues { toJava(it.value) }
        is JsonArray -> e.map { toJava(it) }
        is JsonPrimitive -> when {
            e.isString -> e.content
            e.booleanOrNull != null -> e.booleanOrNull
            e.longOrNull != null -> e.longOrNull
            else -> e.doubleOrNull
        }
    }
}
