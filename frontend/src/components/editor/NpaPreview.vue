<template>
  <div ref="outerRef" class="preview-outer">
    <div class="preview-hint">Prévia aproximada · {{ documento?.codigo_documento }} {{ documento?.titulo ? '— ' + documento.titulo : '' }}</div>

    <div class="pages-wrap" :style="{ zoom: pageScale }">
      <!-- A NPA é uma folha só, dentro de uma moldura que continua em todas as páginas do PDF. A prévia mostra o
           documento inteiro numa moldura contínua (a paginação real é a do PDF). -->
      <div class="pdf-page" data-testid="pagina-npa">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>
        <div v-if="seloRevogado" class="selo-revogado" data-testid="selo-revogado">REVOGADO</div>

        <table class="cabecalho">
          <tbody>
            <tr>
              <td rowspan="3" class="col-dom">
                <div class="dom" />
                <strong>{{ c.identificacao }}</strong>
              </td>
              <td colspan="3">
                <div v-for="(linha, i) in c.linhasDeCima" :key="i" class="rotulo">{{ linha }}</div>
              </td>
            </tr>
            <tr>
              <td class="rotulo">DATAS</td>
              <td><div class="rotulo">EMISSÃO</div>{{ c.emissao }}</td>
              <td><div class="rotulo">EFETIVAÇÃO</div>{{ c.efetivacao }}</td>
            </tr>
            <tr>
              <td class="rotulo">DISTRIBUIÇÃO</td>
              <td colspan="2">{{ c.distribuicao }}</td>
            </tr>
            <tr>
              <td class="rotulo">ASSUNTO</td>
              <td colspan="3">{{ c.assunto }}</td>
            </tr>
            <tr>
              <td class="rotulo">ANEXOS</td>
              <td colspan="3">{{ c.anexos }}</td>
            </tr>
          </tbody>
        </table>

        <template v-for="item in itens" :key="item.el.id">
          <div
            :id="'prev-' + item.el.id"
            class="npa-el"
            :class="[`npa-${item.el.tipo}`, { 'npa-el--selecionado': item.el.id === selectedElementId }]"
          >
            <template v-if="item.el.tipo === 'capitulo'">
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;{{ (item.el.titulo || '').toUpperCase() }}
            </template>
            <template v-else-if="item.el.tipo === 'secao_normativa' || item.el.tipo === 'subsecao_normativa'">
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;<u>{{ item.el.titulo }}</u>
            </template>
            <template v-else>
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;<span class="texto" v-html="item.html" />
            </template>
          </div>
        </template>

        <div class="fecho">{{ c.localEData }}</div>
        <div v-for="(a, i) in c.assinaturas" :key="i" class="assinatura">
          <div>{{ a.rotulo }}</div>
          <div v-for="(linha, j) in a.linhas" :key="j" :class="{ nome: j === 0 }">{{ linha }}</div>
        </div>
        <div v-if="c.publicadaNo" class="publicada">{{ c.publicadaNo }}</div>
        <div v-if="c.revogadaNo" class="publicada">{{ c.revogadaNo }}</div>
      </div>

      <!-- Anexos de imagem: no PDF vêm ao final, cada um em sua página, rotulados A, B, C… -->
      <div v-for="anexo in anexos" :key="anexo.id" class="pdf-page pdf-page--anexo">
        <p class="anexo-titulo">ANEXO {{ letraDoAnexo(anexo.ordem) }}</p>
        <p class="anexo-titulo">{{ (anexo.titulo || '').toUpperCase() }}</p>
        <div v-if="anexo.urlImagem" class="anexo-imagem"><img :src="anexo.urlImagem" alt="" /></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted, onUpdated } from 'vue'
import { generateHTML } from '@tiptap/html'
import { editorExtensions } from '@/editor/extensions.js'
import { cabecalho, letraDoAnexo } from '@/perfis/npa.js'
import { exibeSeloRevogado } from '@/utils/fluxoDocumento.js'
import { resolveMinioUrls } from '@/utils/minioUrls.js'
import { useDocumentosStore } from '@/stores/documentos.js'

// Prévia da NPA: o mesmo conteúdo do PDF (cabeçalho, corpo numerado pelo caminho, fecho e assinaturas) e os mesmos
// textos -- todos calculados por perfis/npa.js, espelho de CabecalhoDaNpa (backend). Só a diagramação é da tela.

const props = defineProps({
  documento:         { type: Object, default: null },
  campos:            { type: Object, default: null },   // { setorEmissor, local, assinaturas, boletimDaRevogacao }
  selectedElementId: { type: String, default: null },
})

const documentsStore = useDocumentosStore()

const WM_TEXT = {
  RASCUNHO: 'RASCUNHO', MINUTA: 'MINUTA', EM_REVISAO: 'EM REVISÃO', EM_PUBLICACAO: 'APROVADO',
}
const WM_COLOR = {
  RASCUNHO: '#b50000', MINUTA: '#b50000', EM_REVISAO: '#3a5bb5', EM_PUBLICACAO: '#1a6b1a',
}
const wmText = computed(() => WM_TEXT[props.documento?.situacao_local] ?? '')
const wmColor = computed(() => WM_COLOR[props.documento?.situacao_local] ?? '#888')
const seloRevogado = computed(() => exibeSeloRevogado(props.documento))

const anexos = computed(() => documentsStore.anexosPorDocumento[String(props.documento?.id)] ?? [])

const c = computed(() => cabecalho(
  props.documento ?? {},
  props.campos ?? { setorEmissor: '[SETOR EMISSOR]', local: '[Local]', assinaturas: [] },
  anexos.value,
))

function conteudoEmHtml(conteudo) {
  if (!conteudo) return ''
  try { return generateHTML(JSON.parse(conteudo), editorExtensions) } catch { return '' }
}

