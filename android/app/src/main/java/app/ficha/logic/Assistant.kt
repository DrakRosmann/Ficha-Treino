package app.ficha.logic

import app.ficha.data.AppData
import app.ficha.data.Profile
import app.ficha.data.RoutineItem
import app.ficha.data.TemplateRoutine
import app.ficha.data.uid

/* ================= Assistente de treino: motor de regras (port do assistant.js) ================= */

val ASST_OPTS = mapOf(
    "objetivo" to listOf("massa" to "Ganhar massa", "definir" to "Definição", "emagrecer" to "Perder gordura", "forca" to "Ganhar força", "condicionamento" to "Condicionamento", "saude" to "Saúde e bem-estar"),
    "nivel" to listOf("iniciante" to "Iniciante", "intermediario" to "Intermediário", "avancado" to "Avançado"),
    "dias" to listOf("2" to "2", "3" to "3", "4" to "4", "5" to "5", "6" to "6"),
    "tempo" to listOf("30" to "30", "45" to "45", "60" to "60", "75" to "75", "90" to "90"),
    "local" to listOf("academia" to "Academia", "casa_halteres" to "Casa com halteres", "casa" to "Casa sem equipamento"),
    "sexo" to listOf("f" to "Feminino", "m" to "Masculino", "" to "Não informar"),
    "foco" to listOf("peito" to "Peito", "costas" to "Costas", "ombros" to "Ombros", "bracos" to "Braços", "pernas" to "Pernas", "gluteos" to "Glúteos", "abdomen" to "Abdômen"),
    "restr" to listOf("joelho" to "Joelho", "lombar" to "Lombar / coluna", "ombro" to "Ombro", "punho" to "Punho / cotovelo", "quadril" to "Quadril", "impacto" to "Sem impacto (saltos)", "pressao" to "Pressão alta / coração"),
)
val ASST_LEVEL_HINT = mapOf("iniciante" to "menos de 6 meses", "intermediario" to "6 meses a 2 anos", "avancado" to "mais de 2 anos")

fun optLabel(field: String, v: String) = ASST_OPTS[field]?.find { it.first == v }?.second ?: v

/* Candidatos por padrão de movimento, em ordem de preferência: [id, ambientes, restrições]
   Ambientes: g = academia, d = casa com halteres, b = só peso do corpo.
   Restrições: K joelho, L lombar, S ombro, W punho/cotovelo, H quadril, I impacto, A evita para iniciantes, X só para avançados. */
private class Cand(val id: String, val env: String, val tags: String)

private fun c(vararg t: String) = t.toList().chunked(3).map { Cand(it[0], it[1], it[2]) }

