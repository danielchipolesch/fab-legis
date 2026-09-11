<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" persistent>
    <q-card style="width:420px; max-width:95vw">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="mdi-account-arrow-right-outline" color="primary" size="22px" class="q-mr-sm" />
        <div class="text-h6 text-primary">{{ titulo }}</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense :disable="enviando" @click="fechar" />
      </q-card-section>
      <div v-if="descricao" class="text-caption text-grey-7 q-px-md q-pb-sm">{{ descricao }}</div>
      <q-separator />

      <q-card-section>
        <q-select
          v-model="selecionado"
          :options="opcoes"
          option-label="rotulo"
          option-value="id"
          label="Pessoa"
          outlined
          dense
          emit-value
          map-options
          :loading="carregando"
          :error="!!erro"
          :error-message="erro"
          :disable="enviando"
        >
          <template #no-option>
            <q-item>
              <q-item-section class="text-grey-6">
                Ninguém com esse papel na sua OM ainda.
              </q-item-section>
            </q-item>
          </template>
        </q-select>
      </q-card-section>

      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn flat label="Cancelar" :disable="enviando" @click="fechar" />
        <q-btn
          color="primary" unelevated
          :label="acaoLabel"
          :loading="enviando"
          :disable="!selecionado"
          @click="confirmar"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { listUsuariosElegiveis } from '@/api/usuarios.js'
import { formatarCpf } from '@/utils/cpf.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  papel: { type: String, required: true }, // 'APROV' | 'PUBLIC'
  titulo: { type: String, required: true },
  descricao: { type: String, default: '' },
  acaoLabel: { type: String, default: 'Confirmar' },
  // Controlado por quem usa o diálogo (RevisaoPage.vue/PublicacaoPage.vue/
  // HomePage.vue) enquanto a chamada de status está em andamento.
  enviando: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirmar'])

const candidatos = ref([])
const selecionado = ref(null)
const carregando = ref(false)
const erro = ref('')

const opcoes = computed(() => candidatos.value.map(c => ({
  id: c.id,
  rotulo: `${c.postoGraduacaoBigrama && c.nomeGuerra ? `${c.postoGraduacaoBigrama} ${c.nomeGuerra}` : c.nome} — ${formatarCpf(c.cpf)}`,
})))

async function carregar() {
  carregando.value = true
  erro.value = ''
  selecionado.value = null
  try {
    candidatos.value = await listUsuariosElegiveis(props.papel)
  } catch (e) {
    erro.value = e?.message ?? 'Erro ao carregar pessoas elegíveis'
  } finally {
    carregando.value = false
  }
}

watch(() => props.modelValue, (aberto) => { if (aberto) carregar() })

function fechar() {
  if (props.enviando) return
  emit('update:modelValue', false)
}

function confirmar() {
  if (!selecionado.value) return
  emit('confirmar', selecionado.value)
}
</script>
