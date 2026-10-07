// Ferramentas de treino: progressão automática (sugestão de carga e repetições),
// volume semanal por músculo e calculadoras (anilhas, aquecimento e 1RM).
// Usa o estado e as funções do app.js (S, getEx, exHistory, openSheet…), chamadas só depois de tudo carregar.
'use strict';

/* ================= Progressão automática ================= */
// Dupla progressão por regras, como no MacroFactor Workouts e no Fitbod:
// fez o topo da faixa de repetições em todas as séries → sobe a carga e recomeça no começo da faixa;
// ficou dentro da faixa → mesma carga, +1 rep; ficou abaixo da faixa duas vezes seguidas → reduz ~10%.
// O RIR (repetições na reserva), quando registrado, deixa o passo maior se sobrou muito.
const DEFAULT_RANGE = [8, 12];
function repRange(target) {
  const t = String(target || '');
  let m = t.match(/(\d+)\s*(?:-|–|a|até)\s*(\d+)/i);
  if (m) return [Math.min(+m[1], +m[2]), Math.max(+m[1], +m[2])];
  m = t.match(/\d+/);
  return m && +m[0] > 0 ? [+m[0], +m[0]] : null;
}
// Menor salto de carga de cada equipamento (kg)
function loadStep(ex) {
  const eq = ex && ex.equip;
  return eq === 'Halteres' ? 2 : eq === 'Kettlebell' ? 4 : eq === 'Máquina' || eq === 'Polia' ? 5 : 2.5;
}
const roundTo = (v, step) => Math.round(v / step) * step;
// Número para preencher campos (sem separador de milhar, vírgula decimal)
const fmtIn = n => String(Math.round(n * 100) / 100).replace('.', ',');
// Séries que contam para a progressão: sem aquecimento e sem drop set
const progSets = sets => sets.filter(s => !s.warm && s.t !== 'drop');

