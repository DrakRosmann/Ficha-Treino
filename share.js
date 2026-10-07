/* Ficha — compartilhar o treino como imagem (Stories) e conquistas */
'use strict';

/* ================= Conquistas ================= */
// tier: 1 bronze · 2 prata · 3 ouro · 4 especial. p(st) → [atual, meta] para a barra de progresso.
const ACH_GROUPS = ['Treinos', 'Constância', 'Força', 'Recordes', 'Dieta', 'Corpo'];
const ACH = [
  { id: 'w1', g: 0, e: '🏁', t: 1, n: 'Primeiro treino', d: 'Registrou o primeiro treino', p: s => [s.n, 1] },
  { id: 'w10', g: 0, e: '💪', t: 1, n: '10 treinos', d: 'Dez treinos registrados', p: s => [s.n, 10] },
  { id: 'w50', g: 0, e: '🔥', t: 2, n: '50 treinos', d: 'Cinquenta treinos registrados', p: s => [s.n, 50] },
  { id: 'w100', g: 0, e: '💯', t: 3, n: '100 treinos', d: 'Cem treinos registrados', p: s => [s.n, 100] },
  { id: 'w250', g: 0, e: '🏛️', t: 4, n: '250 treinos', d: 'Duzentos e cinquenta treinos', p: s => [s.n, 250] },
  { id: 'v10', g: 0, e: '🧱', t: 1, n: '10 toneladas', d: '10.000 kg levantados no total', p: s => [s.vol / 1000, 10] },
  { id: 'v100', g: 0, e: '🏗️', t: 2, n: '100 toneladas', d: '100.000 kg levantados no total', p: s => [s.vol / 1000, 100] },
  { id: 'v1000', g: 0, e: '🌋', t: 4, n: '1.000 toneladas', d: 'Um milhão de quilos levantados', p: s => [s.vol / 1000, 1000] },
  { id: 'ss1', g: 0, e: '🔗', t: 1, n: 'Sem pausa', d: 'Fez uma supersérie ou circuito', p: s => [s.ss, 1] },
  { id: 'early', g: 0, e: '🌅', t: 1, n: 'Madrugador', d: 'Começou um treino antes das 6h30', p: s => [s.early, 1] },
  { id: 'late', g: 0, e: '🌙', t: 1, n: 'Coruja', d: 'Começou um treino depois das 22h', p: s => [s.late, 1] },
  { id: 'st4', g: 1, e: '📅', t: 1, n: '1 mês firme', d: '4 semanas seguidas treinando', p: s => [s.streak, 4] },
  { id: 'st12', g: 1, e: '🗓️', t: 2, n: '3 meses firme', d: '12 semanas seguidas treinando', p: s => [s.streak, 12] },
  { id: 'st26', g: 1, e: '⛰️', t: 3, n: 'Meio ano', d: '26 semanas seguidas treinando', p: s => [s.streak, 26] },
  { id: 'st52', g: 1, e: '👑', t: 4, n: 'Um ano inteiro', d: '52 semanas seguidas treinando', p: s => [s.streak, 52] },
  { id: 'pw', g: 1, e: '✅', t: 2, n: 'Semana perfeita', d: 'Treinou em todos os dias planejados da semana', p: s => [s.perfect, 1] },
  { id: 'bench1', g: 2, e: '🏋️', t: 2, n: 'Supino com o próprio peso', d: 'Supino reto com barra com carga igual ao seu peso', p: s => [s.bench, 1] },
  { id: 'squat15', g: 2, e: '🦵', t: 3, n: 'Agachamento 1,5×', d: 'Agachamento livre com 1,5 vez o seu peso', p: s => [s.squat, 1.5] },
  { id: 'dead2', g: 2, e: '⚡', t: 3, n: 'Terra 2×', d: 'Levantamento terra com o dobro do seu peso', p: s => [s.dead, 2] },
  { id: 'pr1', g: 3, e: '⭐', t: 1, n: 'Primeiro recorde', d: 'Bateu um recorde pessoal', p: s => [s.prs, 1] },
  { id: 'pr10', g: 3, e: '🌟', t: 2, n: '10 recordes', d: 'Dez recordes pessoais', p: s => [s.prs, 10] },
  { id: 'pr50', g: 3, e: '🏆', t: 3, n: '50 recordes', d: 'Cinquenta recordes pessoais', p: s => [s.prs, 50] },
  { id: 'd7', g: 4, e: '🥗', t: 1, n: 'Uma semana na dieta', d: '7 dias seguidos registrando a alimentação', p: s => [s.dietRun, 7] },
  { id: 'd30', g: 4, e: '🍽️', t: 3, n: 'Um mês na dieta', d: '30 dias seguidos registrando a alimentação', p: s => [s.dietRun, 30] },
  { id: 'prot7', g: 4, e: '🥩', t: 2, n: 'Proteína em dia', d: '7 dias seguidos batendo a meta de proteína', p: s => [s.protRun, 7] },
  { id: 'water7', g: 4, e: '💧', t: 1, n: 'Hidratado', d: '7 dias seguidos bebendo a meta de água', p: s => [s.waterRun, 7] },
  { id: 'body10', g: 5, e: '⚖️', t: 1, n: 'De olho na balança', d: 'Dez registros de medidas', p: s => [s.body, 10] },
  { id: 'photo3', g: 5, e: '📸', t: 1, n: 'Antes e depois', d: 'Fotos do progresso em 3 datas', p: s => [s.photoDays, 3] }
];
const TIER = [null, ['#E7A774', '#9A5B2E'], ['#E9EEF3', '#8D99A6'], ['#FFE07A', '#C8961C'], ['#C9B6FF', '#6C4BD8']];
const TIER_NAME = [null, 'Bronze', 'Prata', 'Ouro', 'Especial'];

