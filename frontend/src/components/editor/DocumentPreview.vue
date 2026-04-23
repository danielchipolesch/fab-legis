<template>
  <div class="preview-outer" ref="outerRef">
    <div class="preview-hint">Prévia aproximada · NSCA 5-3 Anexo II</div>

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
          <p class="cabecalho-linha" v-if="orgLabel">{{ orgLabel }}</p>
        </div>

        <!-- Epígrafe portaria: maiúsculas, sem negrito, centralizada (Art. 5 §1) -->
        <p class="epigrafe">PORTARIA Nº ___, DE {{ dataFormatada }}</p>

        <!-- Ementa: alinhada à direita, bloco 9 cm, justificada (Art. 6 §1) -->
        <div class="ementa-bloco">
          <p class="ementa-txt">Aprova a {{ especieCompleta }} que dispõe sobre {{ documento?.assunto_basico }}.</p>
        </div>

        <!-- Preâmbulo: O signatário, autoria em maiúsculo e negrito (Art. 7) -->
        <p class="body-el">
          <strong>O COMANDANTE DA AERONÁUTICA</strong>, no uso das atribuições que lhe confere o art. 12 da
          Lei Complementar nº&nbsp;97, de 9 de junho de 1999, tendo em vista o que consta do Processo
          nº&nbsp;___/___-___/___,
        </p>

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
           • Símbolo FAB (Gládio Alado)
           • ASSUNTO BÁSICO — corpo 19, negrito, maiúsculo, centralizado
           • Legenda — corpo 14, negrito, maiúsculo
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page capa-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

        <!-- Cabeçalho institucional -->
        <p class="capa-inst" style="font-size:22px">MINISTÉRIO DA DEFESA</p>
        <p class="capa-inst" style="font-size:22px">COMANDO DA AERONÁUTICA</p>
        <p class="capa-om" v-if="orgLabel">{{ orgLabel }}</p>

        <!-- Símbolo FAB -->
        <div class="capa-simbolo">
          <img
            v-if="!brasaoFailed"
            src="/brasao-fab.png"
            class="capa-brasao-img"
            alt="Símbolo FAB"
            @error="brasaoFailed = true"
          />
          <div v-else class="capa-gladio">
            <div class="gladio-ring">
              <span class="gladio-star">✦</span>
              <span class="gladio-label">FORÇA AÉREA<br>BRASILEIRA</span>
            </div>
          </div>
        </div>

        <!-- Assunto básico -->
        <p class="capa-assunto">{{ documento?.assunto_basico?.toUpperCase() ?? '' }}</p>

        <!-- Legenda -->
        <div class="capa-legenda">
          <p class="legenda-sigla">{{ docId }}</p>
          <p class="legenda-titulo">{{ especieCompleta }}</p>
          <p class="legenda-ano">{{ anoAtual }}</p>
        </div>
      </div>

      <!-- ═══════════════════════════════════════════════════
           PÁGINA 3 — SUMÁRIO (Art. 19 — opcional)
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

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
      </div>

      <!-- ═══════════════════════════════════════════════════
           PÁGINA 4+ — CORPO DO DOCUMENTO
           Estrutura: cabeçalho → epígrafe → ementa → preâmbulo → normativa → parte final
           Tipografia: Calibri 12, margens 2cm, recuo 2,5cm primeira linha (Art. 9 XX)
      ════════════════════════════════════════════════════════ -->
      <div class="pdf-page corpo-page">
        <div v-if="wmText" class="wm-overlay" :style="{ color: wmColor }">{{ wmText }}</div>

        <!-- Cabeçalho (Art. 18) -->
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
          <p class="cabecalho-linha" v-if="orgLabel">{{ orgLabel }}</p>
        </div>

        <!-- Parte Preliminar -->
        <template v-for="el in secaoPreliminar?.elementos ?? []" :key="el.id">
          <div v-if="el.tipo === 'epigrafe'" :id="'prev-' + el.id" class="epigrafe" v-html="stripHtml(el.conteudo).toUpperCase()"></div>
          <div v-else-if="el.tipo === 'ementa'" :id="'prev-' + el.id" class="ementa-bloco">
            <p class="ementa-txt" v-html="el.conteudo"></p>
          </div>
          <div v-else-if="el.tipo === 'preambulo'"    :id="'prev-' + el.id" class="body-el" v-html="el.conteudo"></div>
          <div v-else-if="el.tipo === 'fundamentacao'" :id="'prev-' + el.id" class="body-el" v-html="el.conteudo"></div>
        </template>

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

          <p v-else :id="'prev-' + item.el.id" class="body-el norm-el">
            <span class="norm-lbl" :class="{ 'norm-lbl-bold': item.el.tipo === 'artigo' }">{{ item.label }}&nbsp;&nbsp;</span><span class="norm-content" v-html="stripHtml(item.el.conteudo)"></span>
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
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
})

// ─── Scaling: ResizeObserver + CSS zoom ──────────────────
const A4_W      = 794      // A4 width em px @ 96 dpi
const outerRef  = ref(null)
const pageScale = ref(0.75) // default seguro até ResizeObserver disparar

