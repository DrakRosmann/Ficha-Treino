/* Ficha — movimento do Liquid Glass (iOS 26)
 * Molas físicas, tocadas no compositor: cada movimento é calculado uma vez (a mola inteira) e
 * entregue ao navegador como animação de transform/opacity, que roda fora do JavaScript, na taxa
 * da tela (até 120 Hz no iPhone com ProMotion) e não trava quando a tela troca de conteúdo.
 * - Barra de abas: a seleção escorre como gota (estica com a velocidade, passa do ponto e assenta);
 *   ao tocar vira lente que segue o dedo, amplia o ícone embaixo e estica no elástico das pontas.
 * - Controles de vidro crescem, seguem o dedo, brilham no ponto tocado e balançam ao soltar.
 * - Botões viram o painel que abrem; segmentado e interruptor deslizam como líquido.
 * - Navegação própria (sem View Transitions), para a barra de vidro continuar viva durante a troca.
 * Desligado no Clássico/Material e com "Reduzir movimento".
 */
'use strict';

const lqRoot = document.documentElement;
const lqReduce = matchMedia('(prefers-reduced-motion: reduce)');
const liquidOn = () => lqRoot.hasAttribute('data-glass') && !lqReduce.matches;
function lqSync() { lqRoot.classList.toggle('lq', liquidOn()); if (!liquidOn()) lqTabsOff(); }
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
// Elástico do iOS: quanto mais passa do limite, menos anda
const rubber = (d, dim) => (1 - 1 / (d * 0.55 / dim + 1)) * dim;

/* ================= Molas ================= */
// [rigidez, amortecimento] (massa 1). Mais rígido = mais rápido; menos amortecido = balança mais.
const LQ_SPRINGS = { press: [900, 52], tab: [600, 35], spring: [560, 38], bounce: [560, 25], soft: [520, 44], nav: [420, 40] };
// Simula a mola saindo de x0 (com velocidade v0) até 0. Amostras [t (s), x, v] a cada 1/240 s.
function springSim(x0, v0, k, c) {
  const dt = 1 / 240, out = [], eps = Math.max(0.0008, Math.abs(x0) * 0.0012);
  let x = x0, v = v0, t = 0;
  for (;;) {
    out.push([t, x, v]);
    const a = -k * x - c * v; v += a * dt; x += v * dt; t += dt;
    if ((t > 0.03 && Math.abs(x) < eps && Math.abs(v) < eps * 25) || t > 2) break;
  }
  out.push([t, 0, 0]);
  return out;
}
// A mola como curva do CSS/WAAPI: linear() com a resposta real (inclui o "passar do ponto")
function springCurve(k, c) {
  const tr = springSim(1, 0, k, c), T = tr[tr.length - 1][0], every = Math.max(1, Math.floor(tr.length / 60)), pts = [];
  for (let i = 0; i < tr.length - 1; i += every) pts.push(`${(1 - tr[i][1]).toFixed(4)} ${(tr[i][0] / T * 100).toFixed(2)}%`);
  pts.push('1 100%');
  return { easing: `linear(${pts.join(', ')})`, duration: Math.round(T * 1000) };
}
// Quadros-chave de uma simulação (no máximo ~90), com propriedades por quadro
function simFrames(tr, map) {
  const T = tr[tr.length - 1][0], step = Math.max(1, Math.round(tr.length / 90));
  return { frames: tr.filter((_, i) => i % step === 0 || i === tr.length - 1).map(([t, x, v]) => ({ offset: t / T, ...map(x, v) })), duration: Math.round(T * 1000) };
}
const LQ_EASE = {};
(() => {
  const ok = window.CSS && CSS.supports && CSS.supports('transition-timing-function', 'linear(0, 1)');
  const vars = [];
  for (const [name, [k, c]] of Object.entries(LQ_SPRINGS)) {
    LQ_EASE[name] = ok ? springCurve(k, c) : { easing: 'cubic-bezier(.3,1.25,.4,1)', duration: 420 };
    vars.push(`--lq-${name}: ${LQ_EASE[name].easing}; --lq-${name}-d: ${LQ_EASE[name].duration}ms;`);
  }
  // Numa folha própria: o applyLook reescreve as variáveis do estilo inline do <html>
  const st = document.createElement('style');
  st.id = 'lq-springs';
  st.textContent = `:root { ${vars.join(' ')} }`;
  document.head.appendChild(st);
})();

