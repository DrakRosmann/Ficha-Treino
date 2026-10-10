package app.ficha.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ficha.ai.AiMeal
import app.ficha.data.AppData
import app.ficha.data.DietGoal
import app.ficha.data.Food
import app.ficha.data.FoodEntry
import app.ficha.data.Foods
import app.ficha.data.OpenFoodFacts
import app.ficha.data.Portion
import app.ficha.data.Profile
import app.ficha.data.Tdee
import app.ficha.data.uid
import app.ficha.logic.ACTIVITY
import app.ficha.logic.GOALS
import app.ficha.logic.PROT
import app.ficha.logic.RATES
import app.ficha.logic.adaptiveTDEE
import app.ficha.logic.addEntry
import app.ficha.logic.bodyHeight
import app.ficha.logic.deleteEntry
import app.ficha.logic.dietTargets
import app.ficha.logic.dietWeight
import app.ficha.logic.fmt
import app.ficha.logic.fmtIn
import app.ficha.logic.fmtInt
import app.ficha.logic.formulaTDEE
import app.ficha.logic.num
import app.ficha.logic.replaceEntry
import app.ficha.logic.resolved
import app.ficha.logic.shortFoodName
import app.ficha.logic.toggleFav
import app.ficha.logic.updateFood
import app.ficha.ui.LocalApp
import app.ficha.ui.components.ConnectedChoice
import app.ficha.ui.components.KeyValue
import app.ficha.ui.components.Seg
import app.ficha.ui.components.SegmentedList
import app.ficha.ui.theme.Fx
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private val MEAL_CHOICES get() = Foods.MEALS.indices.map { it.toString() to Foods.mealShort(it) }

