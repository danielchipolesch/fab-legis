<template>
  <aside
    v-show="modelValue"
    class="editor-sidebar"
    style="width:290px"
  >
    <!-- Barra de progresso ao adicionar elemento -->
    <q-linear-progress
      v-if="editorStore.adicionando"
      indeterminate
      color="primary"
      style="height:2px"
    />

    <!-- Header -->
    <div class="sidebar-header q-pa-sm row items-center">
      <q-icon name="mdi-file-tree-outline" color="primary" class="q-mr-sm" />
      <span class="text-subtitle2 text-weight-bold text-primary">Estrutura do Documento</span>
      <q-space />
      <q-btn icon="mdi-close" size="xs" flat round dense @click="$emit('update:modelValue', false)" />
    </div>

    <q-separator />

    <div class="sidebar-body">
      <template v-for="secao in secoes" :key="secao.id">

        <!-- Section header -->
        <div
          class="secao-header q-pa-sm row items-center"
          :class="{ 'secao-header--active': isExpandida(secao.tipo) }"
          @click="toggleSecao(secao.tipo)"
        >
          <q-icon
            :name="isExpandida(secao.tipo) ? 'mdi-chevron-down' : 'mdi-chevron-right'"
            size="16px"
            class="q-mr-xs"
          />
          <q-icon :name="secaoIcon(secao.tipo)" size="14px" color="primary" class="q-mr-sm" />
          <span class="text-caption text-weight-bold text-uppercase text-primary">
            {{ secao.titulo }}
          </span>
        </div>

        <!-- Section elements -->
        <div v-show="isExpandida(secao.tipo)" class="secao-elementos q-px-xs q-pb-sm">

          <!-- Fixed elements (parte preliminar / parte final) -->
          <template v-if="secao.tipo !== 'parte_normativa'">
            <div
              v-for="el in secao.elementos"
              :key="el.id"
              class="fixed-item row items-center q-px-md q-py-xs"
              :class="{ 'fixed-item--active': selectedId === el.id }"
              @click="$emit('select', el.id)"
            >
              <q-icon :name="elementIcon(el.tipo)" size="13px" color="secondary" class="q-mr-sm" />
              <span class="text-caption">{{ formatLabel(el) }}</span>
            </div>
          </template>

          <!-- Normative elements (drag-and-drop tree) -->
          <template v-else>
            <draggable
              v-model="normativaElementos"
              item-key="id"
              handle=".drag-handle"
              ghost-class="drag-ghost"
              @end="onDragEnd"
            >
              <template #item="{ element }">
                <SectionTreeItem
                  :element="element"
                  :depth="0"
                  :selected-id="selectedId"
                  @select="$emit('select', $event)"
                  @move-up="$emit('move-up', $event)"
                  @move-down="$emit('move-down', $event)"
                  @add-child="(parentId, tipo) => $emit('add-child', parentId, tipo)"
                  @promote="$emit('promote', $event)"
                  @demote="$emit('demote', $event)"
                  @remove="$emit('remove', $event)"
                />
              </template>
            </draggable>

            <!-- Add buttons for normativa -->
            <div class="q-mt-sm column" style="gap:4px">

              <!-- Novo Capítulo — desabilitado se já há artigos top-level -->
              <div>
                <q-btn
                  outline
                  color="primary"
                  size="sm"
                  class="full-width"
                  :disable="hasTopLevelArtigos || editorStore.adicionando"
                  :loading="editorStore.adicionando"
                >
                  <q-icon left name="mdi-folder-plus-outline" />
                  Novo Capítulo
                  <q-tooltip v-if="hasTopLevelArtigos" anchor="top middle" self="bottom middle">
                    Remova os artigos soltos antes de adicionar capítulos
                  </q-tooltip>
                  <q-menu v-if="!hasTopLevelArtigos">
                    <q-list dense style="min-width:240px">
                      <q-item-label header>Título do capítulo</q-item-label>
                      <q-item
                        v-for="preset in CAPITULO_PRESETS"
                        :key="preset"
                        clickable
                        v-close-popup
                        :disable="existingCapituloTitulos.has(preset)"
                        @click="$emit('add-capitulo', preset)"
                      >
                        <q-item-section>{{ preset }}</q-item-section>
                      </q-item>
                      <q-separator />
                      <q-item clickable v-close-popup @click="$emit('add-capitulo', '')">
                        <q-item-section avatar>
                          <q-icon name="mdi-pencil-outline" />
                        </q-item-section>
                        <q-item-section>Personalizado (sem título)</q-item-section>
                      </q-item>
                    </q-list>
                  </q-menu>
                </q-btn>
              </div>

              <!-- Novo Artigo — desabilitado se já há capítulos -->
              <q-btn
                outline
                size="sm"
                class="full-width"
                :disable="hasCapitulos || editorStore.adicionando"
                :loading="editorStore.adicionando"
                @click="$emit('add-artigo')"
              >
                <q-icon left name="mdi-plus" />
                Novo Artigo
                <q-tooltip v-if="hasCapitulos" anchor="top middle" self="bottom middle">
                  Adicione artigos dentro dos capítulos existentes
                </q-tooltip>
              </q-btn>

            </div>
          </template>

        </div>

        <q-separator />
      </template>
    </div>
  </aside>
