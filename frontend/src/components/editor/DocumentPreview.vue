<template>
  <div class="preview-outer" ref="outerRef">
    <div class="preview-hint">Prévia aproximada · {{ docId }} {{ documento?.titulo ? '— ' + documento.titulo : '' }}</div>

    <!--
      Todas as páginas têm largura A4 fixa (794 px).
      CSS zoom escala o layout inteiro para caber no painel.
    -->
    <div class="pages-wrap" :style="{ zoom: pageScale }">

      <!-- ═══════════════════════════════════════════════════
           PÁGINA 1 — PORTARIA DE APROVAÇÃO
           Estrutura: cabeçalho → epígrafe → ementa → preâmbulo → RESOLVE: → artigos → assinatura
           (No PDF real da NSCA 5-3, a portaria vem ANTES da capa)
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

        <!-- Cabeçalho (Art. 18): Brasão + hierarquia institucional -->
        <div class="cabecalho">
          <img
            v-if="!brasaoFailed"
            src="/brasao-do-brasil-republica-colorido.png"
            class="cabecalho-brasao"
            alt="Brasão"
            @error="brasaoFailed = true"
          />
          <div v-else class="cabecalho-brasao-ph">
            <div class="cb-ring"><span class="cb-star">✦</span></div>
          </div>
          <p class="cabecalho-linha bold">MINISTÉRIO DA DEFESA</p>
          <p class="cabecalho-linha bold">COMANDO DA AERONÁUTICA</p>
          <p class="cabecalho-linha cabecalho-om" v-if="orgLabel">{{ orgLabel }}</p>
        </div>

        <!-- Epígrafe portaria: maiúsculas, sem negrito, centralizada (Art. 5 §1) -->
        <div
          :id="preliEpigrafe?.id ? 'prev-' + preliEpigrafe.id : undefined"
          class="epigrafe prel-content"
          v-html="epigrafeHtml ?? ('PORTARIA Nº ___, DE ' + dataFormatada)"
        ></div>

        <!-- Ementa: alinhada à direita, bloco 9 cm, justificada (Art. 6 §1) -->
        <div class="ementa-bloco">
          <div
            :id="preliEmenta?.id ? 'prev-' + preliEmenta.id : undefined"
            class="ementa-txt prel-content"
            v-html="ementaHtml ?? ('Aprova a ' + especieCompleta + ' que dispõe sobre ' + (documento?.assunto_basico ?? '___') + '.')"
          ></div>
        </div>

        <!-- Preâmbulo: O signatário, autoria em maiúsculo e negrito (Art. 7) -->
        <div
          :id="preliPreambulo?.id ? 'prev-' + preliPreambulo.id : undefined"
          class="body-el prel-content"
          v-html="preambuloHtml ?? '<strong>O COMANDANTE DA AERONÁUTICA</strong>, no uso das atribuições que lhe confere o art. 12 da Lei Complementar nº\xA097, de 9 de junho de 1999, tendo em vista o que consta do Processo nº\xA0___/___-___/___,'"
        ></div>

        <!-- Fundamentação: conteúdo adicional do preâmbulo (ex: CONSIDERANDO que...) -->
        <div
          v-if="fundHtml"
          :id="'prev-' + preliFundamentacao.id"
          class="body-el prel-content"
          v-html="fundHtml"
        ></div>

        <p class="resolve-line">resolve:</p>

        <!-- Artigos da portaria -->
        <p class="body-el">
          <strong>Art. 1°</strong>&nbsp; Fica aprovada a {{ especieCompleta }} – {{ docId }}, que dispõe sobre
          {{ documento?.assunto_basico }}.
        </p>
        <p class="body-el">
          <strong>Art. 2°</strong>&nbsp; Esta Portaria entra em vigor na data de sua publicação.
        </p>

        <!-- Assinatura: maiúsculo, sem negrito, centralizado (Art. 12 V) -->
        <div class="assinatura-bloco">
          <p class="assin-data">Brasília, {{ dataFormatada }}</p>
          <p class="assin-nome">{{ orgLabel }}</p>
          <p class="assin-cargo">Comandante da Aeronáutica</p>
        </div>
      </div>

      <!-- ═══════════════════════════════════════════════════
           PÁGINA 2 — CAPA  (NSCA 5-3 Anexo II, Art. 17)
           • MINISTÉRIO DA DEFESA / COMANDO DA AERONÁUTICA — corpo 17, negrito, centralizado
           • Nome da OM — corpo 15, maiúsculo, centralizado
           • Gládio Alado FAB (centrado, grande)
           • ASSUNTO BÁSICO — negrito, maiúsculo, centralizado
           • Legenda — corpo 14, negrito, maiúsculo, com borda
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page capa-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

        <!-- Cabeçalho institucional -->
        <div class="capa-header">
          <p class="capa-inst">MINISTÉRIO DA DEFESA</p>
          <p class="capa-inst">COMANDO DA AERONÁUTICA</p>
          <p class="capa-om" v-if="orgLabel">{{ orgLabel }}</p>
        </div>

        <div class="capa-spacer"></div>

        <!-- Gládio Alado -->
        <div class="capa-simbolo">
          <img
            v-if="!gladioFailed"
            src="/brasao-fab.png"
            class="capa-brasao-img"
            alt="Gládio Alado FAB"
            @error="gladioFailed = true"
          />
          <div v-else class="capa-gladio">
            <div class="gladio-ring">
              <span class="gladio-star">✦</span>
              <span class="gladio-label">FORÇA AÉREA<br>BRASILEIRA</span>
            </div>
          </div>
        </div>

        <div class="capa-spacer capa-spacer-sm"></div>

        <!-- Assunto básico -->
        <p class="capa-assunto">{{ documento?.assunto_basico?.toUpperCase() ?? '' }}</p>

        <div class="capa-spacer capa-spacer-sm"></div>

        <!-- Legenda (Art. 17 V) -->
        <div class="capa-legenda">
          <p class="legenda-sigla">{{ docId }}</p>
          <p class="legenda-titulo">{{ (documento?.titulo ?? especieCompleta).toUpperCase() }}</p>
          <p class="legenda-ano">{{ anoAtual }}</p>
        </div>

        <div class="capa-spacer capa-spacer-xs"></div>
      </div>

      <!-- ═══════════════════════════════════════════════════
           PÁGINA 3+ — SUMÁRIO + PARTE NORMATIVA (contínuos, sem quebra)
           Formato NSCA: ANEXO I → título → SUMÁRIO → corpo normativo
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

        <p class="sumario-anexo">ANEXO I</p>
        <p class="sumario-anexo-titulo">{{ (documento?.titulo ?? especieCompleta).toUpperCase() }} ({{ docId }})</p>

        <p class="sumario-titulo">SUMÁRIO</p>

        <div class="sumario-header-row">
          <span></span>
          <span class="sumario-art-hdr">Art.</span>
        </div>

        <div
          v-for="item in tocItems"
          :key="item.id"
          :class="['toc-row', item.kind]"
        >
          <span class="toc-lbl">{{ item.label }}</span>
          <span class="toc-dots"></span>
          <span class="toc-pg">{{ item.pg }}</span>
        </div>

        <!-- Separador: uma linha em branco entre sumário e corpo -->
        <div class="sumario-corpo-sep"></div>

        <!-- Parte Normativa — capítulos, seções, artigos, parágrafos, incisos, alíneas -->
        <template v-for="item in normativaFlat" :key="item.el.id">

          <div v-if="item.el.tipo === 'capitulo'" :id="'prev-' + item.el.id" class="capitulo-heading">
            <p class="cap-numero">CAPÍTULO {{ toRomanStr(item.el.numero) }}</p>
            <p v-if="item.el.titulo" class="cap-titulo">{{ item.el.titulo.toUpperCase() }}</p>
          </div>

          <div v-else-if="item.el.tipo === 'secao_normativa'" :id="'prev-' + item.el.id" class="secao-heading">
            <p class="sec-numero"><strong>Seção {{ toRomanStr(item.el.numero) }}</strong></p>
            <p v-if="item.el.titulo" class="sec-titulo"><strong>{{ item.el.titulo }}</strong></p>
          </div>

          <div v-else-if="item.el.tipo === 'subsecao_normativa'" :id="'prev-' + item.el.id" class="subsecao-heading">
            <p class="subsec-numero"><strong>Subseção {{ toRomanStr(item.el.numero) }}</strong></p>
            <p v-if="item.el.titulo" class="subsec-titulo"><strong>{{ item.el.titulo }}</strong></p>
          </div>

          <div v-else-if="hasBlockContent(item.el.conteudo)" :id="'prev-' + item.el.id" class="body-el norm-el">
            <span class="norm-lbl" :class="{ 'norm-lbl-bold': item.el.tipo === 'artigo' }">{{ item.label }}</span>
            <div class="norm-content-block" v-html="item.el.conteudo"></div>
          </div>
          <p v-else :id="'prev-' + item.el.id" class="body-el norm-el">
            <span class="norm-lbl" :class="{ 'norm-lbl-bold': item.el.tipo === 'artigo' }">{{ item.label }}</span><span class="norm-content" v-html="stripHtml(item.el.conteudo)"></span>
          </p>

        </template>

        <!-- Parte Final — cláusulas, fecho, assinatura, referenda -->
        <template v-for="el in secaoFinal?.elementos ?? []" :key="el.id">
          <div
            v-if="el.tipo === 'clausula_revogatoria' || el.tipo === 'clausula_vigencia'"
            :id="'prev-' + el.id"
            class="body-el"
            v-html="el.conteudo"
          ></div>
          <div v-else-if="el.tipo === 'fecho'"      :id="'prev-' + el.id" class="body-el" v-html="el.conteudo"></div>
          <div v-else-if="el.tipo === 'assinatura'" :id="'prev-' + el.id" class="assinatura-bloco corpo-assin" v-html="el.conteudo"></div>
          <div v-else-if="el.tipo === 'referenda'"  :id="'prev-' + el.id" class="assinatura-bloco corpo-assin" v-html="el.conteudo"></div>
        </template>

      </div>

    </div><!-- /pages-wrap -->
  </div><!-- /preview-outer -->
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { bodyLabel, formatLabel, toRoman } from '@/utils/numbering.js'

