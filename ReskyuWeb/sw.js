/* ============================================================
   RESKYU — Service Worker  (sw.js must live at root)
   Strategy:
   • Static assets  → Cache-First (images, CSS, JS)
   • HTML pages     → Network-First with offline fallback
   • External CDN   → Network-Only (Firebase, Google Fonts)
   ============================================================ */

const CACHE_NAME    = 'reskyu-v2';
const OFFLINE_URL   = '/offline.html';

/* Assets to pre-cache on install */
const PRECACHE_URLS = [
  '/',
  '/pitch.html',
  '/offline.html',
  '/css/pitch.css',
  '/css/dashboard.css',
  '/css/role-dashboard.css',
  '/js/pitch.js',
  '/js/toast.js',
  '/js/onboarding.js',
  '/img/hero_bg.webp',
  '/img/food_bag.webp'
];

/* ── Install: pre-cache shell ───────────────────────────── */
self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(function (cache) {
        return cache.addAll(PRECACHE_URLS);
      })
      .then(function () {
        return self.skipWaiting(); // activate immediately
      })
  );
});

/* ── Activate: delete old caches ───────────────────────── */
self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys
          .filter(function (key) { return key !== CACHE_NAME; })
          .map(function (key)   { return caches.delete(key);  })
      );
    }).then(function () {
      return self.clients.claim(); // take control immediately
    })
  );
});

/* ── Fetch: routing strategy ────────────────────────────── */
self.addEventListener('fetch', function (event) {
  var url = new URL(event.request.url);

  /* Skip non-GET and non-http(s) requests */
  if (event.request.method !== 'GET') return;
  if (!url.protocol.startsWith('http'))  return;

  /* Skip external CDN — Firebase SDK, Google Fonts, APIs */
  if (
    url.hostname.includes('googleapis.com')  ||
    url.hostname.includes('gstatic.com')     ||
    url.hostname.includes('firebaseio.com')  ||
    url.hostname.includes('firestore.googleapis.com')
  ) return;

  /* HTML navigation → Network-First with offline fallback */
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .catch(function () {
          return caches.match(OFFLINE_URL);
        })
    );
    return;
  }

  /* Static assets (images, CSS, JS) → Cache-First */
  if (
    url.pathname.startsWith('/css/')  ||
    url.pathname.startsWith('/js/')   ||
    url.pathname.startsWith('/img/')  ||
    url.pathname.match(/\.(webp|png|jpg|svg|ico|woff2?)$/)
  ) {
    event.respondWith(
      caches.match(event.request).then(function (cached) {
        if (cached) return cached;

        /* Not in cache — fetch and store */
        return fetch(event.request).then(function (response) {
          if (!response || response.status !== 200 || response.type === 'error') {
            return response;
          }
          var clone = response.clone();
          caches.open(CACHE_NAME).then(function (cache) {
            cache.put(event.request, clone);
          });
          return response;
        });
      })
    );
    return;
  }

  /* Everything else → Network only */
});
