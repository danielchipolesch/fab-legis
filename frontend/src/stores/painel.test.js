import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePainelStore } from './painel.js'
import { CARTOES } from '@/utils/painel.js'

// O hub guarda no Pinia a página de cada card: sair (para abrir um documento) e voltar não recomeça da primeira.
describe('stores/painel', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('todo card começa na primeira página', () => {
    const store = usePainelStore()
    for (const cartao of CARTOES) expect(store.paginas[cartao]).toBe(1)
  })

  it('cada card guarda a sua página, sem mexer nas dos outros', () => {
    const store = usePainelStore()
    store.definirPagina('aguardando', 3)
    expect(store.paginas).toEqual({ em_andamento: 1, aguardando: 3, publicados: 1 })
  })
})
