import { defineStore } from 'pinia'
import * as buscaApi from '@/api/busca.js'

// Guarda a última busca em memória (Pinia, não sessionStorage/localStorage --
// não precisa sobreviver a um F5, só a navegar pra um documento e voltar via
// breadcrumb) para que BuscaPage.vue não perca os resultados ao sair e voltar
// -- sem isso, a pessoa que abre vários resultados um a um (clicando, voltando,
// clicando no próximo) tinha que repetir a busca inteira a cada volta.
export const useBuscaStore = defineStore('busca', {
  state: () => ({
    termo: '',
    termoBuscado: '',
    resultados: [],
    totalElements: 0,
    totalPages: 0,
    pagina: 1,
    carregando: false,
  }),

  actions: {
    async buscarAgora() {
      this.pagina = 1
      await this.carregar()
    },

    async carregar() {
      if (!this.termo || !this.termo.trim()) return
      this.carregando = true
      try {
        const resp = await buscaApi.buscar(this.termo.trim(), this.pagina - 1)
        this.termoBuscado = this.termo.trim()
        this.resultados = resp.content ?? []
        this.totalElements = resp.totalElements ?? 0
        this.totalPages = resp.totalPages ?? 0
      } finally {
        this.carregando = false
      }
    },

    async irParaPagina(pagina) {
      this.pagina = pagina
      await this.carregar()
    },
  },
})