private val ASST_SLOTS: Map<String, List<Cand>> = mapOf(
    "squat" to c("agachamento", "g", "KL", "leg-press-45", "g", "", "hack", "g", "K", "agachamento-smith", "g", "KL", "goblet", "gd", "K",
        "leg-press-horizontal", "g", "", "agachamento-com-halteres", "d", "K", "agachamento-sumo-com-halter", "d", "H",
        "agachamento-com-peso-corporal", "b", "K", "agachamento-frontal", "g", "KLA"),
    "lunge" to c("bulgaro", "gd", "KH", "passada", "gd", "K", "afundo-reverso-com-halteres", "gd", "K", "leg-press-unilateral", "g", "",
        "step-up", "gd", "K", "passada-com-peso-corporal", "b", "K"),
    "hinge" to c("terra-romeno", "g", "L", "stiff-halter", "gd", "L", "stiff-barra", "g", "L", "terra", "g", "LA",
        "levantamento-terra-com-barra-hexagonal", "g", "L", "stiff-unilateral", "d", "L", "ponte-de-gluteo-unilateral", "b", ""),
    "ham_curl" to c("mesa-flexora", "g", "", "cadeira-flexora", "g", "", "flexora-em-pe", "g", "", "ponte-de-gluteo-unilateral", "db", ""),
    "quad_iso" to c("extensora", "g", "K", "cadeira-extensora-unilateral", "g", "K", "cadeirinha", "db", "K"),
    "glute" to c("hip-thrust", "g", "", "elevacao-pelvica-maquina", "g", "", "gluteo-polia", "g", "", "abdutora", "g", "",
        "ponte-de-gluteo-com-barra", "g", "", "ponte-gluteo", "db", "", "ponte-de-gluteo-unilateral", "db", "", "gluteo-coice-no-solo-quatro-apoios", "db", "W"),
    "calf" to c("panturrilha-em-pe", "g", "", "panturrilha-sentado", "g", "", "panturrilha-leg", "g", "", "panturrilha-smith", "g", "",
        "panturrilha-em-pe-com-halteres", "d", "", "panturrilha-unilateral-apoiado-no-halter", "d", ""),
    "h_push" to c("supino-reto-barra", "g", "SW", "supino-reto-halter", "gd", "S", "supino-maquina", "g", "", "supino-articulado", "g", "",
        "supino-reto-com-halteres-pegada-neutra", "gd", "", "supino-no-chao-com-halteres", "d", "", "flexao", "db", "WA", "flexao-joelhos", "b", "W"),
    "incline_push" to c("supino-inclinado-halter", "gd", "S", "supino-inclinado-barra", "g", "SW", "supino-inclinado-articulado", "g", "",
        "supino-inclinado-smith", "g", "S", "supino-inclinado-com-halteres-pegada-neutra", "gd", "", "flexao-de-braco-com-pes-elevados", "b", "WSA"),
    "chest_iso" to c("peck-deck", "g", "", "crossover-alto", "g", "", "crossover-baixo", "g", "", "crucifixo-reto", "gd", "S", "crucifixo-inclinado", "gd", "S"),
    "v_push" to c("desenvolvimento-halter", "gd", "S", "desenvolvimento-maquina", "g", "S", "arnold", "gd", "S", "desenvolvimento-barra", "g", "SLA",
        "desenvolvimento-articulado", "g", "S", "desenvolvimento-em-pe-com-halteres-pegada-neutra", "d", "S", "flexao-em-parada-de-mao-handstand-push-up", "b", "SWX"),
    "lat_raise" to c("elevacao-lateral", "gd", "", "elevacao-lateral-polia", "g", "", "elevacao-lateral-maquina", "g", "", "elevacao-lateral-sentado-com-halteres", "gd", ""),
    "rear_delt" to c("face-pull", "g", "", "crucifixo-invertido", "gd", "", "voador-inverso", "g", "", "crucifixo-invertido-na-polia", "g", ""),
    "v_pull" to c("puxada-aberta", "g", "", "puxada-triangulo", "g", "", "puxada-supinada", "g", "", "barra-fixa", "g", "SA", "puxada-articulada", "g", "",
        "chin-up", "g", "SA", "puxada-unilateral-na-polia", "g", "", "pullover", "d", "S", "remada-invertida-remada-australiana", "b", ""),
    "h_pull" to c("remada-curvada", "g", "LA", "remada-baixa", "g", "", "serrote", "gd", "", "remada-maquina", "g", "", "remada-cavalinho", "g", "L",
        "remada-cavalinho-apoiada-maquina", "g", "", "remada-com-halteres-apoiado-no-banco-inclinado", "gd", "", "remada-curvada-com-halteres", "d", "L",
        "remada-sentado-unilateral-na-polia", "g", "", "remada-invertida-remada-australiana", "b", ""),
    "lat_iso" to c("pulldown", "g", "", "pullover", "gd", "S", "pulldown-com-corda-bracos-estendidos", "g", ""),
    "shrug" to c("encolhimento-halter", "gd", "", "encolhimento-barra", "g", ""),
    "biceps" to c("rosca-direta", "g", "W", "rosca-alternada", "gd", "", "rosca-martelo", "gd", "", "rosca-scott", "g", "W", "rosca-polia", "g", "",
        "rosca-inclinada", "gd", "", "rosca-w", "g", "", "rosca-concentrada", "gd", "", "rosca-maquina", "g", ""),
    "triceps" to c("triceps-pulley", "g", "", "triceps-corda", "g", "", "triceps-frances", "gd", "SW", "triceps-testa", "g", "W", "triceps-coice", "gd", "",
        "triceps-unilateral", "g", "", "triceps-testa-com-halteres", "d", "W", "mergulho-banco", "db", "SW", "flexao-de-braco-fechada-diamante", "b", "WA"),
    "core" to c("prancha", "gdb", "", "abdominal-infra", "gdb", "L", "abdominal-supra", "gdb", "L", "dead-bug-inseto-morto", "gdb", "", "abdominal-polia", "g", "L",
        "elevacao-pernas", "g", "LSA", "prancha-lateral", "gdb", "", "pallof-press", "g", "", "abdominal-bicicleta", "gdb", "L"),
    "cardio" to c("bicicleta", "g", "", "eliptico", "g", "", "esteira", "g", "", "escada", "g", "K", "remo-ergometrico", "g", "L",
        "corrida-caminhada-ao-ar-livre", "db", "", "pular-corda", "db", "IK"),
    "finisher" to c("kettlebell-swing", "g", "L", "burpee", "gdb", "IKW", "escalador-mountain-climber", "gdb", "W", "polichinelo", "gdb", "I",
        "corda-naval-battle-rope", "g", "", "agachamento-com-salto", "gdb", "IK"),
    "mobility" to c("melhor-alongamento-do-mundo-world-s-greatest-stretch", "gdb", "", "alongamento-do-gato-cat-stretch", "gdb", "",
        "circulos-de-quadril-em-pe", "gdb", "", "inchworm-caminhada-da-lagarta", "gdb", ""),
)

