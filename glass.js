/* Ficha — movimento do Liquid Glass (iOS 26)
 * Molas físicas no lugar de curvas fixas. A seleção da barra de abas escorre como uma gota:
 * estica na direção do movimento, achata, passa um pouco do ponto e balança até parar; ao tocar
 * vira uma lente que segue o dedo e amplia o ícone embaixo. Os controles de vidro crescem,
 * acompanham o dedo, brilham onde são tocados e balançam ao soltar; os botões se transformam no
 * painel que abrem; segmentados e interruptores deslizam como líquido.
 * Fica desligado no estilo Clássico/Material e com "Reduzir movimento".
 */
'use strict';

const lqRoot = document.documentElement;
const lqReduce = matchMedia('(prefers-reduced-motion: reduce)');
const liquidOn = () => lqRoot.hasAttribute('data-glass') && !lqReduce.matches;
function lqSync() { lqRoot.classList.toggle('lq', liquidOn()); if (!liquidOn()) lqTabsOff(); }

/* ================= Molas ================= */
// Massa-mola-amortecedor integrada em passos pequenos (estável a qualquer taxa de quadros)
class Spring {
  constructor(x, k = 400, c = 28, eps = 0.05) { this.x = x; this.t = x; this.v = 0; this.k = k; this.c = c; this.eps = eps; }
  step(dt) {
    const n = Math.max(1, Math.ceil(dt / 0.004)), h = dt / n;
    for (let i = 0; i < n; i++) { const a = -this.k * (this.x - this.t) - this.c * this.v; this.v += a * h; this.x += this.v * h; }
    if (Math.abs(this.v) < this.eps * 10 && Math.abs(this.x - this.t) < this.eps) { this.x = this.t; this.v = 0; return false; }
    return true;
  }
}
// Curva de mola como função de tempo do CSS (linear() com a resposta real, incluindo o "passar do ponto")
function springCurve(k, c) {
  const dt = 1 / 240, pts = [];
  let x = 0, v = 0, t = 0;
  for (;;) {
    pts.push([t, x]);
    const a = -k * (x - 1) - c * v; v += a * dt; x += v * dt; t += dt;
    if ((t > 0.05 && Math.abs(x - 1) < 0.0015 && Math.abs(v) < 0.02) || t > 2.5) break;
  }
  const every = Math.max(1, Math.floor(pts.length / 64)), out = [];
  for (let i = 0; i < pts.length; i += every) out.push(`${pts[i][1].toFixed(4)} ${(pts[i][0] / t * 100).toFixed(2)}%`);
  out.push('1 100%');
  return { easing: `linear(${out.join(', ')})`, duration: Math.round(t * 1000) };
}
// Quadros-chave de uma mola que sai de x0 e volta a 0; map(x, velocidade) devolve as propriedades de cada quadro
function springFrames(x0, k, c, map) {
  const dt = 1 / 120, fr = [];
  let x = x0, v = 0, t = 0;
  for (;;) {
    fr.push([t, x, v]);
    const a = -k * x - c * v; v += a * dt; x += v * dt; t += dt;
    if ((Math.abs(x) < 0.25 && Math.abs(v) < 6) || t > 1.6) break;
  }
  fr.push([t, 0, 0]);
  return { frames: fr.filter((f, i) => i % 2 === 0 || i === fr.length - 1).map(([tt, xx, vv]) => ({ offset: tt / t, ...map(xx, vv) })), duration: Math.round(t * 1000) };
}
const LQ_EASE = {};
(() => {
  const ok = window.CSS && CSS.supports && CSS.supports('transition-timing-function', 'linear(0, 1)');
  const vars = [];
  // [nome, rigidez, amortecimento]: bounce balança bem, spring passa um pouco do ponto, soft quase não passa
  for (const [name, k, c, fb] of [['bounce', 360, 19, 'cubic-bezier(.3,1.5,.5,1)'], ['spring', 400, 29, 'cubic-bezier(.3,1.25,.4,1)'], ['soft', 300, 33, 'cubic-bezier(.25,1,.35,1)']]) {
    LQ_EASE[name] = ok ? springCurve(k, c) : { easing: fb, duration: 520 };
    vars.push(`--lq-${name}: ${LQ_EASE[name].easing}; --lq-${name}-d: ${LQ_EASE[name].duration}ms;`);
  }
  // Numa folha própria: o applyLook reescreve as variáveis do estilo inline do <html>
  const st = document.createElement('style');
  st.id = 'lq-springs';
  st.textContent = `:root { ${vars.join(' ')} }`;
  document.head.appendChild(st);
})();