// Maior sequência de dias seguidos (chaves AAAA-MM-DD) que passam no teste
function runOfDays(keys) {
  const ts = [...new Set(keys)].map(k => { const [y, m, d] = k.split('-').map(Number); return new Date(y, m - 1, d).getTime(); }).sort((a, b) => a - b);
  let best = 0, cur = 0, prev = null;
  for (const t of ts) { cur = prev != null && addDays(prev, 1) === t ? cur + 1 : 1; best = Math.max(best, cur); prev = t; }
  return best;
}
function bestLoad(exId) {
  let m = 0;
  for (const s of S.sessions) for (const e of s.exercises) if (e.exId === exId) for (const x of e.sets) if (!x.warm && (x.b || 0) >= 1) m = Math.max(m, x.a || 0);
  return m;
}
function achStats() {
  const ss = S.sessions, st = { n: ss.length, vol: 0, prs: 0, ss: 0, early: 0, late: 0 };
  const weeks = new Set();
  for (const s of ss) {
    st.vol += sessVolume(s); st.prs += (s.prs || []).length;
    if (s.exercises.some(e => e.ss)) st.ss = 1;
    const d = new Date(s.start), h = d.getHours() + d.getMinutes() / 60;
    if (h < 6.5 && h >= 3) st.early = 1;
    if (h >= 22) st.late = 1;
    weeks.add(startOfWeek(s.start));
  }
  // Semanas seguidas (a maior sequência)
  let best = 0, cur = 0, prev = null;
  for (const w of [...weeks].sort((a, b) => a - b)) { cur = prev != null && addDays(prev, 7) === w ? cur + 1 : 1; best = Math.max(best, cur); prev = w; }
  st.streak = best;
  // Semana perfeita: todos os dias planejados (2 ou mais) feitos, nas últimas 12 semanas
  const plan = [...new Set(S.routines.filter(r => typeof isScheduled !== 'function' || isScheduled(r)).flatMap(r => r.days || []))];
  st.perfect = 0;
  if (plan.length >= 2) {
    const today = startOfDay(Date.now());
    for (let k = 0; k < 12 && !st.perfect; k++) {
      const wk = addDays(startOfWeek(today), -7 * k);
      const days = plan.map(d => addDays(wk, (d + 6) % 7));
      if (days.every(t => t <= today && sessionsInRange(t, addDays(t, 1)).length)) st.perfect = 1;
    }
  }
  // Força relativa ao peso atual
  const bw = typeof dietWeight === 'function' ? dietWeight() : num((S.profile || {}).peso);
  st.bench = bw ? bestLoad('supino-reto-barra') / bw : 0;
  st.squat = bw ? bestLoad('agachamento') / bw : 0;
  st.dead = bw ? bestLoad('terra') / bw : 0;
  // Dieta
  const days = (S.food && S.food.days) || {}, keys = Object.keys(days);
  st.dietRun = runOfDays(keys.filter(k => days[k].e && days[k].e.length));
  const tg = typeof dietTargets === 'function' ? dietTargets() : null;
  st.protRun = tg ? runOfDays(keys.filter(k => sum((days[k].e || []).map(x => x.p || 0)) >= tg.p * 0.95)) : 0;
  const wGoal = Math.round(((typeof dietWeight === 'function' && dietWeight()) || 70) * 35 / 50) * 50;
  st.waterRun = runOfDays(keys.filter(k => (days[k].w || 0) >= wGoal));
  // Corpo
  st.body = (S.body || []).length;
  st.photoDays = new Set((S.photos || []).map(p => startOfDay(p.t))).size;
  return st;
}
const achDone = (a, st) => { const [c, g] = a.p(st); return c >= g; };

