package app.ficha.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ficha.ai.AiError
import app.ficha.ai.AiMeal
import app.ficha.ai.FoodAI
import app.ficha.data.AppData
import app.ficha.data.Food
import app.ficha.data.FoodEntry
import app.ficha.data.Foods
import app.ficha.data.Portion
import app.ficha.logic.DAY
import app.ficha.logic.DAY_MS
import app.ficha.logic.MICROS
import app.ficha.logic.addDays
import app.ficha.logic.adaptiveTDEE
import app.ficha.logic.addEntry
import app.ficha.logic.saveMeal
import app.ficha.logic.currentTDEE
import app.ficha.logic.dateLong
import app.ficha.logic.dateOf
import app.ficha.logic.dayKey
import app.ficha.logic.dayMicros
import app.ficha.logic.dayOf
import app.ficha.logic.dayTotals
import app.ficha.logic.dietCheckIn
import app.ficha.logic.dietTargets
import app.ficha.logic.fmt
import app.ficha.logic.fmtInt
import app.ficha.logic.jsDay
import app.ficha.logic.mealNow
import app.ficha.logic.sex
import app.ficha.logic.startOfDay
import app.ficha.logic.totals
import app.ficha.logic.updateFood
import app.ficha.logic.waterGoal
import app.ficha.logic.weightSlope
import app.ficha.logic.withDay
import app.ficha.ui.LocalApp
import app.ficha.ui.components.AiBusyDialog
import app.ficha.ui.components.AiKeySheet
import app.ficha.ui.components.CardBox
import app.ficha.ui.components.Gutter
import app.ficha.ui.components.Hint
import app.ficha.ui.components.KeyValue
import app.ficha.ui.components.Screen
import app.ficha.ui.components.SectionHeader
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.components.aiReady
import app.ficha.ui.components.jpegB64ForAi
import app.ficha.ui.components.rememberPhotoPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/** O que a tela da dieta está mostrando por cima (painéis e diálogos). */
sealed interface DietSheet {
    data class Add(val meal: Int) : DietSheet
    data class Portion(val food: Food, val meal: Int, val entry: FoodEntry? = null) : DietSheet
    data class Custom(val pre: Food? = null, val meal: Int) : DietSheet
    data object Goal : DietSheet
    data class AiText(val meal: Int) : DietSheet
    data class AiReview(val meal: Int, val res: AiMeal) : DietSheet
    data class Barcode(val meal: Int) : DietSheet
    data object Key : DietSheet
}

fun dayLabel(t: Long): String {
    val diff = Math.round((startOfDay(System.currentTimeMillis()) - t) / DAY_MS.toDouble())
    return when (diff) { 0L -> "Hoje"; 1L -> "Ontem"; -1L -> "Amanhã"; else -> dateLong(t) }
}