// Um único laço de animação para todas as molas ativas
const lqAnims = new Set();
let lqRaf = 0, lqLast = 0;
function lqKick(f) { lqAnims.add(f); if (!lqRaf) { lqLast = performance.now(); lqRaf = requestAnimationFrame(lqLoop); } }
function lqLoop(now) {
  const dt = Math.min(0.034, Math.max(0.001, (now - lqLast) / 1000));
  lqLast = now;
  for (const f of [...lqAnims]) if (!f(dt)) lqAnims.delete(f);
  lqRaf = lqAnims.size ? requestAnimationFrame(lqLoop) : 0;
}
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
// Elástico do iOS: quanto mais passa do limite, menos anda
const rubber = (d, dim) => (1 - 1 / (d * 0.55 / dim + 1)) * dim;

/* ================= Barra de abas: gota de vidro ================= */
const TAB = { x: null, p: new Spring(0, 520, 30, 0.001), drag: null, idx: 0, w: 0 };
const tabBar = () => document.getElementById('tabs');
function tabGeom() {
  const bar = tabBar(), n = bar.querySelectorAll('a').length || 1;
  return { bar, n, w: (bar.clientWidth - 8) / n };
}
// Chamado a cada render com o índice da aba atual
function liquidTabs(ti) {
  const bar = tabBar();
  if (!bar) return;
  if (!liquidOn()) { lqTabsOff(); return; }
  bar.classList.add('liquid');
  const g = tabGeom();
  TAB.idx = Math.max(0, ti); TAB.w = g.w;
  if (!TAB.x || bar.classList.contains('mini') || bar.classList.contains('no-tab')) {
    TAB.x = TAB.x || new Spring(0, 380, 25, 0.05);
    TAB.x.x = TAB.x.t = TAB.idx * g.w; TAB.x.v = 0;
  } else TAB.x.t = TAB.idx * g.w;
  lqKick(tabFrame);
}
function lqTabsOff() {
  const bar = tabBar();
  if (!bar || !bar.classList.contains('liquid')) return;
  bar.classList.remove('liquid', 'press');
  const pill = bar.querySelector('.tab-pill');
  if (pill) { pill.style.translate = ''; pill.style.scale = ''; }
  bar.querySelectorAll('a').forEach(a => { a.classList.remove('near'); const s = a.querySelector('svg'); if (s) s.style.scale = ''; });
}
function tabFrame(dt) {
  if (!TAB.x) return false;
  const g = tabGeom();
  if (!g.bar.classList.contains('liquid')) return false;
  if (Math.abs(g.w - TAB.w) > 0.5 && TAB.drag == null) { TAB.w = g.w; TAB.x.x = TAB.x.t = TAB.idx * g.w; TAB.x.v = 0; }
  if (TAB.drag != null) {
    const max = (g.n - 1) * g.w, left = g.bar.getBoundingClientRect().left;
    let t = TAB.drag - left - 4 - g.w / 2;
    if (t < 0) t = -rubber(-t, g.w * 0.8); else if (t > max) t = max + rubber(t - max, g.w * 0.8);
    TAB.x.t = t;
  }
  const a = TAB.x.step(dt), b = TAB.p.step(dt);
  paintTabs(g);
  return a || b || TAB.drag != null;
}
function paintTabs(g) {
  const pill = g.bar.querySelector('.tab-pill');
  if (!pill) return;
  const x = TAB.x.x, v = TAB.x.v, p = TAB.p.x, max = (g.n - 1) * g.w;
  const st = Math.min(0.5, Math.abs(v) / 1700);                       // estica com a velocidade
  const over = (x < 0 ? -x : x > max ? x - max : 0) / g.w;            // e no elástico das pontas
  const sx = (1 + st + over * 0.7) * (1 + 0.18 * p), sy = (1 - st * 0.28 - over * 0.2) * (1 + 0.12 * p);
  pill.style.translate = `${x.toFixed(2)}px 0`;
  pill.style.scale = `${sx.toFixed(4)} ${sy.toFixed(4)}`;
  // A lente amplia o ícone que está embaixo dela
  const pc = x + g.w / 2;
  let near = -1, best = 9;
  g.bar.querySelectorAll('a').forEach((a, i) => {
    const d = Math.abs(i * g.w + g.w / 2 - pc) / g.w, s = a.querySelector('svg');
    if (d < best) { best = d; near = i; }
    const m = 1 + 0.24 * p * Math.max(0, 1 - d);
    if (s) s.style.scale = m > 1.001 ? m.toFixed(3) : '';
  });
  g.bar.querySelectorAll('a').forEach((a, i) => a.classList.toggle('near', p > 0.08 && i === near));
}
// Dedo na barra: vira lente; arrastando, a lente segue o dedo; ao soltar, escorre até a aba
function liquidTabPress(on, idx) {
  if (!TAB.x || !liquidOn()) return;
  TAB.p.t = on ? 1 : 0;
  if (!on) { TAB.drag = null; if (idx != null) TAB.x.t = idx * TAB.w; }
  lqKick(tabFrame);
}
function liquidTabDrag(clientX) { if (!TAB.x || !liquidOn()) return; TAB.drag = clientX; lqKick(tabFrame); }

