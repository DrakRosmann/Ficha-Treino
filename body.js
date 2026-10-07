// Corpo: mapa dos músculos trabalhados e acompanhamento de peso, medidas e composição corporal.
// Carregado antes do app.js; as funções daqui usam utilitários do app.js só quando são chamadas.
'use strict';

/* ================= Mapa muscular ================= */
// Desenho do corpo (polígonos, viewBox 0 0 100 200) adaptado de react-body-highlighter
// Copyright (c) 2020 GV79 — licença MIT — https://github.com/GV79/react-body-highlighter
const BODY_FRONT = [['chest', ['51.8 41.6 51 55.1 58 58 67.8 55.5 70.6 47.3 62 41.6', '29.8 46.5 31.4 55.5 40.8 58 48.2 55.1 47.8 42 37.6 42']],
  ['obliques', ['68.6 63.3 67.3 57.1 58.8 59.6 60 64.1 60.4 83.3 65.7 78.8 66.5 69.8', '33.9 78.4 33.1 71.8 31 63.3 32.2 57.1 40.8 59.2 39.2 63.3 39.2 83.7']],
  ['abs', ['56.3 59.2 58 64.1 58.4 78 58.4 92.7 56.3 98.4 55.1 104.1 51.4 107.8 51 84.5 50.6 67.3 51 57.1', '43.7 58.8 48.6 57.1 49 67.3 48.6 84.5 48.2 107.3 44.5 103.7 40.8 91.4 40.8 78.4 41.2 64.5']],
  ['biceps', ['16.7 68.2 18 71.4 22.9 66.1 29 53.9 27.8 49.4 20.4 55.9', '71.4 49.4 70.2 54.7 76.3 66.1 81.6 71.8 82.9 69 78.8 55.5']],
  ['triceps', ['69.4 55.5 69.4 61.6 75.9 72.7 77.6 70.2 75.5 67.3', '22.4 69.4 29.8 55.5 29.8 60.8 22.9 73.1']],
  ['neck', ['55.5 23.7 50.6 33.5 50.6 39.2 61.6 40 70.6 44.9 69.4 36.7 63.3 35.1 58.4 30.6', '29 44.9 30.2 37.1 36.3 35.1 41.2 30.2 44.5 24.5 49 33.9 48.6 39.2 38 39.6']],
  ['front-deltoids', ['78.4 53.1 79.6 47.8 79.2 41.2 75.9 38 71 36.3 72.2 42.9 71.4 47.3', '28.2 47.3 21.2 53.1 20 47.8 20.4 40.8 24.5 37.1 28.6 37.1 26.9 43.3']],
  ['head', ['42.4 2.9 40 11.8 42 19.6 46.1 23.3 49.8 25.3 54.7 22.4 57.6 19.2 59.2 10.2 57.1 2.4 49.8 0']],
  ['abductors', ['52.7 110.2 54.3 124.9 60 110.2 62 100 64.9 94.3 60 92.7 56.7 104.5', '47.8 110.6 44.9 125.3 42 115.9 40.4 113.1 39.6 107.3 38 102.4 34.7 93.9 39.6 92.2 41.6 99.2 43.7 105.3']],
  ['quadriceps', ['34.7 98.8 37.1 108.2 37.1 127.8 34.3 137.1 31 132.7 29.4 120 28.2 111.4 29.4 100.8 32.2 94.7', '63.3 105.7 64.5 100 66.9 94.7 70.2 101.2 71 111.8 68.2 133.1 65.3 137.6 62.4 128.6 62 111.4', '38.8 129.4 38.4 112.2 41.2 118.4 44.5 129.4 42.9 135.1 40 146.1 36.3 146.5 35.5 140', '59.6 145.7 55.5 129 60.8 113.9 61.2 130.2 64.1 139.6 62.9 146.5', '32.7 138.4 26.5 145.7 25.7 136.7 25.7 127.3 26.9 114.3 29.4 133.5', '71.8 113.1 73.9 124.1 73.9 140.4 72.7 145.7 66.5 138.4 70.2 133.5']],
  ['knees', ['33.9 140 34.7 143.3 35.5 147.3 36.3 151 35.1 156.7 29.8 156.7 27.3 152.7 27.3 147.3 30.2 144.1', '65.7 140 72.2 147.8 72.2 152.2 69.8 157.1 64.9 156.7 62.9 151']],
  ['calves', ['71.4 160.4 73.5 153.5 76.7 161.2 79.6 167.8 78.4 187.8 79.6 195.5 74.7 195.5', '24.9 194.7 27.8 164.9 28.2 160.4 26.1 154.3 24.9 157.6 22.4 161.6 20.8 167.8 22 188.2 20.8 195.5', '72.7 195.1 69.8 159.2 65.3 158.4 64.1 162.4 64.1 165.3 65.7 177.1', '35.5 158.4 35.9 162.4 35.9 166.9 35.1 172.2 35.1 176.7 32.2 182 30.6 187.3 26.9 194.7 27.3 187.8 28.2 180.4 28.6 175.5 29 169.8 29.8 164.1 30.2 158.8']],
  ['forearm', ['6.1 88.6 10.2 75.1 14.7 70.2 16.3 74.3 19.2 73.5 4.5 97.6 0 100', '84.5 69.8 83.3 73.5 80 73.1 95.1 98.4 100 100.4 93.5 89.4 89.8 76.3', '77.6 72.2 77.6 77.6 80.4 84.1 85.3 89.8 92.2 101.2 94.7 99.6', '6.9 101.2 13.5 90.6 18.8 84.1 21.6 77.1 21.2 71.8 4.9 98.8']]];
