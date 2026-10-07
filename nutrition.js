// Aba Dieta: registro das refeições, metas de calorias e macros, água, gasto adaptativo,
// alimentos da TACO, código de barras (Open Food Facts) e IA (descrever refeição, foto do prato e do rótulo).
// Usa o estado e as funções do app.js, do body.js (peso e altura) e do assistant.js (chave e SDK da IA).
'use strict';

/* ================= Alimentos ================= */
const MEALS = ['Café da manhã', 'Almoço', 'Lanche', 'Jantar'];
// Porções caseiras por tipo de alimento (gramas aproximadas)
const UNIT_RULES = [
  [/^arroz\b.*cozid/, [['colher de sopa', 25], ['escumadeira', 80]]],
  [/^feijao\b.*cozid/, [['concha', 100], ['colher de sopa', 20]]],
  [/^ovo\b.*inteiro/, [['unidade', 50]]],
  [/^ovo\b.*clara/, [['unidade', 30]]],
  [/^ovo\b.*gema/, [['unidade', 18]]],
  [/^pao\b.*frances/, [['unidade', 50]]],
  [/^pao\b.*forma/, [['fatia', 25]]],
  [/^pao\b/, [['fatia', 30]]],
  [/^banana\b/, [['unidade', 70]]],
  [/^maca\b/, [['unidade', 130]]],
  [/^(laranja|tangerina|pera|pessego|kiwi|caqui)\b/, [['unidade', 140]]],
  [/^(mamao|melao|melancia|abacaxi|manga)\b/, [['fatia', 120]]],
  [/^(morango|uva|acerola)\b/, [['xícara', 140]]],
  [/^(leite|iogurte|bebida lactea)\b/, [['copo de 200 ml', 206]]],
  [/^cafe\b.*infus/, [['xícara', 50]]],
  [/^(suco|refrigerante|agua de coco|cerveja|bebida)/, [['copo de 200 ml', 200], ['lata de 350 ml', 350]]],
  [/^(aveia|farelo|farinha)\b/, [['colher de sopa', 15]]],
  [/^(azeite|oleo)\b/, [['colher de sopa', 13], ['colher de chá', 4]]],
  [/^(manteiga|margarina)\b/, [['colher de chá', 5], ['colher de sopa', 15]]],
  [/^acucar\b/, [['colher de chá', 5], ['colher de sopa', 12]]],
  [/^(mel|doce|geleia)\b/, [['colher de sopa', 20]]],
  [/^queijo\b/, [['fatia', 20]]],
  [/^requeijao\b/, [['colher de sopa', 15]]],
  [/^(frango|carne|peixe|tilapia|salmao|atum|porco|lombo|file|bife|figado|peru|pescada|merluza|sardinha)\b/, [['filé', 100], ['pedaço', 60]]],
  [/^(batata|mandioca|aipim|inhame|cara|abobora)\b/, [['pedaço', 100], ['colher de sopa', 25]]],
  [/^(macarrao|massa|lasanha|nhoque)\b/, [['pegador', 110], ['prato raso', 200]]],
  [/^cuscuz\b/, [['fatia', 100]]],
  [/^tapioca\b/, [['unidade', 90]]],
  [/^(alface|rucula|agriao|couve|espinafre|repolho|acelga)\b/, [['xícara', 20]]],
  [/^tomate\b/, [['unidade', 100], ['fatia', 15]]],
  [/^(cenoura|beterraba|pepino|abobrinha|chuchu|vagem|quiabo)\b/, [['colher de sopa', 15], ['unidade', 100]]],
  [/^(brocolis|couve-flor|couve flor)\b/, [['ramo', 30]]],
  [/^(amendoim|castanha|noz|nozes|amendoa|macadamia|pistache)/, [['punhado', 30]]],
  [/^(biscoito|bolacha)\b/, [['unidade', 7]]],
  [/^(bolo|torta|pizza)\b/, [['fatia', 80]]]
];
// "colher de sopa" → "colheres de sopa" (só a primeira palavra)
function unitLabel(q, u) {
  if (!(q > 1)) return u;
  return u.replace(/^(\S+)/, w => /ão$/.test(w) ? w.slice(0, -2) + 'ões' : /[rz]$/.test(w) ? w + 'es' : /l$/.test(w) ? w.slice(0, -1) + 'is' : w + 's');
}
function unitsFor(name) {
  const n = norm(name);
  for (const [re, u] of UNIT_RULES) if (re.test(n)) return u;
  return [];
}
// Montado no primeiro uso (as funções do app.js ainda não existem quando este arquivo carrega)
let FOOD_MAP = null;
function foodMap() {
  if (FOOD_MAP) return FOOD_MAP;
  FOOD_MAP = new Map();
  if (typeof TACO_FOODS !== 'undefined') {
    for (const [id, n, gr, k, p, c, f, fi] of TACO_FOODS) FOOD_MAP.set('t' + id, { id: 't' + id, n, gr, k, p, c, f, fi, u: unitsFor(n), src: 'TACO' });
    for (const [id, n, gr, k, p, c, f, fi, u] of EXTRA_FOODS) FOOD_MAP.set('x' + id, { id: 'x' + id, n, gr, k, p, c, f, fi, u: [u], src: 'média de rótulos' });
  }
  return FOOD_MAP;
}
function dietData() {
  if (!S.food) S.food = { days: {}, custom: [], fav: [], recent: [], goal: null, tdee: null };
  return S.food;
}
function getFood(id) { return foodMap().get(id) || dietData().custom.find(f => f.id === id) || null; }
function allFoods() { return [...dietData().custom, ...foodMap().values()]; }
const normCache = new Map();
function normName(f) {
  let v = normCache.get(f.id);
  if (!v || v[0] !== f.n) { v = [f.n, norm(f.n)]; normCache.set(f.id, v); }
  return v[1];
}
function foodSearch(q) {
  const toks = norm(q).split(/[\s,]+/).filter(Boolean);
  if (!toks.length) return [];
  const D = dietData(), out = [];
  for (const f of allFoods()) {
    const nn = normName(f);
    let score = 0, ok = true;
    for (const t of toks) {
      const i = nn.indexOf(t);
      if (i < 0) { ok = false; break; }
      score += i === 0 ? 3 : /[\s,(/-]/.test(nn[i - 1]) ? 2 : 0.5;
    }
    if (!ok) continue;
    score -= nn.length / 40;
    if (/\bcru[as]?\b/.test(nn)) score -= 0.8;
    if (D.fav.includes(f.id)) score += 2;
    if (D.recent.includes(f.id)) score += 1;
    if (f.id[0] === 'c') score += 0.5;
    out.push([score, f]);
  }
  return out.sort((a, b) => b[0] - a[0]).slice(0, 60).map(x => x[1]);
}
// Macros de uma quantidade em gramas
function macrosOf(f, g) {
  const r = g / 100;
  return { k: Math.round(f.k * r), p: Math.round(f.p * r * 10) / 10, c: Math.round(f.c * r * 10) / 10, f: Math.round(f.f * r * 10) / 10, fi: Math.round((f.fi || 0) * r * 10) / 10 };
}

/* ================= Dias e registros ================= */
const dayKey = t => isoDate(t);
let dietOffset = 0; // 0 = hoje, -1 = ontem…
function dietT() { return addDays(startOfDay(Date.now()), dietOffset); }
function dayOf(key, create) {
  const D = dietData();
  if (!D.days[key] && create) D.days[key] = { e: [], w: 0 };
  return D.days[key] || { e: [], w: 0 };
}
function dayTotals(key) {
  const e = dayOf(key).e;
  return { k: sum(e.map(x => x.k)), p: sum(e.map(x => x.p)), c: sum(e.map(x => x.c)), f: sum(e.map(x => x.f)), fi: sum(e.map(x => x.fi || 0)) };
}
function mealNow() {
  const h = new Date().getHours() + new Date().getMinutes() / 60;
  return h < 10.5 ? 0 : h < 15 ? 1 : h < 18.5 ? 2 : 3;
}
function pushRecent(id) {
  const D = dietData();
  D.recent = [id, ...D.recent.filter(x => x !== id)].slice(0, 30);
}
function addEntry(key, entry) {
  dayOf(key, true).e.push({ id: uid(), ...entry });
  if (entry.ref) pushRecent(entry.ref);
}

/* ================= Metas e gasto ================= */
const ACTIVITY = [[1.2, 'Sedentário', 'pouco movimento no dia'], [1.375, 'Leve', 'treina 1 a 3 vezes por semana'], [1.55, 'Moderado', 'treina 3 a 5 vezes'], [1.725, 'Alto', 'treina 6 ou 7 vezes'], [1.9, 'Muito alto', 'treino pesado + trabalho físico']];
const GOALS = [['perder', 'Perder gordura'], ['manter', 'Manter'], ['ganhar', 'Ganhar massa']];
const RATES = { perder: [0.25, 0.5, 0.75, 1], ganhar: [0.1, 0.25, 0.5] }; // % do peso por semana
const PROT = [1.6, 1.8, 2, 2.2];
function lastBody(field) {
  const e = (S.body || []).filter(x => x[field] != null).sort((a, b) => b.t - a.t)[0];
  return e ? e[field] : null;
}
function dietWeight() { return lastBody('peso') || num((S.profile || {}).peso) || null; }
function dietGoal() {
  const g = dietData().goal || {};
  return { obj: g.obj || 'manter', rate: g.rate ?? 0.5, act: g.act || 1.55, prot: g.prot || 2, manual: g.manual || null };
}
// Gasto pela fórmula: Katch-McArdle (com % de gordura) ou Mifflin-St Jeor
function formulaTDEE() {
  const w = dietWeight(), pr = S.profile || {}, h = typeof bodyHeight === 'function' ? bodyHeight() : null;
  const age = num(pr.idade), sx = pr.sexo, bf = lastBody('gordura');
  if (!w) return null;
  let bmr;
  if (bf) bmr = 370 + 21.6 * w * (1 - bf / 100);
  else if (h && age) bmr = 10 * w + 6.25 * h - 5 * age + (sx === 'm' ? 5 : sx === 'f' ? -161 : -78);
  else bmr = 22 * w;
  const tmb = lastBody('tmb');
  if (tmb && !bf) bmr = tmb; // TMB medida pela balança
  return Math.round(bmr * dietGoal().act);
}
// Tendência do peso: inclinação (regressão linear) das pesagens do período, em kg por dia.
// Funciona bem com pesagens espaçadas e ignora o sobe e desce do dia a dia.
function weightSlope(days = 28) {
  const from = addDays(startOfDay(Date.now()), -days);
  const pts = (S.body || []).filter(x => x.peso && x.t >= from).map(x => [x.t / 864e5, x.peso]);
  if (pts.length < 3) return null;
  const span = pts.reduce((a, p) => Math.max(a, p[0]), -Infinity) - pts.reduce((a, p) => Math.min(a, p[0]), Infinity);
  if (span < 10) return null;
  const mx = sum(pts.map(p => p[0])) / pts.length, my = sum(pts.map(p => p[1])) / pts.length;
  const num_ = sum(pts.map(([x, y]) => (x - mx) * (y - my))), den = sum(pts.map(([x]) => (x - mx) ** 2));
  return den ? { perDay: num_ / den, n: pts.length, span } : null;
}
// Gasto adaptativo (como no MacroFactor): o que você comeu menos a energia da variação do peso,
// nas últimas 4 semanas. 1 kg de peso corporal ≈ 7.700 kcal. Mistura com a fórmula enquanto há poucos dados.
function adaptiveTDEE() {
  const prior = formulaTDEE();
  const end = startOfDay(Date.now()), start = addDays(end, -28);
  const logged = [];
  for (let t = start; t < end; t = addDays(t, 1)) {
    const tot = dayTotals(dayKey(t));
    if (tot.k >= 600) logged.push(tot.k);
  }
  const sl = weightSlope(28), weighIns = sl ? sl.n : (S.body || []).filter(x => x.peso && x.t >= start).length;
  if (logged.length < 10 || !sl) return { v: prior, src: 'formula', logged: logged.length, weighIns };
  const obs = sum(logged) / logged.length - sl.perDay * 7700;
  const w = Math.min(1, logged.length / 21) * Math.min(1, sl.n / 6);
  const v = Math.round(Math.max(1200, Math.min(5000, prior ? prior * (1 - w) + obs * w : obs)));
  return { v, src: 'dados', logged: logged.length, weighIns, perWeek: sl.perDay * 7 };
}
function currentTDEE() {
  const D = dietData();
  return D.tdee && D.tdee.v ? D.tdee.v : formulaTDEE();
}
// Uma vez por semana o gasto é recalculado com os seus dados
function dietCheckIn() {
  const D = dietData();
  if (!D.goal || (D.tdee && Date.now() - D.tdee.at < 7 * 864e5)) return;
  const a = adaptiveTDEE();
  if (!a.v) return;
  const old = D.tdee && D.tdee.v;
  D.tdee = { v: a.v, at: Date.now(), src: a.src };
  if (old && Math.abs(old - a.v) >= 50) D.tdeeNote = { from: old, to: a.v, at: Date.now() };
  save();
}
function dietTargets(tdeeIn) {
  if (!dietData().goal) return null;
  const g = dietGoal();
  if (g.manual) return g.manual;
  const tdee = tdeeIn || currentTDEE(), w = dietWeight();
  if (!tdee || !w) return null;
  const sx = (S.profile || {}).sexo;
  let k = tdee;
  if (g.obj === 'perder') k = Math.max(sx === 'm' ? 1500 : 1200, tdee - w * g.rate / 100 * 7700 / 7);
  if (g.obj === 'ganhar') k = tdee + w * g.rate / 100 * 7700 / 7;
  k = Math.round(k / 10) * 10;
  const p = Math.round(w * g.prot);
  const f = Math.round(Math.max(0.6 * w, k * 0.25 / 9));
  const c = Math.max(0, Math.round((k - p * 4 - f * 9) / 4));
  return { k, p, c, f };
}

/* ================= Telas ================= */
function ringSVG(frac, over) {
  const C = 2 * Math.PI * 44, f = Math.max(0, Math.min(1, frac));
  return `<svg class="kring" viewBox="0 0 100 100" aria-hidden="true"><circle class="bg" cx="50" cy="50" r="44"/>
    <circle class="fg ${over ? 'over' : ''}" cx="50" cy="50" r="44" stroke-dasharray="${C}" stroke-dashoffset="${C * (1 - f)}" style="--c:${C}"/></svg>`;
}
function macroBar(label, v, t, cls) {
  const pct = t ? Math.min(100, v / t * 100) : 0;
  return `<div class="mbar ${cls}"><div class="mtop"><span>${label}</span><b class="num">${fmt(v, 0)}${t ? `<small> / ${fmt(t, 0)} g</small>` : ' g'}</b></div>
    <div class="mtrack"><i style="width:${pct}%"></i></div></div>`;
}
function dayLabel(t) {
  const diff = Math.round((startOfDay(Date.now()) - t) / 864e5);
  return diff === 0 ? 'Hoje' : diff === 1 ? 'Ontem' : diff === -1 ? 'Amanhã' : dateLong(t);
}
function viewDieta() {
  dietCheckIn();
  const D = dietData(), t = dietT(), key = dayKey(t), day = dayOf(key), tot = dayTotals(key), tg = dietTargets();
  let html = `<div class="top"><div class="spacer"></div><button class="link-btn" data-act="dGoal">Metas</button></div>
    <div class="diet-date"><button class="icon-btn" data-act="dDay" data-d="-1" aria-label="Dia anterior">${I.chev.replace('<svg', '<svg style="transform:rotate(180deg)"')}</button>
      <div class="grow"><div class="eyebrow" style="margin:0">${dateLong(t)}</div><h1 class="title" style="margin:0">${dayLabel(t)}</h1></div>
      <button class="icon-btn" data-act="dDay" data-d="1" aria-label="Próximo dia" ${dietOffset >= 0 ? 'disabled style="opacity:.3"' : ''}>${I.chev}</button></div>`;

  if (!D.goal) {
    html += `<div class="card accent diet-setup"><div class="hero-title">Defina suas metas</div>
      <p class="small muted" style="margin:0 0 14px;line-height:1.45">Com seu peso, altura, idade e objetivo o app calcula as calorias e os macros do dia. Depois ele ajusta sozinho, pela sua evolução.</p>
      <button class="btn block" data-act="dGoal">Calcular minhas metas</button></div>`;
  }
  if (D.tdeeNote && Date.now() - D.tdeeNote.at < 7 * 864e5) {
    const up = D.tdeeNote.to > D.tdeeNote.from;
    html += `<div class="card diet-note"><b>Gasto atualizado: ${fmtInt(D.tdeeNote.from)} → ${fmtInt(D.tdeeNote.to)} kcal</b>
      <div class="small muted">Pelos seus registros e pela tendência do peso, você gasta ${up ? 'mais' : 'menos'} do que o estimado. As metas foram ajustadas.</div>
      <button class="link-btn" style="padding:6px 0 0" data-act="dNoteOk">Entendi</button></div>`;
  }

  // Resumo do dia
  const kT = tg ? tg.k : 0, left = kT - tot.k;
  html += `<div class="card diet-sum">
    <div class="ksum">${ringSVG(kT ? tot.k / kT : 0, kT && tot.k > kT * 1.05)}
      <div class="kin"><b class="num">${fmtInt(Math.abs(kT ? left : tot.k))}</b><span>${!kT ? 'kcal' : left >= 0 ? 'kcal restantes' : 'kcal acima'}</span></div></div>
    <div class="kside">
      <div class="kv2"><span>Meta</span><b class="num">${kT ? fmtInt(kT) : '—'}</b></div>
      <div class="kv2"><span>Consumido</span><b class="num">${fmtInt(tot.k)}</b></div>
      ${macroBar('Proteína', tot.p, tg && tg.p, 'p')}${macroBar('Carboidrato', tot.c, tg && tg.c, 'c')}${macroBar('Gordura', tot.f, tg && tg.f, 'f')}
    </div></div>`;

  // Ações rápidas
  html += `<div class="diet-actions">
    <button class="btn primary" data-act="dAdd" data-m="${mealNow()}">${I.plus}Adicionar</button>
    <button class="btn" data-act="dAI" data-mode="text">${I.sparkle}Descrever</button>
    <label class="btn" style="cursor:pointer">${I.camera}Foto<input type="file" accept="image/*" capture="environment" data-diet-photo="plate" hidden></label>
  </div>`;

  // Água
  const wGoal = Math.round((dietWeight() || 70) * 35 / 50) * 50, w = day.w || 0;
  html += `<div class="card water"><div class="wtop"><div><b>Água</b><span class="small muted"> · meta ${fmt(wGoal / 1000, 1)} L</span></div><b class="num">${fmt(w / 1000, 2)} L</b></div>
    <div class="wcups">${Array.from({ length: 8 }, (_, i) => `<i class="${w >= (i + 1) * wGoal / 8 ? 'on' : ''}"></i>`).join('')}</div>
    <div class="btn-row"><button class="btn sm" data-act="dWater" data-v="-250" aria-label="Tirar 250 ml">−</button><button class="btn sm" data-act="dWater" data-v="250">+250 ml</button><button class="btn sm" data-act="dWater" data-v="500">+500 ml</button></div></div>`;

  // Refeições
  const yKey = dayKey(addDays(t, -1));
  MEALS.forEach((m, mi) => {
    const items = day.e.filter(e => e.m === mi), mk = sum(items.map(e => e.k));
    const yItems = dayOf(yKey).e.filter(e => e.m === mi);
    html += `<div class="meal"><div class="meal-head"><b>${m}</b><span class="num muted">${items.length ? fmtInt(mk) + ' kcal' : ''}</span>
      <button class="icon-btn sm" data-act="dAdd" data-m="${mi}" aria-label="Adicionar em ${m}">${I.plus}</button></div>
      ${items.length ? `<div class="list">${items.map(e => `<button class="row" data-act="dEntry" data-id="${e.id}">
        <div class="grow"><div class="name">${esc(e.n)}</div><div class="sub">${e.q && e.u ? `${fmt(e.q, 2)} ${esc(unitLabel(e.q, e.u))} · ` : ''}${fmt(e.gr, 0)} g · P ${fmt(e.p, 0)} · C ${fmt(e.c, 0)} · G ${fmt(e.f, 0)}</div></div>
        <b class="num kc">${fmtInt(e.k)}</b></button>`).join('')}</div>`
      : yItems.length ? `<button class="link-btn meal-copy" data-act="dCopyMeal" data-m="${mi}">Repetir de ontem (${yItems.length} ${yItems.length > 1 ? 'itens' : 'item'} · ${fmtInt(sum(yItems.map(e => e.k)))} kcal)</button>` : ''}
    </div>`;
  });

  // Semana
  html += dietWeekHTML(tg);
  html += `<p class="small muted" style="margin:16px 4px 0;line-height:1.5">Alimentos da Tabela Brasileira de Composição de Alimentos (TACO, NEPA/UNICAMP). Os valores são estimativas — para orientação individual, procure um nutricionista.</p>`;
  return html;
}
function dietWeekHTML(tg) {
  const end = startOfDay(Date.now()), cols = [];
  for (let i = 6; i >= 0; i--) {
    const t = addDays(end, -i), tot = dayTotals(dayKey(t));
    cols.push({ t, k: tot.k, p: tot.p });
  }
  const top = Math.max(tg ? tg.k * 1.25 : 0, ...cols.map(c => c.k), 1);
  const logged = cols.filter(c => c.k > 0);
  const avg = logged.length ? sum(logged.map(c => c.k)) / logged.length : 0;
  const a = adaptiveTDEE(), sl = weightSlope(28), wk = sl ? sl.perDay * 7 : null;
  return `<h2 class="section">Últimos 7 dias</h2><div class="card week-k">
    <div class="wbars">${cols.map(c => `<div class="wb"><div class="wbt"><i style="height:${c.k / top * 100}%" class="${tg && c.k > tg.k * 1.05 ? 'over' : ''}"></i>${tg ? `<b style="bottom:${tg.k / top * 100}%"></b>` : ''}</div><small>${DAY[new Date(c.t).getDay()]}</small></div>`).join('')}</div>
    <div class="kvs">
      <div class="kv2"><span>Média registrada</span><b class="num">${avg ? fmtInt(avg) + ' kcal' : '—'}</b></div>
      <div class="kv2"><span>Gasto estimado</span><b class="num">${a.v ? fmtInt(currentTDEE() || a.v) + ' kcal' : '—'}</b></div>
      <div class="kv2"><span>Tendência do peso</span><b class="num">${wk != null ? `${wk > 0 ? '+' : ''}${fmt(wk, 2)} kg/sem` : '—'}</b></div>
    </div>
    <p class="small muted" style="margin:8px 0 0;line-height:1.45">${a.src === 'dados'
      ? `O gasto é calculado com ${a.logged} dias registrados e ${a.weighIns} pesagens das últimas 4 semanas, e atualizado toda semana.`
      : `Por enquanto o gasto vem da fórmula. Registre o que come e se pese (aba Corpo) algumas vezes por semana: com 10 dias de registro e 3 pesagens em pelo menos 10 dias, o app passa a calcular o seu gasto real.`}</p></div>`;
}
// Card na tela Hoje
function dietTodayCard() {
  const D = dietData();
  if (!D.goal && !Object.keys(D.days).length) return '';
  const key = dayKey(Date.now()), tot = dayTotals(key), tg = dietTargets();
  return `<a class="card diet-today" href="#/dieta">
    <div class="dt-top"><b>Dieta de hoje</b><span class="num">${fmtInt(tot.k)}${tg ? ` / ${fmtInt(tg.k)}` : ''} kcal</span></div>
    <div class="mtrack"><i style="width:${tg ? Math.min(100, tot.k / tg.k * 100) : 0}%"></i></div>
    <div class="small muted">Proteína ${fmt(tot.p, 0)}${tg ? ` / ${tg.p}` : ''} g · Água ${fmt((dayOf(key).w || 0) / 1000, 1)} L</div></a>`;
}

/* ---------- Adicionar alimento ---------- */
let dPick = { m: 0, q: '', tab: 'recent' };
function dietAddSheet(m) {
  dPick = { m, q: '', tab: dietData().recent.length ? 'recent' : 'fav' };
  openSheet(`${sheetHead('Adicionar em ' + MEALS[m])}
    <div class="search">${I.search}<input class="input" id="foodq" type="search" placeholder="Buscar alimento (ex.: arroz cozido)" autocomplete="off"></div>
    <div class="diet-tools">
      <button class="chip" data-act="dAI" data-mode="text">${I.sparkle}Descrever com IA</button>
      <label class="chip" style="cursor:pointer">${I.camera}Foto do prato<input type="file" accept="image/*" capture="environment" data-diet-photo="plate" hidden></label>
      <button class="chip" data-act="dBarcode">${I.barcode}Código de barras</button>
      <label class="chip" style="cursor:pointer">${I.camera}Foto do rótulo<input type="file" accept="image/*" capture="environment" data-diet-photo="label" hidden></label>
      <button class="chip" data-act="dCustom">${I.plus}Criar alimento</button>
    </div>
    <div class="chips" id="foodtabs">${foodTabs()}</div>
    <div class="sheet-body" id="foodlist">${foodList()}</div>`);
}
function foodTabs() {
  return [['recent', 'Recentes'], ['fav', 'Favoritos'], ['mine', 'Meus alimentos']].map(([v, l]) => `<button class="chip ${dPick.tab === v && !dPick.q ? 'on' : ''}" data-act="dTab" data-v="${v}">${l}</button>`).join('');
}
function foodRow(f) {
  const u = f.u && f.u[0];
  return `<button class="pick" data-act="dFood" data-id="${f.id}"><div class="grow"><div class="name">${esc(f.n)}</div>
    <div class="sub">${fmtInt(f.k)} kcal · P ${fmt(f.p, 1)} · C ${fmt(f.c, 1)} · G ${fmt(f.f, 1)} <span class="muted">/ 100 g${u ? ` · ${esc(u[0])} ${fmt(u[1], 0)} g` : ''}</span></div></div>
    ${dietData().fav.includes(f.id) ? '<span class="fav-dot" aria-label="Favorito">★</span>' : ''}</button>`;
}
function foodList() {
  const D = dietData();
  let list;
  if (dPick.q) list = foodSearch(dPick.q);
  else if (dPick.tab === 'fav') list = D.fav.map(getFood).filter(Boolean);
  else if (dPick.tab === 'mine') list = D.custom.slice().reverse();
  else list = D.recent.map(getFood).filter(Boolean);
  if (!list.length) {
    const msg = dPick.q ? `Nada encontrado para “${esc(dPick.q)}”. Tente outra palavra, descreva com IA ou crie o alimento.`
      : dPick.tab === 'fav' ? 'Toque na estrela de um alimento para guardá-lo aqui.'
        : dPick.tab === 'mine' ? 'Alimentos que você criar, ler pelo código de barras ou pela foto do rótulo aparecem aqui.'
          : 'Busque um alimento acima. Os que você usar aparecem aqui.';
    return `<p class="muted small" style="text-align:center;padding:22px 10px;line-height:1.5">${msg}</p>`;
  }
  return list.map(foodRow).join('');
}

/* ---------- Porção ---------- */
let dPortion = null; // { food, m, qty, unit (índice; -1 = gramas), entryId, key }
function portionSheet(food, m, entry) {
  const units = food.u || [];
  dPortion = entry
    ? { food, m: entry.m, qty: entry.q ?? entry.gr, unit: entry.u ? Math.max(-1, units.findIndex(u => u[0] === entry.u)) : -1, entryId: entry.id, key: dayKey(dietT()) }
    : { food, m, qty: units.length ? 1 : 100, unit: units.length ? 0 : -1, key: dayKey(dietT()) };
  if (dPortion.unit < 0 && entry) dPortion.qty = entry.gr;
  renderPortion();
}
function portionGrams() {
  const P = dPortion, u = P.unit >= 0 ? P.food.u[P.unit] : null;
  return (num(P.qty) || 0) * (u ? u[1] : 1);
}
function portionPreview() {
  const mm = macrosOf(dPortion.food, portionGrams());
  return `<div class="pprev"><div><b class="num">${fmtInt(mm.k)}</b><span>kcal</span></div><div><b class="num">${fmt(mm.p, 1)}</b><span>proteína</span></div>
    <div><b class="num">${fmt(mm.c, 1)}</b><span>carbo</span></div><div><b class="num">${fmt(mm.f, 1)}</b><span>gordura</span></div></div>
    <p class="small muted" style="text-align:center;margin:6px 0 0">${fmt(portionGrams(), 0)} g</p>`;
}
function renderPortion() {
  const P = dPortion, f = P.food, fav = dietData().fav.includes(f.id);
  const units = [...(f.u || []).map((u, i) => [i, `${u[0]} (${fmt(u[1], 0)} g)`]), [-1, 'gramas']];
  openSheet(`${sheetHead(f.n, `<button class="icon-btn" data-act="dFav" aria-label="${fav ? 'Tirar dos favoritos' : 'Favoritar'}" style="color:${fav ? 'var(--gold)' : 'var(--muted)'};font-size:22px">${fav ? '★' : '☆'}</button>`)}
    <div class="sheet-body">
      <p class="small muted" style="margin:-4px 0 12px">${fmtInt(f.k)} kcal · P ${fmt(f.p, 1)} · C ${fmt(f.c, 1)} · G ${fmt(f.f, 1)} por 100 g${f.src ? ` · ${esc(f.src)}` : ''}</p>
      <div class="calc-in"><button class="btn sm" data-act="dQty" data-d="-1" aria-label="Menos">−</button>
        <input class="input num" inputmode="decimal" data-dq value="${fmtIn(num(P.qty) || 0)}"><button class="btn sm" data-act="dQty" data-d="1" aria-label="Mais">+</button></div>
      <div class="chips wrap" style="margin:12px 0 0">${units.map(([i, l]) => `<button class="chip ${P.unit === i ? 'on' : ''}" data-act="dUnit" data-v="${i}">${esc(l)}</button>`).join('')}</div>
      <div id="dprev">${portionPreview()}</div>
      <div class="seg seg-sm" style="margin-top:14px">${MEALS.map((m, i) => `<button class="${P.m === i ? 'on' : ''}" data-act="dMeal" data-v="${i}">${m.replace('Café da manhã', 'Café')}</button>`).join('')}</div>
    </div>
    <div class="sheet-foot stack"><button class="btn primary block" data-act="dSave">${P.entryId ? 'Salvar' : 'Adicionar'}</button>
      ${P.entryId ? `<button class="btn danger block" data-act="dDelete">${I.trash}Remover</button>` : ''}</div>`);
}

/* ---------- Criar alimento ---------- */
function customSheet(pre = {}) {
  const v = k => pre[k] != null ? fmtIn(pre[k]) : '';
  openSheet(`${sheetHead(pre.id ? 'Editar alimento' : 'Criar alimento')}<div class="sheet-body">
    <label class="field"><span>Nome</span><input class="input" id="cfName" value="${esc(pre.n || '')}" placeholder="Ex.: Pão de queijo da padaria"></label>
    <div class="look-label">Valores por 100 g</div>
    <div class="mini-grid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-top:0">
      <label><span>kcal</span><input class="input num" inputmode="decimal" id="cfK" value="${v('k')}"></label>
      <label><span>Proteína</span><input class="input num" inputmode="decimal" id="cfP" value="${v('p')}"></label>
      <label><span>Carbo</span><input class="input num" inputmode="decimal" id="cfC" value="${v('c')}"></label>
      <label><span>Gordura</span><input class="input num" inputmode="decimal" id="cfF" value="${v('f')}"></label>
      <label><span>Fibra</span><input class="input num" inputmode="decimal" id="cfFi" value="${v('fi')}"></label>
      <label><span>Porção (g)</span><input class="input num" inputmode="decimal" id="cfU" value="${pre.u && pre.u[0] ? fmtIn(pre.u[0][1]) : ''}" placeholder="opcional"></label>
    </div>
    <p class="small muted" style="margin:10px 2px 0;line-height:1.45">Se o rótulo traz os valores por porção, divida pela porção em gramas e multiplique por 100. Ou use “Foto do rótulo” para a IA fazer isso.</p>
  </div><div class="sheet-foot"><button class="btn primary block" data-act="dCustomSave" data-id="${pre.id || ''}" data-code="${esc(pre.code || '')}">Salvar e escolher a porção</button></div>`);
}

/* ---------- Metas ---------- */
let dGoalDraft = null;
function goalSheet() {
  const g = dietGoal(), pr = S.profile || {};
  dGoalDraft = dGoalDraft || { ...g, manual: g.manual ? { ...g.manual } : null };
  const d = dGoalDraft, w = dietWeight(), h = typeof bodyHeight === 'function' ? bodyHeight() : null;
  const need = [];
  if (!w) need.push(['peso', 'Peso (kg)']);
  if (!h) need.push(['altura', 'Altura (cm)']);
  if (!num(pr.idade)) need.push(['idade', 'Idade']);
  // Prévia com o rascunho: gasto pelos dados (se já houver) ou pela fórmula com a atividade escolhida
  const D = dietData(), saved = D.goal;
  D.goal = { ...d };
  const tdee = D.tdee && D.tdee.src === 'dados' ? D.tdee.v : formulaTDEE(), tg = dietTargets(tdee);
  D.goal = saved;
  openSheet(`${sheetHead('Metas da dieta')}<div class="sheet-body">
    ${need.length ? `<div class="mini-grid" style="grid-template-columns:repeat(${need.length},minmax(0,1fr));margin:0 0 12px">${need.map(([k, l]) => `<label><span>${l}</span><input class="input num" inputmode="decimal" data-dg-prof="${k}"></label>`).join('')}</div>` : ''}
    ${!pr.sexo ? `<div class="look-label">Sexo</div><div class="seg seg-sm" style="grid-template-columns:1fr 1fr">${[['f', 'Feminino'], ['m', 'Masculino']].map(([v, l]) => `<button data-act="dSex" data-v="${v}">${l}</button>`).join('')}</div>` : ''}
    <div class="look-label">Objetivo</div>
    <div class="seg seg-sm" style="grid-template-columns:repeat(3,1fr)">${GOALS.map(([v, l]) => `<button class="${d.obj === v ? 'on' : ''}" data-act="dGoalSet" data-k="obj" data-v="${v}">${l}</button>`).join('')}</div>
    ${RATES[d.obj] ? `<div class="look-label">Ritmo (do peso por semana)</div>
      <div class="chips wrap" style="margin:0 0 16px">${RATES[d.obj].map(r => `<button class="chip ${d.rate === r ? 'on' : ''}" data-act="dGoalSet" data-k="rate" data-v="${r}">${fmt(r, 2)}%${w ? ` · ${fmt(w * r / 100, 2)} kg` : ''}</button>`).join('')}</div>` : ''}
    <div class="look-label">Nível de atividade</div>
    <div class="list" style="margin-bottom:16px">${ACTIVITY.map(([v, l, s]) => `<button class="row" data-act="dGoalSet" data-k="act" data-v="${v}"><div class="grow"><div class="name">${l}</div><div class="sub">${s}</div></div>${d.act === v ? I.check.replace('<svg', '<svg class="chev" style="stroke:var(--accent-text)"') : ''}</button>`).join('')}</div>
    <div class="look-label">Proteína por kg de peso</div>
    <div class="chips wrap" style="margin:0 0 16px">${PROT.map(v => `<button class="chip ${d.prot === v ? 'on' : ''}" data-act="dGoalSet" data-k="prot" data-v="${v}">${fmt(v, 1)} g/kg</button>`).join('')}</div>
    <div class="card goal-res">${tg ? `<div class="kv2"><span>Gasto estimado</span><b class="num">${fmtInt(tdee)} kcal</b></div>
      <div class="kv2"><span>Meta de calorias</span><b class="num">${fmtInt(tg.k)} kcal</b></div>
      <div class="kv2"><span>Proteína · Carbo · Gordura</span><b class="num">${tg.p} · ${tg.c} · ${tg.f} g</b></div>` : '<p class="muted small" style="margin:0">Informe peso, altura e idade para calcular.</p>'}</div>
    <p class="small muted" style="margin:10px 2px 0;line-height:1.45">O gasto parte da fórmula de Mifflin-St Jeor (ou Katch-McArdle, se você tiver o % de gordura na aba Corpo) e depois é corrigido toda semana com o que você registra e com a tendência do seu peso.</p>
  </div><div class="sheet-foot"><button class="btn primary block" data-act="dGoalSave">Salvar metas</button></div>`);
}

/* ---------- Código de barras (Open Food Facts) ---------- */
let camStream = null, camTimer = null;
function stopCam() {
  clearInterval(camTimer); camTimer = null;
  if (camStream) { camStream.getTracks().forEach(t => t.stop()); camStream = null; }
}
function barcodeSheet() {
  const scan = 'BarcodeDetector' in window && navigator.mediaDevices && navigator.mediaDevices.getUserMedia;
  openSheet(`${sheetHead('Código de barras')}<div class="sheet-body">
    ${scan ? `<div class="cam"><video id="bcVideo" playsinline muted></video><i class="cam-line"></i></div>` : ''}
    <label class="field" style="margin-top:${scan ? 12 : 0}px"><span>${scan ? 'Ou digite o número' : 'Número do código (embaixo das barras)'}</span>
      <input class="input num" id="bcCode" inputmode="numeric" placeholder="7891234567890" autocomplete="off"></label>
    <p class="small muted" style="margin:0 2px;line-height:1.45">${scan ? '' : 'Neste navegador a câmera não lê códigos de barras; digite os números ou use “Foto do rótulo”. '}Os dados vêm do Open Food Facts, uma base aberta e colaborativa de produtos.</p>
    <div id="bcOut"></div>
  </div><div class="sheet-foot"><button class="btn primary block" data-act="dBarcodeGo">Buscar produto</button></div>`, null, stopCam);
  if (scan) startScan();
}
async function startScan() {
  try {
    const det = new BarcodeDetector({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e'] });
    camStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
    const v = $('#bcVideo');
    if (!v) { stopCam(); return; }
    v.srcObject = camStream; await v.play();
    camTimer = setInterval(async () => {
      try {
        const codes = await det.detect(v);
        if (codes.length) { const c = codes[0].rawValue; stopCam(); $('#bcCode').value = c; barcodeLookup(c); }
      } catch (e) { /* quadro ainda não pronto */ }
    }, 350);
  } catch (e) {
    const v = $('#bcVideo'); if (v) v.closest('.cam').remove();
    toast('Sem acesso à câmera — digite o número');
  }
}
async function offProduct(code) {
  const r = await fetch(`https://world.openfoodfacts.org/api/v2/product/${encodeURIComponent(code)}.json?fields=product_name,product_name_pt,brands,nutriments,serving_quantity,serving_size`);
  if (r.status === 404) return null;
  if (!r.ok) throw new Error('http ' + r.status);
  const d = await r.json();
  if (d.status !== 1 || !d.product) return null;
  const p = d.product, nm = p.nutriments || {};
  const name = [p.product_name_pt || p.product_name, p.brands && p.brands.split(',')[0]].filter(Boolean).join(' · ') || `Produto ${code}`;
  const kcal = nm['energy-kcal_100g'] ?? (nm.energy_100g != null ? nm.energy_100g / 4.184 : null);
  const g = k => Math.round((num(nm[k]) || 0) * 10) / 10;
  return { n: name, code, missing: kcal == null, k: Math.round(kcal || 0), p: g('proteins_100g'), c: g('carbohydrates_100g'), f: g('fat_100g'), fi: g('fiber_100g'),
    u: num(p.serving_quantity) ? [['porção', num(p.serving_quantity)]] : [] };
}
async function barcodeLookup(code) {
  code = String(code || '').replace(/\D/g, '');
  const out = $('#bcOut');
  if (code.length < 8) { toast('Digite os 8 a 14 números do código'); return; }
  const known = dietData().custom.find(f => f.code === code);
  if (known) { stopCam(); closeSheet(); portionSheet(known, dPick.m); return; }
  if (out) out.innerHTML = '<p class="muted small" style="text-align:center;margin-top:14px">Buscando no Open Food Facts…</p>';
  try {
    const prod = await offProduct(code);
    if (!prod) { if (out) out.innerHTML = '<p class="small" style="margin-top:14px;line-height:1.45">Produto não encontrado na base. Use “Foto do rótulo” ou “Criar alimento”.</p>'; return; }
    if (prod.missing) { stopCam(); customSheet({ ...prod, k: null, p: null, c: null, f: null }); toast('Produto sem tabela nutricional na base — preencha pelo rótulo'); return; }
    const f = { id: 'c' + uid(), ...prod, src: 'Open Food Facts' };
    delete f.missing;
    dietData().custom.push(f); save();
    stopCam(); portionSheet(f, dPick.m);
  } catch (e) {
    if (out) out.innerHTML = '<p class="small" style="margin-top:14px">Não foi possível consultar agora. Confira a internet e tente de novo.</p>';
  }
}

/* ---------- IA (Claude): descrever refeição, foto do prato, foto do rótulo ---------- */
const MEAL_AI_SCHEMA = {
  type: 'object', additionalProperties: false, required: ['itens', 'observacao'],
  properties: {
    observacao: { type: 'string' },
    itens: {
      type: 'array', items: {
        type: 'object', additionalProperties: false, required: ['nome', 'gramas', 'kcal', 'proteina', 'carboidrato', 'gordura'],
        properties: { nome: { type: 'string' }, gramas: { type: 'number' }, kcal: { type: 'number' }, proteina: { type: 'number' }, carboidrato: { type: 'number' }, gordura: { type: 'number' } }
      }
    }
  }
};
const LABEL_AI_SCHEMA = {
  type: 'object', additionalProperties: false,
  required: ['legivel', 'nome', 'porcao_g', 'kcal_100g', 'proteina_100g', 'carboidrato_100g', 'gordura_100g', 'fibra_100g', 'observacao'],
  properties: {
    legivel: { type: 'boolean' }, nome: { type: 'string' }, porcao_g: { type: 'number' },
    kcal_100g: { type: 'number' }, proteina_100g: { type: 'number' }, carboidrato_100g: { type: 'number' }, gordura_100g: { type: 'number' }, fibra_100g: { type: 'number' },
    observacao: { type: 'string' }
  }
};
const MEAL_AI_SYSTEM = `Você ajuda a registrar refeições num app brasileiro de treino e dieta.
Identifique cada alimento, estime a quantidade em gramas e calcule calorias, proteína, carboidrato e gordura DAQUELA quantidade (não por 100 g).
Use como referência a Tabela Brasileira de Composição de Alimentos (TACO) e porções caseiras brasileiras (ex.: colher de sopa de arroz cozido ≈ 25 g, concha de feijão ≈ 100 g, pão francês ≈ 50 g, ovo ≈ 50 g).
Quando a quantidade for informada, use-a. Quando não for, use uma porção típica de um adulto. Inclua óleo, molhos e bebidas quando aparecerem ou forem mencionados.
Nomes curtos em português (ex.: "Arroz branco cozido"). Em "observacao", uma frase curta sobre o que foi estimado e o que pode variar.`;
const LABEL_AI_SYSTEM = `Você lê tabelas nutricionais de rótulos de alimentos brasileiros (fotos).
Converta os valores para 100 g (ou 100 ml) usando a porção informada no rótulo. Se a foto não mostrar uma tabela nutricional legível, responda legivel=false e zeros.
"nome": nome do produto se aparecer na foto, senão uma descrição curta. Em "observacao", avise se algo estava ilegível.`;

async function foodAI(kind, payload) {
  const Anthropic = await aiSdk();
  const client = new Anthropic({ apiKey: aiKey(), dangerouslyAllowBrowser: true, maxRetries: 1 });
  const content = [];
  if (payload.image) content.push({ type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: payload.image } });
  content.push({ type: 'text', text: kind === 'label' ? 'Leia a tabela nutricional deste rótulo.' : payload.text ? `Refeição: ${payload.text}` : 'Estime os alimentos e as quantidades deste prato pela foto (use o tamanho do prato e dos talheres como referência).' });
  try {
    const stream = client.beta.messages.stream({
      model: ASST_AI_MODEL,
      max_tokens: 16000,
      // Se o modelo recusar o pedido, a API tenta de novo num modelo alternativo recomendado
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
      output_config: { effort: 'low', format: { type: 'json_schema', schema: kind === 'label' ? LABEL_AI_SCHEMA : MEAL_AI_SCHEMA } },
      system: kind === 'label' ? LABEL_AI_SYSTEM : MEAL_AI_SYSTEM,
      messages: [{ role: 'user', content }]
    });
    const msg = await stream.finalMessage();
    if (msg.stop_reason === 'refusal') throw new AiError('A IA não conseguiu analisar isto. Tente descrever de outro jeito.');
    if (msg.stop_reason === 'max_tokens') throw new AiError('A resposta ficou incompleta. Tente de novo.');
    const text = msg.content.filter(b => b.type === 'text').map(b => b.text).join('');
    try { return JSON.parse(text); } catch (e) { throw new AiError('A resposta da IA veio num formato inesperado. Tente de novo.'); }
  } catch (e) {
    throw aiFriendly(e, Anthropic);
  }
}
// Reduz a foto (até 1280 px, JPEG) antes de enviar: menos tokens e envio mais rápido
function photoToJpeg(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file), img = new Image();
    img.onload = () => {
      const s = Math.min(1, 1280 / Math.max(img.width, img.height));
      const c = document.createElement('canvas');
      c.width = Math.round(img.width * s); c.height = Math.round(img.height * s);
      c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
      URL.revokeObjectURL(url);
      resolve(c.toDataURL('image/jpeg', 0.82).split(',')[1]);
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('imagem')); };
    img.src = url;
  });
}
function aiReady() {
  if (typeof aiKey !== 'function' || typeof foodAI !== 'function') { toast('Atualize o app para usar a IA'); return false; }
  if (!aiKey()) { aiKeySheet(''); toast('Salve a chave da API e tente de novo'); return false; }
  if (!navigator.onLine) { toast('Sem internet — a IA precisa de conexão'); return false; }
  return true;
}
function aiBusy(text) {
  openSheet(`<div class="sheet-body"><div class="ai-run"><div class="ai-orb">${I.sparkle}</div><h3>${text}</h3><p class="muted small">Leva alguns segundos</p></div></div>`);
}
let dAi = null; // { items, m, note }
async function runFoodAI(kind, payload) {
  if (!aiReady()) return;
  const m = dPick.m ?? mealNow();
  aiBusy(kind === 'label' ? 'Lendo o rótulo…' : payload.image ? 'Analisando a foto…' : 'Calculando a refeição…');
  try {
    const data = await foodAI(kind, payload);
    if (kind === 'label') {
      if (!data.legivel) { closeSheet(); toast('Não deu para ler a tabela nutricional. Tente uma foto mais de perto.'); return; }
      customSheet({ n: data.nome, k: data.kcal_100g, p: data.proteina_100g, c: data.carboidrato_100g, f: data.gordura_100g, fi: data.fibra_100g, u: data.porcao_g ? [['porção', data.porcao_g]] : [] });
      if (data.observacao) toast(data.observacao);
      return;
    }
    const items = (data.itens || []).filter(x => x && x.nome && x.gramas > 0).map(x => ({ ...x, on: true, g0: x.gramas }));
    if (!items.length) { closeSheet(); toast('A IA não identificou alimentos. Tente descrever com mais detalhes.'); return; }
    dAi = { items, m, note: data.observacao || '' };
    renderAiReview();
  } catch (e) {
    closeSheet();
    toast(e instanceof AiError ? e.message : 'Não foi possível usar a IA agora');
  }
}
function aiItemMacros(x) {
  const r = (num(x.gramas) || 0) / x.g0;
  return { k: Math.round(x.kcal * r), p: Math.round(x.proteina * r * 10) / 10, c: Math.round(x.carboidrato * r * 10) / 10, f: Math.round(x.gordura * r * 10) / 10 };
}
function aiTotalHTML() {
  const on = dAi.items.filter(x => x.on).map(aiItemMacros);
  return `${on.length} ${on.length === 1 ? 'item' : 'itens'} · <b class="num">${fmtInt(sum(on.map(x => x.k)))} kcal</b> · P ${fmt(sum(on.map(x => x.p)), 0)} · C ${fmt(sum(on.map(x => x.c)), 0)} · G ${fmt(sum(on.map(x => x.f)), 0)}`;
}
function renderAiReview() {
  openSheet(`${sheetHead('Confira a refeição')}<div class="sheet-body">
    ${dAi.note ? `<p class="small muted" style="margin:-4px 0 10px;line-height:1.45">${esc(dAi.note)}</p>` : ''}
    <div class="list">${dAi.items.map((x, i) => {
      const mm = aiItemMacros(x);
      return `<div class="row ai-item"><label class="ai-check"><input type="checkbox" data-ai-on="${i}" ${x.on ? 'checked' : ''}><i>${I.check}</i></label>
        <div class="grow"><div class="name">${esc(x.nome)}</div><div class="sub" data-ai-sub="${i}">${fmtInt(mm.k)} kcal · P ${fmt(mm.p, 0)} · C ${fmt(mm.c, 0)} · G ${fmt(mm.f, 0)}</div></div>
        <label class="ai-g"><input class="input num" inputmode="decimal" data-ai-g="${i}" value="${fmtIn(num(x.gramas))}"><span>g</span></label></div>`;
    }).join('')}</div>
    <p class="small" id="aiTot" style="margin:12px 2px 0">${aiTotalHTML()}</p>
    <div class="seg seg-sm" style="margin-top:12px">${MEALS.map((m, i) => `<button class="${dAi.m === i ? 'on' : ''}" data-act="dAiMeal" data-v="${i}">${m.replace('Café da manhã', 'Café')}</button>`).join('')}</div>
    <p class="small muted" style="margin:10px 2px 0">Estimativa da IA: ajuste as gramas se souber a quantidade.</p>
  </div><div class="sheet-foot"><button class="btn primary block" data-act="dAiAdd">Adicionar à refeição</button></div>`);
}
function aiTextSheet() {
  openSheet(`${sheetHead('Descrever refeição')}<div class="sheet-body">
    <label class="field"><span>O que você comeu?</span><textarea class="input" id="aiMeal" rows="4" placeholder="Ex.: 4 colheres de arroz, 1 concha de feijão, 1 filé de frango grelhado e salada de alface com tomate"></textarea></label>
    <p class="small muted" style="margin:0 2px;line-height:1.45">A IA identifica cada alimento e estima as quantidades e os macros. Você confere antes de salvar. Usa a sua chave da Anthropic.</p>
  </div><div class="sheet-foot"><button class="btn primary block" data-act="dAiRun">${I.sparkle}Calcular</button></div>`);
  setTimeout(() => { const t = $('#aiMeal'); if (t) t.focus(); }, 300);
}