// Marca as novas conquistas. Na primeira vez (dados de antes desta versão), marca em silêncio.
function achCheck() {
  const st = achStats(), first = !S.ach;
  if (first) S.ach = {};
  const fresh = [];
  for (const a of ACH) if (!S.ach[a.id] && achDone(a, st)) { S.ach[a.id] = first ? 1 : Date.now(); if (!first) fresh.push(a.id); }
  return fresh;
}
let achTimer = 0;
function achSoon() {
  clearTimeout(achTimer);
  achTimer = setTimeout(() => {
    if (S.active) return; // no meio do treino, o resumo final mostra
    const n = achCheck();
    if (!n.length) return;
    save();
    const a = ACH.find(x => x.id === n[0]);
    toast(n.length > 1 ? `${a.e} ${n.length} conquistas novas! Veja no Histórico` : `${a.e} Conquista: ${a.n}`);
  }, 1200);
}

function achBadge(a, on, size = '') {
  const [c1, c2] = TIER[a.t];
  return `<span class="ach-b ${on ? '' : 'off'} ${size}" style="--t1:${c1};--t2:${c2}"><i>${a.e}</i></span>`;
}
function achBadgesHTML(ids) {
  const list = ids.map(id => ACH.find(a => a.id === id)).filter(Boolean);
  if (!list.length) return '';
  return `<div class="ach-new">${list.map(a => `<button class="ach-cell" data-act="achOpen" data-id="${a.id}">${achBadge(a, true)}<b>${esc(a.n)}</b></button>`).join('')}</div>`;
}
// Bloco do Histórico
function achHistHTML() {
  if (!S.ach) return '';
  const got = ACH.filter(a => S.ach[a.id]);
  const recent = [...got].sort((x, y) => S.ach[y.id] - S.ach[x.id]).slice(0, 4);
  const st = achStats();
  const next = ACH.filter(a => !S.ach[a.id]).map(a => { const [c, g] = a.p(st); return { a, f: Math.min(1, c / g) }; }).sort((x, y) => y.f - x.f)[0];
  return `<a class="card ach-sum" href="#/conquistas">
    <div class="ach-row">${recent.length ? recent.map(a => achBadge(a, true, 'sm')).join('') : ACH.slice(0, 4).map(a => achBadge(a, false, 'sm')).join('')}</div>
    <div class="grow"><div class="name">Conquistas</div><div class="sub">${got.length} de ${ACH.length}${next ? ` · próxima: ${esc(next.a.n)} (${Math.round(next.f * 100)}%)` : ''}</div></div>${I.chev}</a>`;
}
function viewConquistas() {
  if (!S.ach) achCheck();
  const st = achStats(), got = ACH.filter(a => S.ach[a.id]).length;
  let html = topBar({ back: '#/historico' }) + `<h1 class="title">Conquistas</h1>
    <div class="ach-head"><div class="ach-meter"><i style="width:${got / ACH.length * 100}%"></i></div><span class="num">${got} de ${ACH.length}</span></div>`;
  ACH_GROUPS.forEach((g, gi) => {
    html += `<h2 class="section">${g}</h2><div class="ach-grid">` + ACH.filter(a => a.g === gi).map(a => {
      const on = !!S.ach[a.id], [c, goal] = a.p(st), f = Math.max(0, Math.min(1, c / goal));
      return `<button class="ach-cell" data-act="achOpen" data-id="${a.id}">${achBadge(a, on)}<b>${esc(a.n)}</b>
        ${on ? '' : `<span class="ach-prog"><i style="width:${f * 100}%"></i></span>`}</button>`;
    }).join('') + '</div>';
  });
  return html;
}
function achProgressText(a, st) {
  const [c, g] = a.p(st);
  if (['bench1', 'squat15', 'dead2'].includes(a.id)) return c ? `Seu melhor hoje: ${fmt(c, 2)}× o seu peso` : 'Registre o exercício e o seu peso no Corpo';
  if (a.id.startsWith('v')) return `${fmt(c, c < 10 ? 1 : 0)} de ${fmtInt(g)} toneladas`;
  if (g === 1) return 'Ainda não';
  return `${fmtInt(Math.min(c, g))} de ${fmtInt(g)}`;
}

