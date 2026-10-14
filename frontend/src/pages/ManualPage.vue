<template>
  <q-page class="manual">

    <!-- Sem login não há a barra do sistema (App.vue): o manual leva a sua, com a volta para o login. -->
    <header v-if="!auth.isAuthenticated" class="manual-topo bg-primary text-white">
      <div class="row items-center no-wrap q-px-lg" style="height:60px;gap:12px">
        <q-avatar size="36px" color="white" text-color="primary" rounded><q-icon name="mdi-scale-balance" size="22px" /></q-avatar>
        <div class="col">
          <div class="text-subtitle1 text-weight-bold" style="line-height:1">FAB Legis</div>
          <div class="text-caption" style="opacity:.75;line-height:1.1">Manual do Usuário</div>
        </div>
        <q-btn flat no-caps color="white" icon="mdi-login" label="Entrar" :to="{ name: 'login' }" data-testid="manual-entrar" />
      </div>
    </header>

    <div class="manual-corpo">
      <!-- Índice e busca -->
      <nav class="manual-indice" aria-label="Índice do manual">
        <q-input v-model="termo" dense outlined clearable placeholder="Buscar no manual" class="q-mb-md" data-testid="manual-busca">
          <template #prepend><q-icon name="mdi-magnify" /></template>
        </q-input>

        <template v-if="termo && termo.trim().length >= 2">
          <div v-if="!resultados.length" class="text-grey-7 text-body2" data-testid="manual-sem-resultado">Nada encontrado para "{{ termo }}".</div>
          <q-list v-else separator class="resultados" data-testid="manual-resultados">
            <q-item v-for="(r, i) in resultados" :key="i" clickable :to="destino(r.pagina, r.secaoId)" @click="termo = ''">
              <q-item-section>
                <q-item-label class="text-weight-medium">{{ r.secaoTitulo ?? r.paginaTitulo }}</q-item-label>
                <q-item-label caption>{{ r.paginaTitulo }}</q-item-label>
                <q-item-label caption lines="2">{{ r.trecho }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </template>

        <q-list v-else class="paginas">
          <q-item v-for="p in paginas" :key="p.slug" clickable :to="destino(p.slug)" :active="p.slug === pagina?.slug" active-class="pagina-ativa" :data-testid="`manual-indice-${p.slug}`">
            <q-item-section>{{ p.titulo }}</q-item-section>
            <q-item-section v-if="ehParaMim(p, auth.usuario?.perfis)" side>
              <q-icon name="mdi-account-check-outline" color="primary" size="18px"><q-tooltip>Tem a ver com o seu perfil</q-tooltip></q-icon>
            </q-item-section>
          </q-item>
        </q-list>
      </nav>

      <!-- A página -->
      <article class="manual-pagina" v-if="pagina">
        <q-breadcrumbs v-if="auth.isAuthenticated" active-color="primary" class="q-mb-md" style="font-size:13px">
          <template #separator><q-icon name="mdi-chevron-right" size="16px" color="primary" /></template>
          <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
          <q-breadcrumbs-el label="Área de Trabalho" :to="{ name: 'home' }" />
          <q-breadcrumbs-el label="Manual do Usuário" :to="{ name: 'manual' }" />
        </q-breadcrumbs>

        <div class="row items-start no-wrap">
          <h1 class="manual-titulo col">{{ pagina.titulo }}</h1>
        </div>

        <div v-if="perfisDaPagina.length" class="q-mb-md" data-testid="manual-perfis">
          <span class="text-caption text-grey-7 q-mr-sm">Para:</span>
          <q-chip v-for="nome in perfisDaPagina" :key="nome" dense color="blue-1" text-color="primary" class="q-ma-none q-mr-xs">{{ nome }}</q-chip>
          <q-chip v-if="paraMim" dense color="primary" text-color="white" icon="mdi-account-check-outline" class="q-ma-none" data-testid="manual-para-mim">É o seu perfil</q-chip>
        </div>

        <!-- O Markdown vem dos arquivos do próprio projeto (src/manual) e o HTML cru está desligado na conversão. -->
        <!-- eslint-disable-next-line vue/no-v-html -->
        <div ref="conteudo" class="manual-texto" v-html="html" @click="aoClicar" />

        <div class="manual-navegacao">
          <q-btn v-if="anterior" flat no-caps color="primary" icon="mdi-chevron-left" :label="anterior.titulo" :to="destino(anterior.slug)" />
          <q-space />
          <q-btn v-if="seguinte" flat no-caps color="primary" icon-right="mdi-chevron-right" :label="seguinte.titulo" :to="destino(seguinte.slug)" />
        </div>
      </article>

      <article v-else class="manual-pagina" data-testid="manual-nao-encontrada">
        <h1 class="manual-titulo">Página não encontrada</h1>
        <p>Essa página do manual não existe. Escolha uma no índice.</p>
        <q-btn color="primary" no-caps label="Ir ao início do manual" :to="destino(paginas[0].slug)" />
      </article>

      <!-- Nesta página -->
      <aside class="manual-toc" v-if="pagina && pagina.secoes.length > 1" aria-label="Nesta página">
        <div class="toc-titulo">Nesta página</div>
        <a v-for="s in pagina.secoes" :key="s.id" :href="`#${s.id}`" class="toc-link" @click.prevent="irParaSecao(s.id)">{{ s.titulo }}</a>
      </aside>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { montarPaginas, nomesDosPerfis, ehParaMim, buscarNoManual } from '@/utils/manual.js'
import { renderizarManual } from '@/utils/manualMarkdown.js'

// Os arquivos Markdown do manual entram no pacote desta tela (carregada sob demanda, ver router/index.js).
const paginas = montarPaginas(import.meta.glob('../manual/*.md', { query: '?raw', import: 'default', eager: true }))

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const termo = ref('')
const conteudo = ref(null)

const pagina = computed(() => {
  const slug = route.params.pagina
  return slug ? paginas.find(p => p.slug === slug) ?? null : paginas[0]
})
const indice = computed(() => paginas.findIndex(p => p.slug === pagina.value?.slug))
const anterior = computed(() => paginas[indice.value - 1] ?? null)
const seguinte = computed(() => paginas[indice.value + 1] ?? null)
const html = computed(() => (pagina.value ? renderizarManual(pagina.value.markdown) : ''))
const perfisDaPagina = computed(() => nomesDosPerfis(pagina.value?.perfis))
const paraMim = computed(() => !!pagina.value && ehParaMim(pagina.value, auth.usuario?.perfis))
const resultados = computed(() => buscarNoManual(paginas, termo.value ?? ''))

const destino = (slug, secaoId) => ({ path: `/manual/${slug}`, hash: secaoId ? `#${secaoId}` : '' })

function irParaSecao(id) {
  router.replace({ path: route.path, hash: `#${id}` })
}

// Rola até o título do # da URL (ou ao topo, numa página nova). Espera o HTML da página estar no DOM.
async function rolar() {
  await nextTick()
  const id = route.hash?.slice(1)
  const alvo = id ? conteudo.value?.querySelector(`#${CSS.escape(id)}`) : null
  if (alvo) alvo.scrollIntoView({ behavior: 'smooth', block: 'start' })
  else window.scrollTo({ top: 0 })
}

// Links entre páginas do manual navegam sem recarregar o sistema.
function aoClicar(evento) {
  const a = evento.target.closest?.('a')
  if (!a) return
  const href = a.getAttribute('href') ?? ''
  if (a.hasAttribute('data-interno')) {
    evento.preventDefault()
    router.push(href)
  } else if (href.startsWith('#')) {
    evento.preventDefault()
    irParaSecao(href.slice(1))
  }
}


watch(() => [route.params.pagina, route.hash], rolar)
onMounted(rolar)
</script>

<style scoped>
.manual { background: #F4F6FA; }
.manual-corpo {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr) 210px;
  gap: 32px;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 24px 48px;
  align-items: start;
}
.manual-indice, .manual-toc { position: sticky; top: 76px; }
.manual-indice { max-height: calc(100vh - 100px); overflow-y: auto; }
.paginas, .resultados { background: #fff; border-radius: 12px; box-shadow: 0 1px 2px rgba(20, 30, 60, .06), 0 4px 16px rgba(20, 30, 60, .05); padding: 6px; }
.paginas :deep(.q-item), .resultados :deep(.q-item) { border-radius: 8px; min-height: 40px; }
.pagina-ativa { background: #E8EDF6; color: #0B3D91; font-weight: 600; }

.manual-pagina {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(20, 30, 60, .06), 0 4px 16px rgba(20, 30, 60, .05);
  padding: 32px 40px 28px;
  min-width: 0;
}
.manual-titulo { font-size: 28px; line-height: 1.2; font-weight: 700; color: #1A2E5A; margin: 0 0 16px; }

.manual-texto { font-size: 15.5px; line-height: 1.7; color: #2B3445; max-width: 78ch; }
.manual-texto :deep(h2) { font-size: 20px; font-weight: 700; color: #1A2E5A; margin: 32px 0 10px; padding-top: 4px; scroll-margin-top: 80px; }
.manual-texto :deep(h3) { font-size: 16.5px; font-weight: 600; color: #1A2E5A; margin: 22px 0 6px; scroll-margin-top: 80px; }
.manual-texto :deep(p) { margin: 0 0 12px; }
.manual-texto :deep(ul), .manual-texto :deep(ol) { margin: 0 0 12px; padding-left: 24px; }
.manual-texto :deep(li) { margin: 4px 0; }
.manual-texto :deep(a) { color: #0B3D91; text-decoration: underline; text-underline-offset: 2px; }
.manual-texto :deep(code) { background: #EEF1F6; border-radius: 4px; padding: 1px 5px; font-size: .92em; }
.manual-texto :deep(blockquote) { margin: 14px 0; padding: 10px 16px; background: #EEF3FB; border-left: 4px solid #0B3D91; border-radius: 0 8px 8px 0; }
.manual-texto :deep(blockquote p:last-child) { margin-bottom: 0; }
.manual-texto :deep(.tabela-rolavel) { overflow-x: auto; margin: 0 0 16px; }
.manual-texto :deep(table) { border-collapse: collapse; width: 100%; font-size: 14.5px; }
.manual-texto :deep(th) { text-align: left; background: #E8EDF6; color: #1A2E5A; }
.manual-texto :deep(th), .manual-texto :deep(td) { border: 1px solid #D5DBE6; padding: 8px 12px; vertical-align: top; }

.manual-navegacao { display: flex; margin-top: 36px; padding-top: 16px; border-top: 1px solid #E6E9EF; }

.toc-titulo { font-size: 12px; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; color: #6B7A90; margin-bottom: 8px; }
.toc-link { display: block; padding: 4px 0 4px 12px; border-left: 2px solid #E6E9EF; font-size: 13.5px; color: #4A566B; text-decoration: none; }
.toc-link:hover { color: #0B3D91; border-left-color: #0B3D91; }

@media (max-width: 1180px) {
  .manual-corpo { grid-template-columns: 250px minmax(0, 1fr); }
  .manual-toc { display: none; }
}
@media (max-width: 820px) {
  .manual-corpo { grid-template-columns: minmax(0, 1fr); padding: 16px 16px 40px; gap: 16px; }
  .manual-indice { position: static; max-height: none; }
  .manual-pagina { padding: 22px 18px; }
  .manual-titulo { font-size: 23px; }
}
</style>