/* ================= Ações ================= */
const DIET_ACTIONS = {
  dDay: el => { dietOffset = Math.min(0, dietOffset + +el.dataset.d); rerender(); },
  dAdd: el => dietAddSheet(+el.dataset.m),
  dTab: el => { dPick.tab = el.dataset.v; dPick.q = ''; const q = $('#foodq'); if (q) q.value = ''; $('#foodtabs').innerHTML = foodTabs(); $('#foodlist').innerHTML = foodList(); },
  dFood: el => { const f = getFood(el.dataset.id); if (f) portionSheet(f, dPick.m); },
  dEntry: el => {
    const key = dayKey(dietT()), e = dayOf(key).e.find(x => x.id === el.dataset.id);
    if (!e) return;
    const f = (e.ref && getFood(e.ref)) || { id: '', n: e.n, k: e.k / e.gr * 100, p: e.p / e.gr * 100, c: e.c / e.gr * 100, f: e.f / e.gr * 100, fi: (e.fi || 0) / e.gr * 100, u: [] };
    portionSheet(f, e.m, e);
  },
  dQty: el => {
    const P = dPortion, step = P.unit >= 0 ? (P.food.u[P.unit][1] >= 50 ? 1 : 0.5) : 10;
    P.qty = Math.max(0, (num(P.qty) || 0) + +el.dataset.d * step);
    $('[data-dq]').value = fmtIn(P.qty); $('#dprev').innerHTML = portionPreview();
  },
  dUnit: el => {
    const P = dPortion, g = portionGrams(), u = +el.dataset.v;
    P.unit = u; P.qty = u >= 0 ? Math.max(0.5, Math.round(g / P.food.u[u][1] * 2) / 2) : Math.round(g);
    renderPortion();
  },
  dMeal: el => { dPortion.m = +el.dataset.v; el.parentNode.querySelectorAll('button').forEach(b => b.classList.toggle('on', b === el)); },
  dFav: () => {
    const D = dietData(), id = dPortion.food.id;
    if (!id) { toast('Salve como alimento para favoritar'); return; }
    D.fav = D.fav.includes(id) ? D.fav.filter(x => x !== id) : [id, ...D.fav];
    save(); renderPortion();
  },
  dSave: () => {
    const P = dPortion, g = portionGrams();
    if (!(g > 0)) { toast('Informe a quantidade'); return; }
    const u = P.unit >= 0 ? P.food.u[P.unit] : null;
    const entry = { m: P.m, n: P.food.n, ref: P.food.id || null, gr: Math.round(g * 10) / 10, q: u ? num(P.qty) : null, u: u ? u[0] : null, ...macrosOf(P.food, g) };
    if (P.entryId) {
      const list = dayOf(P.key, true).e, i = list.findIndex(x => x.id === P.entryId);
      if (i >= 0) list[i] = { ...list[i], ...entry };
    } else addEntry(P.key, entry);
    save(); closeSheet(); rerender();
    if (!P.entryId) toast(`${P.food.n.split(',')[0]} adicionado em ${MEALS[P.m]}`);
  },
  dDelete: () => {
    const d = dayOf(dPortion.key, true);
    d.e = d.e.filter(x => x.id !== dPortion.entryId);
    save(); closeSheet(); rerender();
  },
  dCopyMeal: el => {
    const m = +el.dataset.m, t = dietT(), from = dayOf(dayKey(addDays(t, -1))).e.filter(e => e.m === m);
    for (const { id, ...e } of from) addEntry(dayKey(t), e);
    save(); rerender(); toast(`${MEALS[m]} de ontem repetido`);
  },
  dWater: el => {
    const d = dayOf(dayKey(dietT()), true);
    d.w = Math.max(0, (d.w || 0) + +el.dataset.v); save(); rerender();
  },
  dCustom: () => customSheet({}),
  dCustomSave: el => {
    const n = ($('#cfName').value || '').trim(), k = num($('#cfK').value);
    if (!n) { toast('Dê um nome ao alimento'); return; }
    if (k == null) { toast('Informe as calorias por 100 g'); return; }
    const D = dietData(), ug = num($('#cfU').value);
    const f = { id: el.dataset.id || 'c' + uid(), n, k: Math.round(k), p: num($('#cfP').value) || 0, c: num($('#cfC').value) || 0, f: num($('#cfF').value) || 0, fi: num($('#cfFi').value) || 0, u: ug ? [['porção', ug]] : [], src: 'meu alimento' };
    if (el.dataset.code) f.code = el.dataset.code;
    const i = D.custom.findIndex(x => x.id === f.id);
    if (i >= 0) D.custom[i] = f; else D.custom.push(f);
    save(); portionSheet(f, dPick.m ?? mealNow());
  },
  dGoal: () => { dGoalDraft = null; goalSheet(); },
  dGoalSet: el => {
    const k = el.dataset.k, v = k === 'obj' ? el.dataset.v : +el.dataset.v;
    dGoalDraft[k] = v;
    if (k === 'obj' && RATES[v] && !RATES[v].includes(dGoalDraft.rate)) dGoalDraft.rate = v === 'ganhar' ? 0.25 : 0.5;
    goalSheet();
  },
  dSex: el => { S.profile = { ...(S.profile || {}), sexo: el.dataset.v }; save(); goalSheet(); },
  dGoalSave: () => {
    const D = dietData();
    D.goal = { obj: dGoalDraft.obj, rate: dGoalDraft.rate, act: dGoalDraft.act, prot: dGoalDraft.prot, manual: null };
    D.tdee = { v: formulaTDEE(), at: Date.now(), src: 'formula' };
    if (!D.tdee.v) { toast('Informe peso, altura e idade'); return; }
    const a = adaptiveTDEE();
    if (a.src === 'dados') D.tdee = { v: a.v, at: Date.now(), src: 'dados' };
    save(); closeSheet(); rerender(); toast('Metas salvas');
  },
  dNoteOk: () => { delete dietData().tdeeNote; save(); rerender(); },
  dBarcode: () => barcodeSheet(),
  dBarcodeGo: () => barcodeLookup($('#bcCode').value),
  dAI: el => { if (!el.closest('#sheet')) dPick.m = mealNow(); if (aiReady()) aiTextSheet(); },
  dAiRun: () => {
    const t = ($('#aiMeal').value || '').trim();
    if (t.length < 3) { toast('Descreva o que você comeu'); return; }
    runFoodAI('meal', { text: t });
  },
  dAiMeal: el => { dAi.m = +el.dataset.v; el.parentNode.querySelectorAll('button').forEach(b => b.classList.toggle('on', b === el)); },
  dAiAdd: () => {
    const key = dayKey(dietT()), on = dAi.items.filter(x => x.on && num(x.gramas) > 0);
    if (!on.length) { toast('Marque pelo menos um item'); return; }
    for (const x of on) addEntry(key, { m: dAi.m, n: x.nome, ref: null, gr: num(x.gramas), q: null, u: null, ...aiItemMacros(x), fi: 0, ai: true });
    save(); closeSheet(); rerender(); toast(`${on.length} ${on.length > 1 ? 'itens adicionados' : 'item adicionado'} em ${MEALS[dAi.m]}`);
  }
};