function toRomanStr(n) { return toRoman(n ?? 0) }

const props = defineProps({
  documento:         { type: Object, default: null },
  selectedElementId: { type: String, default: null },
})

watch(() => props.selectedElementId, async (id) => {
  if (!id) return
  await nextTick()
  const el = document.getElementById('prev-' + id)
  if (!el) return

  // Find the nearest scrollable ancestor without scrolling the window
  let container = el.parentElement
  while (container && container !== document.body) {
    const overflow = window.getComputedStyle(container).overflowY
    if (overflow === 'auto' || overflow === 'scroll') break
    container = container.parentElement
  }
  if (!container || container === document.body) return

  const containerRect = container.getBoundingClientRect()
  const elRect = el.getBoundingClientRect()
  const targetTop = container.scrollTop + elRect.top - containerRect.top
                    - containerRect.height / 2 + elRect.height / 2
  container.scrollTo({ top: targetTop, behavior: 'smooth' })
})

// ─── Scaling: ResizeObserver + CSS zoom ──────────────────
const A4_W      = 794      // A4 width em px @ 96 dpi
const outerRef  = ref(null)
const pageScale = ref(0.75) // default seguro até ResizeObserver disparar

const brasaoFailed = ref(false)
const gladioFailed = ref(false)

let _ro = null
onMounted(() => {
  _ro = new ResizeObserver(([entry]) => {
    const avail = entry.contentRect.width - 32 // padding 16px × 2
    pageScale.value = +(Math.min(1, avail / A4_W).toFixed(4))
  })
  if (outerRef.value) _ro.observe(outerRef.value)
})
onUnmounted(() => _ro?.disconnect())