@Composable
fun DietScreen(data: AppData) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var offset by rememberSaveable { mutableStateOf(0) }
    var sheet by remember { mutableStateOf<DietSheet?>(null) }
    var busy by remember { mutableStateOf<Pair<String, Job>?>(null) }
    val t = addDays(startOfDay(System.currentTimeMillis()), offset)
    val key = dayKey(t)
    val day = data.dayOf(key)
    val tot = day.totals()
    val tg = data.dietTargets()

    LaunchedEffect(Unit) { app.data.dietCheckIn()?.let { nd -> app.update { nd } } }

    /** IA de refeição (texto ou foto) ou de rótulo, com o indicador enquanto roda. */
    fun runAi(meal: Int, label: Boolean, text: String?, image: String?) {
        val k = aiReady(context, app::toast) { sheet = DietSheet.Key } ?: return
        val title = if (label) "Lendo o rótulo…" else if (image != null) "Analisando a foto…" else "Calculando a refeição…"
        val job = scope.launch {
            try {
                if (label) {
                    val r = FoodAI.label(k, image!!)
                    if (!r.legivel) app.toast("Não deu para ler a tabela nutricional. Tente uma foto mais de perto.")
                    else {
                        sheet = DietSheet.Custom(
                            Food(
                                id = "", n = r.nome, k = r.kcal_100g, p = r.proteina_100g, c = r.carboidrato_100g, f = r.gordura_100g, fi = r.fibra_100g,
                                u = if (r.porcao_g > 0) listOf(Portion("porção", r.porcao_g)) else emptyList(),
                            ),
                            meal,
                        )
                        if (r.observacao.isNotBlank()) app.toast(r.observacao)
                    }
                } else {
                    val r = FoodAI.meal(k, text, image)
                    if (r.itens.none { it.nome.isNotBlank() && it.gramas > 0 }) app.toast("A IA não identificou alimentos. Tente descrever com mais detalhes.")
                    else sheet = DietSheet.AiReview(meal, r)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) app.toast((e as? AiError)?.message ?: "Não foi possível usar a IA agora")
            } finally {
                busy = null
            }
        }
        busy = title to job
    }

    var photoFor by remember { mutableStateOf<Pair<Int, Boolean>?>(null) } // refeição, é rótulo
    val picker = rememberPhotoPicker(onError = app::toast) { b ->
        val (m, label) = photoFor ?: (mealNow() to false)
        runAi(m, label, null, jpegB64ForAi(b))
    }
    var photoMenu by remember { mutableStateOf(false) }
    val askPhoto = { meal: Int, label: Boolean -> photoFor = meal to label; photoMenu = true }

    Screen(
        title = dayLabel(t), subtitle = dateLong(t),
        actions = {
            IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Dia anterior") }
            IconButton(onClick = { if (offset < 0) offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo dia") }
            IconButton(onClick = { sheet = DietSheet.Goal }) { Icon(Icons.Rounded.TrackChanges, "Metas") }
        },
    ) {
        if (data.food.goal == null) item {
            HeroCard(label = "METAS", title = "Defina suas metas") {
                Text(
                    "Com seu peso, altura, idade e objetivo o app calcula as calorias e os macros do dia. Depois ele ajusta sozinho, pela sua evolução.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = { sheet = DietSheet.Goal }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Calcular minhas metas") }
            }
        }
        data.food.tdeeNote?.takeIf { System.currentTimeMillis() - it.at < 7 * DAY_MS }?.let { note ->
            item {
                Spacer(Modifier.height(10.dp))
                CardBox(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("Gasto atualizado: ${fmtInt(note.from)} → ${fmtInt(note.to)} kcal", style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "Pelos seus registros e pela tendência do peso, você gasta ${if ((note.to ?: 0.0) > (note.from ?: 0.0)) "mais" else "menos"} do que o estimado. As metas foram ajustadas.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    TextButton(onClick = { app.update { it.updateFood { f -> f.copy(tdeeNote = null) } } }) { Text("Entendi") }
                }
            }
        }
        item { DaySummary(tot.k, tot.p, tot.c, tot.f, tg) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { sheet = DietSheet.Add(mealNow()) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar alimento", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { if (aiReady(context, app::toast) { sheet = DietSheet.Key } != null) sheet = DietSheet.AiText(mealNow()) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Descrever", maxLines = 1)
                    }
                    Box(Modifier.weight(1f)) {
                        FilledTonalButton(onClick = { askPhoto(mealNow(), false) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Foto do prato", maxLines = 1)
                        }
                        DropdownMenu(photoMenu, { photoMenu = false }) {
                            DropdownMenuItem(text = { Text("Tirar foto") }, onClick = { photoMenu = false; picker.camera() })
                            DropdownMenuItem(text = { Text("Escolher da galeria") }, onClick = { photoMenu = false; picker.gallery() })
                        }
                    }
                }
            }
        }
        item { WaterCard(day.w, data.waterGoal()) { d -> app.update { it.updateFood { f -> f.withDay(key) { x -> x.copy(w = max(0, x.w + d)) } } } } }
        val yKey = dayKey(addDays(t, -1))
        Foods.MEALS.forEachIndexed { mi, m ->
            val items = day.e.filter { it.m == mi }
            val yItems = data.dayOf(yKey).e.filter { it.m == mi }
            item(key = "meal-$mi") {
                Row(Modifier.fillMaxWidth().padding(start = Gutter + 4.dp, end = Gutter, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m, style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
                    if (items.isNotEmpty()) Text("${fmtInt(items.sumOf { it.k ?: 0.0 })} kcal", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalIconButton(onClick = { sheet = DietSheet.Add(mi) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.padding(start = 8.dp).size(36.dp)) {
                        Icon(Icons.Rounded.Add, "Adicionar em $m", Modifier.size(20.dp))
                    }
                }
                if (items.isNotEmpty()) {
                    SegmentedList(items.map { e ->
                        Seg(
                            key = e.id, headline = e.n,
                            supporting = (if (e.q != null && e.u != null) "${fmt(e.q, 2)} ${Foods.unitLabel(e.q, e.u)} · " else "") +
                                "${fmt(e.gr, 0)} g · P ${fmt(e.p, 0)} · C ${fmt(e.c, 0)} · G ${fmt(e.f, 0)}",
                            trailing = { Text(fmtInt(e.k), style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum")) },
                            onClick = {
                                val f = Foods.get(data.food, e.ref) ?: Food(
                                    id = "", n = e.n, k = (e.k ?: 0.0) / (e.gr ?: 1.0) * 100, p = (e.p ?: 0.0) / (e.gr ?: 1.0) * 100,
                                    c = (e.c ?: 0.0) / (e.gr ?: 1.0) * 100, f = (e.f ?: 0.0) / (e.gr ?: 1.0) * 100, fi = (e.fi ?: 0.0) / (e.gr ?: 1.0) * 100,
                                )
                                sheet = DietSheet.Portion(f, e.m, e)
                            },
                        )
                    })
                    if (items.size > 1) TextButton(onClick = {
                        app.prompt("Refeição pronta", "$m de sempre", "Nome da refeição pronta") { n ->
                            if (n.isNotBlank()) {
                                app.update { d -> d.updateFood { f -> f.saveMeal(n.trim(), items) } }
                                app.toast("Refeição salva — está em Adicionar → Refeições")
                            }
                        }
                    }, modifier = Modifier.padding(horizontal = Gutter)) { Text("Salvar como refeição pronta") }
                } else if (yItems.isNotEmpty()) {
                    TextButton(onClick = {
                        app.update { d -> d.updateFood { f -> yItems.fold(f) { acc, e -> acc.addEntry(key, e) } } }
                        app.toast("$m de ontem repetido")
                    }, modifier = Modifier.padding(horizontal = Gutter)) {
                        Text("Repetir de ontem (${yItems.size} ${if (yItems.size > 1) "itens" else "item"} · ${fmtInt(yItems.sumOf { it.k ?: 0.0 })} kcal)")
                    }
                }
            }
        }
        if (day.e.isNotEmpty()) {
            item { SectionHeader("Micronutrientes") }
            item { MicrosCard(data, key) }
        }
        item { SectionHeader("Últimos 7 dias") }
        item { WeekCard(data, tg?.k) }
        item {
            Hint("Alimentos da Tabela Brasileira de Composição de Alimentos (TACO, NEPA/UNICAMP). Os valores são estimativas — para orientação individual, procure um nutricionista.")
        }
    }

    when (val s = sheet) {
        is DietSheet.Add -> AddFoodSheet(
            data, s.meal, key, onDismiss = { sheet = null },
            onFood = { f -> sheet = DietSheet.Portion(f, s.meal) },
            onAiText = { if (aiReady(context, app::toast) { sheet = DietSheet.Key } != null) sheet = DietSheet.AiText(s.meal) },
            onPhoto = { label -> sheet = null; askPhoto(s.meal, label) },
            onBarcode = { sheet = DietSheet.Barcode(s.meal) },
            onCustom = { sheet = DietSheet.Custom(null, s.meal) },
        )
        is DietSheet.Portion -> PortionSheet(data, s.food, s.meal, s.entry, key, onDismiss = { sheet = null })
        is DietSheet.Custom -> CustomFoodSheet(s.pre, onDismiss = { sheet = null }) { f -> sheet = DietSheet.Portion(f, s.meal) }
        DietSheet.Goal -> GoalSheet(data) { sheet = null }
        is DietSheet.AiText -> AiTextSheet(onDismiss = { sheet = null }) { txt -> sheet = null; runAi(s.meal, false, txt, null) }
        is DietSheet.AiReview -> AiReviewSheet(s.res, s.meal, key) { sheet = null }
        is DietSheet.Barcode -> BarcodeSheet(data, onDismiss = { sheet = null }, onFood = { f -> sheet = DietSheet.Portion(f, s.meal) }, onCustom = { f -> sheet = DietSheet.Custom(f, s.meal) }, onLabel = { sheet = null; askPhoto(s.meal, true) })
        DietSheet.Key -> AiKeySheet({ sheet = null })
        null -> {}
    }
    busy?.let { (title, job) -> AiBusyDialog(title) { job.cancel(); busy = null } }
}

@Composable
private fun DaySummary(k: Double, p: Double, c: Double, f: Double, tg: app.ficha.data.Macros?) {
    val kT = tg?.k ?: 0.0
    val left = kT - k
    val over = kT > 0 && k > kT * 1.05
    Spacer(Modifier.height(8.dp))
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { if (kT > 0) (k / kT).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.size(132.dp),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmtInt(abs(if (kT > 0) left else k)), style = MaterialTheme.typography.headlineSmallEmphasized.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold)
                    Text(if (kT <= 0) "kcal" else if (left >= 0) "restantes" else "acima", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { Text("Meta", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (kT > 0) fmtInt(kT) else "—", style = MaterialTheme.typography.labelLarge) }
                Row { Text("Consumido", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(fmtInt(k), style = MaterialTheme.typography.labelLarge) }
                MacroBar("Proteína", p, tg?.p, MaterialTheme.colorScheme.primary)
                MacroBar("Carboidrato", c, tg?.c, MaterialTheme.colorScheme.tertiary)
                MacroBar("Gordura", f, tg?.f, MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun MacroBar(label: String, v: Double, t: Double?, color: Color, unit: String = "g", dec: Int = 0, limit: Boolean = false) {
    val frac = if (t != null && t > 0) (v / t).toFloat() else 0f
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("${fmt(v, dec)}${if (t != null && t > 0) " / ${fmt(t, 0)} $unit" else " $unit"}", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"))
        }
        LinearProgressIndicator(
            progress = { frac.coerceIn(0f, 1f) },
            color = if (limit && frac > 1f) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(6.dp),
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun WaterCard(w: Int, goal: Int, onAdd: (Int) -> Unit) {
    Spacer(Modifier.height(4.dp))
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Água", style = MaterialTheme.typography.titleMediumEmphasized)
            Text(" · meta ${fmt(goal / 1000.0, 1)} L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("${fmt(w / 1000.0, 2)} L", style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0 until 8) {
                val on = w >= (i + 1) * goal / 8.0
                Icon(if (on) Icons.Rounded.WaterDrop else Icons.Outlined.WaterDrop, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAdd(-250) }, shapes = ButtonDefaults.shapes()) { Text("−") }
            FilledTonalButton(onClick = { onAdd(250) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("+250 ml") }
            FilledTonalButton(onClick = { onAdd(500) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("+500 ml") }
        }
    }
}

@Composable
private fun MicrosCard(data: AppData, key: String) {
    val m = data.dayMicros(key)
    val sx = data.sex()
    CardBox {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroBar("Fibras", m.fi, if (sx == "m") 30.0 else 25.0, MaterialTheme.colorScheme.primary, "g", 1)
            MICROS.forEach { mc -> MacroBar(mc.name + if (mc.limit) " (limite)" else "", m.tot[mc.idx], mc.ref(sx), MaterialTheme.colorScheme.tertiary, "mg", mc.dec, mc.limit) }
        }
        Text(
            "Pela Tabela TACO. " + (if (m.cover < 0.98) "Cobre ${Math.round(m.cover * 100)}% das calorias do dia: itens da IA, do código de barras e criados por você não têm micronutrientes. " else "") +
                "Referências diárias para adultos (OMS e IOM).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun WeekCard(data: AppData, target: Double?) {
    val end = startOfDay(System.currentTimeMillis())
    val cols = (6 downTo 0).map { i -> addDays(end, -i).let { t -> t to data.dayTotals(dayKey(t)).k } }
    val top = maxOf(target?.let { it * 1.25 } ?: 0.0, cols.maxOf { it.second }, 1.0)
    val logged = cols.filter { it.second > 0 }
    val avg = if (logged.isNotEmpty()) logged.sumOf { it.second } / logged.size else 0.0
    val a = data.adaptiveTDEE()
    val wk = data.weightSlope(28)?.perDay?.let { it * 7 }
    val cs = MaterialTheme.colorScheme
    CardBox {
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            cols.forEach { (t, k) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Canvas(Modifier.width(22.dp).height(96.dp)) {
                        val h = size.height
                        drawRoundRect(cs.surfaceContainerHighest, Offset.Zero, size, CornerRadius(8.dp.toPx()))
                        val bh = (k / top * h).toFloat()
                        if (bh > 0) drawRoundRect(if (target != null && k > target * 1.05) cs.error else cs.primary, Offset(0f, h - bh), Size(size.width, bh), CornerRadius(8.dp.toPx()))
                        if (target != null) {
                            val y = (h - target / top * h).toFloat()
                            drawLine(cs.onSurface, Offset(-4f, y), Offset(size.width + 4f, y), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                        }
                    }
                    Text(DAY[dateOf(t).jsDay()], style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        KeyValue("Média registrada", if (avg > 0) "${fmtInt(avg)} kcal" else "—")
        KeyValue("Gasto estimado", (data.currentTDEE() ?: a.v)?.let { "${fmtInt(it)} kcal" } ?: "—")
        KeyValue("Tendência do peso", wk?.let { "${if (it > 0) "+" else ""}${fmt(it, 2)} kg/sem" } ?: "—")
        Text(
            if (a.src == "dados") "O gasto é calculado com ${a.logged} dias registrados e ${a.weighIns} pesagens das últimas 4 semanas, e atualizado toda semana."
            else "Por enquanto o gasto vem da fórmula. Registre o que come e se pese (aba Corpo) algumas vezes por semana: com 10 dias de registro e 3 pesagens em pelo menos 10 dias, o app passa a calcular o seu gasto real.",
            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
        )
    }
}

/** Card da dieta na tela Hoje. */
@Composable
fun DietTodayCard(data: AppData, onClick: () -> Unit) {
    if (data.food.goal == null && data.food.days.isEmpty()) return
    val key = dayKey(System.currentTimeMillis())
    val tot = data.dayTotals(key)
    val tg = data.dietTargets()
    Spacer(Modifier.height(10.dp))
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter)) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text("Dieta de hoje", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.weight(1f))
                Text("${fmtInt(tot.k)}${tg?.let { " / ${fmtInt(it.k)}" } ?: ""} kcal", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (tg?.k != null && tg.k > 0) (tot.k / tg.k).toFloat().coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, drawStopIndicator = {},
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Proteína ${fmt(tot.p, 0)}${tg?.p?.let { " / ${fmt(it, 0)}" } ?: ""} g · Água ${fmt(data.dayOf(key).w / 1000.0, 1)} L",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
