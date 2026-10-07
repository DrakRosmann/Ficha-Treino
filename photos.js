// Fotos do progresso (aba Corpo): tiradas pelo app (câmera com guia e timer) ou escolhidas da galeria,
// por pose (frente, lado, costas), com galeria, comparação antes/depois e variação das medidas.
// As imagens ficam no IndexedDB deste aparelho (não cabem no localStorage); os dados de cada foto ficam em S.photos.
// Nada é enviado para a internet nem para a IA. O backup pode incluir as fotos, se você quiser.
'use strict';

const POSES = [['frente', 'Frente'], ['lado', 'Lado'], ['costas', 'Costas']];
const poseName = p => (POSES.find(x => x[0] === p) || [p, p])[1];

/* ================= Armazenamento (IndexedDB) ================= */
let ppDbP = null;
function ppDb() {
  if (!ppDbP) ppDbP = new Promise((res, rej) => {
    const r = indexedDB.open('ficha-fotos', 1);
    r.onupgradeneeded = () => r.result.createObjectStore('photos', { keyPath: 'id' });
    r.onsuccess = () => res(r.result);
    r.onerror = () => { ppDbP = null; rej(r.error); };
  });
  return ppDbP;
}
async function ppTx(mode, fn) {
  const db = await ppDb();
  return new Promise((res, rej) => {
    const tx = db.transaction('photos', mode), req = fn(tx.objectStore('photos'));
    tx.oncomplete = () => res(req ? req.result : undefined);
    tx.onerror = () => rej(tx.error); tx.onabort = () => rej(tx.error);
  });
}
// As imagens são guardadas como ArrayBuffer (JPEG): funciona em todos os navegadores
const ppPut = rec => ppTx('readwrite', st => st.put(rec));
const ppGet = id => ppTx('readonly', st => st.get(id));
const ppDel = id => ppTx('readwrite', st => st.delete(id));
const ppAll = () => ppTx('readonly', st => st.getAll());
const ppClear = () => ppTx('readwrite', st => st.clear());

const ppURLs = new Map();
async function ppURL(id, kind) {
  const k = id + ':' + kind;
  if (ppURLs.has(k)) return ppURLs.get(k);
  const rec = await ppGet(id);
  if (!rec || !rec[kind]) return null;
  const url = URL.createObjectURL(new Blob([rec[kind]], { type: 'image/jpeg' }));
  ppURLs.set(k, url);
  return url;
}
function ppForget(id) {
  for (const kind of ['full', 'thumb']) { const u = ppURLs.get(id + ':' + kind); if (u) { URL.revokeObjectURL(u); ppURLs.delete(id + ':' + kind); } }
}
// Carrega as imagens marcadas com data-pp depois que a tela é desenhada
function hydrateProgressPhotos(root = document) {
  root.querySelectorAll('img[data-pp]:not([src])').forEach(img => {
    ppURL(img.dataset.pp, img.dataset.kind || 'thumb')
      .then(u => { if (u) img.src = u; else img.closest('.pp')?.classList.add('missing'); })
      .catch(() => img.closest('.pp')?.classList.add('missing'));
  });
}

/* ================= Imagem ================= */
function ppImage(src) {
  return new Promise((res, rej) => { const img = new Image(); img.onload = () => res(img); img.onerror = rej; img.src = src; });
}
function ppJpeg(source, sw, sh, maxSide, q) {
  const s = Math.min(1, maxSide / Math.max(sw, sh));
  const c = document.createElement('canvas');
  c.width = Math.round(sw * s); c.height = Math.round(sh * s);
  c.getContext('2d').drawImage(source, 0, 0, c.width, c.height);
  return new Promise(res => c.toBlob(b => res(b), 'image/jpeg', q));
}
// Foto grande (até 1440 px) para ver e comparar + miniatura (360 px) para as listas
async function ppEncode(source, w, h) {
  const full = await ppJpeg(source, w, h, 1440, 0.85), thumb = await ppJpeg(source, w, h, 360, 0.78);
  return { full: await full.arrayBuffer(), thumb: await thumb.arrayBuffer(), kb: Math.round((full.size + thumb.size) / 1024) };
}
async function ppSave(source, w, h, t, pose) {
  const enc = await ppEncode(source, w, h), id = 'pp' + uid();
  await ppPut({ id, full: enc.full, thumb: enc.thumb });
  S.photos = S.photos || [];
  S.photos.push({ id, t, pose, kb: enc.kb });
  save();
  return id;
}