const BODY_BACK = [['head', ['50.6 0 46 0.9 40.9 5.5 40.4 12.8 45.1 20 55.7 20 59.1 13.6 59.6 4.7 55.7 1.3']],
  ['trapezius', ['44.7 21.7 47.7 21.7 47.2 38.3 47.7 64.7 38.3 53.2 35.3 40.9 31.1 36.6 39.1 33.2 43.8 27.2', '52.3 21.7 55.7 21.7 56.6 27.2 60.9 32.8 68.9 36.6 64.7 40.4 61.7 53.2 52.3 64.7 53.2 38.3']],
  ['back-deltoids', ['29.4 37 23 39.1 17.4 44.3 18.3 53.6 24.3 49.4 27.2 46.4', '71.1 37 78.3 39.6 82.6 44.7 81.7 53.6 74.9 48.9 72.3 45.1']],
  ['upper-back', ['31.1 38.7 28.1 48.9 28.5 55.3 34 75.3 47.2 71.1 47.2 66.4 36.6 54 33.6 41.3', '68.9 38.7 71.9 49.4 71.5 56.2 66 75.3 52.8 71.1 52.8 66.4 63.4 54.5 66.4 41.7']],
  ['triceps', ['26.8 49.8 17.9 55.7 14.5 72.3 16.6 81.7 21.7 63.8 26.8 55.7', '73.6 50.2 82.1 55.7 86 73.2 83.4 82.1 77.9 63 73.2 55.7', '26.8 58.3 26.8 68.5 23 75.3 19.1 77.4 22.6 65.5', '72.8 58.3 77 64.7 80.4 77.4 76.6 75.3 72.8 68.9']],
  ['lower-back', ['47.7 72.8 34.5 77 35.3 83.4 49.4 102.1 46.8 83', '52.3 72.8 65.5 77 64.7 83.4 50.6 102.1 53.2 83.8']],
  ['forearm', ['86.4 75.7 91.1 83.4 93.2 94 100 106.4 96.2 104.3 88.1 89.4 84.3 83.8', '13.6 75.7 8.9 83.8 6.8 93.6 0 106.4 3.8 104.3 12.3 88.5 15.7 83', '81.3 79.6 77.4 77.9 79.1 84.7 91.1 103.8 93.2 108.9 94.5 104.7', '18.7 79.6 22.1 77.9 20.9 84.3 9.4 103 6.8 108.5 5.1 104.7']],
  ['gluteal', ['44.7 99.6 30.2 108.5 29.8 118.7 31.5 126 47.2 121.3 49.4 114.9', '55.3 99.1 51.1 114.5 52.3 120.9 68.1 126 69.8 119.1 69.4 108.5']],
  ['adductor', ['48.1 123 44.7 123 41.3 125.5 45.1 144.3 48.5 135.7 48.9 129.4', '51.9 122.6 55.7 123.4 59.1 126 54.9 144.3 51.9 136.2 51.1 129.4']],
  ['hamstring', ['28.9 122.1 31.1 129.4 36.6 126 35.3 135.3 34.5 150.2 29.4 158.3 28.9 146.8 27.7 141.3 27.2 131.5', '71.5 121.7 69.4 128.9 63.8 126 65.5 136.6 66.4 150.2 71.1 158.3 71.5 147.7 72.8 142.1 73.6 131.9', '38.7 125.5 44.3 146 40.4 166.8 36.2 152.8 37 135.3', '61.7 125.5 63.4 136.2 64.3 153.2 60 166.8 56.2 146.4']],
  ['knees', ['34.5 153.2 31.1 159.1 33.6 166.4 37.4 162.6', '66.4 153.6 63 163 66.8 166.4 69.4 159.1']],
  ['calves', ['29.4 160.4 28.5 167.2 24.7 179.6 23.8 192.8 25.5 197 28.5 193.2 29.8 180 31.9 171.1 31.9 166.8', '37.4 165.1 35.3 167.7 33.2 171.9 31.1 180.4 30.2 191.9 34 200 38.7 190.6 39.1 168.9', '63 165.1 61.3 168.5 61.7 190.6 66.4 199.6 70.6 191.9 68.9 179.6 66.8 170.2', '70.6 160.4 72.3 168.5 75.7 179.1 76.6 192.8 74.5 196.6 72.3 193.6 70.6 179.6 68.1 168.1']],
  ['left-soleus', ['28.5 195.7 30.2 195.7 33.6 201.7 30.6 220 28.5 213.6 26.8 198.3']],
  ['right-soleus', ['69.8 195.7 71.9 195.7 73.6 198.3 71.9 213.2 70.2 219.6 67.2 202.1']]];

