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

      <!-- Coluna sem quebra: "column" do Quasar quebra em colunas quando passa do max-height, e o bloco novo ia parar
           ao lado dos outros em vez de embaixo. -->
      <q-card-section ref="corpoRef" class="q-pt-md column no-wrap q-gutter-y-md" style="max-height:65vh;overflow-y:auto">
        <p class="text-caption text-grey-7 q-mb-none">
          A identificação, o assunto e a data da NPA vêm do documento; a distribuição é sempre ostensiva. Aqui ficam o
          setor que emite a NPA, o local do fecho e os blocos de assinatura, em texto livre. Os blocos “Elaborado por”
          (autor e coautores) e “Aprovado por” (quem aprova) são preenchidos sozinhos, no começo e no fim das assinaturas.
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
          <span class="text-subtitle2 text-weight-bold">Outras assinaturas</span>
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

        <div v-if="!form.assinaturas.length" class="text-caption text-grey-6">Nenhum bloco além de Elaborado por e Aprovado por.</div>

        <q-card v-for="(bloco, i) in form.assinaturas" :key="bloco.chave" flat bordered class="q-pa-sm">
          <div class="row items-center q-col-gutter-sm">
            <q-input
              v-model="bloco.rotulo"
              label="Rótulo"
              hint="Ex.: Visto, Proposto por, Ciente"
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
              :label="j === 0 ? 'Linha (ex.: posto e nome)' : 'Linha'"
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
        <div v-if="!somenteLeitura && erros.length" class="text-negative text-caption col">{{ erros[0] }}</div>
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
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { rotuloReservado } from '@/perfis/npa.js'

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
const corpoRef = ref(null)

// Chave estável de cada bloco: com o índice como chave, remover/mover um bloco trocava o conteúdo dos campos de lugar.
let proximaChave = 0
const novoBloco = (rotulo = '', linhas = ['']) => ({ chave: ++proximaChave, rotulo, linhas })

// Reabre sempre com o que está gravado (descarta o que ficou pela metade numa abertura anterior).
watch(() => props.modelValue, (aberto) => {
  if (!aberto) return
  form.setorEmissor = props.campos?.setorEmissor ?? ''
  form.local = props.campos?.local ?? ''
  form.assinaturas = (props.campos?.assinaturas ?? []).map(a => novoBloco(a.rotulo, [...(a.linhas ?? [])]))
})

const erros = computed(() => {
  const errs = []
  if (!form.setorEmissor.trim()) errs.push('Informe o setor emissor.')
  if (!form.local.trim()) errs.push('Informe o local.')
  if (form.assinaturas.some(a => !a.rotulo.trim())) errs.push('Todo bloco de assinatura precisa de um rótulo.')
  if (form.assinaturas.some(a => rotuloReservado(a.rotulo))) {
    errs.push('“Elaborado por” e “Aprovado por” são preenchidos sozinhos; use outro rótulo.')
  }
  return errs
})

// O bloco novo entra embaixo dos demais e a lista rola até ele.
async function adicionarBloco() {
  form.assinaturas.push(novoBloco())
  await nextTick()
  const corpo = corpoRef.value?.$el
  if (corpo) corpo.scrollTo({ top: corpo.scrollHeight, behavior: 'smooth' })
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