// A árvore da parte normativa em ordem de leitura, já com o HTML do texto de cada parágrafo/alínea.
const itens = computed(() => {
  const secao = (props.documento?.secoes ?? []).find(s => s.tipo === 'parte_normativa')
  const saida = []
  const percorrer = (els) => {
    for (const el of els ?? []) {
      saida.push({ el, html: el.tipo === 'paragrafo' || el.tipo === 'alinea' ? conteudoEmHtml(el.conteudo) : '' })
      percorrer(el.filhos)
    }
  }
  percorrer(secao?.elementos)
  return saida
})

// Rola até o elemento selecionado no editor.
watch(() => props.selectedElementId, async (id) => {
  if (!id) return
  await nextTick()
  const el = document.getElementById('prev-' + id)
  if (!el) return
  let container = el.parentElement
  while (container && container !== document.body) {
    const overflow = window.getComputedStyle(container).overflowY
    if (overflow === 'auto' || overflow === 'scroll') break
    container = container.parentElement
  }
  if (!container || container === document.body) return
  const containerRect = container.getBoundingClientRect()
  const elRect = el.getBoundingClientRect()
  container.scrollTo({
    top: container.scrollTop + elRect.top - containerRect.top - containerRect.height / 2 + elRect.height / 2,
    behavior: 'smooth',
  })
})

// ─── Escala: ResizeObserver + CSS zoom (mesma técnica de DocumentoPreview) ───
const A4_W = 794
const outerRef = ref(null)
const pageScale = ref(0.75)

let _ro = null
onMounted(() => {
  _ro = new ResizeObserver(([entry]) => {
    const avail = entry.contentRect.width - 32
    pageScale.value = +(Math.min(1, avail / A4_W).toFixed(4))
  })
  if (outerRef.value) _ro.observe(outerRef.value)
  resolverImagens()
})
onUnmounted(() => _ro?.disconnect())
onUpdated(resolverImagens)

// O bucket do MinIO é privado: as imagens (figuras do texto e anexos) precisam da URL assinada.
async function resolverImagens() {
  const raiz = outerRef.value
  if (!raiz) return
  const imgs = Array.from(raiz.querySelectorAll('img[src^="http"]:not([data-resolved])'))
  if (!imgs.length) return
  const mapa = await resolveMinioUrls(imgs.map(img => img.getAttribute('src')))
  for (const img of imgs) {
    const resolvido = mapa.get(img.getAttribute('src'))
    if (resolvido) img.setAttribute('src', resolvido)
    img.setAttribute('data-resolved', '1')
  }
}
</script>

<style scoped>
.preview-outer {
  padding: 16px;
  background: #525659;
  min-height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.preview-hint {
  text-align: center;
  font-size: 10px;
  color: #aaa;
  font-family: Arial, sans-serif;
  margin-bottom: 14px;
  letter-spacing: 0.3px;
}
.pages-wrap { transform-origin: top left; width: 794px; }

/* Folha A4 (794 × 1123 px @ 96 dpi), margem de 2 cm; a moldura da NPA é a borda interna. */
.pdf-page {
  width: 794px;
  min-height: 1123px;
  background: #fff;
  box-sizing: border-box;
  padding: 76px;
  margin-bottom: 20px;
  box-shadow: 0 3px 18px rgba(0, 0, 0, 0.55);
  position: relative;
  overflow: hidden;
  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.25;
  color: #000;
  text-align: justify;
}
.pdf-page:not(.pdf-page--anexo) { border: 0; }
.pdf-page:not(.pdf-page--anexo)::before {
  content: '';
  position: absolute;
  inset: 76px;
  border: 1px solid #000;
  pointer-events: none;
}
/* O conteúdo fica dentro da moldura. */
.pdf-page:not(.pdf-page--anexo) > * { position: relative; }
.pdf-page:not(.pdf-page--anexo) { padding: 92px; }

.wm-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-45deg);
  font-size: 110px;
  font-weight: 900;
  opacity: 0.07;
  pointer-events: none;
  white-space: nowrap;
  letter-spacing: 12px;
  z-index: 10;
}
.selo-revogado {
  position: absolute;
  top: 88px;
  right: 88px;
  border: 2px solid #c00000;
  color: #c00000;
  font-weight: 700;
  font-size: 14px;
  padding: 2px 8px;
  z-index: 11;
}

table.cabecalho { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 14px; text-align: center; }
table.cabecalho td { border: 1px solid #000; padding: 3px 5px; vertical-align: middle; }
table.cabecalho .col-dom { width: 30%; }
table.cabecalho .dom { height: 84px; }
.rotulo { font-weight: 700; }

.npa-el { padding: 2px 4px; margin: 2px -4px; border-radius: 2px; text-align: left; }
.npa-el--selecionado { background: rgba(255, 213, 79, 0.35); }
.npa-capitulo { font-weight: 700; margin-top: 14px; }
.npa-secao_normativa, .npa-subsecao_normativa { margin-top: 8px; }
.npa-alinea { margin-left: 38px; }
.num { font-weight: 700; }
.npa-alinea .num { font-weight: 400; }
.texto :deep(p) { display: inline; margin: 0; }

.fecho { text-align: center; margin-top: 26px; }
.assinatura { text-align: center; margin-top: 30px; }
.assinatura .nome { font-weight: 700; margin-top: 12px; }
.publicada { text-align: center; font-size: 12px; margin-top: 24px; }

.pdf-page--anexo { padding: 76px; }
.anexo-titulo { text-align: center; font-weight: 700; margin: 0 0 6px; }
.anexo-imagem { text-align: center; }
.anexo-imagem img { max-width: 100%; }
</style>
