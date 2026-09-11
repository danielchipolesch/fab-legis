<template>
  <div class="diff-viewer">

    <!-- Row label: ancestrais em tom neutro (contexto), elemento alterado em destaque -->
    <div class="diff-row-label text-caption q-px-sm q-py-xs q-mb-sm">
      <span v-if="labelAncestrais" class="text-grey-6">{{ labelAncestrais }}, </span>
      <span class="text-weight-bold" :class="labelAtual ? 'text-primary' : 'text-grey-7'">
        {{ labelAtual ?? formatLabel(elemento) }}
      </span>
    </div>

    <!-- Side-by-side or unified diff -->
    <div v-if="hasDiff" class="diff-content" :class="mode === 'side' ? 'row' : ''" :style="mode === 'side' ? 'gap:12px' : ''">

      <!-- LEFT (version A) -->
      <div class="diff-pane col" :class="{ 'diff-pane--side': mode === 'side' }">
        <div class="diff-pane-header text-caption q-px-sm q-py-xs text-white" style="background:#1565C0">
          {{ labelA }}
        </div>
        <div class="diff-pane-content q-pa-sm" v-html="renderedA" />
        <div v-if="imagesA.length" class="diff-images q-pa-sm">
          <img v-for="(img, i) in imagesA" :key="i" :src="img.src" :alt="img.titulo" class="diff-image" />
        </div>
      </div>

      <!-- RIGHT (version B) — only in side mode -->
      <div v-if="mode === 'side'" class="diff-pane col">
        <div class="diff-pane-header text-caption q-px-sm q-py-xs text-white" style="background:#2E7D32">
          {{ labelB }}
        </div>
        <div class="diff-pane-content q-pa-sm" v-html="renderedB" />
        <div v-if="imagesB.length" class="diff-images q-pa-sm">
          <img v-for="(img, i) in imagesB" :key="i" :src="img.src" :alt="img.titulo" class="diff-image" />
        </div>
      </div>

      <!-- Unified diff -->
      <div v-if="mode === 'unified'" class="diff-unified q-pa-sm">
        <span
          v-for="(part, i) in diff"
          :key="i"
          :class="{
            'diff-added':   part.added,
            'diff-removed': part.removed,
          }"
          v-text="part.value"
        />
        <div v-if="imagesB.length" class="diff-images q-pt-sm">
          <img v-for="(img, i) in imagesB" :key="i" :src="img.src" :alt="img.titulo" class="diff-image" />
        </div>
      </div>

    </div>

    <!-- No diff -->
    <div v-else class="text-caption text-grey-7 q-px-sm q-py-sm text-italic">
      Sem alterações nesta seção.
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { diffWords } from 'diff'
import { formatLabel } from '@/utils/numbering.js'
import { resolveMinioUrls } from '@/utils/minioUrls.js'

const props = defineProps({
  elemento:        { type: Object, required: true },
  elementoB:       { type: Object, default: null },
  labelAncestrais: { type: String, default: null }, // cadeia de contexto (ex.: "Art. 12., § 1º")
  labelAtual:      { type: String, default: null },  // elemento alterado; sem override cai para formatLabel(elemento)
  labelA:          { type: String, default: 'Versão anterior' },
  labelB:          { type: String, default: 'Versão atual' },
  mode:            { type: String, default: 'side' }, // 'side' | 'unified'
})

function extractText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.type === 'hardBreak') return '\n'
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch { return '' }
}

// Figuras (anexos/imagens) são nós atômicos sem texto — extractText não os alcança,
// então diffWords nunca as via. Como não dá para "diffar" uma imagem palavra a
// palavra, elas são extraídas à parte e só exibidas (não comparadas).
function extractImages(conteudo) {
  if (!conteudo) return []
  try {
    const imgs = []
    const visit = (node) => {
      if (!node) return
      if (node.type === 'figure' && node.attrs?.src) {
        imgs.push({ src: node.attrs.src, titulo: node.attrs.titulo ?? '' })
      }
      if (node.content) node.content.forEach(visit)
    }
    visit(JSON.parse(conteudo))
    return imgs
  } catch { return [] }
}

const textA = computed(() => extractText(props.elemento?.conteudo))
const textB = computed(() => extractText(props.elementoB?.conteudo ?? props.elemento?.conteudo))

// O bucket do MinIO é privado -- as URLs extraídas precisam ser trocadas por URLs
// assinadas de curta duração antes de virar src (ver utils/minioUrls.js), por isso
// imagesA/imagesB são refs resolvidas de forma assíncrona, não computed direto.
const rawImagesA = computed(() => extractImages(props.elemento?.conteudo))
const rawImagesB = computed(() => extractImages(props.elementoB?.conteudo ?? props.elemento?.conteudo))
const imagesA = ref([])
const imagesB = ref([])

async function resolveImages(raw, alvo) {
  if (!raw.length) { alvo.value = []; return }
  const mapa = await resolveMinioUrls(raw.map(i => i.src))
  alvo.value = raw.map(i => ({ ...i, src: mapa.get(i.src) ?? i.src }))
}

watch(rawImagesA, (raw) => resolveImages(raw, imagesA), { immediate: true })
watch(rawImagesB, (raw) => resolveImages(raw, imagesB), { immediate: true })

const diff = computed(() => {
  if (textA.value === textB.value) return []
  return diffWords(textA.value, textB.value)
})

const hasDiff = computed(() => {
  if (diff.value.length) return true
  const srcsA = imagesA.value.map(i => i.src).join('|')
  const srcsB = imagesB.value.map(i => i.src).join('|')
  return srcsA !== srcsB
})

const renderedA = computed(() => {
  if (!diff.value.length) return `<span>${textA.value}</span>`
  return diff.value.map(part => {
    if (part.added) return ''
    if (part.removed) return `<mark class="diff-mark-removed">${part.value}</mark>`
    return `<span>${part.value}</span>`
  }).join('')
})

const renderedB = computed(() => {
  if (!diff.value.length) return `<span>${textB.value}</span>`
  return diff.value.map(part => {
    if (part.removed) return ''
    if (part.added) return `<mark class="diff-mark-added">${part.value}</mark>`
    return `<span>${part.value}</span>`
  }).join('')
})
</script>

<style scoped>
.diff-viewer {
  margin-bottom: 16px;
  border: 1px solid rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  overflow: hidden;
}
.diff-row-label {
  background: rgba(74, 111, 165, 0.08);
  border-bottom: 1px solid rgba(0, 0, 0, 0.2);
}
.diff-pane { flex: 1; min-width: 0; }
.diff-pane-header {
  font-size: 0.72rem;
  letter-spacing: 0.03em;
}
.diff-pane-content,
.diff-unified {
  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size: 11pt;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 60px;
  background: #FAFBFC;
}
.diff-pane--side + .diff-pane {
  border-left: 1px solid rgba(0, 0, 0, 0.3);
}
.diff-images {
  border-top: 1px dashed rgba(0, 0, 0, 0.2);
}
.diff-image {
  display: block;
  max-width: 100%;
  max-height: 220px;
  object-fit: contain;
  margin: 4px auto;
}
</style>

<style>
.diff-mark-removed {
  background: #FFCDD2;
  color: #B71C1C;
  text-decoration: line-through;
  border-radius: 2px;
  padding: 0 1px;
}
.diff-mark-added {
  background: #C8E6C9;
  color: #1B5E20;
  border-radius: 2px;
  padding: 0 1px;
}
.diff-removed { background: #FFCDD2; color: #B71C1C; }
.diff-added   { background: #C8E6C9; color: #1B5E20; }
</style>
