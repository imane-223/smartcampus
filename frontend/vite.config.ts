/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// L'API est appelée sur la même origine que le frontend (/api), et Vite la relaie au backend.
// Cible par défaut : la pile Docker (port 8090). Avec ./mvnw spring-boot:run, le backend écoute
// sur 8081 : lancez alors VITE_API_TARGET=http://localhost:8081 npm run dev
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8090'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Échoue plutôt que de changer de port en silence
      strictPort: true,
      proxy: {
        '/api': {
          target: apiTarget,
          // false (défaut) : l'en-tête Host reste localhost:5173, comme l'Origin du navigateur.
          // Spring voit alors une requête same-origin, sans CORS.
          changeOrigin: false,
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
    },
  }
})