/* ================= Imagem para Stories ================= */
const SH_W = 1080, SH_H = 1920;
let shareCtx = null; // { kind, id, mode, photo, file, url }

function bestSetText(e) {
  const w = e.sets.filter(isWork);
  if (!w.length) return '';
  if (e.kind === 'w') {
    const b = w.reduce((x, y) => (y.a || 0) > (x.a || 0) || ((y.a || 0) === (x.a || 0) && (y.b || 0) > (x.b || 0)) ? y : x);
    return b.a ? `${fmt(b.a)} kg × ${b.b || 0}` : `${b.b || 0} reps`;
  }
  if (e.kind === 'bw') { const b = max(w.map(s => s.b || 0)), ex = max(w.map(s => s.a || 0)); return `${b} reps${ex ? ` · +${fmt(ex)} kg` : ''}`; }
  if (e.kind === 's') return `${max(w.map(s => s.a || 0))} s`;
  const km = sum(w.map(s => s.b || 0));
  return `${fmt(sum(w.map(s => s.a || 0)))} min${km ? ` · ${fmt(km)} km` : ''}`;
}
function shAccent() { return accentNow().dark[0]; }
function shFont(w, px) { return `${w} ${px}px -apple-system, BlinkMacSystemFont, "SF Pro Display", "Google Sans Flex", system-ui, "Segoe UI", Roboto, sans-serif`; }
function shWrap(c, text, maxW, maxLines) {
  const words = String(text).split(/\s+/), lines = [];
  let line = '';
  for (const w of words) {
    const t = line ? line + ' ' + w : w;
    if (c.measureText(t).width > maxW && line) { lines.push(line); line = w; } else line = t;
  }
  if (line) lines.push(line);
  if (lines.length > maxLines) {
    const keep = lines.slice(0, maxLines);
    let last = keep[maxLines - 1];
    while (c.measureText(last + '…').width > maxW && last.length) last = last.slice(0, -1);
    keep[maxLines - 1] = last + '…';
    return keep;
  }
  return lines;
}
function shFit(c, text, maxW) {
  let t = String(text);
  if (c.measureText(t).width <= maxW) return t;
  while (t.length && c.measureText(t + '…').width > maxW) t = t.slice(0, -1);
  return t + '…';
}
function shRound(c, x, y, w, h, r) { c.beginPath(); c.roundRect ? c.roundRect(x, y, w, h, r) : c.rect(x, y, w, h); }
function shBackground(c, mode, photo) {
  const acc = shAccent();
  if (mode === 'sticker') { c.clearRect(0, 0, SH_W, SH_H); return; }
  if (mode === 'photo' && photo) {
    const s = Math.max(SH_W / photo.width, SH_H / photo.height), w = photo.width * s, h = photo.height * s;
    c.drawImage(photo, (SH_W - w) / 2, (SH_H - h) / 2, w, h);
    const g = c.createLinearGradient(0, 0, 0, SH_H);
    g.addColorStop(0, 'rgba(0,0,0,.45)'); g.addColorStop(.3, 'rgba(0,0,0,.05)'); g.addColorStop(.5, 'rgba(0,0,0,.35)'); g.addColorStop(1, 'rgba(0,0,0,.88)');
    c.fillStyle = g; c.fillRect(0, 0, SH_W, SH_H);
    return;
  }
  const g = c.createLinearGradient(0, 0, 0, SH_H);
  g.addColorStop(0, '#15161B'); g.addColorStop(1, '#07080A');
  c.fillStyle = g; c.fillRect(0, 0, SH_W, SH_H);
  const glow = (x, y, r, a) => {
    const rg = c.createRadialGradient(x, y, 0, x, y, r);
    rg.addColorStop(0, acc + a); rg.addColorStop(1, acc + '00');
    c.fillStyle = rg; c.fillRect(0, 0, SH_W, SH_H);
  };
  glow(SH_W * .95, 80, 900, '55'); glow(-80, SH_H * .9, 800, '22');
}
function shShadow(c, on) { c.shadowColor = on ? 'rgba(0,0,0,.55)' : 'transparent'; c.shadowBlur = on ? 18 : 0; c.shadowOffsetY = on ? 2 : 0; }
function shFooter(c, y, mode) {
  c.textAlign = 'center'; c.fillStyle = mode === 'card' ? 'rgba(255,255,255,.45)' : 'rgba(255,255,255,.8)';
  c.font = shFont(600, 30); c.fillText('Registrado no Ficha', SH_W / 2, y);
  c.textAlign = 'left';
}
function shBadge(c, a, cx, cy, r) {
  const [c1, c2] = TIER[a.t], g = c.createLinearGradient(cx - r, cy - r, cx + r, cy + r);
  g.addColorStop(0, c1); g.addColorStop(1, c2);
  c.beginPath(); c.arc(cx, cy, r, 0, Math.PI * 2); c.fillStyle = g; c.fill();
  c.beginPath(); c.arc(cx, cy, r * .8, 0, Math.PI * 2); c.fillStyle = 'rgba(0,0,0,.18)'; c.fill();
  c.textAlign = 'center'; c.textBaseline = 'middle'; c.font = `${Math.round(r * .95)}px "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif`;
  c.fillStyle = '#fff'; c.fillText(a.e, cx, cy + r * .04);
  c.textAlign = 'left'; c.textBaseline = 'alphabetic';
}

