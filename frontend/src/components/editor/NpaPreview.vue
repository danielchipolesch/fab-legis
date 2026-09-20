<template>
  <div ref="outerRef" class="preview-outer">
    <div class="preview-hint">Prévia aproximada · {{ documento?.codigo_documento }} {{ documento?.titulo ? '— ' + documento.titulo : '' }}</div>

    <div class="pages-wrap" :style="{ zoom: pageScale }">
      <!-- A NPA é uma folha só, dentro de uma moldura que continua em todas as páginas do PDF. A prévia mostra o
           documento inteiro numa moldura contínua (a paginação real é a do PDF). -->
      <div class="pdf-page" data-testid="pagina-npa">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>
        <div v-if="seloRevogado" class="selo-revogado" data-testid="selo-revogado">REVOGADO</div>

        <!-- A moldura vai do cabeçalho até o fim do campo de assinatura; as bordas de cima e dos lados do cabeçalho
             (modelo do Anexo XII, mesma grade do PDF) são as dela. -->
        <div class="moldura">
        <table class="cabecalho">
          <colgroup><col style="width:24.5%"><col style="width:25%"><col style="width:26.5%"><col style="width:24%"></colgroup>
          <tbody>
            <tr style="height:72px">
              <td colspan="4" class="topo">
                <div v-for="(linha, i) in c.linhasDeCima" :key="i" class="rotulo">{{ linha }}</div>
              </td>
            </tr>
            <tr style="height:28px">
              <td rowspan="3" class="e" />
              <td colspan="2" class="e">DATAS</td>
              <td rowspan="2">DISTRIBUIÇÃO</td>
            </tr>
            <tr style="height:29px">
              <td class="e">EMISSÃO</td>
              <td class="e">EFETIVAÇÃO</td>
            </tr>
            <tr style="height:44px">
              <td rowspan="2" class="e">{{ c.emissao }}</td>
              <td rowspan="2" class="e"><div v-for="(linha, i) in c.efetivacao" :key="i">{{ linha }}</div></td>
              <td rowspan="2">{{ c.distribuicao }}</td>
            </tr>
            <tr style="height:37px">
              <td class="e">{{ c.identificacao }}</td>
            </tr>
            <tr style="height:47px">
              <td class="e">ASSUNTO</td>
              <td colspan="3" class="j">{{ c.assunto }}</td>
            </tr>
            <tr style="height:40px">
              <td class="e">ANEXOS</td>
              <td colspan="3" class="j"><div v-for="(linha, i) in c.anexos" :key="i">{{ linha }}</div></td>
            </tr>
          </tbody>
        </table>

        <div class="corpo">

        <template v-for="item in itens" :key="item.el.id">
          <div
            :id="'prev-' + item.el.id"
            class="npa-el"
            :class="`npa-${item.el.tipo}`"
          >
            <template v-if="item.el.tipo === 'capitulo'">
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;{{ (item.el.titulo || '').toUpperCase() }}
            </template>
            <template v-else-if="item.el.tipo === 'secao_normativa' || item.el.tipo === 'subsecao_normativa'">
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;<u>{{ (item.el.titulo || '').toUpperCase() }}</u>
            </template>
            <template v-else>
              <span class="num">{{ item.el._caminho }}</span>&nbsp;&nbsp;<span class="texto" v-html="item.html" />
            </template>
          </div>
        </template>

        <div class="fecho">{{ c.localEData }}</div>
        <div v-for="(a, i) in c.assinaturas" :key="i" class="assinatura">
          <div>{{ a.rotulo }}</div>
          <div class="linhas"><div v-for="(linha, j) in a.linhas" :key="j">{{ linha }}</div></div>
        </div>
        <div v-if="c.publicadaNo" class="publicada">{{ c.publicadaNo }}</div>
        <div v-if="c.revogadaNo" class="publicada">{{ c.revogadaNo }}</div>
        </div>
        </div>
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
/* A moldura é a borda do bloco do cabeçalho + texto: acaba onde acaba o campo de assinatura, não no rodapé. */
.moldura { border: 1px solid #000; }
/* O conteúdo começa na própria moldura (2,5 cm das bordas): as bordas do cabeçalho são as dela. */
.pdf-page:not(.pdf-page--anexo) { padding: 94px; }

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

table.cabecalho { width: 100%; border-collapse: collapse; table-layout: fixed; font-size: 14px; text-align: center; }
table.cabecalho td { border-bottom: 1px solid #000; padding: 3px 6px; vertical-align: middle; }
table.cabecalho td.e { border-right: 1px solid #000; }
table.cabecalho td.j { text-align: justify; }
table.cabecalho td.topo { vertical-align: bottom; }
.rotulo { font-weight: 700; }
.corpo { padding: 10px 10px 12px; }

.npa-el { padding: 2px 0; text-align: left; }
.npa-capitulo { font-weight: 700; margin-top: 14px; }
.npa-secao_normativa, .npa-subsecao_normativa { margin-top: 8px; }
/* Como no modelo: parágrafo com a primeira linha recuada (1,25 cm); alínea a 2,5 cm com a letra pendurada. */
.npa-paragrafo { text-indent: 47px; }
.npa-alinea { margin-left: 117px; text-indent: -23px; }
.num { font-weight: 700; }
.npa-alinea .num { font-weight: 400; }
.texto :deep(p) { display: inline; margin: 0; }

.fecho { text-align: right; margin-top: 26px; }
.assinatura { text-align: left; margin-top: 26px; }
.assinatura .linhas { text-align: center; margin-top: 30px; }
.publicada { text-align: center; font-size: 12px; margin-top: 24px; }

.pdf-page--anexo { padding: 76px; }
.anexo-titulo { text-align: center; font-weight: 700; margin: 0 0 6px; }
.anexo-imagem { text-align: center; }
.anexo-imagem img { max-width: 100%; }
</style>
