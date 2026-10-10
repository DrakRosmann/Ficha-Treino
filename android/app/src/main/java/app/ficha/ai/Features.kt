package app.ficha.ai

import app.ficha.data.AppData
import app.ficha.data.Catalog
import app.ficha.data.CoachReportData
import app.ficha.data.Profile
import app.ficha.data.RoutineItem
import app.ficha.data.TemplateRoutine
import app.ficha.data.Store
import app.ficha.data.uid
import app.ficha.logic.ASST_LEVEL_HINT
import app.ficha.logic.AsstOption
import app.ficha.logic.BODY_FIELDS
import app.ficha.logic.DAY
import app.ficha.logic.REST_OPTIONS
import app.ficha.logic.VOL_GROUPS
import app.ficha.logic.VOL_RANGE
import app.ficha.logic.addDays
import app.ficha.logic.asstBmi
import app.ficha.logic.currentTDEE
import app.ficha.logic.dateLong
import app.ficha.logic.dateShort
import app.ficha.logic.dayKey
import app.ficha.logic.daysLabel
import app.ficha.logic.dietTargets
import app.ficha.logic.dietWeight
import app.ficha.logic.e1rm
import app.ficha.logic.ex
import app.ficha.logic.exName
import app.ficha.logic.fmt
import app.ficha.logic.fmtDur
import app.ficha.logic.fmtInt
import app.ficha.logic.groupSets
import app.ficha.logic.isScheduled
import app.ficha.logic.jsDay
import app.ficha.logic.dateOf
import app.ficha.logic.num
import app.ficha.logic.optLabel
import app.ficha.logic.resolved
import app.ficha.logic.sessionsInRange
import app.ficha.logic.ssInfo
import app.ficha.logic.startOfDay
import app.ficha.logic.startOfWeek
import app.ficha.logic.streakWeeks
import app.ficha.logic.timeHM
import app.ficha.logic.totals
import app.ficha.logic.weightSlope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
private fun schema(s: String) = Json.parseToJsonElement(s).jsonObject

private fun parse(text: String): JsonObject = try {
    Json.parseToJsonElement(text).jsonObject
} catch (e: Exception) {
    throw AiError("A resposta da IA veio num formato inesperado. Tente de novo.")
}

/* ================= Dieta: descrever refeição, foto do prato, foto do rótulo ================= */

private val MEAL_AI_SCHEMA = schema(
    """{"type":"object","additionalProperties":false,"required":["itens","observacao"],"properties":{"observacao":{"type":"string"},
    "itens":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["nome","gramas","kcal","proteina","carboidrato","gordura"],
    "properties":{"nome":{"type":"string"},"gramas":{"type":"number"},"kcal":{"type":"number"},"proteina":{"type":"number"},"carboidrato":{"type":"number"},"gordura":{"type":"number"}}}}}}""",
)
private val LABEL_AI_SCHEMA = schema(
    """{"type":"object","additionalProperties":false,"required":["legivel","nome","porcao_g","kcal_100g","proteina_100g","carboidrato_100g","gordura_100g","fibra_100g","observacao"],
    "properties":{"legivel":{"type":"boolean"},"nome":{"type":"string"},"porcao_g":{"type":"number"},"kcal_100g":{"type":"number"},"proteina_100g":{"type":"number"},
    "carboidrato_100g":{"type":"number"},"gordura_100g":{"type":"number"},"fibra_100g":{"type":"number"},"observacao":{"type":"string"}}}""",
)
private const val MEAL_AI_SYSTEM = """Você ajuda a registrar refeições num app brasileiro de treino e dieta.
Identifique cada alimento, estime a quantidade em gramas e calcule calorias, proteína, carboidrato e gordura DAQUELA quantidade (não por 100 g).
Use como referência a Tabela Brasileira de Composição de Alimentos (TACO) e porções caseiras brasileiras (ex.: colher de sopa de arroz cozido ≈ 25 g, concha de feijão ≈ 100 g, pão francês ≈ 50 g, ovo ≈ 50 g).
Quando a quantidade for informada, use-a. Quando não for, use uma porção típica de um adulto. Inclua óleo, molhos e bebidas quando aparecerem ou forem mencionados.
Nomes curtos em português (ex.: "Arroz branco cozido"). Em "observacao", uma frase curta sobre o que foi estimado e o que pode variar."""
private const val LABEL_AI_SYSTEM = """Você lê tabelas nutricionais de rótulos de alimentos brasileiros (fotos).
Converta os valores para 100 g (ou 100 ml) usando a porção informada no rótulo. Se a foto não mostrar uma tabela nutricional legível, responda legivel=false e zeros.
"nome": nome do produto se aparecer na foto, senão uma descrição curta. Em "observacao", avise se algo estava ilegível."""

