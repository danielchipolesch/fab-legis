import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  // Lê o .env da raiz do monorepo (mesmo arquivo usado pelo docker-compose.yml
  // para substituição de ${VAR:-padrão}), não o padrão do Vite (a própria pasta
  // frontend/) -- consolida num único .env/.env.example em vez de manter dois.
  // Só afeta a execução local (`npm run dev`/`build` fora do Docker): o
  // container de dev já recebe VITE_* diretamente do docker-compose.yml via
  // `environment:`, então nada muda para quem roda via `docker compose up`.
  envDir: fileURLToPath(new URL('..', import.meta.url)),

  plugins: [
    vue({ template: { transformAssetUrls } }),
    quasar({
      sassVariables: fileURLToPath(new URL('./src/css/quasar-variables.sass', import.meta.url)),
    }),
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
          // Quasar — framework de UI
          if (id.includes('node_modules/quasar') ||
              id.includes('node_modules/@quasar')) {
            return 'vendor-quasar'
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
    // Proxy dos endpoints do Authorization Server (não sob /v1) -- faz o
    // navegador enxergar /oauth2/**, /login e /logout como MESMA ORIGEM do
    // frontend (127.0.0.1:5173), nunca como uma chamada cross-origin pro
    // backend (127.0.0.1:8081). Sem isso, o cookie de sessão do Spring
    // Security (SameSite=Lax, padrão) só é enviado de volta em navegações de
    // topo GET -- um POST cross-origin de /login (mesmo sendo uma navegação
    // de página inteira, não fetch) não carrega o cookie em boa parte dos
    // casos reais (Chrome só relaxa essa regra por uma janela curta logo
    // após o cookie ser criado; digitar CPF/senha com calma já é tempo
    // suficiente pra essa janela fechar). O login "funcionava" (POST 200/302)
    // mas autenticava numa sessão nova, sem a authorization request salva,
    // caindo de volta no /login sem erro nenhum -- exatamente o bug relatado.
    // O proxy elimina o problema na raiz: não existe cross-origin nenhum do
    // ponto de vista do navegador, então SameSite nunca entra em jogo.
    // changeOrigin DESLIGADO de propósito -- ele reescreve o header Host da
    // requisição encaminhada pro Host do TARGET (ex.: "backend:8081", o nome
    // do serviço Docker), e o Spring usa esse Host pra reconstruir URLs
    // absolutas em certos redirects (fora dos que já usam o "issuer" fixo).
    // O resultado: o navegador recebia um Location apontando pra
    // "http://backend:8081/...", um hostname que só existe dentro da rede
    // Docker -- inacessível e não resolvível pelo navegador de verdade.
    // Sem changeOrigin, o Host encaminhado é o que o navegador REALMENTE
    // usou (127.0.0.1:5173) -- e como todo /oauth2, /login, /logout também é
    // proxiado por aqui, qualquer URL que o Spring reconstrua nesse Host
    // continua resolvível.
    proxy: {
      '/oauth2':      { target: process.env.PROXY_BACKEND_TARGET ?? 'http://127.0.0.1:8081' },
      '/.well-known': { target: process.env.PROXY_BACKEND_TARGET ?? 'http://127.0.0.1:8081' },
      // /login é AMBOS: rota do Vue Router (GET, exibe a LoginPage) e endpoint
      // de processamento do Spring Security (POST, só esse precisa ir pro
      // backend). bypass devolve o próprio req.url pro Vite quando não é
      // POST -- sinaliza "não proxiar isso", deixa cair no fallback de SPA
      // normal (serve o index.html/router cuida do resto). Sem essa
      // distinção por método, todo GET /login (inclusive o clique em "Sair"
      // te trazendo de volta pra cá) ia parar no backend, que só entende
      // POST nessa rota -- "No endpoint GET /login" (400), SPA inteira quebrada.
      '/login': {
        target: process.env.PROXY_BACKEND_TARGET ?? 'http://127.0.0.1:8081',
        bypass(req) {
          if (req.method !== 'POST') return req.url
        },
      },
      '/logout': { target: process.env.PROXY_BACKEND_TARGET ?? 'http://127.0.0.1:8081' },
    },
  },

  preview: {
    port: 8080,
    host: '0.0.0.0',
  },
})
