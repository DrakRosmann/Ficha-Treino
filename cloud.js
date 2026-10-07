/* Ficha — nuvem (sincronização entre aparelhos) e lembretes por notificação
 * Usa um servidor próprio, grátis, na Cloudflare (pasta server/). Os dados vão criptografados
 * com o código de sincronização (AES-GCM); o servidor só guarda um arquivo embaralhado.
 */
'use strict';

const CLOUD_KEY = 'ficha.sync', PUSH_KEY = 'ficha.push';
const CLOUD_DEFAULT_URL = ''; // endereço do seu Worker, para já vir preenchido em todos os aparelhos
const CLOUD_HELP = 'https://github.com/DrakRosmann/Ficha-Treino/blob/main/server/README.md';
// Ajustes que seguem a conta; os outros (tema, estilo, tela acesa…) são de cada aparelho
const SYNC_SETTINGS = ['rest', 'sound', 'progression', 'rir', 'bar', 'accent', 'timerShortcut'];

const lsGet = k => { try { return JSON.parse(localStorage.getItem(k) || 'null'); } catch (e) { return null; } };
const lsSet = (k, v) => { try { v == null ? localStorage.removeItem(k) : localStorage.setItem(k, JSON.stringify(v)); } catch (e) { /* cheio */ } };
function cloudCfg() { return { url: CLOUD_DEFAULT_URL, ...(lsGet(CLOUD_KEY) || {}) }; }
function setCloudCfg(c) { lsSet(CLOUD_KEY, c); }
function cloudOn() { const c = cloudCfg(); return !!(c.url && c.code); }
const cloudUrl = p => cloudCfg().url.replace(/\/+$/, '') + p;

class CloudError extends Error {}
async function cloudApi(method, path, body, auth) {
  let res;
  try {
    res = await fetch(cloudUrl(path), { method, cache: 'no-store', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: 'Bearer ' + auth } : {}) }, body: body == null ? undefined : JSON.stringify(body) });
  } catch (e) { throw new CloudError('Sem conexão com o servidor'); }
  let j = {};
  try { j = await res.json(); } catch (e) { /* sem corpo */ }
  return { status: res.status, j };
}
const cloudHttpErr = r => new CloudError(r.status === 403 ? 'O servidor recusou o código' : r.status === 413 ? 'Dados grandes demais para o servidor' : `Erro do servidor (${r.status}${r.j.message ? ': ' + r.j.message : ''})`);

/* ================= Criptografia ================= */
const SYNC_ALPHA = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; // sem 0/O, 1/I/L
function newSyncCode() {
  let s = '';
  while (s.length < 20) for (const b of crypto.getRandomValues(new Uint8Array(32))) if (b < 248 && s.length < 20) s += SYNC_ALPHA[b % 31];
  return s;
}
const fmtCode = c => c.match(/.{1,4}/g).join('-');
const cleanCode = s => [...String(s).toUpperCase()].filter(c => SYNC_ALPHA.includes(c)).join('');
const hexOf = buf => [...new Uint8Array(buf)].map(b => b.toString(16).padStart(2, '0')).join('');
const sha256 = async s => hexOf(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s)));
let syncKeyCache = null;
async function syncKeys(code) {
  if (syncKeyCache && syncKeyCache.code === code) return syncKeyCache;
  const te = new TextEncoder(), raw = await crypto.subtle.importKey('raw', te.encode(code), 'PBKDF2', false, ['deriveKey']);
  const key = await crypto.subtle.deriveKey({ name: 'PBKDF2', salt: te.encode('ficha-sync-v1'), iterations: 150000, hash: 'SHA-256' }, raw, { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
  return (syncKeyCache = { code, key, id: await sha256('ficha-id:' + code), auth: await sha256('ficha-auth:' + code) });
}
async function gzip(bytes, compress) {
  if (typeof CompressionStream === 'undefined') return null;
  const s = new Blob([bytes]).stream().pipeThrough(compress ? new CompressionStream('gzip') : new DecompressionStream('gzip'));
  return new Uint8Array(await new Response(s).arrayBuffer());
}
const toB64 = u8 => { let s = ''; for (let i = 0; i < u8.length; i += 0x8000) s += String.fromCharCode(...u8.subarray(i, i + 0x8000)); return btoa(s); };
const fromB64 = s => Uint8Array.from(atob(s), c => c.charCodeAt(0));
async function seal(obj, k) {
  let data = new TextEncoder().encode(JSON.stringify(obj)), tag = 'r1';
  const z = await gzip(data, true);
  if (z) { data = z; tag = 'g1'; }
  const iv = crypto.getRandomValues(new Uint8Array(12));
  return `${tag}:${toB64(iv)}:${toB64(new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, k.key, data)))}`;
}
async function unseal(str, k) {
  const [tag, iv, ct] = String(str).split(':');
  let data;
  try { data = new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: fromB64(iv) }, k.key, fromB64(ct))); }
  catch (e) { throw new CloudError('Não foi possível abrir os dados da nuvem com este código'); }
  if (tag === 'g1') { data = await gzip(data, false); if (!data) throw new CloudError('Este navegador não abre os dados da nuvem (atualize o sistema)'); }
  return JSON.parse(new TextDecoder().decode(data));
}