@kotlinx.serialization.Serializable
data class AiMealItem(val nome: String = "", val gramas: Double = 0.0, val kcal: Double = 0.0, val proteina: Double = 0.0, val carboidrato: Double = 0.0, val gordura: Double = 0.0)

@kotlinx.serialization.Serializable
data class AiMeal(val itens: List<AiMealItem> = emptyList(), val observacao: String = "")

@kotlinx.serialization.Serializable
data class AiLabel(
    val legivel: Boolean = false, val nome: String = "", val porcao_g: Double = 0.0, val kcal_100g: Double = 0.0, val proteina_100g: Double = 0.0,
    val carboidrato_100g: Double = 0.0, val gordura_100g: Double = 0.0, val fibra_100g: Double = 0.0, val observacao: String = "",
)

object FoodAI {
    suspend fun meal(key: String, text: String?, imageB64: String?): AiMeal {
        val parts = buildList {
            if (imageB64 != null) add(Claude.Part.Image(imageB64))
            add(Claude.Part.Text(if (text != null) "Refeição: $text" else "Estime os alimentos e as quantidades deste prato pela foto (use o tamanho do prato e dos talheres como referência)."))
        }
        val out = Claude.run(key, MEAL_AI_SYSTEM, listOf(Claude.Msg(true, parts)), "low", 16000, MEAL_AI_SCHEMA)
        return json.decodeFromJsonElement(AiMeal.serializer(), parse(out))
    }

    suspend fun label(key: String, imageB64: String): AiLabel {
        val parts = listOf(Claude.Part.Image(imageB64), Claude.Part.Text("Leia a tabela nutricional deste rótulo."))
        val out = Claude.run(key, LABEL_AI_SYSTEM, listOf(Claude.Msg(true, parts)), "low", 16000, LABEL_AI_SCHEMA)
        return json.decodeFromJsonElement(AiLabel.serializer(), parse(out))
    }
}

/* ================= Assistente de treino com IA ================= */

private val ASST_SCHEMA = schema(
    """{"type":"object","additionalProperties":false,"required":["observacoes","opcoes"],"properties":{"observacoes":{"type":"string"},
    "opcoes":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["nome","resumo","porque","minutos","fichas"],
    "properties":{"nome":{"type":"string"},"resumo":{"type":"string"},"minutos":{"type":"integer"},"porque":{"type":"array","items":{"type":"string"}},
    "fichas":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["nome","dias","exercicios"],"properties":{"nome":{"type":"string"},
    "dias":{"type":"array","items":{"type":"integer"}},"exercicios":{"type":"array","items":{"type":"object","additionalProperties":false,
    "required":["id","series","reps","descanso","obs"],"properties":{"id":{"type":"string"},"series":{"type":"integer"},"reps":{"type":"string"},
    "descanso":{"type":"integer"},"obs":{"type":"string"}}}}}}}}}}}}""",
)

@kotlinx.serialization.Serializable
private data class AiEx(val id: String = "", val series: Int = 3, val reps: String = "", val descanso: Int = 0, val obs: String = "")

@kotlinx.serialization.Serializable
private data class AiFicha(val nome: String = "Ficha", val dias: List<Int> = emptyList(), val exercicios: List<AiEx> = emptyList())

@kotlinx.serialization.Serializable
private data class AiOpt(val nome: String = "Opção", val resumo: String = "", val porque: List<String> = emptyList(), val minutos: Int = 0, val fichas: List<AiFicha> = emptyList())

@kotlinx.serialization.Serializable
private data class AiPlan(val observacoes: String = "", val opcoes: List<AiOpt> = emptyList())

class AsstAiResult(val opts: List<AsstOption>, val notes: String, val dropped: Int)

object AssistantAI {
    private fun catalog(d: AppData, p: Profile): String {
        val allowed = when (p.local) { "casa_halteres" -> listOf("Halteres", "Peso corporal"); "casa" -> listOf("Peso corporal"); else -> null }
        return (Catalog.exercises + d.custom).filter { allowed == null || it.equip in allowed }
            .joinToString("\n") { "${it.id} | ${it.name} | ${it.group} | ${it.equip} | ${it.kind}" }
    }

