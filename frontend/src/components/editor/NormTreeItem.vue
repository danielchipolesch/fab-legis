<template>
  <draggable
    :list="elements"
    item-key="id"
    handle=".drag-handle"
    :animation="150"
    ghost-class="norm-ghost"
    @end="$emit('reorder')"
  >
    <template #item="{ element }">
      <div class="norm-item">

        <!-- Row -->
        <div
          class="norm-row row items-center no-wrap"
          :class="{ 'norm-row--selected': selectedId === element.id }"
          @click="callbacks.select(element.id)"
        >
          <!-- Expand toggle -->
          <button
            v-if="element.filhos?.length"
            class="expand-btn"
            @click.stop="toggleExpand(element.id)"
          >
            <q-icon
              :name="expanded[element.id] !== false ? 'mdi-chevron-down' : 'mdi-chevron-right'"
              size="12px"
              color="grey-6"
            />
          </button>
          <span v-else class="expand-spacer" />

          <!-- Drag handle -->
          <q-icon
            name="mdi-drag-vertical"
            size="14px"
            color="grey-5"
            class="drag-handle"
          />

          <!-- Type icon -->
          <q-icon
            :name="elementIcon(element.tipo)"
            size="13px"
            :color="isGrouping(element.tipo) ? 'primary' : 'blue-grey-6'"
            class="q-mr-xs"
            style="flex-shrink:0"
          />

          <!-- Label -->
          <span
            class="norm-label col ellipsis text-caption"
            :class="{
              'text-weight-bold text-uppercase': element.tipo === 'capitulo',
              'text-weight-bold': element.tipo === 'secao_normativa' || element.tipo === 'subsecao_normativa',
            }"
          >{{ formatLabel(element) }}</span>

          <!-- Content preview -->
          <span
            v-if="!isGrouping(element.tipo) && nodePreview(element)"
            class="text-caption text-grey-6 q-ml-xs norm-preview"
          >{{ nodePreview(element) }}</span>

          <!-- Empty alert -->
          <q-icon
            v-if="!isGrouping(element.tipo) && !isNodeFilled(element)"
            name="mdi-alert-circle"
            color="amber-7"
            size="12px"
            class="q-ml-xs"
            style="flex-shrink:0"
          >
            <q-tooltip anchor="top middle" self="bottom middle">
              Vazio — necessário para aprovação
            </q-tooltip>
          </q-icon>

          <!-- Actions (visible on hover) -->
          <div class="norm-actions row items-center" style="gap:2px;flex-shrink:0">
            <q-btn
              v-if="childOptions(element).length || canPromote(element)"
              round size="xs" flat dense color="grey"
              @click.stop
            >
              <q-icon size="11px" name="mdi-plus" />
              <q-menu>
                <q-list dense style="min-width:180px">
                  <template v-if="childOptions(element).length">
                    <q-item-label header>Adicionar como filho</q-item-label>
                    <q-item
                      v-for="opt in childOptions(element)"
                      :key="opt.tipo"
                      clickable v-close-popup
                      @click="callbacks.addChild(element.id, opt.tipo)"
                    >
                      <q-item-section avatar>
                        <q-icon :name="elementIcon(opt.tipo)" />
                      </q-item-section>
                      <q-item-section>{{ opt.label }}</q-item-section>
                    </q-item>
                    <q-separator />
                  </template>
                  <template v-if="canPromote(element)">
                    <q-item-label header>Reorganizar</q-item-label>
                    <q-item clickable v-close-popup @click="callbacks.promote(element.id)">
                      <q-item-section avatar>
                        <q-icon name="mdi-arrow-collapse-up" />
                      </q-item-section>
                      <q-item-section>Promover nível</q-item-section>
                    </q-item>
                    <q-item clickable v-close-popup @click="callbacks.demote(element.id)">
                      <q-item-section avatar>
                        <q-icon name="mdi-arrow-expand-down" />
                      </q-item-section>
                      <q-item-section>Rebaixar nível</q-item-section>
                    </q-item>
                    <q-separator />
                  </template>
                  <q-item
                    clickable v-close-popup
                    class="text-negative"
                    @click="callbacks.remove(element.id)"
                  >
                    <q-item-section avatar>
                      <q-icon name="mdi-delete-outline" color="negative" />
                    </q-item-section>
                    <q-item-section>Remover</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </q-btn>
            <q-btn
              v-else
              round size="xs" flat dense color="negative"
              @click.stop="callbacks.remove(element.id)"
            >
              <q-icon size="11px" name="mdi-delete-outline" />
              <q-tooltip anchor="center right" self="center left">Remover</q-tooltip>
            </q-btn>
          </div>
        </div>

        <!-- Children (recursive) -->
        <div
          v-if="element.filhos?.length && expanded[element.id] !== false"
          class="norm-children"
        >
          <NormTreeItem
            :elements="element.filhos"
            :selected-id="selectedId"
            @reorder="$emit('reorder')"
          />
        </div>

      </div>
    </template>
  </draggable>
