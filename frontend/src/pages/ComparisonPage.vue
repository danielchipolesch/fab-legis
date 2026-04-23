<template>
  <v-container fluid class="pa-6">

    <!-- Header -->
    <div class="d-flex align-center mb-6 gap-3">
      <v-btn :to="{ name: 'home' }" icon="mdi-arrow-left" variant="text" />
      <div>
        <h1 class="text-h5 font-weight-bold text-primary mb-0">Comparação de Versões</h1>
        <p class="text-body-2 text-medium-emphasis mb-0">{{ docLabel }}</p>
      </div>
      <v-spacer />
      <StatusBadge v-if="documento" :status="documento.status" />
    </div>

    <template v-if="!documento">
      <v-alert type="error" title="Documento não encontrado" />
    </template>

    <template v-else>

      <!-- Version selectors -->
      <v-card class="mb-5" color="surface-card">
        <v-card-text class="pa-4">
          <v-row align="center">
            <v-col cols="12" md="4">
              <v-select
                v-model="selectedVersionA"
                :items="versaoOptions"
                item-title="label"
                item-value="id"
                label="Versão A (base)"
                hide-details
                prepend-inner-icon="mdi-tag-outline"
              />
            </v-col>
            <v-col cols="12" md="1" class="d-flex justify-center">
              <v-icon size="28" color="secondary">mdi-swap-horizontal</v-icon>
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="selectedVersionB"
                :items="versaoOptions"
                item-title="label"
                item-value="id"
                label="Versão B (comparar com)"
                hide-details
                prepend-inner-icon="mdi-tag-check-outline"
              />
            </v-col>
            <v-col cols="12" md="3" class="d-flex gap-2">
              <v-btn-toggle v-model="diffMode" mandatory color="primary" density="compact" rounded="md">
                <v-btn value="side"    prepend-icon="mdi-view-column-outline" text="Lado a lado" />
                <v-btn value="unified" prepend-icon="mdi-view-stream-outline" text="Unificado" />
              </v-btn-toggle>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <!-- Summary badges -->
      <div class="d-flex flex-wrap gap-3 mb-5">
        <v-chip color="error"   variant="tonal" size="small" prepend-icon="mdi-minus-circle-outline">
          {{ stats.removed }} remoções
        </v-chip>
        <v-chip color="success" variant="tonal" size="small" prepend-icon="mdi-plus-circle-outline">
          {{ stats.added }} adições
        </v-chip>
        <v-chip color="warning" variant="tonal" size="small" prepend-icon="mdi-pencil-circle-outline">
          {{ stats.modified }} modificações
        </v-chip>
        <v-chip color="grey"    variant="tonal" size="small" prepend-icon="mdi-equal-box">
          {{ stats.unchanged }} sem alteração
        </v-chip>
      </div>

      <!-- Per-section diffs -->
      <template v-for="secao in secoesComDiff" :key="secao.id">
        <v-card class="mb-4">
          <v-card-title class="text-subtitle-1 font-weight-bold px-4 py-3 d-flex align-center">
            <v-icon icon="mdi-folder-outline" color="primary" class="mr-2" size="18" />
            {{ secao.titulo }}
          </v-card-title>
          <v-divider />
          <v-card-text class="pa-4">
            <div v-if="!secao.pares.length" class="text-caption text-medium-emphasis">
              Seção sem elementos.
            </div>
            <DiffViewer
              v-for="par in secao.pares"
              :key="par.id"
              :elemento="par.elementoA"
              :elemento-b="par.elementoB"
              :label-a="labelA"
              :label-b="labelB"
              :mode="diffMode"
            />
          </v-card-text>
        </v-card>
      </template>

      <!-- QUADRO DE JUSTIFICATIVAS -->
      <v-card class="mt-6">
        <v-card-title class="text-subtitle-1 font-weight-bold px-4 py-3 d-flex align-center">
          <v-icon icon="mdi-table-edit" color="primary" class="mr-2" size="18" />
          Quadro de Justificativas das Modificações Propostas
          <v-spacer />
          <v-btn
            size="small"
            variant="outlined"
            color="primary"
            prepend-icon="mdi-file-pdf-box"
            @click="exportarQuadro"
          >
            Exportar
          </v-btn>
        </v-card-title>
        <v-divider />
        <v-card-text class="pa-0">
          <v-table density="compact" class="justificativas-table">
            <thead>
              <tr>
                <th style="width:120px">Referência</th>
                <th>Texto Atual</th>
                <th>Texto Proposto</th>
                <th>Justificativa</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="secao in secoesComDiff" :key="secao.id">
                <tr
                  v-for="par in secao.pares.filter(p => p.hasDiff)"
                  :key="par.id"
                >
                  <td class="text-caption font-weight-bold text-primary">
                    {{ formatLabel(par.elementoA) }}
                  </td>
                  <td class="text-caption" style="max-width:220px">
                    <div class="text-truncate-3" v-html="par.elementoA?.conteudo ?? ''" />
                  </td>
                  <td class="text-caption" style="max-width:220px">
                    <div class="text-truncate-3" v-html="par.elementoB?.conteudo ?? '<em>Elemento removido</em>'" />
                  </td>
                  <td>
                    <v-textarea
                      v-model="justificativas[par.id]"
                      density="compact"
                      variant="plain"
                      rows="2"
                      auto-grow
                      hide-details
                      placeholder="Informe a justificativa…"
                      class="text-caption"
                    />
                  </td>
                </tr>
                <tr v-if="!secao.pares.filter(p => p.hasDiff).length">
                  <td colspan="4" class="text-caption text-medium-emphasis text-center py-2">
                    Sem modificações em {{ secao.titulo }}
                  </td>
                </tr>
              </template>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>

    </template>
  </v-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import DiffViewer from '@/components/comparison/DiffViewer.vue'
