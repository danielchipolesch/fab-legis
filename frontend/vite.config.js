import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    vuetify({ autoImport: true }),
  ],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  build: {
    // Target browsers modernos; reduz o polyfill overhead
    target: 'es2020',

    // Habilita source maps apenas em staging (via env var)
    sourcemap: process.env.VITE_APP_ENV === 'staging',

    // Avisos de chunk acima de 600 kB
    chunkSizeWarningLimit: 600,

    rollupOptions: {
      output: {
        // Separa vendors em chunks distintos para melhor cache do browser
        manualChunks(id) {
          // Framework principal — muda raramente
          if (id.includes('node_modules/vue') ||
              id.includes('node_modules/vue-router') ||
              id.includes('node_modules/pinia')) {
            return 'vendor-vue'
          }
          // Vuetify — grande, muda com cada versão
          if (id.includes('node_modules/vuetify') ||
              id.includes('node_modules/@mdi')) {
            return 'vendor-vuetify'
          }
          // TipTap — editor WYSIWYG
          if (id.includes('node_modules/@tiptap') ||
              id.includes('node_modules/prosemirror')) {
            return 'vendor-tiptap'
          }
          // pdfmake é importado dinamicamente — não entra no bundle inicial
          // diff + uuid → utilitários pequenos
          if (id.includes('node_modules/diff') ||
              id.includes('node_modules/uuid')) {
            return 'vendor-utils'
          }
          // Vuedraggable
          if (id.includes('node_modules/vuedraggable') ||
              id.includes('node_modules/sortablejs')) {
            return 'vendor-dnd'
          }
        },

        // Hash no nome dos assets para cache busting imutável
        assetFileNames: 'assets/[name]-[hash][extname]',
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
      },
    },
  },

  server: {
    port: 5173,
    host: '0.0.0.0',
    // Polling necessário no Windows + Docker: eventos de filesystem (inotify)
    // não chegam ao container a partir do volume montado no host Windows.
    watch: {
      usePolling: true,
      interval: 300,
    },
  },

  preview: {
    port: 8080,
    host: '0.0.0.0',
  },
})
