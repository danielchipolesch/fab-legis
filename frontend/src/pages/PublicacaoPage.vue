<template>
  <q-page class="q-pa-xl">
    <q-breadcrumbs active-color="primary" style="font-size:13px" class="q-mb-md">
      <template v-slot:separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Publicação" />
    </q-breadcrumbs>

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Publicação</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Documentos atribuídos a você para publicar, revogar ou devolver.
        </p>
      </div>
    </div>

    <q-card flat bordered>
      <q-table
        :rows="documentos"
        :columns="columns"
        row-key="id"
        :loading="carregando"
        :rows-per-page-options="[15, 25, 50]"
        class="legis-table"
      >
        <template #body-cell-autores="props">
          <q-td :props="props">{{ props.row.autores.join(', ') }}</q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props">
            <StatusBadge :status="props.row.status" />
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="text-center">
            <div class="row justify-center no-wrap" style="gap:4px">
              <q-btn
                icon="mdi-eye-outline"
                size="sm" flat round dense color="primary"
                :to="{ name: 'documento-visualizar', params: { id: props.row.id }, query: { origem: 'publicacao' } }"
              >
                <q-tooltip anchor="top middle" self="bottom middle">Abrir (visualizar)</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-publish"
                size="sm" flat round dense color="positive"
                @click="abrirPublicacao(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ props.row.status === 'EM_REVOGACAO' ? 'Revogar' : 'Publicar' }}
                </q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-undo"
                size="sm" flat round dense color="negative"
                @click="devolver(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">Devolver</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>

        <template #no-data>
          <div class="full-width column items-center q-py-xl text-grey-7">
            <q-icon size="56px" class="q-mb-sm" name="mdi-inbox-outline" />
            <p>Nenhum documento atribuído a você no momento.</p>
          </div>
        </template>
      </q-table>
    </q-card>

    <PublicarDialog
      v-model="dialogPublicar"
      :documento="alvo"
      :is-revogacao="alvo?.status === 'EM_REVOGACAO'"
      :is-republicacao="alvo?.status !== 'EM_REVOGACAO' && !!alvo?.data_publicacao"
      :enviando="enviando"
      @confirmar="confirmarPublicacao"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import * as documentosApi from '@/api/documentos.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import PublicarDialog from '@/components/editor/PublicarDialog.vue'

const $q = useQuasar()
const store = useDocumentosStore()

const documentos = ref([])
const carregando = ref(false)

const columns = [
  { name: 'codigo',   label: 'Código',   field: 'codigo_documento', align: 'center', style: 'width: 120px' },
  { name: 'titulo',   label: 'Título',   field: 'titulo',           align: 'center' },
  { name: 'autores',  label: 'Autores',  field: 'autores',          align: 'center' },
  { name: 'status',   label: 'Situação', field: 'status',           align: 'center', style: 'width: 160px' },
  { name: 'actions',  label: 'Ações',    field: 'actions',          align: 'center', style: 'width: 140px' },
]

async function carregar() {
  carregando.value = true
  try {
    documentos.value = await documentosApi.listMinhaPublicacao()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar sua fila de publicação: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}
onMounted(carregar)

// EM_PUBLICACAO resolve pro alvo final PUBLICADO; EM_REVOGACAO resolve pra
// REVOGADO -- ambos exigem o formulário de portaria/BCA (ver PublicarDialog.vue).
function alvoPublicacao(doc) {
  return doc.status === 'EM_REVOGACAO' ? 'REVOGADO' : 'PUBLICADO'
}

function alvoDevolucao(doc) {
  if (doc.status === 'EM_REVOGACAO') return 'PUBLICADO'
  return doc.ja_publicado_antes ? 'EM_ALTERACAO' : 'MINUTA'
}

const dialogPublicar = ref(false)
const alvo = ref(null)
const enviando = ref(false)

// PublicarDialog precisa de mais campos do que a fila enxuta traz (espécie,
// números, datas de referência de portaria/BCA para validação) -- busca o
// documento completo na hora de abrir o formulário.
async function abrirPublicacao(doc) {
  try {
    alvo.value = await documentosApi.getDocumento(doc.id)
    dialogPublicar.value = true
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar documento: ${e?.message ?? 'erro desconhecido'}` })
  }
}

async function confirmarPublicacao(refs) {
  if (!alvo.value || enviando.value) return
  enviando.value = true
  try {
    await store.changeStatus(alvo.value.id, alvoPublicacao(alvo.value), refs)
    dialogPublicar.value = false
    alvo.value = null
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao publicar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

async function devolver(doc) {
  try {
    await store.changeStatus(doc.id, alvoDevolucao(doc))
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao devolver: ${e?.message ?? 'erro desconhecido'}` })
  }
}
</script>