function drawSession(c, s, mode, photo) {
  const acc = shAccent(), X = 96, W = SH_W - X * 2, sticker = mode === 'sticker', onPhoto = mode === 'photo';
  shBackground(c, mode, photo);
  const vol = sessVolume(s), prKeys = new Set((s.prs || []).map(p => p.exId));
  const exs = s.exercises.filter(e => e.sets.some(isWork));
  const maxRows = sticker ? 4 : onPhoto ? 5 : 8;
  const rows = exs.slice(0, maxRows);
  const ach = (s.ach || []).map(id => ACH.find(a => a.id === id)).filter(Boolean);
  // Altura do bloco, para posicionar (no fim da imagem quando há foto, no meio no adesivo)
  const titleLines = (() => { c.font = shFont(800, 100); return shWrap(c, s.name, W, 2); })();
  // Do topo do rótulo até o fim do último bloco
  const blockH = 34 + 160 + titleLines.length * 108 + 30 + 240 + rows.length * 96 + (exs.length > rows.length ? 60 : 0) + (ach.length && !sticker ? 200 : 0);
  let y = 34 + (onPhoto ? SH_H - 210 - blockH : Math.max(150, (SH_H - blockH) / 2 - (sticker ? 0 : 40)));
  shShadow(c, sticker || onPhoto);

  c.fillStyle = acc; c.font = shFont(800, 34);
  c.fillText('FICHA · TREINO CONCLUÍDO', X, y);
  c.fillStyle = 'rgba(255,255,255,.72)'; c.font = shFont(500, 36);
  c.fillText(`${dateLong(s.start)} · ${timeHM(s.start)}`, X, y + 54);
  y += 60 + 100;
  c.fillStyle = '#fff'; c.font = shFont(800, 100);
  titleLines.forEach(l => { c.fillText(l, X, y); y += 108; });
  y += 30;

  // Números
  const stats = [[fmtDur(s.end - s.start), 'duração'], [String(sessSetCount(s)), 'séries'], [vol ? (vol >= 10000 ? fmt(vol / 1000, 1) + ' t' : fmtInt(vol)) : '—', vol ? (vol >= 10000 ? 'de volume' : 'kg de volume') : 'volume']];
  if (!sticker && !onPhoto) { shRound(c, X - 8, y, W + 16, 190, 44); c.fillStyle = 'rgba(255,255,255,.06)'; c.fill(); }
  stats.forEach(([v, l], i) => {
    const cx = X + W / 6 + i * W / 3;
    c.textAlign = 'center'; c.fillStyle = i === 0 ? acc : '#fff'; c.font = shFont(800, 72);
    c.fillText(v, cx, y + 104);
    c.fillStyle = 'rgba(255,255,255,.62)'; c.font = shFont(600, 30); c.fillText(l, cx, y + 150);
  });
  c.textAlign = 'left';
  y += 190 + 50;

  // Exercícios
  if (rows.length) {
    rows.forEach((e, i) => {
      const best = bestSetText(e), pr = prKeys.has(e.exId);
      const top = y + i * 96;
      if (!sticker && !onPhoto && i) { c.fillStyle = 'rgba(255,255,255,.08)'; c.fillRect(X, top - 20, W, 2); }
      c.font = shFont(700, 40); const bw = c.measureText(best).width;
      c.fillStyle = 'rgba(255,255,255,.75)'; c.textAlign = 'right'; c.fillText(best, X + W, top + 44); c.textAlign = 'left';
      const prW = pr ? 92 : 0;
      c.fillStyle = '#fff'; c.font = shFont(600, 42);
      const nm = shFit(c, exName(e.exId, e.name), W - bw - prW - 40);
      c.fillText(nm, X, top + 44);
      if (pr) {
        const nx = X + c.measureText(nm).width + 18;
        shShadow(c, false); shRound(c, nx, top + 8, 74, 46, 23); c.fillStyle = '#FFD43B'; c.fill();
        c.fillStyle = '#231A00'; c.font = shFont(800, 28); c.fillText('PR', nx + 17, top + 41);
        shShadow(c, sticker || onPhoto);
      }
    });
    y += rows.length * 96;
    if (exs.length > rows.length) { c.fillStyle = 'rgba(255,255,255,.6)'; c.font = shFont(600, 34); c.fillText(`+ ${exs.length - rows.length} exercício${exs.length - rows.length > 1 ? 's' : ''}`, X, y + 30); y += 60; }
  }
  // Conquistas deste treino
  if (ach.length && !sticker) {
    y += 40;
    c.fillStyle = 'rgba(255,255,255,.7)'; c.font = shFont(700, 32); c.fillText(ach.length > 1 ? 'CONQUISTAS DESBLOQUEADAS' : 'CONQUISTA DESBLOQUEADA', X, y);
    y += 30;
    ach.slice(0, 3).forEach((a, i) => {
      const cx = X + 56 + i * (W / Math.min(3, ach.length)), cy = y + 70;
      shShadow(c, false); shBadge(c, a, cx, cy, 56); shShadow(c, onPhoto);
      c.fillStyle = '#fff'; c.font = shFont(700, 34);
      c.fillText(shFit(c, a.n, W / Math.min(3, ach.length) - 140), cx + 76, cy + 12);
    });
  }
  shFooter(c, SH_H - 90, mode);
}

