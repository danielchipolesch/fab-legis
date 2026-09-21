<template>
  <div ref="outerRef" class="preview-outer">
    <div class="preview-hint">Prévia aproximada · {{ documento?.codigo_documento }} {{ documento?.titulo ? '— ' + documento.titulo : '' }}</div>

    <div class="pages-wrap" :style="{ zoom: pageScale }">
      <!-- Uma folha A4 por página do PDF: a moldura vai até o fim de todas elas; o cabeçalho só está na primeira; da
           segunda em diante o "n/total" fica acima da moldura. A quebra é calculada (ver paginarAgora) medindo os blocos. -->
      <div v-for="(pagina, p) in folhas" :key="p" class="pdf-page npa-texto" data-testid="pagina-npa">
        <div v-if="p > 0" class="npa-numero" data-testid="numero-da-pagina">{{ p + 1 }}/{{ folhas.length }}</div>
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>
        <div v-if="seloRevogado" class="selo-revogado" data-testid="selo-revogado">REVOGADO</div>

        <div class="moldura">
          <NpaCabecalho v-if="p === 0" :c="c" />
          <div class="corpo">
            <NpaBloco v-for="i in pagina" :key="blocos[i].chave" :bloco="blocos[i]" com-id />
          </div>
        </div>
      </div>

      <!-- Anexos de imagem: no PDF vêm ao final, cada um em sua página, rotulados A, B, C… -->
      <div v-for="anexo in anexos" :key="anexo.id" class="pdf-page npa-texto pdf-page--anexo">
        <p class="anexo-titulo">ANEXO {{ letraDoAnexo(anexo.ordem) }}</p>
        <p class="anexo-titulo">{{ (anexo.titulo || '').toUpperCase() }}</p>
        <div v-if="anexo.urlImagem" class="anexo-imagem"><img :src="anexo.urlImagem" alt="" /></div>
      </div>
    </div>

    <!-- Cópia invisível de todo o conteúdo, só para medir a altura de cada bloco (fora do zoom, com a largura exata da
         moldura); as folhas acima são montadas a partir dessas medidas. -->
    <div ref="medidorRef" class="medidor npa-texto" aria-hidden="true" @load.capture="agendarPaginacao">
      <NpaCabecalho :c="c" />
      <div class="corpo">
        <NpaBloco v-for="b in blocos" :key="b.chave" :bloco="b" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted, onUpdated } from 'vue'
import { generateHTML } from '@tiptap/html'
import { editorExtensions } from '@/editor/extensions.js'
import { cabecalho, letraDoAnexo } from '@/perfis/npa.js'
import { paginar } from '@/utils/paginacaoDaNpa.js'
import { exibeSeloRevogado } from '@/utils/fluxoDocumento.js'
import { resolveMinioUrls } from '@/utils/minioUrls.js'
import { useDocumentosStore } from '@/stores/documentos.js'
import NpaCabecalho from '@/components/editor/NpaCabecalho.vue'
import NpaBloco from '@/components/editor/NpaBloco.vue'

// Prévia da NPA: o mesmo conteúdo do PDF (cabeçalho, corpo numerado pelo caminho, fecho e assinaturas) e os mesmos
// textos -- todos calculados por perfis/npa.js, espelho de CabecalhoDaNpa (backend). Só a diagramação é da tela.

