import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:5137',
        changeOrigin: true
      },
      '/xiaozhi': {
        target: 'http://localhost:5137',
        changeOrigin: true
      }
    }
  }
})