/* ================= Dados ================= */
const photosOf = pose => (S.photos || []).filter(p => !pose || p.pose === pose).sort((a, b) => a.t - b.t);
function photoDates() { return [...new Set((S.photos || []).map(p => p.t))].sort((a, b) => a - b); }
// Registro do corpo mais próximo da data da foto (até 4 dias de diferença)
function bodyNear(t, field) {
  let best = null;
  for (const e of S.body || []) {
    if (e[field] == null) continue;
    const d = Math.abs(e.t - t);
    if (d <= 4 * 864e5 && (!best || d < best.d)) best = { d, v: e[field] };
  }
  return best ? best.v : null;
}
const ppDelta = (v, unit, dec = 1) => `${v > 0 ? '+' : v < 0 ? '−' : ''}${fmt(Math.abs(v), dec)}${unit}`;
function photoCompareFacts(a, b) {
  const out = [`${Math.round((b.t - a.t) / 864e5)} dias`];
  for (const [f, label, unit, dec] of [['peso', 'peso', ' kg', 1], ['gordura', 'gordura', ' pts', 1], ['cintura', 'cintura', ' cm', 1], ['braco', 'braço', ' cm', 1]]) {
    const va = bodyNear(a.t, f), vb = bodyNear(b.t, f);
    if (va != null && vb != null) out.push(`${label} ${ppDelta(vb - va, unit, dec)}`);
  }
  return out.join(' · ');
}

/* ================= Telas ================= */
function ppThumb(p, cls = '') {
  return `<button class="pp ${cls}" data-act="ppView" data-id="${p.id}" aria-label="Foto de ${poseName(p.pose).toLowerCase()} de ${dateShort(p.t)}"><img data-pp="${p.id}" alt=""></button>`;
}
// Seção na aba Corpo
function photosCorpoHTML() {
  const all = photosOf(), dates = photoDates();
  let html = `<h2 class="section">Fotos do progresso${all.length ? '<a class="link-btn" href="#/fotos">Ver todas</a>' : ''}</h2>`;
  if (!all.length) {
    return html + `<div class="card pp-empty"><div class="pp-poses">${POSES.map(([, l]) => `<div class="pp ph"><span>${l}</span></div>`).join('')}</div>
      <p class="small muted" style="margin:12px 0;line-height:1.5">Tire fotos de frente, de lado e de costas a cada 2 a 4 semanas. A balança não mostra tudo: as fotos mostram a mudança no corpo. Elas ficam só neste aparelho.</p>
      <button class="btn primary block" data-act="ppAdd">${I.camera}Tirar as primeiras fotos</button></div>`;
  }
  const last = dates[dates.length - 1], lastSet = POSES.map(([p]) => all.find(x => x.t === last && x.pose === p));
  const front = photosOf('frente');
  const w = bodyNear(last, 'peso');
  html += `<div class="card pp-card"><div class="small muted pp-cap">Últimas fotos: ${dateShort(last)}${w ? ` · ${fmt(w)} kg` : ''}</div>
    <div class="pp-poses">${lastSet.map((p, i) => p ? ppThumb(p) : `<button class="pp ph" data-act="ppAdd" data-pose="${POSES[i][0]}"><span>${POSES[i][1]}</span>${I.plus}</button>`).join('')}</div>
    ${front.length >= 2 ? `<a class="pp-ba" href="#/fotos"><div class="pp"><img data-pp="${front[0].id}" alt=""><small>${dateShort(front[0].t)}</small></div><span class="pp-arrow">→</span>
      <div class="pp"><img data-pp="${front[front.length - 1].id}" alt=""><small>${dateShort(front[front.length - 1].t)}</small></div>
      <div class="pp-ba-txt"><b>Antes e depois</b><span class="small muted">${photoCompareFacts(front[0], front[front.length - 1])}</span></div></a>` : ''}
    <div class="btn-row" style="margin-top:12px"><button class="btn sm primary" data-act="ppAdd">${I.camera}Adicionar fotos</button>${all.length > 1 ? '<a class="btn sm" href="#/fotos">Comparar</a>' : ''}</div></div>`;
  return html;
}

