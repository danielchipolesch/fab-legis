<template>
  <q-page class="q-pa-xl">

    <!-- Header -->
    <div class="row items-center q-mb-xl" style="gap:12px">
      <q-btn :to="{ name: 'home' }" icon="mdi-arrow-left" flat round dense />
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Comparação de Versões</h1>
        <p class="text-body2 text-grey-7 q-mb-none">{{ docLabel }}</p>
      </div>
      <q-space />
      <StatusBadge v-if="documento" :status="documento.status" />
    </div>

    <template v-if="!documento">
      <q-banner class="bg-negative text-white" rounded>
        <template #avatar>
          <q-icon name="mdi-alert-circle-outline" color="white" />
        </template>
        Documento não encontrado
      </q-banner>
    </template>

    <template v-else>

      <!-- Version selectors -->
      <q-card flat bordered class="q-mb-lg">
        <q-card-section class="q-pa-md">
          <div class="row items-center q-col-gutter-md">
            <div class="col-12 col-md-4">
              <q-select
                v-model="selectedVersionA"
                :options="versaoOptions"
                option-label="label"
                option-value="id"
                emit-value
                map-options
                label="Versão A (base)"
                outlined
                dense
                hide-bottom-space
              >
                <template #prepend>
                  <q-icon name="mdi-tag-outline" />
                </template>
              </q-select>
            </div>
            <div class="col-12 col-md-1 row justify-center">
              <q-icon size="28px" color="secondary" name="mdi-swap-horizontal" />
            </div>
            <div class="col-12 col-md-4">
              <q-select
                v-model="selectedVersionB"
                :options="versaoOptions"
                option-label="label"
                option-value="id"
                emit-value
                map-options
                label="Versão B (comparar com)"
                outlined
                dense
                hide-bottom-space
              >
                <template #prepend>
                  <q-icon name="mdi-tag-check-outline" />
                </template>
              </q-select>
            </div>
            <div class="col-12 col-md-3 row justify-center">
              <q-btn-toggle
                v-model="diffMode"
                no-caps
                unelevated
                toggle-color="primary"
                color="grey-3"
                text-color="grey-8"
                :options="[
                  { value: 'side',    label: 'Lado a lado', icon: 'mdi-view-column-outline' },
                  { value: 'unified', label: 'Unificado',   icon: 'mdi-view-stream-outline' },
                ]"
              />
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Summary badges -->
      <div class="row q-gutter-sm q-mb-lg">
        <q-chip color="red-2" text-color="red-10" size="sm" square icon="mdi-minus-circle-outline">
          {{ stats.removed }} remoções
        </q-chip>
        <q-chip color="green-2" text-color="green-10" size="sm" square icon="mdi-plus-circle-outline">
          {{ stats.added }} adições
        </q-chip>
        <q-chip color="orange-2" text-color="orange-10" size="sm" square icon="mdi-pencil-circle-outline">
          {{ stats.modified }} modificações
        </q-chip>
        <q-chip color="grey-3" text-color="grey-9" size="sm" square icon="mdi-equal-box">
          {{ stats.unchanged }} sem alteração
        </q-chip>
      </div>

      <!-- Per-section diffs -->
      <template v-for="secao in secoesComDiff" :key="secao.id">
        <q-card flat bordered class="q-mb-md">
          <q-card-section class="text-subtitle1 text-weight-bold q-px-md q-py-sm row items-center">
            <q-icon name="mdi-folder-outline" color="amber-8" class="q-mr-sm" size="18px" />
            {{ secao.titulo }}
          </q-card-section>
          <q-separator />
          <q-card-section class="q-pa-md">
            <div v-if="!secao.pares.length" class="text-caption text-grey-7">
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
          </q-card-section>
        </q-card>
      </template>

      <!-- QUADRO DE JUSTIFICATIVAS -->
      <q-card flat bordered class="q-mt-xl">
        <q-card-section class="text-subtitle1 text-weight-bold q-px-md q-py-sm row items-center">
          <q-icon name="mdi-table-edit" color="primary" class="q-mr-sm" size="18px" />
          Quadro de Justificativas das Modificações Propostas
          <q-space />
          <q-btn
            size="sm"
            outline
            color="primary"
            @click="exportarQuadro"
          >
            <q-icon left name="mdi-file-pdf-box" />
            Exportar
          </q-btn>
        </q-card-section>
        <q-separator />
        <q-card-section class="q-pa-none">
          <q-markup-table flat dense class="justificativas-table">
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
                  <td class="text-caption text-weight-bold text-primary">
                    {{ formatLabel(par.elementoA) }}
                  </td>
                  <td class="text-caption" style="max-width:220px">
                    <div class="text-truncate-3" v-html="conteudoToHtml(par.elementoA?.conteudo)" />
                  </td>
                  <td class="text-caption" style="max-width:220px">
                    <div class="text-truncate-3" v-html="par.elementoB ? conteudoToHtml(par.elementoB.conteudo) : '<em>Elemento removido</em>'" />
                  </td>
                  <td>
                    <q-input
                      v-model="justificativas[par.id]"
                      type="textarea"
                      borderless
                      dense
                      autogrow
                      hide-bottom-space
                      placeholder="Informe a justificativa…"
                      class="text-caption"
                    />
                  </td>
                </tr>
                <tr v-if="!secao.pares.filter(p => p.hasDiff).length">
                  <td colspan="4" class="text-caption text-grey-7 text-center q-py-sm">
                    Sem modificações em {{ secao.titulo }}
                  </td>
                </tr>
              </template>
            </tbody>
          </q-markup-table>
        </q-card-section>
      </q-card>

    </template>
  </q-page>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import DiffViewer from '@/components/comparison/DiffViewer.vue'
import { formatLabel } from '@/utils/numbering.js'
import { diffWords } from 'diff'
import { generateHTML } from '@tiptap/html'
import { editorExtensions } from '@/editor/extensions.js'

function conteudoToHtml(conteudo) {
  if (!conteudo) return ''
  try { return generateHTML(JSON.parse(conteudo), editorExtensions) } catch { return '' }
}

function extractText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.type === 'hardBreak') return '\n'
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch { return '' }
}

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

function hasDiff(a, b) {
  return extractText(a?.conteudo) !== extractText(b?.conteudo)
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
  background: rgba(11, 61, 145, 0.08) !important;
  color: #0B3D91 !important;
  font-weight: 700 !important;
  font-size: 0.78rem !important;
  text-align: left;
}
.justificativas-table td {
  vertical-align: top;
  padding-top: 8px !important;
  padding-bottom: 8px !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.2) !important;
}
</style>