/** Se um padrão não tiver opção segura/disponível, tenta estes no lugar */
private val ASST_FALLBACK = mapOf(
    "squat" to listOf("glute", "quad_iso"), "lunge" to listOf("glute", "ham_curl"), "hinge" to listOf("glute", "ham_curl"),
    "h_push" to listOf("chest_iso"), "incline_push" to listOf("chest_iso", "h_push"), "v_push" to listOf("lat_raise"),
    "v_pull" to listOf("h_pull", "lat_iso"), "h_pull" to listOf("v_pull"), "quad_iso" to listOf("glute"),
)
private val ASST_COMPOUND = setOf("squat", "lunge", "hinge", "h_push", "incline_push", "v_push", "v_pull", "h_pull")
private val ASST_SLOT_KIND = setOf("squat", "lunge", "hinge", "quad_iso", "ham_curl", "glute", "calf", "h_push", "incline_push", "chest_iso", "v_push",
    "lat_raise", "triceps", "v_pull", "h_pull", "lat_iso", "rear_delt", "biceps", "shrug")

/** Fichas: padrões na ordem; ":2"/":3" = prioridade menor (sai primeiro se faltar tempo) */
private val ASST_SESSIONS = mapOf(
    "fbA" to ("Corpo inteiro A" to "squat h_push v_pull hinge:2 lat_raise:2 core:2 biceps:3 triceps:3"),
    "fbB" to ("Corpo inteiro B" to "hinge incline_push h_pull v_push:2 lunge:2 core:2 calf:3 triceps:3"),
    "fbC" to ("Corpo inteiro C" to "lunge h_push v_pull v_push:2 glute:2 core:2 biceps:3 rear_delt:3"),
    "upA" to ("Superiores A" to "h_push h_pull v_push v_pull incline_push:2 lat_raise:2 biceps:3 triceps:3"),
    "loA" to ("Inferiores A" to "squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:3 core:3"),
    "upB" to ("Superiores B" to "incline_push v_pull h_pull v_push:2 chest_iso:2 rear_delt:2 biceps:3 triceps:3"),
    "loB" to ("Inferiores B" to "hinge squat glute:2 lunge:2 ham_curl:2 quad_iso:3 calf:3 core:3"),
    "push" to ("Push — peito, ombro e tríceps" to "h_push incline_push v_push chest_iso:2 lat_raise:2 triceps:2 triceps:3"),
    "pull" to ("Pull — costas e bíceps" to "v_pull h_pull h_pull:2 lat_iso:2 rear_delt:2 biceps:2 biceps:3 shrug:3"),
    "legs" to ("Legs — pernas" to "squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 core:3"),
    "abcA" to ("A — Peito, ombro e tríceps" to "h_push incline_push v_push chest_iso:2 lat_raise:2 triceps:2 triceps:3"),
    "abcB" to ("B — Costas e bíceps" to "v_pull h_pull h_pull:2 lat_iso:2 rear_delt:2 biceps:2 biceps:3"),
    "abcC" to ("C — Pernas e abdômen" to "squat hinge quad_iso:2 ham_curl:2 lunge:2 calf:3 core:3"),
    "abcdA" to ("A — Peito e tríceps" to "h_push incline_push chest_iso:2 chest_iso:3 triceps:2 triceps:2"),
    "abcdB" to ("B — Costas e bíceps" to "v_pull h_pull h_pull:2 lat_iso:2 biceps:2 biceps:3"),
    "abcdC" to ("C — Pernas" to "squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 glute:3"),
    "abcdD" to ("D — Ombros e abdômen" to "v_push lat_raise rear_delt:2 lat_raise:2 shrug:3 core:2 core:3"),
    "e1" to ("A — Peito" to "h_push incline_push chest_iso:2 chest_iso:2 incline_push:3"),
    "e2" to ("B — Costas" to "v_pull h_pull v_pull:2 h_pull:2 lat_iso:2 shrug:3"),
    "e3" to ("C — Pernas" to "squat hinge lunge:2 quad_iso:2 ham_curl:2 calf:2 glute:3"),
    "e4" to ("D — Ombros" to "v_push lat_raise v_push:2 lat_raise:2 rear_delt:2 shrug:3 core:3"),
    "e5" to ("E — Braços e abdômen" to "biceps triceps biceps:2 triceps:2 core:2 biceps:3 triceps:3"),
    "cA" to ("Circuito A" to "squat h_push h_pull lunge:2 v_push:2 core:2"),
    "cB" to ("Circuito B" to "hinge incline_push v_pull glute:2 lat_raise:2 core:2"),
)