/* ================= Toque nos controles de vidro ================= */
// Crescem, seguem o dedo um pouco, brilham no ponto tocado e balançam ao soltar
const LQ_PRESS = '.btn, .chip, .seg button, .icon-btn, .link-btn, .tpl-banner, .ach-cell, .look-row button, .swatches button';
const LQ_MORPH = '.btn, .icon-btn, .link-btn, .chip, .ss-link';
const LQ = { last: null, held: null, src: null, in: null, out: null };
const lqJ = new WeakMap();
function glint(el, e) {
  let g = el.querySelector(':scope > .lq-glint');
  if (!g) {
    g = document.createElement('span'); g.className = 'lq-glint'; g.setAttribute('aria-hidden', 'true');
    if (getComputedStyle(el).position === 'static') el.style.position = 'relative';
    el.appendChild(g);
  }
  const r = el.getBoundingClientRect();
  g.style.setProperty('--gx', (e.clientX - r.left).toFixed(1) + 'px');
  g.style.setProperty('--gy', (e.clientY - r.top).toFixed(1) + 'px');
  g.style.setProperty('--gr', Math.round(clamp(Math.min(r.width, r.height) * 1.7, 44, 120)) + 'px');
  clearTimeout(g._t);
  g.classList.remove('out'); g.classList.add('on');
  return g;
}
function glintOff(g) {
  if (!g) return;
  g.classList.add('out');
  g._t = setTimeout(() => g.remove(), 650);
}
function jelly(el) {
  let j = lqJ.get(el);
  if (!j) {
    j = { el, s: new Spring(1, 620, 20, 0.0006), x: new Spring(0, 520, 30, 0.04), y: new Spring(0, 520, 30, 0.04) };
    j.f = dt => {
      if (!j.el.isConnected) return false;
      const a = j.s.step(dt), b = j.x.step(dt), c = j.y.step(dt);
      const kx = 1 + Math.abs(j.x.x) / Math.max(40, j.w) * 0.6, ky = 1 + Math.abs(j.y.x) / Math.max(30, j.h) * 0.6;
      j.el.style.scale = `${(j.s.x * kx / Math.sqrt(ky)).toFixed(4)} ${(j.s.x * ky / Math.sqrt(kx)).toFixed(4)}`;
      if (j.move) j.el.style.translate = `${j.x.x.toFixed(2)}px ${j.y.x.toFixed(2)}px`;
      const busy = a || b || c || LQ.held === j;
      if (!busy) { j.el.style.scale = ''; if (j.move) j.el.style.translate = ''; }
      return busy;
    };
    lqJ.set(el, j);
  }
  return j;
}
document.addEventListener('pointerdown', e => {
  LQ.last = { el: e.target.closest('button, a, label, [data-act]'), t: performance.now() };
  if (!liquidOn() || e.button > 0) return;
  const bar = e.target.closest('#tabs');
  if (bar) { LQ.held = { g: glint(bar, e), el: bar, ox: e.clientX, oy: e.clientY }; return; }
  const el = e.target.closest(LQ_PRESS);
  if (!el || el.disabled || el.closest('.sets, .heat')) return;
  const r = el.getBoundingClientRect(), j = jelly(el);
  j.w = r.width; j.h = r.height; j.ox = e.clientX; j.oy = e.clientY;
  j.move = getComputedStyle(el).translate === 'none'; // não mexe em quem já usa translate
  j.s.t = r.width > 220 ? 1.025 : r.width < 64 ? 1.16 : 1.08;
  j.g = glint(el, e);
  LQ.held = j;
  lqKick(j.f);
}, true);
window.addEventListener('pointermove', e => {
  const j = LQ.held;
  if (!j) return;
  const r = j.el.getBoundingClientRect();
  if (j.g) { j.g.style.setProperty('--gx', (e.clientX - r.left).toFixed(1) + 'px'); j.g.style.setProperty('--gy', (e.clientY - r.top).toFixed(1) + 'px'); }
  if (j.s) { j.x.t = clamp((e.clientX - j.ox) * 0.12, -7, 7); j.y.t = clamp((e.clientY - j.oy) * 0.12, -6, 6); lqKick(j.f); }
}, { passive: true });
function lqRelease() {
  const j = LQ.held;
  if (!j) return;
  LQ.held = null;
  glintOff(j.g);
  if (j.s) { j.s.t = 1; j.x.t = 0; j.y.t = 0; lqKick(j.f); }
}
window.addEventListener('pointerup', lqRelease);
window.addEventListener('pointercancel', lqRelease);

