import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1000, // raise threshold to 1 MB (charts + react are heavy)
    rollupOptions: {
      output: {
        manualChunks: {
          // Split vendor libraries into separate chunks
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-charts': ['recharts'],
          'vendor-ui': ['@headlessui/react', 'lucide-react', 'react-hot-toast'],
          'vendor-forms': ['react-hook-form', '@hookform/resolvers', 'yup'],
          'vendor-utils': ['axios', 'date-fns'],
        }
      }
    }
  }
})
