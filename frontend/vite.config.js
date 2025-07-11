import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 将 /api 的请求代理到 http://localhost:8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    }
  }
})