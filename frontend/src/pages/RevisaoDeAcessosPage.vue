<template>
  <q-page class="q-pa-xl">

    <div class="q-mb-lg">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Manter Revisão de Acessos</h1>
      <p class="text-body2 text-grey-7 q-mb-xs">
        Quem tem papel elevado (aprovador, publicador, auditor, administrador) precisa ter o acesso reconfirmado a cada 6 meses,
        ao trocar de OM e quando a conta fica 180 dias sem entrar. <strong>Manter</strong> reconfirma os papéis; <strong>Retirar
        papéis…</strong> reduz. Cada revisão fica na auditoria de acessos.
      </p>
      <p class="text-body2 text-grey-7 q-mb-none" data-testid="quem-revisa">{{ textoDeQuemRevisa(auth.temEscopoGeral) }}</p>
    </div>

    <q-banner v-if="pendentes" rounded class="bg-amber-1 text-amber-10 q-mb-md" data-testid="aviso-de-pendencias">
      <template #avatar><q-icon name="mdi-account-alert-outline" /></template>
      {{ pendentes }} {{ pendentes === 1 ? 'acesso está pendente' : 'acessos estão pendentes' }} de revisão.
    </q-banner>

    <q-card flat bordered>
      <q-table
        :rows="linhas" :columns="colunas" row-key="usuarioId" :loading="carregando"
        :rows-per-page-options="[15, 25, 50]" :pagination="{ rowsPerPage: 15 }"
        no-data-label="Ninguém com papel elevado para você revisar." flat
      >
        <template #body-cell-pessoa="props">
          <q-td :props="props">
            <div>{{ nomeDaPessoa(props.row) }}</div>
            <div class="text-caption text-grey-7">{{ ocultarCpf(props.row.cpf) }} · {{ props.row.omSigla }}</div>
          </q-td>
        </template>
        <template #body-cell-papeis="props">
          <q-td :props="props">
            <q-chip v-for="p in props.row.papeisElevados" :key="p" dense square size="sm" color="blue-2" text-color="secondary">
              {{ rotuloDoPapel(p) }}
            </q-chip>
          </q-td>
        </template>
        <template #body-cell-situacao="props">
          <q-td :props="props">
            <q-chip dense square size="sm" :color="motivoDaRevisao(props.row.motivo).color" :text-color="motivoDaRevisao(props.row.motivo).textColor">
              {{ motivoDaRevisao(props.row.motivo).label }}
              <q-tooltip v-if="motivoDaRevisao(props.row.motivo).explicacao">{{ motivoDaRevisao(props.row.motivo).explicacao }}</q-tooltip>
            </q-chip>
            <div class="text-caption text-grey-7">Última revisão: {{ formatarData(props.row.dtRevisaoAcesso) }}</div>
            <div class="text-caption text-grey-7">Último acesso: {{ formatarData(props.row.dtUltimoAcesso) }}</div>
          </q-td>
        </template>
        <template #body-cell-acoes="props">
          <q-td :props="props" class="text-right" style="white-space:nowrap">
            <q-btn flat dense color="positive" icon="mdi-check" label="Manter" :loading="salvando === props.row.usuarioId"
                   :data-testid="`manter-${props.row.cpf}`" @click="manter(props.row)" />
            <q-btn flat dense color="negative" icon="mdi-account-minus-outline" label="Retirar papéis…"
                   :disable="salvando === props.row.usuarioId" :data-testid="`retirar-${props.row.cpf}`" @click="abrirRetirada(props.row)" />
          </q-td>
        </template>
      </q-table>
    </q-card>

    <!-- Retirar papéis: desmarque o que a pessoa deixa de ter; o que continuar marcado é mantido. -->
    <q-dialog v-model="dialog.aberto" persistent>
      <q-card style="min-width:400px">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-account-minus-outline" color="negative" size="24px" class="q-mr-sm" />
          <span class="text-h6">Retirar papéis</span>
        </q-card-section>
        <q-card-section v-if="dialog.linha" class="q-pt-md">
          <div class="text-weight-medium q-mb-sm">{{ nomeDaPessoa(dialog.linha) }}</div>
          <div class="text-caption text-grey-7 q-mb-xs">Desmarque o que a pessoa deixa de ter:</div>
          <div class="column">
            <q-checkbox v-for="p in dialog.linha.papeisElevados" :key="p" v-model="dialog.mantidos[p]" :label="rotuloDoPapel(p)" />
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="grey-7" v-close-popup />
          <q-btn unelevated label="Confirmar" color="primary" data-testid="confirmar-retirada" @click="confirmarRetirada" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { listRevisaoDeAcessos, revisarAcesso } from '@/api/revisaoDeAcessos.js'
import { useAuthStore } from '@/stores/auth.js'
import { ocultarCpf } from '@/utils/cpf.js'
import { caixaAlta } from '@/utils/texto.js'
import { motivoDaRevisao, rotuloDoPapel, contarPendentes, textoDeQuemRevisa } from '@/utils/revisaoDeAcessos.js'

const $q = useQuasar()
const auth = useAuthStore()

const linhas = ref([])
const carregando = ref(false)
const salvando = ref(null)
const dialog = reactive({ aberto: false, linha: null, mantidos: {} })

const pendentes = computed(() => contarPendentes(linhas.value))

const colunas = [
  { name: 'pessoa',   label: 'Pessoa',          field: 'nome', align: 'left' },
  { name: 'papeis',   label: 'Papéis elevados', field: 'papeisElevados', align: 'left' },
  { name: 'situacao', label: 'Situação',        field: 'motivo', align: 'left' },
  { name: 'acoes',    label: '',                field: 'usuarioId', align: 'right' },
]

const nomeDaPessoa = (l) => (l.postoGraduacaoBigrama && l.nomeGuerra
  ? `${l.postoGraduacaoBigrama} ${caixaAlta(l.nomeGuerra)}`
  : caixaAlta(l.nome))

function formatarData(iso) {
  if (!iso) return '—'
  const [y, m, d] = String(iso).split('T')[0].split('-')
  return `${d}/${m}/${y}`
}

async function carregar() {
  carregando.value = true
  try {
    linhas.value = await listRevisaoDeAcessos()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar a revisão de acessos: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

async function enviar(linha, papeisMantidos, mensagem) {
  salvando.value = linha.usuarioId
  try {
    const atualizada = await revisarAcesso(linha.usuarioId, ['EDIT', ...papeisMantidos])
    const i = linhas.value.findIndex(l => l.usuarioId === linha.usuarioId)
    // Sem papel elevado a pessoa sai da lista; senão, a linha volta "em dia".
    if (!atualizada.papeisElevados.length) linhas.value.splice(i, 1)
    else linhas.value.splice(i, 1, atualizada)
    $q.notify({ type: 'positive', message: mensagem })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Não foi possível registrar a revisão: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = null
  }
}

function manter(linha) {
  enviar(linha, linha.papeisElevados, `Acesso de ${nomeDaPessoa(linha)} mantido.`)
}

function abrirRetirada(linha) {
  dialog.linha = linha
  dialog.mantidos = Object.fromEntries(linha.papeisElevados.map(p => [p, true]))
  dialog.aberto = true
}

function confirmarRetirada() {
  const linha = dialog.linha
  dialog.aberto = false
  if (!linha) return
  enviar(linha, linha.papeisElevados.filter(p => dialog.mantidos[p]), `Acesso de ${nomeDaPessoa(linha)} revisado.`)
}

onMounted(carregar)
</script>