function suggestNext(exId, target) {
  const ex = getEx(exId), kind = ex ? ex.kind : null;
  if (!kind || kind === 'c') return null;
  const hist = exHistory(exId).map(h => progSets(h.sets)).filter(s => s.length);
  if (!hist.length) {
    return kind === 's' ? null : { type: 'first', sets: [], text: 'Primeira vez', why: 'Escolha uma carga em que sobrem 2 ou 3 repetições no fim de cada série. A partir do próximo treino o app sugere a evolução.' };
  }
  const last = hist[hist.length - 1], prev = hist[hist.length - 2];
  const rirs = last.map(s => s.rir).filter(v => v != null);
  const easy = rirs.length > 0 && sum(rirs) / rirs.length >= 3;
  if (kind === 's') {
    const add = easy ? 10 : 5;
    return { type: 'reps', sets: last.map(s => ({ a: (s.a || 0) + add, b: null })), text: `+${add} s por série`, why: `Tente segurar ${add} segundos a mais que no último treino` };
  }
  const [lo, hi] = repRange(target) || DEFAULT_RANGE;
  const inc = easy ? 2 : 1;
  const W = max(last.map(s => s.a || 0));
  // Peso corporal sem carga extra: progride nas repetições
  if (kind === 'bw' && !W) {
    const minR = Math.min(...last.map(s => s.b || 0));
    if (minR >= hi) return { type: 'up', sets: last.map(s => ({ a: null, b: (s.b || 0) + 1 })), text: `${hi}+ reps`, why: `Você passou de ${hi} reps em todas as séries: continue somando reps ou adicione carga (+kg) ou uma variação mais difícil` };
    return { type: 'reps', sets: last.map(s => ({ a: null, b: Math.min(hi, (s.b || 0) + inc) })), text: `${Math.min(hi, minR + inc)} reps`, why: `Tente ${inc === 1 ? '1 rep' : '2 reps'} a mais por série (faixa ${lo}–${hi})` };
  }
  const step = kind === 'bw' ? 2.5 : loadStep(ex);
  const kg = v => kind === 'bw' ? `+${fmt(v, 2)} kg` : `${fmt(v, 2)} kg`;
  const top = last.filter(s => (s.a || 0) === W), topMin = Math.min(...top.map(s => s.b || 0));
  if (topMin >= hi) {
    const a = W + step * inc;
    return {
      type: 'up', sets: last.map(s => (s.a || 0) === W ? { a, b: lo } : { a: s.a || 0, b: s.b }),
      text: `${kg(a)} × ${lo}`, why: `Você fez ${hi === lo ? hi : `${hi}+`} reps em todas as séries com ${kg(W)}: suba a carga${hi > lo ? ` e recomece em ${lo} reps` : ''}`
    };
  }
  if (topMin < lo) {
    const prevTop = prev ? prev.filter(s => (s.a || 0) === W) : [];
    if (prevTop.length && Math.min(...prevTop.map(s => s.b || 0)) < lo) {
      const a = Math.max(kind === 'bw' ? 0 : step, roundTo(W * 0.9, step));
      return { type: 'down', sets: last.map(s => ({ a: Math.min(a, s.a || 0), b: lo })), text: `${kg(a)} × ${lo}`, why: `Duas vezes abaixo de ${lo} reps com ${kg(W)}: reduza um pouco a carga e volte a progredir` };
    }
    return { type: 'keep', sets: last.map(s => ({ a: s.a || 0, b: Math.max(lo, s.b || 0) })), text: `${kg(W)} × ${lo}`, why: `Ficou abaixo de ${lo} reps: mantenha a carga e busque ${lo} reps` };
  }
  return {
    type: 'reps', sets: last.map(s => ({ a: s.a || 0, b: Math.min(hi, (s.b || 0) + inc) })),
    text: `${kg(W)} × ${Math.min(hi, topMin + inc)}`, why: `Mantenha a carga e tente ${inc === 1 ? '1 rep' : '2 reps'} a mais por série (faixa ${lo}–${hi})`
  };
}
// Sugestão de um exercício do treino em andamento (calculada uma vez por exercício)
const sgCache = new WeakMap();
function suggestionFor(ex) {
  if (S.settings.progression === false) return null;
  const c = sgCache.get(ex);
  if (c && c.target === ex.target) return c.sg;
  const sg = suggestNext(ex.exId, ex.target);
  sgCache.set(ex, { target: ex.target, sg });
  return sg;
}
const SG_ICON = { up: '↑', reps: '+', keep: '=', down: '↓', first: '•' };
function suggestionHTML(sg) {
  if (!sg) return '';
  return `<div class="sugg ${sg.type}"><b>${SG_ICON[sg.type]} ${sg.type === 'first' ? '' : 'Hoje: '}${esc(sg.text)}</b><span>${esc(sg.why)}</span></div>`;
}

