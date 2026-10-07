/* Ficha — registro de treinos (PWA, sem dependências)
 * Dados salvos localmente no aparelho (localStorage).
 */
'use strict';

/* ================= Utilidades ================= */
const KEY = 'ficha.v1';
const DAY = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const DAY_LONG = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
const MONTH = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
const WEEK_ORDER = [1, 2, 3, 4, 5, 6, 0]; // semana começando na segunda
const REST_OPTIONS = [0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300];

const $ = (s, r = document) => r.querySelector(s);
const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const norm = s => String(s).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
const num = v => {
  if (v === '' || v == null) return null;
  const n = parseFloat(String(v).replace(',', '.'));
  return Number.isFinite(n) ? n : null;
};
const fmt = (n, d = 1) => n == null ? '' : Number(n).toLocaleString('pt-BR', { maximumFractionDigits: d });
const fmtInt = n => Math.round(n || 0).toLocaleString('pt-BR');
const sum = a => a.reduce((x, y) => x + y, 0);
const max = a => a.length ? Math.max(...a) : 0;
const clone = o => JSON.parse(JSON.stringify(o));

function clock(sec) {
  sec = Math.max(0, Math.round(sec));
  const h = Math.floor(sec / 3600), m = Math.floor(sec % 3600 / 60), s = sec % 60;
  return h ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}` : `${m}:${String(s).padStart(2, '0')}`;
}
function fmtDur(ms) {
  const m = Math.round(ms / 60000), h = Math.floor(m / 60);
  return h ? `${h}h${String(m % 60).padStart(2, '0')}` : `${m} min`;
}
function fmtRest(s) { return !s ? 'sem descanso' : s < 60 ? `${s}s` : clock(s); }
function startOfDay(t) { const d = new Date(t); d.setHours(0, 0, 0, 0); return d.getTime(); }
function startOfWeek(t) { const d = new Date(startOfDay(t)); const off = (d.getDay() + 6) % 7; d.setDate(d.getDate() - off); return d.getTime(); }
function addDays(t, n) { const d = new Date(t); d.setDate(d.getDate() + n); return d.getTime(); }
function dateShort(t) { const d = new Date(t); return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`; }
function dateLong(t) { const d = new Date(t); return `${DAY_LONG[d.getDay()]}, ${d.getDate()} de ${MONTH[d.getMonth()]}`; }
function timeHM(t) { const d = new Date(t); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`; }
function relDay(t) {
  const diff = Math.round((startOfDay(Date.now()) - startOfDay(t)) / 864e5);
  if (diff === 0) return 'Hoje';
  if (diff === 1) return 'Ontem';
  if (diff < 7) return `Há ${diff} dias`;
  return dateShort(t);
}
function daysLabel(days) {
  if (!days || !days.length) return 'Sem dia fixo';
  if (days.length === 7) return 'Todos os dias';
  return WEEK_ORDER.filter(d => days.includes(d)).map(d => DAY[d]).join(' · ');
}

/* ================= Ícones ================= */
const I = {
  back: '<svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg>',
  plus: '<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>',
  check: '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  chev: '<svg class="chev" viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"/></svg>',
  up: '<svg viewBox="0 0 24 24"><path d="M6 15l6-6 6 6"/></svg>',
  down: '<svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg>',
  trash: '<svg viewBox="0 0 24 24"><path d="M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13M10 11v5.5M14 11v5.5"/></svg>',
  more: '<svg viewBox="0 0 24 24"><circle cx="5.5" cy="12" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="18.5" cy="12" r="1.2"/></svg>',
  search: '<svg viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5 20 20"/></svg>',
  play: '<svg viewBox="0 0 24 24"><path d="M8 5.5v13l10.5-6.5z"/></svg>',
  x: '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6 6 18"/></svg>',
  copy: '<svg viewBox="0 0 24 24"><rect x="8.5" y="8.5" width="11" height="11" rx="2"/><path d="M15.5 8.5V6a1.5 1.5 0 0 0-1.5-1.5H6A1.5 1.5 0 0 0 4.5 6v8A1.5 1.5 0 0 0 6 15.5h2.5"/></svg>',
  edit: '<svg viewBox="0 0 24 24"><path d="M4.5 19.5h4l10-10-4-4-10 10z"/><path d="M13 7l4 4"/></svg>',
  dumbbell: '<svg viewBox="0 0 24 24"><path d="M2.5 12h2M19.5 12h2M8 12h8"/><rect x="4.5" y="7.5" width="3.5" height="9" rx="1"/><rect x="16" y="7.5" width="3.5" height="9" rx="1"/></svg>',
  list: '<svg viewBox="0 0 24 24"><rect x="5" y="3.5" width="14" height="17" rx="2"/><path d="M8.5 9h7M8.5 13h7M8.5 17h4"/></svg>',
  chart: '<svg viewBox="0 0 24 24"><path d="M3.5 19.5h17"/><path d="M6 16l4-5 3.5 3 5-7"/></svg>',
  trophy: '<svg viewBox="0 0 24 24"><path d="M8 4.5h8v5a4 4 0 0 1-8 0zM8 6.5H5a3 3 0 0 0 3 4M16 6.5h3a3 3 0 0 1-3 4M12 13.5V17M8.5 20h7M10 17h4v3h-4z"/></svg>',
  video: '<svg viewBox="0 0 24 24"><rect x="3" y="5.5" width="18" height="13" rx="3"/><path d="M10.5 9.5v5l4-2.5z"/></svg>',
  expand: '<svg viewBox="0 0 24 24"><path d="M14 4.5h5.5V10M10 19.5H4.5V14M19.5 4.5 13.5 10.5M4.5 19.5l6-6"/></svg>',
  download: '<svg viewBox="0 0 24 24"><path d="M12 4.5v11M7 11l5 5 5-5M5 19.5h14"/></svg>',
  folder: '<svg viewBox="0 0 24 24"><path d="M3.5 7.5a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"/></svg>',
  sparkle: '<svg viewBox="0 0 24 24"><path d="M11 3.5l1.9 5 5 1.9-5 1.9-1.9 5-1.9-5-5-1.9 5-1.9z"/><path d="M18.5 14.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z"/></svg>',
  palette: '<svg viewBox="0 0 24 24"><path d="M12 3.5a8.5 8.5 0 0 0 0 17c1.2 0 1.8-.8 1.8-1.7 0-1.3-1.2-1.6-1.2-2.8 0-1 .8-1.7 1.8-1.7h2.1a4 4 0 0 0 4-4c0-3.8-3.8-6.8-8.5-6.8z"/><circle cx="7.8" cy="11" r="1"/><circle cx="10.5" cy="7.5" r="1"/><circle cx="15" cy="7.8" r="1"/></svg>'
};

/* ================= Estado ================= */
function blank() {
  return {
    v: 1, settings: { rest: 90, sound: true, theme: 'auto', accent: 'limao', glass: true },
    custom: [], programs: [], routines: [], sessions: [], active: null, videos: {}, profile: null
  };
}
// Dados antigos (sem programas): as fichas existentes viram o programa "Meu treino"
function migrate(d) {
  if (!Array.isArray(d.programs)) {
    d.programs = [];
    if ((d.routines || []).length) {
      const p = { id: uid(), name: 'Meu treino', active: true };
      d.programs.push(p);
      d.routines.forEach(r => { r.programId = p.id; });
    }
  }
  return d;
}
function load() {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) {
      const d = migrate(JSON.parse(raw));
      const b = blank();
      return { ...b, ...d, settings: { ...b.settings, ...(d.settings || {}) } };
    }
  } catch (e) { console.warn(e); }
  return blank();
}
let S = load();
function save() {
  try { localStorage.setItem(KEY, JSON.stringify(S)); }
  catch (e) { toast('Não foi possível salvar os dados'); }
}

/* ================= Exercícios ================= */
const EXMAP = new Map(BUILTIN_EXERCISES.map(e => [e.id, e]));
function allEx() { return BUILTIN_EXERCISES.concat(S.custom); }
function getEx(id) { return EXMAP.get(id) || S.custom.find(e => e.id === id) || null; }
function exName(id, fb) { const e = getEx(id); return e ? e.name : (fb || 'Exercício removido'); }
function exKind(id, fb) { const e = getEx(id); return e ? e.kind : (fb || 'w'); }

/* ================= Execução: foto, músculos e vídeo ================= */
const IMG_CACHE = 'ficha-img-v1'; // mesmo nome usado em sw.js
const isUrl = s => /^https?:\/\/\S+$/i.test(String(s || ''));
const ytUrl = q => 'https://www.youtube.com/results?search_query=' + encodeURIComponent(q);

// Miniatura (posição inicial). Se a foto não carregar (offline), fica o ícone.
function thumb(ex) {
  return `<span class="thumb">${I.dumbbell}${ex && ex.img ? `<img src="img/thumb/${ex.img}.webp" alt="" loading="lazy" decoding="async" onerror="this.remove()">` : ''}</span>`;
}
// Foto animada alternando posição inicial e final
function demo(ex) {
  if (!ex || !ex.img) return `<div class="demo off"><div class="demo-off">${I.dumbbell}<span>Sem foto para este exercício.<br>Veja um vídeo abaixo.</span></div></div>`;
  const src = `img/ex/${ex.img}.webp`;
  return `<div class="demo" role="img" aria-label="Execução: posição inicial e final">
    <div class="demo-off">${I.dumbbell}<span>A foto aparece quando houver internet.</span></div>
    <img src="${src}" alt="" decoding="async" onerror="this.parentNode.classList.add('off')"><img class="f2" src="${src}" alt="" decoding="async">
    <b class="demo-step s1">1 · Início</b><b class="demo-step s2">2 · Fim</b></div>`;
}
function musclesHTML(ex) {
  if (!ex || !(ex.primary || []).length) return '';
  const tag = (c, p) => `<span class="mtag ${p ? 'p' : ''}">${esc(MUSCLES[c] || c)}</span>`;
  return `<div class="muscles">${ex.primary.map(c => tag(c, true)).join('')}${(ex.secondary || []).map(c => tag(c, false)).join('')}</div>`;
}
function videoActions(id) {
  const ex = getEx(id), name = ex ? ex.name : exName(id), mine = S.videos[id];
  return `<div class="video-actions">
    ${isUrl(mine) ? `<a class="btn primary block" href="${esc(mine)}" target="_blank" rel="noopener">${I.play}Meu vídeo</a>` : ''}
    <a class="btn block" href="${ytUrl('como fazer ' + name)}" target="_blank" rel="noopener">${I.video}Ver vídeos da execução</a>
    <div class="video-links">
      ${ex && ex.en ? `<a class="link-btn" href="${ytUrl(ex.en + ' exercise')}" target="_blank" rel="noopener">Buscar em inglês</a>` : '<span></span>'}
      <button class="link-btn" data-act="setVideo" data-id="${id}">${mine ? 'Trocar meu vídeo' : 'Adicionar meu vídeo'}</button>
    </div></div>`;
}
function openHowTo(id, opts = {}) {
  const ex = getEx(id);
  const evo = opts.evo && location.hash !== `#/exercicio/${id}`
    ? `<a class="btn block" href="#/exercicio/${id}" data-act="closeSheet" style="margin-top:8px">${I.chart}Evolução e histórico</a>` : '';
  openSheet(`${sheetHead(ex ? ex.name : exName(id))}
    <div class="sheet-body">
      ${demo(ex)}
      ${ex ? `<div class="small muted" style="margin:10px 2px 0">${esc(ex.group)} · ${esc(ex.equip)}</div>` : ''}
      ${musclesHTML(ex)}
      ${videoActions(id)}${evo}
    </div>${opts.foot || ''}`, { howTo: opts });
}

// Baixa todas as fotos para o cache do app (uso offline)
let photoDl = null;
async function downloadPhotos() {
  if (photoDl || !('caches' in window)) return;
  const urls = BUILTIN_EXERCISES.filter(e => e.img).flatMap(e => [`img/thumb/${e.img}.webp`, `img/ex/${e.img}.webp`]);
  photoDl = { done: 0, total: urls.length, fail: 0 };
  const paint = () => { const el = $('[data-photo-status]'); if (el) el.textContent = photoStatus(); };
  paint();
  try {
    const c = await caches.open(IMG_CACHE);
    for (let i = 0; i < urls.length; i += 8) {
      await Promise.all(urls.slice(i, i + 8).map(async u => {
        try {
          if (!(await c.match(u))) { const r = await fetch(u); if (r.ok) await c.put(u, r); else photoDl.fail++; }
        } catch (e) { photoDl.fail++; }
        photoDl.done++;
      }));
      paint();
    }
    toast(photoDl.fail ? `Fotos baixadas, ${photoDl.fail} falharam — tente de novo` : 'Fotos salvas para usar offline');
  } catch (e) { toast('Não foi possível salvar as fotos'); }
  photoDl = null; paint();
}
function photoStatus() {
  if (photoDl) return `Baixando… ${Math.round(photoDl.done / photoDl.total * 100)}%`;
  return `${BUILTIN_EXERCISES.filter(e => e.img).length} exercícios · cerca de 19 MB`;
}

const isWork = s => !s.warm;
function setVol(kind, s) { return kind === 'w' && isWork(s) ? (s.a || 0) * (s.b || 0) : 0; }
function e1rm(w, r) { if (!w || !r) return 0; return r === 1 ? w : w * (1 + r / 30); }
function sessVolume(sess) { return sum(sess.exercises.map(ex => sum(ex.sets.map(s => setVol(ex.kind, s))))); }
function sessSetCount(sess) { return sum(sess.exercises.map(ex => ex.sets.length)); }