    private fun system(d: AppData, p: Profile) = """Você é um treinador experiente, formado em educação física, montando programas de treino para um app de academia. Escreva em português do Brasil, de forma clara e curta.

Monte exatamente 3 opções de programa, diferentes entre si (por exemplo, divisões de treino diferentes), ordenadas da mais recomendada para a menos recomendada para esta pessoa.

Regras:
- Use somente exercícios do catálogo abaixo, sempre pelo id exato.
- Respeite todas as restrições e observações da pessoa. Na dúvida, escolha a alternativa mais segura e explique no campo "obs" do exercício.
- O número de fichas e os dias devem combinar com os dias por semana informados. "dias" usa 0 = domingo, 1 = segunda … 6 = sábado. Se as fichas forem feitas em sequência, sem dia fixo, deixe "dias" vazio.
- Cada ficha precisa caber no tempo por treino informado, contando séries, descansos e cardio. Informe em "minutos" a duração aproximada da ficha mais longa.
- "reps" é texto: faixa de repetições ("8-12"); para exercícios do tipo s, segundos ("30-45"); para cardio (tipo c), minutos ("20") com "series" = 1.
- "descanso" em segundos, um destes valores: 0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300.
- "obs": dica curta de execução ou ajuste para esta pessoa (pode ficar vazio).
- "resumo": uma frase descrevendo a opção. "porque": 3 a 5 frases curtas dizendo por que ela serve para esta pessoa.
- "observacoes": recomendações gerais curtas (aquecimento, progressão de carga, cuidados).
- Não faça diagnóstico médico. Se houver dor, lesão ou condição de saúde, recomende acompanhamento de um profissional.

Catálogo (id | nome | grupo | equipamento | tipo — w = carga × repetições, bw = peso do corpo, s = segundos, c = cardio):
${catalog(d, p)}"""

    private fun user(p: Profile): String {
        val bmi = asstBmi(p)
        val lines = listOfNotNull(
            "Objetivo: ${optLabel("objetivo", p.objetivo)}",
            "Experiência: ${optLabel("nivel", p.nivel)} (${ASST_LEVEL_HINT[p.nivel]})",
            "Dias por semana: ${p.dias}",
            "Tempo por treino: ${p.tempo} minutos",
            "Local: ${optLabel("local", p.local)}",
            if (p.sexo.isNotEmpty()) "Sexo: ${optLabel("sexo", p.sexo)}" else null,
            num(p.idade)?.let { "Idade: ${fmt(it, 0)} anos" },
            num(p.peso)?.let { "Peso: ${fmt(it)} kg" },
            num(p.altura)?.let { "Altura: ${fmt(it, 0)} cm" },
            bmi?.let { "IMC: ${"%.1f".format(java.util.Locale.US, it)}" },
            if (p.foco.isNotEmpty()) "Quer dar prioridade a: ${p.foco.joinToString(", ") { optLabel("foco", it) }}" else null,
            if (p.restr.isNotEmpty()) "Restrições: ${p.restr.joinToString(", ") { optLabel("restr", it) }}" else "Restrições: nenhuma informada",
            p.obs.trim().takeIf { it.isNotEmpty() }?.let { "Observações da pessoa: $it" },
        )
        return "Monte as 3 opções de programa para esta pessoa:\n" + lines.joinToString("\n") { "- $it" }
    }

    suspend fun generate(key: String, d: AppData, p: Profile, onProgress: (thinking: Boolean, chars: Int) -> Unit): AsstAiResult {
        var chars = 0
        val text = Claude.run(
            key, system(d, p), listOf(Claude.Msg(true, listOf(Claude.Part.Text(user(p))))), "medium", 32000, ASST_SCHEMA, cache = true,
            onText = { chars += it.length; onProgress(false, chars) }, onThinking = { onProgress(true, 0) },
        )
        val plan = json.decodeFromJsonElement(AiPlan.serializer(), parse(text))
        var dropped = 0
        fun snap(v: Int) = REST_OPTIONS.minBy { abs(it - v) }
        val opts = plan.opcoes.map { o ->
            val sessions = o.fichas.map { f ->
                TemplateRoutine(
                    name = f.nome.take(60),
                    days = f.dias.filter { it in 0..6 }.distinct().sorted(),
                    items = f.exercicios.filter { x -> (d.ex(x.id) != null).also { if (!it) dropped++ } }
                        .map { x -> RoutineItem(uid(), x.id, x.series.coerceIn(1, 10), x.reps.take(12), snap(x.descanso), x.obs.take(140)) },
                )
            }.filter { it.items.isNotEmpty() }
            AsstOption(
                o.nome, "${sessions.size} ficha${if (sessions.size > 1) "s" else ""}${if (o.minutos > 0) " · cerca de ${o.minutos} min" else ""}",
                (listOf(o.resumo) + o.porque).filter { it.isNotBlank() }, sessions, o.nome.take(60),
            )
        }.filter { it.sessions.isNotEmpty() }
        if (opts.isEmpty()) throw AiError("A IA não montou nenhuma opção válida. Tente de novo.")
        return AsstAiResult(opts, plan.observacoes, dropped)
    }
}

