<template>
  <!-- Mesmo padrão dos diálogos do projeto (NovoDocumentoDialog, CamposDaNpaDialog): cabeçalho com ícone, título e fechar;
       separadores; rodapé com os botões à direita. -->
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="min-width: 560px; max-width: 680px; width: 100%" data-testid="detalhes-do-documento">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon :name="modulo.icone" color="primary" size="24px" class="q-mr-sm" />
        <span class="text-h6 text-weight-bold">{{ doc?.codigo_documento }}</span>
        <q-space />
        <q-btn v-close-popup icon="mdi-close" size="sm" flat round dense />
      </q-card-section>

      <q-card-section class="q-pt-none">
        <div class="text-body2 text-grey-8 q-mb-sm">{{ doc?.titulo }}</div>
        <StatusBadge :situacao-bca="doc?.situacao_bca" :situacao-local="doc?.situacao_local" />
      </q-card-section>

      <q-separator />

      <template v-if="doc">
        <EtapasDoCiclo :doc="doc" />

        <q-separator />

        <q-list dense>
          <q-item v-for="linha in linhas" :key="linha.rotulo" data-testid="linha-de-detalhe">
            <q-item-section side class="text-grey-7 detalhe-rotulo">{{ linha.rotulo }}</q-item-section>
            <q-item-section>{{ linha.valor }}</q-item-section>
          </q-item>
        </q-list>
      </template>

      <q-separator />

      <!-- Aqui só se consulta: qualquer ação sobre o documento é feita no módulo dele. O botão é só navegação. -->
      <q-card-actions align="right" class="q-pa-md">
        <span class="text-caption text-grey-7 q-mr-auto">Para agir sobre o documento, use o módulo {{ modulo.nome }}.</span>
        <q-btn v-close-popup flat color="primary" label="Ir para o módulo" :to="{ name: modulo.rota }" data-testid="ir-para-o-modulo" />
        <q-btn v-close-popup unelevated color="primary" label="Fechar" />
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
.detalhe-rotulo { min-width: 150px; align-items: flex-start; }
</style>