// Códigos de músculo do catálogo (MUSCLES) → regiões do desenho
const MUSCLE_REGIONS = {
  pei: ['chest'], abd: ['abs', 'obliques'], omb: ['front-deltoids', 'back-deltoids'], bic: ['biceps'], tri: ['triceps'],
  ant: ['forearm'], qua: ['quadriceps'], adu: ['abductors', 'adductor'], abu: ['gluteal'], glu: ['gluteal'],
  pos: ['hamstring'], pan: ['calves', 'left-soleus', 'right-soleus'], dor: ['upper-back'], mei: ['trapezius', 'upper-back'],
  lom: ['lower-back'], tra: ['trapezius', 'neck'], pes: ['neck']
};
// Para exercícios sem músculos cadastrados (ex.: personalizados), usa o grupo
const GROUP_MUSCLES = {
  'Peito': 'pei|omb,tri', 'Costas': 'dor,mei|bic', 'Ombros': 'omb|tra', 'Bíceps': 'bic|ant', 'Tríceps': 'tri|',
  'Antebraço': 'ant|', 'Quadríceps': 'qua|glu', 'Posterior de coxa': 'pos|glu', 'Glúteos': 'glu|pos',
  'Panturrilha': 'pan|', 'Abdômen': 'abd|', 'Pescoço': 'pes|tra'
};
function musclesOf(ex) {
  if (!ex) return { p: [], s: [] };
  if ((ex.primary || []).length) return { p: ex.primary, s: ex.secondary || [] };
  const g = GROUP_MUSCLES[ex.group];
  if (!g) return { p: [], s: [] };
  const [p, s] = g.split('|');
  return { p: p.split(','), s: s ? s.split(',') : [] };
}
function bodyFigure(data, cls, label) {
  return `<figure class="bm-fig"><svg viewBox="0 0 100 200" role="img" aria-label="${label}">${data.map(([m, polys]) =>
    polys.map(pts => `<polygon points="${pts}" class="${m === 'head' || m === 'knees' ? 'bm-base' : 'bm-m ' + (cls[m] || '')}"/>`).join('')).join('')}</svg><figcaption>${label}</figcaption></figure>`;
}
function bodyMapSVG(cls) { return `<div class="bm">${bodyFigure(BODY_FRONT, cls, 'Frente')}${bodyFigure(BODY_BACK, cls, 'Costas')}</div>`; }
// Mapa com os músculos de um exercício: principais em destaque, secundários mais claros
function muscleMapHTML(ex) {
  const { p, s } = musclesOf(ex);
  if (!p.length) return '';
  const cls = {};
  for (const c of s) for (const r of MUSCLE_REGIONS[c] || []) cls[r] = 'sec';
  for (const c of p) for (const r of MUSCLE_REGIONS[c] || []) cls[r] = 'pri';
  const names = list => list.map(c => esc(MUSCLES[c] || c)).join(', ');
  return `<div class="bm-card">${bodyMapSVG(cls)}<div class="bm-legend">
      <div class="bm-key"><i class="pri"></i><span><b>Principais</b>${names(p)}</span></div>
      ${s.length ? `<div class="bm-key"><i class="sec"></i><span><b>Secundários</b>${names(s)}</span></div>` : ''}
    </div></div>`;
}
// Séries por músculo nos últimos dias (principal conta 1 por série, secundário 0,5)
function muscleLoad(days) {
  const since = startOfDay(Date.now()) - (days - 1) * 864e5, load = {};
  for (const sess of S.sessions) {
    if (sess.start < since) continue;
    for (const e of sess.exercises) {
      const n = e.sets.filter(x => !x.warm).length;
      if (!n) continue;
      const { p, s } = musclesOf(getEx(e.exId));
      for (const c of p) load[c] = (load[c] || 0) + n;
      for (const c of s) load[c] = (load[c] || 0) + n / 2;
    }
  }
  return load;
}
function muscleLoadHTML(days) {
  const load = muscleLoad(days), cls = {}, lvl = v => v >= 10 ? 3 : v >= 5 ? 2 : v > 0 ? 1 : 0, best = {};
  for (const [c, v] of Object.entries(load)) for (const r of MUSCLE_REGIONS[c] || []) best[r] = Math.max(best[r] || 0, v);
  for (const [r, v] of Object.entries(best)) if (lvl(v)) cls[r] = 'l' + lvl(v);
  const top = Object.entries(load).sort((a, b) => b[1] - a[1]);
  return `<div class="card bm-heat">${bodyMapSVG(cls)}
    <div class="bm-scale"><span><i class="l1"></i>1–4</span><span><i class="l2"></i>5–9</span><span><i class="l3"></i>10+ séries</span></div>
    ${top.length ? `<div class="muscles" style="justify-content:center">${top.map(([c, v]) => `<span class="mtag ${v >= 5 ? 'p' : ''}">${esc(MUSCLES[c] || c)} <b class="num">${fmt(v, 1)}</b></span>`).join('')}</div>`
      : '<p class="small muted" style="text-align:center;margin:8px 0 0">Nenhum treino nos últimos 7 dias.</p>'}
  </div>`;
}

