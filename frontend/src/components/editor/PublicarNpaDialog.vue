<template>
  <q-dialog :model-value="modelValue" :persistent="enviando" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="min-width:420px;max-width:520px;width:100%">
      <q-card-section class="text-h6">{{ isRevogacao ? 'Revogar NPA' : 'Publicar NPA' }}?</q-card-section>

      <q-card-section class="q-pt-none">
        A NPA <strong>{{ documento?.codigo_documento }}</strong>
        <template v-if="isRevogacao">será <strong>revogada</strong> no Boletim Interno.</template>
        <template v-else>será <strong>publicada</strong> no Boletim Interno e passa a valer.</template>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pt-md q-pb-sm column q-gutter-y-md">
        <div class="text-caption text-grey-7">
          Informe o Boletim Interno {{ isRevogacao ? 'que revoga' : 'que publica' }} esta NPA. Não há portaria nem BCA.
        </div>
        <div class="row q-col-gutter-md">
          <q-input
            v-model="form.numero"
            type="number" min="1" max="9999"
            label="Número do Boletim Interno *"
            outlined dense class="col-6"
            lazy-rules
            :rules="[
              v => (v !== '' && v !== null && v !== undefined) || 'Informe o número',
              v => (v >= 1 && v <= 9999) || 'Deve estar entre 1 e 9999',
            ]"
            :disable="enviando"
          />
          <q-input
            v-model="form.data"
            type="date"
            label="Data *"
            outlined dense class="col-6"
            lazy-rules
            :rules="[
              v => !!v || 'Informe a data',
              v => !isRevogacao || !documento?.data_bca_referencia || v >= documento.data_bca_referencia.slice(0, 10)
                || 'Anterior à data da publicação',
            ]"
            :disable="enviando"
          />
        </div>
        <div class="text-caption text-grey-8">
          Aparecerá como: <em>{{ previa }}</em>
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat :disable="enviando" @click="fechar">Cancelar</q-btn>
        <q-btn
          color="primary" unelevated
          :loading="enviando"
          :disable="erros.length > 0"
          @click="confirmar"
        >
          <q-icon left name="mdi-publish" />
          {{ isRevogacao ? 'Revogar' : 'Publicar' }}
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ehRevogacao } from '@/utils/fluxoDocumento.js'
import { dataPorExtenso } from '@/perfis/npa.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  documento:  { type: Object, default: null },
  enviando:   { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirmar'])

const form = reactive({ numero: '', data: '' })

// Reabre sempre em branco: o Boletim é digitado a cada publicação ou revogação.
watch(() => props.modelValue, (aberto) => {
  if (aberto) Object.assign(form, { numero: '', data: '' })
})

const isRevogacao = computed(() => !!props.documento && ehRevogacao(props.documento))

const previa = computed(() => {
  const numero = form.numero !== '' && form.numero != null ? form.numero : '__'
  return `Boletim Interno Ostensivo nº ${numero}, de ${dataPorExtenso(form.data) ?? '__ de ______ de ____'}`
})

// Espelha PublicacaoDeNpa (backend): número de 1 a 9999; a data da revogação não é anterior à da publicação.
// Datas são strings ISO "YYYY-MM-DD", então a comparação de string já ordena.
const erros = computed(() => {
  const errs = []
  const numero = parseInt(form.numero, 10)
  if (form.numero === '' || isNaN(numero)) errs.push('Informe o número do Boletim Interno.')
  else if (numero < 1 || numero > 9999) errs.push('O número do Boletim Interno deve estar entre 1 e 9999.')
  if (!form.data) errs.push('Informe a data do Boletim Interno.')
  if (isRevogacao.value && form.data && props.documento?.data_bca_referencia
      && form.data < props.documento.data_bca_referencia.slice(0, 10)) {
    errs.push('A data da revogação não pode ser anterior à da publicação.')
  }
  return errs
})

function fechar() {
  if (props.enviando) return
  emit('update:modelValue', false)
}

function confirmar() {
  if (erros.value.length) return
  emit('confirmar', { numeroBoletimInterno: parseInt(form.numero, 10), dataBoletimInterno: form.data })
}
</script>
