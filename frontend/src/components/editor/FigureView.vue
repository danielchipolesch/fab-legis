<template>
  <node-view-wrapper as="figure" class="doc-figure-edit" :class="{ 'doc-figure-edit--selected': selected }">

    <!-- Título: "Figura N — " fixo + campo de descrição -->
    <div class="figura-titulo-row">
      <span class="figura-prefix">Figura N —&nbsp;</span>
      <input
        class="figura-titulo-input"
        :value="node.attrs.titulo"
        placeholder="Descrição da figura"
        @input="updateAttributes({ titulo: $event.target.value })"
        @keydown.enter.prevent
      />
    </div>

    <img
      :src="srcResolvido"
      :alt="node.attrs.alt"
      class="figura-img"
      @error="onImgError"
    />

    <!-- Fonte: "Fonte: " fixo + campo do texto da fonte -->
    <div class="figura-fonte-row">
      <span class="figura-prefix">Fonte:&nbsp;</span>
      <input
        class="figura-fonte-input"
        :value="node.attrs.fonte"
        placeholder="nome/órgão/ano"
        @input="updateAttributes({ fonte: $event.target.value })"
        @keydown.enter.prevent
      />
    </div>

  </node-view-wrapper>
</template>

<script setup>
import { ref, watch } from 'vue'
import { NodeViewWrapper } from '@tiptap/vue-3'
import { resolveMinioUrl } from '@/utils/minioUrls.js'

const props = defineProps({
  node:             { type: Object, required: true },
  updateAttributes: { type: Function, required: true },
  selected:         { type: Boolean, default: false },
  editor:           { type: Object, default: null },
})

// O bucket do MinIO é privado -- a URL armazenada não é diretamente buscável pelo
// navegador, precisa ser trocada por uma URL assinada de curta duração antes de
// virar src (ver utils/minioUrls.js).
const srcResolvido = ref(props.node.attrs.src)
watch(() => props.node.attrs.src, async (src) => {
  srcResolvido.value = await resolveMinioUrl(src)
}, { immediate: true })

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
  box-sizing: border-box;
}

.doc-figure-edit--selected {
  border-color: #1976d2;
  background: #e3f2fd;
}

/* Linha de título e linha de fonte: centralizadas, prefixo fixo + campo editável */
.figura-titulo-row,
.figura-fonte-row {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}

.figura-titulo-row {
  font-size: 12px;
  font-style: italic;
  color: #333;
  margin-bottom: 8px;
}

.figura-fonte-row {
  font-size: 11px;
  color: #555;
  margin-top: 6px;
}

.figura-prefix {
  white-space: nowrap;
  flex-shrink: 0;
  user-select: none;
}

.figura-titulo-input,
.figura-fonte-input {
  border: none;
  background: transparent;
  outline: none;
  font-family: inherit;
  font-size: inherit;
  font-style: inherit;
  color: inherit;
  padding: 2px 4px;
  border-radius: 3px;
  min-width: 100px;
  width: auto;
  flex: 1;
  max-width: 320px;
  text-align: left;
}

.figura-titulo-input:focus,
.figura-fonte-input:focus {
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
</style>
