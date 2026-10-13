<template>
  <q-page class="q-pa-xl">

    <div class="q-mb-lg">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Habilitação de Documentos</h1>
      <p class="text-body2 text-grey-7 q-mb-none">
        Quais espécies normativas convencionais cada organização militar pode <strong>criar</strong>. As demais pessoas continuam
        consultando todos os documentos e podem ser coautoras; a NPA é criada por qualquer OM.
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
          <q-toggle v-model="soHabilitadas" label="Só as OMs habilitadas" data-testid="so-habilitadas" />
        </div>
      </q-card-section>

      <q-table
        :rows="linhas"
        :columns="colunas"
        row-key="omId"
        :loading="carregando"
        :rows-per-page-options="[15, 25, 50]"
        :pagination="{ rowsPerPage: 15, sortBy: 'sigla' }"
        no-data-label="Nenhuma OM encontrada."
        flat
      >
        <template #body-cell-especies="props">
          <q-td :props="props" style="min-width: 360px">
            <!-- Uma lista de escolha por OM: cada mudança é gravada na hora (e desfeita, com aviso, se o servidor recusar). -->
            <q-select
              :model-value="props.row.especieIds"
              :options="especies"
              option-value="id"
              option-label="sigla"
              emit-value
              map-options
              multiple
              use-chips
              dense
              outlined
              options-dense
              :loading="salvando === props.row.omId"
              :disable="salvando === props.row.omId"
              :label="props.row.especieIds.length ? undefined : 'Nenhuma espécie'"
              :data-testid="`habilitacao-${props.row.sigla}`"
              @update:model-value="ids => alterar(props.row, ids)"
            >
              <template #before-options>
                <q-item clickable dense @click="alterar(props.row, especies.map(e => e.id))">
                  <q-item-section>Habilitar todas</q-item-section>
                </q-item>
                <q-item clickable dense @click="alterar(props.row, [])">
                  <q-item-section>Remover todas</q-item-section>
                </q-item>
                <q-separator />
              </template>
            </q-select>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { listHabilitacoes, definirHabilitacao } from '@/api/habilitacao.js'
import { listEspeciesNormativas, normalizeEspecie } from '@/api/referencias.js'
import { filtrarOms } from '@/utils/habilitacao.js'

const $q = useQuasar()

const oms = ref([])
const especies = ref([])
const carregando = ref(false)
const salvando = ref(null)
const busca = ref('')
const soHabilitadas = ref(false)

const colunas = [
  { name: 'sigla', label: 'Sigla', field: 'sigla', align: 'left', sortable: true, style: 'width: 140px' },
  { name: 'nome', label: 'Organização Militar', field: 'nome', align: 'left', sortable: true },
  { name: 'especies', label: 'Pode criar', field: 'especieIds', align: 'left' },
]

const linhas = computed(() => filtrarOms(oms.value, busca.value, soHabilitadas.value))

async function carregar() {
  carregando.value = true
  try {
    const [habilitacoes, todas] = await Promise.all([listHabilitacoes(), listEspeciesNormativas()])
    oms.value = habilitacoes
    // Só as convencionais dependem de habilitação (a NPA é de toda OM): são as únicas que o administrador escolhe aqui.
    especies.value = todas.map(normalizeEspecie).filter(e => e.tipoDeEspecie === 'CONVENCIONAL')
      .sort((a, b) => a.sigla.localeCompare(b.sigla))
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar as habilitações: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

async function alterar(linha, novosIds) {
  const anteriores = linha.especieIds
  salvando.value = linha.omId
  try {
    const salvo = await definirHabilitacao(linha.omId, novosIds)
    linha.especieIds = salvo.especieIds
    $q.notify({ type: 'positive', message: `${linha.sigla}: habilitação atualizada.` })
  } catch (e) {
    linha.especieIds = anteriores
    $q.notify({ type: 'negative', message: `Não foi possível salvar a habilitação de ${linha.sigla}: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = null
  }
}

onMounted(carregar)
</script>