/* ================= Barra de abas: gota de vidro ================= */
const TAB = { idx: 0, w: 0, x: 0, to: 0, anim: null, traj: null, t0: 0, ready: false, drag: null, dragX: 0, raf: 0 };
const tabBar = () => document.getElementById('tabs');
const pillEl = () => { const b = tabBar(); return b && b.querySelector('.tab-pill'); };
function tabGeom() {
  const bar = tabBar(), n = bar.querySelectorAll('a').length || 1;
  return { bar, n, w: (bar.clientWidth - 8) / n };
}
// Posição + estique pela velocidade (e pelo elástico das pontas)
function pillTransform(x, v = 0, over = 0) {
  const st = Math.min(0.5, Math.abs(v) / 1700);
  return `translateX(${x.toFixed(2)}px) scale(${(1 + st + over * 0.7).toFixed(4)}, ${(1 - st * 0.28 - over * 0.2).toFixed(4)})`;
}
// Onde a gota está agora (também no meio de uma animação) e a que velocidade
function pillState() {
  if (TAB.anim && TAB.traj) {
    const tr = TAB.traj, i = Math.floor((performance.now() - TAB.t0) / 1000 * 240);
    if (i < tr.length - 1) return { x: TAB.to + tr[Math.max(0, i)][1], v: tr[Math.max(0, i)][2] };
  }
  return { x: TAB.x, v: 0 };
}
function pillStop() { if (TAB.anim) { TAB.anim.cancel(); TAB.anim = null; } }
function pillSet(x) { const p = pillEl(); pillStop(); TAB.x = TAB.to = x; if (p) p.style.transform = pillTransform(x); }
// Leva a gota até "to" com a mola, partindo de onde ela está (com a velocidade que tiver)
function pillGo(to, v0) {
  const pill = pillEl();
  if (!pill) return;
  const cur = pillState(), v = v0 != null ? v0 : cur.v;
  pillStop();
  TAB.x = TAB.to = to;
  pill.style.transform = pillTransform(to);
  if (Math.abs(cur.x - to) < 0.5 && Math.abs(v) < 5) return;
  const [k, c] = LQ_SPRINGS.tab, tr = springSim(cur.x - to, v, k, c);
  const { frames, duration } = simFrames(tr, (x, vv) => ({ transform: pillTransform(to + x, vv) }));
  TAB.traj = tr; TAB.t0 = performance.now();
  const a = TAB.anim = pill.animate(frames, { duration, easing: 'linear' });
  a.onfinish = () => { if (TAB.anim === a) TAB.anim = null; };
}
// Chamado a cada render com o índice da aba atual
function liquidTabs(ti) {
  const bar = tabBar();
  if (!bar) return;
  if (!liquidOn()) { lqTabsOff(); return; }
  bar.classList.add('liquid');
  const g = tabGeom(), idx = Math.max(0, ti), to = idx * g.w;
  TAB.idx = idx; TAB.w = g.w;
  if (TAB.drag) return;
  if (!TAB.ready || bar.classList.contains('mini') || bar.classList.contains('no-tab')) { TAB.ready = true; pillSet(to); return; }
  if (Math.abs(TAB.to - to) < 0.5 && (TAB.anim || Math.abs(TAB.x - to) < 0.5)) return;
  pillGo(to);
}
function lqTabsOff() {
  const bar = tabBar();
  if (!bar || !bar.classList.contains('liquid')) return;
  pillStop(); TAB.ready = false; TAB.drag = null;
  bar.classList.remove('liquid', 'press', 'dragging');
  const pill = pillEl();
  if (pill) pill.style.transform = '';
  bar.querySelectorAll('a').forEach(a => { a.classList.remove('near'); const s = a.querySelector('svg'); if (s) s.style.scale = ''; });
}
// A lente amplia o ícone que está embaixo dela
function lensIcons(g, x) {
  const pc = x + g.w / 2;
  let near = -1, best = 9;
  const links = g.bar.querySelectorAll('a');
  links.forEach((a, i) => {
    const d = Math.abs(i * g.w + g.w / 2 - pc) / g.w, s = a.querySelector('svg');
    if (d < best) { best = d; near = i; }
    if (s) s.style.scale = d < 1 ? (1 + 0.24 * (1 - d)).toFixed(3) : '';
  });
  links.forEach((a, i) => a.classList.toggle('near', i === near));
}
// Dedo na barra (chamado pelo app.js): ao tocar vira lente (CSS), arrastando segue o dedo
function liquidTabPress(on, idx) {
  const bar = tabBar();
  if (!bar || !bar.classList.contains('liquid') || on) return;
  if (TAB.raf) { cancelAnimationFrame(TAB.raf); TAB.raf = 0; }
  bar.classList.remove('dragging');
  bar.querySelectorAll('a').forEach(a => { a.classList.remove('near'); const s = a.querySelector('svg'); if (s) s.style.scale = ''; });
  if (!TAB.drag) return;
  const v = TAB.drag.v;
  TAB.drag = null;
  pillGo((idx != null ? idx : TAB.idx) * TAB.w, clamp(v, -4000, 4000)); // solta com a velocidade do dedo
}
function liquidTabDrag(clientX) {
  const bar = tabBar();
  if (!bar || !bar.classList.contains('liquid')) return;
  TAB.dragX = clientX;
  if (TAB.raf) return;
  TAB.raf = requestAnimationFrame(() => {
    TAB.raf = 0;
    const g = tabGeom(), max = (g.n - 1) * g.w, left = g.bar.getBoundingClientRect().left;
    let x = TAB.dragX - left - 4 - g.w / 2, over = 0;
    if (x < 0) { x = -rubber(-x, g.w * 0.8); over = -x / g.w; } else if (x > max) { x = max + rubber(x - max, g.w * 0.8); over = (x - max) / g.w; }
    const now = performance.now();
    if (!TAB.drag) { TAB.drag = { x: pillState().x, t: now - 16, v: 0 }; pillStop(); g.bar.classList.add('dragging'); }
    const inst = (x - TAB.drag.x) / (Math.max(8, now - TAB.drag.t) / 1000);
    TAB.drag.v = TAB.drag.v * 0.55 + inst * 0.45; TAB.drag.x = x; TAB.drag.t = now;
    TAB.x = TAB.to = x;
    g.bar.querySelector('.tab-pill').style.transform = pillTransform(x, TAB.drag.v, over);
    lensIcons(g, x);
  });
}