import { formatLabel } from '@/utils/numbering.js'
import { diffWords } from 'diff'

const route = useRoute()
const store = useDocumentsStore()

const documento = computed(() => store.getById(route.params.id))

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return ''
  return [d.especie, d.numero_basico, d.numero_secundario].filter(Boolean).join(' ')
    + (d.assunto_basico ? ` — ${d.assunto_basico}` : '')
})

// Build version list: current + snapshots
const versaoOptions = computed(() => {
  const d = documento.value
  if (!d) return []
  const opts = [{ id: '__atual__', label: `Versão atual (${d.status})`, secoes: d.secoes }]
  for (const v of (d.versoes ?? []).slice().reverse()) {
    opts.push({ id: v.versao_id, label: `${v.status} — ${v.data_snapshot?.slice(0, 10) ?? ''}`, secoes: v.secoes })
  }
  return opts
})

// Default: latest published vs. current
const defaultA = computed(() => {
  const pub = versaoOptions.value.find(v => v.label?.includes('PUBLICADO'))
  return pub?.id ?? versaoOptions.value[1]?.id ?? versaoOptions.value[0]?.id
})
const defaultB = '__atual__'

const selectedVersionA = ref(null)
const selectedVersionB = ref(defaultB)

watch(versaoOptions, (opts) => {
  if (!selectedVersionA.value && opts.length >= 2) {
    selectedVersionA.value = defaultA.value
  }
}, { immediate: true })

const diffMode = ref('side')
const justificativas = ref({})

function getSecoes(versionId) {
  const opt = versaoOptions.value.find(v => v.id === versionId)
  return opt?.secoes ?? documento.value?.secoes ?? []
}

function stripHtml(html) {
  const div = document.createElement('div')
  div.innerHTML = html ?? ''
  return div.textContent || ''
}

function hasDiff(a, b) {
  return stripHtml(a?.conteudo) !== stripHtml(b?.conteudo)
}

function flatElements(elementos, acc = []) {
  for (const el of (elementos ?? [])) {
    acc.push(el)
    flatElements(el.filhos, acc)
  }
  return acc
}

const secoesComDiff = computed(() => {
  const secoesA = getSecoes(selectedVersionA.value)
  const secoesB = getSecoes(selectedVersionB.value)

  return secoesA.map(secA => {
    const secB = secoesB.find(s => s.tipo === secA.tipo) ?? { elementos: [] }
    const elA = flatElements(secA.elementos)
    const elB = flatElements(secB.elementos)

    const pares = []
    const maxLen = Math.max(elA.length, elB.length)
    for (let i = 0; i < maxLen; i++) {
      const eA = elA[i] ?? null
      const eB = elB[i] ?? null
      pares.push({
        id: eA?.id ?? eB?.id ?? `par-${i}`,
        elementoA: eA,
        elementoB: eB,
        hasDiff: hasDiff(eA, eB),
      })
    }

    return { id: secA.id, titulo: secA.titulo, tipo: secA.tipo, pares }
  })
})

const stats = computed(() => {
  let added = 0, removed = 0, modified = 0, unchanged = 0
  for (const sec of secoesComDiff.value) {
    for (const par of sec.pares) {
      if (!par.elementoA) added++
      else if (!par.elementoB) removed++
      else if (par.hasDiff) modified++
      else unchanged++
    }
  }
  return { added, removed, modified, unchanged }
})

const labelA = computed(() => versaoOptions.value.find(v => v.id === selectedVersionA.value)?.label ?? 'Versão A')
const labelB = computed(() => versaoOptions.value.find(v => v.id === selectedVersionB.value)?.label ?? 'Versão B')

function exportarQuadro() {
  // Future: export to PDF/DOCX
  window.print()
}
</script>

<style scoped>
.text-truncate-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.justificativas-table th {
  background: rgba(var(--v-theme-primary), 0.08) !important;
  color: rgb(var(--v-theme-primary)) !important;
  font-weight: 700 !important;
  font-size: 0.78rem !important;
}
.justificativas-table td {
  vertical-align: top;
  padding-top: 8px !important;
  padding-bottom: 8px !important;
  border-bottom: 1px solid rgba(var(--v-border-color), 0.2) !important;
}
</style>
