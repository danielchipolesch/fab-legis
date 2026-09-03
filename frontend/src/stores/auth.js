import { defineStore } from 'pinia'
import * as api from '@/api/auth.js'
import { setAuthTokenGetter, setUnauthorizedHandler, setRefreshHandler } from '@/api/client.js'

const STORAGE_KEY = 'fab-legis.auth'

function lerArmazenado() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const salvo = lerArmazenado()
    return {
      token: salvo?.token ?? null,
      refreshToken: salvo?.refreshToken ?? null,
      usuario: salvo ? {
        id: salvo.usuarioId,
        nome: salvo.nome,
        cpf: salvo.cpf,
        omId: salvo.omId,
        omNome: salvo.omNome,
        nomeGuerra: salvo.nomeGuerra,
        postoGraduacaoBigrama: salvo.postoGraduacaoBigrama,
        papeis: salvo.papeis ?? [],
      } : null,
      loading: false,
      erro: null,
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    // ADMIN é puramente administrativo (usuários/OMs) -- não implica EDIT/APROV/PUBLIC
    // nenhum, ver PapelEnum no backend.
    isAdmin: (state) => state.usuario?.papeis?.includes('ADMIN') ?? false,
    isEditor: (state) => state.usuario?.papeis?.includes('EDIT') ?? false,
    isAprovador: (state) => state.usuario?.papeis?.includes('APROV') ?? false,
    isPublicador: (state) => state.usuario?.papeis?.includes('PUBLIC') ?? false,
    // Espelha o backend (hasRole('AUDITOR') -- ver AuditoriaController): papel
    // independente de ADMIN, precisa estar marcado no próprio cadastro.
    isAuditor: (state) => state.usuario?.papeis?.includes('AUDITOR') ?? false,
  },

  actions: {
    // Chamado uma vez no boot do app (main.js) -- plugueia o client HTTP no
    // token/estado deste store sem criar dependência circular entre módulos.
    inicializar() {
      setAuthTokenGetter(() => this.token)
      setUnauthorizedHandler(() => this.logout())
      setRefreshHandler(() => this.refresh())
    },

    _aplicarResposta(resp) {
      this.token = resp.token
      this.refreshToken = resp.refreshToken
      this.usuario = {
        id: resp.usuarioId,
        nome: resp.nome,
        cpf: resp.cpf,
        omId: resp.omId,
        omNome: resp.omNome,
        nomeGuerra: resp.nomeGuerra,
        postoGraduacaoBigrama: resp.postoGraduacaoBigrama,
        papeis: resp.papeis ?? [],
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(resp))
    },

    async login(cpf, senha) {
      this.loading = true
      this.erro = null
      try {
        const resp = await api.login(cpf, senha)
        this._aplicarResposta(resp)
        return true
      } catch (e) {
        this.erro = e?.message ?? 'Erro ao entrar.'
        return false
      } finally {
        this.loading = false
      }
    },

    // Troca o refresh token guardado por um novo par access+refresh -- ver
    // client.js (chamado automaticamente por ele quando uma requisição toma
    // 401 por access token expirado, antes de deslogar). O refresh token é
    // rotacionado a cada uso no backend, então este método também precisa
    // gravar o novo refreshToken recebido, nunca reusar o antigo.
    async refresh() {
      if (!this.refreshToken) return false
      try {
        const resp = await api.refresh(this.refreshToken)
        this._aplicarResposta(resp)
        return true
      } catch {
        return false
      }
    },

    // Atualiza os dados de identificação do usuário logado em memória e no
    // localStorage sem precisar de um novo login -- chamado pela tela de
    // usuários quando o admin edita o próprio perfil, para que o topbar
    // reflita a mudança na hora (reatividade do Pinia) em vez de exigir
    // recarregar a página.
    atualizarPerfil(dados) {
      if (!this.usuario) return
      this.usuario = {
        ...this.usuario,
        nome: dados.nome ?? this.usuario.nome,
        nomeGuerra: dados.nomeGuerra ?? null,
        postoGraduacaoBigrama: dados.postoGraduacaoBigrama ?? null,
        omId: dados.omId ?? this.usuario.omId,
        omNome: dados.omNome ?? this.usuario.omNome,
        papeis: dados.papeis ?? this.usuario.papeis,
      }
      const salvo = lerArmazenado()
      if (salvo) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          ...salvo,
          nome: this.usuario.nome,
          nomeGuerra: this.usuario.nomeGuerra,
          postoGraduacaoBigrama: this.usuario.postoGraduacaoBigrama,
          omId: this.usuario.omId,
          omNome: this.usuario.omNome,
          papeis: this.usuario.papeis,
        }))
      }
    },

    async logout() {
      const refreshToken = this.refreshToken
      this.token = null
      this.refreshToken = null
      this.usuario = null
      localStorage.removeItem(STORAGE_KEY)
      if (refreshToken) {
        try { await api.logout(refreshToken) } catch { /* revogação é best-effort */ }
      }
    },
  },
})
