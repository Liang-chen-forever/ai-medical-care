// Service Worker for 硅谷小智 PWA
const CACHE_NAME = 'xiaozhi-v1'
const OFFLINE_URL = '/offline.html'

const PRECACHE_URLS = [
  '/',
  '/offline.html',
  '/src/assets/style.css'
]

// Install: precache static assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE_URLS))
  )
  self.skipWaiting()
})

// Activate: clean old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  )
  self.clients.claim()
})

// Fetch: network-first strategy for API, cache-first for static
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url)

  // API requests: network-first (don't cache API responses)
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/xiaozhi/')) {
    return
  }

  // Navigation requests: network-first, fallback to offline page
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request).catch(() =>
        caches.match(OFFLINE_URL)
      )
    )
    return
  }

  // Static assets: cache-first
  event.respondWith(
    caches.match(event.request).then((cached) =>
      cached || fetch(event.request).then((response) => {
        if (response.ok) {
          const clone = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone))
        }
        return response
      })
    )
  )
})