private class Split(val name: String, val sessions: List<String>, val why: String)

private val ASST_SPLITS = mapOf(
    "fb2" to Split("Corpo inteiro A/B", listOf("fbA", "fbB"), "Corpo inteiro em cada treino: cada músculo trabalha toda vez que você treina."),
    "fb3" to Split("Corpo inteiro A/B/C", listOf("fbA", "fbB", "fbC"), "Corpo inteiro com 3 fichas diferentes: muita frequência por músculo e treinos curtos — ótimo para evoluir rápido e aprender os movimentos."),
    "ul2" to Split("Superiores / Inferiores", listOf("upA", "loA"), "Um dia para a parte de cima e outro para as pernas: treinos mais focados."),
    "ul4" to Split("Upper / Lower", listOf("upA", "loA", "upB", "loB"), "Superiores e inferiores alternados: cada músculo treina 2x por semana com bom volume — um dos formatos mais eficientes para 4 dias."),
    "ppl" to Split("Push / Pull / Legs", listOf("push", "pull", "legs"), "Empurrar, puxar e pernas: junta os músculos que trabalham juntos. Com 6 dias, cada ficha se repete 2x na semana."),
    "abc" to Split("ABC", listOf("abcA", "abcB", "abcC"), "O clássico das academias: cada ficha foca em poucos grupos, com bastante volume para cada um."),
    "abcd" to Split("ABCD", listOf("abcdA", "abcdB", "abcdC", "abcdD"), "Quatro fichas para dar mais atenção a cada grupo, incluindo ombros e abdômen."),
    "abcde" to Split("ABCDE", listOf("e1", "e2", "e3", "e4", "e5"), "Um grupo por dia com muito volume — para quem já treina há bastante tempo."),
    "ulppl" to Split("Upper / Lower + PPL", listOf("upA", "loA", "push", "pull", "legs"), "Mistura Upper/Lower com Push/Pull/Legs: cada músculo treina 2x por semana em 5 dias."),
    "ulfb" to Split("Superiores / Inferiores / Corpo inteiro", listOf("upA", "loA", "fbA"), "Parte de cima, pernas e um corpo inteiro: cada músculo treina 2x por semana em só 3 dias."),
    "circ" to Split("Circuito corpo inteiro", listOf("cA", "cB"), "Circuitos de corpo inteiro com pouco descanso: mantém o coração acelerado e aumenta o gasto de calorias."),
)