function drawAch(c, a, mode, photo) {
  shBackground(c, mode, photo);
  const sticker = mode === 'sticker', onPhoto = mode === 'photo';
  shShadow(c, sticker || onPhoto);
  const cy = onPhoto ? SH_H - 760 : SH_H / 2 - 240;
  if (!sticker && !onPhoto) {
    const [c1] = TIER[a.t], rg = c.createRadialGradient(SH_W / 2, cy, 0, SH_W / 2, cy, 520);
    rg.addColorStop(0, c1 + '55'); rg.addColorStop(1, c1 + '00'); c.fillStyle = rg; c.fillRect(0, 0, SH_W, SH_H);
  }
  shShadow(c, false); shBadge(c, a, SH_W / 2, cy, 190); shShadow(c, sticker || onPhoto);
  c.textAlign = 'center';
  c.fillStyle = TIER[a.t][0]; c.font = shFont(800, 36);
  c.fillText(`CONQUISTA · ${TIER_NAME[a.t].toUpperCase()}`, SH_W / 2, cy + 300);
  c.fillStyle = '#fff'; c.font = shFont(800, 92);
  const nameLines = shWrap(c, a.n, SH_W - 160, 2);
  nameLines.forEach((l, i) => c.fillText(l, SH_W / 2, cy + 410 + i * 100));
  c.fillStyle = 'rgba(255,255,255,.72)'; c.font = shFont(500, 42);
  const lines = shWrap(c, a.d, SH_W - 220, 3), base = cy + 410 + (nameLines.length - 1) * 100 + 90;
  lines.forEach((l, i) => c.fillText(l, SH_W / 2, base + i * 56));
  const when = S.ach && S.ach[a.id] > 1 ? S.ach[a.id] : 0;
  if (when) { c.fillStyle = 'rgba(255,255,255,.5)'; c.font = shFont(600, 34); c.fillText(dateLong(when), SH_W / 2, base + lines.length * 56 + 40); }
  c.textAlign = 'left';
  shFooter(c, SH_H - 90, mode);
}

