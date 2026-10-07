/* Ficha — servidor de sincronização e lembretes (Cloudflare Worker + KV)
 *
 * Sincronização: guarda um único arquivo criptografado por usuário. A criptografia é feita no
 * aparelho com o código de sincronização; o servidor nunca vê os dados nem o código.
 * Lembretes: guarda a inscrição de notificação de cada aparelho e os horários; um gatilho (cron)
 * a cada 5 minutos envia os lembretes na hora certa, no fuso de cada aparelho (Web Push com VAPID).
 *
 * Precisa de: um KV namespace ligado com o nome FICHA e um Cron Trigger a cada 5 minutos (veja server/README.md).
 * Variáveis opcionais: ALLOW_ORIGIN (ex.: https://drakrosmann.github.io) e VAPID_SUBJECT (mailto: ou https:).
 */

const MAX_DATA = 8 * 1024 * 1024;
const PUSH_HOSTS = ['push.apple.com', 'fcm.googleapis.com', 'push.services.mozilla.com', 'notify.windows.com'];

export default {
  async fetch(req, env, ctx) {
    const cors = {
      'Access-Control-Allow-Origin': env.ALLOW_ORIGIN || '*',
      'Access-Control-Allow-Methods': 'GET, PUT, POST, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Authorization, Content-Type',
      'Access-Control-Max-Age': '86400'
    };
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });
    const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { ...cors, 'Content-Type': 'application/json', 'Cache-Control': 'no-store' } });
    try {
      const res = await route(req, env, ctx, json);
      return res || json({ error: 'not_found' }, 404);
    } catch (e) {
      return json({ error: e.code || 'server_error', message: e.message }, e.status || 500);
    }
  },
  async scheduled(controller, env, ctx) {
    ctx.waitUntil(sendDue(env, Date.now()));
  }
};

function fail(status, code, message = code) { const e = new Error(message); e.status = status; e.code = code; throw e; }
async function body(req, max = 256 * 1024) {
  const text = await req.text();
  if (text.length > max) fail(413, 'too_large');
  try { return JSON.parse(text || '{}'); } catch (e) { fail(400, 'bad_json'); }
}

async function route(req, env, ctx, json) {
  const url = new URL(req.url), p = url.pathname.replace(/\/+$/, ''), m = req.method;
  if (!env.FICHA) fail(500, 'no_kv', 'Ligue um KV namespace com o nome FICHA ao Worker');
  if (p === '/v1/ping' || p === '' || p === '/') return json({ ok: true, app: 'ficha', v: 1, push: true });

  // ===== Sincronização =====
  let r = p.match(/^\/v1\/data\/([a-f0-9]{64})$/);
  if (r) {
    const key = 'd:' + r[1], auth = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
    if (!/^[a-f0-9]{64}$/.test(auth)) fail(401, 'no_auth');
    const h = await sha256hex(auth), cur = await env.FICHA.get(key, 'json');
    if (cur && cur.h !== h) fail(403, 'forbidden');
    if (m === 'GET') return cur ? json({ v: cur.v, d: cur.d, at: cur.at }) : json({ error: 'empty' }, 404);
    if (m === 'PUT') {
      const b = await body(req, MAX_DATA + 1024);
      if (typeof b.d !== 'string' || b.d.length > MAX_DATA) fail(400, 'bad_data');
      const v = cur ? cur.v : 0;
      if ((b.base | 0) !== v) return json({ error: 'conflict', v, d: cur && cur.d, at: cur && cur.at }, 409);
      const at = Date.now();
      await env.FICHA.put(key, JSON.stringify({ v: v + 1, d: b.d, at, h }));
      return json({ v: v + 1, at });
    }
    if (m === 'DELETE') { await env.FICHA.delete(key); return json({ ok: true }); }
    return null;
  }

  // ===== Lembretes (Web Push) =====
  if (p === '/v1/push/key' && m === 'GET') return json({ key: (await vapidKeys(env)).pub });
  r = p.match(/^\/v1\/push\/([a-f0-9]{32})(\/test|\/done)?$/);
  if (r) {
    const key = 's:' + r[1];
    if (!r[2] && m === 'PUT') {
      const b = await body(req);
      const sub = cleanSub(b.sub);
      const rem = (Array.isArray(b.rem) ? b.rem : []).slice(0, 80).map(cleanRem).filter(Boolean);
      const old = await env.FICHA.get(key, 'json');
      const tz = validTz(b.tz) ? b.tz : 'America/Sao_Paulo';
      await env.FICHA.put(key, JSON.stringify({ sub, tz, rem, last: old && old.sub.endpoint === sub.endpoint ? old.last || {} : {}, done: old ? old.done || {} : {}, at: Date.now() }));
      return json({ ok: true, n: rem.length });
    }
    if (!r[2] && m === 'DELETE') { await env.FICHA.delete(key); return json({ ok: true }); }
    const rec = await env.FICHA.get(key, 'json');
    if (!rec) fail(404, 'no_subscription');
    if (r[2] === '/test' && m === 'POST') {
      const res = await sendPush(env, rec.sub, { title: 'Ficha', body: 'As notificações estão funcionando ✓', tag: 'teste', url: './#/hoje' });
      if (res.gone) await env.FICHA.delete(key);
      return json({ ok: res.ok, status: res.status }, res.ok ? 200 : 502);
    }
    if (r[2] === '/done' && m === 'POST') {
      const b = await body(req, 8192);
      if (!/^\d{4}-\d{2}-\d{2}$/.test(b.date || '')) fail(400, 'bad_date');
      const kinds = (Array.isArray(b.kinds) ? b.kinds : []).map(String).filter(k => /^[a-z0-9]{1,20}$/.test(k)).slice(0, 20);
      rec.done = { [b.date]: kinds };
      await env.FICHA.put(key, JSON.stringify(rec));
      return json({ ok: true });
    }
  }
  return null;
}