/* ================= Botão que vira painel ================= */
// O painel nasce do botão tocado (cresce da forma e do lugar dele) e volta para ele ao fechar
function liquidSheetIn(sh, bd, swap) {
  if (LQ.out) { LQ.out.cancel(); LQ.out = null; }
  if (LQ.in && !swap) { LQ.in.cancel(); LQ.in = null; }
  sh.classList.remove('morph');
  if (swap) return;
  if (LQ.src) { LQ.src.classList.remove('morph-src'); LQ.src = null; }
  if (!liquidOn() || !sh.animate) return;
  const lp = LQ.last;
  if (!lp || !lp.el || performance.now() - lp.t > 900 || !lp.el.isConnected) return;
  const src = lp.el.closest(LQ_MORPH);
  if (!src || src.closest('#sheet, #tabs')) return;
  const r = src.getBoundingClientRect();
  if (!r.width || r.bottom < 0 || r.top > innerHeight) return;
  sh.classList.add('morph');
  const s = sh.getBoundingClientRect();
  const rad = Math.min(r.height / 2, parseFloat(getComputedStyle(src).borderTopLeftRadius) || r.height / 2);
  const E = LQ_EASE.spring;
  const a = LQ.in = sh.animate([
    { translate: `${(r.left - s.left).toFixed(1)}px ${(r.top - s.top).toFixed(1)}px`, clipPath: `inset(0px ${(s.width - r.width).toFixed(1)}px ${(s.height - r.height).toFixed(1)}px 0px round ${rad}px)` },
    { translate: '0px 0px', clipPath: 'inset(0px 0px 0px 0px round 38px)' }
  ], { duration: E.duration, easing: E.easing });
  a.finished.then(() => { if (LQ.in === a) { LQ.in = null; sh.classList.remove('morph'); } }, () => { });
  [...sh.children].forEach(c => { if (!c.classList.contains('grab')) c.animate([{ opacity: 0, filter: 'blur(5px)' }, { opacity: 1, filter: 'blur(0px)' }], { duration: 300, delay: 70, easing: 'ease-out', fill: 'backwards' }); });
  src.classList.add('morph-src');
  LQ.src = src;
}
// Devolve true quando cuida do fechamento (done é chamado no fim)
function liquidSheetOut(sh, bd, done) {
  if (LQ.in) { LQ.in.cancel(); LQ.in = null; }
  const src = LQ.src;
  LQ.src = null;
  const restore = () => { if (src) src.classList.remove('morph-src'); sh.classList.remove('morph'); };
  if (!src || !liquidOn() || !src.isConnected || sh.style.transform || !sh.animate) { restore(); return false; }
  const r = src.getBoundingClientRect(), s = sh.getBoundingClientRect();
  if (!r.width || r.bottom < 0 || r.top > innerHeight) { restore(); return false; }
  const rad = Math.min(r.height / 2, parseFloat(getComputedStyle(src).borderTopLeftRadius) || r.height / 2);
  sh.classList.add('morph');
  [...sh.children].forEach(c => { if (!c.classList.contains('grab')) c.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 140, easing: 'ease-in', fill: 'forwards' }); });
  const a = sh.animate([
    { translate: '0px 0px', clipPath: `inset(0px 0px 0px 0px round 38px)` },
    { translate: `${(r.left - s.left).toFixed(1)}px ${(r.top - s.top).toFixed(1)}px`, clipPath: `inset(0px ${(s.width - r.width).toFixed(1)}px ${(s.height - r.height).toFixed(1)}px 0px round ${rad}px)` }
  ], { duration: Math.min(460, LQ_EASE.soft.duration), easing: LQ_EASE.soft.easing, fill: 'forwards' });
  LQ.out = a;
  a.finished.then(() => {
    if (LQ.out !== a) return;
    LQ.out = null; done(); restore();
    a.cancel();
  }).catch(() => restore());
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
    const { frames, duration } = springFrames(dx, 420, 26, (x, v) => ({ translate: `${x.toFixed(2)}px 0`, scale: `${(1 + Math.min(0.4, Math.abs(v) / 1900)).toFixed(4)} ${(1 - Math.min(0.14, Math.abs(v) / 6000)).toFixed(4)}` }));
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
    clearTimeout(i._t); i._t = setTimeout(() => i.classList.remove('lq-drop'), 900);
  });
}, true);

/* ================= Liga e desliga ================= */
new MutationObserver(lqSync).observe(lqRoot, { attributes: true, attributeFilter: ['data-glass'] });
lqReduce.addEventListener && lqReduce.addEventListener('change', lqSync);
if (window.ResizeObserver && tabBar()) new ResizeObserver(() => { if (TAB.x) lqKick(tabFrame); }).observe(tabBar());
lqSync();