async function shareRender() {
  const sc = shareCtx;
  if (!sc) return;
  const cv = document.createElement('canvas');
  cv.width = SH_W; cv.height = SH_H;
  const c = cv.getContext('2d');
  try { await document.fonts.ready; } catch (e) { }
  if (sc.kind === 'session') { const s = S.sessions.find(x => x.id === sc.id); if (!s) return; drawSession(c, s, sc.mode, sc.photo); }
  else drawAch(c, ACH.find(a => a.id === sc.id), sc.mode, sc.photo);
  const blob = await new Promise(r => cv.toBlob(r, 'image/png'));
  if (shareCtx !== sc || !blob) return;
  if (sc.url) URL.revokeObjectURL(sc.url);
  sc.url = URL.createObjectURL(blob);
  sc.file = new File([blob], sc.kind === 'session' ? 'ficha-treino.png' : 'ficha-conquista.png', { type: 'image/png' });
  const img = $('#shImg'), dl = $('#shSave'), go = $('#shGo');
  if (img) { img.src = sc.url; img.closest('.sh-prev').classList.remove('loading'); img.closest('.sh-prev').classList.toggle('sticker', sc.mode === 'sticker'); }
  if (dl) { dl.href = sc.url; dl.download = sc.file.name; dl.classList.remove('off'); }
  if (go) go.disabled = false;
}
function openShare(kind, id) {
  const sc = { kind, id, mode: 'card', photo: null, file: null, url: null }, canShare = !!navigator.share;
  openSheet(`${sheetHead(kind === 'session' ? 'Compartilhar treino' : 'Compartilhar conquista')}
    <div class="sheet-body">
      <div class="seg sh-modes">
        <button class="on" data-act="shMode" data-v="card">Cartão</button>
        <button data-act="shMode" data-v="photo">Com foto</button>
        <button data-act="shMode" data-v="sticker">Adesivo</button></div>
      <input type="file" accept="image/*" id="shPhoto" data-share-photo hidden>
      <div class="sh-prev loading"><img id="shImg" alt="Prévia da imagem"></div>
      <p class="small muted sh-hint" style="text-align:center;margin:8px 0 0">Formato dos Stories (1080 × 1920). O adesivo tem fundo transparente para colar sobre a sua foto.</p>
    </div>
    <div class="sheet-foot stack">
      ${canShare ? `<button class="btn primary block" id="shGo" data-act="shGo" disabled>${I.share}Compartilhar</button>` : ''}
      <a class="btn block ${canShare ? '' : 'primary'} off" id="shSave" download>${I.download}Salvar imagem</a>
    </div>`, null, () => { if (sc.url) URL.revokeObjectURL(sc.url); if (shareCtx === sc) shareCtx = null; });
  shareCtx = sc;
  shareRender();
}
function shareChange(t) {
  if (!t.hasAttribute('data-share-photo')) return false;
  const f = t.files && t.files[0];
  t.value = '';
  if (!f || !shareCtx) return true;
  const url = URL.createObjectURL(f), img = new Image();
  img.onload = () => {
    if (!shareCtx) return;
    shareCtx.photo = img; shareCtx.mode = 'photo';
    document.querySelectorAll('.sh-modes button').forEach(b => b.classList.toggle('on', b.dataset.v === 'photo'));
    shareRender();
  };
  img.onerror = () => toast('Não foi possível abrir a foto');
  img.src = url;
  return true;
}