const METRICS = {
  w: [
    { id: 'max', label: 'Carga máxima', unit: 'kg', f: ss => max(ss.map(s => s.a || 0)) },
    { id: 'rm', label: '1RM estimado', unit: 'kg', f: ss => max(ss.map(s => e1rm(s.a, s.b))) },
    { id: 'vol', label: 'Volume', unit: 'kg', f: ss => sum(ss.map(s => (s.a || 0) * (s.b || 0))) }
  ],
  bw: [
    { id: 'reps', label: 'Reps máximas', unit: 'reps', f: ss => max(ss.map(s => s.b || 0)) },
    { id: 'tot', label: 'Total de reps', unit: 'reps', f: ss => sum(ss.map(s => s.b || 0)) },
    { id: 'extra', label: 'Carga extra', unit: 'kg', f: ss => max(ss.map(s => s.a || 0)) }
  ],
  s: [
    { id: 'max', label: 'Tempo máximo', unit: 's', f: ss => max(ss.map(s => s.a || 0)) },
    { id: 'tot', label: 'Tempo total', unit: 's', f: ss => sum(ss.map(s => s.a || 0)) }
  ],
  c: [
    { id: 'min', label: 'Tempo', unit: 'min', f: ss => sum(ss.map(s => s.a || 0)) },
    { id: 'km', label: 'Distância', unit: 'km', f: ss => sum(ss.map(s => s.b || 0)) }
  ]
};

// Histórico de um exercício, do mais antigo para o mais recente (só séries válidas)
function exHistory(exId) {
  const out = [];
  for (const sess of S.sessions) {
    const exs = sess.exercises.filter(e => e.exId === exId);
    if (!exs.length) continue;
    out.push({ sess, kind: exs[0].kind, sets: exs.flatMap(e => e.sets) });
  }
  return out.sort((a, b) => a.sess.start - b.sess.start);
}
function lastSets(exId) {
  const h = exHistory(exId);
  return h.length ? h[h.length - 1].sets.filter(isWork) : [];
}
function fmtSet(kind, s) {
  if (!s) return '—';
  if (kind === 'w') return `${fmt(s.a ?? 0)} × ${fmt(s.b ?? 0, 0)}`;
  if (kind === 'bw') return s.a ? `+${fmt(s.a)} × ${fmt(s.b ?? 0, 0)}` : `${fmt(s.b ?? 0, 0)} reps`;
  if (kind === 's') return `${fmt(s.a ?? 0, 0)}s`;
  if (kind === 'c') return `${fmt(s.a ?? 0)} min${s.b ? ` · ${fmt(s.b, 2)} km` : ''}`;
  return '';
}

/* ================= Programas ================= */
// Um programa agrupa fichas (ex.: "Meu treino" → Push, Pull, Legs). Fichas sem programa são "avulsas".
const tpls = () => (typeof TEMPLATES === 'undefined' ? [] : TEMPLATES);
function programOf(r) { return r && r.programId ? S.programs.find(p => p.id === r.programId) || null : null; }
function progRoutines(pid) { return S.routines.filter(r => r.programId === pid); }
const isActiveProg = p => p.active !== false;
// Fichas que valem para a tela Hoje: avulsas e as de programas ativos
function isScheduled(r) { const p = programOf(r); return !p || isActiveProg(p); }
// Próxima ficha na ordem do programa, a partir do último treino feito nele
function nextInProgram(p) {
  const rs = progRoutines(p.id);
  if (!rs.length) return null;
  const ids = new Set(rs.map(r => r.id));
  const last = S.sessions.find(s => ids.has(s.routineId));
  if (!last) return rs[0];
  return rs[(rs.findIndex(r => r.id === last.routineId) + 1) % rs.length];
}
function moveInProgram(id, d) {
  const r = S.routines.find(x => x.id === id), rs = progRoutines(r.programId), o = rs[rs.indexOf(r) + d];
  if (!o) return;
  const a = S.routines.indexOf(r), b = S.routines.indexOf(o);
  [S.routines[a], S.routines[b]] = [S.routines[b], S.routines[a]];
}
const letter = i => String.fromCharCode(65 + (i % 26));

// Folha de confirmação ao criar um programa a partir de um modelo ou do assistente
let pendingProgram = null;
function openUseProgram(def) {
  pendingProgram = def;
  const hasDays = def.routines.some(r => r.days.length);
  const othersActive = S.programs.filter(isActiveProg).length;
  const n = def.routines.length;
  openSheet(`${sheetHead('Usar “' + def.name + '”')}<div class="sheet-body">
    <p class="muted" style="margin:0 0 12px">Cria o programa com ${n} ficha${n > 1 ? 's' : ''}. Depois você pode trocar exercícios, séries e dias.</p>
    <div class="list">
      ${hasDays ? `<label class="row"><div class="grow"><div class="name">Usar os dias sugeridos</div>
        <div class="sub wrap">${def.routines.map(r => `${esc(r.name.split(' — ')[0])}: ${r.days.length ? daysLabel(r.days) : 'livre'}`).join(' · ')}</div></div>
        <span class="switch"><input type="checkbox" id="tplDays" checked><i></i></span></label>` : ''}
      ${othersActive ? `<label class="row"><div class="grow"><div class="name">Pausar os outros programas</div>
        <div class="sub wrap">A tela Hoje passa a mostrar só este programa</div></div>
        <span class="switch"><input type="checkbox" id="tplOnly" checked><i></i></span></label>` : ''}
    </div>
    ${hasDays ? '<p class="small muted" style="margin:10px 4px 0">Sem dias fixos, o app sugere a próxima ficha na ordem a cada treino.</p>' : ''}
  </div>
  <div class="sheet-foot"><button class="btn primary block" data-act="createPending">Criar programa</button></div>`);
}

/* ================= Aparência ================= */
// Cores de destaque: [destaque, texto sobre o destaque, destaque usado como texto] para tema escuro e claro
const ACCENTS = [
  { id: 'limao', name: 'Limão', dark: ['#C8F250', '#12160A', '#C8F250'], light: ['#B6E63A', '#15190A', '#4B7A00'] },
  { id: 'verde', name: 'Verde', dark: ['#34D399', '#03200F', '#4ADE9F'], light: ['#22B45E', '#03200F', '#15803D'] },
  { id: 'turquesa', name: 'Turquesa', dark: ['#2DD4BF', '#032420', '#4FE0CD'], light: ['#14B8A6', '#022B26', '#0B7F72'] },
  { id: 'azul', name: 'Azul', dark: ['#4DA3FF', '#04111F', '#6CB4FF'], light: ['#0A84FF', '#FFFFFF', '#0066CC'] },
  { id: 'roxo', name: 'Roxo', dark: ['#A78BFA', '#140B2E', '#B9A2FF'], light: ['#7C3AED', '#FFFFFF', '#6D28D9'] },
  { id: 'rosa', name: 'Rosa', dark: ['#FF6FAE', '#2A0716', '#FF8ABF'], light: ['#E83E8C', '#FFFFFF', '#C2185B'] },
  { id: 'vermelho', name: 'Vermelho', dark: ['#FF5A52', '#2A0505', '#FF7A72'], light: ['#E5322B', '#FFFFFF', '#C2241D'] },
  { id: 'laranja', name: 'Laranja', dark: ['#FF9F43', '#2A1400', '#FFB066'], light: ['#F28C1C', '#241200', '#B85C00'] },
  { id: 'amarelo', name: 'Amarelo', dark: ['#FFD43B', '#241C00', '#FFD43B'], light: ['#F5C400', '#1F1800', '#8A6D00'] },
  { id: 'mono', name: 'Grafite', dark: ['#F2F3EF', '#0E1013', '#F2F3EF'], light: ['#15181C', '#FFFFFF', '#15181C'] }
];
const THEMES = [['auto', 'Automático'], ['light', 'Claro'], ['dark', 'Escuro'], ['black', 'Preto']];
const lightMQ = matchMedia('(prefers-color-scheme: light)');
function schemeNow() {
  const t = S.settings.theme;
  return t === 'light' || (t === 'auto' && lightMQ.matches) ? 'light' : 'dark';
}
function accentVars(scheme) {
  const a = (ACCENTS.find(x => x.id === S.settings.accent) || ACCENTS[0])[scheme];
  const [r, g, b] = [1, 3, 5].map(i => parseInt(a[0].slice(i, i + 2), 16));
  return { '--accent': a[0], '--accent-ink': a[1], '--accent-text': a[2], '--accent-soft': `rgba(${r}, ${g}, ${b}, .16)` };
}
function applyLook() {
  const root = document.documentElement, scheme = schemeNow();
  root.dataset.scheme = scheme;
  root.toggleAttribute('data-black', S.settings.theme === 'black');
  root.toggleAttribute('data-glass', !!S.settings.glass);
  for (const [k, v] of Object.entries(accentVars(scheme))) root.style.setProperty(k, v);
  const bg = getComputedStyle(root).getPropertyValue('--bg').trim();
  document.querySelectorAll('meta[name="theme-color"]').forEach(m => { m.removeAttribute('media'); m.content = bg; });
  // Cópia leve para o index.html aplicar o tema antes do app carregar (evita piscar)
  try {
    localStorage.setItem('ficha.look', JSON.stringify({
      theme: S.settings.theme, glass: !!S.settings.glass, dark: accentVars('dark'), light: accentVars('light')
    }));
  } catch (e) { /* ignora */ }
}
lightMQ.addEventListener && lightMQ.addEventListener('change', () => { if (S.settings.theme === 'auto') applyLook(); });

/* ================= UI: toast, sheet, som ================= */
let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg; t.hidden = false;
  t.style.animation = 'none'; void t.offsetWidth; t.style.animation = '';
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { t.hidden = true; }, 2400);
}

let sheetCtx = null;
function openSheet(html, ctx = null) {
  sheetCtx = ctx;
  const sh = $('#sheet');
  sh.innerHTML = '<div class="grab"></div>' + html;
  sh.hidden = false; $('#sheet-backdrop').hidden = false;
  document.documentElement.style.overflow = 'hidden';
}
function closeSheet() {
  sheetCtx = null;
  $('#sheet').hidden = true; $('#sheet-backdrop').hidden = true;
  $('#sheet').innerHTML = '';
  document.documentElement.style.overflow = '';
}
function sheetHead(title, right = '') {
  return `<div class="sheet-head"><h3>${esc(title)}</h3>${right}<button class="icon-btn" data-act="closeSheet" aria-label="Fechar">${I.x}</button></div>`;
}
function confirmSheet({ title, text = '', ok = 'Confirmar', danger = false, onOk, extra = '' }) {
  openSheet(`${sheetHead(title)}
    <div class="sheet-body">${text ? `<p class="muted" style="margin:0 0 14px">${text}</p>` : ''}${extra}</div>
    <div class="sheet-foot stack">
      <button class="btn block ${danger ? 'danger' : 'primary'}" data-act="sheetOk">${esc(ok)}</button>
      <button class="btn block" data-act="closeSheet">Cancelar</button>
    </div>`, { onOk });
}

let audioCtx;
function unlockAudio() {
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
  } catch (e) { /* sem áudio */ }
}
function beep() {
  if (!S.settings.sound || !audioCtx) return;
  try {
    const t0 = audioCtx.currentTime;
    [0, 0.22, 0.44].forEach((d, i) => {
      const o = audioCtx.createOscillator(), g = audioCtx.createGain();
      o.type = 'sine'; o.frequency.value = i === 2 ? 1320 : 880;
      g.gain.setValueAtTime(0.0001, t0 + d);
      g.gain.exponentialRampToValueAtTime(0.35, t0 + d + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t0 + d + 0.18);
      o.connect(g).connect(audioCtx.destination);
      o.start(t0 + d); o.stop(t0 + d + 0.2);
    });
  } catch (e) { /* ignora */ }
  if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
}