/* ================= Treinador IA: relatório da semana e conversa ================= */

private val COACH_SCHEMA = schema(
    """{"type":"object","additionalProperties":false,"required":["resumo","destaques","atencao","recomendacoes","metas_semana"],"properties":{
    "resumo":{"type":"string","description":"Duas ou três frases sobre a semana"},
    "destaques":{"type":"array","items":{"type":"string"},"description":"O que foi bem (até 4)"},
    "atencao":{"type":"array","items":{"type":"string"},"description":"Pontos de atenção (até 4)"},
    "recomendacoes":{"type":"array","items":{"type":"object","additionalProperties":false,"required":["titulo","detalhe"],"properties":{"titulo":{"type":"string"},"detalhe":{"type":"string"}}},"description":"Ações concretas para a próxima semana (até 4)"},
    "metas_semana":{"type":"array","items":{"type":"string"},"description":"Metas objetivas e mensuráveis para a semana (até 3)"}}}""",
)

private const val COACH_SYSTEM = """Você é o treinador do app Ficha, um app brasileiro de registro de treino de musculação e dieta. Fala em português do Brasil, de forma direta, motivadora e prática, como um bom personal trainer e nutricionista esportivo.

Regras:
- Baseie-se nos dados do usuário abaixo. Cite números concretos (cargas, séries, calorias, proteína, peso) quando ajudar.
- Use evidência atual de treino de força e nutrição: sobrecarga progressiva, 10–20 séries por músculo por semana, 1,6–2,2 g/kg de proteína, déficit ou superávit moderado, sono e recuperação.
- Se faltar dado para uma conclusão, diga o que falta registrar, sem inventar.
- Nada de diagnóstico médico. Com dor, lesão ou sintomas, recomende procurar um profissional de saúde.
- Respostas curtas e escaneáveis. Use listas curtas e **negrito** só no essencial."""

val COACH_SUGGEST = listOf(
    "Meu treino está bom para o meu objetivo?", "Por que minha carga parou de subir?", "Estou comendo proteína suficiente?",
    "O que ajustar na dieta esta semana?", "Monte um aquecimento para o treino de hoje",
)

object CoachAI {
    private fun cdate(t: Long) = "${DAY[dateOf(t).jsDay()]} ${dateShort(t)}"

    private fun topSets(kind: String, sets: List<app.ficha.data.SetRecord>): String {
        val w = sets.filter { !it.warm }
        if (w.isEmpty()) return "—"
        return when (kind) {
            "w" -> w.joinToString(", ") { s -> "${fmt(s.a ?: 0.0)}×${fmt(s.b ?: 0.0, 0)}${s.rir?.let { " RIR${fmt(it, 0)}" } ?: ""}${s.t?.let { " (${if (it == "drop") "drop" else if (it == "fail") "falha" else it})" } ?: ""}" }
            "bw" -> w.joinToString(", ") { s -> "${fmt(s.b ?: 0.0, 0)}${if ((s.a ?: 0.0) > 0) "+${fmt(s.a)}kg" else ""}" }
            "s" -> w.joinToString(", ") { s -> "${fmt(s.a ?: 0.0, 0)}s" }
            else -> w.joinToString(", ") { s -> "${fmt(s.a ?: 0.0)}min${if ((s.b ?: 0.0) > 0) " ${fmt(s.b)}km" else ""}" }
        }
    }

