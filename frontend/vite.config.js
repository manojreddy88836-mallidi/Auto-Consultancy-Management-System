import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Dev proxy: forward /api calls to local backend
      // This proxy is ONLY active during development (npm run dev)
      // In production builds (Render static site), VITE_API_BASE_URL env var is used instead
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        }
      }
    },
    build: {
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-react': ['react', 'react-dom', 'react-router-dom'],
            'vendor-charts': ['recharts'],
            'vendor-ui': ['@headlessui/react', 'lucide-react', 'react-hot-toast'],
            'vendor-forms': ['react-hook-form', '@hookform/resolvers', 'yup'],
            'vendor-utils': ['axios', 'date-fns'],
          }
        }
      }
    },
    // Make VITE_API_BASE_URL available at build time
    define: {
      __VITE_API_BASE_URL__: JSON.stringify(env.VITE_API_BASE_URL || '')
    }
  }
})