// ─── Constantes ───────────────────────────────────────────
const ESPECIE_COMPLETA = {
  ICA:       'INSTRUÇÃO DO COMANDO DA AERONÁUTICA',
  NSCA:      'NORMA DE SISTEMA DO COMANDO DA AERONÁUTICA',
  Portaria:  'PORTARIA',
  Resolução: 'RESOLUÇÃO',
  Decreto:   'DECRETO',
  Aviso:     'AVISO',
  Mensagem:  'MENSAGEM',
}

const WM_COLOR = { RASCUNHO: '#b50000', MINUTA: '#b50000', APROVADO: '#1a6b1a' }

// ─── Helpers ─────────────────────────────────────────────
function stripHtml(html) {
  const d = document.createElement('div')
  d.innerHTML = html ?? ''
  return (d.textContent || '').trim()
}

function hasBlockContent(html) {
  return /<(table|ul|ol|blockquote|h[1-6]|figure)/i.test(html ?? '')
}

function formatarDataBR(iso) {
  if (!iso) return '___________'
  const [y, m, d] = iso.split('-')
  const meses = ['janeiro','fevereiro','março','abril','maio','junho',
                 'julho','agosto','setembro','outubro','novembro','dezembro']
  return `${+d} de ${meses[+m - 1]} de ${y}`
}