    /** Resumo compacto em texto: perfil, plano, treinos, evolução, dieta e corpo. */
    fun context(d: AppData): String {
        val now = System.currentTimeMillis()
        val today = startOfDay(now)
        val L = mutableListOf<String>()
        val p = d.profile ?: Profile()
        val bw = d.dietWeight()
        L += "Data de hoje: ${dateLong(now)} de ${LocalDate.now().year}."
        L += "## Perfil\nSexo: ${when (p.sexo) { "m" -> "masculino"; "f" -> "feminino"; else -> "não informado" }} · Idade: ${p.idade.ifEmpty { "?" }} · Altura: ${p.altura.ifEmpty { "?" }} cm · " +
            "Peso atual: ${bw?.let { fmt(it) + " kg" } ?: "?"}${if (d.profile != null) " · Objetivo no assistente: ${p.objetivo} · Nível: ${p.nivel}" else ""}" +
            if (p.obs.isNotBlank()) "\nObservações do usuário: ${p.obs}" else ""
        val rs = d.routines.filter { it.items.isNotEmpty() && d.isScheduled(it) }
        if (rs.isNotEmpty()) L += "## Fichas em uso\n" + rs.joinToString("\n") { r ->
            val ss = r.items.map { it.ss }
            "- ${r.name} (${daysLabel(r.days)}): " + r.items.mapIndexed { i, it ->
                val g = ssInfo(ss, i)
                "${d.exName(it.exId)} ${it.sets}×${it.reps.ifEmpty { "?" }}${g?.let { " [${it.g.name.lowercase()} ${it.g.letter}]" } ?: ""}"
            }.joinToString("; ")
        }
        val recent = d.sessions.filter { it.start >= addDays(today, -28) }.sortedBy { it.start }
        val wk = startOfWeek(now)
        L += "## Frequência\nTreinos por semana (da mais antiga para a atual): ${listOf(3, 2, 1, 0).joinToString(", ") { k -> d.sessionsInRange(addDays(wk, -7 * k), addDays(wk, -7 * k + 7)).size.toString() }} · " +
            "Semanas seguidas treinando: ${d.streakWeeks(now)} · Total de treinos registrados: ${d.sessions.size}"
        L += if (recent.isNotEmpty()) "## Treinos dos últimos 28 dias (séries de trabalho: kg×reps)\n" + recent.joinToString("\n") { s ->
            "- ${cdate(s.start)} ${timeHM(s.start)} · ${s.name} · ${fmtDur(s.end - s.start)}" +
                (if (s.prs.isNotEmpty()) " · recordes: ${s.prs.joinToString(", ") { "${it.name} ${it.label} ${fmt(it.value)}${it.unit}" }}" else "") + "\n" +
                s.exercises.joinToString("\n") { e -> "  ${d.exName(e.exId, e.name)}: ${topSets(e.kind, e.sets)}" } +
                if (s.notes.isNotBlank()) "\n  Anotação: ${s.notes}" else ""
        } else "## Treinos dos últimos 28 dias\nNenhum treino registrado."
        // Evolução dos principais exercícios (1RM estimado por semana, 8 semanas)
        val freq = HashMap<String, Int>()
        for (s in d.sessions.filter { it.start >= addDays(today, -56) }) for (e in s.exercises) if (e.kind == "w") freq[e.exId] = (freq[e.exId] ?: 0) + 1
        val main = freq.entries.sortedByDescending { it.value }.take(8).map { it.key }
        if (main.isNotEmpty()) L += "## Evolução (melhor 1RM estimado por semana, 8 semanas, da mais antiga para a atual; \"-\" = não treinou)\n" + main.joinToString("\n") { id ->
            val wks = (7 downTo 0).map { k ->
                val a = addDays(wk, -7 * k)
                var m = 0.0
                for (s in d.sessionsInRange(a, addDays(a, 7))) for (e in s.exercises) if (e.exId == id) for (x in e.sets.filter { !it.warm }) m = max(m, e1rm(x.a, x.b))
                if (m > 0) fmt(m, 0) else "-"
            }
            "- ${d.exName(id)}: ${wks.joinToString(" → ")} kg"
        }
        val cur = d.groupSets(wk, addDays(wk, 7))
        val avg = d.groupSets(addDays(wk, -28), wk).map { it / 4 }
        L += "## Séries por grupo muscular (semana atual / média das 4 anteriores; referência ${VOL_RANGE.first}–${VOL_RANGE.second} por semana)\n" +
            VOL_GROUPS.mapIndexedNotNull { i, (n, _) -> if (cur[i] > 0 || avg[i] > 0) "$n: ${fmt(cur[i], 1)} / ${fmt(avg[i], 1)}" else null }.joinToString(" · ")
        // Dieta (14 dias)
        val tg = d.dietTargets()
        val rows = (13 downTo 0).mapNotNull { i ->
            val t = addDays(today, -i)
            val day = d.food.days[dayKey(t)] ?: return@mapNotNull null
            if (day.e.isEmpty() && day.w == 0) return@mapNotNull null
            val tot = day.totals()
            "- ${cdate(t)}: ${fmtInt(tot.k)} kcal · P ${fmt(tot.p, 0)} · C ${fmt(tot.c, 0)} · G ${fmt(tot.f, 0)} · fibras ${fmt(tot.fi, 0)} g · água ${fmt(day.w / 1000.0, 1)} L${if (i == 0) " (hoje, dia em andamento)" else ""}"
        }
        val g = d.food.goal?.resolved()
        L += "## Dieta\n" + (if (g != null) "Objetivo: ${mapOf("perder" to "perder gordura", "manter" to "manter", "ganhar" to "ganhar massa")[g.obj]}${if (g.obj != "manter") " (${fmt(g.rate)}% do peso por semana)" else ""}" else "Metas não definidas") +
            (tg?.let { " · Meta diária: ${fmtInt(it.k)} kcal, P ${fmt(it.p, 0)} g, C ${fmt(it.c, 0)} g, G ${fmt(it.f, 0)} g" } ?: "") +
            (d.currentTDEE()?.let { " · Gasto estimado (TDEE): ${fmtInt(it)} kcal${if (d.food.tdee?.v != null) " (ajustado pelos registros)" else " (fórmula)"}" } ?: "") +
            if (rows.isNotEmpty()) "\nÚltimos 14 dias:\n" + rows.joinToString("\n") else "\nSem registros de alimentação nos últimos 14 dias."
        // Corpo
        val body = d.body.sortedBy { it.t }
        if (body.isNotEmpty()) {
            val sb = StringBuilder("## Corpo\n")
            val pesos = body.filter { it["peso"] != null && it.t >= addDays(today, -56) }
            if (pesos.isNotEmpty()) sb.append("Pesagens (8 semanas): ${pesos.joinToString(", ") { "${dateShort(it.t)} ${fmt(it["peso"])}" }}\n")
            d.weightSlope(28)?.let { sb.append("Tendência do peso (regressão, 28 dias): ${if (it.perDay >= 0) "+" else ""}${fmt(it.perDay * 7, 2)} kg/semana\n") }
            val last = body.last()
            val meds = BODY_FIELDS.filter { it.key != "peso" && last[it.key] != null }.map { f ->
                val first = body.first { it[f.key] != null }
                "${f.name} ${fmt(last[f.key], f.dec)}${f.unit}${if (first !== last) " (era ${fmt(first[f.key], f.dec)} em ${dateShort(first.t)})" else ""}"
            }
            if (meds.isNotEmpty()) sb.append("Últimas medidas (${dateShort(last.t)}): ${meds.joinToString(", ")}")
            L += sb.toString().trim()
        }
        if (d.photos.isNotEmpty()) L += "## Fotos do progresso\n${d.photos.size} fotos, a mais recente em ${dateShort(d.photos.maxOf { it.t })}."
        return L.joinToString("\n\n")
    }