/* ================= Medidas e composição corporal ================= */
// [campo, nome, unidade, seção, melhor direção ('up' | 'down' | 'goal' | null), casas decimais, dica de como medir]
const BODY_FIELDS = [
  ['peso', 'Peso', 'kg', 'comp', 'goal', 1, ''],
  ['gordura', 'Gordura', '%', 'comp', 'down', 1, 'Da balança de bioimpedância ou avaliação física'],
  ['musculo', 'Massa muscular', 'kg', 'comp', 'up', 1, ''],
  ['agua', 'Água', '%', 'comp', null, 1, ''],
  ['visceral', 'Gordura visceral', 'nível', 'comp', 'down', 0, ''],
  ['ossea', 'Massa óssea', 'kg', 'comp', null, 1, ''],
  ['tmb', 'Metabolismo basal', 'kcal', 'comp', null, 0, ''],
  ['pescoco', 'Pescoço', 'cm', 'med', null, 1, 'Logo abaixo do pomo de adão'],
  ['ombros', 'Ombros', 'cm', 'med', 'up', 1, 'Volta completa na parte mais larga dos ombros'],
  ['peito', 'Peito', 'cm', 'med', 'up', 1, 'Na altura dos mamilos, ao fim de uma expiração'],
  ['braco', 'Braço', 'cm', 'med', 'up', 1, 'Meio do braço, contraído'],
  ['antebraco', 'Antebraço', 'cm', 'med', 'up', 1, 'Parte mais grossa, braço estendido'],
  ['cintura', 'Cintura', 'cm', 'med', 'down', 1, 'Parte mais fina do tronco, sem contrair'],
  ['abdomen', 'Abdômen', 'cm', 'med', 'down', 1, 'Na altura do umbigo, sem contrair'],
  ['quadril', 'Quadril', 'cm', 'med', null, 1, 'Parte mais larga dos glúteos, pés juntos'],
  ['coxa', 'Coxa', 'cm', 'med', 'up', 1, 'Logo abaixo do glúteo'],
  ['panturrilha', 'Panturrilha', 'cm', 'med', 'up', 1, 'Parte mais grossa, em pé']
];
const BF = Object.fromEntries(BODY_FIELDS.map(f => [f[0], f]));
const BODY_SHORT = { musculo: 'Músculo', visceral: 'Visceral', ossea: 'Ossos', tmb: 'TMB' }; // rótulos curtos do formulário
function bodyHeight() { const h = num((S.profile || {}).altura); return h ? (h > 3 ? h : h * 100) : null; }
function bodySex() { return (S.profile || {}).sexo || ''; }
// Estimativa de % de gordura pelo método da Marinha dos EUA (fita métrica)
function navyFat(e) {
  const h = bodyHeight(), sx = bodySex(), n = e.pescoco;
  if (!h || !n) return null;
  if (sx === 'm') { const w = e.abdomen || e.cintura; return w && w > n ? 495 / (1.0324 - 0.19077 * Math.log10(w - n) + 0.15456 * Math.log10(h)) - 450 : null; }
  if (sx === 'f') { const w = e.cintura, q = e.quadril; return w && q && w + q > n ? 495 / (1.29579 - 0.35004 * Math.log10(w + q - n) + 0.221 * Math.log10(h)) - 450 : null; }
  return null;
}
// Métricas calculadas: [nome, unidade, cálculo, melhor direção, casas]
const BODY_DERIVED = {
  imc: ['IMC', '', e => { const h = bodyHeight(); return h && e.peso ? e.peso / (h / 100) ** 2 : null; }, null, 1],
  magra: ['Massa magra', 'kg', e => e.peso && e.gordura ? e.peso * (1 - e.gordura / 100) : null, 'up', 1],
  gordkg: ['Massa de gordura', 'kg', e => e.peso && e.gordura ? e.peso * e.gordura / 100 : null, 'down', 1],
  navy: ['Gordura estimada', '%', navyFat, 'down', 1],
  rcq: ['Cintura / quadril', '', e => e.cintura && e.quadril ? e.cintura / e.quadril : null, 'down', 2]
};
const BODY_CHART_ORDER = ['peso', 'gordura', 'navy', 'musculo', 'magra', 'gordkg', 'imc', 'cintura', 'abdomen', 'quadril', 'peito', 'ombros',
  'braco', 'antebraco', 'coxa', 'panturrilha', 'pescoco', 'rcq', 'agua', 'visceral', 'ossea', 'tmb'];