const GROUPING_TIPOS = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

/**
 * Achata os elementos normativos preservando os agrupamentos (capítulo, seção, subseção)
 * como itens especiais para renderização no preview.
 * Todos os elementos de corpo compartilham o mesmo recuo de 2,5 cm (Art. 9 XX e).
 */
function flattenNorm(elementos, out = []) {
  for (const el of elementos ?? []) {
    if (GROUPING_TIPOS.has(el.tipo)) {
      out.push({ el, label: null })
      if (el.filhos?.length) flattenNorm(el.filhos, out)
    } else {
      out.push({ el, label: bodyLabel(el) })
      if (el.filhos?.length) flattenNorm(el.filhos, out)
    }
  }
  return out
}

// ─── Computeds ────────────────────────────────────────────
const docId = computed(() => {
  const d = props.documento
  if (!d) return ''
  const numStr = [d.numero_basico, d.numero_secundario].filter(Boolean).join('-')
  return [d.especie, numStr].filter(Boolean).join(' ')
})

const especieCompleta = computed(() =>
  ESPECIE_COMPLETA[props.documento?.especie] ?? props.documento?.especie?.toUpperCase() ?? ''
)

const dataFormatada = computed(() => formatarDataBR(props.documento?.data_criacao))
const anoAtual      = new Date().getFullYear()

const orgLabel = computed(() =>
  (props.documento?.organizacao ?? '')
    .replace(/^[A-Z]+ — /, '')
    .toUpperCase()
)

const wmText  = computed(() => {
  const s = props.documento?.status
  return ['RASCUNHO', 'MINUTA', 'APROVADO'].includes(s) ? s : ''
})
const wmColor = computed(() => WM_COLOR[props.documento?.status] ?? '#888')

// Acesso direto às seções por tipo
const secaoPreliminar = computed(() =>
  (props.documento?.secoes ?? []).find(s => s.tipo === 'parte_preliminar')
)

// Elementos da parte preliminar (para a portaria)
function _preliText(el) {
  return el?.conteudo?.replace(/<[^>]*>/g, '').trim() ?? ''
}
const preliEpigrafe      = computed(() => secaoPreliminar.value?.elementos?.find(e => e.tipo === 'epigrafe'))
const preliEmenta        = computed(() => secaoPreliminar.value?.elementos?.find(e => e.tipo === 'ementa'))
const preliPreambulo     = computed(() => secaoPreliminar.value?.elementos?.find(e => e.tipo === 'preambulo'))
const preliFundamentacao = computed(() => secaoPreliminar.value?.elementos?.find(e => e.tipo === 'fundamentacao'))

const epigrafeHtml    = computed(() => _preliText(preliEpigrafe.value)     ? preliEpigrafe.value.conteudo     : null)
const ementaHtml      = computed(() => _preliText(preliEmenta.value)       ? preliEmenta.value.conteudo       : null)
const preambuloHtml   = computed(() => _preliText(preliPreambulo.value)    ? preliPreambulo.value.conteudo    : null)
const fundHtml        = computed(() => _preliText(preliFundamentacao.value) ? preliFundamentacao.value.conteudo : null)
const secaoNormativa = computed(() =>
  (props.documento?.secoes ?? []).find(s => s.tipo === 'parte_normativa')
)
const secaoFinal = computed(() =>
  (props.documento?.secoes ?? []).find(s => s.tipo === 'parte_final')
)

// Flat list for corpo rendering
const normativaFlat = computed(() =>
  flattenNorm(secaoNormativa.value?.elementos ?? [])
)

// Extrai figuras de todos os elementos do documento para a LISTA DE FIGURAS
const figurasNoDocumento = computed(() => {
  const figuras = []
  function extrairDe(elementos) {
    for (const el of elementos ?? []) {
      if (el.conteudo) {
        const div = document.createElement('div')
        div.innerHTML = el.conteudo
        div.querySelectorAll('figure[data-type="figura"]').forEach((fig) => {
          const titulo = fig.querySelector('.figura-titulo')?.textContent?.trim() ?? ''
          figuras.push(titulo)
        })
      }
      extrairDe(el.filhos)
    }
  }
  for (const s of props.documento?.secoes ?? []) extrairDe(s.elementos)
  return figuras
})

