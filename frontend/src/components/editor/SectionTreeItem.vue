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
      <q-icon
        class="drag-handle q-mr-xs"
        size="14px"
        color="grey"
        name="mdi-drag-vertical"
      />

      <!-- expand/collapse -->
      <q-btn
        v-if="element.filhos?.length"
        :icon="expanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
        size="xs"
        flat
        round
        dense
        color="grey"
        class="q-mr-xs"
        style="min-width:20px;width:20px;height:20px"
        @click.stop="expanded = !expanded"
      />
      <span v-else class="q-mr-xs" style="display:inline-block;width:20px" />

      <!-- icon -->
      <q-icon :name="elementIcon(element.tipo)" size="14px" :color="iconColor" class="q-mr-xs" />

      <!-- label -->
      <span class="tree-label ellipsis col" :title="fullLabel">
        <span :class="labelClass">{{ label }}</span>
        <span v-if="preview" class="text-caption text-grey-7 q-ml-xs">{{ preview }}</span>
      </span>

      <!-- status icon (conteúdo elements only) -->
      <q-icon
        v-if="!isGrouping && !isFilled"
        name="mdi-alert-circle"
        color="amber-7"
        size="12px"
        class="status-icon q-mr-xs"
      >
        <q-tooltip anchor="top middle" self="bottom middle">
          Vazio — necessário para aprovação
        </q-tooltip>
      </q-icon>

      <!-- action buttons (shown on hover) -->
      <div class="tree-actions row" style="gap:4px">
        <q-btn round size="xs" flat dense color="grey" @click.stop="$emit('move-up', element.id)">
          <q-icon size="12px" name="mdi-arrow-up" />
          <q-tooltip anchor="center right" self="center left">Mover acima</q-tooltip>
        </q-btn>
        <q-btn round size="xs" flat dense color="grey" @click.stop="$emit('move-down', element.id)">
          <q-icon size="12px" name="mdi-arrow-down" />
          <q-tooltip anchor="center right" self="center left">Mover abaixo</q-tooltip>
        </q-btn>
        <q-btn v-if="childOptions.length || canPromote" round size="xs" flat dense color="grey" @click.stop>
          <q-icon size="12px" name="mdi-plus" />
          <q-menu>
            <q-list dense style="min-width:180px">
              <template v-if="childOptions.length">
                <q-item-label header>Adicionar como filho</q-item-label>
                <q-item
                  v-for="opt in childOptions"
                  :key="opt.tipo"
                  clickable
                  v-close-popup
                  @click="$emit('add-child', element.id, opt.tipo)"
                >
                  <q-item-section avatar>
                    <q-icon :name="elementIcon(opt.tipo)" />
                  </q-item-section>
                  <q-item-section>{{ opt.label }}</q-item-section>
                </q-item>
                <q-separator />
              </template>
              <template v-if="canPromote">
                <q-item-label header>Reorganizar</q-item-label>
                <q-item clickable v-close-popup @click="$emit('promote', element.id)">
                  <q-item-section avatar>
                    <q-icon name="mdi-arrow-collapse-up" />
                  </q-item-section>
                  <q-item-section>Promover nível</q-item-section>
                </q-item>
                <q-item clickable v-close-popup @click="$emit('demote', element.id)">
                  <q-item-section avatar>
                    <q-icon name="mdi-arrow-expand-down" />
                  </q-item-section>
                  <q-item-section>Rebaixar nível</q-item-section>
                </q-item>
                <q-separator />
              </template>
              <q-item clickable v-close-popup class="text-negative" @click="$emit('remove', element.id)">
                <q-item-section avatar>
                  <q-icon name="mdi-delete-outline" color="negative" />
                </q-item-section>
                <q-item-section>Remover</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
        <q-btn v-else round size="xs" flat dense color="negative" @click.stop="$emit('remove', element.id)">
          <q-icon size="12px" name="mdi-delete-outline" />
          <q-tooltip anchor="center right" self="center left">Remover</q-tooltip>
        </q-btn>
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
  'text-weight-bold':   isGrouping.value,
  'text-weight-medium': !isGrouping.value,
  'text-uppercase':     props.element.tipo === 'capitulo',
}))

const iconColor = computed(() => isGrouping.value ? 'primary' : 'blue-grey-6')

const preview = computed(() => {
  if (isGrouping.value) return ''
  const text = props.element.conteudo?.replace(/<[^>]+>/g, '').trim() ?? ''
  return text.length > 35 ? text.slice(0, 35) + '…' : text
})

const isFilled = computed(() => {
  if (isGrouping.value) return false
  const text = props.element.conteudo?.replace(/<[^>]+>/g, '').trim() ?? ''
  return text.length > 0
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
  background: rgba(74, 111, 165, 0.08);
}
.tree-item--active {
  background: rgba(74, 111, 165, 0.16) !important;
}
.tree-item--grouping {
  border-left: 2px solid rgba(11, 61, 145, 0.35);
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
.status-icon {
  flex-shrink: 0;
  opacity: 0.85;
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
