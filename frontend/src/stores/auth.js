import { defineStore } from 'pinia'
import * as api from '@/api/auth.js'
import { setAuthTokenGetter, setUnauthorizedHandler } from '@/api/client.js'

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
      usuario: salvo ? {
        id: salvo.usuarioId,
        nome: salvo.nome,
        cpf: salvo.cpf,
        omId: salvo.omId,
        omNome: salvo.omNome,
        papeis: salvo.papeis ?? [],
      } : null,
      loading: false,
      erro: null,
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.usuario?.papeis?.includes('ADMIN') ?? false,
    isAprovador: (state) => state.usuario?.papeis?.includes('APROVADOR') ?? false,
  },

  actions: {
    // Chamado uma vez no boot do app (main.js) -- plugueia o client HTTP no
    // token/estado deste store sem criar dependência circular entre módulos.
    inicializar() {
      setAuthTokenGetter(() => this.token)
      setUnauthorizedHandler(() => this.logout())
    },

    async login(cpf, senha) {
      this.loading = true
      this.erro = null
      try {
        const resp = await api.login(cpf, senha)
        this.token = resp.token
        this.usuario = {
          id: resp.usuarioId,
          nome: resp.nome,
          cpf: resp.cpf,
          omId: resp.omId,
          omNome: resp.omNome,
          papeis: resp.papeis ?? [],
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(resp))
        return true
      } catch (e) {
        this.erro = e?.message ?? 'Erro ao entrar.'
        return false
      } finally {
        this.loading = false
      }
    },

    logout() {
      this.token = null
      this.usuario = null
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})