/* ================= Mesclagem entre aparelhos =================
   Três vias: compara o que mudou aqui e na nuvem desde a última sincronização (a "base").
   Mudou só de um lado → fica a mudança. Mudou dos dois → vale a versão do aparelho que mexeu por último.
   Excluir de um lado e não mexer do outro → excluído. */
const J = x => x === undefined ? undefined : JSON.stringify(x);
const isObj = x => !!x && typeof x === 'object' && !Array.isArray(x);
const idList = (...arrs) => arrs.every(a => Array.isArray(a) && a.every(x => isObj(x) && x.id != null));
function mergeAny(b, l, r, newerL) {
  const sl = J(l), sr = J(r), sb = J(b);
  if (sl === sr) return l;
  if (sl === sb) return r;   // só a nuvem mudou (inclusive excluir)
  if (sr === sb) return l;   // só este aparelho mudou
  if (l === undefined) return r; // editar vence excluir
  if (r === undefined) return l;
  if (isObj(l) && isObj(r)) return mergeObj(b, l, r, newerL);
  if (idList(l, r)) return mergeList(b, l, r, newerL);
  return newerL ? l : r;     // mudou dos dois lados: vale o aparelho que mexeu por último
}
function mergeObj(b, l, r, newerL, fields = {}) {
  b = isObj(b) ? b : {};
  if (!isObj(l) || !isObj(r)) return mergeAny(b, l, r, newerL);
  const out = {};
  for (const k of new Set([...Object.keys(l), ...Object.keys(r)])) {
    const v = fields[k] ? fields[k](b[k], l[k], r[k]) : mergeAny(b[k], l[k], r[k], newerL);
    if (v !== undefined) out[k] = v;
  }
  return out;
}
// Listas de registros com id: junta por id; a ordem segue o lado que reordenou
function mergeList(b, l, r, newerL) {
  b = Array.isArray(b) ? b : []; l = Array.isArray(l) ? l : []; r = Array.isArray(r) ? r : [];
  if (!idList(b, l, r)) return mergeAny(b, l, r, newerL);
  const bm = new Map(b.map(x => [x.id, x])), lm = new Map(l.map(x => [x.id, x])), rm = new Map(r.map(x => [x.id, x]));
  // Reordenou? Compara a ordem dos itens que já existiam na base
  const moved = (a, m) => J(a.filter(x => bm.has(x.id)).map(x => x.id)) !== J(b.filter(x => m.has(x.id)).map(x => x.id));
  const lFirst = moved(l, lm) || !moved(r, rm);
  const [p, q] = lFirst ? [l, r] : [r, l], pm = new Map(p.map(x => [x.id, 1]));
  const out = [];
  for (const id of [...p.map(x => x.id), ...q.map(x => x.id).filter(k => !pm.has(k))]) {
    const v = mergeAny(bm.get(id), lm.get(id), rm.get(id), newerL);
    if (v !== undefined) out.push(v);
  }
  return out;
}
function mergeState(B, L, R) {
  B = B || {};
  const nl = (L.mt || 0) >= (R.mt || 0);
  const sorted = f => (b, l, r) => { const v = mergeAny(b, l, r, nl); return Array.isArray(v) ? [...v].sort(f) : v; };
  return mergeObj(B, L, R, nl, {
    sessions: sorted((x, y) => y.start - x.start),
    body: sorted((x, y) => x.t - y.t),
    settings: (b, l, r) => {
      const out = { ...(l || {}) };
      for (const k of SYNC_SETTINGS) { const v = mergeAny((b || {})[k], (l || {})[k], (r || {})[k], nl); if (v === undefined) delete out[k]; else out[k] = v; }
      return out;
    },
    ach: (b, l, r) => {
      if (!l && !r) return undefined;
      const out = { ...(r || {}) };
      for (const [k, v] of Object.entries(l || {})) out[k] = out[k] ? Math.min(out[k], v) : v;
      return out;
    },
    active: () => undefined, photos: () => undefined,
    mt: () => Math.max(L.mt || 0, R.mt || 0)
  });
}

/* ================= Sincronização ================= */
let cloudBase = null, cloudBaseCmp = null; // última versão em comum (texto)
let cloudBusy = null, cloudAgain = false, cloudTimer = 0, cloudApplying = false, cloudPulled = 0;
let cloudStatus = { at: 0, err: '' };
const syncable = s => { const o = { ...s }; delete o.active; delete o.photos; return o; };
const cmpStr = o => JSON.stringify({ ...o, mt: 0 });

