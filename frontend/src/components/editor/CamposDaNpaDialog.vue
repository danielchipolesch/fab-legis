<template>
  <q-dialog :model-value="modelValue" :persistent="salvando" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="min-width:560px;max-width:680px;width:100%">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="mdi-table-headers-eye" color="primary" size="24px" class="q-mr-sm" />
        <span class="text-h6 text-weight-bold">Cabeçalho e assinaturas</span>
        <q-space />
        <q-btn icon="mdi-close" size="sm" flat round dense :disable="salvando" @click="fechar" />
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pt-md column q-gutter-y-md" style="max-height:65vh;overflow-y:auto">
        <p class="text-caption text-grey-7 q-mb-none">
          A identificação, o assunto e a data da NPA vêm do documento; a distribuição é sempre ostensiva. Aqui ficam o
          setor que emite a NPA, o local do fecho e as assinaturas, em texto livre.
        </p>

        <q-input
          v-model="form.setorEmissor"
          label="Setor emissor *"
          hint="Aparece abaixo do nome da OM, no cabeçalho"
          outlined dense maxlength="255"
          :readonly="somenteLeitura"
        />
        <q-input
          v-model="form.local"
          label="Local do fecho *"
          hint="Aparece como “Local, dd de mês de aaaa” — a data é a da aprovação"
          outlined dense maxlength="120"
          :readonly="somenteLeitura"
        />

        <div class="row items-center">
          <span class="text-subtitle2 text-weight-bold">Assinaturas</span>
          <q-space />
          <q-btn
            v-if="!somenteLeitura"
            flat dense color="primary" size="sm"
            :disable="form.assinaturas.length >= LIMITE_DE_BLOCOS"
            @click="adicionarBloco"
          >
            <q-icon left name="mdi-plus" /> Bloco de assinatura
          </q-btn>
        </div>

        <div v-if="!form.assinaturas.length" class="text-caption text-grey-6">Sem assinaturas.</div>

        <q-card v-for="(bloco, i) in form.assinaturas" :key="i" flat bordered class="q-pa-sm">
          <div class="row items-center q-col-gutter-sm">
            <q-input
              v-model="bloco.rotulo"
              label="Rótulo"
              hint="Ex.: Elaborado por, Visto, Proposto por, Aprovo"
              outlined dense class="col" maxlength="60"
              :readonly="somenteLeitura"
            />
            <div v-if="!somenteLeitura" class="col-auto row no-wrap">
              <q-btn flat round dense size="sm" icon="mdi-arrow-up" :disable="i === 0" @click="mover(i, -1)">
                <q-tooltip>Subir</q-tooltip>
              </q-btn>
              <q-btn flat round dense size="sm" icon="mdi-arrow-down" :disable="i === form.assinaturas.length - 1" @click="mover(i, 1)">
                <q-tooltip>Descer</q-tooltip>
              </q-btn>
              <q-btn flat round dense size="sm" color="negative" icon="mdi-delete-outline" @click="removerBloco(i)">
                <q-tooltip>Remover bloco</q-tooltip>
              </q-btn>
            </div>
          </div>
          <div v-for="(_, j) in bloco.linhas" :key="j" class="row items-center no-wrap q-mt-xs">
            <q-input
              v-model="bloco.linhas[j]"
              :label="j === 0 ? 'Linha (nome, em negrito)' : 'Linha'"
              outlined dense class="col" maxlength="200"
              :readonly="somenteLeitura"
            />
            <q-btn
              v-if="!somenteLeitura"
              flat round dense size="sm" color="negative" icon="mdi-close"
              @click="bloco.linhas.splice(j, 1)"
            >
              <q-tooltip>Remover linha</q-tooltip>
            </q-btn>
          </div>
          <q-btn
            v-if="!somenteLeitura && bloco.linhas.length < LIMITE_DE_LINHAS"
            flat dense size="sm" color="primary" class="q-mt-xs"
            @click="bloco.linhas.push('')"
          >
            <q-icon left name="mdi-plus" /> Linha
          </q-btn>
        </q-card>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat :disable="salvando" @click="fechar">{{ somenteLeitura ? 'Fechar' : 'Cancelar' }}</q-btn>
        <q-btn
          v-if="!somenteLeitura"
          color="primary" unelevated
          :loading="salvando"
          :disable="erros.length > 0"
          @click="salvar"
        >
          <q-icon left name="mdi-check" /> Salvar
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'

// Espelham os limites de CamposDaNpaDto/AssinaturaDaNpaDto no backend.
const LIMITE_DE_BLOCOS = 10
const LIMITE_DE_LINHAS = 6

const props = defineProps({
  modelValue:    { type: Boolean, default: false },
  campos:        { type: Object, default: null },   // { setorEmissor, local, assinaturas }
  salvando:      { type: Boolean, default: false },
  somenteLeitura: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'salvar'])

const form = reactive({ setorEmissor: '', local: '', assinaturas: [] })

// Reabre sempre com o que está gravado (descarta o que ficou pela metade numa abertura anterior).
watch(() => props.modelValue, (aberto) => {
  if (!aberto) return
  form.setorEmissor = props.campos?.setorEmissor ?? ''
  form.local = props.campos?.local ?? ''
  form.assinaturas = (props.campos?.assinaturas ?? []).map(a => ({ rotulo: a.rotulo, linhas: [...(a.linhas ?? [])] }))
})

const erros = computed(() => {
  const errs = []
  if (!form.setorEmissor.trim()) errs.push('Informe o setor emissor.')
  if (!form.local.trim()) errs.push('Informe o local.')
  if (form.assinaturas.some(a => !a.rotulo.trim())) errs.push('Todo bloco de assinatura precisa de um rótulo.')
  return errs
})

function adicionarBloco() {
  form.assinaturas.push({ rotulo: '', linhas: [''] })
}

function removerBloco(i) {
  form.assinaturas.splice(i, 1)
}

function mover(i, direcao) {
  const j = i + direcao
  if (j < 0 || j >= form.assinaturas.length) return
  const [bloco] = form.assinaturas.splice(i, 1)
  form.assinaturas.splice(j, 0, bloco)
}

function fechar() {
  if (props.salvando) return
  emit('update:modelValue', false)
}

function salvar() {
  if (erros.value.length) return
  emit('salvar', {
    setorEmissor: form.setorEmissor.trim(),
    local: form.local.trim(),
    // Linhas em branco não vão: o bloco só leva o que foi escrito.
    assinaturas: form.assinaturas.map(a => ({
      rotulo: a.rotulo.trim(),
      linhas: a.linhas.map(l => l.trim()).filter(Boolean),
    })),
  })
}
</script>