// Campos e fotos (chamados pelos ouvintes de input/change do app.js)
function dietInput(t) {
  if (t.id === 'foodq') {
    dPick.q = t.value.trim();
    $('#foodlist').innerHTML = foodList();
    $('#foodtabs').innerHTML = foodTabs();
    return true;
  }
  if (t.hasAttribute('data-dq') && dPortion) { dPortion.qty = t.value; $('#dprev').innerHTML = portionPreview(); return true; }
  if (t.dataset.aiG != null && dAi) {
    const i = +t.dataset.aiG, x = dAi.items[i], mm = (x.gramas = t.value, aiItemMacros(x));
    $(`[data-ai-sub="${i}"]`).textContent = `${fmtInt(mm.k)} kcal · P ${fmt(mm.p, 0)} · C ${fmt(mm.c, 0)} · G ${fmt(mm.f, 0)}`;
    $('#aiTot').innerHTML = aiTotalHTML();
    return true;
  }
  if (t.dataset.dgProf) {
    const v = num(t.value);
    if (v) { S.profile = { ...(S.profile || {}), [t.dataset.dgProf]: String(v) }; save(); }
    return true;
  }
  return false;
}
function dietChange(t) {
  if (t.dataset.aiOn != null && dAi) { dAi.items[+t.dataset.aiOn].on = t.checked; $('#aiTot').innerHTML = aiTotalHTML(); return true; }
  if (t.dataset.dietPhoto && t.files && t.files[0]) {
    const kind = t.dataset.dietPhoto, file = t.files[0];
    t.value = '';
    if (!t.closest('#sheet')) dPick.m = mealNow();
    if (!aiReady()) return true;
    photoToJpeg(file).then(img => runFoodAI(kind === 'label' ? 'label' : 'meal', { image: img }))
      .catch(() => toast('Não foi possível abrir a foto'));
    return true;
  }
  if (t.dataset.dgProf) { goalSheet(); return true; }
  return false;
}
