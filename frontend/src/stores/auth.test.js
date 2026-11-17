import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// O ambiente de teste é "node" (sem DOM): a store lê window.location e localStorage, então eles são simulados ANTES de ela ser importada.
vi.hoisted(() => {
  const memoria = new Map()
  globalThis.window = { location: { origin: 'https://fab-legis.exemplo' } }
  globalThis.localStorage = {
    getItem: (k) => (memoria.has(k) ? memoria.get(k) : null),
    setItem: (k, v) => memoria.set(k, String(v)),
    removeItem: (k) => memoria.delete(k),
    clear: () => memoria.clear(),
  }
})

// Ordem em que o client HTTP é plugado e a primeira chamada à API sai. Registrada pelos mocks abaixo.
const ordem = []

vi.mock('@/api/client.js', () => ({
  setAuthTokenGetter: () => ordem.push('token'),
  setUnauthorizedHandler: () => ordem.push('nao-autorizado'),
  setRefreshHandler: () => ordem.push('renovacao'),
}))
vi.mock('@/api/habilitacao.js', () => ({
  listMinhasEspeciesCriaveis: async () => { ordem.push('GET /habilitacao-de-documentos/minhas'); return [] },
}))

import { useAuthStore } from './auth.js'

// Recarregar a página com a pessoa logada: o token vem do localStorage e a primeira chamada à API (as espécies que a OM pode criar)
// sai no próprio boot. Se o client HTTP ainda não estiver plugado nesse instante, a chamada sai SEM token, toma 401 e força uma
// renovação silenciosa -- que, se falhar, desloga a pessoa a cada recarga (achado no teste de ponta a ponta com HTTPS).
describe('stores/auth.inicializar', () => {
  beforeEach(() => {
    ordem.length = 0
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('pluga o token, o tratamento de 401 e a renovação ANTES da primeira chamada à API', () => {
    localStorage.setItem('fab-legis.auth', JSON.stringify({ token: 'jwt-de-teste', usuarioId: 1, nome: 'Fulano', papeis: [] }))
    setActivePinia(createPinia()) // a store lê o localStorage ao ser criada

    useAuthStore().inicializar()

    expect(ordem).toEqual(['token', 'nao-autorizado', 'renovacao', 'GET /habilitacao-de-documentos/minhas'])
  })

  it('sem sessão guardada, não chama a API no boot (só pluga o client)', () => {
    useAuthStore().inicializar()

    expect(ordem).toEqual(['token', 'nao-autorizado', 'renovacao'])
  })
})