function bmeta(k) {
  if (BF[k]) { const [, name, unit, , better, dec] = BF[k]; return { name, unit, better, dec }; }
  const [name, unit, , better, dec] = BODY_DERIVED[k];
  return { name, unit, better, dec };
}
function bval(k, e) { return BF[k] ? (e[k] ?? null) : BODY_DERIVED[k][2](e); }
function bodyEntries() { return (S.body || []).slice().sort((a, b) => a.t - b.t); }
function imcLabel(v) { return v < 18.5 ? 'abaixo do peso' : v < 25 ? 'peso normal' : v < 30 ? 'sobrepeso' : 'obesidade'; }
const isoDate = t => { const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; };
function fmtDelta(v, dec) { return v == null || Math.abs(v) < Math.pow(10, -dec) / 2 ? '=' : `${v > 0 ? '+' : '−'}${fmt(Math.abs(v), dec)}`; }
// Direção boa da métrica; para o peso depende da meta (perder ou ganhar)
function betterOf(k) {
  const b = bmeta(k).better;
  if (b !== 'goal') return b;
  const g = S.bodyGoal && num(S.bodyGoal.peso), first = bodyEntries().find(e => e.peso != null);
  return g && first ? (g < first.peso ? 'down' : 'up') : null;
}
function deltaClass(k, d) {
  const b = betterOf(k);
  if (!b || !d) return '';
  return (b === 'up') === (d > 0) ? 'good' : 'bad';
}

