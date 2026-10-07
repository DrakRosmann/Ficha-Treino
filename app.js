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
  check: '<svg viewBox="0 0 24 24"><path pathLength="1" d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
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
  camera: '<svg viewBox="0 0 24 24"><path d="M4 8.5a2 2 0 0 1 2-2h2l1.5-2h5L16 6.5h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/><circle cx="12" cy="13" r="3.5"/></svg>',
  barcode: '<svg viewBox="0 0 24 24"><path d="M4 6v12M7 6v12M10.5 6v12M13 6v12M16.5 6v12M20 6v12"/></svg>',
  food: '<svg viewBox="0 0 24 24"><path d="M7 3v18M4.5 3v5a2.5 2.5 0 0 0 5 0V3M17 21V3c-2 1.2-3.5 3.6-3.5 6.5V13H17"/></svg>',
  dumbbell: '<svg viewBox="0 0 24 24"><path d="M2.5 12h2M19.5 12h2M8 12h8"/><rect x="4.5" y="7.5" width="3.5" height="9" rx="1"/><rect x="16" y="7.5" width="3.5" height="9" rx="1"/></svg>',
  list: '<svg viewBox="0 0 24 24"><rect x="5" y="3.5" width="14" height="17" rx="2"/><path d="M8.5 9h7M8.5 13h7M8.5 17h4"/></svg>',
  chart: '<svg viewBox="0 0 24 24"><path d="M3.5 19.5h17"/><path d="M6 16l4-5 3.5 3 5-7"/></svg>',
  trophy: '<svg viewBox="0 0 24 24"><path d="M8 4.5h8v5a4 4 0 0 1-8 0zM8 6.5H5a3 3 0 0 0 3 4M16 6.5h3a3 3 0 0 1-3 4M12 13.5V17M8.5 20h7M10 17h4v3h-4z"/></svg>',
  video: '<svg viewBox="0 0 24 24"><rect x="3" y="5.5" width="18" height="13" rx="3"/><path d="M10.5 9.5v5l4-2.5z"/></svg>',
  expand: '<svg viewBox="0 0 24 24"><path d="M14 4.5h5.5V10M10 19.5H4.5V14M19.5 4.5 13.5 10.5M4.5 19.5l6-6"/></svg>',
  download: '<svg viewBox="0 0 24 24"><path d="M12 4.5v11M7 11l5 5 5-5M5 19.5h14"/></svg>',
  folder: '<svg viewBox="0 0 24 24"><path d="M3.5 7.5a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"/></svg>',
  sparkle: '<svg viewBox="0 0 24 24"><path d="M11 3.5l1.9 5 5 1.9-5 1.9-1.9 5-1.9-5-5-1.9 5-1.9z"/><path d="M18.5 14.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z"/></svg>',
  body: '<svg viewBox="0 0 24 24"><circle cx="12" cy="4.6" r="2.1"/><path d="M4.5 8.6c2.6.9 5 1.3 7.5 1.3s4.9-.4 7.5-1.3M12 9.9v5.4M12 15.3l-3.4 6.2M12 15.3l3.4 6.2"/></svg>',
  palette: '<svg viewBox="0 0 24 24"><path d="M12 3.5a8.5 8.5 0 0 0 0 17c1.2 0 1.8-.8 1.8-1.7 0-1.3-1.2-1.6-1.2-2.8 0-1 .8-1.7 1.8-1.7h2.1a4 4 0 0 0 4-4c0-3.8-3.8-6.8-8.5-6.8z"/><circle cx="7.8" cy="11" r="1"/><circle cx="10.5" cy="7.5" r="1"/><circle cx="15" cy="7.8" r="1"/></svg>'
};

