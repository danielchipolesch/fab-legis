<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="width:420px; max-width:95vw">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="mdi-account-multiple-plus-outline" color="primary" size="22px" class="q-mr-sm" />
        <div class="text-h6 text-primary">Autores do documento</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup />
      </q-card-section>
      <div class="text-caption text-grey-7 q-px-md q-pb-sm">
        Todos os autores são iguais: editam este documento (nas etapas em que o texto muda) e, enquanto ele estiver
        em Rascunho, também podem excluí-lo. Qualquer autor pode atribuir a autoria a outra pessoa. A autoria é da pessoa,
        não da OM: quem for transferido continua autor. Quem desvincula um autor é o Administrador da OM do documento
        — ou o próprio autor, ao se desvincular de si mesmo, depois de sair da OM do documento.
      </div>
      <q-separator />

      <q-card-section v-if="semAcesso" class="text-grey-7 text-body2" data-testid="sem-acesso-autores">
        Só os autores do documento e o Administrador da OM dele veem e atribuem a autoria. Para passar a editá-lo,
        peça a um autor ou ao Administrador da OM.
      </q-card-section>

      <q-card-section v-else>
        <!-- Busca várias pessoas, que ficam como "fichas"; um só botão atribui a autoria a todas as selecionadas. -->
        <q-form @submit.prevent="adicionar">
          <q-select
            v-model="selecionados"
            :options="opcoes"
            option-label="rotulo"
            option-value="id"
            label="Buscar pessoas por nome de guerra ou nome completo"
            outlined
            dense
            multiple
            use-chips
            use-input
            emit-value
            map-options
            input-debounce="300"
            data-testid="buscar-autores"
            @filter="filtrar"
            :loading="buscando"
            :error="!!erro"
            :error-message="erro"
          >
            <template #no-option>
              <q-item>
                <q-item-section class="text-grey-6">
                  {{ termoBusca.trim().length < 2 ? 'Digite ao menos 2 letras do nome' : 'Ninguém encontrado' }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>
          <div class="row items-center justify-end q-mt-sm" style="gap:8px">
            <span v-if="selecionados.length" class="text-caption text-grey-7">
              {{ selecionados.length }} {{ selecionados.length === 1 ? 'pessoa selecionada' : 'pessoas selecionadas' }}
            </span>
            <q-btn
              unelevated no-caps color="primary" icon="mdi-account-plus-outline"
              :label="selecionados.length > 1 ? 'Adicionar autores' : 'Adicionar autor'"
              :disable="!selecionados.length" :loading="adicionando" type="submit" data-testid="adicionar-autores"
            />
          </div>
        </q-form>
      </q-card-section>

      <div v-if="atribuiASiMesmo" class="q-px-md q-pb-sm">
        <q-btn
          flat dense no-caps color="primary" icon="mdi-account-plus-outline" label="Atribuir a autoria a mim"
          :loading="adicionando" data-testid="atribuir-a-mim" @click="atribuirAMim"
        />
      </div>

      <q-separator />

      <q-card-section v-if="!semAcesso" class="q-pa-none" style="max-height:280px; overflow-y:auto">
        <q-list separator>
          <q-item v-for="c in compartilhamentos" :key="c.usuarioId">
            <q-item-section avatar>
              <q-avatar size="32px" color="blue-2" text-color="primary">
                <q-icon name="mdi-account" size="18px" />
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ caixaAlta(c.nome) }}</q-item-label>
              <q-item-label caption>{{ ocultarCpf(c.cpf) }}</q-item-label>
            </q-item-section>
            <q-item-section v-if="podeDesvincularAutor(c)" side>
              <q-btn icon="mdi-delete-outline" flat round dense size="sm" color="negative" data-testid="desvincular-autor" @click="desvincular(c)">
                <q-tooltip>{{ rotuloDeDesvincular(c.usuarioId, usuario) }}</q-tooltip>
              </q-btn>
            </q-item-section>
          </q-item>
          <q-item v-if="!compartilhamentos.length">
            <q-item-section class="text-grey-6 text-caption text-center q-py-md">
              Nenhum autor.
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import * as api from '@/api/documentos.js'
import { buscarUsuariosPorNome } from '@/api/usuarios.js'
import { ocultarCpf } from '@/utils/cpf.js'
import { caixaAlta } from '@/utils/texto.js'
import { useAuthStore } from '@/stores/auth.js'
import { podeDesvincular, rotuloDeDesvincular, podeAtribuirAutoriaASiMesmo } from '@/utils/autoria.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  documentoId: { type: [String, Number], required: true },
  // OM do documento: define quem desvincula um autor (o Administrador dela) e quem pode se desvincular (quem já saiu dela).
  omDoDocumentoId: { type: [String, Number], default: null },
})
defineEmits(['update:modelValue'])