</template>

<script setup>
import { inject } from 'vue'
import draggable from 'vuedraggable'
import { formatLabel, elementIcon } from '@/utils/numbering.js'

defineOptions({ name: 'NormTreeItem' })

defineProps({
  elements:   { type: Array,  required: true },
  selectedId: { type: String, default: null  },
})

defineEmits(['reorder'])

const expanded  = inject('normExpanded')
const callbacks = inject('normCallbacks')

const GROUPING   = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])
const ARTIGO_SET = new Set(['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea'])

const CHILD_MAP = {
  capitulo:           [
    { tipo: 'secao_normativa', label: 'Seção' },
    { tipo: 'artigo',          label: 'Artigo' },
  ],
  secao_normativa:    [
    { tipo: 'subsecao_normativa', label: 'Subseção' },
    { tipo: 'artigo',             label: 'Artigo'   },
  ],
  subsecao_normativa: [{ tipo: 'artigo',     label: 'Artigo'        }],
  artigo:             [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)'   },
    { tipo: 'inciso',          label: 'Inciso'           },
  ],
  paragrafo_unico: [{ tipo: 'inciso',    label: 'Inciso'    }],
  paragrafo:       [{ tipo: 'inciso',    label: 'Inciso'    }],
  inciso:          [{ tipo: 'alinea',    label: 'Alínea'    }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

const isGrouping   = (tipo) => GROUPING.has(tipo)
const canPromote   = (node) => ARTIGO_SET.has(node.tipo)
const childOptions = (node) => CHILD_MAP[node.tipo] ?? []

function toggleExpand(id) {
  // Default to expanded (undefined === expanded), so toggle to false first
  expanded[id] = expanded[id] === false ? true : false
}

function extractNodeText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch { return '' }
}

const isNodeFilled = (node) => extractNodeText(node?.conteudo).length > 0

const nodePreview = (node) => {
  const text = extractNodeText(node?.conteudo)
  return text.length > 28 ? text.slice(0, 28) + '…' : text
}
</script>

<style scoped>
.norm-item {
  display: block;
}

.norm-row {
  min-height: 28px;
  border-radius: 6px;
  cursor: pointer;
  padding: 2px 4px 2px 2px;
  gap: 2px;
  transition: background 0.15s;
}
.norm-row:hover {
  background: rgba(74, 111, 165, 0.08);
}
.norm-row--selected {
  background: rgba(74, 111, 165, 0.16) !important;
}

.expand-btn {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  border-radius: 3px;
}
.expand-btn:hover {
  background: rgba(74, 111, 165, 0.12);
}

.expand-spacer {
  width: 18px;
  flex-shrink: 0;
}

.drag-handle {
  cursor: grab;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.norm-row:hover .drag-handle {
  opacity: 1;
}

.norm-label {
  font-size: 0.78rem;
  line-height: 1.25;
}

.norm-preview {
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  font-size: 0.7rem;
}

.norm-actions {
  opacity: 0;
  transition: opacity 0.15s;
}
.norm-row:hover .norm-actions {
  opacity: 1;
}

.norm-children {
  padding-left: 14px;
}
</style>

<style>
.norm-ghost {
  opacity: 0.4;
  background: rgba(74, 111, 165, 0.12) !important;
  border-radius: 6px;
}
</style>
