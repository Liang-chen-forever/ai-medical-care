// PWA 注册和离线监测
export function registerPWA() {
  if (import.meta.env.PROD && 'serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').then(
        () => console.log('[PWA] Service Worker registered'),
        (err) => console.log('[PWA] Service Worker failed:', err)
      )
    })
  }

  // 离线/在线状态监测
  function updateOnlineStatus() {
    const root = document.getElementById('app-root')
    if (!root) return
    if (navigator.onLine) {
      root.classList.remove('offline')
    } else {
      root.classList.add('offline')
    }
  }

  window.addEventListener('online', updateOnlineStatus)
  window.addEventListener('offline', updateOnlineStatus)
  updateOnlineStatus()
}