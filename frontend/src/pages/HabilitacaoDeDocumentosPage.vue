<template>
  <q-page class="q-pa-xl">

    <div class="q-mb-lg">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Habilitação de Documentos</h1>
      <p class="text-body2 text-grey-7 q-mb-none">
        Para cada espécie normativa convencional, as organizações militares que podem <strong>criá-la</strong>. As demais pessoas
        continuam consultando todos os documentos e podem ser coautoras; a NPA é criada por qualquer OM.
      </p>
    </div>

    <q-card flat bordered>
      <q-card-section class="row items-center q-col-gutter-md">
        <div class="col-12 col-sm-6">
          <!-- Filtro por OM: marca, em cada espécie, se a OM escolhida a cria (a pergunta inversa: "o que a OM X pode criar?"). -->
          <q-select
            v-model="omDoFiltro"
            :options="opcoesDoFiltro"
            option-value="id" option-label="sigla" emit-value map-options
            use-input input-debounce="0" clearable dense outlined
            label="Ver o que uma OM pode criar"
            data-testid="filtro-om"
            @filter="filtrarFiltro"
          >
            <template #prepend><q-icon name="mdi-magnify" /></template>
          </q-select>
        </div>
        <div v-if="omDoFiltro != null" class="col-12 col-sm text-body2" data-testid="resumo-do-filtro">
          <strong>{{ siglaDoFiltro }}</strong> cria {{ especiesDoFiltro.length }} de {{ especies.length }} espécies convencionais.
        </div>
      </q-card-section>

      <q-linear-progress v-if="carregando" indeterminate />

      <q-list separator>
        <q-item v-for="e in especies" :key="e.id" class="q-py-md" :class="{ 'marcada': omDoFiltro != null && criaNaEspecie(e) }">
          <q-item-section side top style="width: 120px">
            <div class="text-weight-bold text-primary text-subtitle1">{{ e.sigla }}</div>
            <div class="text-caption text-grey-7">{{ e.nome }}</div>
          </q-item-section>

          <q-item-section>
            <!-- As OMs habilitadas, só pela sigla. Cada mudança é gravada na hora (e desfeita, com aviso, se o servidor recusar). -->
            <q-select
              :model-value="omIdsDa(e)"
              :options="opcoesDe(e)"
              option-value="id" option-label="sigla" emit-value map-options
              multiple use-chips use-input input-debounce="0" dense outlined options-dense
              :loading="salvando === e.id" :disable="salvando === e.id"
              :label="omIdsDa(e).length ? undefined : 'Nenhuma OM — pesquise para adicionar'"
              :data-testid="`habilitacao-${e.sigla}`"
              @filter="(val, update) => filtrarOpcoes(e, val, update)"
              @update:model-value="ids => alterar(e, ids)"
            >
              <template #before-options>
                <q-item clickable dense :disable="!omIdsDa(e).length" @click="alterar(e, [])">
                  <q-item-section>Remover todas as OMs</q-item-section>
                </q-item>
                <q-separator />
              </template>
              <template #selected-item="scope">
                <q-chip removable dense :tabindex="scope.tabindex"
                        :color="omDoFiltro === idDe(scope.opt) ? 'primary' : undefined"
                        :text-color="omDoFiltro === idDe(scope.opt) ? 'white' : undefined"
                        @remove="scope.removeAtIndex(scope.index)">
                  {{ siglaDe(scope.opt) }}
                </q-chip>
              </template>
              <template #no-option><q-item><q-item-section class="text-grey">Nenhuma OM encontrada</q-item-section></q-item></template>
            </q-select>
          </q-item-section>
        </q-item>

        <q-item v-if="!carregando && !especies.length">
          <q-item-section class="text-grey-7">Nenhuma espécie convencional cadastrada.</q-item-section>
        </q-item>
      </q-list>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { listHabilitacoes, definirHabilitacaoDaEspecie } from '@/api/habilitacao.js'
import { listEspeciesNormativas, normalizeEspecie } from '@/api/referencias.js'
import { omIdsPorEspecie, filtrarOpcoesDeOm, especiesDaOm } from '@/utils/habilitacao.js'

const $q = useQuasar()

const oms = ref([])                  // [{ id, sigla, nome }]
const especies = ref([])             // só as convencionais
const habilitadas = ref({})          // { [especieId]: [omId] }
const carregando = ref(false)
const salvando = ref(null)
const omDoFiltro = ref(null)
const opcoesDoFiltro = ref([])
const opcoesPorEspecie = ref({})     // { [especieId]: OMs que combinam com o que foi digitado }

// A opção selecionada pode vir como a OM ou só como o id (quando a busca a tirou da lista de opções): os dois casos mostram a sigla.
const idDe = (opt) => (opt !== null && typeof opt === 'object' ? opt.id : opt)
const siglaDe = (opt) => oms.value.find(o => o.id === idDe(opt))?.sigla ?? ''
const omIdsDa = (e) => habilitadas.value[e.id] ?? []
const criaNaEspecie = (e) => omIdsDa(e).includes(omDoFiltro.value)
const siglaDoFiltro = computed(() => oms.value.find(o => o.id === omDoFiltro.value)?.sigla ?? '')
const especiesDoFiltro = computed(() => especiesDaOm(especies.value, habilitadas.value, omDoFiltro.value))

// As opções são sempre as OMs (as já escolhidas continuam na lista, para o seletor mostrar a sigla delas).
const opcoesDe = (e) => opcoesPorEspecie.value[e.id] ?? oms.value

function filtrarOpcoes(e, valor, update) {
  update(() => { opcoesPorEspecie.value = { ...opcoesPorEspecie.value, [e.id]: filtrarOpcoesDeOm(oms.value, valor) } })
}

function filtrarFiltro(valor, update) {
  update(() => { opcoesDoFiltro.value = filtrarOpcoesDeOm(oms.value, valor) })
}

async function carregar() {
  carregando.value = true
  try {
    const [porOm, todas] = await Promise.all([listHabilitacoes(), listEspeciesNormativas()])
    oms.value = porOm.map(h => ({ id: h.omId, sigla: h.sigla, nome: h.nome })).sort((a, b) => a.sigla.localeCompare(b.sigla))
    opcoesDoFiltro.value = oms.value
    habilitadas.value = omIdsPorEspecie(porOm)
    // Só as convencionais dependem de habilitação (a NPA é de toda OM): são as únicas que o administrador escolhe aqui.
    especies.value = todas.map(normalizeEspecie).filter(e => e.tipoDeEspecie === 'CONVENCIONAL')
      .sort((a, b) => a.sigla.localeCompare(b.sigla))
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar as habilitações: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

async function alterar(especie, novosIds) {
  const anteriores = omIdsDa(especie)
  habilitadas.value = { ...habilitadas.value, [especie.id]: novosIds }
  salvando.value = especie.id
  try {
    const salvo = await definirHabilitacaoDaEspecie(especie.id, novosIds)
    habilitadas.value = { ...habilitadas.value, [especie.id]: salvo.omIds }
    $q.notify({ type: 'positive', message: `${especie.sigla}: habilitação atualizada.` })
  } catch (e) {
    habilitadas.value = { ...habilitadas.value, [especie.id]: anteriores }
    $q.notify({ type: 'negative', message: `Não foi possível salvar a habilitação de ${especie.sigla}: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = null
  }
}

onMounted(carregar)
</script>

<style scoped>
.marcada { background: rgba(25, 118, 210, 0.06); }
</style>