function validTz(tz) { try { new Intl.DateTimeFormat('en-US', { timeZone: tz }); return typeof tz === 'string'; } catch (e) { return false; } }
function cleanSub(s) {
  if (!s || typeof s.endpoint !== 'string' || !s.keys || !s.keys.p256dh || !s.keys.auth) fail(400, 'bad_subscription');
  const u = new URL(s.endpoint);
  if (u.protocol !== 'https:' || !PUSH_HOSTS.some(h => u.hostname === h || u.hostname.endsWith('.' + h))) fail(400, 'bad_endpoint');
  return { endpoint: s.endpoint, keys: { p256dh: String(s.keys.p256dh), auth: String(s.keys.auth) } };
}
function cleanRem(x) {
  if (!x || !/^\d{1,2}:\d{2}$/.test(x.hm || '')) return null;
  const days = (Array.isArray(x.days) ? x.days : []).map(Number).filter(d => d >= 0 && d <= 6);
  if (!days.length) return null;
  const s = (v, n) => String(v || '').slice(0, n);
  return { id: s(x.id, 40), kind: s(x.kind, 20), days, hm: x.hm, title: s(x.title, 120), body: s(x.body, 300), url: s(x.url, 200) || './' };
}

/* ===== Envio dos lembretes na hora ===== */
function localParts(t, tz) {
  const f = new Intl.DateTimeFormat('en-US', { timeZone: tz, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', weekday: 'short' });
  const o = Object.fromEntries(f.formatToParts(new Date(t)).map(x => [x.type, x.value]));
  return { date: `${o.year}-${o.month}-${o.day}`, dow: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].indexOf(o.weekday), min: (+o.hour % 24) * 60 + +o.minute };
}
// Lembretes que vencem agora: no dia da semana, entre o horário e 1 hora depois (cobre atrasos do cron), uma vez por dia
export function dueReminders(rec, t) {
  const L = localParts(t, rec.tz || 'America/Sao_Paulo'), done = (rec.done || {})[L.date] || [], out = [];
  for (const r of rec.rem || []) {
    if (!r.days.includes(L.dow)) continue;
    const [h, mm] = r.hm.split(':').map(Number), at = h * 60 + mm;
    if (L.min < at || L.min >= at + 60) continue;
    if ((rec.last || {})[r.id] === L.date) continue;
    out.push({ r, skip: done.includes(r.kind) });
  }
  return { date: L.date, list: out };
}
async function sendDue(env, t) {
  let cursor;
  do {
    const page = await env.FICHA.list({ prefix: 's:', cursor });
    for (const k of page.keys) {
      const rec = await env.FICHA.get(k.name, 'json');
      if (!rec) continue;
      const { date, list } = dueReminders(rec, t);
      if (!list.length) continue;
      rec.last = rec.last || {};
      let gone = false;
      for (const { r, skip } of list) {
        rec.last[r.id] = date;
        if (skip || gone) continue;
        const res = await sendPush(env, rec.sub, { title: r.title, body: r.body, url: r.url, tag: r.id });
        if (res.gone) gone = true;
      }
      if (gone) await env.FICHA.delete(k.name);
      else await env.FICHA.put(k.name, JSON.stringify(rec));
    }
    cursor = page.list_complete ? null : page.cursor;
  } while (cursor);
}

/* ===== Web Push: VAPID (RFC 8292) e criptografia aes128gcm (RFC 8291) ===== */
const enc = new TextEncoder();
const b64u = buf => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const unb64u = s => Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - s.length % 4) % 4)), c => c.charCodeAt(0));
const concat = (...a) => { const out = new Uint8Array(a.reduce((n, x) => n + x.length, 0)); let o = 0; for (const x of a) { out.set(x, o); o += x.length; } return out; };
async function sha256hex(s) { return [...new Uint8Array(await crypto.subtle.digest('SHA-256', enc.encode(s)))].map(b => b.toString(16).padStart(2, '0')).join(''); }

