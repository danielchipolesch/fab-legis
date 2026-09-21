import { defineStore } from 'pinia'

// Estado do hub (tela inicial) que precisa sobreviver a sair e voltar dentro da mesma sessão -- por exemplo, abrir um
// documento a partir de um card e voltar: cada card continua na página em que estava, em vez de recomeçar da primeira.
// Mesmo raciocínio de stores/documentos.js (filtros da tela do módulo) e de stores/busca.js.
export const usePainelStore = defineStore('painel', {
  state: () => ({
    // A página (1, 2, 3...) em que cada card do hub está (ver utils/painel.js, CARTOES).
    paginas: { em_andamento: 1, aguardando: 1, publicados: 1 },
  }),

  actions: {
    definirPagina(cartao, pagina) {
      this.paginas[cartao] = pagina
    },
  },
})