// Galeria e comparação (#/fotos)
let ppPose = 'frente', ppCmp = { a: null, b: null, mode: 'slide' };
function viewFotos() {
  const list = photosOf(ppPose), counts = Object.fromEntries(POSES.map(([p]) => [p, photosOf(p).length]));
  let html = topBar({ back: '#/corpo', right: `<button class="link-btn" data-act="ppAdd">${I.plus.replace('<svg', '<svg class="inline-ic"')}Adicionar</button>` }) + `<h1 class="title">Fotos do progresso</h1>
    <div class="seg" style="grid-template-columns:repeat(3,1fr);margin-bottom:14px">${POSES.map(([p, l]) => `<button class="${ppPose === p ? 'on' : ''}" data-act="ppPose" data-v="${p}">${l}${counts[p] ? ` · ${counts[p]}` : ''}</button>`).join('')}</div>`;
  if (!list.length) {
    return html + `<div class="card">${emptyState(I.camera, `Nenhuma foto de ${poseName(ppPose).toLowerCase()}`, 'Use sempre o mesmo lugar, a mesma luz e a mesma distância. A câmera com guia mostra a foto anterior por cima para você se posicionar igual.', `<button class="btn primary block" data-act="ppAdd" data-pose="${ppPose}">${I.camera}Adicionar foto</button>`)}</div>`;
  }
  if (list.length >= 2) {
    const ids = list.map(p => p.id);
    if (!ids.includes(ppCmp.a)) ppCmp.a = list[0].id;
    if (!ids.includes(ppCmp.b) || ppCmp.b === ppCmp.a) ppCmp.b = list[list.length - 1].id;
    const a = list.find(p => p.id === ppCmp.a), b = list.find(p => p.id === ppCmp.b);
    const sel = (k, cur) => `<select class="input" data-pp-cmp="${k}" aria-label="${k === 'a' ? 'Antes' : 'Depois'}">${list.map(p => `<option value="${p.id}" ${p.id === cur ? 'selected' : ''}>${dateShort(p.t)}${new Date(p.t).getFullYear() !== new Date().getFullYear() ? '/' + new Date(p.t).getFullYear() : ''}</option>`).join('')}</select>`;
    html += `<div class="card pp-cmp-card">
      <div class="pp-sel"><label><span>Antes</span>${sel('a', a.id)}</label><label><span>Depois</span>${sel('b', b.id)}</label></div>
      <div class="seg seg-sm" style="grid-template-columns:1fr 1fr;margin:12px 0">${[['slide', 'Deslizar'], ['side', 'Lado a lado']].map(([v, l]) => `<button class="${ppCmp.mode === v ? 'on' : ''}" data-act="ppMode" data-v="${v}">${l}</button>`).join('')}</div>
      ${ppCmp.mode === 'slide'
        ? `<div class="pp-slide" style="--pos:50%"><img data-pp="${b.id}" data-kind="full" alt="Depois"><div class="pp-before"><img data-pp="${a.id}" data-kind="full" alt="Antes"></div>
            <i class="pp-handle"></i><span class="pp-tag l">${dateShort(a.t)}</span><span class="pp-tag r">${dateShort(b.t)}</span>
            <input type="range" min="0" max="100" value="50" data-pp-slider aria-label="Deslize para comparar antes e depois"></div>`
        : `<div class="pp-side"><figure><img data-pp="${a.id}" data-kind="full" alt="Antes"><figcaption>${dateShort(a.t)}</figcaption></figure>
            <figure><img data-pp="${b.id}" data-kind="full" alt="Depois"><figcaption>${dateShort(b.t)}</figcaption></figure></div>`}
      <p class="small pp-facts">${photoCompareFacts(a, b)}</p></div>`;
  } else {
    html += `<p class="small muted" style="margin:0 4px 12px">Adicione outra foto de ${poseName(ppPose).toLowerCase()} em outra data para comparar.</p>`;
  }
  html += `<h2 class="section">Linha do tempo</h2><div class="pp-grid">${list.slice().reverse().map(p => {
    const w = bodyNear(p.t, 'peso');
    return `<div class="pp-cell">${ppThumb(p)}<div class="small"><b>${dateShort(p.t)}</b>${w ? ` <span class="muted">${fmt(w)} kg</span>` : ''}</div></div>`;
  }).join('')}</div>
  <p class="small muted" style="margin:14px 4px 0;line-height:1.5">${(S.photos || []).length} fotos · ${fmt(sum((S.photos || []).map(p => p.kb || 0)) / 1024, 1)} MB neste aparelho. As fotos não vão para a internet; o backup só as inclui se você escolher.</p>`;
  return html;
}

