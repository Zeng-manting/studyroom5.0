import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 后端 context-path 为 /api，开发环境代理转发（FR-AD-001 本地调试）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