/* ================= Volume semanal por músculo ================= */
// Séries válidas (sem aquecimento) por grupo muscular; músculo secundário conta meia série.
const VOL_GROUPS = [
  ['Peito', ['pei']], ['Costas', ['dor', 'mei']], ['Ombros', ['omb']], ['Bíceps', ['bic']], ['Tríceps', ['tri']],
  ['Quadríceps', ['qua']], ['Posteriores', ['pos']], ['Glúteos', ['glu']], ['Panturrilhas', ['pan']], ['Abdômen', ['abd']],
  ['Trapézio', ['tra']], ['Antebraços', ['ant']], ['Lombar', ['lom']], ['Adutores', ['adu']], ['Abdutores', ['abu']], ['Pescoço', ['pes']]
];
const VOL_MAIN = 10;          // os 10 primeiros aparecem sempre; os outros só quando treinados
const VOL_RANGE = [10, 20];   // faixa usada nos estudos de hipertrofia (séries por músculo por semana)
const VOL_SCALE = 25;
function groupSets(from, to) {
  const out = VOL_GROUPS.map(() => 0);
  if (typeof musclesOf !== 'function') return out;
  for (const sess of S.sessions) {
    if (sess.start < from || sess.start >= to) continue;
    for (const e of sess.exercises) {
      const ex = getEx(e.exId);
      if (e.kind === 'c' || (ex && ex.group === 'Alongamento')) continue;
      const n = e.sets.filter(x => !x.warm).length;
      if (!n) continue;
      const { p, s } = musclesOf(ex);
      VOL_GROUPS.forEach(([, codes], i) => {
        if (codes.some(c => p.includes(c))) out[i] += n;
        else if (codes.some(c => s.includes(c))) out[i] += n / 2;
      });
    }
  }
  return out;
}
let volRange = 'week';
const VOL_RANGES = [['week', 'Esta semana'], ['last', 'Semana passada'], ['avg', 'Média 4 sem.']];
function volumeHTML() {
  const wk = startOfWeek(Date.now());
  const vals = volRange === 'last' ? groupSets(addDays(wk, -7), wk)
    : volRange === 'avg' ? groupSets(addDays(wk, -28), wk).map(v => v / 4)
      : groupSets(wk, addDays(wk, 7));
  const [lo, hi] = VOL_RANGE, pct = v => Math.min(100, v / VOL_SCALE * 100);
  const rows = VOL_GROUPS.map(([name], i) => ({ name, v: vals[i], i })).filter(r => r.i < VOL_MAIN || r.v > 0).map(({ name, v }) => {
    const cls = !v ? '' : v < lo ? 'low' : v <= hi ? 'ok' : 'high';
    return `<div class="vrow"><span class="vname">${name}</span>
      <div class="vbar"><i style="left:${pct(lo)}%;width:${pct(hi) - pct(lo)}%"></i><b class="${cls}" style="width:${pct(v)}%"></b></div>
      <span class="vnum num">${fmt(v, 1)}</span></div>`;
  }).join('');
  return `<h2 class="section">Séries por músculo</h2>
    <div class="seg seg-sm" style="grid-template-columns:repeat(3,1fr);margin-bottom:10px">${VOL_RANGES.map(([v, l]) => `<button class="${volRange === v ? 'on' : ''}" data-act="volRange" data-v="${v}">${l}</button>`).join('')}</div>
    <div class="card vol">${rows}
      <div class="vlegend"><span><i class="zone"></i>faixa recomendada: ${lo}–${hi} séries</span><span><i class="low"></i>abaixo</span><span><i class="high"></i>acima</span></div>
      <p class="small muted" style="margin:8px 0 0">Para hipertrofia, os estudos indicam ${lo} a ${hi} séries por músculo por semana. Séries de aquecimento não contam e músculos secundários contam meia série${volRange === 'week' ? '. A semana atual ainda está em andamento' : ''}.</p></div>`;
}

/* ================= Calculadoras ================= */
const PLATES = [25, 20, 15, 10, 5, 2.5, 1.25];
const PLATE_COLORS = { 25: '#D8392F', 20: '#2F6BD6', 15: '#E5B315', 10: '#2E9B57', 5: '#ECECE6', 2.5: '#B9473D', 1.25: '#A8ADB4' };
const BARS = [[20, '20 kg'], [15, '15 kg'], [10, '10 kg'], [0, 'Sem barra']];
function barNow() { return S.settings.bar ?? 20; }
function platesAvail() { return (S.settings.plates || PLATES).slice().sort((a, b) => b - a); }
// Anilhas de cada lado para chegar ao peso total (a sobra > 0 quando não fecha exato)
function platesFor(total, bar, avail) {
  let side = (total - bar) / 2;
  if (!(side >= 0)) return null;
  const out = [];
  for (const p of avail) while (side >= p - 1e-9) { out.push(p); side -= p; }
  return { plates: out, rest: Math.round(side * 1000) / 1000 };
}
function warmupSets(W, bar, step) {
  if (!(W > 0)) return [];
  const scheme = W < 40 ? [[0.5, 10], [0.75, 5]] : [[0.4, 10], [0.6, 5], [0.8, 3]];
  const out = [];
  for (const [p, r] of scheme) {
    const w = Math.max(bar, roundTo(W * p, step));
    if (w >= W || (out.length && out[out.length - 1].w === w)) continue;
    out.push({ w, r, p });
  }
  return out;
}
// Repetições → % do 1RM pela fórmula de Epley (a mesma do 1RM estimado dos gráficos)
const RM_REPS = [1, 2, 3, 4, 5, 6, 8, 10, 12, 15];
const rmLoad = (rm, r) => r === 1 ? rm : rm / (1 + r / 30);

