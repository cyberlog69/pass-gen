// PATCH #10: Cache-Only strategy — never dynamically cache network responses.
// All assets are pre-cached at install time. Dynamic network caching is a risk
// as a MITM could inject a poisoned response that gets permanently cached.
const CACHE_NAME = 'passgen-v3';
const ASSETS = [
  './',
  './index.html',
  './css/styles.css',
  './css/fonts.css',
  './fonts/Inter.woff2',
  './fonts/JetBrainsMono.woff2',
  './js/words.js',
  './js/app.js',
  './icon.svg',
  './manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) return caches.delete(key);
        })
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  // Cache-Only: serve from cache only. If not cached, respond with a
  // minimal offline fallback rather than fetching from the network.
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) return cachedResponse;
      // Asset not in cache — return a safe 503 offline response
      return new Response('Offline — asset not cached', {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'text/plain' }
      });
    })
  );
});