/* ---------- Adicionar ---------- */
let ppAdd = { t: 0, pose: 'frente' };
const camOK = () => !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
function ppAddSheet() {
  const today = startOfDay(Date.now());
  if (!ppAdd.t) ppAdd.t = today;
  const done = photosOf().filter(p => p.t === ppAdd.t);
  const prev = photosOf(ppAdd.pose).filter(p => p.t < ppAdd.t).pop();
  openSheet(`${sheetHead('Fotos do progresso')}<div class="sheet-body">
    <label class="field"><span>Data</span><input class="input" type="date" id="ppDate" value="${isoDate(ppAdd.t)}" max="${isoDate(today)}"></label>
    <div class="look-label">Pose</div>
    <div class="seg" style="grid-template-columns:repeat(3,1fr)">${POSES.map(([p, l]) => `<button class="${ppAdd.pose === p ? 'on' : ''}" data-act="ppAddPose" data-v="${p}">${done.some(x => x.pose === p) ? '✓ ' : ''}${l}</button>`).join('')}</div>
    ${done.length ? `<div class="pp-poses" style="margin-bottom:14px">${POSES.map(([p, l]) => { const x = done.find(d => d.pose === p); return x ? ppThumb(x) : `<div class="pp ph"><span>${l}</span></div>`; }).join('')}</div>` : ''}
    <div class="stack">
      ${camOK() ? `<button class="btn primary block" data-act="ppCam">${I.camera}Câmera com guia</button>` : ''}
      <label class="btn block ${camOK() ? '' : 'primary'}" style="cursor:pointer">${I.plus}Escolher foto ou usar a câmera do celular<input type="file" accept="image/*" data-pp-file hidden></label>
    </div>
    ${prev ? `<p class="small muted" style="margin:10px 2px 0">A câmera com guia mostra por cima, transparente, a foto de ${poseName(ppAdd.pose).toLowerCase()} de ${dateShort(prev.t)}.</p>` : ''}
    <div class="look-label" style="margin-top:16px">Para comparar bem</div>
    <ul class="pp-tips"><li>Mesmo lugar, mesma luz e mesma distância</li><li>De manhã, em jejum, com roupas parecidas</li><li>Celular na altura do umbigo, reto; use o timer e apoie o celular</li><li>Corpo relaxado, na mesma postura</li></ul>
  </div><div class="sheet-foot"><button class="btn block" data-act="closeSheet">${done.length ? 'Concluir' : 'Cancelar'}</button></div>`);
}
function ppNextPose(t) {
  const have = new Set(photosOf().filter(p => p.t === t).map(p => p.pose));
  return (POSES.find(([p]) => !have.has(p)) || POSES[0])[0];
}
async function ppAfterSave(pose) {
  toast(`Foto de ${poseName(pose).toLowerCase()} salva`);
  ppAdd.pose = ppNextPose(ppAdd.t);
}