let syncDbP = null;
function syncDb() {
  return syncDbP || (syncDbP = new Promise((res, rej) => {
    const r = indexedDB.open('ficha-sync', 1);
    r.onupgradeneeded = () => r.result.createObjectStore('kv');
    r.onsuccess = () => res(r.result); r.onerror = () => { syncDbP = null; rej(r.error); };
  }));
}
async function sdb(key, val) {
  const db = await syncDb();
  return new Promise((res, rej) => {
    const t = db.transaction('kv', val === undefined ? 'readonly' : 'readwrite'), st = t.objectStore('kv');
    const q = val === undefined ? st.get(key) : val === null ? st.delete(key) : st.put(val, key);
    t.oncomplete = () => res(q.result); t.onerror = () => rej(t.error);
  });
}
async function loadBase(id) {
  if (cloudBase !== null) return;
  const v = await sdb('base').catch(() => null);
  cloudBase = v && v.id === id ? v.s : '';
  cloudBaseCmp = cloudBase ? cmpStr(JSON.parse(cloudBase)) : '';
}
async function setBase(id, obj) {
  cloudBase = obj ? JSON.stringify(obj) : '';
  cloudBaseCmp = obj ? cmpStr(obj) : '';
  await sdb('base', obj ? { id, s: cloudBase } : null).catch(() => { });
}
function cloudSafeToRender() {
  const a = document.activeElement;
  return $('#sheet').hidden && !(a && /^(INPUT|TEXTAREA|SELECT)$/.test(a.tagName));
}
function mergeIn(remote) {
  const merged = mergeState(cloudBase ? JSON.parse(cloudBase) : null, syncable(S), remote);
  merged.active = S.active; merged.photos = S.photos;
  if (S.ach && !merged.ach) merged.ach = S.ach;
  const before = JSON.stringify(S);
  for (const k of Object.keys(S)) if (!(k in merged)) delete S[k];
  Object.assign(S, merged);
  if (JSON.stringify(S) === before) return false;
  cloudApplying = true; save(); cloudApplying = false;
  if (typeof applyLook === 'function') applyLook();
  if (cloudSafeToRender()) rerender();
  return true;
}
// mode 'pull': busca a nuvem antes; 'push': envia direto e só busca se houver conflito
function cloudSync(mode = 'push') {
  if (!cloudOn()) return Promise.resolve();
  if (cloudBusy) { cloudAgain = true; return cloudBusy; }
  clearTimeout(cloudTimer);
  cloudBusy = (async () => {
    const cfg = cloudCfg(), k = await syncKeys(cfg.code);
    await loadBase(k.id);
    let remote = null;
    if (mode === 'pull' || cfg.v == null) {
      const r = await cloudApi('GET', '/v1/data/' + k.id, null, k.auth);
      if (r.status === 200) remote = r.j;
      else if (r.status === 404) remote = { v: 0 };
      else throw cloudHttpErr(r);
      cloudPulled = Date.now();
    }
    for (let tries = 0; tries < 4; tries++) {
      if (remote && remote.v !== cfg.v) {
        if (remote.d) { const rs = await unseal(remote.d, k); mergeIn(rs); await setBase(k.id, rs); }
        else await setBase(k.id, null);
        cfg.v = remote.v; setCloudCfg({ ...cloudCfg(), v: cfg.v });
      }
      remote = null;
      if (cmpStr(syncable(S)) === cloudBaseCmp) break; // nada novo para enviar
      S.mt = Date.now();
      cloudApplying = true; save(); cloudApplying = false;
      const data = syncable(S);
      const r = await cloudApi('PUT', '/v1/data/' + k.id, { base: cfg.v || 0, d: await seal(data, k) }, k.auth);
      if (r.status === 200) { cfg.v = r.j.v; setCloudCfg({ ...cloudCfg(), v: cfg.v }); await setBase(k.id, data); break; }
      if (r.status === 409) { remote = { v: r.j.v, d: r.j.d }; continue; }
      throw cloudHttpErr(r);
    }
    cloudStatus = { at: Date.now(), err: '' };
    setCloudCfg({ ...cloudCfg(), at: cloudStatus.at });
  })().catch(e => {
    cloudStatus = { at: cloudStatus.at, err: e instanceof CloudError ? e.message : 'Falha ao sincronizar' };
    if (!(e instanceof CloudError)) console.warn(e);
  }).finally(() => {
    cloudBusy = null;
    if (location.hash === '#/nuvem' && cloudSafeToRender()) rerender();
    if (cloudAgain) { cloudAgain = false; cloudSoon(); }
  });
  return cloudBusy;
}
// Chamado a cada save(): envia alguns segundos depois da última mudança
function cloudSoon() {
  if (cloudApplying) return;
  if (cloudOn()) { clearTimeout(cloudTimer); cloudTimer = setTimeout(() => cloudSync('push'), 5000); }
  if (pushCfg().on) { clearTimeout(pushTimer); pushTimer = setTimeout(pushRefresh, 4000); }
}
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    if (cloudOn() && cloudTimer) { clearTimeout(cloudTimer); cloudTimer = 0; cloudSync('push'); }
    if (pushCfg().on) pushRefresh();
  } else if (cloudOn() && Date.now() - cloudPulled > 20000) cloudSync('pull');
});