// Par de chaves do servidor: criado na primeira vez e guardado no KV
let vapidMem = null, jwtMem = {};
async function vapidKeys(env) {
  if (vapidMem) return vapidMem;
  let k = await env.FICHA.get('vapid', 'json');
  if (!k) {
    const pair = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify']);
    k = { jwk: await crypto.subtle.exportKey('jwk', pair.privateKey), pub: b64u(await crypto.subtle.exportKey('raw', pair.publicKey)) };
    await env.FICHA.put('vapid', JSON.stringify(k));
  }
  return (vapidMem = k);
}
// Token VAPID (JWT ES256) por serviço de push. A Apple pede para não renovar mais de uma vez por hora: vale 12 h e é renovado a cada 6 h.
async function vapidAuth(env, endpoint) {
  const k = await vapidKeys(env), aud = new URL(endpoint).origin, now = Math.floor(Date.now() / 1000);
  let c = jwtMem[aud] || await env.FICHA.get('jwt:' + aud, 'json');
  if (!c || c.pub !== k.pub || now - c.at > 6 * 3600 || now < c.at) {
    const key = await crypto.subtle.importKey('jwk', k.jwk, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign']);
    const head = b64u(enc.encode(JSON.stringify({ typ: 'JWT', alg: 'ES256' })));
    const claims = b64u(enc.encode(JSON.stringify({ aud, exp: now + 12 * 3600, sub: env.VAPID_SUBJECT || 'https://github.com/DrakRosmann/Ficha-Treino' })));
    const sig = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, key, enc.encode(`${head}.${claims}`));
    c = { t: `${head}.${claims}.${b64u(sig)}`, at: now, pub: k.pub };
    await env.FICHA.put('jwt:' + aud, JSON.stringify(c));
  }
  jwtMem[aud] = c;
  return `vapid t=${c.t}, k=${k.pub}`;
}
async function hkdf(salt, ikm, info, len) {
  const key = await crypto.subtle.importKey('raw', ikm, 'HKDF', false, ['deriveBits']);
  return new Uint8Array(await crypto.subtle.deriveBits({ name: 'HKDF', hash: 'SHA-256', salt, info }, key, len * 8));
}
export async function encryptPush(sub, payload) {
  const uaPub = unb64u(sub.keys.p256dh), authSecret = unb64u(sub.keys.auth);
  const eph = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' }, true, ['deriveBits']);
  const asPub = new Uint8Array(await crypto.subtle.exportKey('raw', eph.publicKey));
  const uaKey = await crypto.subtle.importKey('raw', uaPub, { name: 'ECDH', namedCurve: 'P-256' }, false, []);
  const shared = new Uint8Array(await crypto.subtle.deriveBits({ name: 'ECDH', public: uaKey }, eph.privateKey, 256));
  const ikm = await hkdf(authSecret, shared, concat(enc.encode('WebPush: info\0'), uaPub, asPub), 32);
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const cek = await hkdf(salt, ikm, enc.encode('Content-Encoding: aes128gcm\0'), 16);
  const nonce = await hkdf(salt, ikm, enc.encode('Content-Encoding: nonce\0'), 12);
  const aes = await crypto.subtle.importKey('raw', cek, 'AES-GCM', false, ['encrypt']);
  const ct = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv: nonce }, aes, concat(enc.encode(payload), new Uint8Array([2]))));
  const rs = new Uint8Array([0, 0, 16, 0]); // 4096
  return concat(salt, rs, new Uint8Array([asPub.length]), asPub, ct);
}
async function sendPush(env, sub, msg) {
  try {
    const res = await fetch(sub.endpoint, {
      method: 'POST',
      headers: { Authorization: await vapidAuth(env, sub.endpoint), TTL: '3600', Urgency: 'normal', 'Content-Encoding': 'aes128gcm', 'Content-Type': 'application/octet-stream' },
      body: await encryptPush(sub, JSON.stringify(msg))
    });
    return { ok: res.ok, status: res.status, gone: res.status === 404 || res.status === 410 };
  } catch (e) {
    return { ok: false, status: 0, gone: false };
  }
}
