import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDocumentosStore } from './documentos.js'

const CONVENCIONAL = 'CONVENCIONAL'
const NPA = 'COMUNICACAO_OFICIAL_PADRONIZADA'

// A tela de cada módulo usa o mesmo estado (aba, filtros, página, modo de visualização) no Pinia; trocar de módulo guarda o
// de quem saiu e traz o de quem entra -- os filtros de um não valem no outro, mas também não se perdem.
describe('entrarNoModulo', () => {
  let store
  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDocumentosStore()
  })

  it('a primeira vez num módulo começa do padrão', () => {
    store.entrarNoModulo(CONVENCIONAL)
    expect(store.moduloAtivo).toBe(CONVENCIONAL)
    expect(store.abaAtiva).toBe('meus')
    expect(store.filtros).toEqual({ busca: '', especie: null, situacaoBca: null, situacaoLocal: null })
    expect(store.tablePagination.page).toBe(1)
    expect(store.viewMode).toBe('tabela')
  })

  it('entrar de novo no mesmo módulo não mexe em nada', () => {
    store.entrarNoModulo(CONVENCIONAL)
    store.abaAtiva = 'minha_om'
    store.filtros.busca = 'gestão'
    store.entrarNoModulo(CONVENCIONAL)
    expect(store.abaAtiva).toBe('minha_om')
    expect(store.filtros.busca).toBe('gestão')
  })

  it('o outro módulo começa do padrão, sem herdar os filtros do primeiro', () => {
    store.entrarNoModulo(CONVENCIONAL)
    store.abaAtiva = 'outras_oms'
    store.filtros.busca = 'gestão'
    store.tablePagination.page = 4

    store.entrarNoModulo(NPA)

    expect(store.abaAtiva).toBe('meus')
    expect(store.filtros.busca).toBe('')
    expect(store.tablePagination.page).toBe(1)
  })

  it('voltar a um módulo traz de volta aba, filtros, página e modo de visualização', () => {
    store.entrarNoModulo(CONVENCIONAL)
    store.abaAtiva = 'outras_oms'
    store.filtros.busca = 'gestão'
    store.filtros.situacaoLocal = 'MINUTA'
    store.tablePagination.page = 4
    store.viewMode = 'cards'

    store.entrarNoModulo(NPA)
    store.abaAtiva = 'minha_om'          // o que a pessoa fez no NPA não pode contaminar o convencional
    store.entrarNoModulo(CONVENCIONAL)

    expect(store.abaAtiva).toBe('outras_oms')
    expect(store.filtros).toMatchObject({ busca: 'gestão', situacaoLocal: 'MINUTA' })
    expect(store.tablePagination.page).toBe(4)
    expect(store.viewMode).toBe('cards')

    store.entrarNoModulo(NPA)
    expect(store.abaAtiva).toBe('minha_om')
  })

  it('a lista mostrada é esvaziada ao trocar de módulo', () => {
    store.entrarNoModulo(CONVENCIONAL)
    store.documentos = [{ id: 1 }]
    store.totalElements = 1
    store.entrarNoModulo(NPA)
    expect(store.documentos).toEqual([])
    expect(store.totalElements).toBe(0)
  })
})
