// Service worker: deixa o app funcionando offline.
// Ao publicar uma nova versão, aumente o número em CACHE para forçar a atualização.
const CACHE = 'ficha-v2';
// Fotos dos exercícios: cache separado, que sobrevive às atualizações do app (mesmo nome em app.js).
const IMG_CACHE = 'ficha-img-v1';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './exercises.js',
  './manifest.webmanifest',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/apple-touch-icon.png'
];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
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
  e.respondWith(
    fetch(e.request)
      .then(res => {
        const copy = res.clone();
        caches.open(CACHE).then(c => c.put(e.request, copy));
        return res;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }).then(r => r || caches.match('./index.html')))
  );
});
