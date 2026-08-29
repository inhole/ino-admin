import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  optimizeDeps: { exclude: ['dnd-kit-sortable-tree'] },
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080', '/actuator': 'http://localhost:8080' }
  },
  test: { environment: 'jsdom', setupFiles: './src/test/setup.ts', exclude: ['tests/e2e/**', 'node_modules/**'] }
})
