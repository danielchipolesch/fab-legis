<template>
  <q-page class="q-pa-xl">

    <div class="q-mb-lg">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Distintivos das OMs</h1>
      <p class="text-body2 text-grey-7 q-mb-none">
        O <strong>DOM</strong> (distintivo da organização militar) é a imagem que a NPA mostra no cabeçalho. Cada NPA usa o
        distintivo da OM do próprio documento — uma NPA clonada mostra o da OM de quem clonou. PNG ou JPEG, até 1 MB.
      </p>
    </div>

    <q-card flat bordered>
      <q-card-section class="row items-center q-col-gutter-md">
        <div class="col-12 col-sm-6">
          <q-input v-model="busca" dense outlined clearable label="Buscar OM por sigla ou nome" data-testid="busca-om">
            <template #prepend><q-icon name="mdi-magnify" /></template>
          </q-input>
        </div>
        <div class="col-12 col-sm-auto">
          <q-toggle v-model="soComDistintivo" label="Só as OMs com distintivo" data-testid="so-com-distintivo" />
        </div>
      </q-card-section>

      <q-table
        :rows="linhas"
        :columns="colunas"
        row-key="id"
        :loading="carregando"
        :rows-per-page-options="[15, 25, 50]"
        :pagination="{ rowsPerPage: 15, sortBy: 'sigla' }"
        no-data-label="Nenhuma OM encontrada."
        flat
      >
        <template #body-cell-distintivo="props">
          <q-td :props="props" style="width: 120px">
            <img v-if="imagens[props.row.urlDistintivo]" :src="imagens[props.row.urlDistintivo]" alt="Distintivo"
                 class="miniatura" :data-testid="`distintivo-${props.row.sigla}`" />
            <span v-else class="text-grey-6">—</span>
          </q-td>
        </template>
        <template #body-cell-acoes="props">
          <q-td :props="props" class="text-right">
            <q-btn flat dense color="primary" icon="mdi-upload" :label="props.row.urlDistintivo ? 'Trocar' : 'Enviar'"
                   :loading="salvando === props.row.id" :data-testid="`enviar-${props.row.sigla}`"
                   @click="escolher(props.row)" />
            <q-btn v-if="props.row.urlDistintivo" flat dense color="negative" icon="mdi-delete-outline" label="Remover"
                   :disable="salvando === props.row.id" :data-testid="`remover-${props.row.sigla}`"
                   @click="remover(props.row)" />
          </q-td>
        </template>
      </q-table>
    </q-card>

    <input ref="seletor" type="file" accept="image/png,image/jpeg" class="hidden" @change="aoEscolher" />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { listOrganizacoesMilitares } from '@/api/usuarios.js'
import { definirDistintivo, removerDistintivo } from '@/api/distintivos.js'
import { resolveMinioUrls } from '@/utils/minioUrls.js'
import { erroDoDistintivo, filtrarOmsDoDistintivo } from '@/utils/distintivo.js'

const $q = useQuasar()

const oms = ref([])
const imagens = ref({})
const carregando = ref(false)
const salvando = ref(null)
const busca = ref('')
const soComDistintivo = ref(false)
const seletor = ref(null)
let omEscolhida = null

const colunas = [
  { name: 'sigla', label: 'Sigla', field: 'sigla', align: 'left', sortable: true, style: 'width: 140px' },
  { name: 'nome', label: 'Organização Militar', field: 'nome', align: 'left', sortable: true },
  { name: 'distintivo', label: 'DOM', field: 'urlDistintivo', align: 'left' },
  { name: 'acoes', label: '', field: 'id', align: 'right' },
]

const linhas = computed(() => filtrarOmsDoDistintivo(oms.value, busca.value, soComDistintivo.value))

async function resolverImagens() {
  const urls = oms.value.map(o => o.urlDistintivo).filter(Boolean)
  if (!urls.length) return
  try {
    imagens.value = { ...imagens.value, ...Object.fromEntries(await resolveMinioUrls(urls)) }
  } catch {
    // sem miniatura: a lista continua utilizável
  }
}

async function carregar() {
  carregando.value = true
  try {
    oms.value = await listOrganizacoesMilitares()
    await resolverImagens()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar as OMs: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

function escolher(om) {
  omEscolhida = om
  seletor.value?.click()
}

function atualizar(salva) {
  const i = oms.value.findIndex(o => o.id === salva.id)
  if (i >= 0) oms.value.splice(i, 1, salva)
}

async function aoEscolher(event) {
  const arquivo = event.target.files?.[0]
  event.target.value = ''
  const om = omEscolhida
  if (!arquivo || !om) return
  const erro = erroDoDistintivo(arquivo)
  if (erro) {
    $q.notify({ type: 'negative', message: erro })
    return
  }
  salvando.value = om.id
  try {
    atualizar(await definirDistintivo(om.id, arquivo))
    await resolverImagens()
    $q.notify({ type: 'positive', message: `${om.sigla}: distintivo atualizado.` })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Não foi possível salvar o distintivo de ${om.sigla}: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = null
  }
}

async function remover(om) {
  salvando.value = om.id
  try {
    atualizar(await removerDistintivo(om.id))
    $q.notify({ type: 'positive', message: `${om.sigla}: distintivo removido.` })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Não foi possível remover o distintivo de ${om.sigla}: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = null
  }
}

onMounted(carregar)
</script>

<style scoped>
.miniatura { max-width: 64px; max-height: 40px; object-fit: contain; display: block; }
</style>
