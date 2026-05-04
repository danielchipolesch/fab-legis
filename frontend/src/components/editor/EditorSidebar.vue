<template>
  <v-navigation-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    width="290"
    color="surface"
    border="e"
    permanent
  >
    <!-- Header -->
    <div class="sidebar-header pa-3 d-flex align-center">
      <v-icon icon="mdi-file-tree-outline" color="primary" class="mr-2" />
      <span class="text-subtitle-2 font-weight-bold text-primary">Estrutura do Documento</span>
      <v-spacer />
      <v-btn icon="mdi-close" size="x-small" variant="text" @click="$emit('update:modelValue', false)" />
    </div>

    <v-divider />

    <div class="sidebar-body">
      <template v-for="secao in secoes" :key="secao.id">

        <!-- Section header -->
        <div
          class="secao-header pa-2 d-flex align-center"
          :class="{ 'secao-header--active': isExpandida(secao.tipo) }"
          @click="toggleSecao(secao.tipo)"
        >
          <v-icon
            :icon="isExpandida(secao.tipo) ? 'mdi-chevron-down' : 'mdi-chevron-right'"
            size="16"
            class="mr-1"
          />
          <v-icon :icon="secaoIcon(secao.tipo)" size="14" color="primary" class="mr-2" />
          <span class="text-caption font-weight-bold text-uppercase text-primary">
            {{ secao.titulo }}
          </span>
        </div>

        <!-- Section elements -->
        <div v-show="isExpandida(secao.tipo)" class="secao-elementos pl-1 pr-1 pb-2">

          <!-- Fixed elements (parte preliminar / parte final) -->
          <template v-if="secao.tipo !== 'parte_normativa'">
            <div
              v-for="el in secao.elementos"
              :key="el.id"
              class="fixed-item d-flex align-center px-3 py-1"
              :class="{ 'fixed-item--active': selectedId === el.id }"
              @click="$emit('select', el.id)"
            >
              <v-icon :icon="elementIcon(el.tipo)" size="13" color="secondary" class="mr-2" />
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
            <div class="mt-2 d-flex flex-column gap-1">

              <!-- Novo Capítulo — desabilitado se já há artigos top-level -->
              <v-tooltip
                :text="hasTopLevelArtigos ? 'Remova os artigos soltos antes de adicionar capítulos' : ''"
                location="top"
              >
                <template #activator="{ props: tp }">
                  <div v-bind="tp">
                    <v-menu :disabled="hasTopLevelArtigos">
                      <template #activator="{ props: mp }">
                        <v-btn
                          v-bind="mp"
                          variant="outlined"
                          color="primary"
                          size="small"
                          prepend-icon="mdi-folder-plus-outline"
                          :disabled="hasTopLevelArtigos"
                          block
                        >
                          Novo Capítulo
                        </v-btn>
                      </template>
                      <v-list density="compact" min-width="240">
                        <v-list-subheader>Título do capítulo</v-list-subheader>
                        <v-list-item
                          v-for="preset in CAPITULO_PRESETS"
                          :key="preset"
                          :title="preset"
                          :disabled="existingCapituloTitulos.has(preset)"
                          @click="$emit('add-capitulo', preset)"
                        />
                        <v-divider />
                        <v-list-item
                          prepend-icon="mdi-pencil-outline"
                          title="Personalizado (sem título)"
                          @click="$emit('add-capitulo', '')"
                        />
                      </v-list>
                    </v-menu>
                  </div>
                </template>
              </v-tooltip>

              <!-- Novo Artigo — desabilitado se já há capítulos -->
              <v-tooltip
                :text="hasCapitulos ? 'Adicione artigos dentro dos capítulos existentes' : ''"
                location="top"
              >
                <template #activator="{ props: tp }">
                  <v-btn
                    v-bind="tp"
                    variant="outlined"
                    size="small"
                    prepend-icon="mdi-plus"
                    :disabled="hasCapitulos"
                    block
                    @click="$emit('add-artigo')"
                  >
                    Novo Artigo
                  </v-btn>
                </template>
              </v-tooltip>

            </div>
          </template>

        </div>

        <v-divider />
      </template>
    </div>
  </v-navigation-drawer>
</template>

<script setup>
import { reactive, computed } from 'vue'
import draggable from 'vuedraggable'
import SectionTreeItem from './SectionTreeItem.vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'

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
.sidebar-header {
  background: rgb(var(--v-theme-surface));
  position: sticky;
  top: 0;
  z-index: 2;
}
.sidebar-body {
  overflow-y: auto;
  height: calc(100vh - 120px);
}
.secao-header {
  cursor: pointer;
  background: rgb(var(--v-theme-surface));
}
.secao-header:hover {
  background: rgba(var(--v-theme-primary), 0.06);
}
.secao-header--active {
  background: rgba(var(--v-theme-primary), 0.1);
}
.fixed-item {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
  min-height: 26px;
}
.fixed-item:hover {
  background: rgba(var(--v-theme-secondary), 0.1);
}
.fixed-item--active {
  background: rgba(var(--v-theme-primary), 0.15);
}
.drag-ghost {
  opacity: 0.4;
  background: rgba(var(--v-theme-primary), 0.1);
  border-radius: 6px;
}
</style>
