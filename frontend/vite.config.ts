/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// L'API est appelée sur la même origine que le frontend (/api), et Vite la relaie au backend.
// Ne modifiez pas la cible ici : définissez VITE_API_TARGET (voir docs/conventions.md).
//   - API en local (./mvnw spring-boot:run) : port 8081, valeur par défaut
//   - API en Docker (docker compose up -d) : VITE_API_TARGET=http://localhost:8090,
//     car compose.yaml publie le port 8081 du conteneur sur le port 8090 de la machine
export default defineConfig(({ mode }) => {
  // Préfixe '' : loadEnv lit process.env en plus des fichiers .env*
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = process.env.VITE_API_TARGET || env.VITE_API_TARGET || 'http://localhost:8081'

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
