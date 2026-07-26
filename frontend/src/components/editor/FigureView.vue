<template>
  <node-view-wrapper as="figure" class="doc-figure-edit" :class="{ 'doc-figure-edit--selected': selected }">
    <input
      class="figura-titulo-input"
      :value="node.attrs.titulo"
      placeholder="Título da figura (ex: Figura 1 — Descrição)"
      @input="updateAttributes({ titulo: $event.target.value })"
      @keydown.enter.prevent
    />
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
import { NodeViewWrapper } from '@tiptap/vue-3'

defineProps({
  node:             { type: Object, required: true },
  updateAttributes: { type: Function, required: true },
  selected:         { type: Boolean, default: false },
  editor:           { type: Object, default: null },
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

.figura-titulo-input,
.figura-fonte-input {
  display: block;
  width: 100%;
  border: none;
  background: transparent;
  text-align: center;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  padding: 2px 4px;
  border-radius: 3px;
}

.figura-titulo-input {
  font-size: 12px;
  font-style: italic;
  color: #333;
  margin-bottom: 8px;
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
  font-size: 11px;
  color: #666;
  margin-top: 6px;
}
.figura-fonte-input:focus {
  background: rgba(25, 118, 210, 0.07);
}
</style>