</template>

<script setup>
import { reactive, computed } from 'vue'
import draggable from 'vuedraggable'
import SectionTreeItem from './SectionTreeItem.vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'
import { useEditorStore } from '@/stores/editor.js'

const editorStore = useEditorStore()

const props = defineProps({
  modelValue: { type: Boolean, default: true },
  secoes:     { type: Array, default: () => [] },
  selectedId: { type: String, default: null },
})

const emit = defineEmits([
  'update:modelValue',
  'select',
  'move-up', 'move-down',
  'add-child', 'add-artigo', 'add-capitulo',
  'promote', 'demote', 'remove',
  'reorder-normativa',
])

const CAPITULO_PRESETS = [
  'DISPOSIÇÕES PRELIMINARES',
  'DISPOSIÇÕES GERAIS',
  'DISPOSIÇÕES FINAIS',
  'DISPOSIÇÕES TRANSITÓRIAS',
]

const secaoExpandida = reactive({
  parte_preliminar: true,
  parte_normativa:  true,
  parte_final:      true,
})

function toggleSecao(tipo) {
  secaoExpandida[tipo] = !secaoExpandida[tipo]
}

function isExpandida(tipo) {
  return secaoExpandida[tipo] !== false
}

const normativaElementos = computed({
  get() {
    const s = props.secoes.find(s => s.tipo === 'parte_normativa')
    return s?.elementos ?? []
  },
  set(val) {
    emit('reorder-normativa', val)
  },
})

// Enforce: artigos top-level e capítulos não podem coexistir
const hasCapitulos       = computed(() => normativaElementos.value.some(e => e.tipo === 'capitulo'))
const hasTopLevelArtigos = computed(() => normativaElementos.value.some(e => e.tipo === 'artigo'))

const existingCapituloTitulos = computed(() =>
  new Set(
    normativaElementos.value
      .filter(e => e.tipo === 'capitulo' && e.titulo)
      .map(e => e.titulo.toUpperCase())
  )
)

function onDragEnd() {}

function secaoIcon(tipo) {
  const m = {
    parte_preliminar: 'mdi-text-box-outline',
    parte_normativa:  'mdi-format-list-numbered',
    parte_final:      'mdi-flag-outline',
  }
  return m[tipo] ?? 'mdi-folder-outline'
}
</script>

<style scoped>
.editor-sidebar {
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-right: 1px solid rgba(0, 0, 0, 0.12);
}
.sidebar-header {
  background: var(--color-surface);
  position: sticky;
  top: 0;
  z-index: 2;
}
.sidebar-body {
  overflow-y: auto;
  flex: 1 1 auto;
}
.secao-header {
  cursor: pointer;
  background: var(--color-surface);
}
.secao-header:hover {
  background: rgba(26, 46, 90, 0.06);
}
.secao-header--active {
  background: rgba(26, 46, 90, 0.1);
}
.fixed-item {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
  min-height: 26px;
}
.fixed-item:hover {
  background: rgba(74, 111, 165, 0.1);
}
.fixed-item--active {
  background: rgba(26, 46, 90, 0.15);
}
.drag-ghost {
  opacity: 0.4;
  background: rgba(26, 46, 90, 0.1);
  border-radius: 6px;
}
</style>