let calc = null; // { mode, w, r }
function openCalc(mode = 'plates', w, r) {
  calc = { mode, w: w > 0 ? w : (calc && calc.w) || 60, r: r > 0 ? r : (calc && calc.r) || 5 };
  renderCalc();
}
function barbellHTML(bar, plates) {
  // da ponta para o centro: as menores por fora, as maiores junto do colar
  const pl = plates.slice().reverse().map(p => `<i style="--pc:${PLATE_COLORS[p] || '#999'};--ph:${Math.round(34 + p * 2.2)}px;--pw:${p >= 10 ? 13 : p >= 5 ? 9 : 7}px" title="${fmt(p, 2)} kg"></i>`).join('');
  return `<div class="barbell" aria-hidden="true"><span class="sleeve"></span>${pl}<span class="collar"></span><span class="shaft">${bar ? fmt(bar) + ' kg' : ''}</span></div>`;
}
function calcOut() {
  const { mode, w, r } = calc, bar = barNow();
  if (mode === 'plates') {
    const res = platesFor(w, bar, platesAvail());
    if (!res) return `<p class="muted">O peso total precisa ser pelo menos o da barra (${fmt(bar)} kg).</p>`;
    const per = res.plates.length ? res.plates.map(p => fmt(p, 2)).join(' + ') + ' kg' : 'nenhuma anilha';
    const loaded = bar + 2 * sum(res.plates);
    return `${barbellHTML(bar, res.plates)}
      <div class="calc-big"><span class="muted small">Cada lado</span><b class="num">${per}</b></div>
      ${res.rest > 0 ? `<p class="small" style="color:var(--gold);margin:6px 0 0">Não fecha exato com essas anilhas: monta ${fmt(loaded, 2)} kg (faltam ${fmt(res.rest * 2, 2)} kg).</p>` : ''}`;
  }
  if (mode === 'warm') {
    const sets = warmupSets(w, bar, 2.5);
    if (!sets.length) return '<p class="muted">Informe a carga de trabalho.</p>';
    return `<div class="list calc-list">${sets.map((s, i) => {
      const res = platesFor(s.w, bar, platesAvail());
      const per = res && res.plates.length ? `cada lado: ${res.plates.map(p => fmt(p, 2)).join(' + ')}` : bar ? 'só a barra' : '';
      return `<div class="kv"><span>${i + 1}ª · ${Math.round(s.p * 100)}%${per ? ` <small class="muted">${per}</small>` : ''}</span><b class="num">${fmt(s.w, 2)} kg × ${s.r}</b></div>`;
    }).join('')}<div class="kv"><span>Séries de trabalho</span><b class="num">${fmt(w, 2)} kg</b></div></div>
      <p class="small muted" style="margin:8px 2px 0">Descanse cerca de 1 minuto entre as séries de aquecimento. Elas não contam no volume nem nos recordes.</p>`;
  }
  const rm = e1rm(w, Math.round(r));
  if (!rm) return '<p class="muted">Informe a carga e as repetições.</p>';
  return `<div class="calc-big"><span class="muted small">1RM estimado</span><b class="num">${fmt(rm, 1)} kg</b></div>
    <div class="list calc-list">${RM_REPS.map(n => `<div class="kv"><span>${n} rep${n > 1 ? 's' : ''} <small class="muted">${Math.round(rmLoad(100, n))}%</small></span><b class="num">${fmt(n === 1 ? rm : roundTo(rmLoad(rm, n), 0.5), 1)} kg</b></div>`).join('')}</div>
    <p class="small muted" style="margin:8px 2px 0">Estimativa pela fórmula de Epley; é mais precisa com séries de até 10 repetições.</p>`;
}
function renderCalc() {
  const { mode, w, r } = calc, bar = barNow();
  const field = (k, label, v) => `<label class="field" style="flex:1;margin:0"><span>${label}</span>
    <div class="calc-in"><button class="btn sm" data-act="calcStep" data-k="${k}" data-d="-1" aria-label="Diminuir">−</button>
    <input class="input num" inputmode="decimal" data-calc="${k}" value="${fmtIn(v)}"><button class="btn sm" data-act="calcStep" data-k="${k}" data-d="1" aria-label="Aumentar">+</button></div></label>`;
  const barSeg = `<div class="look-label" style="margin-top:14px">Barra</div>
    <div class="seg seg-sm">${BARS.map(([v, l]) => `<button class="${bar === v ? 'on' : ''}" data-act="calcBar" data-v="${v}">${l}</button>`).join('')}</div>`;
  let body = mode === 'rm'
    ? `<div style="display:grid;gap:12px">${field('w', 'Carga (kg)', w)}${field('r', 'Repetições', r)}</div>`
    : field('w', mode === 'plates' ? 'Peso total na barra (kg)' : 'Carga de trabalho (kg)', w) + barSeg;
  if (mode === 'plates') {
    body += `<div class="look-label" style="margin-top:14px">Anilhas disponíveis</div>
      <div class="chips wrap" style="margin:0">${PLATES.map(p => `<button class="chip ${platesAvail().includes(p) ? 'on' : ''}" data-act="calcPlate" data-v="${p}">${fmt(p, 2)}</button>`).join('')}</div>`;
  }
  openSheet(`${sheetHead('Calculadoras')}<div class="sheet-body">
    <div class="seg seg-sm" style="grid-template-columns:repeat(3,1fr);margin-bottom:14px">${[['plates', 'Anilhas'], ['warm', 'Aquecimento'], ['rm', '1RM']].map(([v, l]) => `<button class="${mode === v ? 'on' : ''}" data-act="calcMode" data-v="${v}">${l}</button>`).join('')}</div>
    ${body}<div id="calcOut" class="calc-out">${calcOut()}</div></div>`);
}
function calcInput(t) {
  if (!calc) return;
  const v = num(t.value);
  calc[t.dataset.calc] = v != null && v >= 0 ? v : 0;
  const out = $('#calcOut');
  if (out) out.innerHTML = calcOut();
}

/* ================= Ações ================= */
const TOOLS_ACTIONS = {
  volRange: el => { volRange = el.dataset.v; rerender(); },
  calc: el => openCalc(el.dataset.mode || 'plates', num(el.dataset.w), num(el.dataset.r)),
  calcMode: el => { calc.mode = el.dataset.v; renderCalc(); },
  calcBar: el => { S.settings.bar = +el.dataset.v; save(); renderCalc(); },
  calcPlate: el => {
    const p = +el.dataset.v, cur = platesAvail();
    const next = cur.includes(p) ? cur.filter(x => x !== p) : cur.concat(p);
    if (!next.length) { toast('Deixe pelo menos uma anilha'); return; }
    S.settings.plates = next.sort((a, b) => b - a); save(); renderCalc();
  },
  calcStep: el => {
    const k = el.dataset.k, d = +el.dataset.d;
    calc[k] = Math.max(0, (calc[k] || 0) + d * (k === 'r' ? 1 : 2.5));
    const inp = $(`[data-calc="${k}"]`);
    if (inp) inp.value = fmtIn(calc[k]);
    $('#calcOut').innerHTML = calcOut();
  }
};
