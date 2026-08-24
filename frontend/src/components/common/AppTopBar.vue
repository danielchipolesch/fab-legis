<template>
  <q-header elevated class="bg-primary text-white">
    <q-toolbar style="height:60px">
      <q-toolbar-title>
        <div class="row items-center no-wrap" style="gap:12px">
          <q-avatar size="36px" color="white" text-color="primary" rounded>
            <q-icon name="mdi-scale-balance" size="22px" />
          </q-avatar>
          <div>
            <div class="text-subtitle1 text-weight-bold text-white" style="line-height:1">FAB Legis</div>
            <div class="text-caption text-white" style="opacity:.75;line-height:1.1">Gestão de Legislação do COMAER</div>
          </div>
        </div>
      </q-toolbar-title>

      <q-btn
        :to="{ name: 'home' }"
        flat
        color="white"
        class="q-mr-xs"
      >
        <q-icon left name="mdi-home-outline" />
        Início
      </q-btn>

      <q-btn
        v-if="auth.isAdmin"
        :to="{ name: 'usuarios' }"
        flat
        color="white"
        class="q-mr-xs"
      >
        <q-icon left name="mdi-account-multiple-outline" />
        Usuários
      </q-btn>

      <q-btn
        v-if="auth.isAuditor"
        :to="{ name: 'auditoria' }"
        flat
        color="white"
        class="q-mr-xs"
      >
        <q-icon left name="mdi-text-box-search-outline" />
        Auditoria
      </q-btn>

      <q-separator vertical class="q-mx-sm" style="opacity:.4" color="white" />

      <q-btn v-if="auth.usuario" icon="mdi-bell-outline" flat round color="white">
        <q-badge v-if="naoLidas.length" floating rounded color="negative">{{ naoLidas.length }}</q-badge>
        <q-tooltip anchor="bottom middle" self="top middle">Notificações</q-tooltip>
        <q-menu anchor="bottom right" self="top right" class="q-mt-sm">
          <q-list style="min-width:340px;max-width:400px">
            <q-item-label header class="row items-center q-pb-none">
              <span class="col">Notificações</span>
              <q-btn
                v-if="naoLidas.length"
                flat dense no-caps size="sm" color="primary"
                @click="marcarTodasLidas"
              >
                Marcar todas como lidas
              </q-btn>
            </q-item-label>

            <q-separator class="q-mt-sm" />

            <q-list separator style="max-height:360px;overflow-y:auto">
              <q-item
                v-for="n in naoLidas"
                :key="n.id"
                clickable
                v-close-popup
                @click="abrirNotificacao(n)"
              >
                <q-item-section avatar>
                  <q-icon :name="notifIcon(n.tipo)" color="primary" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-body2">{{ n.mensagem }}</q-item-label>
                  <q-item-label caption>{{ formatarRelativo(n.dtCriacao) }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-item v-if="!naoLidas.length">
                <q-item-section class="text-grey-6 text-caption text-center q-py-md">
                  Nenhuma notificação nova.
                </q-item-section>
              </q-item>
            </q-list>
          </q-list>
        </q-menu>
      </q-btn>

      <q-btn v-if="auth.usuario" flat no-caps class="q-ml-xs">
        <q-avatar size="26px" color="secondary" text-color="white" class="q-mr-sm">
          <q-icon name="mdi-account" size="16px" />
        </q-avatar>
        <span class="text-body2 text-white">{{ auth.usuario.nome }}</span>
        <q-icon right name="mdi-chevron-down" color="white" />
        <q-menu anchor="bottom right" self="top right" class="q-mt-sm">
          <q-list dense style="min-width:240px">
            <q-item>
              <q-item-section avatar>
                <q-avatar size="34px" color="secondary" text-color="white">
                  <q-icon name="mdi-account" size="18px" />
                </q-avatar>
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-weight-medium">{{ auth.usuario.nome }}</q-item-label>
                <q-item-label caption>{{ formatarCpf(auth.usuario.cpf) }}</q-item-label>
                <q-item-label caption>{{ auth.usuario.omNome }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-separator />
            <q-item clickable v-close-popup @click="sair">
              <q-item-section avatar>
                <q-icon name="mdi-logout" />
              </q-item-section>
              <q-item-section>Sair</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </q-btn>
    </q-toolbar>
  </q-header>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from '@/stores/auth.js'
import { formatarCpf } from '@/utils/cpf.js'
import * as notificacoesApi from '@/api/notificacoes.js'

const router = useRouter()
const $q = useQuasar()
const auth = useAuthStore()

function sair() {
  auth.logout()
  router.push({ name: 'login' })
}

// ── Notificações em tempo real (SSE) ────────────────────────────────────────
// AppTopBar é sempre montado (inclusive na tela de login), então o ciclo de
// vida da conexão acompanha auth.isAuthenticated em vez do mount/unmount do
// componente. EventSource é usado (não WebSocket) porque o fluxo é só
// servidor->cliente -- e ele já reconecta sozinho em queda de conexão, o
// WebSocket não faria isso de graça.
const naoLidas = ref([])
let eventSource = null

const NOTIF_ICON = {
  DOCUMENTO_COMPARTILHADO: 'mdi-account-multiple-plus-outline',
  APROVACAO_PENDENTE: 'mdi-clock-alert-outline',
}
function notifIcon(tipo) {
  return NOTIF_ICON[tipo] ?? 'mdi-bell-outline'
}

function formatarRelativo(iso) {
  if (!iso) return ''
  const diffMs = Date.now() - new Date(iso).getTime()
  const min = Math.floor(diffMs / 60000)
  if (min < 1) return 'agora'
  if (min < 60) return `há ${min} min`
  const h = Math.floor(min / 60)
  if (h < 24) return `há ${h}h`
  return `há ${Math.floor(h / 24)}d`
}

async function carregarNaoLidas() {
  try {
    naoLidas.value = await notificacoesApi.listNaoLidas()
  } catch {
    // Sem lista inicial não impede o resto do app de funcionar -- o SSE
    // ainda traz notificações novas a partir de agora.
  }
}

function conectarSse() {
  if (eventSource || !auth.token) return
  eventSource = new EventSource(notificacoesApi.streamUrl(auth.token))
  eventSource.addEventListener('notificacao', (event) => {
    const notificacao = JSON.parse(event.data)
    naoLidas.value.unshift(notificacao)
    $q.notify({
      type: 'info',
      icon: notifIcon(notificacao.tipo),
      position: 'top-right',
      message: notificacao.mensagem,
    })
  })
  // onerror não precisa de tratamento manual: o browser reconecta o
  // EventSource sozinho, a menos que o servidor feche a conexão de propósito.
}

function desconectarSse() {
  eventSource?.close()
  eventSource = null
}

watch(() => auth.isAuthenticated, (autenticado) => {
  if (autenticado) {
    carregarNaoLidas()
    conectarSse()
  } else {
    desconectarSse()
    naoLidas.value = []
  }
}, { immediate: true })

onUnmounted(desconectarSse)

async function marcarTodasLidas() {
  try {
    await notificacoesApi.marcarTodasComoLidas()
    naoLidas.value = []
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao marcar notificações: ${e?.message ?? 'erro desconhecido'}` })
  }
}

async function abrirNotificacao(notificacao) {
  naoLidas.value = naoLidas.value.filter(n => n.id !== notificacao.id)
  try {
    await notificacoesApi.marcarComoLida(notificacao.id)
  } catch {
    // Navegação não deve travar por causa disso -- só fica sem marcar como lida.
  }
  router.push({ name: 'documento-visualizar', params: { id: notificacao.documentoId } })
}
</script>
