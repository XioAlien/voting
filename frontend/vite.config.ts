import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = env.VITE_API_BASE_URL?.trim()
  const proxyTarget = env.VITE_PROXY_TARGET?.trim() || 'http://localhost:8080'
  const port = Number(env.VITE_PORT || 5173)

  return {
    plugins: [react()],
    server: {
      host: '0.0.0.0',
      port,
      proxy: apiBaseUrl
        ? undefined
        : {
          '/api': {
            target: proxyTarget,
            changeOrigin: true,
          },
        },
    },
  }
})
