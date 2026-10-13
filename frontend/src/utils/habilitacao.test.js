import { describe, it, expect } from 'vitest'
import { podeCriarNoModulo, podeCriarEspecie, apenasCriaveis, filtrarOms } from './habilitacao.js'

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

  it('a tela de administração busca a OM por sigla ou nome, sem acento nem caixa, e filtra as habilitadas', () => {
    const oms = [
      { omId: 1, sigla: 'CENDOC', nome: 'Centro de Documentação Aeronáutica', especieIds: [1, 2] },
      { omId: 2, sigla: 'EMAER', nome: 'Estado-Maior da Aeronáutica', especieIds: [] },
      { omId: 3, sigla: 'GABAER', nome: 'Gabinete do Comandante da Aeronáutica', especieIds: [1] },
    ]
    expect(filtrarOms(oms, 'documentacao', false).map(o => o.sigla)).toEqual(['CENDOC'])
    expect(filtrarOms(oms, 'GAB', false).map(o => o.sigla)).toEqual(['GABAER'])
    expect(filtrarOms(oms, 'aeronautica', false)).toHaveLength(3)
    expect(filtrarOms(oms, '', true).map(o => o.sigla)).toEqual(['CENDOC', 'GABAER'])
    expect(filtrarOms(oms, 'emaer', true)).toEqual([])
    expect(filtrarOms(undefined, 'x', false)).toEqual([])
  })
})