const SHARE_ACTIONS = {
  shareSession: el => openShare('session', el.dataset.id),
  achOpen: el => {
    const a = ACH.find(x => x.id === el.dataset.id), on = S.ach && S.ach[a.id], st = achStats();
    openSheet(`${sheetHead('Conquista')}<div class="sheet-body ach-detail">
      ${achBadge(a, !!on, 'lg')}
      <div class="ach-tier" style="color:${TIER[a.t][1]}">${TIER_NAME[a.t]}</div>
      <h3>${esc(a.n)}</h3><p class="muted">${esc(a.d)}</p>
      <p class="small ${on ? '' : 'muted'}">${on ? (on > 1 ? `Desbloqueada em ${dateLong(on)}` : 'Desbloqueada') : achProgressText(a, st)}</p></div>
      ${on ? `<div class="sheet-foot"><button class="btn primary block" data-act="achShare" data-id="${a.id}">${I.share}Compartilhar</button></div>` : ''}`);
  },
  achShare: el => openShare('ach', el.dataset.id),
  shMode: el => {
    if (!shareCtx) return;
    const v = el.dataset.v;
    if (v === 'photo' && !shareCtx.photo) { $('#shPhoto').click(); return; }
    shareCtx.mode = v;
    document.querySelectorAll('.sh-modes button').forEach(b => b.classList.toggle('on', b === el));
    $('.sh-prev').classList.add('loading');
    shareRender();
  },
  shGo: async () => {
    const sc = shareCtx;
    if (!sc || !sc.file) return;
    const data = { files: [sc.file] };
    try {
      if (navigator.canShare && !navigator.canShare(data)) throw new Error('nofiles');
      await navigator.share(data);
    } catch (e) {
      if (e && e.name === 'AbortError') return;
      $('#shSave').click();
    }
  }
};