/* ================= Toque nos controles de vidro ================= */
// Crescem (transição de mola no compositor), seguem o dedo, brilham no ponto tocado e balançam ao soltar
const LQ_PRESS = '.btn, .chip, .seg button, .icon-btn, .link-btn, .tpl-banner, .ach-cell, .look-row button, .swatches button';
const LQ_MORPH = '.btn, .icon-btn, .link-btn, .chip, .ss-link';
const LQ = { last: null, held: null, src: null, in: null, out: null, nav: null };
function glint(el, e) {
  let g = el.querySelector(':scope > .lq-glint');
  if (!g) {
    g = document.createElement('span'); g.className = 'lq-glint'; g.setAttribute('aria-hidden', 'true'); g.innerHTML = '<i></i>';
    if (getComputedStyle(el).position === 'static') el.style.position = 'relative';
    el.appendChild(g);
  }
  const r = el.getBoundingClientRect();
  g._r = Math.round(clamp(Math.min(r.width, r.height) * 1.7, 44, 120));
  g.firstChild.style.width = g.firstChild.style.height = 2 * g._r + 'px';
  glintMove(g, e.clientX - r.left, e.clientY - r.top);
  clearTimeout(g._t);
  g.classList.remove('out'); g.classList.add('on');
  return g;
}
function glintMove(g, x, y) { g.firstChild.style.transform = `translate(${(x - g._r).toFixed(1)}px, ${(y - g._r).toFixed(1)}px)`; }
function glintOff(g) { if (!g) return; g.classList.add('out'); g._t = setTimeout(() => g.remove(), 650); }
document.addEventListener('pointerdown', e => {
  LQ.last = { el: e.target.closest('button, a, label, [data-act]'), t: performance.now() };
  if (!liquidOn() || e.button > 0) return;
  const bar = e.target.closest('#tabs');
  if (bar) { LQ.held = { el: bar, g: glint(bar, e), bar: true }; return; }
  const el = e.target.closest(LQ_PRESS);
  if (!el || el.disabled || el.closest('.sets, .heat')) return;
  const r = el.getBoundingClientRect();
  el.style.setProperty('--lq-s', r.width > 220 ? 1.025 : r.width < 64 ? 1.15 : 1.07);
  clearTimeout(el._lqT);
  el.classList.remove('lq-up'); el.classList.add('lq-down');
  LQ.held = { el, g: glint(el, e), ox: e.clientX, oy: e.clientY, move: getComputedStyle(el).translate === 'none' };
}, true);
window.addEventListener('pointermove', e => {
  const h = LQ.held;
  if (!h) return;
  const r = h.el.getBoundingClientRect();
  glintMove(h.g, e.clientX - r.left, e.clientY - r.top);
  if (!h.bar && h.move) h.el.style.translate = `${clamp((e.clientX - h.ox) * 0.12, -7, 7).toFixed(1)}px ${clamp((e.clientY - h.oy) * 0.12, -6, 6).toFixed(1)}px`;
}, { passive: true });
function lqRelease() {
  const h = LQ.held;
  if (!h) return;
  LQ.held = null;
  glintOff(h.g);
  if (h.bar) return;
  const el = h.el;
  el.classList.remove('lq-down'); el.classList.add('lq-up'); // volta com a mola que balança
  if (h.move) el.style.translate = '';
  el._lqT = setTimeout(() => { el.classList.remove('lq-up'); el.style.removeProperty('--lq-s'); }, LQ_EASE.bounce.duration + 60);
}
window.addEventListener('pointerup', lqRelease);
window.addEventListener('pointercancel', lqRelease);

