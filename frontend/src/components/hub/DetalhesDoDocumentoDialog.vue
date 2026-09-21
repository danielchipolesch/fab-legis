<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="width:640px;max-width:95vw" data-testid="detalhes-do-documento">
      <q-card-section class="row items-start no-wrap q-pb-sm">
        <div class="col">
          <div class="row items-center no-wrap" style="gap:8px">
            <q-icon :name="modulo.icone" color="primary" size="20px" />
            <span class="text-subtitle1 text-weight-bold text-primary">{{ doc?.codigo_documento }}</span>
            <StatusBadge :situacao-bca="doc?.situacao_bca" :situacao-local="doc?.situacao_local" size="sm" />
          </div>
          <div class="text-body2 text-grey-8 q-mt-xs">{{ doc?.titulo }}</div>
        </div>
        <q-btn icon="mdi-close" flat round dense size="sm" @click="$emit('update:modelValue', false)" />
      </q-card-section>

      <q-separator />

      <q-card-section v-if="doc" class="q-pt-md">
        <EtapasDoCiclo :doc="doc" />
      </q-card-section>

      <q-separator />

      <q-card-section v-if="doc" class="q-py-md">
        <div v-for="linha in linhas" :key="linha.rotulo" class="row q-py-xs detalhe" data-testid="linha-de-detalhe">
          <div class="col-4 text-caption text-grey-7">{{ linha.rotulo }}</div>
          <div class="col-8 text-body2">{{ linha.valor }}</div>
        </div>
      </q-card-section>

      <q-separator />

      <!-- Aqui só se consulta: qualquer ação sobre o documento é feita no módulo dele. O link é só navegação. -->
      <q-card-actions class="q-pa-md items-center">
        <span class="text-caption text-grey-7 col">Para agir sobre o documento, use o módulo {{ modulo.nome }}.</span>
        <q-btn flat no-caps color="primary" :to="{ name: modulo.rota }" v-close-popup data-testid="ir-para-o-modulo">
          Ir para o módulo
        </q-btn>
        <q-btn unelevated color="primary" no-caps label="Fechar" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EtapasDoCiclo from '@/components/hub/EtapasDoCiclo.vue'
import { moduloDoDocumento } from '@/perfis/index.js'
import { detalhesDoDocumento } from '@/utils/painel.js'
import { listCompartilhamentos } from '@/api/documentos.js'
import { caixaAlta } from '@/utils/texto.js'

// "Exibir detalhes" de uma linha de card do hub: as etapas do ciclo do documento e as informações que o card pede
// (utils/painel.js) -- só leitura. cartao: em_andamento | aguardando | publicados.
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  doc: { type: Object, default: null },
  cartao: { type: String, default: 'em_andamento' },
})
defineEmits(['update:modelValue'])

const modulo = computed(() => moduloDoDocumento(props.doc))
const coautores = ref([])

// Os coautores não vêm na listagem: buscam-se ao abrir.
watch(() => [props.modelValue, props.doc?.id], async ([aberto, id]) => {
  coautores.value = []
  if (!aberto || !id) return
  try {
    const lista = await listCompartilhamentos(id)
    coautores.value = (lista ?? []).map(c => (c.postoGraduacaoBigrama && c.nomeGuerra
      ? `${c.postoGraduacaoBigrama} ${caixaAlta(c.nomeGuerra)}` : caixaAlta(c.nome)))
  } catch {
    coautores.value = []   // sem os coautores o resto dos detalhes continua valendo
  }
})

function formatarData(iso) {
  if (!iso) return '—'
  const [y, m, d] = String(iso).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

const linhas = computed(() => props.doc
  ? detalhesDoDocumento(props.cartao, props.doc, { coautores: coautores.value, formatarData })
  : [])
</script>

<style scoped>
.detalhe + .detalhe { border-top: 1px solid rgba(0, 0, 0, 0.05); }
</style>
