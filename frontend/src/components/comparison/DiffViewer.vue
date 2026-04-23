<template>
  <div class="diff-viewer">

    <!-- Row label -->
    <div class="diff-row-label text-caption font-weight-bold text-medium-emphasis px-3 py-1 mb-2">
      {{ formatLabel(elemento) }}
    </div>

    <!-- Side-by-side or unified diff -->
    <div v-if="diff.length" class="diff-content" :class="mode === 'side' ? 'd-flex gap-3' : ''">

      <!-- LEFT (version A) -->
      <div class="diff-pane flex-1" :class="{ 'diff-pane--side': mode === 'side' }">
        <div class="diff-pane-header text-caption px-3 py-1 text-white" style="background:#1565C0">
          {{ labelA }}
        </div>
        <div class="diff-pane-content pa-3" v-html="renderedA" />
      </div>

      <!-- RIGHT (version B) — only in side mode -->
      <div v-if="mode === 'side'" class="diff-pane flex-1">
        <div class="diff-pane-header text-caption px-3 py-1 text-white" style="background:#2E7D32">
          {{ labelB }}
        </div>
        <div class="diff-pane-content pa-3" v-html="renderedB" />
      </div>

      <!-- Unified diff -->
      <div v-if="mode === 'unified'" class="diff-unified pa-3">
        <span
          v-for="(part, i) in diff"
          :key="i"
          :class="{
            'diff-added':   part.added,
            'diff-removed': part.removed,
          }"
          v-text="part.value"
        />
      </div>

    </div>

    <!-- No diff -->
    <div v-else class="text-caption text-medium-emphasis px-3 py-2 font-italic">
      Sem alterações nesta seção.
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { diffWords } from 'diff'
import { formatLabel } from '@/utils/numbering.js'

const props = defineProps({
  elemento:  { type: Object, required: true },
  elementoB: { type: Object, default: null },
  labelA:    { type: String, default: 'Versão anterior' },
  labelB:    { type: String, default: 'Versão atual' },
  mode:      { type: String, default: 'side' }, // 'side' | 'unified'
})

function stripHtml(html) {
  const div = document.createElement('div')
  div.innerHTML = html ?? ''
  return div.textContent || ''
}

const textA = computed(() => stripHtml(props.elemento?.conteudo ?? ''))
const textB = computed(() => stripHtml(props.elementoB?.conteudo ?? props.elemento?.conteudo ?? ''))

const diff = computed(() => {
  if (textA.value === textB.value) return []
  return diffWords(textA.value, textB.value)
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
  border: 1px solid rgba(var(--v-border-color), 0.3);
  border-radius: 8px;
  overflow: hidden;
}
.diff-row-label {
  background: rgba(var(--v-theme-secondary), 0.08);
  border-bottom: 1px solid rgba(var(--v-border-color), 0.2);
}
.diff-pane { flex: 1; min-width: 0; }
.diff-pane-header {
  font-size: 0.72rem;
  letter-spacing: 0.03em;
}
.diff-pane-content,
.diff-unified {
  font-family: 'Times New Roman', serif;
  font-size: 11pt;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 60px;
  background: #FAFBFC;
}
.diff-pane--side + .diff-pane {
  border-left: 1px solid rgba(var(--v-border-color), 0.3);
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