/* ================= Botão que vira painel ================= */
// O painel cresce do botão tocado até o lugar dele e volta para o botão ao fechar (só transform e opacity)
function morphFrom(src, s) {
  const r = src.getBoundingClientRect();
  if (!r.width || r.bottom < 0 || r.top > innerHeight) return null;
  const k = clamp(r.width / s.width, 0.12, 1);
  return `translate(${(r.left - s.left).toFixed(1)}px, ${(r.top + r.height / 2 - s.top - s.height * k / 2).toFixed(1)}px) scale(${k.toFixed(4)})`;
}
function fadeKids(sh, show) {
  [...sh.children].forEach(c => {
    if (c.classList.contains('grab')) return;
    c.animate(show ? [{ opacity: 0 }, { opacity: 1 }] : [{ opacity: 1 }, { opacity: 0 }], show ? { duration: 220, delay: 60, easing: 'ease-out', fill: 'backwards' } : { duration: 120, easing: 'ease-in', fill: 'forwards' });
  });
}
function liquidSheetIn(sh, bd, swap) {
  if (LQ.out) { LQ.out.cancel(); LQ.out = null; }
  if (LQ.in && !swap) { LQ.in.cancel(); LQ.in = null; }
  if (swap) return;
  sh.classList.remove('morph');
  if (LQ.src) { LQ.src.classList.remove('morph-src'); LQ.src = null; }
  if (!liquidOn() || !sh.animate) return;
  const lp = LQ.last;
  if (!lp || !lp.el || performance.now() - lp.t > 900 || !lp.el.isConnected) return;
  const src = lp.el.closest(LQ_MORPH);
  if (!src || src.closest('#sheet, #tabs')) return;
  sh.classList.add('morph');
  const from = morphFrom(src, sh.getBoundingClientRect());
  if (!from) { sh.classList.remove('morph'); return; }
  const E = LQ_EASE.spring, a = LQ.in = sh.animate([{ transform: from }, { transform: 'none' }], { duration: E.duration, easing: E.easing });
  fadeKids(sh, true);
  src.classList.add('morph-src');
  LQ.src = src;
  a.finished.then(() => { if (LQ.in === a) { LQ.in = null; sh.classList.remove('morph'); } }, () => { });
}
// Devolve true quando cuida do fechamento (done é chamado no fim)
function liquidSheetOut(sh, bd, done) {
  if (LQ.in) { LQ.in.cancel(); LQ.in = null; }
  const src = LQ.src;
  LQ.src = null;
  const restore = () => { if (src) src.classList.remove('morph-src'); sh.classList.remove('morph'); };
  const to = src && liquidOn() && src.isConnected && !sh.style.transform && sh.animate ? morphFrom(src, sh.getBoundingClientRect()) : null;
  if (!to) { restore(); return false; }
  sh.classList.add('morph');
  fadeKids(sh, false);
  const E = LQ_EASE.soft, a = LQ.out = sh.animate([{ transform: 'none' }, { transform: to }], { duration: Math.min(420, E.duration), easing: E.easing, fill: 'forwards' });
  a.finished.then(() => { if (LQ.out !== a) return; LQ.out = null; done(); restore(); a.cancel(); }, () => restore());
  return true;
}