let bodyMetric = 'peso', bodyRange = 'all';
const BODY_RANGES = [['30', '30 dias'], ['90', '3 meses'], ['365', '1 ano'], ['all', 'Tudo']];

function viewCorpo() {
  const all = bodyEntries(), last = all[all.length - 1], first = all[0];
  let html = topBar({ right: `<button class="link-btn" data-act="bodyNew">${I.plus.replace('<svg', '<svg class="inline-ic"')}Registrar</button>` }) + `<h1 class="title">Corpo</h1>`;
  if (!all.length) {
    html += `<div class="card">${emptyState(I.body, 'Acompanhe sua evolução',
      'Registre peso, medidas e composição corporal (gordura, massa muscular…) de tempos em tempos. Os gráficos mostram a sua evolução.',
      `<button class="btn primary block" data-act="bodyNew">${I.plus}Registrar medidas</button>`)}</div>`;
  } else {
    // Resumo: último valor de cada métrica principal e variação desde o primeiro registro
    const latest = k => { for (let i = all.length - 1; i >= 0; i--) { const v = bval(k, all[i]); if (v != null) return [v, all[i]]; } return null; };
    const earliest = k => { for (const e of all) { const v = bval(k, e); if (v != null) return v; } return null; };
    const rows = ['peso', 'gordura', 'navy', 'musculo', 'magra', 'imc', 'rcq'].map(k => {
      const L = latest(k); if (!L) return '';
      if (k === 'navy' && latest('gordura')) return '';
      const m = bmeta(k), d = L[0] - earliest(k);
      return `<button class="kv kv-btn" data-act="bodyMetric" data-k="${k}"><span>${m.name}${k === 'navy' ? ' <small class="muted">(pelas medidas)</small>' : ''}</span>
        <b>${fmt(L[0], m.dec)}${m.unit ? ' ' + m.unit : ''}${k === 'imc' ? ` <small class="muted">${imcLabel(L[0])}</small>` : ''}
        ${all.length > 1 ? `<em class="dl ${deltaClass(k, d)}">${fmtDelta(d, m.dec)}</em>` : ''}</b></button>`;
    }).join('');
    const goal = S.bodyGoal && num(S.bodyGoal.peso);
    const lw = latest('peso');
    html += `<div class="eyebrow" style="margin-top:-8px">Último registro: ${relDay(last.t).toLowerCase() === 'hoje' ? 'hoje' : dateShort(last.t)}${all.length > 1 ? ` · variação desde ${dateShort(first.t)}` : ''}</div>
      <div class="list" style="margin-top:8px">${rows}
        <button class="kv kv-btn" data-act="bodyGoal"><span>Meta de peso</span><b>${goal ? `${fmt(goal)} kg${lw ? ` <em class="dl">faltam ${fmt(Math.abs(lw[0] - goal))} kg</em>` : ''}` : '<span class="link-btn" style="padding:0">Definir</span>'}</b></button>
      </div>`;

    // Gráfico
    const avail = BODY_CHART_ORDER.filter(k => all.some(e => bval(k, e) != null));
    if (!avail.includes(bodyMetric)) bodyMetric = avail[0];
    const m = bmeta(bodyMetric);
    const since = bodyRange === 'all' ? 0 : startOfDay(Date.now()) - (+bodyRange) * 864e5;
    const pts = all.filter(e => e.t >= since).map(e => ({ t: e.t, v: bval(bodyMetric, e) })).filter(p => p.v != null);
    const better = betterOf(bodyMetric);
    html += `<h2 class="section">Evolução</h2>
      <div class="chips">${avail.map(k => `<button class="chip ${k === bodyMetric ? 'on' : ''}" data-act="bodyMetric" data-k="${k}">${bmeta(k).name}</button>`).join('')}</div>
      <div class="card">
        <div class="seg seg-sm" style="margin-bottom:12px">${BODY_RANGES.map(([v, l]) => `<button class="${bodyRange === v ? 'on' : ''}" data-act="bodyRange" data-v="${v}">${l}</button>`).join('')}</div>
        ${chartSVG(pts, m.unit, { time: true, better, dec: m.dec, max: 200, goal: bodyMetric === 'peso' ? goal : null,
          empty: pts.length ? 'Registre mais uma vez para ver a linha de evolução.' : 'Sem registros neste período.' })}
      </div>`;
    if (bodyMetric === 'navy') html += `<p class="small muted" style="margin:-2px 4px 10px">Estimativa pelo método da Marinha dos EUA (pescoço, cintura${bodySex() === 'f' ? ', quadril' : ''} e altura). Serve para comparar a sua própria evolução.</p>`;

    // Medidas
    const meds = BODY_FIELDS.filter(f => f[3] === 'med' && latest(f[0]));
    if (meds.length) {
      html += `<h2 class="section">Medidas</h2><div class="list">` + meds.map(([k, name, unit, , , dec]) => {
        const L = latest(k), d = L[0] - earliest(k);
        return `<button class="kv kv-btn" data-act="bodyMetric" data-k="${k}"><span>${name}</span><b>${fmt(L[0], dec)} ${unit}${all.length > 1 ? ` <em class="dl ${deltaClass(k, d)}">${fmtDelta(d, dec)}</em>` : ''}</b></button>`;
      }).join('') + '</div>';
    }
  }

  html += `<h2 class="section">Músculos treinados <span class="small" style="text-transform:none;letter-spacing:0">últimos 7 dias</span></h2>${muscleLoadHTML(7)}`;

  if (all.length) {
    html += `<h2 class="section">Registros <span class="small" style="text-transform:none;letter-spacing:0">${all.length}</span></h2><div class="list">` +
      all.slice().reverse().map(e => {
        const parts = [e.peso != null ? `${fmt(e.peso)} kg` : '', e.gordura != null ? `${fmt(e.gordura)}% gordura` : '', e.musculo != null ? `${fmt(e.musculo)} kg músculo` : '',
          e.cintura != null ? `cintura ${fmt(e.cintura)}` : ''].filter(Boolean);
        const nMed = BODY_FIELDS.filter(f => f[3] === 'med' && e[f[0]] != null).length;
        return `<button class="row" data-act="bodyEdit" data-id="${e.id}"><div class="dot"><span class="num">${new Date(e.t).getDate()}</span></div>
          <div class="grow"><div class="name">${dateLong(e.t)}</div><div class="sub">${parts.join(' · ') || `${nMed} medidas`}${nMed && parts.length ? ` · ${nMed} medidas` : ''}</div></div>${I.chev}</button>`;
      }).join('') + '</div>';
  }
  html += `<p class="small muted" style="margin:14px 4px 0;line-height:1.5">Dica: meça sempre nas mesmas condições — de manhã, em jejum e depois de ir ao banheiro. Variações de 1–2 kg de um dia para o outro são normais (água e alimentação).</p>`;
  return html;
}

