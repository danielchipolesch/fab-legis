<template>
  <q-card flat bordered class="painel-card column" :data-testid="`cartao-${cartao}`">
    <q-card-section class="row items-center no-wrap q-py-sm">
      <span class="text-subtitle2 text-weight-bold ellipsis">{{ titulo }}</span>
      <span class="text-subtitle2 text-weight-bold q-ml-xs" data-testid="total">({{ total }})</span>
      <q-space />
      <q-btn flat round dense size="sm" icon="mdi-refresh" :loading="carregando" @click="carregar">
        <q-tooltip>Atualizar</q-tooltip>
      </q-btn>
    </q-card-section>
    <q-separator />

    <div class="linhas col">
      <div
        v-for="doc in documentos"
        :key="doc.id"
        class="linha q-px-md q-py-sm"
        data-testid="linha"
      >
        <div class="row items-center no-wrap" style="gap:8px">
          <q-icon :name="moduloDe(doc.tipo_de_especie).icone" color="primary" size="18px">
            <q-tooltip>{{ moduloDe(doc.tipo_de_especie).nome }}</q-tooltip>
          </q-icon>
          <span class="text-weight-bold text-primary identificacao">{{ doc.codigo_documento }}</span>
          <span class="titulo text-grey-8 ellipsis col">{{ doc.titulo }}</span>
          <StatusBadge :situacao-bca="doc.situacao_bca" :situacao-local="doc.situacao_local" :mostrar="cartao === 'publicados' ? 'bca' : 'local'" size="xs" />
          <q-btn flat round dense size="sm" icon="mdi-dots-vertical" color="grey-8" data-testid="exibir-detalhes" @click="$emit('detalhes', doc)">
            <q-tooltip>Exibir detalhes</q-tooltip>
          </q-btn>
        </div>
        <div class="row items-center no-wrap q-mt-xs" style="gap:6px">
          <q-icon v-if="acao(doc).pendente" name="mdi-alert" color="warning" size="18px" data-testid="pendente" />
          <router-link :to="acao(doc).rota" class="acao" data-testid="acao-da-linha">{{ acao(doc).rotulo }}</router-link>
          <q-space />
          <span v-if="cartao === 'publicados'" class="text-caption text-grey-7 ellipsis" data-testid="om-da-linha">{{ doc.om_nome }}</span>
        </div>
      </div>

      <div v-if="!documentos.length && !carregando" class="vazio column items-center justify-center text-grey-7 q-pa-lg" data-testid="vazio">
        <q-icon name="mdi-inbox-outline" size="36px" class="q-mb-xs" />
        <span>{{ vazio }}</span>
      </div>
    </div>

    <q-separator />
    <div class="row justify-center q-py-sm rodape">
      <q-pagination v-if="totalPaginas > 1" v-model="pagina" :max="totalPaginas" :max-pages="5" size="sm" direction-links boundary-links flat color="primary" active-color="primary" @update:model-value="carregar" />
    </div>
  </q-card>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { listarCartao } from '@/api/painel.js'
import { acaoDaLinha } from '@/utils/painel.js'
import { moduloDe } from '@/perfis/index.js'
import { useDocumentosStore } from '@/stores/documentos.js'

// Um dos três cards do hub (utils/painel.js): carrega a própria página, do backend, e mostra cada documento com o módulo
// dele, a situação e a ação a seguir (um link -- nada é executado aqui). O ⋮ só pede ao hub que mostre os detalhes.
const props = defineProps({
  cartao: { type: String, required: true },     // em_andamento | aguardando | publicados
  titulo: { type: String, required: true },
  vazio:  { type: String, default: 'Nenhum documento.' },
})
defineEmits(['detalhes'])

const $q = useQuasar()
const docStore = useDocumentosStore()

const TAMANHO_DA_PAGINA = 8
const documentos = ref([])
const total = ref(0)
const pagina = ref(1)
const carregando = ref(false)

const totalPaginas = computed(() => Math.max(1, Math.ceil(total.value / TAMANHO_DA_PAGINA)))
const acao = (doc) => acaoDaLinha(props.cartao, doc)

async function carregar() {
  carregando.value = true
  try {
    const { items, totalElements } = await listarCartao(props.cartao, { page: pagina.value - 1, size: TAMANHO_DA_PAGINA })
    documentos.value = items
    total.value = totalElements
    // Última página esvaziada (documentos saíram do card): volta para a anterior.
    if (!items.length && pagina.value > 1) { pagina.value--; await carregar() }
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar "${props.titulo}": ${e?.message ?? 'erro desconhecido'}`, position: 'bottom-right' })
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)
// Algo fora da tela (ex.: te adicionaram como coautor) mudou o que este card mostra.
watch(() => docStore.refreshSignal, carregar)

defineExpose({ carregar })
</script>

<style scoped>
.painel-card { height: 100%; min-height: 520px; }
.linhas { overflow-y: auto; }
.linha { border-bottom: 1px solid rgba(0, 0, 0, 0.08); }
.linha:hover { background: rgba(74, 111, 165, 0.05); }
.identificacao { white-space: nowrap; font-size: 13px; }
.titulo { font-size: 13px; min-width: 0; }
.acao { font-size: 13px; color: #0b57d0; text-decoration: underline; }
.vazio { min-height: 200px; }
.rodape { min-height: 44px; }
</style>