/* ================= Navegação entre telas ================= */
// A tela nova entra deslizando por cima da antiga (e sai ao voltar), como no iOS. Sem View Transitions,
// a barra de vidro continua viva (com o desfoque e a gota animando) durante a troca.
function liquidNav(dir, update) {
  if (!liquidOn() || !document.body.animate) return false;
  const view = document.getElementById('view');
  if (LQ.nav) LQ.nav();
  if (dir === 'tab') { update(); return true; } // trocar de aba é instantâneo, como no iPhone
  const r = view.getBoundingClientRect(), cs = getComputedStyle(view), W = innerWidth;
  const ghost = document.createElement('div');
  ghost.className = 'lq-ghost';
  ghost.setAttribute('aria-hidden', 'true');
  ghost.style.cssText = `left:${r.left}px;top:${r.top}px;width:${r.width}px;padding:${cs.paddingTop} ${cs.paddingRight} ${cs.paddingBottom} ${cs.paddingLeft}`;
  ghost.append(...view.childNodes);
  document.body.appendChild(ghost);
  update();
  const back = dir === 'back', E = LQ_EASE.nav, opt = { duration: E.duration, easing: E.easing };
  ghost.classList.toggle('over', back);
  view.classList.add('lq-nav', back ? 'under' : 'over');
  const a1 = ghost.animate(back ? [{ transform: 'none' }, { transform: `translateX(${W}px)` }] : [{ transform: 'none', opacity: 1 }, { transform: `translateX(${-W * 0.28}px)`, opacity: 0.55 }], opt);
  const a2 = view.animate(back ? [{ transform: `translateX(${-W * 0.28}px)`, opacity: 0.55 }, { transform: 'none', opacity: 1 }] : [{ transform: `translateX(${W}px)` }, { transform: 'none' }], opt);
  const end = LQ.nav = () => { if (LQ.nav !== end) return; LQ.nav = null; a1.cancel(); a2.cancel(); ghost.remove(); view.classList.remove('lq-nav', 'under', 'over'); };
  a2.finished.then(end, end);
  return true;
}

