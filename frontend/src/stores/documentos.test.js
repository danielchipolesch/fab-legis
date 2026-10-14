import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDocumentosStore } from './documentos.js'
import * as api from '@/api/documentos.js'

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

// POST /documentos e POST /documentos/{id}/clonar devolvem o DTO enxuto, sem a árvore de itens -- mas o backend já
// criou a estrutura inicial da espécie na hora (EstruturaInicialDeNovoDocumento). createDocumento/cloneDocumento têm
// de buscar essa estrutura real (fetchDocumento -> GET /documentos/{id}), nunca supor um template genérico no
// cliente: um template fixo de espécie convencional, salvo de volta pelo autoSave assim que o editor abre
// (DocumentoEditorPage.vue, _fromTemplate), sobrescreveria a estrutura certa de uma NPA recém-criada pela errada.
describe('createDocumento e cloneDocumento buscam a estrutura real, não um template', () => {
  let store

  const secaoNpaReal = { tipo: 'parte_normativa', elementos: [{ id: 'e1', tipo: 'capitulo', titulo: 'DISPOSIÇÕES PRELIMINARES' }] }
  const npaCriada = { id: 42, tipo_de_especie: NPA, codigo_documento: 'NPA-1' }
  const npaComEstrutura = { ...npaCriada, secoes: [secaoNpaReal] }

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDocumentosStore()
    vi.restoreAllMocks()
  })

  it('createDocumento não usa gerarSecoesTemplate -- busca a estrutura que o backend já criou', async () => {
    vi.spyOn(api, 'createDocumento').mockResolvedValue(npaCriada)
    vi.spyOn(api, 'getDocumento').mockResolvedValue(npaComEstrutura)

    const doc = await store.createDocumento({ idEspecieNormativa: 12, identificacao: 'NPA-1' })

    expect(api.getDocumento).toHaveBeenCalledWith(42)
    expect(doc.secoes).toEqual([secaoNpaReal])
    expect(doc._fromTemplate).toBe(false)
  })

  it('cloneDocumento idem, e mantém a contagem de réplicas do original', async () => {
    store.documentos = [{ id: 7, qtd_replicas: 0 }]
    vi.spyOn(api, 'cloneDocumento').mockResolvedValue(npaCriada)
    vi.spyOn(api, 'getDocumento').mockResolvedValue(npaComEstrutura)

    const clone = await store.cloneDocumento(7)

    expect(api.getDocumento).toHaveBeenCalledWith(42)
    expect(clone.secoes).toEqual([secaoNpaReal])
    expect(store.documentos.find(d => d.id === 7).qtd_replicas).toBe(1)
  })
})
