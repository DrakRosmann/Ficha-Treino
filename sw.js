// Service worker: deixa o app funcionando offline.
// Ao publicar uma nova versão, aumente o número em CACHE para forçar a atualização.
const CACHE = 'ficha-v20';
// Fotos dos exercícios: cache separado, que sobrevive às atualizações do app (mesmo nome em app.js).
const IMG_CACHE = 'ficha-img-v1';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './exercises.js',
  './templates.js',
  './assistant.js',
  './body.js',
  './tools.js',
  './foods.js',
  './nutrition.js',
  './photos.js',
  './share.js',
  './coach.js',
  './cloud.js',
  './glass.js',
  './manifest.webmanifest',
  './fonts/google-sans-flex.woff2',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/apple-touch-icon.png'
];

self.addEventListener('install', e => {
  // cache: 'reload' ignora o cache HTTP e baixa a versão mais nova de cada arquivo
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS.map(u => new Request(u, { cache: 'reload' })))).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE && k !== IMG_CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;

  // Fotos não mudam: cache primeiro, rede só na primeira vez.
  if (url.pathname.includes('/img/')) {
    e.respondWith(caches.open(IMG_CACHE).then(c => c.match(e.request).then(hit => hit || fetch(e.request).then(res => {
      if (res.ok) c.put(e.request, res.clone());
      return res;
    }))));
    return;
  }

  // Rede primeiro (pega atualizações quando há internet), cache como reserva offline.
  // Os arquivos do app são sempre conferidos com o servidor (no-cache), para atualizar na hora.
  const req = e.request.mode === 'navigate' ? e.request : new Request(e.request, { cache: 'no-cache' });
  e.respondWith(
    fetch(req)
      .then(res => {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(e.request, copy));
        return res;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }).then(r => r || caches.match('./index.html')))
  );
});

// Lembretes (Web Push): o servidor manda { title, body, url, tag }
self.addEventListener('push', e => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch (err) { d = { body: e.data && e.data.text() }; }
  e.waitUntil(self.registration.showNotification(d.title || 'Ficha', {
    body: d.body || '', tag: d.tag || undefined, icon: './icons/icon-192.png', badge: './icons/icon-192.png', data: { url: d.url || './' }
  }));
});
self.addEventListener('notificationclick', e => {
  e.notification.close();
  const url = new URL((e.notification.data && e.notification.data.url) || './', self.registration.scope).href;
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
    const c = list.find(w => w.url.startsWith(self.registration.scope));
    if (c) return c.focus().then(w => (w || c).navigate ? (w || c).navigate(url) : null).catch(() => self.clients.openWindow(url));
    return self.clients.openWindow(url);
  }));
});