const props = defineProps({
  documento:         { type: Object, default: null },
  campos:            { type: Object, default: null },   // { setorEmissor, local, assinaturas, elaboradoPor, aprovadoPor, boletimDaRevogacao }
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

// ─── Blocos ─────────────────────────────────────────────────────────────────────
// O corpo em ordem de leitura, em pedaços que não se partem entre folhas: cada elemento da parte normativa (com o HTML
// do texto de parágrafos e alíneas), o fecho, cada bloco de assinatura e as linhas de publicação/revogação.
// juntoComOProximo: título de capítulo/seção/subseção não fica sozinho no fim da folha, nem o fecho longe da assinatura.
const AGRUPAMENTOS = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

const blocos = computed(() => {
  const lista = []
  const secao = (props.documento?.secoes ?? []).find(s => s.tipo === 'parte_normativa')
  const percorrer = (els) => {
    for (const el of els ?? []) {
      lista.push({
        chave: el.id, idAttr: 'prev-' + el.id, tipo: 'elemento', el,
        html: el.tipo === 'paragrafo' || el.tipo === 'alinea' ? conteudoEmHtml(el.conteudo) : '',
        juntoComOProximo: AGRUPAMENTOS.has(el.tipo),
      })
      percorrer(el.filhos)
    }
  }
  percorrer(secao?.elementos)

  lista.push({ chave: 'fecho', tipo: 'fecho', texto: c.value.localEData, juntoComOProximo: true })
  c.value.assinaturas.forEach((a, i) => lista.push({ chave: 'assinatura-' + i, tipo: 'assinatura', assinatura: a, juntoComOProximo: false }))
  if (c.value.publicadaNo) lista.push({ chave: 'publicada', tipo: 'publicada', texto: c.value.publicadaNo, juntoComOProximo: false })
  if (c.value.revogadaNo) lista.push({ chave: 'revogada', tipo: 'publicada', texto: c.value.revogadaNo, juntoComOProximo: false })
  return lista
})

// ─── Paginação ──────────────────────────────────────────────────────────────────
// A folha A4 é 794 × 1123 px (96 dpi); a moldura fica a 2,5 cm das bordas (94 px) e vai de ponta a ponta da área do
// texto: 606 × 935 px, com 1 px de borda. Dentro dela o corpo tem 10 px acima e 12 px abaixo.
const ALTURA_DA_MOLDURA = 935 - 2
const FOLGA_DO_CORPO = 10 + 12

// Até medir, tudo numa folha só (o que já dá para ler); medido, os blocos vão para as folhas certas.
const paginas = ref([[]])
watch(blocos, (lista) => { if (paginas.value.length === 1) paginas.value = [lista.map((_, i) => i)] }, { immediate: true })

// Com blocos a menos do que na última medida (elemento removido), os índices que sobram não valem até a paginação refazer.
const folhas = computed(() => paginas.value.map(f => f.filter(i => i < blocos.value.length)))

const medidorRef = ref(null)
let _agendada = false

function agendarPaginacao() {
  if (_agendada) return
  _agendada = true
  // setTimeout (não requestAnimationFrame): numa aba em segundo plano o rAF não roda e a prévia ficaria sem paginar.
  setTimeout(() => { _agendada = false; paginarAgora() }, 0)
}

async function paginarAgora() {
  await nextTick()
  const raiz = medidorRef.value
  if (!raiz) return
  const alturas = Array.from(raiz.querySelectorAll('.bloco')).map(el => el.offsetHeight)
  if (alturas.length !== blocos.value.length) return   // o medidor ainda não refletiu os blocos atuais
  const alturaDoCabecalho = raiz.querySelector('table.cabecalho')?.offsetHeight ?? 0
  const livre = ALTURA_DA_MOLDURA - FOLGA_DO_CORPO
  const novas = paginar(alturas, {
    primeira: livre - alturaDoCabecalho,
    demais: livre,
    juntoComOProximo: blocos.value.map(b => b.juntoComOProximo),
  })
  if (JSON.stringify(novas) !== JSON.stringify(paginas.value)) paginas.value = novas
}

watch([blocos, c], agendarPaginacao, { flush: 'post' })

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
  agendarPaginacao()
  // A fonte do documento pode chegar depois da primeira medida e mudar a quebra das linhas.
  document.fonts?.ready?.then(agendarPaginacao)
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
  agendarPaginacao()
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

/* Texto da NPA: o da folha e o da cópia que mede -- têm de ser idênticos para as alturas medidas valerem. */
.npa-texto {
  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.25;
  color: #000;
  text-align: justify;
}

/* Folha A4 (794 × 1123 px @ 96 dpi). A moldura da NPA fica a 2,5 cm das bordas e vai até o fim da página. */
.pdf-page {
  width: 794px;
  height: 1123px;
  background: #fff;
  box-sizing: border-box;
  margin-bottom: 20px;
  box-shadow: 0 3px 18px rgba(0, 0, 0, 0.55);
  position: relative;
  overflow: hidden;
}
.moldura {
  position: absolute;
  top: 94px;
  left: 94px;
  width: 606px;
  height: 935px;
  box-sizing: border-box;
  border: 1px solid #000;
  overflow: hidden;
}
/* "2/3" na margem de cima, à direita, acima da moldura (da segunda folha em diante). */
.npa-numero { position: absolute; top: 60px; right: 94px; font-size: 12px; }

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
  top: 60px;
  left: 94px;
  border: 2px solid #c00000;
  color: #c00000;
  font-weight: 700;
  font-size: 14px;
  padding: 2px 8px;
  z-index: 11;
}

.corpo { padding: 10px 10px 12px; }

/* Cópia invisível para medir: mesma largura da moldura por dentro (606 - 2 px de borda), fora da vista e do zoom. */
.medidor {
  position: absolute;
  left: -10000px;
  top: 0;
  width: 604px;
  visibility: hidden;
  pointer-events: none;
}

.pdf-page--anexo { height: auto; min-height: 1123px; padding: 76px; }
.anexo-titulo { text-align: center; font-weight: 700; margin: 0 0 6px; }
.anexo-imagem { text-align: center; }
.anexo-imagem img { max-width: 100%; }
</style>