private val ASST_GOAL_WHY = mapOf(
    "massa" to "Ganhar massa: 3–4 séries de 8–12 repetições, descanso de 60–90 s, terminando cada série perto da falha.",
    "forca" to "Força: exercícios principais pesados (4–6 repetições) com descanso longo; os acessórios usam mais repetições.",
    "definir" to "Definição: 10–15 repetições com descanso curto e cardio no fim, para gastar calorias sem perder músculo.",
    "emagrecer" to "Perder gordura: treino de força para manter os músculos, descanso curto, um exercício intenso no fim e cardio.",
    "condicionamento" to "Condicionamento: repetições altas, pouco descanso e um exercício metabólico para fechar o treino.",
    "saude" to "Saúde: volume moderado, movimentos básicos, mobilidade no início e cardio leve — o mais importante é a constância.",
)
private val ASST_RESTR_WHY = mapOf(
    "joelho" to "joelho (sem agachamentos profundos, afundos e saltos)",
    "lombar" to "lombar (sem levantamento terra e remadas sem apoio; abdômen com exercícios de estabilização)",
    "ombro" to "ombro (sem desenvolvimentos e mergulhos; supino com pegada neutra ou máquina)",
    "punho" to "punho e cotovelo (sem barra reta e flexões no chão)",
    "quadril" to "quadril (sem afundos amplos e agachamento sumô)",
    "impacto" to "sem impacto (sem saltos nem corrida)",
    "pressao" to "pressão alta (descansos maiores e sem cargas máximas)",
)
private val ASST_RESTR_TAG = mapOf("joelho" to 'K', "lombar" to 'L', "ombro" to 'S', "punho" to 'W', "quadril" to 'H', "impacto" to 'I')
/** Dicas por restrição: [padrões em que vale a dica, texto] — aparece uma vez por ficha */
private val ASST_RESTR_NOTE = mapOf(
    "joelho" to listOf("squat lunge quad_iso" to "Joelho: amplitude confortável e carga moderada"),
    "lombar" to listOf("squat" to "Lombar: costas bem apoiadas, sem arredondar", "h_pull v_pull" to "Lombar: coluna neutra, sem jogar o tronco"),
    "ombro" to listOf("h_push incline_push v_push" to "Ombro: amplitude sem dor, cotovelos um pouco abaixo dos ombros", "lat_raise" to "Ombro: suba só até a altura dos ombros"),
    "punho" to listOf("h_push incline_push triceps" to "Punho: mantenha o punho firme e alinhado", "biceps h_pull v_pull" to "Punho: use pegada neutra se incomodar"),
    "quadril" to listOf("squat lunge glute" to "Quadril: amplitude confortável, sem dor"),
    "pressao" to listOf("squat hinge lunge h_push incline_push v_push h_pull v_pull" to "Respire durante o movimento, sem prender o ar"),
)
private val ASST_EX_NOTE = mapOf(
    "remada-invertida-remada-australiana" to "Use uma barra baixa ou uma mesa bem firme",
    "esteira" to "Caminhada inclinada ou corrida leve",
    "flexao-joelhos" to "Quando ficar fácil, passe para a flexão completa",
    "mergulho-banco" to "Use um banco ou uma cadeira firme",
)
private val ASST_HEAVY = setOf("squat", "hinge", "h_push", "incline_push", "v_push", "v_pull", "h_pull")
private val ASST_WEEK = mapOf(2 to listOf(1, 4), 3 to listOf(1, 3, 5), 4 to listOf(1, 2, 4, 5), 5 to listOf(1, 2, 3, 4, 5), 6 to listOf(1, 2, 3, 4, 5, 6))

/** Uma opção de programa (do motor de regras ou da IA). */
class AsstOption(val name: String, val sub: String, val why: List<String>, val sessions: List<TemplateRoutine>, val programName: String)

fun asstBmi(p: Profile): Double? {
    val w = num(p.peso) ?: return null
    val h = num(p.altura) ?: return null
    val m = if (h > 3) h / 100 else h
    return w / (m * m)
}

private fun asstEnv(p: Profile) = when (p.local) { "casa_halteres" -> 'd'; "casa" -> 'b'; else -> 'g' }

private fun asstAvoid(p: Profile): Set<Char> {
    val tags = p.restr.mapNotNull { ASST_RESTR_TAG[it] }.toMutableSet()
    val bmi = asstBmi(p)
    val age = num(p.idade)
    if ('K' in tags || (bmi != null && bmi >= 30) || (age != null && age >= 55)) tags += 'I'
    if (p.nivel == "iniciante") tags += 'A'
    if (p.nivel != "avancado") tags += 'X'
    return tags
}