const brasaoFailed = ref(false)

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
  return d ? [d.especie, d.numero_basico, d.numero_secundario].filter(Boolean).join(' ') : ''
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

// TOC para o sumário — inclui capítulos, seções, subseções e artigos
const tocItems = computed(() => {
  const items = []
  let pg = 4

  function walk(elementos) {
    for (const el of elementos ?? []) {
      if (el.tipo === 'capitulo') {
        const titulo = el.titulo ? ` — ${el.titulo.toUpperCase()}` : ''
        items.push({ id: el.id, label: `CAPÍTULO ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-capitulo', pg: `${pg}°` })
        walk(el.filhos)
      } else if (el.tipo === 'secao_normativa') {
        const titulo = el.titulo ? ` — ${el.titulo}` : ''
        items.push({ id: el.id, label: `Seção ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-secao', pg: `${pg}°` })
        walk(el.filhos)
      } else if (el.tipo === 'subsecao_normativa') {
        const titulo = el.titulo ? ` — ${el.titulo}` : ''
        items.push({ id: el.id, label: `Subseção ${toRomanStr(el.numero)}${titulo}`, kind: 'toc-subsecao', pg: `${pg}°` })
        walk(el.filhos)
      } else if (el.tipo === 'artigo') {
        const txt = stripHtml(el.conteudo)
        const trunc = txt.length > 50 ? txt.slice(0, 50) + '…' : txt
        items.push({ id: el.id, label: `${bodyLabel(el)} ${trunc}`, kind: 'toc-artigo', pg: `${pg}°` })
        pg++
      }
    }
  }

  walk(secaoNormativa.value?.elementos ?? [])
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
════════════════════════════════════════════════════════════ */
.capa-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* MINISTÉRIO DA DEFESA / COMANDO DA AERONÁUTICA — corpo 17 ≈ 23px, negrito */
.capa-inst {
  font-size:   23px;
  font-weight: bold;
  margin: 2px 0;
  text-transform: uppercase;
}

/* Nome da OM — corpo 15 ≈ 20px */
.capa-om {
  font-size: 20px;
  margin: 4px 0 16px;
  text-transform: uppercase;
}

/* Símbolo FAB (Gládio Alado) */
.capa-simbolo { margin: 20px 0; }
.capa-brasao-img { width: 130px; height: 130px; object-fit: contain; }
.capa-gladio { display: flex; justify-content: center; }
.gladio-ring {
  width: 130px; height: 130px;
  border-radius: 50%;
  background: #1A2E5A;
  border: 8px solid #B8972E;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: #fff;
}
.gladio-star  { font-size: 42px; line-height: 1; }
.gladio-label { font-size: 8px; text-align: center; line-height: 1.3; margin-top: 4px; font-family: Arial, sans-serif; }

/* ASSUNTO BÁSICO — corpo 19 ≈ 25px, negrito, maiúsculo */
.capa-assunto {
  font-size:   25px;
  font-weight: bold;
  text-transform: uppercase;
  margin: 20px 0 16px;
  max-width: 500px;
  text-align: center;
}

/* Legenda — corpo 14 ≈ 19px, negrito, maiúsculo */
.capa-legenda {
  width: 9cm;
  height: 5cm;
  border: 2px solid #000;
  padding: 12px 20px;
  text-align: center;
  margin-top: auto;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
}
.legenda-sigla  { font-size: 19px; font-weight: bold; margin: 0; }
.legenda-titulo { font-size: 19px; font-weight: bold; margin: 0; text-transform: uppercase; }
.legenda-ano    { font-size: 19px; font-weight: bold; margin: 0; }

/* ═══════════════════════════════════════════════════════════
   CABEÇALHO  (NSCA 5-3 Art. 18)
   Aparece na primeira página de cada documento/anexo
════════════════════════════════════════════════════════════ */
.cabecalho {
  text-align: center;
  margin-bottom: 16px;
  line-height: 1.2;
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
.sumario-titulo {
  text-align:  center;
  font-weight: bold;
  font-size:   16px;
  margin:      0 0 12px;
  text-indent: 0;
}

.sumario-header-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
  font-weight: bold;
  font-size: 14px;
}
.sumario-art-hdr { min-width: 60px; text-align: right; }

.toc-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 4px;
  font-size: 14px;
}
.toc-row.toc-capitulo  { font-weight: bold; text-transform: uppercase; margin-top: 8px; }
.toc-row.toc-secao     { font-weight: bold; padding-left: 16px; margin-top: 4px; }
.toc-row.toc-subsecao  { padding-left: 28px; }
.toc-row.toc-artigo    { padding-left: 28px; }
.toc-lbl  { flex-shrink: 0; max-width: 82%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.toc-dots { flex-grow: 1; border-bottom: 1px dotted #999; margin: 0 6px 3px; min-width: 16px; }
.toc-pg   { flex-shrink: 0; min-width: 48px; text-align: right; color: #333; }
</style>
