import { describe, it, expect } from 'vitest'
import { podeCriarNoModulo, podeCriarEspecie, apenasCriaveis, omIdsPorEspecie, filtrarOpcoesDeOm, especiesDaOm } from './habilitacao.js'

const CONV = 'CONVENCIONAL'
const COP = 'COMUNICACAO_OFICIAL_PADRONIZADA'

// Uma OM sem habilitação só cria NPA; uma habilitada cria também as suas convencionais.
const semHabilitacao = [{ sigla: 'NPA', tipoDeEspecie: COP }]
const comHabilitacao = [{ sigla: 'DCA', tipoDeEspecie: CONV }, { sigla: 'NPA', tipoDeEspecie: COP }]

describe('habilitacao', () => {
  it('o módulo de espécies convencionais só mostra o criar para quem cria alguma convencional', () => {
    expect(podeCriarNoModulo(semHabilitacao, CONV)).toBe(false)
    expect(podeCriarNoModulo(comHabilitacao, CONV)).toBe(true)
  })

  it('toda OM cria NPA: o módulo da NPA sempre mostra o criar', () => {
    expect(podeCriarNoModulo(semHabilitacao, COP)).toBe(true)
    expect(podeCriarNoModulo(comHabilitacao, COP)).toBe(true)
  })

  it('a lista ainda não carregada ou ausente não mostra criar em lugar nenhum (nunca por engano)', () => {
    expect(podeCriarNoModulo([], CONV)).toBe(false)
    expect(podeCriarNoModulo(undefined, COP)).toBe(false)
    expect(podeCriarNoModulo(null, CONV)).toBe(false)
  })

  it('clonar exige poder criar a espécie do documento', () => {
    expect(podeCriarEspecie(comHabilitacao, 'DCA')).toBe(true)
    expect(podeCriarEspecie(comHabilitacao, 'ICA')).toBe(false)
    expect(podeCriarEspecie(semHabilitacao, 'DCA')).toBe(false)
    expect(podeCriarEspecie(semHabilitacao, 'NPA')).toBe(true)
  })

  it('o seletor de espécie do diálogo só oferece as que a OM cria, na ordem original', () => {
    const doSistema = [
      { sigla: 'DCA', tipoDeEspecie: CONV }, { sigla: 'ICA', tipoDeEspecie: CONV }, { sigla: 'NPA', tipoDeEspecie: COP },
    ]
    expect(apenasCriaveis(doSistema, comHabilitacao).map(e => e.sigla)).toEqual(['DCA', 'NPA'])
    expect(apenasCriaveis(doSistema, semHabilitacao).map(e => e.sigla)).toEqual(['NPA'])
    expect(apenasCriaveis(doSistema, [])).toEqual([])
  })
})

describe('habilitação — visão por espécie (tela de administração)', () => {
  const habilitacoes = [
    { omId: 1, especieIds: [10, 11] },
    { omId: 2, especieIds: [] },
    { omId: 3, especieIds: [10] },
  ]

  it('vira "cada espécie com as suas OMs"; espécie sem OM não aparece no mapa', () => {
    expect(omIdsPorEspecie(habilitacoes)).toEqual({ 10: [1, 3], 11: [1] })
    expect(omIdsPorEspecie(undefined)).toEqual({})
  })

  it('o seletor busca a OM por sigla ou nome, sem acento nem caixa', () => {
    const oms = [
      { id: 1, sigla: 'CENDOC', nome: 'Centro de Documentação Aeronáutica' },
      { id: 2, sigla: 'EMAER', nome: 'Estado-Maior da Aeronáutica' },
    ]
    expect(filtrarOpcoesDeOm(oms, 'documentacao').map(o => o.sigla)).toEqual(['CENDOC'])
    expect(filtrarOpcoesDeOm(oms, 'EMA').map(o => o.sigla)).toEqual(['EMAER'])
    expect(filtrarOpcoesDeOm(oms, '')).toHaveLength(2)
    expect(filtrarOpcoesDeOm(undefined, 'x')).toEqual([])
  })

  it('as espécies que uma OM pode criar', () => {
    const mapa = omIdsPorEspecie(habilitacoes)
    expect(especiesDaOm([{ id: 10 }, { id: 11 }, { id: 12 }], mapa, 1).map(e => e.id)).toEqual([10, 11])
    expect(especiesDaOm([{ id: 10 }, { id: 11 }], mapa, 3).map(e => e.id)).toEqual([10])
    expect(especiesDaOm([{ id: 10 }], mapa, 2)).toEqual([])
  })
})