/* ================= Estado ================= */
function blank() {
  return {
    v: 1, settings: { rest: 90, sound: true, theme: 'auto', accent: 'limao', style: 'auto', styleAuto: true, keepAwake: true, iosTimer: false, timerShortcut: 'Descanso Ficha',
      progression: true, rir: true, bar: 20, lastBackup: 0, backupSnooze: 0 },
    custom: [], programs: [], routines: [], sessions: [], active: null, videos: {}, profile: null, body: [], bodyGoal: {},
    food: { days: {}, custom: [], fav: [], recent: [], goal: null, tdee: null }
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
  if (d.settings) {
    delete d.settings.lockTimer; // opção antiga (descanso por áudio), removida
    delete d.settings.glass; // "Efeito vidro" (liga/desliga) virou a escolha de estilo
    // O estilo passou a seguir o aparelho (Automático) — aplicado uma vez; a escolha manual continua em Ajustes
    if (!d.settings.styleAuto) { d.settings.style = 'auto'; d.settings.styleAuto = true; }
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
  if (typeof muscleMapHTML === 'function') return muscleMapHTML(ex);
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
function setPill(kind, s, mark) {
  return `<span class="num ${s.t || ''}" ${s.warm ? 'style="opacity:.6"' : ''}><i>${mark}</i>${fmtSet(kind, s)}${s.rir != null ? ` <small>RIR ${s.rir >= 5 ? '5+' : s.rir}</small>` : ''}</span>`;
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
// [id, rótulo curto, nome, descrição]
const STYLES = [
  ['auto', 'Automático', 'Automático', 'Segue o aparelho: Liquid Glass no iPhone e iPad, Material You no Android e Clássico nos demais'],
  ['classic', 'Clássico', 'Clássico', 'Visual sólido e simples, sem transparências'],
  ['glass', 'Glass', 'Liquid Glass', 'Barra flutuante e painéis translúcidos, no estilo do iOS 26'],
  ['material', 'Material', 'Material You', 'No estilo do Android, com cores tonais geradas a partir da cor de destaque, formas arredondadas e efeito de toque']
];
// Estilo nativo de cada sistema (o mesmo teste está no index.html, para aplicar antes do app carregar)
function deviceStyle() {
  const ua = navigator.userAgent || '', plat = (navigator.userAgentData && navigator.userAgentData.platform) || '';
  if (/android/i.test(ua) || /android/i.test(plat)) return 'material';
  // iPadOS se apresenta como Mac: diferencia pela tela de toque
  if (/iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1)) return 'glass';
  return 'classic';
}
const lightMQ = matchMedia('(prefers-color-scheme: light)');
function schemeNow() {
  const t = S.settings.theme;
  return t === 'light' || (t === 'auto' && lightMQ.matches) ? 'light' : 'dark';
}
// Estilo em uso: o escolhido em Ajustes ou, no Automático, o do aparelho
function styleNow() {
  const v = S.settings.style;
  return v !== 'auto' && STYLES.some(x => x[0] === v) ? v : deviceStyle();
}
function accentNow() { return ACCENTS.find(x => x.id === S.settings.accent) || ACCENTS[0]; }
function accentVars(scheme) {
  const a = accentNow()[scheme];
  const [r, g, b] = [1, 3, 5].map(i => parseInt(a[0].slice(i, i + 2), 16));
  return { '--accent': a[0], '--accent-ink': a[1], '--accent-text': a[2], '--accent-soft': `rgba(${r}, ${g}, ${b}, .16)` };
}

/* Material You: paleta tonal como a do Android 12+ ("Tonal Spot"), gerada a partir da cor de destaque.
   Tom = luminosidade L* do Material 3 (0 preto, 100 branco); matiz e croma em OKLCH, ajustados ao sRGB. */
const MD = (() => {
  const lin = c => c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  const gam = c => c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055;
  function oklch(hex) {
    const [r, g, b] = [1, 3, 5].map(i => lin(parseInt(hex.slice(i, i + 2), 16) / 255));
    const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    const A = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
    const B = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
    return [0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s, Math.hypot(A, B), Math.atan2(B, A)];
  }
  function rgbOf(L, C, h) {
    const a = C * Math.cos(h), b = C * Math.sin(h);
    const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
    const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
    const s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3;
    return [4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
      -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
      -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s];
  }
  const ok = c => c.every(v => v >= -1e-4 && v <= 1.0001);
  function fit(L, C, h) { // reduz o croma até a cor existir no sRGB
    if (ok(rgbOf(L, C, h))) return rgbOf(L, C, h);
    let lo = 0, hi = C;
    for (let i = 0; i < 14; i++) { const mid = (lo + hi) / 2; if (ok(rgbOf(L, mid, h))) lo = mid; else hi = mid; }
    return rgbOf(L, lo, h);
  }
  const lum = c => 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  const toneY = t => { const f = (t + 16) / 116; return f ** 3 > 216 / 24389 ? f ** 3 : t / (24389 / 27); };
  const memo = new Map();
  function tone(t, C, h) {
    const key = `${t}|${C}|${h.toFixed(3)}`;
    if (memo.has(key)) return memo.get(key);
    let c = [t / 100, t / 100, t / 100];
    if (t > 0 && t < 100) {
      const y = toneY(t);
      let lo = 0, hi = 1;
      for (let i = 0; i < 18; i++) { const mid = (lo + hi) / 2; c = fit(mid, C, h); if (lum(c) < y) lo = mid; else hi = mid; }
    }
    const hex = '#' + c.map(v => Math.round(gam(Math.min(1, Math.max(0, v))) * 255).toString(16).padStart(2, '0')).join('');
    memo.set(key, hex);
    return hex;
  }
  // Paletas: primária, secundária, terciária (matiz +60°), neutra e neutra variante. Cor sem saturação → tons de cinza.
  function palettes(seed) {
    const [, c, h] = oklch(seed), gray = c < 0.03, k = gray ? 0 : 1;
    const pal = (C, hh = h) => t => tone(t, C * k, hh);
    return { gray, p: pal(0.11), s: pal(0.045), t: pal(0.07, h + Math.PI / 3), n: pal(0.012), nv: pal(0.022), e: t => tone(t, 0.17, 0.5) };
  }
  // Papéis de cor do Material 3 → variáveis do app
  function vars(seed, scheme, black) {
    const { gray, p, s, t, n, nv, e } = palettes(seed), d = scheme === 'dark';
    const T = (dark, light) => d ? dark : light;
    return {
      '--bg': d && black ? '#000000' : T(n(6), n(98)),
      '--surface': T(n(black ? 6 : 12), n(94)),
      '--surface-2': T(n(black ? 10 : 17), n(92)),
      '--surface-3': T(n(black ? 14 : 22), n(90)),
      '--line': T(nv(30), nv(80)),
      '--text': T(n(90), n(10)),
      '--muted': T(nv(80), nv(30)),
      '--faint': T(nv(60), nv(50)),
      '--accent': gray ? T(p(90), p(20)) : T(p(80), p(40)),
      '--accent-ink': gray ? T(p(10), p(100)) : T(p(20), p(100)),
      '--accent-text': gray ? T(p(90), p(20)) : T(p(80), p(40)),
      '--accent-soft': T(s(30), s(90)),
      '--danger': T(e(80), e(40)),
      '--danger-soft': T(e(30), e(90)),
      '--md-surface-low': d && black ? n(4) : T(n(10), n(96)),
      '--md-primary-container': T(p(30), p(90)),
      '--md-on-primary-container': T(p(90), p(10)),
      '--md-secondary-container': T(s(30), s(90)),
      '--md-on-secondary-container': T(s(90), s(10)),
      '--md-tertiary-container': T(t(30), t(90)),
      '--md-on-tertiary-container': T(t(90), t(10)),
      '--md-inverse-surface': T(n(90), n(20)),
      '--md-inverse-on-surface': T(n(20), n(95))
    };
  }
  // Amostra da cor em Ajustes: primária em cima, secundária e terciária embaixo (como no Android)
  function swatch(seed, scheme) {
    const { gray, p, s, t } = palettes(seed), d = scheme === 'dark';
    return [gray ? p(d ? 90 : 20) : p(d ? 80 : 40), s(d ? 70 : 60), t(d ? 70 : 60), gray ? p(d ? 10 : 100) : p(d ? 20 : 100)];
  }
  return { vars, swatch };
})();
// Cor de origem do Material You: a versão viva da cor de destaque (igual nos temas claro e escuro)
function mdSeed(a = accentNow()) { return a.dark[0]; }
function lookVars(scheme) {
  if (styleNow() !== 'material') return accentVars(scheme);
  return MD.vars(mdSeed(), scheme, S.settings.theme === 'black');
}
function applyLook() {
  const root = document.documentElement, scheme = schemeNow(), style = styleNow();
  root.dataset.scheme = scheme;
  root.toggleAttribute('data-black', S.settings.theme === 'black');
  root.toggleAttribute('data-glass', style === 'glass');
  root.toggleAttribute('data-material', style === 'material');
  [...root.style].filter(k => k.startsWith('--')).forEach(k => root.style.removeProperty(k));
  for (const [k, v] of Object.entries(lookVars(scheme))) root.style.setProperty(k, v);
  const bg = getComputedStyle(root).getPropertyValue('--bg').trim();
  document.querySelectorAll('meta[name="theme-color"]').forEach(m => { m.removeAttribute('media'); m.content = bg; });
  // Cópia leve para o index.html aplicar o tema antes do app carregar (evita piscar)
  try {
    localStorage.setItem('ficha.look', JSON.stringify({
      theme: S.settings.theme, style, dark: lookVars('dark'), light: lookVars('light')
    }));
  } catch (e) { /* ignora */ }
}
lightMQ.addEventListener && lightMQ.addEventListener('change', () => { if (S.settings.theme === 'auto') applyLook(); });
// Material You: onda de toque (ripple) nos botões e linhas
document.addEventListener('pointerdown', e => {
  if (!document.documentElement.hasAttribute('data-material')) return;
  const el = e.target.closest('.btn, .row, .chip, .seg button, .icon-btn, .pick, .kv-btn, .sets .check');
  if (!el || el.disabled) return;
  const r = el.getBoundingClientRect(), size = Math.hypot(r.width, r.height) * 2;
  const w = document.createElement('span');
  w.className = 'ripple';
  w.style.cssText = `width:${size}px;height:${size}px;left:${e.clientX - r.left - size / 2}px;top:${e.clientY - r.top - size / 2}px`;
  el.appendChild(w);
  w.addEventListener('animationend', () => w.remove());
});

/* ================= UI: toast, sheet, som ================= */
let toastTimer;
const reduceMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg; t.hidden = false; t.classList.remove('out');
  t.style.animation = 'none'; void t.offsetWidth; t.style.animation = '';
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    t.classList.add('out');
    toastTimer = setTimeout(() => { t.hidden = true; t.classList.remove('out'); }, reduceMotion() ? 0 : 220);
  }, 2400);
}

