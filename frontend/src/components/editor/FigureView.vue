<template>
  <node-view-wrapper as="figure" class="doc-figure-edit" :class="{ 'doc-figure-edit--selected': selected }">
    <!-- Linha do título: número automático (readonly) + descrição (editável) -->
    <div class="figura-titulo-row">
      <span class="figura-num-prefix">Figura {{ figureNumber }} —&nbsp;</span>
      <input
        class="figura-titulo-input"
        :value="node.attrs.titulo"
        placeholder="Descrição da figura"
        @input="updateAttributes({ titulo: $event.target.value })"
        @keydown.enter.prevent
      />
    </div>

    <img
      :src="node.attrs.src"
      :alt="node.attrs.alt"
      class="figura-img"
      @error="onImgError"
    />

    <input
      class="figura-fonte-input"
      :value="node.attrs.fonte"
      placeholder="Fonte: (ex: elaborado pelo autor, 2024)"
      @input="updateAttributes({ fonte: $event.target.value })"
      @keydown.enter.prevent
    />
  </node-view-wrapper>
</template>

<script setup>
import { computed } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'

const props = defineProps({
  node:             { type: Object, required: true },
  updateAttributes: { type: Function, required: true },
  selected:         { type: Boolean, default: false },
  editor:           { type: Object, default: null },
  getPos:           { type: Function, default: null },
})

// Conta quantas figuras existem antes desta no documento → número sequencial
const figureNumber = computed(() => {
  if (!props.getPos || !props.editor?.state?.doc) return '?'
  const pos = props.getPos()
  let count = 0
  props.editor.state.doc.nodesBetween(0, pos, (node) => {
    if (node.type.name === 'figure') count++
  })
  return count + 1
})

function onImgError(e) {
  e.target.style.display = 'none'
}
</script>

<style scoped>
.doc-figure-edit {
  display: block;
  margin: 16px auto;
  text-align: center;
  border: 1px dashed #b0bec5;
  border-radius: 6px;
  padding: 12px;
  background: #f9fafb;
  cursor: default;
  max-width: 100%;
}

.doc-figure-edit--selected {
  border-color: #1976d2;
  background: #e3f2fd;
}

/* Linha título: prefixo fixo + input da descrição lado a lado */
.figura-titulo-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 8px;
  font-size: 12px;
  font-style: italic;
  color: #333;
}

.figura-num-prefix {
  white-space: nowrap;
  flex-shrink: 0;
}

.figura-titulo-input {
  border: none;
  background: transparent;
  outline: none;
  font-family: inherit;
  font-size: inherit;
  font-style: inherit;
  color: inherit;
  padding: 2px 4px;
  border-radius: 3px;
  min-width: 120px;
  flex: 1;
  text-align: left;
}
.figura-titulo-input:focus {
  background: rgba(25, 118, 210, 0.07);
}

.figura-img {
  max-width: 100%;
  max-height: 400px;
  height: auto;
  border: 1px solid #e0e0e0;
  border-radius: 2px;
  display: block;
  margin: 0 auto;
}

.figura-fonte-input {
  display: block;
  width: 100%;
  border: none;
  background: transparent;
  text-align: center;
  outline: none;
  font-family: inherit;
  font-size: 11px;
  color: #666;
  margin-top: 6px;
  box-sizing: border-box;
  padding: 2px 4px;
  border-radius: 3px;
}
.figura-fonte-input:focus {
  background: rgba(25, 118, 210, 0.07);
}
</style>