/* ================= Lembretes (Web Push) ================= */
let pushTimer = 0, pushKeyCache = null;
function pushCfg() {
  const c = lsGet(PUSH_KEY) || {};
  const d = { treino: { on: true, hm: '08:00' }, agua: { on: false, from: 9, to: 21, every: 3 }, meals: { on: false, hm: ['08:00', '12:30', '16:00', '20:00'] }, peso: { on: true, day: 1, hm: '07:30' }, backup: { on: false, day: 0, hm: '20:00' } };
  const rem = c.rem || {};
  for (const k of Object.keys(d)) rem[k] = { ...d[k], ...(rem[k] || {}) };
  return { on: false, ...c, rem };
}
const setPushCfg = c => lsSet(PUSH_KEY, c);
function pushSupport() {
  const ios = /iPhone|iPad|iPod/.test(navigator.userAgent) || (/Macintosh/.test(navigator.userAgent) && navigator.maxTouchPoints > 1);
  const standalone = navigator.standalone || matchMedia('(display-mode: standalone)').matches;
  if ('serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window) return { ok: true, ios };
  if (ios && !standalone) return { ok: false, msg: 'No iPhone, as notificações funcionam com o app instalado: no Safari, toque em Compartilhar → Adicionar à Tela de Início e abra o Ficha por lá (iOS 16.4 ou mais novo).' };
  return { ok: false, msg: 'Este navegador não recebe notificações de sites.' };
}
const waterGoalMl = () => Math.round(((typeof dietWeight === 'function' && dietWeight()) || 70) * 35 / 50) * 50;
const hm = h => `${String(Math.floor(h)).padStart(2, '0')}:${String(Math.round(h % 1 * 60)).padStart(2, '0')}`;
// Lista de lembretes que vai para o servidor (o texto é montado aqui, com os nomes das fichas)
function pushReminders(R) {
  const out = [], ALL = [0, 1, 2, 3, 4, 5, 6];
  if (R.treino.on) {
    const sched = S.routines.filter(r => r.items.length && (typeof isScheduled !== 'function' || isScheduled(r)));
    for (const d of ALL) {
      const rs = sched.filter(r => (r.days || []).includes(d));
      if (!rs.length) continue;
      const n = sum(rs.map(r => r.items.length));
      out.push({ id: 'treino' + d, kind: 'treino', days: [d], hm: R.treino.hm, title: 'Dia de treinar 💪', body: `${rs.map(r => r.name || 'Treino').join(' + ')} · ${n} exercício${n > 1 ? 's' : ''}`, url: './#/hoje' });
    }
  }
  if (R.agua.on) {
    for (let h = +R.agua.from; h <= +R.agua.to; h += Math.max(1, +R.agua.every)) {
      out.push({ id: 'agua' + h, kind: 'agua', days: ALL, hm: hm(h), title: 'Hora de beber água 💧', body: `Meta de hoje: ${fmt(waterGoalMl() / 1000, 1)} L. Registre na Dieta.`, url: './#/dieta' });
    }
  }
  if (R.meals.on) {
    const names = typeof MEALS !== 'undefined' ? MEALS : ['Café da manhã', 'Almoço', 'Lanche', 'Jantar'];
    R.meals.hm.forEach((t, m) => { if (t) out.push({ id: 'meal' + m, kind: 'meal' + m, days: ALL, hm: t, title: `${names[m]} 🍽️`, body: 'Registre o que você comeu na Dieta.', url: './#/dieta' }); });
  }
  if (R.peso.on) out.push({ id: 'peso', kind: 'peso', days: [+R.peso.day], hm: R.peso.hm, title: 'Pesagem da semana ⚖️', body: 'De manhã, em jejum e depois de ir ao banheiro. Registre no Corpo.', url: './#/corpo' });
  if (R.backup.on) out.push({ id: 'backup', kind: 'backup', days: [+R.backup.day], hm: R.backup.hm, title: 'Backup da semana', body: 'Salve um backup dos seus treinos no iCloud Drive.', url: './#/ajustes' });
  return out;
}
// O que já foi feito hoje: o servidor não manda o lembrete disso
function pushDoneToday() {
  const t0 = startOfDay(Date.now()), key = isoDate(t0), kinds = [];
  if (sessionsInRange(t0, addDays(t0, 1)).length) kinds.push('treino');
  const day = S.food && S.food.days && S.food.days[key];
  if (day) {
    [0, 1, 2, 3].forEach(m => { if ((day.e || []).some(e => e.m === m)) kinds.push('meal' + m); });
    if ((day.w || 0) >= waterGoalMl()) kinds.push('agua');
  }
  if ((S.body || []).some(e => e.peso && e.t >= addDays(t0, -6))) kinds.push('peso');
  if (Date.now() - (S.settings.lastBackup || 0) < 6 * 864e5 || cloudOn()) kinds.push('backup');
  return { date: key, kinds };
}
const b64uToBytes = s => fromB64(s.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - s.length % 4) % 4));
async function pushKey() {
  if (pushKeyCache) return pushKeyCache;
  const r = await cloudApi('GET', '/v1/push/key');
  if (r.status !== 200 || !r.j.key) throw cloudHttpErr(r);
  return (pushKeyCache = r.j.key);
}
async function pushSubscription(create) {
  const reg = await navigator.serviceWorker.ready;
  let sub = await reg.pushManager.getSubscription();
  if (!create) return sub;
  const key = await pushKey();
  const cur = sub && sub.options && sub.options.applicationServerKey;
  if (sub && cur && toB64(new Uint8Array(cur)).replace(/=+$/, '') !== toB64(b64uToBytes(key)).replace(/=+$/, '')) { await sub.unsubscribe(); sub = null; }
  return sub || reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: b64uToBytes(key) });
}
// Manda inscrição e horários (só quando mudam) e o que já foi feito hoje
async function pushRefresh(force) {
  const c = pushCfg();
  if (!c.on || !cloudCfg().url) return;
  try {
    const sub = await pushSubscription(false);
    if (!sub) { setPushCfg({ ...c, on: false }); return; }
    const body = { sub: sub.toJSON(), tz: Intl.DateTimeFormat().resolvedOptions().timeZone, rem: pushReminders(c.rem) };
    const sig = await sha256(cloudCfg().url + JSON.stringify(body));
    if (force || sig !== c.sig) {
      const r = await cloudApi('PUT', '/v1/push/' + c.sid, body);
      if (r.status !== 200) throw cloudHttpErr(r);
      setPushCfg({ ...pushCfg(), sig, doneSig: '' });
    }
    const done = pushDoneToday(), dsig = JSON.stringify(done);
    if (dsig !== pushCfg().doneSig) {
      const r = await cloudApi('POST', `/v1/push/${c.sid}/done`, done);
      if (r.status === 200) setPushCfg({ ...pushCfg(), doneSig: dsig });
    }
  } catch (e) { if (force) throw e; }
}
async function pushEnable(permission) {
  const perm = await permission;
  if (perm !== 'granted') {
    toast(perm === 'denied' ? 'Notificações bloqueadas. Libere nos Ajustes do aparelho → Notificações → Ficha' : 'Permissão não concedida');
    rerender(); return;
  }
  try {
    await pushSubscription(true);
    const c = pushCfg();
    setPushCfg({ ...c, on: true, sid: c.sid || hexOf(crypto.getRandomValues(new Uint8Array(16))), sig: '' });
    await pushRefresh(true);
    toast('Lembretes ativados');
  } catch (e) {
    setPushCfg({ ...pushCfg(), on: false });
    toast(e instanceof CloudError ? e.message : 'Não foi possível ativar as notificações');
  }
  rerender();
}
async function pushDisable() {
  const c = pushCfg();
  setPushCfg({ ...c, on: false, sig: '' });
  try { if (c.sid) await cloudApi('DELETE', '/v1/push/' + c.sid); } catch (e) { /* offline: o servidor apaga quando a inscrição expirar */ }
  try { const s = await pushSubscription(false); if (s) await s.unsubscribe(); } catch (e) { /* ignora */ }
  rerender();
}