/* ---------- Câmera com guia ---------- */
let ppCam = { facing: 'environment', timer: 3, ghost: 35, stream: null, busy: false };
function ppStopCam() {
  if (ppCam.stream) { ppCam.stream.getTracks().forEach(t => t.stop()); ppCam.stream = null; }
  clearInterval(ppCam.count); ppCam.busy = false;
}
function ppCamSheet() {
  const prev = photosOf(ppAdd.pose).filter(p => p.t <= ppAdd.t).pop();
  openSheet(`${sheetHead(`${poseName(ppAdd.pose)} · ${dateShort(ppAdd.t)}`)}<div class="sheet-body">
    <div class="pcam ${ppCam.facing === 'user' ? 'mirror' : ''}" style="--ghost:${ppCam.ghost / 100}">
      <video playsinline muted autoplay></video>
      ${prev ? `<img class="ghost" data-pp="${prev.id}" data-kind="full" alt="">` : ''}
      <div class="pcam-grid"></div><div class="pcount" hidden></div><div class="pflash"></div>
    </div>
    <div class="seg seg-sm" style="grid-template-columns:repeat(3,1fr);margin-top:10px">${POSES.map(([p, l]) => `<button class="${ppAdd.pose === p ? 'on' : ''}" data-act="ppCamPose" data-v="${p}">${l}</button>`).join('')}</div>
    <div class="pcam-ctl">
      <button class="btn sm" data-act="ppFlip">${ppCam.facing === 'user' ? 'Câmera traseira' : 'Câmera frontal'}</button>
      <div class="chips" style="margin:0;padding:0">${[0, 3, 10].map(s => `<button class="chip ${ppCam.timer === s ? 'on' : ''}" data-act="ppTimer" data-v="${s}">${s ? s + ' s' : 'Sem timer'}</button>`).join('')}</div>
    </div>
    ${prev ? `<label class="pcam-ghost"><span class="small muted">Guia</span><input type="range" min="0" max="70" value="${ppCam.ghost}" data-pp-ghost aria-label="Transparência da foto anterior"></label>` : ''}
  </div><div class="sheet-foot"><button class="btn primary block pshoot" data-act="ppShoot">${I.camera}Tirar foto</button></div>`, null, ppStopCam);
  ppStartCam();
}
async function ppStartCam() {
  ppStopCam();
  try {
    ppCam.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: ppCam.facing, width: { ideal: 1440 }, height: { ideal: 1920 } }, audio: false });
    const v = $('.pcam video');
    if (!v) { ppStopCam(); return; }
    v.srcObject = ppCam.stream; await v.play().catch(() => {});
  } catch (e) {
    toast('Sem acesso à câmera. Permita a câmera ou escolha uma foto.');
    ppAddSheet();
  }
}
function ppShootNow() {
  const v = $('.pcam video');
  if (!v || !v.videoWidth) { toast('A câmera ainda está abrindo'); ppCam.busy = false; return; }
  const c = document.createElement('canvas'), w = v.videoWidth, h = v.videoHeight;
  c.width = w; c.height = h;
  const ctx = c.getContext('2d');
  if (ppCam.facing === 'user') { ctx.translate(w, 0); ctx.scale(-1, 1); } // salva como aparece na tela
  ctx.drawImage(v, 0, 0, w, h);
  const f = $('.pflash'); if (f) { f.classList.remove('on'); void f.offsetWidth; f.classList.add('on'); }
  const pose = ppAdd.pose;
  ppSave(c, w, h, ppAdd.t, pose).then(() => {
    ppCam.busy = false;
    ppAfterSave(pose);
    if (photosOf().filter(p => p.t === ppAdd.t).length >= POSES.length) { closeSheet(); rerender(); return; }
    ppCamSheet(); rerender();
  }).catch(() => { ppCam.busy = false; toast('Não foi possível salvar a foto'); });
}

/* ---------- Visualizar ---------- */
function ppViewSheet(id) {
  const p = (S.photos || []).find(x => x.id === id);
  if (!p) return;
  const list = photosOf(p.pose), i = list.findIndex(x => x.id === id), w = bodyNear(p.t, 'peso'), bf = bodyNear(p.t, 'gordura');
  openSheet(`${sheetHead(`${poseName(p.pose)} · ${dateShort(p.t)}`)}<div class="sheet-body">
    <div class="pp-full pp"><img data-pp="${p.id}" data-kind="full" alt="Foto de ${poseName(p.pose).toLowerCase()}"></div>
    <p class="small muted" style="text-align:center;margin:8px 0 0">${dateLong(p.t)}${w ? ` · ${fmt(w)} kg` : ''}${bf ? ` · ${fmt(bf)}% gordura` : ''}</p>
    <div class="btn-row" style="margin-top:12px">
      <button class="btn sm" data-act="ppView" data-id="${i > 0 ? list[i - 1].id : ''}" ${i > 0 ? '' : 'disabled style="opacity:.4"'}>‹ Anterior</button>
      <button class="btn sm" data-act="ppView" data-id="${i < list.length - 1 ? list[i + 1].id : ''}" ${i < list.length - 1 ? '' : 'disabled style="opacity:.4"'}>Próxima ›</button>
    </div>
  </div><div class="sheet-foot stack">
    ${list.length > 1 ? `<button class="btn block" data-act="ppCompareWith" data-id="${p.id}">Comparar com a ${i === 0 ? 'última' : 'primeira'}</button>` : ''}
    <button class="btn danger block" data-act="ppDelete" data-id="${p.id}">${I.trash}Excluir foto</button></div>`);
}

/* ================= Backup ================= */
function ppB64(buf) {
  const bytes = new Uint8Array(buf); let s = '';
  for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  return btoa(s);
}
function ppBuf(b64) {
  const s = atob(b64), out = new Uint8Array(s.length);
  for (let i = 0; i < s.length; i++) out[i] = s.charCodeAt(i);
  return out.buffer;
}
// { id: [foto, miniatura] } em base64, para o arquivo de backup
async function ppExportData() {
  const ids = new Set((S.photos || []).map(p => p.id)), out = {};
  for (const r of await ppAll()) if (ids.has(r.id)) out[r.id] = [ppB64(r.full), ppB64(r.thumb)];
  return out;
}
async function ppImportData(data) {
  for (const [id, [full, thumb]] of Object.entries(data || {})) { ppForget(id); await ppPut({ id, full: ppBuf(full), thumb: ppBuf(thumb) }); }
}
async function ppWipe() {
  for (const u of ppURLs.values()) URL.revokeObjectURL(u);
  ppURLs.clear();
  try { await ppClear(); } catch (e) { /* sem IndexedDB */ }
}