function bodyForm(entry) {
  const e = entry || {}, h = bodyHeight(), sx = bodySex();
  const inp = ([k, name, unit, , , , tip]) => `<label title="${esc(tip)}"><span>${BODY_SHORT[k] || name}${unit && unit !== 'nível' && unit !== 'cm' ? ` (${unit})` : ''}</span>
    <input class="input num" type="text" inputmode="decimal" data-bf="${k}" value="${e[k] != null ? fmt(e[k], 2) : ''}" placeholder="—"></label>`;
  openSheet(`${sheetHead(entry ? 'Editar registro' : 'Registrar medidas')}<div class="sheet-body">
      <label class="field"><span>Data</span><input class="input" type="date" id="bdDate" value="${isoDate(e.t || Date.now())}" max="${isoDate(Date.now())}"></label>
      ${!h || !sx ? `<div class="card bd-who"><div class="small muted" style="margin-bottom:8px">Altura e sexo servem para calcular o IMC e estimar a gordura pelas medidas.</div>
        <div class="mini-grid" style="grid-template-columns:1fr 2fr;margin-top:0">
          <label><span>Altura (cm)</span><input class="input num" type="text" inputmode="decimal" id="bdAltura" value="${h ? fmt(h, 0) : ''}" placeholder="170"></label>
          <label><span>Sexo</span><div class="seg bd-sex" style="grid-template-columns:1fr 1fr">${[['f', 'Feminino'], ['m', 'Masculino']].map(([v, l]) => `<button class="${sx === v ? 'on' : ''}" data-act="bodySex" data-v="${v}">${l}</button>`).join('')}</div></label>
        </div></div>` : ''}
      <h2 class="section" style="margin-top:14px">Peso e composição</h2>
      <div class="mini-grid bd-grid">${BODY_FIELDS.filter(f => f[3] === 'comp').map(inp).join('')}</div>
      <h2 class="section">Medidas (cm)</h2>
      <div class="mini-grid bd-grid">${BODY_FIELDS.filter(f => f[3] === 'med').map(inp).join('')}</div>
      <details class="bd-how"><summary>Como medir</summary><ul>${BODY_FIELDS.filter(f => f[6]).map(f => `<li><b>${f[1]}:</b> ${esc(f[6])}</li>`).join('')}</ul>
        <p>Use fita métrica sem apertar a pele e meça sempre do mesmo lado.</p></details>
      <label class="field" style="margin-top:12px"><span>Observação</span><input class="input" id="bdNota" value="${esc(e.nota || '')}" placeholder="Ex.: depois de uma semana de dieta" maxlength="140"></label>
    </div>
    <div class="sheet-foot stack">
      <button class="btn primary block" data-act="bodySave" data-id="${entry ? entry.id : ''}">Salvar</button>
      ${entry ? `<button class="btn danger block" data-act="bodyDelete" data-id="${entry.id}">Excluir registro</button>` : ''}
    </div>`);
}