/* ---------- Adicionar alimento ---------- */
@Composable
fun AddFoodSheet(
    data: AppData,
    meal: Int,
    dayKey: String,
    onDismiss: () -> Unit,
    onFood: (Food) -> Unit,
    onAiText: () -> Unit,
    onPhoto: (label: Boolean) -> Unit,
    onBarcode: () -> Unit,
    onCustom: () -> Unit,
) {
    val app = LocalApp.current
    var q by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(if (data.food.recent.isNotEmpty()) "recent" else "fav") }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box {
            Column(Modifier.fillMaxHeight(0.94f).imePadding()) {
                Text("Adicionar em ${Foods.MEALS[meal]}", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true, shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Rounded.Close, "Limpar") } },
                    placeholder = { Text("Buscar alimento (ex.: arroz cozido)") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AssistChip(onClick = onAiText, label = { Text("Descrever com IA") }, leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp)) }) }
                    item { AssistChip(onClick = { onPhoto(false) }, label = { Text("Foto do prato") }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp)) }) }
                    item { AssistChip(onClick = onBarcode, label = { Text("Código de barras") }, leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, null, Modifier.size(18.dp)) }) }
                    item { AssistChip(onClick = { onPhoto(true) }, label = { Text("Foto do rótulo") }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp)) }) }
                    item { AssistChip(onClick = onCustom, label = { Text("Criar alimento") }, leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) }) }
                }
                if (q.isBlank()) LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("recent" to "Recentes", "fav" to "Favoritos", "meals" to "Refeições", "mine" to "Meus alimentos")) { (v, l) ->
                        FilterChip(selected = tab == v, onClick = { tab = v }, label = { Text(l) })
                    }
                }
                val D = data.food
                val listState = rememberLazyListState()
                // A lista tem chaves: sem isso, ao digitar ela fica presa num item que mudou de lugar
                LaunchedEffect(q, tab) { listState.scrollToItem(0) }
                LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (q.isBlank() && tab == "meals") {
                        if (D.meals.isEmpty()) item { EmptyHint("Monte uma refeição e toque em “Salvar como refeição pronta”. Depois ela entra inteira num toque.") }
                        items(D.meals, key = { it.id }) { ml ->
                            ListItem(
                                onClick = {
                                    app.update { d -> d.updateFood { f -> ml.items.fold(f) { acc, x -> acc.addEntry(dayKey, x.toEntry(meal)) } } }
                                    app.toast("${ml.n} adicionada em ${Foods.MEALS[meal]}")
                                    onDismiss()
                                },
                                supportingContent = {
                                    Text(
                                        "${fmtInt(ml.items.sumOf { it.k ?: 0.0 })} kcal · P ${fmt(ml.items.sumOf { it.p ?: 0.0 }, 0)} g · ${ml.items.size} itens: " +
                                            ml.items.take(4).joinToString(", ") { shortFoodName(it.n) } + if (ml.items.size > 4) "…" else "",
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { app.update { d -> d.updateFood { f -> f.copy(meals = f.meals.filter { it.id != ml.id }) } } }) {
                                        Icon(Icons.Rounded.Delete, "Apagar refeição pronta")
                                    }
                                },
                                content = { Text(ml.n) },
                            )
                        }
                    } else {
                        val list = when {
                            q.isNotBlank() -> Foods.search(D, q)
                            tab == "fav" -> D.fav.mapNotNull { Foods.get(D, it) }
                            tab == "mine" -> D.custom.reversed()
                            else -> D.recent.mapNotNull { Foods.get(D, it) }
                        }
                        if (list.isEmpty()) item {
                            EmptyHint(
                                when {
                                    q.isNotBlank() -> "Nada encontrado para “$q”. Tente outra palavra, descreva com IA ou crie o alimento."
                                    tab == "fav" -> "Toque na estrela de um alimento para guardá-lo aqui."
                                    tab == "mine" -> "Alimentos que você criar, ler pelo código de barras ou pela foto do rótulo aparecem aqui."
                                    else -> "Busque um alimento acima. Os que você usar aparecem aqui."
                                },
                            )
                        }
                        items(list, key = { it.id }) { f ->
                            val u = f.u.firstOrNull()
                            ListItem(
                                onClick = { onFood(f) },
                                supportingContent = {
                                    Text("${fmtInt(f.k)} kcal · P ${fmt(f.p, 1)} · C ${fmt(f.c, 1)} · G ${fmt(f.f, 1)} / 100 g" + (u?.let { " · ${it.label} ${fmt(it.g, 0)} g" } ?: ""))
                                },
                                trailingContent = if (f.id in D.fav) ({ Icon(Icons.Rounded.Star, "Favorito", tint = Fx.colors.gold) }) else null,
                                content = { Text(f.n, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
            SnackbarHost(LocalApp.current.snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}

/* ---------- Porção ---------- */
@Composable
fun PortionSheet(data: AppData, food: Food, meal0: Int, entry: FoodEntry?, dayKey: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val units = food.u
    var unit by remember {
        mutableStateOf(if (entry != null) (if (entry.u != null) units.indexOfFirst { it.label == entry.u }.coerceAtLeast(-1) else -1) else if (units.isNotEmpty()) 0 else -1)
    }
    var qty by remember { mutableStateOf(fmtIn(if (entry != null) (if (unit >= 0) entry.q ?: 1.0 else entry.gr ?: 100.0) else if (units.isNotEmpty()) 1.0 else 100.0)) }
    var meal by remember { mutableStateOf(meal0) }
    val grams = (num(qty) ?: 0.0) * (if (unit >= 0) units[unit].g else 1.0)
    val fav = food.id in data.food.fav
    val preview = Foods.entryFor(food, grams, meal, null, null)
    Sheet(onDismiss, food.n) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${fmtInt(food.k)} kcal · P ${fmt(food.p, 1)} · C ${fmt(food.c, 1)} · G ${fmt(food.f, 1)} por 100 g" + (food.src?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (food.id.isEmpty()) app.toast("Salve como alimento para favoritar")
                    else app.update { d -> d.updateFood { it.toggleFav(food.id) } }
                }) { Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarOutline, if (fav) "Tirar dos favoritos" else "Favoritar", tint = if (fav) Fx.colors.gold else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val step = if (unit >= 0) (if (units[unit].g >= 50) 1.0 else 0.5) else 10.0
                FilledTonalButton(onClick = { qty = fmtIn(max(0.0, (num(qty) ?: 0.0) - step)) }, shapes = ButtonDefaults.shapes()) { Text("−") }
                OutlinedTextField(
                    qty, { qty = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                FilledTonalButton(onClick = { qty = fmtIn((num(qty) ?: 0.0) + step) }, shapes = ButtonDefaults.shapes()) { Text("+") }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (units.mapIndexed { i, u -> i to "${u.label} (${fmt(u.g, 0)} g)" } + (-1 to "gramas")).forEach { (i, l) ->
                    FilterChip(selected = unit == i, onClick = {
                        val g = grams
                        unit = i
                        qty = fmtIn(if (i >= 0) max(0.5, Math.round(g / units[i].g * 2) / 2.0) else Math.round(g).toDouble())
                    }, label = { Text(l) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(fmtInt(preview.k) to "kcal", fmt(preview.p, 1) to "proteína", fmt(preview.c, 1) to "carbo", fmt(preview.f, 1) to "gordura").forEach { (v, l) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v, style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFeatureSettings = "tnum"))
                            Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("${fmt(grams, 0)} g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            ConnectedChoice(MEAL_CHOICES, meal.toString()) { meal = it.toInt() }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (grams <= 0) { app.toast("Informe a quantidade"); return@Button }
                val u = if (unit >= 0) units[unit] else null
                val e = Foods.entryFor(food, grams, meal, if (u != null) num(qty) else null, u?.label)
                if (entry != null) app.update { d -> d.updateFood { it.replaceEntry(dayKey, e.copy(id = entry.id)) } }
                else {
                    app.update { d -> d.updateFood { it.addEntry(dayKey, e) } }
                    app.toast("${shortFoodName(food.n)} adicionado em ${Foods.MEALS[meal]}")
                }
                onDismiss()
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                Text(if (entry != null) "Salvar" else "Adicionar", style = MaterialTheme.typography.titleMedium)
            }
            if (entry != null) OutlinedButton(
                onClick = { app.update { d -> d.updateFood { it.deleteEntry(dayKey, entry.id) } }; onDismiss() }, shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Remover") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- Criar alimento ---------- */
@Composable
fun CustomFoodSheet(pre: Food?, onDismiss: () -> Unit, onSaved: (Food) -> Unit) {
    val app = LocalApp.current
    fun v(x: Double?) = x?.let { fmtIn(it) } ?: ""
    var name by remember { mutableStateOf(pre?.n ?: "") }
    var k by remember { mutableStateOf(v(pre?.k?.takeIf { pre.id.isNotEmpty() || it > 0 })) }
    var p by remember { mutableStateOf(v(pre?.p)) }
    var c by remember { mutableStateOf(v(pre?.c)) }
    var f by remember { mutableStateOf(v(pre?.f)) }
    var fi by remember { mutableStateOf(v(pre?.fi)) }
    var u by remember { mutableStateOf(pre?.u?.firstOrNull()?.let { fmtIn(it.g) } ?: "") }
    val editing = pre != null && pre.id.startsWith("c") && pre.n.isNotEmpty() && pre.code == null
    Sheet(onDismiss, if (editing) "Editar alimento" else "Criar alimento") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, placeholder = { Text("Ex.: Pão de queijo da padaria") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Valores por 100 g", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            @Composable
            fun numField(label: String, value: String, set: (String) -> Unit, modifier: Modifier) = OutlinedTextField(
                value, { set(it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }) }, label = { Text(label) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                numField("kcal", k, { k = it }, Modifier.weight(1f)); numField("Proteína", p, { p = it }, Modifier.weight(1f)); numField("Carbo", c, { c = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                numField("Gordura", f, { f = it }, Modifier.weight(1f)); numField("Fibra", fi, { fi = it }, Modifier.weight(1f)); numField("Porção (g)", u, { u = it }, Modifier.weight(1f))
            }
            Text(
                "Se o rótulo traz os valores por porção, divida pela porção em gramas e multiplique por 100. Ou use “Foto do rótulo” para a IA fazer isso.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                val kv = num(k)
                when {
                    name.isBlank() -> app.toast("Dê um nome ao alimento")
                    kv == null -> app.toast("Informe as calorias por 100 g")
                    else -> {
                        val food = Food(
                            id = pre?.id?.takeIf { it.startsWith("c") } ?: ("c" + uid()), n = name.trim(), k = kv.roundToInt().toDouble(),
                            p = num(p) ?: 0.0, c = num(c) ?: 0.0, f = num(f) ?: 0.0, fi = num(fi) ?: 0.0,
                            u = num(u)?.let { listOf(Portion("porção", it)) } ?: emptyList(), src = pre?.src?.takeIf { pre.code != null } ?: "meu alimento", code = pre?.code,
                        )
                        app.update { d -> d.updateFood { fd -> fd.copy(custom = if (fd.custom.any { it.id == food.id }) fd.custom.map { if (it.id == food.id) food else it } else fd.custom + food) } }
                        onSaved(food)
                    }
                }
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) { Text("Salvar e escolher a porção") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/* ---------- Metas ---------- */
@Composable
fun GoalSheet(data: AppData, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val g0 = data.food.goal.resolved()
    var obj by remember { mutableStateOf(g0.obj) }
    var rate by remember { mutableStateOf(g0.rate ?: 0.5) }
    var act by remember { mutableStateOf(g0.act ?: 1.55) }
    var prot by remember { mutableStateOf(g0.prot ?: 2.0) }
    val pr = data.profile ?: Profile()
    // O que falta é decidido ao abrir: senão o campo some assim que a primeira tecla preenche o valor
    val need = remember {
        buildList {
            if (data.dietWeight() == null) add("peso" to "Peso (kg)")
            if (data.bodyHeight() == null) add("altura" to "Altura (cm)")
            if (num(pr.idade) == null) add("idade" to "Idade")
        }
    }
    val askSex = remember { pr.sexo.isEmpty() }
    val draft = DietGoal(obj, rate, act, prot, null)
    val dd = data.copy(food = data.food.copy(goal = draft))
    val tdee = if (data.food.tdee?.src == "dados") data.food.tdee.v else dd.formulaTDEE(draft)
    val tg = dd.dietTargets(tdee, draft)
    Sheet(onDismiss, "Metas da dieta") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            if (need.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                need.forEach { (k, l) ->
                    var v by remember { mutableStateOf("") }
                    OutlinedTextField(
                        v, { x ->
                            v = x.filter { it.isDigit() || it == ',' || it == '.' }.take(if (k == "idade") 3 else 5)
                            num(v)?.let { n -> app.update { d -> d.copy(profile = (d.profile ?: Profile()).let { p -> when (k) { "peso" -> p.copy(peso = fmtIn(n)); "altura" -> p.copy(altura = fmtIn(n)); else -> p.copy(idade = fmtIn(n)) } }) } }
                        }, label = { Text(l) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                    )
                }
            }
            if (askSex) {
                Label2("Sexo")
                ConnectedChoice(listOf("f" to "Feminino", "m" to "Masculino"), pr.sexo) { v -> app.update { d -> d.copy(profile = (d.profile ?: Profile()).copy(sexo = v)) } }
            }
            Label2("Objetivo")
            ConnectedChoice(GOALS, obj) { v ->
                obj = v
                RATES[v]?.let { if (rate !in it) rate = if (v == "ganhar") 0.25 else 0.5 }
            }
            RATES[obj]?.let { rs ->
                Label2("Ritmo (do peso por semana)")
                val w = data.dietWeight()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rs.forEach { r -> FilterChip(selected = rate == r, onClick = { rate = r }, label = { Text("${fmt(r, 2)}%" + (w?.let { " · ${fmt(it * r / 100, 2)} kg" } ?: "")) }) }
                }
            }
            Label2("Nível de atividade")
            SegmentedList(ACTIVITY.map { a ->
                Seg(headline = a.name, supporting = a.desc, trailing = if (act == a.v) ({ Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) }) else null, onClick = { act = a.v })
            }, Modifier)
            Label2("Proteína por kg de peso")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PROT.forEach { v -> FilterChip(selected = prot == v, onClick = { prot = v }, label = { Text("${fmt(v, 1)} g/kg") }) }
            }
            Spacer(Modifier.height(14.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    if (tg != null && tdee != null) {
                        KeyValue("Gasto estimado", "${fmtInt(tdee)} kcal", MaterialTheme.colorScheme.onPrimaryContainer)
                        KeyValue("Meta de calorias", "${fmtInt(tg.k)} kcal", MaterialTheme.colorScheme.onPrimaryContainer)
                        KeyValue("Proteína · Carbo · Gordura", "${fmt(tg.p, 0)} · ${fmt(tg.c, 0)} · ${fmt(tg.f, 0)} g", MaterialTheme.colorScheme.onPrimaryContainer)
                    } else Text("Informe peso, altura e idade para calcular.", modifier = Modifier.padding(vertical = 10.dp))
                }
            }
            Text(
                "O gasto parte da fórmula de Mifflin-St Jeor (ou Katch-McArdle, se você tiver o % de gordura na aba Corpo) e depois é corrigido toda semana com o que você registra e com a tendência do seu peso.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp),
            )
            Button(onClick = {
                val d0 = app.data.copy(food = app.data.food.copy(goal = draft))
                val v = d0.formulaTDEE(draft)
                if (v == null) { app.toast("Informe peso, altura e idade"); return@Button }
                val a = d0.adaptiveTDEE()
                val td = if (a.src == "dados" && a.v != null) Tdee(a.v, System.currentTimeMillis(), "dados") else Tdee(v, System.currentTimeMillis(), "formula")
                app.update { d -> d.updateFood { it.copy(goal = draft, tdee = td) } }
                app.toast("Metas salvas")
                onDismiss()
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) { Text("Salvar metas", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun Label2(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp))
}

/* ---------- IA ---------- */
@Composable
fun AiTextSheet(onDismiss: () -> Unit, onRun: (String) -> Unit) {
    val app = LocalApp.current
    var t by remember { mutableStateOf("") }
    Sheet(onDismiss, "Descrever refeição") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                t, { t = it }, label = { Text("O que você comeu?") }, minLines = 4,
                placeholder = { Text("Ex.: 4 colheres de arroz, 1 concha de feijão, 1 filé de frango grelhado e salada de alface com tomate") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "A IA identifica cada alimento e estima as quantidades e os macros. Você confere antes de salvar. Usa a sua chave da Anthropic.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { if (t.trim().length < 3) app.toast("Descreva o que você comeu") else onRun(t.trim()) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Calcular")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun AiReviewSheet(res: AiMeal, meal0: Int, dayKey: String, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val items = remember { res.itens.filter { it.nome.isNotBlank() && it.gramas > 0 } }
    val on = remember { mutableStateListOf(*Array(items.size) { true }) }
    val grams = remember { mutableStateListOf(*items.map { fmtIn(it.gramas) }.toTypedArray()) }
    var meal by remember { mutableStateOf(meal0) }
    fun macros(i: Int): DoubleArray {
        val x = items[i]
        val r = (num(grams[i]) ?: 0.0) / x.gramas
        return doubleArrayOf((x.kcal * r).roundToInt().toDouble(), Math.round(x.proteina * r * 10) / 10.0, Math.round(x.carboidrato * r * 10) / 10.0, Math.round(x.gordura * r * 10) / 10.0)
    }
    Sheet(onDismiss, "Confira a refeição") {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            if (res.observacao.isNotBlank()) Text(res.observacao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
            items.forEachIndexed { i, x ->
                val m = macros(i)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Checkbox(on[i], { on[i] = it })
                    Column(Modifier.weight(1f)) {
                        Text(x.nome, style = MaterialTheme.typography.bodyLarge)
                        Text("${fmtInt(m[0])} kcal · P ${fmt(m[1], 0)} · C ${fmt(m[2], 0)} · G ${fmt(m[3], 0)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        grams[i], { grams[i] = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, singleLine = true, suffix = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(96.dp),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            val sel = items.indices.filter { on[it] }.map { macros(it) }
            Text(
                "${sel.size} ${if (sel.size == 1) "item" else "itens"} · ${fmtInt(sel.sumOf { it[0] })} kcal · P ${fmt(sel.sumOf { it[1] }, 0)} · C ${fmt(sel.sumOf { it[2] }, 0)} · G ${fmt(sel.sumOf { it[3] }, 0)}",
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
            Spacer(Modifier.height(12.dp))
            ConnectedChoice(MEAL_CHOICES, meal.toString()) { meal = it.toInt() }
            Text("Estimativa da IA: ajuste as gramas se souber a quantidade.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
            Button(onClick = {
                val idx = items.indices.filter { on[it] && (num(grams[it]) ?: 0.0) > 0 }
                if (idx.isEmpty()) { app.toast("Marque pelo menos um item"); return@Button }
                app.update { d ->
                    d.updateFood { f ->
                        idx.fold(f) { acc, i ->
                            val m = macros(i)
                            acc.addEntry(dayKey, FoodEntry(m = meal, n = items[i].nome, gr = num(grams[i]), k = m[0], p = m[1], c = m[2], f = m[3], fi = 0.0, ai = true))
                        }
                    }
                }
                app.toast("${idx.size} ${if (idx.size > 1) "itens adicionados" else "item adicionado"} em ${Foods.MEALS[meal]}")
                onDismiss()
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) { Text("Adicionar à refeição", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- Código de barras (Open Food Facts) ---------- */
@Composable
fun BarcodeSheet(data: AppData, onDismiss: () -> Unit, onFood: (Food) -> Unit, onCustom: (Food) -> Unit, onLabel: () -> Unit) {
    val app = LocalApp.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    fun lookup(raw: String) {
        val c = raw.filter { it.isDigit() }
        if (c.length < 8) { app.toast("Digite os 8 a 14 números do código"); return }
        data.food.custom.find { it.code == c }?.let { onFood(it); return }
        loading = true
        msg = "Buscando no Open Food Facts…"
        scope.launch {
            try {
                val p = OpenFoodFacts.product(c)
                when {
                    p == null -> msg = "Produto não encontrado na base. Use “Foto do rótulo” ou “Criar alimento”."
                    p.missing -> {
                        app.toast("Produto sem tabela nutricional na base — preencha pelo rótulo")
                        onCustom(p.food.copy(k = null, p = null, c = null, f = null))
                    }
                    else -> {
                        app.update { d -> d.updateFood { f -> f.copy(custom = f.custom + p.food) } }
                        onFood(p.food)
                    }
                }
            } catch (e: Exception) {
                msg = "Não foi possível consultar agora. Confira a internet e tente de novo."
            } finally {
                loading = false
            }
        }
    }
    Sheet(onDismiss, "Código de barras") {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = {
                val opts = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).build()
                GmsBarcodeScanning.getClient(context, opts).startScan()
                    .addOnSuccessListener { b -> b.rawValue?.let { code = it; lookup(it) } }
                    .addOnFailureListener { app.toast("Sem leitor de código neste aparelho — digite o número") }
            }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight)) {
                Icon(Icons.Rounded.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Ler com a câmera", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                code, { code = it.filter(Char::isDigit).take(14) }, label = { Text("Ou digite o número") }, placeholder = { Text("7891234567890") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
            if (msg.isNotEmpty()) Text(msg, style = MaterialTheme.typography.bodyMedium, fontWeight = if (loading) FontWeight.Normal else FontWeight.Medium)
            Text("Os dados vêm do Open Food Facts, uma base aberta e colaborativa de produtos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLabel, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Foto do rótulo") }
                Button(onClick = { lookup(code) }, enabled = !loading, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Buscar produto") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