/* ================= Ações ================= */
const PHOTO_ACTIONS = {
  ppAdd: el => { ppAdd = { t: startOfDay(Date.now()), pose: el.dataset.pose || ppNextPose(startOfDay(Date.now())) }; ppAddSheet(); },
  ppAddPose: el => { ppAdd.pose = el.dataset.v; ppAddSheet(); },
  ppCam: () => ppCamSheet(),
  ppCamPose: el => { ppAdd.pose = el.dataset.v; ppCamSheet(); },
  ppFlip: () => { ppCam.facing = ppCam.facing === 'user' ? 'environment' : 'user'; ppCamSheet(); },
  ppTimer: el => { ppCam.timer = +el.dataset.v; el.parentNode.querySelectorAll('.chip').forEach(c => c.classList.toggle('on', c === el)); },
  ppShoot: () => {
    if (ppCam.busy) return;
    ppCam.busy = true;
    let n = ppCam.timer;
    if (!n) { ppShootNow(); return; }
    const box = $('.pcount');
    const show = () => { if (box) { box.hidden = false; box.textContent = n; box.classList.remove('tick'); void box.offsetWidth; box.classList.add('tick'); } };
    show();
    ppCam.count = setInterval(() => {
      n--;
      if (n > 0) { show(); return; }
      clearInterval(ppCam.count);
      if (box) box.hidden = true;
      ppShootNow();
    }, 1000);
  },
  ppView: el => { if (el.dataset.id) ppViewSheet(el.dataset.id); },
  ppPose: el => { ppPose = el.dataset.v; rerender(); },
  ppMode: el => { ppCmp.mode = el.dataset.v; rerender(); },
  ppCompareWith: el => {
    const p = (S.photos || []).find(x => x.id === el.dataset.id), list = photosOf(p.pose);
    const other = list[0].id === p.id ? list[list.length - 1] : list[0];
    ppPose = p.pose;
    ppCmp = { ...ppCmp, a: other.t < p.t ? other.id : p.id, b: other.t < p.t ? p.id : other.id };
    closeSheet(); go('#/fotos');
  },
  ppDelete: el => {
    const id = el.dataset.id;
    confirmSheet({
      title: 'Excluir esta foto?', text: 'Ela será apagada deste aparelho.', ok: 'Excluir', danger: true,
      onOk: async () => {
        S.photos = (S.photos || []).filter(p => p.id !== id); save();
        ppForget(id);
        try { await ppDel(id); } catch (e) { /* já não existia */ }
        rerender(); toast('Foto excluída');
      }
    });
  }
};

// Campos (chamados pelos ouvintes de input/change do app.js)
function photoInput(t) {
  if (t.hasAttribute('data-pp-slider')) { t.closest('.pp-slide').style.setProperty('--pos', t.value + '%'); return true; }
  if (t.hasAttribute('data-pp-ghost')) { ppCam.ghost = +t.value; t.closest('.sheet-body').querySelector('.pcam').style.setProperty('--ghost', t.value / 100); return true; }
  return false;
}
function photoChange(t) {
  if (t.id === 'ppDate') {
    const [y, m, d] = (t.value || isoDate(Date.now())).split('-').map(Number);
    ppAdd.t = Math.min(startOfDay(Date.now()), new Date(y, m - 1, d).getTime());
    ppAdd.pose = ppNextPose(ppAdd.t); ppAddSheet(); return true;
  }
  if (t.dataset.ppCmp) { ppCmp[t.dataset.ppCmp] = t.value; rerender(); return true; }
  if (t.hasAttribute('data-pp-file') && t.files && t.files[0]) {
    const file = t.files[0], pose = ppAdd.pose;
    t.value = '';
    const url = URL.createObjectURL(file);
    ppImage(url)
      .then(img => ppSave(img, img.naturalWidth, img.naturalHeight, ppAdd.t, pose))
      .then(() => { URL.revokeObjectURL(url); ppAfterSave(pose); ppAddSheet(); rerender(); })
      .catch(() => { URL.revokeObjectURL(url); toast('Não foi possível abrir a foto'); });
    return true;
  }
  return false;
}