/* ================= Tela ================= */
function cloudAgo(t) {
  if (!t) return 'ainda não';
  const s = Math.round((Date.now() - t) / 1000);
  return s < 60 ? 'agora há pouco' : s < 3600 ? `há ${Math.round(s / 60)} min` : `${relDay(t).toLowerCase()} às ${timeHM(t)}`;
}
function viewNuvem() {
  const cfg = cloudCfg(), on = cloudOn(), pc = pushCfg(), sup = pushSupport();
  let html = topBar({ back: '#/ajustes' }) + `<div class="eyebrow">Entre aparelhos e lembretes</div><h1 class="title">Nuvem e lembretes</h1>
    <h2 class="section">Servidor</h2>
    <div class="card cloud-srv"><label class="field" style="margin:0"><span>Endereço do seu servidor</span>
      <input class="input" type="url" inputmode="url" autocapitalize="off" autocomplete="off" spellcheck="false" placeholder="https://ficha-sync.seu-nome.workers.dev" value="${esc(cfg.url || '')}" data-cloud-url></label>
      <div class="btn-row" style="margin-top:10px"><button class="btn sm" data-act="cloudTest">Testar conexão</button><a class="btn sm" href="${CLOUD_HELP}" target="_blank" rel="noopener">Como criar (grátis)</a></div>
      <p class="small muted" style="margin:10px 0 0;line-height:1.5">O Ficha não tem servidor próprio: você cria o seu na Cloudflare, de graça, em uns 5 minutos, e só você usa.</p></div>`;

  html += `<h2 class="section">Sincronização</h2>`;
  if (!cfg.url) html += `<p class="small muted" style="margin:0 4px">Configure o servidor acima para sincronizar.</p>`;
  else if (!on) {
    html += `<div class="card"><p style="margin:0 0 12px;line-height:1.5">Mantenha os mesmos treinos, fichas, dieta e medidas no iPhone, iPad ou outro celular — e não perca nada se trocar de aparelho.</p>
      <div class="stack"><button class="btn primary block" data-act="cloudCreate">Começar a sincronizar</button>
      <button class="btn block" data-act="cloudJoin">Já tenho um código</button></div></div>`;
  } else {
    const st = cloudBusy ? 'Sincronizando…' : cloudStatus.err ? cloudStatus.err : `Sincronizado ${cloudAgo(cloudStatus.at || cfg.at)}`;
    html += `<div class="list">
      <div class="row"><div class="grow"><div class="name">${cloudStatus.err ? 'Não sincronizou' : 'Sincronização ativa'}</div><div class="sub ${cloudStatus.err ? 'err' : ''}">${esc(st)}</div></div><span class="badge ${cloudStatus.err ? 'gold' : 'accent'}">${cloudStatus.err ? 'Erro' : 'Ativa'}</span></div>
      <button class="row" data-act="cloudNow"><div class="grow name">Sincronizar agora</div>${I.chev}</button>
      <button class="row" data-act="cloudCode"><div class="grow"><div class="name">Código de sincronização</div><div class="sub">Use para entrar nos outros aparelhos</div></div>${I.chev}</button>
      <button class="row" data-act="cloudOff"><div class="grow name">Desligar neste aparelho</div></button>
      <button class="row" data-act="cloudWipe"><div class="grow name" style="color:var(--danger)">Apagar meus dados da nuvem</div></button></div>`;
  }
  html += `<p class="small muted" style="margin:10px 4px 0;line-height:1.5">Criptografia de ponta a ponta: os dados saem do aparelho embaralhados com o seu código, e o servidor não consegue ler. As fotos do progresso e o treino em andamento ficam só em cada aparelho.</p>`;

  html += `<h2 class="section">Lembretes</h2>`;
  if (!sup.ok) html += `<div class="card small" style="line-height:1.5">${sup.msg}</div>`;
  else if (!cfg.url) html += `<p class="small muted" style="margin:0 4px">Os lembretes saem do seu servidor: configure-o acima.</p>`;
  else {
    const R = pc.rem, sw = (path, v) => `<span class="switch"><input type="checkbox" data-rem="${path}" ${v ? 'checked' : ''}><i></i></span>`;
    const time = (path, v) => `<input class="input num rem-time" type="time" value="${esc(v)}" data-rem="${path}">`;
    const daySel = (path, v) => `<select class="input rem-sel" data-rem="${path}">${DAY_LONG.map((d, i) => `<option value="${i}" ${+v === i ? 'selected' : ''}>${d}</option>`).join('')}</select>`;
    const numSel = (path, v, opts, f) => `<select class="input num rem-sel" data-rem="${path}">${opts.map(o => `<option value="${o}" ${+v === o ? 'selected' : ''}>${f(o)}</option>`).join('')}</select>`;
    const sched = S.routines.filter(r => r.items.length && (typeof isScheduled !== 'function' || isScheduled(r)));
    const tDays = [...new Set(sched.flatMap(r => r.days || []))];
    html += `<div class="list">
      <label class="row"><div class="grow"><div class="name">Notificações neste aparelho</div><div class="sub">${pc.on ? 'Ativadas' : 'Desligadas'}</div></div><span class="switch"><input type="checkbox" data-push-on ${pc.on ? 'checked' : ''}><i></i></span></label></div>`;
    if (pc.on) {
      const names = typeof MEALS !== 'undefined' ? MEALS : ['Café da manhã', 'Almoço', 'Lanche', 'Jantar'];
      html += `<div class="list rem-list" style="margin-top:12px">
        <div class="rem"><label class="rem-top"><div class="grow"><div class="name">Treino do dia</div><div class="sub">${tDays.length ? daysLabel(tDays) + ' · pelos dias das fichas' : 'Defina os dias nas fichas para receber'}</div></div>${sw('treino.on', R.treino.on)}</label>
          ${R.treino.on ? `<div class="rem-opts"><span>Horário</span>${time('treino.hm', R.treino.hm)}</div>` : ''}</div>
        <div class="rem"><label class="rem-top"><div class="grow"><div class="name">Beber água</div><div class="sub">Para quando bater a meta do dia</div></div>${sw('agua.on', R.agua.on)}</label>
          ${R.agua.on ? `<div class="rem-opts"><span>Das</span>${numSel('agua.from', R.agua.from, [6, 7, 8, 9, 10, 11, 12], o => o + 'h')}<span>às</span>${numSel('agua.to', R.agua.to, [16, 17, 18, 19, 20, 21, 22, 23], o => o + 'h')}<span>a cada</span>${numSel('agua.every', R.agua.every, [1, 2, 3, 4], o => o + 'h')}</div>` : ''}</div>
        <div class="rem"><label class="rem-top"><div class="grow"><div class="name">Registrar refeições</div><div class="sub">Não avisa a refeição já registrada</div></div>${sw('meals.on', R.meals.on)}</label>
          ${R.meals.on ? `<div class="rem-meals">${names.map((n, m) => `<label><span>${n}</span>${time('meals.hm.' + m, R.meals.hm[m])}</label>`).join('')}</div>` : ''}</div>
        <div class="rem"><label class="rem-top"><div class="grow"><div class="name">Pesagem semanal</div><div class="sub">Não avisa se já pesou nos últimos dias</div></div>${sw('peso.on', R.peso.on)}</label>
          ${R.peso.on ? `<div class="rem-opts">${daySel('peso.day', R.peso.day)}${time('peso.hm', R.peso.hm)}</div>` : ''}</div>
        <div class="rem"><label class="rem-top"><div class="grow"><div class="name">Backup semanal</div><div class="sub">${on ? 'Com a sincronização ativa, seus dados já ficam na nuvem' : 'Lembra de salvar o arquivo de backup'}</div></div>${sw('backup.on', R.backup.on)}</label>
          ${R.backup.on ? `<div class="rem-opts">${daySel('backup.day', R.backup.day)}${time('backup.hm', R.backup.hm)}</div>` : ''}</div>
      </div>
      <button class="btn block" style="margin-top:12px" data-act="pushTest">Enviar notificação de teste</button>`;
    }
    html += `<p class="small muted" style="margin:10px 4px 0;line-height:1.5">Os lembretes saem do seu servidor no horário deste aparelho (${esc(Intl.DateTimeFormat().resolvedOptions().timeZone)}), com até 5 minutos de diferença. O que você já fez no dia não é lembrado. Cada aparelho tem os seus lembretes.</p>`;
  }
  return html;
}
function codeSheet(code, fresh) {
  openSheet(`${sheetHead(fresh ? 'Guarde seu código' : 'Código de sincronização')}<div class="sheet-body">
    <div class="sync-code num">${fmtCode(code)}</div>
    <p class="muted" style="margin:12px 0 0;line-height:1.5">${fresh ? 'Pronto: seus dados estão na nuvem. ' : ''}Para usar em outro aparelho, abra o Ficha nele, vá em Ajustes → Nuvem e lembretes → <b>Já tenho um código</b> e digite este código.</p>
    <p class="small muted" style="margin:10px 0 0;line-height:1.5">Ele é a chave dos seus dados: sem ele ninguém consegue ler — nem você, se perder. Guarde no app Senhas ou nas Notas. Não compartilhe com outras pessoas.</p></div>
    <div class="sheet-foot stack">
      ${navigator.share ? `<button class="btn primary block" data-act="cloudShareCode">${I.share}Compartilhar / guardar</button>` : ''}
      <button class="btn block ${navigator.share ? '' : 'primary'}" data-act="cloudCopyCode">${I.copy}Copiar código</button></div>`);
}
function cloudInput(t) { return t.hasAttribute('data-cloud-url') || t.id === 'joinCode'; }
function cloudChange(t) {
  if (t.hasAttribute('data-cloud-url')) {
    let v = t.value.trim().replace(/\/+$/, '');
    if (v && !/^https?:\/\//i.test(v)) v = 'https://' + v;
    if (v !== cloudCfg().url) { setCloudCfg({ ...cloudCfg(), url: v, v: null }); pushKeyCache = null; cloudBase = null; t.value = v; }
    return true;
  }
  if (t.hasAttribute('data-push-on')) {
    if (t.checked) pushEnable(Notification.requestPermission());
    else pushDisable();
    return true;
  }
  if (t.dataset.rem) {
    const c = pushCfg(), path = t.dataset.rem.split('.');
    let o = c.rem;
    for (const k of path.slice(0, -1)) o = o[k];
    const last = path[path.length - 1];
    o[last] = t.type === 'checkbox' ? t.checked : t.tagName === 'SELECT' ? +t.value : t.value;
    setPushCfg(c);
    if (t.type === 'checkbox') rerender();
    clearTimeout(pushTimer); pushTimer = setTimeout(pushRefresh, 800);
    return true;
  }
  return false;
}

// Desliga a nuvem neste aparelho sem apagar a cópia do servidor (usado ao apagar os dados do aparelho)
function cloudForget() {
  clearTimeout(cloudTimer); cloudTimer = 0;
  setCloudCfg({ ...cloudCfg(), code: null, v: null }); cloudBase = null;
  sdb('base', null).catch(() => { });
  if (pushCfg().on) pushDisable();
}

const CLOUD_ACTIONS = {
  cloudTest: async () => {
    const u = $('[data-cloud-url]');
    if (u) cloudChange(u);
    if (!cloudCfg().url) { toast('Digite o endereço do servidor'); return; }
    try {
      const r = await cloudApi('GET', '/v1/ping');
      toast(r.status === 200 && r.j.app === 'ficha' ? 'Conectado ao servidor ✓' : r.j.message || `O endereço respondeu, mas não é um servidor do Ficha (${r.status})`);
    } catch (e) { toast(e.message || 'Sem conexão com o servidor'); }
  },
  cloudCreate: async () => {
    const code = newSyncCode();
    setCloudCfg({ ...cloudCfg(), code, v: null, at: 0 });
    cloudBase = null; cloudStatus = { at: 0, err: '' };
    rerender();
    await cloudSync('pull');
    if (cloudStatus.err) { setCloudCfg({ ...cloudCfg(), code: null, v: null }); toast(cloudStatus.err); rerender(); return; }
    codeSheet(code, true);
  },
  cloudJoin: () => openSheet(`${sheetHead('Entrar com um código')}<div class="sheet-body">
      <label class="field"><span>Código de sincronização</span><input class="input num" id="joinCode" autocapitalize="characters" autocomplete="off" spellcheck="false" placeholder="XXXX-XXXX-XXXX-XXXX-XXXX"></label>
      <p class="small muted" style="margin:0 2px;line-height:1.5">Os dados deste aparelho serão juntados com os da nuvem (nada é apagado).</p></div>
      <div class="sheet-foot"><button class="btn primary block" data-act="cloudJoinGo">Entrar</button></div>`),
  cloudJoinGo: async el => {
    const code = cleanCode(($('#joinCode') || {}).value || '');
    if (code.length !== 20) { toast('O código tem 20 letras e números'); return; }
    el.disabled = true; el.textContent = 'Conferindo…';
    try {
      const k = await syncKeys(code), r = await cloudApi('GET', '/v1/data/' + k.id, null, k.auth);
      if (r.status === 404) throw new CloudError('Nenhum dado com este código. Confira as letras.');
      if (r.status !== 200) throw cloudHttpErr(r);
      await unseal(r.j.d, k);
    } catch (e) {
      el.disabled = false; el.textContent = 'Entrar';
      toast(e instanceof CloudError ? e.message : 'Não foi possível entrar'); return;
    }
    setCloudCfg({ ...cloudCfg(), code, v: null, at: 0 });
    cloudBase = null; cloudStatus = { at: 0, err: '' };
    closeSheet();
    await cloudSync('pull');
    toast(cloudStatus.err || 'Dados sincronizados ✓');
    rerender();
  },
  cloudNow: async () => { await cloudSync('pull'); toast(cloudStatus.err || 'Sincronizado ✓'); },
  cloudCode: () => { const c = cloudCfg().code; if (c) codeSheet(c, false); },
  cloudCopyCode: async () => {
    try { await navigator.clipboard.writeText(fmtCode(cloudCfg().code)); toast('Código copiado'); } catch (e) { toast('Não foi possível copiar'); }
  },
  cloudShareCode: async () => {
    try { await navigator.share({ text: `Código de sincronização do Ficha: ${fmtCode(cloudCfg().code)}` }); } catch (e) { /* cancelado */ }
  },
  cloudOff: () => confirmSheet({
    title: 'Desligar a sincronização?', text: 'Os dados continuam neste aparelho e na nuvem. Para voltar, use o código.', ok: 'Desligar',
    onOk: async () => { setCloudCfg({ ...cloudCfg(), code: null, v: null }); cloudBase = null; await sdb('base', null).catch(() => { }); rerender(); }
  }),
  cloudWipe: () => confirmSheet({
    title: 'Apagar os dados da nuvem?', danger: true, ok: 'Apagar da nuvem',
    text: 'Apaga a cópia do servidor e desliga a sincronização neste aparelho. Os dados deste aparelho continuam aqui. Desligue também nos outros aparelhos, senão eles enviam tudo de novo.',
    onOk: async () => {
      try {
        const k = await syncKeys(cloudCfg().code), r = await cloudApi('DELETE', '/v1/data/' + k.id, null, k.auth);
        if (r.status !== 200) throw cloudHttpErr(r);
        setCloudCfg({ ...cloudCfg(), code: null, v: null }); cloudBase = null; await sdb('base', null).catch(() => { });
        toast('Dados apagados da nuvem');
      } catch (e) { toast(e instanceof CloudError ? e.message : 'Não foi possível apagar'); }
      rerender();
    }
  }),
  pushTest: async () => {
    try {
      await pushRefresh(true);
      const r = await cloudApi('POST', `/v1/push/${pushCfg().sid}/test`);
      toast(r.status === 200 ? 'Enviada — deve chegar em instantes' : r.status === 404 ? 'Inscrição não encontrada: desligue e ligue as notificações' : 'O serviço de notificações recusou o envio');
    } catch (e) { toast(e instanceof CloudError ? e.message : 'Não foi possível enviar'); }
  }
};

// Ao abrir o app
if (cloudOn()) setTimeout(() => cloudSync('pull'), 300);
if (pushCfg().on) setTimeout(() => pushRefresh(), 1500);