/** Pseudo-aleatório determinístico (para "gerar outra variação") */
private fun asstRand(seed: Int): () -> Double {
    var x = ((seed.toLong() * 9301 + 49297) % 233280)
    return {
        x = (x * 9301 + 49297) % 233280
        x / 233280.0
    }
}

private fun asstSplitKeys(p: Profile): List<String> {
    val d = p.dias
    val lv = p.nivel
    val burn = p.objetivo == "emagrecer" || p.objetivo == "condicionamento"
    var list = when {
        d <= 2 -> if (burn) listOf("circ", "fb2", "ul2") else listOf("fb2", "ul2", "circ")
        d == 3 -> if (lv == "iniciante") listOf("fb3", "ulfb", "ppl") else if (burn) listOf("fb3", "circ", "ppl") else listOf("ppl", "fb3", "abc")
        d == 4 -> if (lv == "iniciante") listOf("ul4", "fb2", "abcd") else if (burn) listOf("ul4", "circ", "fb2") else listOf("ul4", "abcd", "ppl")
        d == 5 -> if (lv == "avancado") listOf("ulppl", "abcde", "ul4") else listOf("ulppl", "ul4", "abc")
        else -> if (lv == "iniciante") listOf("ppl", "ul4", "fb3") else listOf("ppl", "abc", "ulppl")
    }
    // Só com o peso do corpo, divisões por grupo ficam pobres: prefere corpo inteiro e circuitos
    if (p.local == "casa") list = (list.filter { it in listOf("fb2", "fb3", "circ", "ul2", "ul4", "ulfb") } + listOf("fb3", "circ", "fb2")).distinct()
    return list.take(3)
}

private fun asstDays(n: Int, d: Int): List<List<Int>>? {
    val week = ASST_WEEK[d] ?: ASST_WEEK.getValue(3)
    if (n == week.size) return week.map { listOf(it) }
    if (week.size % n == 0) return (0 until n).map { i -> week.filterIndexed { j, _ -> j % n == i } }
    return null // não divide certinho: treino em sequência, sem dia fixo
}

private fun asstScheme(p: Profile, role: Char, cIdx: Int, kind: String, cardioMin: Int, circuit: Boolean, slot: String): Triple<Int, String, Int> {
    val c = role == 'c'
    var (sets, reps, rest) = when (p.objetivo) {
        "forca" -> if (c) (if (cIdx < 2 && slot in ASST_HEAVY) Triple(5, "4-6", 180) else Triple(4, "6-8", 120)) else Triple(3, "8-10", 90)
        "definir" -> if (c) Triple(3, "10-12", 75) else Triple(3, "12-15", 45)
        "emagrecer" -> if (c) Triple(3, "12-15", 60) else Triple(3, "15", 45)
        "condicionamento" -> if (c) Triple(3, "12-15", 45) else Triple(3, "15-20", 30)
        "saude" -> if (c) Triple(3, "10-12", 75) else Triple(2, "12-15", 60)
        else -> if (c) Triple(4, "8-12", 90) else Triple(3, "10-15", 60)
    }
    if (p.nivel == "iniciante") {
        sets = minOf(sets, 3)
        if (p.objetivo == "forca" && c) { reps = "6-8"; rest = 150 }
    }
    if (p.nivel == "avancado" && c && sets < 4 && p.objetivo != "saude") sets++
    if ("pressao" in p.restr) {
        rest = maxOf(rest, 60)
        if (p.objetivo == "forca") { sets = minOf(sets, 3); reps = "8-10"; rest = 120 }
    }
    if (slot == "core") { sets = minOf(sets, 3); reps = "12-15"; rest = 45 }
    if (circuit) rest = 30
    if (kind == "s") reps = if (p.nivel == "iniciante") "20-30" else "30-45"
    if (kind == "c") return Triple(1, cardioMin.toString(), 0)
    return Triple(sets, reps, rest)
}

private fun asstMinutes(sets: Int, rest: Int, kind: String) = if (kind == "c") 0.0 else sets * (rest + 40) / 60.0 + 1