/* ================= Segmentado: a seleção desliza como líquido ================= */
const segAt = (x, y) => { for (const el of document.elementsFromPoint(x, y)) { const s = el.closest && el.closest('.seg'); if (s) return s; } return null; };
document.addEventListener('click', e => {
  if (!liquidOn()) return;
  const b = e.target.closest('.seg button');
  if (!b || b.classList.contains('on')) return;
  const old = b.parentElement.querySelector('button.on');
  if (!old) return;
  const from = old.getBoundingClientRect(), segR = b.parentElement.getBoundingClientRect();
  requestAnimationFrame(() => { // a ação já trocou a seleção (e talvez redesenhou a tela)
    const seg = segAt(segR.left + segR.width / 2, segR.top + segR.height / 2), on = seg && seg.querySelector('button.on');
    if (!on) return;
    const to = on.getBoundingClientRect(), sr = seg.getBoundingClientRect(), dx = from.left - to.left;
    if (Math.abs(dx) < 2) return;
    seg.querySelectorAll(':scope > .lq-seg').forEach(p => p.remove());
    const pill = document.createElement('i');
    pill.className = 'lq-seg';
    pill.style.cssText = `left:${(to.left - sr.left).toFixed(1)}px;top:${(to.top - sr.top).toFixed(1)}px;width:${to.width.toFixed(1)}px;height:${to.height.toFixed(1)}px`;
    seg.appendChild(pill);
    on.classList.add('lq-hold');
    const [k, c] = LQ_SPRINGS.tab;
    const { frames, duration } = simFrames(springSim(dx, 0, k, c), (x, v) => ({ transform: `translateX(${x.toFixed(2)}px) scale(${(1 + Math.min(0.4, Math.abs(v) / 1900)).toFixed(4)}, ${(1 - Math.min(0.14, Math.abs(v) / 6000)).toFixed(4)})` }));
    const a = pill.animate(frames, { duration, easing: 'linear' });
    const end = () => { pill.remove(); on.classList.remove('lq-hold'); };
    a.finished.then(end, end);
  });
}, true);

/* ================= Interruptor: a bolinha vira gota ================= */
document.addEventListener('change', e => {
  const inp = e.target;
  if (!liquidOn() || !inp.matches || !inp.matches('.switch input[type="checkbox"]')) return;
  const sw0 = inp.closest('.switch'), r = sw0.getBoundingClientRect(), was = !inp.checked;
  requestAnimationFrame(() => {
    let sw = null;
    for (const el of document.elementsFromPoint(r.left + r.width / 2, r.top + r.height / 2)) { sw = el.closest && el.closest('.switch'); if (sw) break; }
    if (!sw) return;
    const i = sw.querySelector('i'), now = sw.querySelector('input').checked;
    if (!i) return;
    if (sw !== sw0) { // a tela foi redesenhada: parte do estado anterior
      if (now === was) return;
      const cls = was ? 'lq-from-on' : 'lq-from-off';
      i.classList.add(cls); void i.offsetWidth; i.classList.remove(cls);
    }
    i.classList.remove('lq-drop'); void i.offsetWidth; i.classList.add('lq-drop');
    clearTimeout(i._t); i._t = setTimeout(() => i.classList.remove('lq-drop'), 700);
  });
}, true);

/* ================= Liga e desliga ================= */
new MutationObserver(lqSync).observe(lqRoot, { attributes: true, attributeFilter: ['data-glass'] });
lqReduce.addEventListener && lqReduce.addEventListener('change', lqSync);
// Barra muda de largura (minimizar, girar a tela): reposiciona a gota sem animar
if (window.ResizeObserver && tabBar()) new ResizeObserver(() => {
  const bar = tabBar();
  if (!bar.classList.contains('liquid') || TAB.drag || TAB.anim) return;
  const g = tabGeom();
  if (Math.abs(g.w - TAB.w) > 0.5) { TAB.w = g.w; pillSet(TAB.idx * g.w); }
}).observe(tabBar());
lqSync();
