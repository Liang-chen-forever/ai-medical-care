import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ErrorBoundary from './components/ErrorBoundary.vue'
import { registerPWA } from './utils/pwa.js'
import './assets/style.css'

const app = createApp(App)
app.use(router)
app.component('ErrorBoundary', ErrorBoundary)
app.mount('#app')

// 注册 PWA
registerPWA()