/* ================= Roteamento ================= */
const routes = [
  [/^#\/hoje$/, viewHoje, 'hoje'],
  [/^#\/fichas$/, viewFichas, 'fichas'],
  [/^#\/ficha\/([\w-]+)$/, viewFicha, 'fichas'],
  [/^#\/programa\/([\w-]+)$/, viewPrograma, 'fichas'],
  [/^#\/modelos$/, viewModelos, 'fichas'],
  [/^#\/modelo\/([\w-]+)$/, viewModelo, 'fichas'],
  [/^#\/assistente$/, () => typeof viewAssistente === 'function' ? viewAssistente() : viewUpdating(), 'fichas'],
  [/^#\/exercicios$/, viewExercicios, 'exercicios'],
  [/^#\/exercicio\/([\w-]+)$/, viewExercicio, 'exercicios'],
  [/^#\/historico$/, viewHistorico, 'historico'],
  [/^#\/sessao\/([\w-]+)$/, viewSessao, 'historico'],
  [/^#\/treino$/, viewTreino, null],
  [/^#\/ajustes$/, viewAjustes, 'ajustes']
];
let current = null, currentTab = null, lastHash = null, afterRender = null;

function route() {
  const h = location.hash || '#/hoje';
  for (const [re, fn, tab] of routes) {
    const m = h.match(re);
    if (m) {
      current = () => fn(...m.slice(1));
      currentTab = tab;
      const changed = h !== lastHash;
      lastHash = h;
      render();
      if (changed) window.scrollTo(0, 0);
      return;
    }
  }
  location.replace('#/hoje');
}
function go(hash) { if (location.hash === hash) route(); else location.hash = hash; }
function render() {
  const html = current();
  if (html == null) return; // a view redirecionou
  $('#view').innerHTML = html;
  document.body.classList.toggle('in-workout', location.hash === '#/treino');
  document.querySelectorAll('#tabs a').forEach(a => a.classList.toggle('on', a.dataset.tab === currentTab));
  renderDock();
  if (afterRender) { const f = afterRender; afterRender = null; f(); }
}
function rerender() { const y = window.scrollY; render(); window.scrollTo(0, y); }

function topBar({ back, right = '' } = {}) {
  return `<div class="top">${back ? `<a class="icon-btn" href="${back}" aria-label="Voltar">${I.back}</a>` : ''}<div class="spacer"></div>${right}</div>`;
}
function emptyState(icon, title, text, actions = '') {
  return `<div class="empty">${icon}<b>${esc(title)}</b><div>${text}</div>${actions ? `<div class="stack" style="margin-top:18px">${actions}</div>` : ''}</div>`;
}

// Arquivo novo ainda não carregado (app no meio de uma atualização)
function viewUpdating() {
  return topBar({ back: '#/fichas' }) + `<div class="card">${emptyState(I.download, 'Atualizando o app', 'Esta função chegou numa versão nova. Recarregue para usar.',
    `<button class="btn primary block" data-act="checkUpdate">Recarregar</button>`)}</div>`;
}

/* ================= Tela: Hoje ================= */
function sessionsInRange(a, b) { return S.sessions.filter(s => s.start >= a && s.start < b); }

function streakWeeks() {
  let wk = startOfWeek(Date.now()), n = 0;
  if (!sessionsInRange(wk, addDays(wk, 7)).length) wk = addDays(wk, -7);
  while (sessionsInRange(wk, addDays(wk, 7)).length) { n++; wk = addDays(wk, -7); }
  return n;
}

function viewHoje() {
  const now = Date.now(), dow = new Date().getDay();
  const wk = startOfWeek(now);
  const sched = S.routines.filter(isScheduled);
  const today = sched.filter(r => r.days.includes(dow));
  // Programas ativos sem dia fixo: sugere a próxima ficha na ordem
  const seq = S.programs.filter(isActiveProg)
    .map(p => ({ p, rs: progRoutines(p.id) }))
    .filter(x => x.rs.length && x.rs.every(r => !r.days.length))
    .map(x => ({ p: x.p, r: nextInProgram(x.p) }));
  const heroIds = new Set([...today.map(r => r.id), ...seq.map(x => x.r.id)]);
  const others = sched.filter(r => !heroIds.has(r.id));
  const doneToday = sessionsInRange(startOfDay(now), addDays(startOfDay(now), 1));

  const week = [0, 1, 2, 3, 4, 5, 6].map(i => {
    const t = addDays(wk, i), d = new Date(t);
    const plan = sched.some(r => r.days.includes(d.getDay()));
    const done = sessionsInRange(t, addDays(t, 1)).length > 0;
    return `<div class="d ${plan ? 'plan' : ''} ${done ? 'done' : ''} ${t === startOfDay(now) ? 'today' : ''}">
      <small>${DAY[d.getDay()]}</small><b class="num">${d.getDate()}</b><i></i></div>`;
  }).join('');

  const thisWeek = sessionsInRange(wk, addDays(wk, 7));
  const monthStart = new Date(); monthStart.setDate(1); monthStart.setHours(0, 0, 0, 0);
  const thisMonth = S.sessions.filter(s => s.start >= monthStart.getTime()).length;

  let title;
  if (S.active) title = 'Treino em andamento';
  else if (!S.routines.length) title = 'Vamos montar seu treino';
  else if (doneToday.length) title = 'Treino feito hoje';
  else if (today.length || seq.length) title = 'Dia de treinar';
  else title = 'Dia de descanso';

  let html = `<div class="eyebrow">${dateLong(now)}</div><h1 class="title">${title}</h1>
    <div class="week">${week}</div>`;

  if (S.active) {
    const a = S.active, total = sum(a.exercises.map(e => e.sets.length)), done = sum(a.exercises.map(e => e.sets.filter(s => s.done).length));
    html += `<div class="card accent">
      <div class="small muted" style="font-weight:700">EM ANDAMENTO · <span class="num" data-elapsed="${a.start}">${clock((now - a.start) / 1000)}</span></div>
      <div class="hero-title">${esc(a.name)}</div>
      <div class="small muted" style="margin-bottom:14px">${done} de ${total} séries concluídas</div>
      <a class="btn block" href="#/treino">Continuar treino</a></div>`;
  } else if (!S.routines.length) {
    html += `<div class="card">${emptyState(I.list, 'Nenhuma ficha ainda',
      'Crie suas fichas de treino escolhendo os exercícios de cada dia, ou comece com um programa pronto (PPL, Upper/Lower, ABC…) e ajuste do seu jeito.',
      `<a class="btn primary block" href="#/assistente">${I.sparkle}Montar meu treino</a>
       <a class="btn block" href="#/modelos">${I.list}Ver modelos prontos</a>
       <button class="btn block" data-act="newRoutine">${I.plus}Criar minha ficha</button>`)}</div>`;
  } else {
    for (const r of today) {
      const p = programOf(r);
      html += heroRoutine(r, doneToday.some(s => s.routineId === r.id), `FICHA DE HOJE${p ? ' · ' + p.name.toUpperCase() : ''}`);
    }
    for (const { p, r } of seq) html += heroRoutine(r, false, `PRÓXIMA FICHA · ${p.name.toUpperCase()}`);
  }

  if (S.sessions.length || S.routines.length) {
    html += `<div class="stats">
      <div class="stat"><b class="num">${thisWeek.length}</b><span>nesta semana</span></div>
      <div class="stat"><b class="num">${thisMonth}</b><span>neste mês</span></div>
      <div class="stat"><b class="num">${streakWeeks()}</b><span>semanas seguidas</span></div></div>`;
  }

  if (!S.active && S.routines.length) {
    if (others.length) {
      html += `<h2 class="section">${today.length || seq.length ? 'Outras fichas' : 'Escolha uma ficha'}</h2><div class="list">` +
        others.map(r => {
          const p = programOf(r);
          return `<button class="row" data-act="startRoutine" data-id="${r.id}">
          <div class="grow"><div class="name">${esc(r.name || 'Sem nome')}</div><div class="sub">${p ? esc(p.name) + ' · ' : ''}${daysLabel(r.days)} · ${r.items.length} exercícios</div></div>
          <span class="badge accent">Iniciar</span></button>`;
        }).join('') + '</div>';
    }
    html += `<div class="btn-row" style="margin-top:12px"><button class="btn" data-act="startEmpty">${I.plus}Treino livre</button><a class="btn" href="#/fichas">${I.list}Ver fichas</a></div>`;
  }

  const last = S.sessions[0];
  if (last) {
    html += `<h2 class="section">Último treino</h2><div class="list">
      <a class="row" href="#/sessao/${last.id}"><div class="grow"><div class="name">${esc(last.name)}</div>
      <div class="sub">${relDay(last.start)} · ${fmtDur(last.end - last.start)} · ${sessSetCount(last)} séries${sessVolume(last) ? ` · ${fmtInt(sessVolume(last))} kg` : ''}</div></div>${I.chev}</a></div>`;
  }
  return html;
}

function heroRoutine(r, done, label) {
  const items = r.items.slice(0, 6).map(it => `<li><span>${esc(exName(it.exId))}</span><span class="num">${it.sets} × ${esc(it.reps || '—')}</span></li>`).join('');
  const n = r.items.length - 6;
  const more = n > 0 ? `<li><span>+ ${n} exercício${n > 1 ? 's' : ''}</span><span></span></li>` : '';
  return `<div class="card accent">
    <div class="small muted" style="font-weight:700">${done ? 'CONCLUÍDO HOJE ✓' : esc(label)}</div>
    <div class="hero-title">${esc(r.name || 'Sem nome')}</div>
    ${r.items.length ? `<ul class="hero-list">${items}${more}</ul>` : '<p class="small muted">Ficha vazia — adicione exercícios.</p>'}
    <div class="btn-row">
      ${r.items.length ? `<button class="btn" data-act="startRoutine" data-id="${r.id}">${I.play}${done ? 'Treinar de novo' : 'Iniciar treino'}</button>` : ''}
      <a class="btn" href="#/ficha/${r.id}" style="flex:0 0 auto" aria-label="Editar ficha">${I.edit}</a>
    </div></div>`;
}

/* ================= Tela: Fichas ================= */
function routineRow(r, i) {
  return `<a class="row" href="#/ficha/${r.id}">
    <div class="dot ${isScheduled(r) && r.days.includes(new Date().getDay()) ? 'on' : ''}">${letter(i)}</div>
    <div class="grow"><div class="name">${esc(r.name || 'Sem nome')}</div><div class="sub">${daysLabel(r.days)} · ${r.items.length} exercícios</div></div>${I.chev}</a>`;
}
function viewFichas() {
  let html = topBar({ right: `<button class="link-btn" data-act="newMenu">${I.plus.replace('<svg', '<svg class="inline-ic"')}Novo</button>` }) + `<h1 class="title">Fichas</h1>
    <a class="tpl-banner" href="#/assistente"><span class="tpl-ic">${I.sparkle}</span><div class="grow"><b>Montar meu treino</b><span>Responda algumas perguntas e escolha entre 3 opções</span></div>${I.chev}</a>
    <a class="tpl-banner alt" href="#/modelos"><span class="tpl-ic">${I.list}</span><div class="grow"><b>Modelos prontos</b><span>PPL, Upper/Lower, ABC, ABCDE, em casa e mais</span></div>${I.chev}</a>`;
  if (!S.routines.length && !S.programs.length) {
    return html + `<div class="card">${emptyState(I.list, 'Nenhuma ficha ainda',
      'Uma <b>ficha</b> é a lista de exercícios de um dia de treino. Um <b>programa</b> agrupa várias fichas — por exemplo “Meu treino” com Push, Pull e Legs.',
      `<button class="btn primary block" data-act="newProgram">${I.folder}Criar programa</button>
       <button class="btn block" data-act="newRoutine">${I.plus}Criar ficha avulsa</button>`)}</div>`;
  }
  if (S.programs.length) {
    html += '<h2 class="section">Programas</h2><div class="list">' + S.programs.map(p => {
      const rs = progRoutines(p.id), on = isActiveProg(p);
      return `<a class="row" href="#/programa/${p.id}"><div class="dot ${on ? 'on' : ''}">${I.folder}</div>
        <div class="grow"><div class="name">${esc(p.name || 'Sem nome')}</div>
        <div class="sub">${rs.length ? rs.map(r => esc(r.name || 'Sem nome')).join(' · ') : 'Nenhuma ficha'}</div></div>
        ${on ? '' : '<span class="badge">Pausado</span>'}${I.chev}</a>`;
    }).join('') + '</div>';
  }
  const loose = S.routines.filter(r => !programOf(r));
  if (loose.length) {
    html += `<h2 class="section">${S.programs.length ? 'Fichas avulsas' : 'Fichas'}</h2><div class="list">` + loose.map(routineRow).join('') + '</div>';
  }
  html += `<p class="small muted" style="text-align:center;margin-top:14px">Toque num programa para ver as fichas dele. Use “Novo” para criar programas e fichas.</p>`;
  return html;
}

/* ================= Tela: Programa ================= */
function viewPrograma(id) {
  const p = S.programs.find(x => x.id === id);
  if (!p) { location.replace('#/fichas'); return null; }
  const rs = progRoutines(id), on = isActiveProg(p), next = nextInProgram(p), fixed = rs.some(r => r.days.length);
  let html = topBar({ back: '#/fichas', right: `<button class="icon-btn" data-act="programMenu" data-id="${id}" aria-label="Mais opções">${I.more}</button>` }) + `
    <div class="eyebrow">Programa</div>
    <input class="title-input" value="${esc(p.name)}" placeholder="Nome do programa" data-pname maxlength="60">
    <div class="list"><label class="row"><div class="grow"><div class="name">Programa ativo</div>
      <div class="sub">${on ? 'As fichas aparecem na tela Hoje' : 'Pausado — não aparece na tela Hoje'}</div></div>
      <span class="switch"><input type="checkbox" data-pactive ${on ? 'checked' : ''}><i></i></span></label></div>`;
  if (next && next.items.length) {
    html += `<button class="btn primary block" style="margin-top:12px" data-act="startRoutine" data-id="${next.id}">${I.play}<span class="ellip">Iniciar próxima: ${esc(next.name || 'Sem nome')}</span></button>`;
  }
  html += `<h2 class="section">Fichas <span class="muted small" style="text-transform:none;letter-spacing:0">${rs.length}</span></h2>`;
  if (rs.length) {
    html += '<div class="list">' + rs.map((r, i) => `<div class="row prow">
      <a class="plink" href="#/ficha/${r.id}"><div class="dot ${next && next.id === r.id ? 'on' : ''}">${letter(i)}</div>
        <div class="grow"><div class="name">${esc(r.name || 'Sem nome')}</div><div class="sub">${daysLabel(r.days)} · ${r.items.length} exercícios</div></div></a>
      <button class="icon-btn" data-act="progUp" data-id="${r.id}" aria-label="Subir" ${i === 0 ? 'disabled style="opacity:.3"' : ''}>${I.up}</button>
      <button class="icon-btn" data-act="progDown" data-id="${r.id}" aria-label="Descer" ${i === rs.length - 1 ? 'disabled style="opacity:.3"' : ''}>${I.down}</button>
    </div>`).join('') + '</div>';
  } else {
    html += `<div class="card">${emptyState(I.list, 'Programa vazio', 'Crie as fichas deste programa — por exemplo Push, Pull e Legs, ou A, B e C.')}</div>`;
  }
  const loose = S.routines.filter(r => !programOf(r));
  html += `<div class="stack" style="margin-top:14px">
      <button class="btn block" data-act="newRoutine" data-prog="${id}">${I.plus}Nova ficha neste programa</button>
      ${loose.length ? `<button class="btn block" data-act="moveIntoProgram" data-id="${id}">${I.folder}Trazer fichas avulsas</button>` : ''}
    </div>
    <p class="small muted" style="margin:14px 4px 0;line-height:1.5">${fixed
      ? 'As fichas aparecem na tela Hoje nos dias marcados. Para treinar em sequência (A → B → C…) sem dia fixo, desmarque os dias de todas as fichas.'
      : 'Sem dias fixos: a tela Hoje sugere a próxima ficha na ordem (A → B → C…), seguindo o último treino feito. Marque dias nas fichas se preferir dias fixos.'}</p>`;
  return html;
}

/* ================= Tela: Modelos prontos ================= */
let tplFilter = '';
const TPL_FILTERS = [['', 'Todos'], ['Iniciante', 'Iniciante'], ['Intermediário', 'Intermediário'], ['Avançado', 'Avançado'], ['casa', 'Em casa']];
function viewModelos() {
  const list = tpls().filter(t => !tplFilter || t.level === tplFilter || t.place === tplFilter);
  return topBar({ back: '#/fichas' }) + `<h1 class="title">Modelos prontos</h1>
    <p class="muted" style="margin:-8px 2px 14px">Escolha um programa, veja as fichas e adicione aos seus. Depois dá para mudar tudo.</p>
    <div class="chips">${TPL_FILTERS.map(([v, l]) => `<button class="chip ${tplFilter === v ? 'on' : ''}" data-act="tplFilter" data-v="${v}">${l}</button>`).join('')}</div>
    ${list.map(t => `<a class="card tpl" href="#/modelo/${t.id}">
      <div class="tpl-top"><b>${esc(t.name)}</b>${I.chev}</div>
      <div class="tpl-badges"><span class="badge accent">${esc(t.level)}</span><span class="badge">${esc(t.freq)}</span>${t.place === 'casa' ? '<span class="badge">Em casa</span>' : ''}</div>
      <p>${esc(t.desc)}</p>
      <div class="tpl-routines">${t.routines.map((r, i) => `<span><i>${letter(i)}</i>${esc(r.name)}</span>`).join('')}</div>
    </a>`).join('') || `<div class="card">${emptyState(I.search, 'Nenhum modelo', 'Tente outro filtro.')}</div>`}`;
}
function viewModelo(id) {
  const t = tpls().find(x => x.id === id);
  if (!t) { location.replace('#/modelos'); return null; }
  return topBar({ back: '#/modelos' }) + `<div class="eyebrow">${esc(t.level)} · ${esc(t.freq)}</div><h1 class="title">${esc(t.name)}</h1>
    <p class="muted" style="margin:-6px 2px 4px;line-height:1.5">${esc(t.desc)}</p>
    ${t.routines.map((r, i) => `<h2 class="section"><span>${letter(i)} · ${esc(r.name)}</span><span class="small" style="text-transform:none;letter-spacing:0">${r.days.length ? daysLabel(r.days) : 'Sem dia fixo'}</span></h2>
      <div class="list">${r.items.map(([exId, sets, reps]) => {
        const ex = getEx(exId), k = exKind(exId);
        return `<button class="row" data-act="howTo" data-id="${exId}">${thumb(ex)}<div class="grow"><div class="name">${esc(exName(exId))}</div>
          <div class="sub num">${sets} × ${esc(reps)}${k === 's' ? ' s' : k === 'c' ? ' min' : ''}</div></div>${I.chev}</button>`;
      }).join('')}</div>`).join('')}
    <div class="tpl-cta"><button class="btn primary block" data-act="useTpl" data-id="${t.id}">${I.plus}Usar este programa</button></div>`;
}

function viewFicha(id) {
  const r = S.routines.find(x => x.id === id);
  if (!r) { location.replace('#/fichas'); return null; }
  const prog = programOf(r);
  const days = WEEK_ORDER.map(d => `<button class="chip ${r.days.includes(d) ? 'on' : ''}" data-act="toggleDay" data-d="${d}">${DAY[d]}</button>`).join('');
  const items = r.items.map((it, i) => {
    const ex = getEx(it.exId), kind = exKind(it.exId);
    const repsLabel = kind === 's' ? 'Segundos' : kind === 'c' ? 'Minutos' : 'Reps';
    return `<div class="card">
      <div class="item-head"><button class="thumb-btn" data-act="howTo" data-id="${it.exId}" aria-label="Ver execução">${thumb(ex)}</button>
        <div class="grow"><div class="name">${i + 1}. ${esc(exName(it.exId))}</div>
        <div class="small muted">${esc(ex ? `${ex.group} · ${ex.equip}` : '')}</div></div>
        <div class="item-tools">
          <button class="icon-btn" data-act="itemUp" data-i="${i}" aria-label="Subir" ${i === 0 ? 'disabled style="opacity:.3"' : ''}>${I.up}</button>
          <button class="icon-btn" data-act="itemDown" data-i="${i}" aria-label="Descer" ${i === r.items.length - 1 ? 'disabled style="opacity:.3"' : ''}>${I.down}</button>
          <button class="icon-btn" data-act="itemRemove" data-i="${i}" aria-label="Remover">${I.trash}</button>
        </div></div>
      <div class="mini-grid">
        <label><span>Séries</span><input class="input num" type="number" inputmode="numeric" min="1" max="20" value="${it.sets}" data-rf="sets" data-i="${i}"></label>
        <label><span>${repsLabel}</span><input class="input num" type="text" inputmode="text" value="${esc(it.reps)}" placeholder="8-12" data-rf="reps" data-i="${i}"></label>
        <label><span>Descanso</span><select class="input num" data-rf="rest" data-i="${i}">${REST_OPTIONS.map(s => `<option value="${s}" ${s === +it.rest ? 'selected' : ''}>${s ? fmtRest(s) : '—'}</option>`).join('')}</select></label>
      </div>
      <input class="input note-input" type="text" placeholder="Observação (ex.: banco no 3, pegada aberta)" value="${esc(it.note)}" data-rf="note" data-i="${i}">
    </div>`;
  }).join('');

  const progOpts = `<option value="">Nenhum (ficha avulsa)</option>` +
    S.programs.map(p => `<option value="${p.id}" ${prog && prog.id === p.id ? 'selected' : ''}>${esc(p.name || 'Sem nome')}</option>`).join('') +
    `<option value="__new">+ Novo programa…</option>`;
  return topBar({ back: prog ? `#/programa/${prog.id}` : '#/fichas', right: `<button class="icon-btn" data-act="routineMenu" aria-label="Mais opções">${I.more}</button>` }) + `
    ${prog ? `<div class="eyebrow">${esc(prog.name)}</div>` : ''}
    <input class="title-input" value="${esc(r.name)}" placeholder="Nome da ficha" data-rname maxlength="60">
    <label class="prog-pick">${I.folder}<span>Programa</span><select class="input" data-rprog>${progOpts}</select></label>
    <h2 class="section" style="margin-top:4px">Dias da semana</h2>
    <div class="days">${days}</div>
    <h2 class="section">Exercícios <span class="muted small" style="text-transform:none;letter-spacing:0">${r.items.length}</span></h2>
    ${items || `<div class="card">${emptyState(I.dumbbell, 'Ficha vazia', 'Adicione os exercícios deste treino.')}</div>`}
    <div class="stack" style="margin-top:14px">
      <button class="btn block" data-act="addToRoutine">${I.plus}Adicionar exercícios</button>
      ${r.items.length ? `<button class="btn primary block" data-act="startRoutine" data-id="${r.id}">${I.play}Iniciar treino</button>` : ''}
    </div>`;
}

/* ================= Seletor de exercícios ================= */
let picker = null; // { sel:Set, q, g, multi, onDone }
function openPicker(opts) {
  picker = { sel: new Set(), q: '', g: '', multi: true, ...opts };
  renderPicker();
}
function renderPicker() {
  const p = picker;
  const chips = ['', ...GROUPS].map(g => `<button class="chip ${p.g === g ? 'on' : ''}" data-act="pickGroup" data-g="${esc(g)}">${g || 'Todos'}</button>`).join('');
  openSheet(`${sheetHead(p.multi ? 'Adicionar exercícios' : 'Escolher exercício')}
    <div class="search">${I.search}<input class="input" id="pickq" type="search" placeholder="Buscar exercício" value="${esc(p.q)}" autocomplete="off"></div>
    <div class="chips" style="margin-bottom:4px">${chips}</div>
    <div class="sheet-body" id="picklist">${pickerList()}</div>
    ${p.multi ? `<div class="sheet-foot"><button class="btn primary block" data-act="pickDone" id="pickbtn">${pickBtnLabel()}</button></div>` : ''}`, { picker: true });
}
function pickBtnLabel() { const n = picker.sel.size; return n ? `Adicionar ${n} exercício${n > 1 ? 's' : ''}` : 'Selecione os exercícios'; }
function filterEx(q, g) {
  const nq = norm(q.trim());
  return allEx().filter(e => (!g || e.group === g) && (!nq || norm(`${e.name} ${e.equip} ${e.en || ''}`).includes(nq)));
}
function groupedList(list, rowFn) {
  let out = '';
  for (const g of GROUPS) {
    const items = list.filter(e => e.group === g).sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
    if (items.length) out += `<div class="group-label">${g}</div>` + items.map(rowFn).join('');
  }
  return out;
}
function pickerList() {
  const p = picker, list = filterEx(p.q, p.g);
  const rows = groupedList(list, e => `<div class="pick-wrap"><button class="pick ${p.sel.has(e.id) ? 'on' : ''}" data-act="pickToggle" data-id="${e.id}">
    ${thumb(e)}<div class="grow"><div class="name">${esc(e.name)}</div><div class="sub">${esc(e.equip)}${e.custom ? ' · personalizado' : ''}</div></div>
    ${p.multi ? `<span class="tick">${I.check}</span>` : I.chev}</button>
    <button class="pick-info" data-act="pickPreview" data-id="${e.id}" aria-label="Ver execução de ${esc(e.name)}">${I.expand}</button></div>`);
  return (rows || `<div class="empty"><b>Nada encontrado</b>Não achou? Crie o exercício.</div>`) +
    `<div style="padding:16px 0"><button class="btn block" data-act="newExercise" data-from="picker">${I.plus}Criar exercício${p.q ? ` “${esc(p.q)}”` : ''}</button></div>`;
}

/* ================= Formulário de exercício personalizado ================= */
function exerciseForm(ex, from) {
  const e = ex || { name: from === 'picker' && picker ? picker.q : '', group: (picker && picker.g) || GROUPS[0], equip: 'Halteres', kind: 'w' };
  openSheet(`${sheetHead(ex ? 'Editar exercício' : 'Novo exercício')}
    <div class="sheet-body">
      <label class="field"><span>Nome</span><input class="input" id="exf-name" value="${esc(e.name)}" placeholder="Ex.: Remada articulada" maxlength="60"></label>
      <label class="field"><span>Grupo muscular</span><select class="input" id="exf-group">${GROUPS.map(g => `<option ${g === e.group ? 'selected' : ''}>${g}</option>`).join('')}</select></label>
      <label class="field"><span>Equipamento</span><select class="input" id="exf-equip">${EQUIPMENT.map(g => `<option ${g === e.equip ? 'selected' : ''}>${g}</option>`).join('')}</select></label>
      <label class="field"><span>Como registrar</span><select class="input" id="exf-kind">${Object.entries(KINDS).map(([k, v]) => `<option value="${k}" ${k === e.kind ? 'selected' : ''}>${v.label}</option>`).join('')}</select></label>
    </div>
    <div class="sheet-foot"><button class="btn primary block" data-act="saveExercise" data-id="${ex ? ex.id : ''}" data-from="${from || ''}">Salvar</button></div>`);
}

/* ================= Tela: Exercícios ================= */
let exQ = '', exG = '';
function viewExercicios() {
  const chips = ['', ...GROUPS].map(g => `<button class="chip ${exG === g ? 'on' : ''}" data-act="exGroup" data-g="${esc(g)}">${g || 'Todos'}</button>`).join('');
  return topBar({ right: `<button class="link-btn" data-act="newExercise">Novo</button>` }) + `<h1 class="title">Exercícios</h1>
    <div class="search">${I.search}<input class="input" id="exq" type="search" placeholder="Buscar entre ${allEx().length} exercícios" value="${esc(exQ)}" autocomplete="off"></div>
    <div class="chips">${chips}</div>
    <div id="exlist">${exList()}</div>`;
}
function exList() {
  const done = new Set(S.sessions.flatMap(s => s.exercises.map(e => e.exId)));
  const list = filterEx(exQ, exG);
  let html = '';
  for (const g of GROUPS) {
    const items = list.filter(e => e.group === g).sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
    if (!items.length) continue;
    html += `<h2 class="section">${g}<span class="small">${items.length}</span></h2><div class="list">` + items.map(e => `<a class="row" href="#/exercicio/${e.id}">${thumb(e)}
      <div class="grow"><div class="name">${esc(e.name)}</div><div class="sub">${esc(e.equip)}${e.custom ? ' · personalizado' : ''}${done.has(e.id) ? ' · <span style="color:var(--accent-text)">com histórico</span>' : ''}</div></div>${I.chev}</a>`).join('') + '</div>';
  }
  return html || `<div class="card">${emptyState(I.search, 'Nada encontrado', 'Tente outro termo ou crie um exercício novo.', `<button class="btn block" data-act="newExercise">${I.plus}Criar exercício</button>`)}</div>`;
}

let exMetric = {};
function viewExercicio(id) {
  const ex = getEx(id);
  const hist = exHistory(id);
  if (!ex && !hist.length) { location.replace('#/exercicios'); return null; }
  const kind = ex ? ex.kind : hist[0].kind;
  const name = ex ? ex.name : hist[0].sess.exercises.find(e => e.exId === id).name;
  const metrics = METRICS[kind];
  const mId = exMetric[id] || metrics[0].id;
  const metric = metrics.find(m => m.id === mId) || metrics[0];

  let html = topBar({ back: '#/exercicios', right: ex && ex.custom ? `<button class="link-btn" data-act="editExercise" data-id="${id}">Editar</button>` : '' }) +
    `<div class="eyebrow">${esc(ex ? `${ex.group} · ${ex.equip}` : 'Exercício removido')}</div><h1 class="title">${esc(name)}</h1>`;
  if (ex) html += `${demo(ex)}${musclesHTML(ex)}${videoActions(id)}<h2 class="section">Seus registros</h2>`;

  if (!hist.length) {
    html += `<div class="card">${emptyState(I.chart, 'Sem registros ainda', 'Quando você fizer este exercício num treino, a evolução aparece aqui.')}</div>`;
  } else {
    const work = hist.map(h => ({ t: h.sess.start, sets: h.sets.filter(isWork) })).filter(h => h.sets.length);
    const best = metrics.map(m => ({ m, v: max(work.map(h => m.f(h.sets))) })).filter(b => b.v > 0);
    html += `<div class="list" style="margin-bottom:10px">
      <div class="kv"><span>Treinos com este exercício</span><b>${hist.length}</b></div>
      ${best.map(b => `<div class="kv"><span>Recorde · ${b.m.label}</span><b>${fmt(b.v)} ${b.m.unit}</b></div>`).join('')}
      <div class="kv"><span>Última vez</span><b>${relDay(hist[hist.length - 1].sess.start)}</b></div></div>`;
    html += `<h2 class="section">Evolução</h2>
      <div class="chips">${metrics.map(m => `<button class="chip ${m.id === metric.id ? 'on' : ''}" data-act="exMetric" data-ex="${id}" data-m="${m.id}">${m.label}</button>`).join('')}</div>
      <div class="card">${chartSVG(work.map(h => ({ t: h.t, v: metric.f(h.sets) })), metric.unit)}</div>`;
    html += `<h2 class="section">Histórico</h2><div class="card" style="padding:4px 16px">` +
      hist.slice().reverse().slice(0, 30).map(h => `<a class="sess-ex" href="#/sessao/${h.sess.id}" style="display:block">
        <div class="name" style="display:flex;justify-content:space-between"><span>${dateLong(h.sess.start)}</span>${I.chev}</div>
        <div class="set-pills">${h.sets.map(s => `<span class="num" ${s.warm ? 'style="opacity:.6"' : ''}>${s.warm ? 'Aq · ' : ''}${fmtSet(kind, s)}</span>`).join('')}</div></a>`).join('') + '</div>';
  }
  if (ex && ex.custom) html += `<div style="margin-top:18px"><button class="btn danger block" data-act="deleteExercise" data-id="${id}">${I.trash}Excluir exercício</button></div>`;
  return html;
}

function chartSVG(points, unit) {
  if (points.length < 2) return `<div class="empty small" style="padding:18px">Faça este exercício em pelo menos 2 treinos para ver o gráfico.</div>`;
  const pts = points.slice(-24);
  const W = 340, H = 180, pl = 40, pr = 12, pt = 14, pb = 26;
  const vs = pts.map(p => p.v);
  let lo = Math.min(...vs), hi = Math.max(...vs);
  if (lo === hi) { lo -= 1; hi += 1; }
  const pad = (hi - lo) * 0.12; lo = Math.max(0, lo - pad); hi += pad;
  const x = i => pl + i * (W - pl - pr) / (pts.length - 1);
  const y = v => pt + (1 - (v - lo) / (hi - lo)) * (H - pt - pb);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p.v).toFixed(1)}`).join('');
  const area = `${line}L${x(pts.length - 1).toFixed(1)},${H - pb}L${pl},${H - pb}Z`;
  const ticks = [lo, (lo + hi) / 2, hi];
  const grid = ticks.map(v => `<line class="grid" x1="${pl}" x2="${W - pr}" y1="${y(v)}" y2="${y(v)}"/><text x="${pl - 6}" y="${y(v) + 4}" text-anchor="end">${fmt(v, hi - lo < 10 ? 1 : 0)}</text>`).join('');
  const dots = pts.map((p, i) => `<circle class="pt ${i === pts.length - 1 ? 'last' : ''}" cx="${x(i)}" cy="${y(p.v)}" r="${i === pts.length - 1 ? 4.5 : 3}"/>`).join('');
  const lastV = pts[pts.length - 1].v, firstV = pts[0].v, diff = lastV - firstV;
  return `<div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
      <div><b style="font-size:24px" class="num">${fmt(lastV)}</b> <span class="muted small">${unit}</span></div>
      <span class="badge ${diff >= 0 ? 'accent' : ''} num">${diff >= 0 ? '+' : ''}${fmt(diff)} ${unit}</span></div>
    <svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Gráfico de evolução">
      ${grid}<path class="area" d="${area}"/><path class="line" d="${line}"/>${dots}
      <text x="${pl}" y="${H - 6}">${dateShort(pts[0].t)}</text><text x="${W - pr}" y="${H - 6}" text-anchor="end">${dateShort(pts[pts.length - 1].t)}</text>
    </svg>`;
}

/* ================= Treino ativo ================= */
function makeActiveExercise(exId, sets, reps = '', rest = S.settings.rest, note = '') {
  const ex = getEx(exId);
  return {
    uid: uid(), exId, name: ex ? ex.name : 'Exercício', kind: ex ? ex.kind : 'w',
    target: reps, rest: +rest, note,
    sets: Array.from({ length: Math.max(1, sets) }, () => ({ a: '', b: '', done: false, warm: false }))
  };
}
function startWorkout(name, routineId, exercises) {
  if (S.active) {
    confirmSheet({
      title: 'Já existe um treino em andamento', text: `Deseja descartar “${esc(S.active.name)}” e começar outro?`,
      ok: 'Descartar e começar', danger: true,
      onOk: () => { S.active = null; startWorkout(name, routineId, exercises); }
    });
    return;
  }
  S.active = { id: uid(), name, routineId, start: Date.now(), notes: '', rest: null, exercises };
  save(); go('#/treino');
}
function startFromRoutine(r) {
  startWorkout(r.name, r.id, r.items.map(it => makeActiveExercise(it.exId, it.sets, it.reps, it.rest, it.note)));
}

function placeholder(ex, i, field) {
  const last = lastSets(ex.exId);
  const ref = last[i] || last[last.length - 1];
  if (ref && ref[field] != null) return fmt(ref[field], 2);
  for (let j = i - 1; j >= 0; j--) if (ex.sets[j][field] !== '') return ex.sets[j][field];
  return '';
}

function viewTreino() {
  const a = S.active;
  if (!a) { location.replace('#/hoje'); return null; }
  const total = sum(a.exercises.map(e => e.sets.length));
  const done = sum(a.exercises.map(e => e.sets.filter(s => s.done).length));
  const vol = sum(a.exercises.map(e => sum(e.sets.filter(s => s.done && !s.warm).map(s => e.kind === 'w' ? (num(s.a) || 0) * (num(s.b) || 0) : 0))));

  let html = `<div class="workout-head">
    <div class="row1">
      <a class="icon-btn" href="#/hoje" aria-label="Minimizar">${I.down}</a>
      <div class="wname" data-act="renameWorkout">${esc(a.name)}</div>
      <button class="btn primary sm" data-act="finishWorkout">Finalizar</button>
    </div>
    <div class="meta"><span>⏱ <b class="num" data-elapsed="${a.start}">${clock((Date.now() - a.start) / 1000)}</b></span>
      <span><b class="num">${done}/${total}</b> séries</span>${vol ? `<span><b class="num">${fmtInt(vol)}</b> kg</span>` : ''}</div>
    <div class="progress"><i style="width:${total ? done / total * 100 : 0}%"></i></div></div>`;

  if (!a.exercises.length) {
    html += `<div class="card">${emptyState(I.dumbbell, 'Treino livre', 'Adicione os exercícios conforme for treinando.')}</div>`;
  }

  a.exercises.forEach((ex, x) => {
    const K = KINDS[ex.kind];
    const last = lastSets(ex.exId);
    const allDone = ex.sets.length && ex.sets.every(s => s.done);
    const head = `<tr><th>Série</th><th>Anterior</th><th>${K.a}</th>${K.b ? `<th>${K.b}</th>` : ''}<th>✓</th></tr>`;
    let wn = 0;
    const rows = ex.sets.map((s, i) => {
      const label = s.warm ? 'A' : String(++wn);
      const prev = last[i] ? fmtSet(ex.kind, last[i]) : '—';
      const inp = f => `<input type="text" inputmode="decimal" value="${esc(s[f])}" placeholder="${esc(placeholder(ex, i, f))}" data-wf="${f}" data-x="${x}" data-s="${i}" aria-label="${f === 'a' ? K.a : K.b} da série ${label}">`;
      return `<tr class="${s.done ? 'done' : ''}">
        <td><button class="setno ${s.warm ? 'warm' : ''}" data-act="toggleWarm" data-x="${x}" data-s="${i}" aria-label="Alternar aquecimento">${label}</button></td>
        <td class="prev num">${prev}</td>
        <td>${inp('a')}</td>${K.b ? `<td>${inp('b')}</td>` : ''}
        <td><button class="check" data-act="toggleSet" data-x="${x}" data-s="${i}" aria-label="Concluir série">${I.check}</button></td></tr>`;
    }).join('');
    const targetTxt = [ex.sets.filter(s => !s.warm).length + ' × ' + (ex.target || '—') + (ex.kind === 's' ? 's' : ex.kind === 'c' ? ' min' : ''), `descanso ${fmtRest(ex.rest)}`].join(' · ');
    html += `<div class="ex-card ${allDone ? 'complete' : ''}">
      <div class="ex-title"><button class="thumb-btn" data-act="howTo" data-id="${ex.exId}" aria-label="Ver execução">${thumb(getEx(ex.exId))}</button>
        <div class="grow"><a class="name" href="#/exercicio/${ex.exId}">${esc(ex.name)}</a><div class="target num">${targetTxt}</div></div>
        <button class="icon-btn" data-act="exMenu" data-x="${x}" aria-label="Opções do exercício" style="margin:-8px -6px 0 0">${I.more}</button></div>
      ${ex.note ? `<div class="ex-note">${esc(ex.note)}</div>` : ''}
      <table class="sets">${head}${rows}</table>
      <div class="set-actions">
        <button class="btn sm" data-act="addSet" data-x="${x}">${I.plus}Série</button>
        ${ex.sets.length > 1 ? `<button class="btn sm" data-act="removeSet" data-x="${x}">Remover série</button>` : ''}
      </div></div>`;
  });

  html += `<div class="stack" style="margin-top:6px">
    <button class="btn block" data-act="addToWorkout">${I.plus}Adicionar exercício</button>
    <label class="field" style="margin-top:14px"><span>Anotações do treino</span>
      <textarea class="input" data-wnotes placeholder="Como foi o treino? Energia, dores, ajustes…">${esc(a.notes)}</textarea></label>
    <button class="btn danger block" data-act="discardWorkout">Descartar treino</button></div>`;
  return html;
}

function startRest(sec) {
  if (!S.active || !sec) return;
  S.active.rest = { end: Date.now() + sec * 1000, total: sec };
  save();
}

function finishWorkout() {
  const a = S.active;
  const done = sum(a.exercises.map(e => e.sets.filter(s => s.done).length));
  const pending = sum(a.exercises.map(e => e.sets.filter(s => !s.done).length));
  if (!done) {
    confirmSheet({
      title: 'Nenhuma série concluída', text: 'Marque as séries com ✓ conforme for treinando. Deseja descartar este treino?',
      ok: 'Descartar treino', danger: true, onOk: () => { S.active = null; save(); go('#/hoje'); }
    });
    return;
  }
  const routine = S.routines.find(r => r.id === a.routineId);
  let changed = false;
  if (routine) {
    const sig1 = routine.items.map(it => `${it.exId}:${it.sets}`).join('|');
    const sig2 = a.exercises.filter(e => e.sets.some(s => s.done)).map(e => `${e.exId}:${e.sets.filter(s => !s.warm).length}`).join('|');
    changed = sig1 !== sig2;
  }
  const extra = changed ? `<label class="row" style="border-radius:14px;background:var(--surface);margin-bottom:6px">
      <div class="grow"><div class="name">Atualizar a ficha</div><div class="sub">Salvar exercícios e nº de séries de hoje em “${esc(routine.name)}”</div></div>
      <span class="switch"><input type="checkbox" id="updRoutine"><i></i></span></label>` : '';
  confirmSheet({
    title: 'Finalizar treino?',
    text: `${done} série${done > 1 ? 's' : ''} concluída${done > 1 ? 's' : ''}${pending ? ` · ${pending} não marcada${pending > 1 ? 's' : ''} será${pending > 1 ? 'ão' : ''} descartada${pending > 1 ? 's' : ''}` : ''}.`,
    ok: 'Finalizar e salvar', extra,
    onOk: () => {
      const upd = $('#updRoutine') && $('#updRoutine').checked;
      commitWorkout(upd ? routine : null);
    }
  });
}

function commitWorkout(updateRoutine) {
  const a = S.active;
  const sess = {
    id: a.id, name: a.name, routineId: a.routineId, start: a.start, end: Date.now(), notes: a.notes.trim(),
    exercises: a.exercises.map(e => ({
      exId: e.exId, name: e.name, kind: e.kind,
      sets: e.sets.filter(s => s.done).map(s => {
        const o = { a: num(s.a), b: e.kind === 's' ? null : num(s.b) };
        if (s.warm) o.warm = true;
        return o;
      })
    })).filter(e => e.sets.length)
  };
  // Recordes pessoais (comparados com os treinos anteriores)
  sess.prs = [];
  for (const e of sess.exercises) {
    const prev = exHistory(e.exId).map(h => h.sets.filter(isWork)).filter(s => s.length);
    const cur = e.sets.filter(isWork);
    if (!prev.length || !cur.length) continue;
    for (const m of METRICS[e.kind]) {
      if (m.id === 'vol' || m.id === 'tot' || m.id === 'km') continue;
      const best = max(prev.map(m.f)), now = m.f(cur);
      if (now > best && now > 0) sess.prs.push({ exId: e.exId, name: e.name, label: m.label, value: now, unit: m.unit });
    }
  }
  if (updateRoutine) {
    updateRoutine.items = a.exercises.filter(e => e.sets.some(s => s.done)).map(e => {
      const old = updateRoutine.items.find(it => it.exId === e.exId);
      return { id: old ? old.id : uid(), exId: e.exId, sets: e.sets.filter(s => !s.warm).length || 1, reps: e.target || (old && old.reps) || '', rest: e.rest, note: e.note || '' };
    });
  }
  S.sessions.unshift(sess);
  S.sessions.sort((x, y) => y.start - x.start);
  S.active = null;
  save();
  afterRender = () => showSummary(sess);
  go(`#/sessao/${sess.id}`);
}

function showSummary(sess) {
  const vol = sessVolume(sess);
  const prs = sess.prs || [];
  openSheet(`<div class="sheet-body"><div class="summary-big">
      <div style="display:inline-grid;place-items:center;width:64px;height:64px;border-radius:20px;background:var(--accent);color:var(--accent-ink)">
        <svg viewBox="0 0 24 24" style="width:34px;height:34px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round">${I.trophy.replace(/<\/?svg[^>]*>/g, '')}</svg></div>
      <h3>Treino concluído!</h3><div class="muted">${esc(sess.name)}</div></div>
    <div class="stats" style="margin-top:16px">
      <div class="stat"><b class="num">${fmtDur(sess.end - sess.start)}</b><span>duração</span></div>
      <div class="stat"><b class="num">${sessSetCount(sess)}</b><span>séries</span></div>
      <div class="stat"><b class="num">${vol ? fmtInt(vol) : '—'}</b><span>kg de volume</span></div></div>
    ${prs.length ? `<h2 class="section">Recordes pessoais 🎉</h2><div class="list">${prs.map(p => `<div class="kv"><span>${esc(p.name)} · ${p.label}</span><b style="color:var(--gold)">${fmt(p.value)} ${p.unit}</b></div>`).join('')}</div>` : ''}
    </div><div class="sheet-foot"><button class="btn primary block" data-act="closeSheet">Fechar</button></div>`);
}

/* ================= Tela: Histórico ================= */
function viewHistorico() {
  let html = `<div class="top"></div><h1 class="title">Histórico</h1>`;
  if (!S.sessions.length) {
    return html + `<div class="card">${emptyState(I.chart, 'Nenhum treino registrado', 'Seus treinos finalizados aparecem aqui, com duração, volume e recordes.')}</div>`;
  }
  // Mapa de calor: últimas 17 semanas
  const weeks = 17, today = startOfDay(Date.now());
  const first = addDays(startOfWeek(today), -(weeks - 1) * 7);
  const counts = new Map();
  for (const s of S.sessions) { const k = startOfDay(s.start); counts.set(k, (counts.get(k) || 0) + 1); }
  let cells = '';
  for (let i = 0; i < weeks * 7; i++) {
    const t = addDays(first, i);
    const c = counts.get(t) || 0;
    cells += `<i class="${t > today ? 'fut' : c > 1 ? 'l2' : c ? 'l2' : ''} ${t === today ? 'today' : ''}" title="${dateShort(t)}"></i>`;
  }
  const wk = startOfWeek(Date.now());
  const thisWeek = sessionsInRange(wk, addDays(wk, 7));
  const weekVol = sum(thisWeek.map(sessVolume));
  const avgDur = sum(S.sessions.slice(0, 20).map(s => s.end - s.start)) / Math.min(20, S.sessions.length);

  html += `<div class="card"><div class="heat">${cells}</div>
    <div class="heat-legend"><span>${dateShort(first)}</span><span>Seg → Dom por coluna</span><span>hoje</span></div></div>
    <div class="stats">
      <div class="stat"><b class="num">${S.sessions.length}</b><span>treinos no total</span></div>
      <div class="stat"><b class="num">${weekVol >= 10000 ? fmt(weekVol / 1000) + 't' : fmtInt(weekVol)}</b><span>kg nesta semana</span></div>
      <div class="stat"><b class="num">${fmtDur(avgDur)}</b><span>duração média</span></div></div>`;

  let month = '';
  for (const s of S.sessions) {
    const d = new Date(s.start), m = `${MONTH[d.getMonth()]} ${d.getFullYear()}`;
    if (m !== month) { html += `${month ? '</div>' : ''}<h2 class="section">${m}</h2><div class="list">`; month = m; }
    const vol = sessVolume(s);
    html += `<a class="row" href="#/sessao/${s.id}">
      <div class="dot"><span class="num">${d.getDate()}</span></div>
      <div class="grow"><div class="name">${esc(s.name)}${(s.prs || []).length ? ` <span class="badge gold">${s.prs.length} PR</span>` : ''}</div>
      <div class="sub">${DAY[d.getDay()]} ${timeHM(s.start)} · ${fmtDur(s.end - s.start)} · ${sessSetCount(s)} séries${vol ? ` · ${fmtInt(vol)} kg` : ''}</div></div>${I.chev}</a>`;
  }
  return html + '</div>';
}

function viewSessao(id) {
  const s = S.sessions.find(x => x.id === id);
  if (!s) { location.replace('#/historico'); return null; }
  const vol = sessVolume(s);
  const prKeys = new Set((s.prs || []).map(p => p.exId));
  let html = topBar({ back: '#/historico', right: `<button class="link-btn" data-act="sessionMenu" data-id="${id}">Opções</button>` }) +
    `<div class="eyebrow">${dateLong(s.start)} · ${timeHM(s.start)}</div><h1 class="title">${esc(s.name)}</h1>
    <div class="stats">
      <div class="stat"><b class="num">${fmtDur(s.end - s.start)}</b><span>duração</span></div>
      <div class="stat"><b class="num">${sessSetCount(s)}</b><span>séries</span></div>
      <div class="stat"><b class="num">${vol ? fmtInt(vol) : '—'}</b><span>kg de volume</span></div></div>`;
  if ((s.prs || []).length) {
    html += `<h2 class="section">Recordes pessoais</h2><div class="list">${s.prs.map(p => `<div class="kv"><span>${esc(p.name)} · ${p.label}</span><b style="color:var(--gold)">${fmt(p.value)} ${p.unit}</b></div>`).join('')}</div>`;
  }
  html += `<h2 class="section">Exercícios</h2><div class="card" style="padding:4px 16px">` + s.exercises.map(e => {
    let wn = 0;
    return `<a class="sess-ex" href="#/exercicio/${e.exId}" style="display:block">
      <div class="name" style="display:flex;justify-content:space-between;gap:8px"><span>${esc(exName(e.exId, e.name))}${prKeys.has(e.exId) ? ' <span class="badge gold">PR</span>' : ''}</span>${I.chev}</div>
      <div class="set-pills">${e.sets.map(x => `<span class="num" ${x.warm ? 'style="opacity:.6"' : ''}><i>${x.warm ? 'A' : ++wn}</i>${fmtSet(e.kind, x)}</span>`).join('')}</div></a>`;
  }).join('') + '</div>';
  if (s.notes) html += `<h2 class="section">Anotações</h2><div class="card" style="white-space:pre-wrap">${esc(s.notes)}</div>`;
  html += `<div class="stack" style="margin-top:18px"><button class="btn primary block" data-act="repeatSession" data-id="${id}">${I.play}Repetir este treino</button></div>`;
  return html;
}

/* ================= Tela: Ajustes ================= */
function viewAjustes() {
  const standalone = window.navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  const scheme = schemeNow();
  return `<div class="top"></div><h1 class="title">Ajustes</h1>
    <h2 class="section">Aparência</h2>
    <div class="card look">
      <div class="look-label">Tema</div>
      <div class="seg" role="radiogroup" aria-label="Tema">${THEMES.map(([v, l]) => `<button class="${S.settings.theme === v ? 'on' : ''}" data-act="setTheme" data-v="${v}" role="radio" aria-checked="${S.settings.theme === v}">${l}</button>`).join('')}</div>
      <div class="look-label">Cor de destaque</div>
      <div class="swatches" role="radiogroup" aria-label="Cor de destaque">${ACCENTS.map(a => {
        const on = (S.settings.accent || 'limao') === a.id, c = a[scheme];
        return `<button class="sw ${on ? 'on' : ''}" data-act="setAccent" data-v="${a.id}" role="radio" aria-checked="${on}" style="--sw:${c[0]};--sw-ink:${c[1]}"><i>${on ? I.check : ''}</i><span>${a.name}</span></button>`;
      }).join('')}</div>
    </div>
    <div class="list" style="margin-top:10px">
      <label class="row"><div class="grow"><div class="name">Efeito vidro (Liquid Glass)</div><div class="sub wrap">Barra flutuante e painéis translúcidos, no estilo do iOS 26</div></div>
        <span class="switch"><input type="checkbox" data-setting="glass" ${S.settings.glass ? 'checked' : ''}><i></i></span></label>
    </div>

    <h2 class="section">Treino</h2>
    <div class="list">
      <label class="row"><div class="grow"><div class="name">Descanso padrão</div><div class="sub">Usado em treino livre e exercícios novos</div></div>
        <select class="input" style="width:auto;min-height:38px;padding:6px 34px 6px 12px" data-setting="rest">${REST_OPTIONS.filter(Boolean).map(s => `<option value="${s}" ${s === S.settings.rest ? 'selected' : ''}>${fmtRest(s)}</option>`).join('')}</select></label>
      <label class="row"><div class="grow"><div class="name">Som ao fim do descanso</div><div class="sub">Toca com o app aberto</div></div>
        <span class="switch"><input type="checkbox" data-setting="sound" ${S.settings.sound ? 'checked' : ''}><i></i></span></label>
    </div>

    ${'caches' in window ? `<h2 class="section">Fotos dos exercícios</h2>
    <div class="list">
      <button class="row" data-act="downloadPhotos"><div class="grow"><div class="name">Baixar fotos para usar offline</div><div class="sub" data-photo-status>${photoStatus()}</div></div>${I.download.replace('<svg', '<svg class="chev"')}</button>
    </div>
    <p class="small muted" style="margin:10px 4px 0">Sem baixar, cada foto fica salva depois da primeira vez que aparece. Use no Wi-Fi.</p>` : ''}

    <h2 class="section">Inteligência artificial</h2>
    <div class="list">
      <button class="row" data-act="aiKeyEdit"><div class="grow"><div class="name">Chave da API da Anthropic</div>
        <div class="sub">${typeof aiKey === 'function' && aiKey() ? 'Configurada neste aparelho' : 'Não configurada — necessária para montar treino com IA'}</div></div>${I.chev}</button>
      <a class="row" href="#/assistente"><div class="grow"><div class="name">Assistente de treino</div><div class="sub">Monta opções de programa a partir do seu perfil</div></div>${I.chev}</a>
    </div>
    <p class="small muted" style="margin:10px 4px 0">A IA usa o Claude, da Anthropic, e é cobrada na sua conta da Anthropic. Sem chave, o assistente funciona com regras de treino, offline e grátis.</p>

    <h2 class="section">Seus dados</h2>
    <div class="list">
      <button class="row" data-act="exportData"><div class="grow"><div class="name">Exportar backup</div><div class="sub">Salve um arquivo .json no app Arquivos ou iCloud</div></div>${I.chev}</button>
      <label class="row" style="cursor:pointer"><div class="grow"><div class="name">Importar backup</div><div class="sub">Substitui os dados atuais pelos do arquivo</div></div>${I.chev}
        <input type="file" accept="application/json,.json" id="importFile" hidden></label>
      <a class="row" href="#/modelos"><div class="grow"><div class="name">Modelos de treino prontos</div><div class="sub">PPL, Upper/Lower, ABC, ABCDE, em casa e mais</div></div>${I.chev}</a>
      <button class="row" data-act="wipeData"><div class="grow"><div class="name" style="color:var(--danger)">Apagar todos os dados</div></div></button>
    </div>
    <p class="small muted" style="margin:10px 4px 0">${S.programs.length} programas · ${S.routines.length} fichas · ${S.sessions.length} treinos · ${S.custom.length} exercícios personalizados. Tudo fica salvo só neste aparelho — exporte um backup de vez em quando.</p>

    ${standalone ? '' : `<h2 class="section">Instalar no iPhone</h2>
    <div class="card small" style="line-height:1.55">
      1. Abra este endereço no <b>Safari</b>.<br>
      2. Toque em <b>Compartilhar</b> (quadrado com seta para cima).<br>
      3. Escolha <b>Adicionar à Tela de Início</b> e confirme.<br>
      <span class="muted">O app abre em tela cheia, funciona offline e mantém seus dados.</span></div>`}

    <div style="text-align:center;margin-top:22px"><button class="link-btn" data-act="checkUpdate">Procurar atualização</button></div>
    <p class="small muted" style="text-align:center;margin-top:6px">Ficha · versão 1.3<br>
      Fotos e músculos dos exercícios: <a href="https://github.com/yuhonas/free-exercise-db" target="_blank" rel="noopener" style="text-decoration:underline">free-exercise-db</a> (domínio público)</p>`;
}

function exportData() {
  const data = JSON.stringify({ app: 'ficha', exportedAt: new Date().toISOString(), ...S }, null, 2);
  const d = new Date();
  const name = `ficha-backup-${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}.json`;
  const file = new File([data], name, { type: 'application/json' });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    navigator.share({ files: [file], title: 'Backup do Ficha' }).catch(() => {});
    return;
  }
  const url = URL.createObjectURL(file);
  const a = document.createElement('a');
  a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  toast('Backup exportado');
}
function importData(file) {
  const reader = new FileReader();
  reader.onload = () => {
    let d;
    try { d = JSON.parse(reader.result); } catch (e) { toast('Arquivo inválido'); return; }
    if (!d || !Array.isArray(d.routines) || !Array.isArray(d.sessions)) { toast('Esse arquivo não é um backup do Ficha'); return; }
    confirmSheet({
      title: 'Importar backup?', text: `O arquivo tem ${d.routines.length} fichas e ${d.sessions.length} treinos. Os dados atuais deste aparelho serão substituídos.`,
      ok: 'Importar', danger: true,
      onOk: () => {
        const b = blank();
        migrate(d);
        S = { ...b, settings: { ...b.settings, ...(d.settings || {}) }, custom: d.custom || [], videos: d.videos || {}, profile: d.profile || null, programs: d.programs, routines: d.routines, sessions: d.sessions.sort((x, y) => y.start - x.start), active: d.active || null };
        save(); applyLook(); toast('Backup importado'); go('#/hoje');
      }
    });
  };
  reader.readAsText(file);
}

/* ================= Dock (descanso / treino em andamento) ================= */
function renderDock() {
  const dock = $('#dock');
  const a = S.active;
  let html = '';
  if (a && a.rest && a.rest.end > Date.now()) {
    const left = (a.rest.end - Date.now()) / 1000, C = 2 * Math.PI * 19;
    html += `<div class="dock-bar rest">
      <svg class="ring" viewBox="0 0 44 44"><circle class="bg" cx="22" cy="22" r="19"/><circle class="fg" cx="22" cy="22" r="19" stroke-dasharray="${C}" stroke-dashoffset="${C * (1 - left / a.rest.total)}" data-ring="${C}"/></svg>
      <div class="grow"><div class="lbl">Descanso</div><div class="big num" data-rest>${clock(left)}</div></div>
      <button class="btn sm" data-act="restAdd" data-d="-15">−15</button>
      <button class="btn sm" data-act="restAdd" data-d="15">+15</button>
      <button class="btn sm" data-act="restSkip">Pular</button></div>`;
  }
  if (a && location.hash !== '#/treino') {
    html += `<a class="dock-bar" href="#/treino"><div class="grow"><div class="lbl">Treino em andamento</div>
      <div style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(a.name)} · <span class="num" data-elapsed="${a.start}">${clock((Date.now() - a.start) / 1000)}</span></div></div>
      <span class="btn sm primary" style="background:var(--accent);color:var(--accent-ink)">Abrir</span></a>`;
  }
  dock.innerHTML = html;
}

function tick() {
  const now = Date.now();
  document.querySelectorAll('[data-elapsed]').forEach(el => { el.textContent = clock((now - +el.dataset.elapsed) / 1000); });
  const a = S.active;
  if (a && a.rest) {
    const left = (a.rest.end - now) / 1000;
    if (left <= 0) {
      a.rest = null; save(); renderDock();
      beep(); toast('Descanso terminado — próxima série!');
    } else {
      const el = $('[data-rest]');
      if (!el) renderDock();
      else {
        el.textContent = clock(left);
        const ring = $('[data-ring]');
        if (ring) ring.setAttribute('stroke-dashoffset', ring.dataset.ring * (1 - left / a.rest.total));
      }
    }
  }
}

/* ================= Ações ================= */
const ACT = {
  closeSheet: () => closeSheet(),
  sheetOk: () => { const ctx = sheetCtx; closeSheet(); if (ctx && ctx.onOk) ctx.onOk(); },

  // Fichas
  newMenu: () => openSheet(`${sheetHead('Criar')}<div class="sheet-body"><div class="list">
      <button class="row" data-act="newProgram"><div class="dot on">${I.folder}</div><div class="grow"><div class="name">Novo programa</div><div class="sub">Uma pasta com várias fichas (ex.: Push, Pull, Legs)</div></div>${I.chev}</button>
      <button class="row" data-act="newRoutine"><div class="dot">${I.list}</div><div class="grow"><div class="name">Nova ficha avulsa</div><div class="sub">Uma ficha solta, fora de programa</div></div>${I.chev}</button>
      <a class="row" href="#/modelos" data-act="closeSheet"><div class="dot">${I.sparkle}</div><div class="grow"><div class="name">Usar um modelo pronto</div><div class="sub">PPL, Upper/Lower, ABC, ABCDE, em casa…</div></div>${I.chev}</a>
    </div></div>`),
  newProgram: () => {
    const p = { id: uid(), name: '', active: true };
    S.programs.push(p); save(); closeSheet();
    afterRender = () => { const el = $('[data-pname]'); if (el) el.focus(); };
    go(`#/programa/${p.id}`);
  },
  newRoutine: el => {
    const pid = el && el.dataset.prog && S.programs.some(p => p.id === el.dataset.prog) ? el.dataset.prog : null;
    const r = { id: uid(), name: '', days: [], items: [], programId: pid };
    S.routines.push(r); save(); closeSheet();
    afterRender = () => { const el2 = $('[data-rname]'); if (el2) el2.focus(); };
    go(`#/ficha/${r.id}`);
  },
  programMenu: el => {
    const p = S.programs.find(x => x.id === el.dataset.id);
    openSheet(`${sheetHead(p.name || 'Programa')}<div class="sheet-body"><div class="list">
      <button class="row" data-act="dupProgram" data-id="${p.id}"><div class="grow name">Duplicar programa</div></button>
      <button class="row" data-act="deleteProgram" data-id="${p.id}"><div class="grow name" style="color:var(--danger)">Excluir programa</div></button>
    </div></div>`);
  },
  dupProgram: el => {
    const p = S.programs.find(x => x.id === el.dataset.id);
    const c = { ...clone(p), id: uid(), name: (p.name || 'Programa') + ' (cópia)' };
    S.programs.splice(S.programs.indexOf(p) + 1, 0, c);
    for (const r of progRoutines(p.id)) {
      const rc = clone(r); rc.id = uid(); rc.programId = c.id; rc.items.forEach(it => it.id = uid());
      S.routines.push(rc);
    }
    save(); closeSheet(); go(`#/programa/${c.id}`);
  },
  deleteProgram: el => {
    const p = S.programs.find(x => x.id === el.dataset.id), n = progRoutines(p.id).length;
    openSheet(`${sheetHead('Excluir programa?')}<div class="sheet-body">
      <p class="muted" style="margin:0 0 14px">“${esc(p.name || 'Sem nome')}” tem ${n} ficha${n === 1 ? '' : 's'}. Os treinos já registrados continuam no histórico.</p></div>
      <div class="sheet-foot stack">
        ${n ? `<button class="btn block" data-act="deleteProgramKeep" data-id="${p.id}">Excluir e manter as fichas como avulsas</button>` : ''}
        <button class="btn danger block" data-act="deleteProgramAll" data-id="${p.id}">${n ? 'Excluir programa e fichas' : 'Excluir programa'}</button>
        <button class="btn block" data-act="closeSheet">Cancelar</button>
      </div>`);
  },
  deleteProgramKeep: el => {
    const id = el.dataset.id;
    S.routines.forEach(r => { if (r.programId === id) r.programId = null; });
    S.programs = S.programs.filter(p => p.id !== id); save(); closeSheet(); go('#/fichas');
  },
  deleteProgramAll: el => {
    const id = el.dataset.id;
    S.routines = S.routines.filter(r => r.programId !== id);
    S.programs = S.programs.filter(p => p.id !== id); save(); closeSheet(); go('#/fichas');
  },
  progUp: el => { moveInProgram(el.dataset.id, -1); save(); rerender(); },
  progDown: el => { moveInProgram(el.dataset.id, 1); save(); rerender(); },
  moveIntoProgram: el => {
    const pid = el.dataset.id, loose = S.routines.filter(r => !programOf(r));
    openSheet(`${sheetHead('Trazer fichas avulsas')}<div class="sheet-body"><div class="list">
      ${loose.map(r => `<button class="row" data-act="moveRoutineTo" data-id="${r.id}" data-prog="${pid}"><div class="grow"><div class="name">${esc(r.name || 'Sem nome')}</div><div class="sub">${daysLabel(r.days)} · ${r.items.length} exercícios</div></div><span class="badge accent">Mover</span></button>`).join('')}
    </div></div>`);
  },
  moveRoutineTo: el => {
    const r = S.routines.find(x => x.id === el.dataset.id);
    r.programId = el.dataset.prog;
    // vai para o fim da lista para ficar por último no programa
    S.routines.splice(S.routines.indexOf(r), 1); S.routines.push(r);
    save(); toast('Ficha movida');
    if (S.routines.some(x => !programOf(x))) ACT.moveIntoProgram({ dataset: { id: el.dataset.prog } }); else closeSheet();
    rerender();
  },

  // Modelos prontos
  tplFilter: el => { tplFilter = el.dataset.v; rerender(); },
  useTpl: el => {
    const t = tpls().find(x => x.id === el.dataset.id);
    openUseProgram({ name: t.name, routines: t.routines });
  },
  createPending: () => {
    const def = pendingProgram; if (!def) return;
    const useDays = !$('#tplDays') || $('#tplDays').checked, only = $('#tplOnly') && $('#tplOnly').checked;
    if (only) S.programs.forEach(p => { p.active = false; });
    const p = { id: uid(), name: def.name, active: true };
    S.programs.push(p);
    for (const tr of def.routines) {
      S.routines.push({
        id: uid(), name: tr.name, days: useDays ? [...tr.days] : [], programId: p.id,
        items: tr.items.filter(([exId]) => getEx(exId)).map(([exId, sets, reps, rest, note]) => ({ id: uid(), exId, sets, reps, rest, note: note || '' }))
      });
    }
    pendingProgram = null;
    save(); closeSheet(); toast('Programa criado — ajuste como quiser');
    go(`#/programa/${p.id}`);
  },

  // Aparência
  setTheme: el => { S.settings.theme = el.dataset.v; save(); applyLook(); rerender(); },
  setAccent: el => { S.settings.accent = el.dataset.v; save(); applyLook(); rerender(); },
  checkUpdate: () => {
    toast('Procurando atualização…');
    const done = () => setTimeout(() => location.reload(), 500);
    if (!('serviceWorker' in navigator)) { done(); return; }
    navigator.serviceWorker.getRegistration().then(reg => reg ? reg.update() : null).catch(() => {}).then(done);
  },
  toggleDay: el => {
    const r = curRoutine(), d = +el.dataset.d;
    r.days = r.days.includes(d) ? r.days.filter(x => x !== d) : [...r.days, d];
    save(); rerender();
  },
  itemUp: el => { moveItem(curRoutine().items, +el.dataset.i, -1); save(); rerender(); },
  itemDown: el => { moveItem(curRoutine().items, +el.dataset.i, 1); save(); rerender(); },
  itemRemove: el => { curRoutine().items.splice(+el.dataset.i, 1); save(); rerender(); },
  addToRoutine: () => {
    const r = curRoutine();
    openPicker({
      onDone: ids => {
        for (const exId of ids) {
          const ex = getEx(exId);
          r.items.push({ id: uid(), exId, sets: ex.kind === 'c' ? 1 : 3, reps: ex.kind === 's' ? '30' : ex.kind === 'c' ? '20' : '10-12', rest: ex.kind === 'c' ? 0 : S.settings.rest, note: '' });
        }
        save(); rerender();
      }
    });
  },
  routineMenu: () => {
    const r = curRoutine();
    openSheet(`${sheetHead(r.name || 'Ficha')}<div class="sheet-body"><div class="list">
      <button class="row" data-act="dupRoutine"><div class="grow name">Duplicar ficha</div></button>
      <button class="row" data-act="deleteRoutine"><div class="grow name" style="color:var(--danger)">Excluir ficha</div></button>
    </div></div>`);
  },
  dupRoutine: () => {
    const r = curRoutine(), c = clone(r);
    c.id = uid(); c.name = (r.name || 'Ficha') + ' (cópia)'; c.items.forEach(it => it.id = uid());
    S.routines.splice(S.routines.indexOf(r) + 1, 0, c); save(); closeSheet(); go(`#/ficha/${c.id}`);
  },
  deleteRoutine: () => {
    const r = curRoutine();
    confirmSheet({
      title: 'Excluir ficha?', text: `“${esc(r.name || 'Sem nome')}” será removida. Os treinos já registrados continuam no histórico.`,
      ok: 'Excluir', danger: true, onOk: () => { const p = programOf(r); S.routines = S.routines.filter(x => x !== r); save(); go(p ? `#/programa/${p.id}` : '#/fichas'); }
    });
  },
  startRoutine: el => {
    const r = S.routines.find(x => x.id === el.dataset.id);
    if (!r.items.length) { toast('Adicione exercícios à ficha primeiro'); return; }
    startFromRoutine(r);
  },
  startEmpty: () => startWorkout('Treino livre', null, []),

  // Seletor
  pickGroup: el => { picker.g = el.dataset.g; renderPicker(); },
  pickToggle: el => {
    const id = el.dataset.id;
    if (!picker.multi) { const cb = picker.onDone; closeSheet(); picker = null; cb([id]); return; }
    picker.sel.has(id) ? picker.sel.delete(id) : picker.sel.add(id);
    el.classList.toggle('on');
    $('#pickbtn').textContent = pickBtnLabel();
  },
  pickDone: () => {
    if (!picker.sel.size) { toast('Toque nos exercícios para selecionar'); return; }
    const ids = [...picker.sel], cb = picker.onDone;
    closeSheet(); picker = null; cb(ids);
  },

  // Exercícios
  exGroup: el => { exG = el.dataset.g; rerender(); },
  exMetric: el => { exMetric[el.dataset.ex] = el.dataset.m; rerender(); },
  newExercise: el => exerciseForm(null, el.dataset.from),
  editExercise: el => exerciseForm(getEx(el.dataset.id)),
  saveExercise: el => {
    const name = $('#exf-name').value.trim();
    if (!name) { toast('Dê um nome ao exercício'); $('#exf-name').focus(); return; }
    const data = { name, group: $('#exf-group').value, equip: $('#exf-equip').value, kind: $('#exf-kind').value };
    let ex;
    if (el.dataset.id) { ex = getEx(el.dataset.id); Object.assign(ex, data); }
    else { ex = { id: 'c-' + uid(), ...data, custom: true }; S.custom.push(ex); }
    save();
    if (el.dataset.from === 'picker' && picker) {
      if (picker.multi) { picker.sel.add(ex.id); picker.q = ''; picker.g = ex.group; renderPicker(); }
      else { const cb = picker.onDone; closeSheet(); picker = null; cb([ex.id]); }
    } else { closeSheet(); toast('Exercício salvo'); rerender(); }
  },
  deleteExercise: el => {
    const ex = getEx(el.dataset.id);
    confirmSheet({
      title: 'Excluir exercício?', text: `“${esc(ex.name)}” sairá das fichas. O histórico de treinos é mantido.`,
      ok: 'Excluir', danger: true,
      onOk: () => {
        S.custom = S.custom.filter(e => e.id !== ex.id);
        S.routines.forEach(r => { r.items = r.items.filter(it => it.exId !== ex.id); });
        save(); go('#/exercicios');
      }
    });
  },

  // Treino ativo
  toggleSet: el => {
    const ex = S.active.exercises[+el.dataset.x], i = +el.dataset.s, s = ex.sets[i];
    if (s.done) { s.done = false; save(); rerender(); return; }
    const K = KINDS[ex.kind];
    for (const f of ['a', 'b']) if (s[f] === '' && (f === 'a' || K.b)) s[f] = placeholder(ex, i, f);
    const need = ex.kind === 'w' || ex.kind === 'bw' ? 'b' : 'a';
    if (num(s[need]) == null || num(s[need]) <= 0) {
      rerender();
      const inp = $(`[data-wf="${need}"][data-x="${el.dataset.x}"][data-s="${i}"]`);
      if (inp) inp.focus();
      toast(need === 'b' ? 'Informe as repetições' : ex.kind === 's' ? 'Informe o tempo em segundos' : 'Informe os minutos');
      return;
    }
    if (ex.kind === 'w' && s.a === '') s.a = '0';
    s.done = true;
    unlockAudio();
    startRest(ex.rest);
    save(); rerender();
  },
  toggleWarm: el => {
    const s = S.active.exercises[+el.dataset.x].sets[+el.dataset.s];
    s.warm = !s.warm; save(); rerender();
    toast(s.warm ? 'Série de aquecimento (não conta em recordes)' : 'Série normal');
  },
  addSet: el => {
    const ex = S.active.exercises[+el.dataset.x];
    const last = ex.sets[ex.sets.length - 1];
    ex.sets.push({ a: last ? last.a : '', b: last ? last.b : '', done: false, warm: false });
    save(); rerender();
  },
  removeSet: el => {
    const ex = S.active.exercises[+el.dataset.x];
    if (ex.sets.length > 1) ex.sets.pop();
    save(); rerender();
  },
  exMenu: el => {
    const x = +el.dataset.x, ex = S.active.exercises[x];
    openSheet(`${sheetHead(ex.name)}<div class="sheet-body">
      <h2 class="section" style="margin-top:4px">Descanso entre séries</h2>
      <div class="chips" style="flex-wrap:wrap;margin:0 0 6px;padding:0">${REST_OPTIONS.map(s => `<button class="chip ${s === ex.rest ? 'on' : ''}" data-act="exRest" data-x="${x}" data-v="${s}">${s ? fmtRest(s) : 'Nenhum'}</button>`).join('')}</div>
      <div class="list" style="margin-top:16px">
        ${x > 0 ? `<button class="row" data-act="exMove" data-x="${x}" data-d="-1"><div class="grow name">Mover para cima</div></button>` : ''}
        ${x < S.active.exercises.length - 1 ? `<button class="row" data-act="exMove" data-x="${x}" data-d="1"><div class="grow name">Mover para baixo</div></button>` : ''}
        <button class="row" data-act="howTo" data-id="${ex.exId}"><div class="grow name">Ver execução (foto e vídeo)</div></button>
        <button class="row" data-act="exReplace" data-x="${x}"><div class="grow name">Substituir exercício</div></button>
        <a class="row" href="#/exercicio/${ex.exId}" data-act="closeSheet"><div class="grow name">Ver evolução</div></a>
        <button class="row" data-act="exRemove" data-x="${x}"><div class="grow name" style="color:var(--danger)">Remover do treino</div></button>
      </div></div>`);
  },
  exRest: el => { S.active.exercises[+el.dataset.x].rest = +el.dataset.v; save(); closeSheet(); rerender(); },
  exMove: el => { moveItem(S.active.exercises, +el.dataset.x, +el.dataset.d); save(); closeSheet(); rerender(); },
  exRemove: el => { S.active.exercises.splice(+el.dataset.x, 1); save(); closeSheet(); rerender(); },
  exReplace: el => {
    const x = +el.dataset.x;
    closeSheet();
    openPicker({
      multi: false,
      onDone: ([id]) => {
        const old = S.active.exercises[x];
        const n = makeActiveExercise(id, old.sets.length, old.target, old.rest);
        S.active.exercises[x] = n; save(); rerender();
      }
    });
  },
  addToWorkout: () => openPicker({
    onDone: ids => {
      for (const id of ids) {
        const ex = getEx(id);
        S.active.exercises.push(makeActiveExercise(id, ex.kind === 'c' ? 1 : 3, '', ex.kind === 'c' ? 0 : S.settings.rest));
      }
      save(); rerender();
      window.scrollTo(0, document.body.scrollHeight);
    }
  }),
  renameWorkout: () => {
    const n = prompt('Nome do treino', S.active.name);
    if (n && n.trim()) { S.active.name = n.trim(); save(); rerender(); }
  },
  finishWorkout: () => finishWorkout(),
  discardWorkout: () => confirmSheet({
    title: 'Descartar treino?', text: 'Nada deste treino será salvo no histórico.', ok: 'Descartar', danger: true,
    onOk: () => { S.active = null; save(); go('#/hoje'); }
  }),
  restAdd: el => {
    const r = S.active && S.active.rest; if (!r) return;
    const d = +el.dataset.d * 1000;
    r.end += d; r.total = Math.max(1, r.total + d / 1000);
    if (r.end <= Date.now()) S.active.rest = null;
    save(); renderDock();
  },
  restSkip: () => { if (S.active) { S.active.rest = null; save(); renderDock(); } },

  // Execução (foto / vídeo)
  howTo: el => openHowTo(el.dataset.id, { evo: true }),
  setVideo: el => {
    const id = el.dataset.id;
    const v = prompt('Cole o link do vídeo da execução (YouTube, Instagram, Google Drive…). Deixe vazio para remover.', S.videos[id] || '');
    if (v == null) return;
    const t = v.trim();
    if (t && !isUrl(t)) { toast('Link inválido — ele deve começar com https://'); return; }
    if (t) S.videos[id] = t; else delete S.videos[id];
    save(); toast(t ? 'Vídeo salvo' : 'Vídeo removido');
    if (!$('#sheet').hidden && sheetCtx && sheetCtx.howTo) openHowTo(id, sheetCtx.howTo); else rerender();
  },
  pickPreview: el => {
    const id = el.dataset.id, on = picker.sel.has(id);
    picker.scroll = $('#picklist').scrollTop;
    openHowTo(id, {
      foot: `<div class="sheet-foot btn-row">
        <button class="btn" data-act="pickBack">${I.back}Voltar</button>
        <button class="btn primary" data-act="pickFromPreview" data-id="${id}">${!picker.multi ? 'Escolher' : on ? 'Desmarcar' : 'Selecionar'}</button></div>`
    });
  },
  pickBack: () => backToPicker(),
  pickFromPreview: el => {
    const id = el.dataset.id;
    if (!picker.multi) { const cb = picker.onDone; closeSheet(); picker = null; cb([id]); return; }
    picker.sel.has(id) ? picker.sel.delete(id) : picker.sel.add(id);
    backToPicker();
  },
  downloadPhotos: () => downloadPhotos(),

  // Histórico
  sessionMenu: el => {
    openSheet(`${sheetHead('Treino')}<div class="sheet-body"><div class="list">
      <button class="row" data-act="renameSession" data-id="${el.dataset.id}"><div class="grow name">Renomear</div></button>
      <button class="row" data-act="deleteSession" data-id="${el.dataset.id}"><div class="grow name" style="color:var(--danger)">Excluir do histórico</div></button>
    </div></div>`);
  },
  renameSession: el => {
    const s = S.sessions.find(x => x.id === el.dataset.id);
    closeSheet();
    const n = prompt('Nome do treino', s.name);
    if (n && n.trim()) { s.name = n.trim(); save(); rerender(); }
  },
  deleteSession: el => {
    const id = el.dataset.id;
    confirmSheet({
      title: 'Excluir treino?', text: 'Ele sai do histórico e dos gráficos de evolução.', ok: 'Excluir', danger: true,
      onOk: () => { S.sessions = S.sessions.filter(s => s.id !== id); save(); go('#/historico'); }
    });
  },
  repeatSession: el => {
    const s = S.sessions.find(x => x.id === el.dataset.id);
    const routine = S.routines.find(r => r.id === s.routineId);
    startWorkout(s.name, s.routineId, s.exercises.map(e => {
      const it = routine && routine.items.find(i => i.exId === e.exId);
      const n = makeActiveExercise(e.exId, e.sets.length, it ? it.reps : '', it ? it.rest : S.settings.rest, it ? it.note : '');
      e.sets.forEach((x, i) => { n.sets[i].warm = !!x.warm; });
      return n;
    }));
  },

  // Ajustes
  exportData: () => exportData(),
  wipeData: () => confirmSheet({
    title: 'Apagar tudo?', text: 'Fichas, histórico e exercícios personalizados serão apagados deste aparelho. Isso não pode ser desfeito.',
    ok: 'Apagar tudo', danger: true,
    onOk: () => { S = blank(); save(); applyLook(); if (typeof aiSetKey === 'function') aiSetKey(''); toast('Dados apagados'); go('#/hoje'); }
  })
};

if (typeof ASSIST_ACTIONS !== 'undefined') Object.assign(ACT, ASSIST_ACTIONS);

function backToPicker() {
  renderPicker();
  const l = $('#picklist'); if (l) l.scrollTop = picker.scroll || 0;
}
function curRoutine() { const id = location.hash.split('/')[2]; return S.routines.find(r => r.id === id); }
function moveItem(arr, i, d) {
  const j = i + d;
  if (j < 0 || j >= arr.length) return;
  [arr[i], arr[j]] = [arr[j], arr[i]];
}

/* ================= Eventos ================= */
document.addEventListener('click', e => {
  const el = e.target.closest('[data-act]');
  if (!el) return;
  const fn = ACT[el.dataset.act];
  if (!fn) return;
  if (el.tagName !== 'A') e.preventDefault();
  fn(el, e);
});
document.addEventListener('pointerdown', unlockAudio, { once: true });

document.addEventListener('input', e => {
  const t = e.target;
  if (t.dataset.wf && S.active) {
    const ex = S.active.exercises[+t.dataset.x];
    ex.sets[+t.dataset.s][t.dataset.wf] = t.value.replace(/[^\d.,]/g, '');
    save();
  } else if (t.dataset.rf) {
    const r = curRoutine(), it = r.items[+t.dataset.i], f = t.dataset.rf;
    it[f] = f === 'sets' ? Math.max(1, Math.min(20, parseInt(t.value) || 1)) : f === 'rest' ? +t.value : t.value;
    save();
  } else if (t.hasAttribute('data-rname')) {
    curRoutine().name = t.value; save();
  } else if (t.hasAttribute('data-pname')) {
    const pr = S.programs.find(x => x.id === location.hash.split('/')[2]);
    if (pr) { pr.name = t.value; save(); }
  } else if (t.hasAttribute('data-wnotes')) {
    S.active.notes = t.value; save();
  } else if (t.id === 'exq') {
    exQ = t.value; $('#exlist').innerHTML = exList();
  } else if (t.dataset.pf) {
    if (typeof onProfileInput === 'function') onProfileInput(t);
  } else if (t.id === 'pickq') {
    picker.q = t.value; $('#picklist').innerHTML = pickerList();
  }
});
document.addEventListener('change', e => {
  const t = e.target;
  if (t.dataset.setting === 'rest') { S.settings.rest = +t.value; save(); toast('Descanso padrão atualizado'); }
  else if (t.dataset.setting === 'sound') { S.settings.sound = t.checked; save(); if (t.checked) { unlockAudio(); beep(); } }
  else if (t.dataset.setting === 'glass') { S.settings.glass = t.checked; save(); applyLook(); }
  else if (t.hasAttribute('data-pactive')) {
    const pr = S.programs.find(x => x.id === location.hash.split('/')[2]);
    if (pr) { pr.active = t.checked; save(); rerender(); }
  } else if (t.hasAttribute('data-rprog')) {
    const r = curRoutine();
    let v = t.value;
    if (v === '__new') {
      const n = prompt('Nome do novo programa', 'Meu treino');
      if (!n || !n.trim()) { rerender(); return; }
      const pr = { id: uid(), name: n.trim(), active: true };
      S.programs.push(pr); v = pr.id;
    }
    r.programId = v || null; save(); rerender();
    toast(v ? 'Ficha movida para o programa' : 'Ficha agora é avulsa');
  }
  else if (t.id === 'importFile' && t.files[0]) { importData(t.files[0]); t.value = ''; }
});
// Seleciona o conteúdo ao focar nos campos de carga/reps (agiliza a edição)
document.addEventListener('focusin', e => { if (e.target.dataset && e.target.dataset.wf) setTimeout(() => e.target.select(), 0); });
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !$('#sheet').hidden) closeSheet();
  if (e.key === 'Enter' && e.target.dataset && e.target.dataset.wf) e.target.blur();
});
$('#sheet-backdrop').addEventListener('click', closeSheet);
window.addEventListener('hashchange', () => { if (!$('#sheet').hidden) closeSheet(); route(); });
document.addEventListener('visibilitychange', () => { if (!document.hidden) tick(); });

/* ================= Início ================= */
applyLook();
setInterval(tick, 500);
route();

if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js', { updateViaCache: 'none' }).then(reg => {
      // Procura versão nova sempre que o app volta para a tela
      document.addEventListener('visibilitychange', () => { if (!document.hidden) reg.update().catch(() => {}); });
    }).catch(() => {});
  });
  // Versão nova instalada: recarrega uma vez para usar os arquivos novos (os dados ficam no aparelho)
  const hadController = !!navigator.serviceWorker.controller;
  let reloading = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!hadController || reloading) return;
    reloading = true; location.reload();
  });
}