private fun AppData.asstPick(slot: String, p: Profile, env: Char, avoid: Set<Char>, used: Set<String>, offset: Int, rnd: () -> Double): String? {
    val ok = (ASST_SLOTS[slot] ?: emptyList()).filter { cnd -> env in cnd.env && cnd.tags.none { it in avoid } && cnd.id !in used && ex(cnd.id) != null }
    if (ok.isEmpty()) return null
    var list = ok
    // Iniciante na academia: máquinas e polias primeiro (mais seguras para aprender)
    if (p.nivel == "iniciante" && env == 'g' && (slot in ASST_COMPOUND || slot in ASST_SLOT_KIND)) {
        val easy = { cnd: Cand -> if (ex(cnd.id)?.equip in listOf("Máquina", "Polia", "Smith")) 0 else 1 }
        list = ok.withIndex().sortedWith(compareBy({ easy(it.value) }, { it.index })).map { it.value }
    }
    val span = minOf(list.size, 3)
    val i = (offset + (rnd() * span).toInt()) % span
    return list[i].id
}

private class Slot(val slot: String, val pri: Int, val order: Int)
private class Chosen(val id: String, val slot: String, val role: Char, val sets: Int, val reps: String, val rest: Int, val order: Int)

private fun AppData.asstBuildOption(p: Profile, key: String, seed: Int): AsstOption {
    val split = ASST_SPLITS.getValue(key)
    val env = asstEnv(p)
    val avoid = asstAvoid(p)
    val rnd = asstRand(seed + key.length * 7)
    val goal = p.objetivo
    val tempo = if (p.tempo > 0) p.tempo else 60
    var cardioMin = mapOf("emagrecer" to 20, "condicionamento" to 15, "definir" to 15, "saude" to 20)[goal] ?: 0
    if (tempo <= 30) cardioMin = if (goal == "emagrecer" || goal == "condicionamento") 10 else 0
    val finisher = goal == "emagrecer" || goal == "condicionamento"
    val mobility = goal == "saude" || (p.nivel == "iniciante" && (num(p.idade) ?: 0.0) >= 50)
    val daysPlan = asstDays(split.sessions.size, p.dias)
    val focus = p.foco.toSet()
    var maxMin = 0
    val sessions = split.sessions.mapIndexed { k, sk ->
        val (name, spec) = ASST_SESSIONS.getValue(sk)
        val circuit = sk.startsWith("c")
        val slots = spec.split(' ').mapIndexed { order, t -> t.split(':').let { Slot(it[0], it.getOrNull(1)?.toInt() ?: 1, order) } }.toMutableList()
        fun has(list: List<String>) = slots.any { it.slot in list }
        val chest = has(listOf("h_push", "incline_push"))
        val pushC = chest || has(listOf("v_push"))
        val pullC = has(listOf("v_pull", "h_pull"))
        val leg = has(listOf("squat", "hinge", "lunge", "glute", "quad_iso", "ham_curl"))
        fun add(slot: String, pri: Int) { slots += Slot(slot, pri, 50 + slots.size) }
        // Prioridades da pessoa: um exercício a mais para o grupo nas fichas em que ele já aparece
        if ("peito" in focus && chest) add("chest_iso", 1)
        if ("costas" in focus && pullC) add("lat_iso", 1)
        if ("ombros" in focus && (pushC || pullC)) add("lat_raise", 1)
        if ("bracos" in focus) { if (pullC) add("biceps", 1); if (pushC) add("triceps", 1) }
        if ("pernas" in focus && leg) add("quad_iso", 1)
        if ("gluteos" in focus && leg) { add("glute", 1); add("glute", 2) }
        if ("abdomen" in focus) add("core", 1)
        val extra = (if (mobility) 4 else 0) + (if (finisher) 5 else 0) + 5
        var budget = (tempo - cardioMin - extra).toDouble()
        val used = mutableSetOf<String>()
        val chosen = mutableListOf<Chosen>()
        var cIdx = 0
        for (pri in 1..3) {
            for (s in slots.filter { it.pri == pri }.sortedBy { it.order }) {
                if (chosen.size >= 9) break
                var slot = s.slot
                var id = asstPick(slot, p, env, avoid, used, k, rnd)
                if (id == null) for (alt in ASST_FALLBACK[slot] ?: emptyList()) {
                    id = asstPick(alt, p, env, avoid, used, k, rnd)
                    if (id != null) { slot = alt; break }
                }
                if (id == null) continue
                val exd = ex(id)!!
                val role = if (slot in ASST_COMPOUND) 'c' else 'i'
                val (sets, reps, rest) = asstScheme(p, role, if (role == 'c') cIdx else 9, exd.kind, cardioMin, circuit, slot)
                val cost = asstMinutes(sets, rest, exd.kind)
                if (chosen.size >= 3 && cost > budget) continue
                budget -= cost
                if (role == 'c') cIdx++
                used += id
                chosen += Chosen(id, slot, role, sets, reps, rest, s.order)
            }
        }
        // Ordem final: compostos, isolados, abdômen
        val rank = { x: Chosen -> if (x.slot == "core") 2 else if (x.role == 'c') 0 else 1 }
        chosen.sortWith(compareBy({ rank(it) }, { it.order }))
        val noted = mutableSetOf<String>()
        val items = chosen.map { x -> RoutineItem(uid(), x.id, x.sets, x.reps, x.rest, asstNote(p, x.slot, x.id, noted)) }.toMutableList()
        if (circuit && items.isNotEmpty()) items[0] = items[0].copy(note = "Circuito: faça um exercício após o outro e descanse 1–2 min ao fim de cada volta")
        if (mobility) asstPick("mobility", p, env, avoid, used, k, rnd)?.let { items.add(0, RoutineItem(uid(), it, 2, "30", 0, "Aquecimento")) }
        if (finisher) asstPick("finisher", p, env, avoid, used, k, rnd)?.let { items += RoutineItem(uid(), it, 3, if (ex(it)?.kind == "s") "30" else "15-20", 30, "Final intenso") }
        if (cardioMin > 0) asstPick("cardio", p, env, avoid, used, k, rnd)?.let { items += RoutineItem(uid(), it, 1, cardioMin.toString(), 0, if ('I' in avoid) "Ritmo moderado, sem impacto" else "Ritmo moderado") }
        val minutes = Math.round(5 + items.sumOf { asstMinutes(it.sets, it.rest, ex(it.exId)?.kind ?: "w") } + cardioMin).toInt()
        maxMin = maxOf(maxMin, minutes)
        TemplateRoutine(name, daysPlan?.get(k) ?: emptyList(), items)
    }
    val why = mutableListOf(split.why, ASST_GOAL_WHY[goal] ?: "")
    if (p.nivel == "iniciante") why += "Iniciante: priorizei máquinas e exercícios simples. Foque na técnica e aumente a carga aos poucos."
    val restr = p.restr.mapNotNull { ASST_RESTR_WHY[it] }
    if (restr.isNotEmpty()) why += "Cuidados com: " + restr.joinToString("; ") + "."
    val bmi = asstBmi(p)
    val age = num(p.idade)
    if ("impacto" !in p.restr && "joelho" !in p.restr && ((bmi != null && bmi >= 30) || (age != null && age >= 55))) {
        why += "Para poupar as articulações, deixei de fora exercícios com saltos e impacto."
    }
    if (focus.isNotEmpty()) why += "Mais volume para: " + p.foco.joinToString(", ") { optLabel("foco", it).lowercase() } + "."
    why += if (daysPlan != null) "Dias: ${sessions.joinToString(" · ") { "${it.name.substringBefore(" — ")} ${daysLabel(it.days)}" }}."
    else "Treine na ordem (${sessions.indices.joinToString(" → ") { letter(it) }}) nos ${p.dias} dias que preferir — o app sugere a próxima ficha."
    return AsstOption(split.name, "${p.dias}x por semana · cerca de ${Math.round(maxMin / 5.0) * 5} min", why.filter { it.isNotEmpty() }, sessions, "Meu treino — ${split.name}")
}

private fun asstNote(p: Profile, slot: String, id: String, noted: MutableSet<String>): String {
    for (r in p.restr) for ((slots, txt) in ASST_RESTR_NOTE[r] ?: emptyList()) {
        if (txt !in noted && slot in slots.split(' ')) {
            noted += txt
            return txt
        }
    }
    return ASST_EX_NOTE[id] ?: ""
}

fun AppData.asstGenerateLocal(p: Profile, seed: Int = 0): List<AsstOption> = asstSplitKeys(p).map { asstBuildOption(p, it, seed) }
