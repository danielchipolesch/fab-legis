<template>
  <div>
    <!-- Item row -->
    <div
      class="tree-item"
      :class="{
        'tree-item--active': isSelected,
        'tree-item--grouping': isGrouping,
      }"
      :style="{ paddingLeft: `${depth * 14 + 8}px` }"
      @click="$emit('select', element.id)"
    >
      <!-- drag handle -->
      <v-icon
        class="drag-handle mr-1"
        size="14"
        color="grey"
        icon="mdi-drag-vertical"
      />

      <!-- expand/collapse -->
      <v-btn
        v-if="element.filhos?.length"
        :icon="expanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
        size="x-small"
        variant="text"
        color="grey"
        class="mr-1"
        style="min-width:20px;width:20px;height:20px"
        @click.stop="expanded = !expanded"
      />
      <span v-else class="mr-1" style="display:inline-block;width:20px" />

      <!-- icon -->
      <v-icon :icon="elementIcon(element.tipo)" size="14" :color="iconColor" class="mr-1" />

      <!-- label -->
      <span class="tree-label text-truncate flex-grow-1" :title="fullLabel">
        <span :class="labelClass">{{ label }}</span>
        <span v-if="preview" class="text-caption text-medium-emphasis ml-1">{{ preview }}</span>
      </span>

      <!-- action buttons (shown on hover) -->
      <div class="tree-actions d-flex gap-1">
        <v-btn icon size="x-small" variant="text" color="grey" @click.stop="$emit('move-up', element.id)">
          <v-icon size="12">mdi-arrow-up</v-icon>
          <v-tooltip activator="parent" location="right">Mover acima</v-tooltip>
        </v-btn>
        <v-btn icon size="x-small" variant="text" color="grey" @click.stop="$emit('move-down', element.id)">
          <v-icon size="12">mdi-arrow-down</v-icon>
          <v-tooltip activator="parent" location="right">Mover abaixo</v-tooltip>
        </v-btn>
        <v-menu v-if="childOptions.length || canPromote">
          <template #activator="{ props: mp }">
            <v-btn v-bind="mp" icon size="x-small" variant="text" color="grey" @click.stop>
              <v-icon size="12">mdi-plus</v-icon>
            </v-btn>
          </template>
          <v-list density="compact" min-width="180">
            <template v-if="childOptions.length">
              <v-list-subheader>Adicionar como filho</v-list-subheader>
              <v-list-item
                v-for="opt in childOptions"
                :key="opt.tipo"
                :prepend-icon="elementIcon(opt.tipo)"
                :title="opt.label"
                @click="$emit('add-child', element.id, opt.tipo)"
              />
              <v-divider />
            </template>
            <template v-if="canPromote">
              <v-list-subheader>Reorganizar</v-list-subheader>
              <v-list-item prepend-icon="mdi-arrow-collapse-up" title="Promover nível" @click="$emit('promote', element.id)" />
              <v-list-item prepend-icon="mdi-arrow-expand-down" title="Rebaixar nível" @click="$emit('demote', element.id)" />
              <v-divider />
            </template>
            <v-list-item prepend-icon="mdi-delete-outline" title="Remover" class="text-error" @click="$emit('remove', element.id)" />
          </v-list>
        </v-menu>
        <v-btn v-else icon size="x-small" variant="text" color="error" @click.stop="$emit('remove', element.id)">
          <v-icon size="12">mdi-delete-outline</v-icon>
          <v-tooltip activator="parent" location="right">Remover</v-tooltip>
        </v-btn>
      </div>
    </div>

    <!-- children -->
    <template v-if="expanded && element.filhos?.length">
      <SectionTreeItem
        v-for="child in element.filhos"
        :key="child.id"
        :element="child"
        :depth="depth + 1"
        :selected-id="selectedId"
        @select="$emit('select', $event)"
        @move-up="$emit('move-up', $event)"
        @move-down="$emit('move-down', $event)"
        @add-child="(id, tipo) => $emit('add-child', id, tipo)"
        @promote="$emit('promote', $event)"
        @demote="$emit('demote', $event)"
        @remove="$emit('remove', $event)"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'

const props = defineProps({
  element:    { type: Object, required: true },
  depth:      { type: Number, default: 0 },
  selectedId: { type: String, default: null },
})

defineEmits(['select', 'move-up', 'move-down', 'add-child', 'promote', 'demote', 'remove'])

const expanded = ref(true)

const GROUPING_TIPOS = ['capitulo', 'secao_normativa', 'subsecao_normativa']
const ARTIGO_TIPOS   = ['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea']

const isGrouping = computed(() => GROUPING_TIPOS.includes(props.element.tipo))
const canPromote = computed(() => ARTIGO_TIPOS.includes(props.element.tipo))

const isSelected = computed(() => props.selectedId === props.element.id)

const label = computed(() => formatLabel(props.element))

const labelClass = computed(() => ({
  'font-weight-bold':    isGrouping.value,
  'font-weight-medium':  !isGrouping.value,
  'text-uppercase':      props.element.tipo === 'capitulo',
}))

const iconColor = computed(() => isGrouping.value ? 'primary' : 'secondary')

const preview = computed(() => {
  if (isGrouping.value) return ''
  const text = props.element.conteudo?.replace(/<[^>]+>/g, '').trim() ?? ''
  return text.length > 35 ? text.slice(0, 35) + '…' : text
})

const fullLabel = computed(() => `${label.value} ${preview.value}`)

const CHILD_MAP = {
  capitulo:           [
    { tipo: 'secao_normativa', label: 'Seção' },
    { tipo: 'artigo',          label: 'Artigo' },
  ],
  secao_normativa:    [
    { tipo: 'subsecao_normativa', label: 'Subseção' },
    { tipo: 'artigo',             label: 'Artigo' },
  ],
  subsecao_normativa: [
    { tipo: 'artigo',             label: 'Artigo' },
  ],
  artigo: [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)' },
    { tipo: 'inciso',          label: 'Inciso (I, II…)' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso (I, II…)' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso (I, II…)' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea (a, b…)' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea (1, 2…)' }],
}

const childOptions = computed(() => CHILD_MAP[props.element.tipo] ?? [])
</script>

<style scoped>
.tree-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding-top: 3px;
  padding-bottom: 3px;
  padding-right: 4px;
  border-radius: 6px;
  transition: background 0.15s;
  min-height: 28px;
}
.tree-item:hover {
  background: rgba(var(--v-theme-secondary), 0.1);
}
.tree-item--active {
  background: rgba(var(--v-theme-primary), 0.15) !important;
}
.tree-item--grouping {
  border-left: 2px solid rgba(var(--v-theme-primary), 0.3);
  margin-left: 2px;
}
.tree-label {
  font-size: 0.78rem;
  line-height: 1.2;
  max-width: 155px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tree-actions {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.tree-item:hover .tree-actions {
  opacity: 1;
}
.drag-handle {
  cursor: grab;
  opacity: 0;
  transition: opacity 0.15s;
}
.tree-item:hover .drag-handle {
  opacity: 0.5;
}
</style>
