import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      '/api': { target: 'http://localhost:5137', changeOrigin: true },
      '/xiaozhi': { target: 'http://localhost:5137', changeOrigin: true }
    }
  }
})