let sheetCtx = null, sheetCleanup = null, sheetHideTimer = 0;
function runSheetCleanup() { if (sheetCleanup) { const f = sheetCleanup; sheetCleanup = null; f(); } }
// cleanup: chamado quando o painel fecha ou é trocado (ex.: desligar a câmera)
function openSheet(html, ctx = null, cleanup = null) {
  runSheetCleanup();
  sheetCtx = ctx; sheetCleanup = cleanup;
  const sh = $('#sheet'), bd = $('#sheet-backdrop');
  const swap = !sh.hidden && !sh.classList.contains('closing');
  clearTimeout(sheetHideTimer);
  sh.classList.remove('closing', 'swap'); bd.classList.remove('closing');
  sh.style.transform = ''; sh.style.transition = ''; bd.style.opacity = '';
  sh.innerHTML = '<div class="grab"></div>' + html;
  if (swap) { void sh.offsetWidth; sh.classList.add('swap'); } // troca de conteúdo com o painel aberto
  sh.hidden = false; bd.hidden = false;
  document.documentElement.style.overflow = 'hidden';
}
function closeSheet() {
  sheetCtx = null;
  runSheetCleanup();
  const sh = $('#sheet'), bd = $('#sheet-backdrop');
  document.documentElement.style.overflow = '';
  if (sh.hidden || sh.classList.contains('closing')) return;
  const done = () => {
    sh.hidden = true; bd.hidden = true; sh.innerHTML = '';
    sh.classList.remove('closing', 'swap'); bd.classList.remove('closing');
    sh.style.transform = ''; sh.style.transition = ''; bd.style.opacity = '';
  };
  if (reduceMotion()) { done(); return; }
  sh.classList.add('closing'); bd.classList.add('closing');
  clearTimeout(sheetHideTimer);
  sheetHideTimer = setTimeout(done, 260);
}
// Arrastar o painel para baixo (pela alça ou pelo título) fecha
(() => {
  let drag = null;
  document.addEventListener('pointerdown', e => {
    const sh = $('#sheet');
    if (sh.hidden || sh.classList.contains('closing')) return;
    const h = e.target.closest('#sheet .grab, #sheet .sheet-head');
    if (!h || e.target.closest('button, input, a, label')) return;
    drag = { y: e.clientY, t: performance.now(), dy: 0 };
    sh.style.transition = 'none';
  });
  document.addEventListener('pointermove', e => {
    if (!drag) return;
    drag.dy = Math.max(0, e.clientY - drag.y);
    $('#sheet').style.transform = `translateY(${drag.dy}px)`;
    $('#sheet-backdrop').style.opacity = String(Math.max(0, 1 - drag.dy / 400));
  });
  const end = () => {
    if (!drag) return;
    const sh = $('#sheet'), v = drag.dy / Math.max(1, performance.now() - drag.t), dy = drag.dy;
    drag = null;
    if (dy > 110 || (v > 0.6 && dy > 30)) { closeSheet(); return; }
    sh.style.transition = 'transform .3s cubic-bezier(.32,.72,0,1)';
    sh.style.transform = ''; $('#sheet-backdrop').style.opacity = '';
  };
  document.addEventListener('pointerup', end);
  document.addEventListener('pointercancel', end);
})();
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
  [/^#\/corpo$/, () => typeof viewCorpo === 'function' ? viewCorpo() : viewUpdating(), 'corpo'],
  [/^#\/assistente$/, () => typeof viewAssistente === 'function' ? viewAssistente() : viewUpdating(), 'fichas'],
  [/^#\/dieta$/, () => typeof viewDieta === 'function' ? viewDieta() : viewUpdating(), 'dieta'],
  [/^#\/exercicios$/, viewExercicios, 'fichas'],
  [/^#\/exercicio\/([\w-]+)$/, viewExercicio, 'fichas'],
  [/^#\/historico$/, viewHistorico, 'historico'],
  [/^#\/sessao\/([\w-]+)$/, viewSessao, 'historico'],
  [/^#\/treino$/, viewTreino, null],
  [/^#\/ajustes$/, viewAjustes, 'ajustes']
];
let current = null, currentTab = null, lastHash = null, afterRender = null;
const TAB_ROOTS = ['#/hoje', '#/fichas', '#/dieta', '#/historico', '#/corpo', '#/ajustes'];
let navStack = [], viewAnim = false;

function route() {
  const h = location.hash || '#/hoje';
  for (const [re, fn, tab] of routes) {
    const m = h.match(re);
    if (m) {
      current = () => fn(...m.slice(1));
      currentTab = tab;
      const changed = h !== lastHash, first = lastHash == null;
      let dir = 'none';
      if (changed && !first) {
        if (navStack[navStack.length - 2] === h) { dir = 'back'; navStack.pop(); }
        else if (TAB_ROOTS.includes(h)) { dir = 'tab'; navStack = [h]; }
        else { dir = 'forward'; navStack.push(h); }
      } else if (first) navStack = [h];
      lastHash = h;
      viewAnim = changed;
      const update = () => { render(); if (changed) window.scrollTo(0, 0); };
      if (dir !== 'none' && document.startViewTransition && !reduceMotion() && !document.hidden) {
        document.documentElement.dataset.nav = dir;
        document.startViewTransition(update);
      } else update();
      return;
    }
  }
  location.replace('#/hoje');
}
function go(hash) { if (location.hash === hash) route(); else location.hash = hash; }
function render() {
  syncWakeLock();
  const html = current();
  if (html == null) return; // a view redirecionou
  const view = $('#view');
  view.innerHTML = html;
  // Animações de entrada (gráficos, anéis, barras) só quando a tela muda, não a cada atualização
  if (viewAnim) {
    viewAnim = false;
    view.classList.remove('anim'); void view.offsetWidth; view.classList.add('anim');
    view.classList.toggle('no-vt', !document.startViewTransition);
    clearTimeout(render.animT); render.animT = setTimeout(() => view.classList.remove('anim', 'no-vt'), 1400);
  }
  document.body.classList.toggle('in-workout', location.hash === '#/treino');
  const tabs = [...document.querySelectorAll('#tabs a')], ti = tabs.findIndex(a => a.dataset.tab === currentTab);
  tabs.forEach((a, i) => a.classList.toggle('on', i === ti));
  const bar = $('#tabs');
  bar.style.setProperty('--ti', Math.max(0, ti)); bar.style.setProperty('--tn', tabs.length);
  bar.classList.toggle('no-tab', ti < 0);
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

  if (typeof dietTodayCard === 'function') html += dietTodayCard();
  if (!S.active && backupDue()) {
    const n = unbackedSessions();
    html += `<div class="card backup-card"><div class="bk-row"><span class="bk-ic">${I.download}</span>
      <div class="grow"><b>Faça um backup</b><div class="small muted">${n} treino${n > 1 ? 's' : ''} ainda não ${n > 1 ? 'estão salvos' : 'está salvo'} fora deste aparelho. Guarde o arquivo no iCloud Drive.</div></div></div>
      <div class="btn-row" style="margin-top:12px"><button class="btn sm primary" data-act="exportData">Salvar backup</button><button class="btn sm" data-act="backupLater">Depois</button></div></div>`;
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
    <a class="tpl-banner alt" href="#/modelos"><span class="tpl-ic">${I.list}</span><div class="grow"><b>Modelos prontos</b><span>PPL, Upper/Lower, ABC, ABCDE, em casa e mais</span></div>${I.chev}</a>
    <a class="tpl-banner alt" href="#/exercicios"><span class="tpl-ic">${I.dumbbell}</span><div class="grow"><b>Exercícios</b><span>${allEx().length} exercícios com foto da execução, músculos e sua evolução</span></div>${I.chev}</a>`;
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
  return topBar({ back: '#/fichas', right: `<button class="link-btn" data-act="newExercise">Novo</button>` }) + `<h1 class="title">Exercícios</h1>
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
    // Próximo treino: sugestão pela faixa de repetições da primeira ficha que tem o exercício
    if (typeof suggestNext === 'function' && S.settings.progression !== false) {
      const it = S.routines.flatMap(r => r.items).find(x => x.exId === id);
      const sg = suggestNext(id, it ? it.reps : '');
      if (sg && sg.type !== 'first') html += suggestionHTML(sg).replace('Hoje: ', 'Próximo treino: ');
    }
    if (kind === 'w' && typeof openCalc === 'function') {
      const top = work.length ? work[work.length - 1].sets.reduce((a, b) => (b.a || 0) > (a.a || 0) ? b : a) : null;
      html += `<div class="btn-row" style="margin:10px 0 0">
        <button class="btn sm" data-act="calc" data-mode="plates" data-w="${top ? top.a : ''}">Anilhas</button>
        <button class="btn sm" data-act="calc" data-mode="warm" data-w="${top ? top.a : ''}">Aquecimento</button>
        <button class="btn sm" data-act="calc" data-mode="rm" data-w="${top ? top.a : ''}" data-r="${top ? top.b : ''}">1RM</button></div>`;
    }
    html += `<h2 class="section">Evolução</h2>
      <div class="chips">${metrics.map(m => `<button class="chip ${m.id === metric.id ? 'on' : ''}" data-act="exMetric" data-ex="${id}" data-m="${m.id}">${m.label}</button>`).join('')}</div>
      <div class="card">${chartSVG(work.map(h => ({ t: h.t, v: metric.f(h.sets) })), metric.unit)}</div>`;
    html += `<h2 class="section">Histórico</h2><div class="card" style="padding:4px 16px">` +
      hist.slice().reverse().slice(0, 30).map(h => `<a class="sess-ex" href="#/sessao/${h.sess.id}" style="display:block">
        <div class="name" style="display:flex;justify-content:space-between"><span>${dateLong(h.sess.start)}</span>${I.chev}</div>
        <div class="set-pills">${(n => h.sets.map(s => setPill(kind, s, setMark(s) || ++n)))(0).join('')}</div></a>`).join('') + '</div>';
  }
  if (ex && ex.custom) html += `<div style="margin-top:18px"><button class="btn danger block" data-act="deleteExercise" data-id="${id}">${I.trash}Excluir exercício</button></div>`;
  return html;
}

function chartSVG(points, unit, opts = {}) {
  const { time = false, goal = null, better = 'up', dec = 1, max = 24, empty } = opts;
  if (points.length < 2) return `<div class="empty small" style="padding:18px">${empty || 'Faça este exercício em pelo menos 2 treinos para ver o gráfico.'}</div>`;
  const pts = points.slice(-max);
  const W = 340, H = 180, pl = 40, pr = 12, pt = 14, pb = 26;
  const vs = pts.map(p => p.v).concat(goal != null ? [goal] : []);
  let lo = Math.min(...vs), hi = Math.max(...vs);
  if (lo === hi) { lo -= 1; hi += 1; }
  const pad = (hi - lo) * 0.12; lo = Math.max(0, lo - pad); hi += pad;
  const t0 = pts[0].t, t1 = pts[pts.length - 1].t;
  const x = (p, i) => time && t1 > t0 ? pl + (p.t - t0) / (t1 - t0) * (W - pl - pr) : pl + i * (W - pl - pr) / (pts.length - 1);
  const y = v => pt + (1 - (v - lo) / (hi - lo)) * (H - pt - pb);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${x(p, i).toFixed(1)},${y(p.v).toFixed(1)}`).join('');
  const area = `${line}L${x(pts[pts.length - 1], pts.length - 1).toFixed(1)},${H - pb}L${pl},${H - pb}Z`;
  const ticks = [lo, (lo + hi) / 2, hi];
  const grid = ticks.map(v => `<line class="grid" x1="${pl}" x2="${W - pr}" y1="${y(v)}" y2="${y(v)}"/><text x="${pl - 6}" y="${y(v) + 4}" text-anchor="end">${fmt(v, hi - lo < 10 ? Math.max(1, dec) : 0)}</text>`).join('');
  const showAll = pts.length <= 40;
  const dots = pts.map((p, i) => i === pts.length - 1 || showAll ? `<circle class="pt ${i === pts.length - 1 ? 'last' : ''}" cx="${x(p, i)}" cy="${y(p.v)}" r="${i === pts.length - 1 ? 4.5 : 3}"/>` : '').join('');
  const goalLine = goal != null ? `<line class="goal" x1="${pl}" x2="${W - pr}" y1="${y(goal)}" y2="${y(goal)}"/><text class="goal-t" x="${W - pr}" y="${y(goal) - 4}" text-anchor="end">meta ${fmt(goal, dec)}</text>` : '';
  const lastV = pts[pts.length - 1].v, firstV = pts[0].v, diff = lastV - firstV;
  const good = better === 'up' ? diff >= 0 : better === 'down' ? diff <= 0 : false;
  return `<div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px">
      <div><b style="font-size:24px" class="num">${fmt(lastV, dec)}</b> <span class="muted small">${unit}</span></div>
      <span class="badge ${good ? 'accent' : ''} num">${diff >= 0 ? '+' : '−'}${fmt(Math.abs(diff), dec)} ${unit}</span></div>
    <svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Gráfico de evolução">
      ${grid}${goalLine}<path class="area" d="${area}"/><path class="line" pathLength="1" d="${line}"/>${dots}
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

// Valor sugerido para um campo vazio: a progressão automática, senão o último treino, senão a série anterior
function placeholder(ex, i, field) {
  // Drop set: ~20% menos carga que a série anterior
  if (ex.sets[i].t === 'drop' && i > 0) {
    const pv = ex.sets[i - 1][field] !== '' ? ex.sets[i - 1][field] : placeholder(ex, i - 1, field);
    if (field !== 'a' || num(pv) == null || typeof loadStep !== 'function') return pv;
    const step = loadStep(getEx(ex.exId));
    return fmtIn(Math.max(step, roundTo(num(pv) * 0.8, step)));
  }
  if (!ex.sets[i].warm) {
    const wi = ex.sets.slice(0, i).filter(x => !x.warm).length;
    const sg = typeof suggestionFor === 'function' ? suggestionFor(ex) : null;
    const ref = sg && sg.sets.length ? sg.sets[wi] || sg.sets[sg.sets.length - 1] : null;
    if (ref && ref[field] != null) return fmtIn(ref[field]);
    const last = lastSets(ex.exId), old = last[wi] || last[last.length - 1];
    if (old && old[field] != null) return fmtIn(old[field]);
  }
  for (let j = i - 1; j >= 0; j--) if (ex.sets[j][field] !== '') return ex.sets[j][field];
  return '';
}
// Tipo da série: normal (''), aquecimento, drop set ou até a falha
const SET_TYPES = [
  ['', 'Normal', 'Conta no volume, nos recordes e na progressão'],
  ['warm', 'Aquecimento', 'Não conta no volume nem nos recordes', 'A'],
  ['drop', 'Drop set', 'Reduz a carga e continua sem descanso; conta no volume', 'D'],
  ['fail', 'Até a falha', 'Série levada até não conseguir mais nenhuma repetição', 'F']
];
const setType = s => s.warm ? 'warm' : s.t || '';
const setMark = s => (SET_TYPES.find(t => t[0] === setType(s)) || [])[3];

let justDone = null; // série recém-concluída (anima o ✓)
function viewTreino() {
  const pop = justDone; justDone = null;
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
    const rir = S.settings.rir !== false && (ex.kind === 'w' || ex.kind === 'bw');
    const head = `<tr><th>Série</th><th>Anterior</th><th>${K.a}</th>${K.b ? `<th>${K.b}</th>` : ''}${rir ? '<th><button class="th-btn" data-act="rirHelp">RIR</button></th>' : ''}<th>✓</th></tr>`;
    let wn = 0, wi = 0;
    const rows = ex.sets.map((s, i) => {
      const ty = setType(s), label = setMark(s) || String(++wn);
      const ref = s.warm ? null : last[wi++];
      const prev = ref ? fmtSet(ex.kind, ref) : '—';
      const inp = f => `<input type="text" inputmode="decimal" value="${esc(s[f])}" placeholder="${esc(placeholder(ex, i, f))}" data-wf="${f}" data-x="${x}" data-s="${i}" aria-label="${f === 'a' ? K.a : K.b} da série ${label}">`;
      const rirSel = `<select class="rir" data-rir data-x="${x}" data-s="${i}" aria-label="RIR da série ${label}">${['', 0, 1, 2, 3, 4, 5].map(v => `<option value="${v}" ${String(s.rir ?? '') === String(v) ? 'selected' : ''}>${v === '' ? '–' : v === 5 ? '5+' : v}</option>`).join('')}</select>`;
      return `<tr class="${s.done ? 'done' : ''} ${pop === `${x}-${i}` ? 'pop' : ''}">
        <td><button class="setno ${ty}" data-act="setMenu" data-x="${x}" data-s="${i}" aria-label="Tipo da série ${label}">${label}</button></td>
        <td class="prev num">${prev}</td>
        <td>${inp('a')}</td>${K.b ? `<td>${inp('b')}</td>` : ''}${rir ? `<td class="rirc">${s.warm ? '' : rirSel}</td>` : ''}
        <td><button class="check" data-act="toggleSet" data-x="${x}" data-s="${i}" aria-label="Concluir série">${I.check}</button></td></tr>`;
    }).join('');
    const sg = typeof suggestionFor === 'function' && !allDone ? suggestionFor(ex) : null;
    const targetTxt = [ex.sets.filter(s => !s.warm).length + ' × ' + (ex.target || '—') + (ex.kind === 's' ? 's' : ex.kind === 'c' ? ' min' : ''), `descanso ${fmtRest(ex.rest)}`].join(' · ');
    html += `<div class="ex-card ${allDone ? 'complete' : ''} ${allDone && pop && pop.split('-')[0] === String(x) ? 'just-complete' : ''}">
      <div class="ex-title"><button class="thumb-btn" data-act="howTo" data-id="${ex.exId}" aria-label="Ver execução">${thumb(getEx(ex.exId))}</button>
        <div class="grow"><a class="name" href="#/exercicio/${ex.exId}">${esc(ex.name)}</a><div class="target num">${targetTxt}</div></div>
        <button class="icon-btn" data-act="exMenu" data-x="${x}" aria-label="Opções do exercício" style="margin:-8px -6px 0 0">${I.more}</button></div>
      ${ex.note ? `<div class="ex-note">${esc(ex.note)}</div>` : ''}
      ${sg ? suggestionHTML(sg) : ''}
      <table class="sets ${rir ? 'has-rir' : ''}"><colgroup><col class="c-no"><col class="c-prev"><col>${K.b ? '<col>' : ''}${rir ? '<col class="c-rir">' : ''}<col class="c-ok"></colgroup>${head}${rows}</table>
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
  if (S.settings.iosTimer) iosTimerRun(sec);
}

/* ================= Tela ligada e Timer do iPhone ================= */
// Mantém a tela acesa durante o treino (Screen Wake Lock), para o cronômetro não parar.
let wakeLock = null;
function syncWakeLock() {
  const want = S.settings.keepAwake && S.active && !document.hidden;
  if (want && !wakeLock && 'wakeLock' in navigator) {
    wakeLock = 'pedindo';
    navigator.wakeLock.request('screen')
      .then(l => {
        wakeLock = l;
        l.addEventListener('release', () => { if (wakeLock === l) wakeLock = null; });
        if (!(S.settings.keepAwake && S.active && !document.hidden)) syncWakeLock(); // mudou enquanto pedia
      })
      .catch(() => { wakeLock = null; });
  } else if (!want && wakeLock && wakeLock !== 'pedindo') {
    wakeLock.release().catch(() => {}); wakeLock = null;
  }
}

// Web apps não podem criar Live Activities (só apps nativos). O app Atalhos do iPhone pode:
// um atalho com a ação "Iniciar Timer" liga o Timer do relógio, que aparece na tela bloqueada
// e na Dynamic Island e toca o alarme do iPhone. O app chama o atalho pelo link shortcuts://.
function iosTimerRun(sec) {
  const name = (S.settings.timerShortcut || '').trim() || 'Descanso Ficha';
  location.href = `shortcuts://run-shortcut?name=${encodeURIComponent(name)}&input=text&text=${Math.max(1, Math.round(sec))}`;
}
function iosTimerHelp() {
  openSheet(`${sheetHead('Timer do iPhone')}<div class="sheet-body">
    <p class="muted" style="margin:0 0 14px">Ao terminar uma série, o app usa o <b>Atalhos</b> para iniciar o <b>Timer do relógio</b> com o tempo de descanso. É o timer de verdade do iPhone: aparece na tela bloqueada e na Dynamic Island e toca o alarme do relógio.</p>
    <div class="look-label">Configure uma vez</div>
    <ol class="steps">
      <li>Abra o app <b>Atalhos</b> e toque em <b>+</b> para criar um atalho.</li>
      <li>Toque em <b>Adicionar Ação</b>, busque <b>Iniciar Timer</b> (do Relógio) e adicione.</li>
      <li>Toque na duração, escolha <b>Entrada do Atalho</b> e mude a unidade para <b>segundos</b>.</li>
      <li>Toque no nome no topo, renomeie para <b data-ios-name>${esc(S.settings.timerShortcut || 'Descanso Ficha')}</b> e toque em OK.</li>
    </ol>
    <label class="field"><span>Nome do atalho</span>
      <input class="input" data-ios-shortcut value="${esc(S.settings.timerShortcut || '')}" placeholder="Descanso Ficha" autocapitalize="words" autocomplete="off"></label>
    <p class="small muted" style="margin:4px 0 0;line-height:1.5">O iPhone abre o Atalhos por um instante; para voltar, toque em <b>◀</b> no canto superior esquerdo. Se ele perguntar se pode abrir o Atalhos, toque em <b>Abrir</b>. Os botões −15, +15 e Pular mudam só o descanso do app — toque no tempo do descanso para enviar o novo tempo ao iPhone.</p>
  </div>
  <div class="sheet-foot stack">
    <button class="btn block primary" data-act="iosTimerTest">Testar com 10 segundos</button>
    <button class="btn block" data-act="iosTimerCreate">Abrir o Atalhos para criar</button>
  </div>`);
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
        if (s.t) o.t = s.t;
        const rir = num(s.rir);
        if (rir != null && !s.warm) o.rir = rir;
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

// Confete (comemoração no fim do treino)
function confetti(n = 80) {
  if (reduceMotion() || !Element.prototype.animate) return;
  const cs = getComputedStyle(document.documentElement);
  const colors = [cs.getPropertyValue('--accent').trim(), cs.getPropertyValue('--gold').trim(), '#4DA3FF', '#FF6FAE', '#34D399', '#FFFFFF'];
  const box = document.createElement('div');
  box.className = 'confetti'; document.body.appendChild(box);
  const W = innerWidth, H = innerHeight;
  for (let i = 0; i < n; i++) {
    const p = document.createElement('i'), x0 = W / 2 + (Math.random() - .5) * 60, y0 = H * .32;
    const dx = (Math.random() - .5) * W * 1.1, up = 120 + Math.random() * 220, rot = (Math.random() - .5) * 900;
    p.style.cssText = `left:${x0}px;top:${y0}px;background:${colors[i % colors.length]};width:${6 + Math.random() * 5}px;height:${8 + Math.random() * 8}px`;
    box.appendChild(p);
    p.animate([
      { transform: 'translate(0,0) rotate(0)', opacity: 1 },
      { transform: `translate(${dx * .55}px,${-up}px) rotate(${rot * .4}deg)`, opacity: 1, offset: .28 },
      { transform: `translate(${dx}px,${H - y0 + 60}px) rotate(${rot}deg)`, opacity: .85 }
    ], { duration: 1700 + Math.random() * 1100, easing: 'cubic-bezier(.25,.6,.45,1)', delay: Math.random() * 120, fill: 'forwards' });
  }
  setTimeout(() => box.remove(), 3200);
}
// Números que sobem até o valor (resumo do treino)
function countUp(root) {
  if (reduceMotion()) return;
  root.querySelectorAll('[data-count]').forEach(el => {
    const to = +el.dataset.count, f = el.dataset.fmt === 'int' ? fmtInt : v => String(Math.round(v)), t0 = performance.now();
    const step = t => { const k = Math.min(1, (t - t0) / 800), e = 1 - (1 - k) ** 3; el.textContent = f(to * e); if (k < 1) requestAnimationFrame(step); };
    requestAnimationFrame(step);
  });
}
function showSummary(sess) {
  const vol = sessVolume(sess);
  const prs = sess.prs || [];
  openSheet(`<div class="sheet-body"><div class="summary-big">
      <div class="trophy-ic" style="display:inline-grid;place-items:center;width:64px;height:64px;border-radius:20px;background:var(--accent);color:var(--accent-ink)">
        <svg viewBox="0 0 24 24" style="width:34px;height:34px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round">${I.trophy.replace(/<\/?svg[^>]*>/g, '')}</svg></div>
      <h3>Treino concluído!</h3><div class="muted">${esc(sess.name)}</div></div>
    <div class="stats" style="margin-top:16px">
      <div class="stat"><b class="num">${fmtDur(sess.end - sess.start)}</b><span>duração</span></div>
      <div class="stat"><b class="num" data-count="${sessSetCount(sess)}">${sessSetCount(sess)}</b><span>séries</span></div>
      <div class="stat"><b class="num" ${vol ? `data-count="${Math.round(vol)}" data-fmt="int"` : ''}>${vol ? fmtInt(vol) : '—'}</b><span>kg de volume</span></div></div>
    ${prs.length ? `<h2 class="section">Recordes pessoais 🎉</h2><div class="list">${prs.map(p => `<div class="kv"><span>${esc(p.name)} · ${p.label}</span><b style="color:var(--gold)">${fmt(p.value)} ${p.unit}</b></div>`).join('')}</div>` : ''}
    </div><div class="sheet-foot stack">${backupDue() ? `<button class="btn block" data-act="exportData">${I.download}Salvar backup · ${unbackedSessions()} treinos sem backup</button>` : ''}
      <button class="btn primary block" data-act="closeSheet">Fechar</button></div>`);
  countUp($('#sheet'));
  setTimeout(() => confetti(prs.length ? 120 : 70), 150);
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
  if (typeof volumeHTML === 'function') html += volumeHTML();

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
      <div class="set-pills">${e.sets.map(x => setPill(e.kind, x, setMark(x) || ++wn)).join('')}</div></a>`;
  }).join('') + '</div>';
  if (s.notes) html += `<h2 class="section">Anotações</h2><div class="card" style="white-space:pre-wrap">${esc(s.notes)}</div>`;
  html += `<div class="stack" style="margin-top:18px"><button class="btn primary block" data-act="repeatSession" data-id="${id}">${I.play}Repetir este treino</button></div>`;
  return html;
}

/* ================= Tela: Ajustes ================= */
function viewAjustes() {
  const standalone = window.navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  const scheme = schemeNow(), style = styleNow();
  const chosen = STYLES.some(x => x[0] === S.settings.style) ? S.settings.style : 'auto';
  return `<div class="top"></div><h1 class="title">Ajustes</h1>
    <h2 class="section">Aparência</h2>
    <div class="card look">
      <div class="look-label">Tema</div>
      <div class="seg" role="radiogroup" aria-label="Tema">${THEMES.map(([v, l]) => `<button class="${S.settings.theme === v ? 'on' : ''}" data-act="setTheme" data-v="${v}" role="radio" aria-checked="${S.settings.theme === v}">${l}</button>`).join('')}</div>
      <div class="look-label">Estilo</div>
      <div class="seg" role="radiogroup" aria-label="Estilo">${STYLES.map(([v, l, name]) => `<button class="${chosen === v ? 'on' : ''}" data-act="setStyle" data-v="${v}" role="radio" aria-checked="${chosen === v}" aria-label="${name}">${l}</button>`).join('')}</div>
      <p class="small muted look-desc">${chosen === 'auto'
        ? `${STYLES[0][3]}. Neste aparelho: <b>${STYLES.find(x => x[0] === style)[2]}</b>.`
        : `<b>${STYLES.find(x => x[0] === chosen)[2]}:</b> ${STYLES.find(x => x[0] === chosen)[3].replace(/^./, c => c.toLowerCase())}.`}</p>
      <div class="look-label">${style === 'material' ? 'Cor (a paleta é gerada a partir dela)' : 'Cor de destaque'}</div>
      <div class="swatches" role="radiogroup" aria-label="Cor de destaque">${ACCENTS.map(a => {
        const on = (S.settings.accent || 'limao') === a.id;
        const c = style === 'material' ? MD.swatch(mdSeed(a), scheme) : a[scheme];
        const vars = style === 'material' ? `--sw:${c[0]};--sw2:${c[1]};--sw3:${c[2]};--sw-ink:${c[3]}` : `--sw:${c[0]};--sw-ink:${c[1]}`;
        return `<button class="sw ${on ? 'on' : ''}" data-act="setAccent" data-v="${a.id}" role="radio" aria-checked="${on}" style="${vars}"><i>${on ? I.check : ''}</i><span>${a.name}</span></button>`;
      }).join('')}</div>
    </div>

    <h2 class="section">Treino</h2>
    <div class="list">
      <label class="row"><div class="grow"><div class="name">Descanso padrão</div><div class="sub">Usado em treino livre e exercícios novos</div></div>
        <select class="input" style="width:auto;min-height:38px;padding:6px 34px 6px 12px" data-setting="rest">${REST_OPTIONS.filter(Boolean).map(s => `<option value="${s}" ${s === S.settings.rest ? 'selected' : ''}>${fmtRest(s)}</option>`).join('')}</select></label>
      <label class="row"><div class="grow"><div class="name">Som ao fim do descanso</div><div class="sub">Alarme quando o descanso acaba</div></div>
        <span class="switch"><input type="checkbox" data-setting="sound" ${S.settings.sound ? 'checked' : ''}><i></i></span></label>
      <label class="row"><div class="grow"><div class="name">Sugerir carga e repetições</div><div class="sub wrap">Progressão automática: o app sugere quanto fazer em cada exercício com base no último treino</div></div>
        <span class="switch"><input type="checkbox" data-setting="progression" ${S.settings.progression !== false ? 'checked' : ''}><i></i></span></label>
      <label class="row"><div class="grow"><div class="name">Registrar RIR</div><div class="sub wrap">Repetições na reserva: quantas ainda sobravam ao fim da série. Opcional; deixa a progressão mais precisa</div></div>
        <span class="switch"><input type="checkbox" data-setting="rir" ${S.settings.rir !== false ? 'checked' : ''}><i></i></span></label>
      ${typeof openCalc === 'function' ? `<button class="row" data-act="calc" data-mode="plates"><div class="grow"><div class="name">Calculadoras</div><div class="sub">Anilhas, aquecimento e 1RM</div></div>${I.chev}</button>` : ''}
      <label class="row"><div class="grow"><div class="name">Manter a tela ligada no treino</div><div class="sub wrap">A tela não apaga sozinha enquanto há um treino em andamento</div></div>
        <span class="switch"><input type="checkbox" data-setting="keepAwake" ${S.settings.keepAwake ? 'checked' : ''}><i></i></span></label>
      <label class="row"><div class="grow"><div class="name">Usar o Timer do iPhone</div><div class="sub wrap">Ao terminar uma série, inicia o Timer do relógio pelo app Atalhos: aparece na tela bloqueada e na Dynamic Island, com o alarme do iPhone</div></div>
        <span class="switch"><input type="checkbox" data-setting="iosTimer" ${S.settings.iosTimer ? 'checked' : ''}><i></i></span></label>
      <button class="row" data-act="iosTimerHelp"><div class="grow"><div class="name">Configurar o Timer do iPhone</div><div class="sub">Atalho: “${esc(S.settings.timerShortcut || 'Descanso Ficha')}”</div></div>${I.chev}</button>
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
      <button class="row" data-act="exportData"><div class="grow"><div class="name">Exportar backup</div><div class="sub">${backupLabel()}</div></div>${I.chev}</button>
      <label class="row" style="cursor:pointer"><div class="grow"><div class="name">Importar backup</div><div class="sub">Substitui os dados atuais pelos do arquivo</div></div>${I.chev}
        <input type="file" accept="application/json,.json" id="importFile" hidden></label>
      ${prevCopy() ? `<button class="row" data-act="undoImport"><div class="grow"><div class="name">Desfazer a última importação</div><div class="sub">Volta aos dados de antes de importar (${dateShort(prevCopy().at)})</div></div>${I.chev}</button>` : ''}
      ${storageSafe != null ? `<div class="row"><div class="grow"><div class="name">Proteção contra limpeza automática</div><div class="sub wrap">${storageSafe ? 'Ativada: o sistema não apaga os dados do app para liberar espaço' : 'Não concedida pelo navegador: mantenha os backups em dia'}</div></div><span class="badge ${storageSafe ? 'accent' : 'gold'}">${storageSafe ? 'Ativa' : 'Sem'}</span></div>` : ''}
      <a class="row" href="#/modelos"><div class="grow"><div class="name">Modelos de treino prontos</div><div class="sub">PPL, Upper/Lower, ABC, ABCDE, em casa e mais</div></div>${I.chev}</a>
      <button class="row" data-act="wipeData"><div class="grow"><div class="name" style="color:var(--danger)">Apagar todos os dados</div></div></button>
    </div>
    <p class="small muted" style="margin:10px 4px 0">${S.programs.length} programas · ${S.routines.length} fichas · ${S.sessions.length} treinos · ${(S.body || []).length} registros de medidas · ${S.custom.length} exercícios personalizados. Tudo fica salvo só neste aparelho. Exporte um backup e guarde no iCloud Drive (app Arquivos) para não perder nada se trocar ou perder o celular.</p>

    ${standalone ? '' : `<h2 class="section">Instalar no iPhone</h2>
    <div class="card small" style="line-height:1.55">
      1. Abra este endereço no <b>Safari</b>.<br>
      2. Toque em <b>Compartilhar</b> (quadrado com seta para cima).<br>
      3. Escolha <b>Adicionar à Tela de Início</b> e confirme.<br>
      <span class="muted">O app abre em tela cheia, funciona offline e mantém seus dados.</span></div>`}

    <div style="text-align:center;margin-top:22px"><button class="link-btn" data-act="checkUpdate">Procurar atualização</button></div>
    <p class="small muted" style="text-align:center;margin-top:6px">Ficha · versão 2.0<br>
      Fotos e músculos dos exercícios: <a href="https://github.com/yuhonas/free-exercise-db" target="_blank" rel="noopener" style="text-decoration:underline">free-exercise-db</a> (domínio público)<br>
      Desenho do mapa muscular: <a href="https://github.com/GV79/react-body-highlighter" target="_blank" rel="noopener" style="text-decoration:underline">react-body-highlighter</a> (MIT)<br>
      Alimentos: TACO, 4ª ed. (NEPA/UNICAMP) e <a href="https://world.openfoodfacts.org" target="_blank" rel="noopener" style="text-decoration:underline">Open Food Facts</a> (ODbL)</p>`;
}

/* ================= Segurança dos dados ================= */
// Tudo fica no aparelho: pede ao navegador para não apagar os dados quando faltar espaço,
// lembra de fazer backup e guarda uma cópia antes de importar (para desfazer).
const PREV_KEY = 'ficha.prev';
let storageSafe = null; // true/false depois de perguntar ao navegador
function protectStorage() {
  if (!navigator.storage || !navigator.storage.persist) return;
  navigator.storage.persisted()
    .then(p => p || navigator.storage.persist())
    .then(p => { storageSafe = !!p; })
    .catch(() => { storageSafe = false; });
}
function unbackedSessions() { return S.sessions.filter(s => s.end > (S.settings.lastBackup || 0)).length; }
function backupDue() {
  return unbackedSessions() >= 3 && Date.now() - (S.settings.lastBackup || 0) > 7 * 864e5 && Date.now() > (S.settings.backupSnooze || 0);
}
function markBackup() {
  S.settings.lastBackup = Date.now(); S.settings.backupSnooze = 0; save();
  toast('Backup salvo'); if ($('#sheet').hidden) rerender();
}
function backupLabel() {
  const t = S.settings.lastBackup;
  return t ? `Último: ${relDay(t).toLowerCase()}${unbackedSessions() ? ` · ${unbackedSessions()} treino${unbackedSessions() > 1 ? 's' : ''} depois dele` : ''}` : 'Nenhum backup feito ainda';
}
function prevCopy() {
  try { const d = JSON.parse(localStorage.getItem(PREV_KEY)); return d && d.data && Date.now() - d.at < 30 * 864e5 ? d : null; } catch (e) { return null; }
}
// Estado a partir de um backup (ou da cópia de segurança)
function fromBackup(d) {
  const b = blank();
  migrate(d);
  return { ...b, settings: { ...b.settings, ...(d.settings || {}) }, custom: d.custom || [], videos: d.videos || {}, profile: d.profile || null, body: d.body || [], bodyGoal: d.bodyGoal || {}, food: d.food || b.food, programs: d.programs || [], routines: d.routines, sessions: d.sessions.sort((x, y) => y.start - x.start), active: d.active || null };
}

function exportData() {
  const data = JSON.stringify({ app: 'ficha', exportedAt: new Date().toISOString(), ...S }, null, 2);
  const d = new Date();
  const name = `ficha-backup-${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}.json`;
  const file = new File([data], name, { type: 'application/json' });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    navigator.share({ files: [file], title: 'Backup do Ficha' }).then(markBackup).catch(() => {});
    return;
  }
  const url = URL.createObjectURL(file);
  const a = document.createElement('a');
  a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  markBackup();
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
        try { localStorage.setItem(PREV_KEY, JSON.stringify({ at: Date.now(), data: S })); } catch (e) { /* sem espaço para a cópia */ }
        S = fromBackup(d);
        save(); applyLook(); toast('Backup importado — dá para desfazer em Ajustes'); go('#/hoje');
      }
    });
  };
  reader.readAsText(file);
}

/* ================= Dock (descanso / treino em andamento) ================= */
let dockSig = { rest: false, active: false }; // o que já estava no dock (para animar só o que aparece)
function renderDock() {
  const dock = $('#dock');
  const a = S.active;
  let html = '';
  if (a && a.rest && a.rest.end > Date.now()) {
    const left = (a.rest.end - Date.now()) / 1000, C = 2 * Math.PI * 19;
    html += `<div class="dock-bar rest ${dockSig.rest ? '' : 'in'}">
      <svg class="ring" viewBox="0 0 44 44"><circle class="bg" cx="22" cy="22" r="19"/><circle class="fg" cx="22" cy="22" r="19" stroke-dasharray="${C}" stroke-dashoffset="${C * (1 - left / a.rest.total)}" data-ring="${C}"/></svg>
      ${S.settings.iosTimer
        ? `<button class="grow" data-act="iosTimerSync" aria-label="Enviar o tempo ao Timer do iPhone"><div class="lbl">Descanso ↻</div><div class="big num" data-rest>${clock(left)}</div></button>`
        : `<div class="grow"><div class="lbl">Descanso</div><div class="big num" data-rest>${clock(left)}</div></div>`}
      <button class="btn sm" data-act="restAdd" data-d="-15">−15</button>
      <button class="btn sm" data-act="restAdd" data-d="15">+15</button>
      <button class="btn sm" data-act="restSkip">Pular</button></div>`;
  }
  if (a && location.hash !== '#/treino') {
    html += `<a class="dock-bar ${dockSig.active ? '' : 'in'}" href="#/treino"><div class="grow"><div class="lbl">Treino em andamento</div>
      <div style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(a.name)} · <span class="num" data-elapsed="${a.start}">${clock((Date.now() - a.start) / 1000)}</span></div></div>
      <span class="btn sm primary" style="background:var(--accent);color:var(--accent-ink)">Abrir</span></a>`;
  }
  dock.innerHTML = html;
  dockSig = { rest: !!dock.querySelector('.rest'), active: !!dock.querySelector('a.dock-bar') };
}

function tick() {
  const now = Date.now();
  document.querySelectorAll('[data-elapsed]').forEach(el => { el.textContent = clock((now - +el.dataset.elapsed) / 1000); });
  const a = S.active;
  if (a && a.rest) {
    const left = (a.rest.end - now) / 1000;
    if (left <= 0) {
      a.rest = null; save(); renderDock();
      if (!S.settings.iosTimer) beep(); // com o Timer do iPhone, quem toca é o relógio
      toast('Descanso terminado — próxima série!');
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
  setStyle: el => { S.settings.style = el.dataset.v; save(); applyLook(); rerender(); },
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
    justDone = `${el.dataset.x}-${i}`;
    unlockAudio();
    const next = ex.sets[i + 1];
    if (!(next && next.t === 'drop' && !next.done)) startRest(ex.rest); // drop set vem sem descanso
    save(); rerender();
  },
  setMenu: el => {
    const x = +el.dataset.x, i = +el.dataset.s, ex = S.active.exercises[x], cur = setType(ex.sets[i]);
    openSheet(`${sheetHead('Tipo de série')}<div class="sheet-body"><div class="list">${SET_TYPES.map(([v, l, d, m]) => `
      <button class="row" data-act="setType" data-x="${x}" data-s="${i}" data-v="${v}"><span class="setno ${v}">${m || '1'}</span>
        <div class="grow"><div class="name">${l}</div><div class="sub wrap">${d}</div></div>${cur === v ? I.check.replace('<svg', '<svg class="chev" style="stroke:var(--accent-text)"') : ''}</button>`).join('')}</div>
      ${ex.sets.length > 1 ? `<button class="btn danger block" style="margin-top:14px" data-act="setDelete" data-x="${x}" data-s="${i}">${I.trash}Remover esta série</button>` : ''}</div>`);
  },
  setType: el => {
    const s = S.active.exercises[+el.dataset.x].sets[+el.dataset.s], v = el.dataset.v;
    s.warm = v === 'warm';
    if (v === 'drop' || v === 'fail') s.t = v; else delete s.t;
    if (v === 'fail' && (s.rir == null || s.rir === '')) s.rir = '0';
    if (s.warm) delete s.rir;
    save(); closeSheet(); rerender();
  },
  setDelete: el => {
    const ex = S.active.exercises[+el.dataset.x];
    if (ex.sets.length > 1) ex.sets.splice(+el.dataset.s, 1);
    save(); closeSheet(); rerender();
  },
  rirHelp: () => openSheet(`${sheetHead('RIR: repetições na reserva')}<div class="sheet-body">
    <p style="margin:0 0 12px;line-height:1.5">Quantas repetições você ainda conseguiria fazer ao terminar a série, com boa execução.</p>
    <div class="list">${[['0', 'Falha: não sairia mais nenhuma'], ['1–2', 'Bem perto da falha (o ideal para hipertrofia)'], ['3–4', 'Moderado: sobrou um pouco'], ['5+', 'Leve: sobrou bastante']].map(([v, t]) => `<div class="kv"><span>${t}</span><b>${v}</b></div>`).join('')}</div>
    <p class="small muted" style="margin:12px 2px 0;line-height:1.5">É opcional. Quando você registra, a progressão automática acerta melhor o próximo passo: se sobrou muito, ela sobe mais a carga. Dá para esconder em Ajustes → Treino.</p></div>`),
  addWarmup: el => {
    const x = +el.dataset.x, ex = S.active.exercises[x], g = getEx(ex.exId);
    const i0 = ex.sets.findIndex(s => !s.warm);
    const W = i0 < 0 ? null : num(ex.sets[i0].a) || num(placeholder(ex, i0, 'a'));
    closeSheet();
    if (!W) { toast('Informe a carga da primeira série'); return; }
    const barbell = g && g.equip === 'Barra';
    const sets = warmupSets(W, barbell ? barNow() : 0, loadStep(g));
    if (!sets.length) { toast('Carga leve demais para aquecimento'); return; }
    ex.sets = sets.map(w => ({ a: fmtIn(w.w), b: String(w.r), done: false, warm: true })).concat(ex.sets.filter(s => !s.warm || s.done));
    save(); rerender();
    toast(`${sets.length} séries de aquecimento até ${fmt(W, 2)} kg`);
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
        ${ex.kind === 'w' && typeof warmupSets === 'function' ? `<button class="row" data-act="addWarmup" data-x="${x}"><div class="grow"><div class="name">Adicionar aquecimento</div><div class="sub">Séries leves calculadas pela carga de trabalho</div></div></button>
        <button class="row" data-act="calc" data-mode="plates" data-w="${esc(num(ex.sets.find(s => !s.warm && !s.done)?.a) || num(placeholder(ex, Math.max(0, ex.sets.findIndex(s => !s.warm)), 'a')) || '')}"><div class="grow name">Calculadora de anilhas</div></button>` : ''}
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
    if (S.settings.iosTimer && S.active.rest) toast('Toque no tempo para atualizar o Timer do iPhone');
  },
  restSkip: () => { if (S.active) { S.active.rest = null; save(); renderDock(); } },
  iosTimerSync: () => {
    const r = S.active && S.active.rest;
    if (r && r.end > Date.now()) iosTimerRun((r.end - Date.now()) / 1000);
  },
  iosTimerHelp: () => iosTimerHelp(),
  iosTimerTest: () => iosTimerRun(10),
  iosTimerCreate: () => { location.href = 'shortcuts://create-shortcut'; },

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
    onOk: () => { S = blank(); save(); localStorage.removeItem(PREV_KEY); applyLook(); if (typeof aiSetKey === 'function') aiSetKey(''); toast('Dados apagados'); go('#/hoje'); }
  }),
  undoImport: () => {
    const p = prevCopy();
    if (!p) return;
    confirmSheet({
      title: 'Desfazer a importação?', text: `Volta aos dados que estavam neste aparelho antes de importar (${dateShort(p.at)}): ${p.data.routines.length} fichas e ${p.data.sessions.length} treinos.`,
      ok: 'Desfazer importação', danger: true,
      onOk: () => { S = fromBackup(p.data); save(); localStorage.removeItem(PREV_KEY); applyLook(); toast('Dados anteriores restaurados'); go('#/hoje'); }
    });
  },
  backupLater: () => { S.settings.backupSnooze = Date.now() + 3 * 864e5; save(); rerender(); }
};

if (typeof ASSIST_ACTIONS !== 'undefined') Object.assign(ACT, ASSIST_ACTIONS);
if (typeof BODY_ACTIONS !== 'undefined') Object.assign(ACT, BODY_ACTIONS);
if (typeof TOOLS_ACTIONS !== 'undefined') Object.assign(ACT, TOOLS_ACTIONS);
if (typeof DIET_ACTIONS !== 'undefined') Object.assign(ACT, DIET_ACTIONS);

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
  if (typeof dietInput === 'function' && dietInput(t)) return;
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
  } else if (t.hasAttribute('data-ios-shortcut')) {
    S.settings.timerShortcut = t.value.trim(); save();
    const b = $('[data-ios-name]'); if (b) b.textContent = S.settings.timerShortcut || 'Descanso Ficha';
  } else if (t.dataset.calc) {
    if (typeof calcInput === 'function') calcInput(t);
  } else if (t.dataset.pf) {
    if (typeof onProfileInput === 'function') onProfileInput(t);
  } else if (t.id === 'pickq') {
    picker.q = t.value; $('#picklist').innerHTML = pickerList();
  }
});
document.addEventListener('change', e => {
  const t = e.target;
  if (typeof dietChange === 'function' && dietChange(t)) return;
  if (t.dataset.setting === 'rest') { S.settings.rest = +t.value; save(); toast('Descanso padrão atualizado'); }
  else if (t.dataset.setting === 'sound') { S.settings.sound = t.checked; save(); if (t.checked) { unlockAudio(); beep(); } }
  else if (t.dataset.setting === 'keepAwake') { S.settings.keepAwake = t.checked; save(); syncWakeLock(); }
  else if (t.dataset.setting === 'iosTimer') { S.settings.iosTimer = t.checked; save(); if (t.checked) iosTimerHelp(); }
  else if (t.dataset.setting === 'progression' || t.dataset.setting === 'rir') { S.settings[t.dataset.setting] = t.checked; save(); }
  else if (t.hasAttribute('data-rir') && S.active) {
    const s = S.active.exercises[+t.dataset.x].sets[+t.dataset.s];
    s.rir = t.value; save();
  }
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
document.addEventListener('visibilitychange', () => { if (!document.hidden) tick(); syncWakeLock(); });

/* ================= Início ================= */
applyLook();
setInterval(tick, 500);
route();
protectStorage();

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