const BODY_ACTIONS = {
  bodyNew: () => {
    // Se já existe registro de hoje, edita esse
    const today = (S.body || []).find(e => e.t === startOfDay(Date.now()));
    bodyForm(today || null);
  },
  bodyEdit: el => bodyForm((S.body || []).find(e => e.id === el.dataset.id)),
  bodySex: el => { el.parentNode.querySelectorAll('button').forEach(b => b.classList.toggle('on', b === el)); },
  bodySave: el => {
    const vals = {};
    document.querySelectorAll('[data-bf]').forEach(i => { const v = num(i.value); if (v != null && v >= 0) vals[i.dataset.bf] = v; });
    const nota = ($('#bdNota').value || '').trim();
    if (!Object.keys(vals).length) { toast('Preencha pelo menos uma medida'); return; }
    const [y, mo, d] = ($('#bdDate').value || isoDate(Date.now())).split('-').map(Number);
    const t = new Date(y, mo - 1, d).getTime();
    // Altura e sexo (compartilhados com o assistente de treino)
    const p = { ...(S.profile || {}) };
    const alt = $('#bdAltura') && num($('#bdAltura').value);
    if (alt) p.altura = String(alt);
    const sx = document.querySelector('.bd-sex button.on');
    if (sx) p.sexo = sx.dataset.v;
    S.profile = p;
    S.body = S.body || [];
    const editing = el.dataset.id ? S.body.find(e => e.id === el.dataset.id) : null;
    const same = S.body.find(e => e.t === t && e !== editing);
    const base = { id: editing ? editing.id : uid(), t, ...vals };
    if (nota) base.nota = nota;
    if (same) {
      // Junta com o registro que já existe nesse dia
      Object.assign(same, vals); if (nota) same.nota = nota;
      if (editing) S.body = S.body.filter(e => e !== editing);
    } else if (editing) {
      const i = S.body.indexOf(editing); S.body[i] = base;
    } else S.body.push(base);
    save(); closeSheet(); toast('Medidas salvas');
    if (vals.peso != null && !BF[bodyMetric]) bodyMetric = 'peso';
    if (location.hash === '#/corpo') rerender(); else go('#/corpo');
  },
  bodyDelete: el => {
    const id = el.dataset.id;
    confirmSheet({
      title: 'Excluir registro?', text: 'As medidas deste dia saem dos gráficos.', ok: 'Excluir', danger: true,
      onOk: () => { S.body = (S.body || []).filter(e => e.id !== id); save(); rerender(); }
    });
  },
  bodyMetric: el => { bodyMetric = el.dataset.k; rerender(); },
  bodyRange: el => { bodyRange = el.dataset.v; rerender(); },
  bodyGoal: () => {
    const cur = S.bodyGoal && S.bodyGoal.peso;
    const v = prompt('Meta de peso (kg). Deixe vazio para remover.', cur ? fmt(num(cur)) : '');
    if (v == null) return;
    const n = num(v);
    S.bodyGoal = { ...(S.bodyGoal || {}), peso: n && n > 0 ? n : null };
    save(); rerender();
  }
};