    suspend fun report(key: String, d: AppData): CoachReportData {
        val text = Claude.run(
            key, "$COACH_SYSTEM\n\n# Dados do usuário\n${context(d)}",
            listOf(Claude.Msg(true, listOf(Claude.Part.Text("Faça o relatório da minha semana de treino e dieta: compare com as semanas anteriores e diga o que fazer na próxima.")))),
            "medium", 16000, COACH_SCHEMA, cache = true,
        )
        return Store.json.decodeFromJsonElement(CoachReportData.serializer(), parse(text))
    }

    /** Conversa: começa pelo usuário e alterna os papéis (mensagens seguidas do mesmo lado são juntadas). */
    suspend fun chat(key: String, d: AppData, history: List<app.ficha.data.ChatMsg>, onText: (String) -> Unit): String {
        val msgs = mutableListOf<Pair<Boolean, StringBuilder>>()
        for (m in history) {
            val user = m.r == "u"
            if (msgs.isEmpty() && !user) continue
            if (msgs.isNotEmpty() && msgs.last().first == user) msgs.last().second.append("\n\n").append(m.x)
            else msgs += user to StringBuilder(m.x)
        }
        return Claude.run(
            key, "$COACH_SYSTEM\n\n# Dados do usuário\n${context(d)}",
            msgs.map { (u, t) -> Claude.Msg(u, listOf(Claude.Part.Text(t.toString()))) },
            "low", 8000, null, cache = true, onText = onText,
        )
    }

    fun weekKey(t: Long = System.currentTimeMillis()) = dateOf(startOfWeek(t)).toString()
}
