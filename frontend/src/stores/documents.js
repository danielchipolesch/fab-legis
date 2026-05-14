import { defineStore } from 'pinia'
import * as api from '@/api/documentos.js'

export const useDocumentsStore = defineStore('documents', {
  state: () => ({
    documentos: [],
    loading: false,
  }),

  getters: {
    getById: (state) => (id) => state.documentos.find(d => String(d.id) === String(id)) ?? null,
  },

  actions: {
    async fetchAll() {
      if (this.loading) return
      this.loading = true
      try {
        this.documentos = await api.listar()
      } finally {
        this.loading = false
      }
    },

    async fetchDocumento(id) {
      const doc = await api.obterPorId(id)
      if (!doc) return null
      const idx = this.documentos.findIndex(d => String(d.id) === String(id))
      if (idx !== -1) this.documentos[idx] = doc
      else this.documentos.push(doc)
      return doc
    },

    async createDocumento(payload) {
      const novo = await api.criar(payload)
      this.documentos.unshift(novo)
      return novo
    },

    async cloneDocumento(id) {
      const clone = await api.clonar(id)
      if (clone) this.documentos.unshift(clone)
      return clone
    },

    async saveDocumento(documento) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(documento.id))
      if (idx === -1) return
      const atualizado = await api.atualizar(documento.id, documento.titulo ?? documento.tituloDocumento)
      if (atualizado) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
    },

    async changeStatus(id, novoStatus) {
      const atualizado = await api.mudarStatus(id, novoStatus)
      if (atualizado) {
        const idx = this.documentos.findIndex(d => String(d.id) === String(id))
        if (idx !== -1) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
      }
    },

    async deleteDocumento(id) {
      await api.excluir(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },
  },
})