const $q = useQuasar()
const usuario = computed(() => useAuthStore().usuario)
const podeDesvincularAutor = (c) => podeDesvincular(c.usuarioId, usuario.value, props.omDoDocumentoId)
const atribuiASiMesmo = computed(() => podeAtribuirAutoriaASiMesmo(compartilhamentos.value, usuario.value, props.omDoDocumentoId))
const erro = ref('')
const semAcesso = ref(false)
const adicionando = ref(false)
const compartilhamentos = ref([])

// Quem já foi escolhido continua nas opções mesmo que a busca mude (senão a ficha perde o nome); quem já é autor não é oferecido.
const candidatos = ref([])
const escolhidos = ref(new Map())
const selecionados = ref([])
const termoBusca = ref('')
const buscando = ref(false)

const rotuloDe = (c) =>
  `${c.postoGraduacaoBigrama && c.nomeGuerra ? `${c.postoGraduacaoBigrama} ${caixaAlta(c.nomeGuerra)}` : caixaAlta(c.nome)} — ${c.omSigla ?? ''} (${ocultarCpf(c.cpf)})`

const opcoes = computed(() => {
  const jaSaoAutores = new Set(compartilhamentos.value.map(a => String(a.usuarioId)))
  const todos = new Map(escolhidos.value)
  candidatos.value.forEach(c => todos.set(c.id, c))
  return [...todos.values()]
    .filter(c => !jaSaoAutores.has(String(c.id)))
    .map(c => ({ id: c.id, cpf: c.cpf, rotulo: rotuloDe(c) }))
})

watch(selecionados, (ids) => {
  const mapa = new Map()
  ids.forEach(id => {
    const c = candidatos.value.find(x => x.id === id) ?? escolhidos.value.get(id)
    if (c) mapa.set(id, c)
  })
  escolhidos.value = mapa
})

async function carregar() {
  semAcesso.value = false
  try {
    compartilhamentos.value = await api.listCompartilhamentos(props.documentoId)
  } catch (e) {
    // 403: quem não é autor do documento (nem Administrador da OM dele) não vê a lista -- só a leitura do documento é aberta.
    if (e?.status === 403) { semAcesso.value = true; compartilhamentos.value = [] }
    else $q.notify({ type: 'negative', message: `Erro ao carregar os autores: ${e?.message ?? 'erro desconhecido'}` })
  }
}

watch(() => props.modelValue, (aberto) => {
  if (aberto) {
    selecionados.value = []
    candidatos.value = []
    erro.value = ''
    carregar()
  }
})

// Quasar chama isto a cada tecla (já espaçado pelo input-debounce acima) --
// `update()` troca as opções do dropdown pelo resultado da busca no backend.
function filtrar(val, update, abort) {
  termoBusca.value = val
  buscando.value = true
  buscarUsuariosPorNome(val)
    .then(resultado => { candidatos.value = resultado; update() })
    .catch(() => abort())
    .finally(() => { buscando.value = false })
}

async function adicionar() {
  erro.value = ''
  const pessoas = selecionados.value.map(id => escolhidos.value.get(id)).filter(Boolean)
  if (!pessoas.length) {
    erro.value = 'Selecione ao menos uma pessoa na lista'
    return
  }
  adicionando.value = true
  const falhas = []
  const falharam = new Set()
  let atribuidas = 0
  for (const p of pessoas) {
    try {
      await api.compartilharDocumento(props.documentoId, p.cpf)
      atribuidas++
    } catch (e) {
      falharam.add(p.id)
      falhas.push(`${caixaAlta(p.nomeGuerra || p.nome)}: ${e?.message ?? 'erro desconhecido'}`)
    }
  }
  // Quem deu certo sai da seleção; quem falhou fica, com o motivo, para tentar de novo.
  selecionados.value = selecionados.value.filter(id => falharam.has(id))
  candidatos.value = []
  await carregar()
  adicionando.value = false
  if (atribuidas) $q.notify({ type: 'positive', message: atribuidas === 1 ? 'Autoria atribuída.' : `Autoria atribuída a ${atribuidas} pessoas.` })
  if (falhas.length) erro.value = falhas.join(' · ')
}

// O Administrador da OM atribui a autoria a si mesmo (para poder editar): a mesma chamada, com o próprio CPF.
async function atribuirAMim() {
  adicionando.value = true
  try {
    await api.compartilharDocumento(props.documentoId, usuario.value.cpf)
    await carregar()
    $q.notify({ type: 'positive', message: 'Você agora é autor deste documento.' })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao atribuir a autoria: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    adicionando.value = false
  }
}

async function desvincular(c) {
  const eu = String(c.usuarioId) === String(usuario.value?.id)
  try {
    await api.desvincularAutor(props.documentoId, c.usuarioId)
    await carregar()
    $q.notify({ type: 'positive', message: eu ? 'Você se desvinculou do documento.' : 'Autor desvinculado.' })
  } catch (e) {
    // Ex.: "esta é a única pessoa atribuída como elaboradora" -- a mensagem do servidor diz o porquê.
    $q.notify({ type: 'negative', message: e?.message ?? 'Não foi possível desvincular o autor.' })
  }
}
</script>