// TOC no formato NSCA: capítulos/seções com intervalo de artigos (ex: "1°/6°", "7°/11")
const tocItems = computed(() => {
  const items = []
  const elementos = secaoNormativa.value?.elementos ?? []

  function firstArtigoNum(lista) {
    for (const el of lista ?? []) {
      if (el.tipo === 'artigo') return el.numero
      if (el.filhos?.length) { const f = firstArtigoNum(el.filhos); if (f != null) return f }
    }
    return null
  }

  function lastArtigoNum(lista) {
    let last = null
    for (const el of lista ?? []) {
      if (el.tipo === 'artigo') last = el.numero
      if (el.filhos?.length) { const l = lastArtigoNum(el.filhos); if (l != null) last = l }
    }
    return last
  }

  // Formato: ordinal (°) até 9, cardinal (sem sufixo) a partir de 10 — Decreto 12.002 Art. 9
  function fmtNum(n) { return n <= 9 ? `${n}°` : `${n}` }

  function fmtRange(lista) {
    const first = firstArtigoNum(lista)
    if (first == null) return ''
    const last = lastArtigoNum(lista)
    return first === last ? fmtNum(first) : `${fmtNum(first)}/${fmtNum(last)}`
  }

  const temAgrupamento = elementos.some(el =>
    el.tipo === 'capitulo' || el.tipo === 'secao_normativa' || el.tipo === 'subsecao_normativa'
  )

  if (temAgrupamento) {
    function walk(lista) {
      for (const el of lista ?? []) {
        if (el.tipo === 'capitulo') {
          const titulo = el.titulo ? ` - ${el.titulo.toUpperCase()}` : ''
          items.push({ id: el.id, label: `CAPÍTULO ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-capitulo', pg: fmtRange(el.filhos) })
          walk(el.filhos)
        } else if (el.tipo === 'secao_normativa') {
          const titulo = el.titulo ? ` - ${el.titulo}` : ''
          items.push({ id: el.id, label: `Seção ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-secao', pg: fmtRange(el.filhos) })
          walk(el.filhos)
        } else if (el.tipo === 'subsecao_normativa') {
          const titulo = el.titulo ? ` - ${el.titulo}` : ''
          items.push({ id: el.id, label: `Subseção ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-subsecao', pg: fmtRange(el.filhos) })
          walk(el.filhos)
        }
      }
    }
    walk(elementos)
  } else {
    for (const el of elementos) {
      if (el.tipo === 'artigo') {
        const txt = stripHtml(el.conteudo)
        const trunc = txt.length > 50 ? txt.slice(0, 50) + '…' : txt
        items.push({ id: el.id, label: `${bodyLabel(el).trim()} ${trunc}`, kind: 'toc-artigo', pg: el.numero != null ? fmtNum(el.numero) : '' })
      }
    }
  }

  // LISTA DE FIGURAS — ao final do sumário, se houver figuras no documento
  if (figurasNoDocumento.value.length) {
    items.push({ id: '__figuras-hdr', label: 'LISTA DE FIGURAS', kind: 'toc-figuras-hdr', pg: '' })
    figurasNoDocumento.value.forEach((titulo, i) => {
      items.push({ id: `__fig-${i}`, label: titulo || `Figura ${i + 1}`, kind: 'toc-figura', pg: '' })
    })
  }

  return items
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════
   Container externo
════════════════════════════════════════════════════════════ */
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

/* ═══════════════════════════════════════════════════════════
   Wrapper das páginas — zoom CSS escala todo o layout
════════════════════════════════════════════════════════════ */
.pages-wrap {
  transform-origin: top left;
  width: 794px;
}

/* ═══════════════════════════════════════════════════════════
   Folha A4 — NSCA 5-3 Art. 9 XX
   Dimensões: 794 × 1123 px @ 96 dpi
   Margens: 2 cm = 76 px todos os lados
   Fonte: Calibri / Carlito, corpo 12 (16 px)
   Espaçamento: simples (1,2) + 8 px após parágrafo
   Recuo primeira linha: 2,5 cm = 94 px
════════════════════════════════════════════════════════════ */
.pdf-page {
  width:      794px;
  min-height: 1123px;
  background: #fff;
  box-sizing: border-box;
  padding:    76px;          /* 2 cm */
  margin-bottom: 20px;
  box-shadow: 0 3px 18px rgba(0,0,0,0.55);
  position:   relative;
  overflow:   hidden;

  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size:   16px;         /* 12 pt */
  line-height: 1.2;          /* espaçamento simples */
  color:       #000;
  text-align:  justify;
}

/* ─── Marca d'água ──────────────────────────────────────── */
.wm-overlay {
  position:      absolute;
  top:   50%;
  left:  50%;
  transform: translate(-50%, -50%) rotate(-45deg);
  font-size:     110px;
  font-weight:   900;
  opacity:       0.07;
  pointer-events: none;
  white-space:   nowrap;
  letter-spacing: 12px;
  z-index: 10;
  font-family: Arial, sans-serif;
}

/* ═══════════════════════════════════════════════════════════
   CAPA  (NSCA 5-3 Art. 17)
   Altura fixa para distribuição vertical com flex spacers
════════════════════════════════════════════════════════════ */
.capa-page {
  height: 1123px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.capa-header { width: 100%; }

/* Espaçadores flex que dividem o espaço vertical restante */
.capa-spacer    { flex: 0.3; }
.capa-spacer-sm { flex: 0.4; }
.capa-spacer-xs { flex: 0.35; }

/* MINISTÉRIO DA DEFESA / COMANDO DA AERONÁUTICA — corpo 17 ≈ 23px, negrito */
.capa-inst {
  font-size:   23px;
  font-weight: bold;
  margin: 2px 0;
  text-transform: uppercase;
}

/* Nome da OM — sem negrito, maiúsculo */
.capa-om {
  font-size: 18px;
  margin: 4px 0 0;
  text-transform: uppercase;
}

/* Gládio Alado — grande, proporcional ao documento real */
.capa-simbolo { }
.capa-brasao-img { width: 320px; height: 320px; object-fit: contain; }
.capa-gladio { display: flex; justify-content: center; }
.gladio-ring {
  width: 220px; height: 220px;
  border-radius: 50%;
  background: #1A2E5A;
  border: 10px solid #B8972E;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: #fff;
}
.gladio-star  { font-size: 70px; line-height: 1; }
.gladio-label { font-size: 12px; text-align: center; line-height: 1.3; margin-top: 6px; font-family: Arial, sans-serif; }

/* ASSUNTO BÁSICO — negrito, maiúsculo, grande */
.capa-assunto {
  font-size:   28px;
  font-weight: bold;
  text-transform: uppercase;
  margin: 0;
  max-width: 500px;
  text-align: center;
}

/* Legenda (Art. 17 V) — corpo 14 ≈ 19px, negrito, maiúsculo, com borda */
.capa-legenda {
  width: 420px;
  height: 5cm;
  border: 1.5px solid #000;
  padding: 20px 28px;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
}
.legenda-sigla  { font-size: 16px; font-weight: bold; margin: 0; }
.legenda-titulo { font-size: 16px; font-weight: bold; margin: 0; text-transform: uppercase; }
.legenda-ano    { font-size: 16px; font-weight: bold; margin: 0; }

/* ═══════════════════════════════════════════════════════════
   CABEÇALHO  (NSCA 5-3 Art. 18)
   Aparece na primeira página de cada documento/anexo
════════════════════════════════════════════════════════════ */
.cabecalho {
  text-align: center;
  margin-bottom: 16px;
  line-height: 1.0;
}
.cabecalho-brasao {
  width: 60px;
  height: 60px;
  object-fit: contain;
  display: block;
  margin: 0 auto 6px;
}
.cabecalho-brasao-ph { display: flex; justify-content: center; margin-bottom: 6px; }
.cb-ring {
  width: 60px; height: 60px;
  border-radius: 50%;
  background: #1A2E5A;
  border: 5px solid #B8972E;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.cb-star { font-size: 22px; }

.cabecalho-linha {
  font-size: 16px; /* 12pt, mesma do corpo */
  margin: 0;
  text-align: center;
}
.cabecalho-linha.bold { font-weight: bold; }
.cabecalho-linha.cabecalho-om { text-decoration: underline; }

/* ═══════════════════════════════════════════════════════════
   EPÍGRAFE  (NSCA 5-3 Art. 5 §1)
   Maiúsculas, sem negrito, centralizada, sem ponto final
════════════════════════════════════════════════════════════ */
.epigrafe {
  text-align:     center;
  text-transform: uppercase;
  font-weight:    normal;
  margin:         16px 0 0;
  text-indent:    0;
  font-size:      16px;
}
/* Remove margem de <p> injetado via v-html nos elementos da portaria */
.prel-content p { margin: 0; }

/* ═══════════════════════════════════════════════════════════
   EMENTA  (NSCA 5-3 Art. 6 §1)
   Alinhada à margem direita, bloco de 9 cm, justificada
   Cálculo: content-width (642px) − 9cm (340px) = 302px de margem esquerda
════════════════════════════════════════════════════════════ */
.ementa-bloco {
  margin-left: 302px;   /* deixa 9 cm à direita */
  margin-top:  4px;
  margin-bottom: 12px;
  text-indent: 0;       /* sem recuo de primeira linha (Art. 6 §2) */
}
.ementa-txt {
  margin: 0;
  text-align: justify;
  font-size: 16px;
}
.ementa-txt :deep(p) { margin: 0; display: inline; }

/* ═══════════════════════════════════════════════════════════
   ELEMENTO DO CORPO — parágrafo genérico
   Recuo 2,5 cm na PRIMEIRA LINHA (Art. 9 XX e)
   Espaçamento simples + 6 pt (≈ 8 px) após cada parágrafo (Art. 9 XX d)
   TODOS os tipos (artigo, parágrafo, inciso, alínea) usam o MESMO recuo —
   a hierarquia é expressa apenas pelo rótulo (Art./§/I/a)), não por indentação
════════════════════════════════════════════════════════════ */
.body-el {
  text-indent:   94px;   /* 2,5 cm @ 96 dpi */
  margin-top:    0;
  margin-bottom: 8px;    /* 6 pt ≈ 8 px */
  line-height:   1.2;
  text-align:    justify;
}

/* Parágrafos internos de v-html sem recuo adicional */
.body-el :deep(p)        { display: inline; margin: 0; text-indent: 0; }
.body-el :deep(p + p)    { display: block; margin-top: 8px; text-indent: 94px; }
.body-el :deep(strong)   { font-weight: bold; }
.body-el :deep(em)       { font-style: italic; }
.body-el :deep(u)        { text-decoration: underline; }

/* ═══════════════════════════════════════════════════════════
   CAPÍTULO  (NSCA 5-3 Art. 9 XVI)
   Maiúsculas, negrito, centralizado; linha em branco antes
════════════════════════════════════════════════════════════ */
.capitulo-heading {
  text-align: center;
  margin-top:    20px;
  margin-bottom: 4px;
  text-indent:   0;
}
.cap-numero {
  font-weight: bold;
  text-transform: uppercase;
  font-size: 16px;
  margin: 0;
  text-indent: 0;
}
.cap-titulo {
  font-weight: bold;
  text-transform: uppercase;
  font-size: 16px;
  margin: 0 0 8px;
  text-indent: 0;
}

/* ═══════════════════════════════════════════════════════════
   SEÇÃO / SUBSEÇÃO  (NSCA 5-3 Art. 9 XVIII)
   Centralizada, 2 linhas (numeração + título), negrito
════════════════════════════════════════════════════════════ */
.secao-heading,
.subsecao-heading {
  text-align: center;
  margin-top:    16px;
  margin-bottom: 4px;
  text-indent:   0;
}
.sec-numero, .subsec-numero {
  font-size: 16px;
  margin: 0;
  text-indent: 0;
}
.sec-titulo, .subsec-titulo {
  font-size: 16px;
  margin: 0 0 6px;
  text-indent: 0;
}

/* ─── Elementos normativos (artigo, parágrafo, inciso, alínea, sub-alínea) ─── */
.norm-el {
  /* herda .body-el — mesmo recuo 2,5 cm */
}

/* Conteúdo com bloco (tabela, lista): reseta recuo e renderiza HTML completo */
.norm-content-block {
  text-indent: 0;
  margin-top: 6px;
}
.norm-content-block :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 4px 0;
  font-size: 14px;
  line-height: 1.3;
}
.norm-content-block :deep(td),
.norm-content-block :deep(th) {
  border: 1px solid #999;
  padding: 4px 8px;
  vertical-align: top;
}
.norm-content-block :deep(th) {
  background: rgba(11, 61, 145, 0.06);
  font-weight: bold;
  text-align: center;
}
.norm-content-block :deep(p) {
  margin: 0;
  text-indent: 0;
}

/* Rótulo do artigo em negrito; demais (§, incisos, alíneas) em peso normal */
.norm-lbl      { font-weight: normal; }
.norm-lbl-bold { font-weight: bold; }

/* Conteúdo inline com o rótulo */
.norm-content { font-weight: normal; }

/* ─── "resolve:" — início da parte dispositiva ──────────── */
.resolve-line {
  text-align:  center;
  margin:      12px 0;
  font-weight: normal;
  font-size:   16px;
  text-indent: 0;
}

/* ═══════════════════════════════════════════════════════════
   ASSINATURA  (NSCA 5-3 Art. 12 V)
   Maiúsculo, sem negrito, centralizado
════════════════════════════════════════════════════════════ */
.assinatura-bloco {
  margin-top: 48px;
  text-align: center;
  text-indent: 0;
}
.assin-data  { margin: 0 0 32px; font-size: 16px; }
.assin-nome  { font-size: 16px; text-transform: uppercase; font-weight: normal; margin: 0; }
.assin-cargo { font-size: 16px; margin: 0; }

/* Assinatura no corpo (parte final) */
.corpo-assin :deep(p)      { margin: 0; display: block; text-indent: 0; }
.corpo-assin :deep(strong) { font-weight: bold; }

/* ═══════════════════════════════════════════════════════════
   SUMÁRIO  (NSCA 5-3 Art. 19)
════════════════════════════════════════════════════════════ */
.sumario-anexo {
  text-align:  center;
  font-weight: bold;
  font-size:   16px;
  margin:      0;
  text-indent: 0;
}

.sumario-anexo-titulo {
  text-align:  center;
  font-weight: bold;
  font-size:   16px;
  margin:      0 0 20px;
  text-indent: 0;
}

.sumario-titulo {
  text-align:  center;
  font-weight: bold;
  font-size:   16px;
  margin:      0 0 16px;
  text-indent: 0;
}

.sumario-header-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
  font-weight: bold;
  font-size: 14px;
}
.sumario-art-hdr { min-width: 72px; text-align: right; }

/* Espaço de uma linha entre o sumário e o início da parte normativa */
.sumario-corpo-sep { height: 1.2em; }

.toc-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 3px;
  font-size: 14px;
}
.toc-row.toc-capitulo    { font-weight: bold; text-transform: uppercase; margin-top: 6px; }
.toc-row.toc-secao       { padding-left: 16px; margin-top: 2px; }
.toc-row.toc-subsecao    { padding-left: 28px; }
.toc-row.toc-artigo      { padding-left: 28px; }
.toc-row.toc-figuras-hdr { font-weight: bold; text-transform: uppercase; margin-top: 10px; }
.toc-row.toc-figura      { padding-left: 16px; font-style: italic; }
.toc-lbl  { flex-shrink: 0; max-width: 80%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.toc-dots { flex-grow: 1; border-bottom: 1px dotted #999; margin: 0 6px 3px; min-width: 16px; }
.toc-pg   { flex-shrink: 0; min-width: 72px; text-align: right; color: #333; }

/* ═══════════════════════════════════════════════════════════
   FIGURAS — renderização no corpo do documento
   (injetadas via v-html dentro de .norm-content-block)
════════════════════════════════════════════════════════════ */
.norm-content-block :deep(figure.doc-figure) {
  display: block;
  text-align: center;
  margin: 16px auto;
  max-width: 100%;
}
.norm-content-block :deep(.figura-titulo) {
  font-size: 13px;
  font-style: italic;
  margin: 0 0 6px;
  text-indent: 0;
  text-align: center;
}
.norm-content-block :deep(.figura-img) {
  max-width: 100%;
  height: auto;
  border: 1px solid #ddd;
  display: block;
  margin: 0 auto;
}
.norm-content-block :deep(.figura-fonte) {
  font-size: 12px;
  color: #555;
  margin: 4px 0 0;
  text-indent: 0;
  text-align: center;
}
</style>
