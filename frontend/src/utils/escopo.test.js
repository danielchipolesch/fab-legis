import { describe, it, expect } from 'vitest'
import { ehOmGeral, temEscopoGeral, omsDoEscopo, papeisConcediveis, podeAdministrar } from './escopo.js'

const GERAL = ['ELB', 'ADM_GER']
const LOCAL = ['ELB', 'ADM_OM']

describe('escopo de acesso', () => {
  it('só o CENDOC é a OM do escopo geral', () => {
    expect(ehOmGeral('CENDOC')).toBe(true)
    expect(ehOmGeral('cendoc')).toBe(true)
    expect(temEscopoGeral('GABAER')).toBe(false)
    expect(temEscopoGeral(null)).toBe(false)
  })

  it('o administrador geral escolhe qualquer OM; o local, só a própria (o do CENDOC também)', () => {
    const oms = [{ id: 1, sigla: 'CENDOC' }, { id: 2, sigla: 'BAGL' }, { id: 3, sigla: 'BABE' }]
    expect(omsDoEscopo(oms, 'CENDOC', GERAL).map(o => o.id)).toEqual([1, 2, 3])
    expect(omsDoEscopo(oms, 'BAGL', LOCAL).map(o => o.id)).toEqual([2])
    expect(omsDoEscopo(oms, 'CENDOC', LOCAL).map(o => o.id)).toEqual([1])
    expect(omsDoEscopo(undefined, 'BAGL', LOCAL)).toEqual([])
  })

  it('o geral só vê o que vale para a OM de destino: os papéis do CENDOC só no CENDOC, o Administrador da OM em qualquer uma', () => {
    expect(papeisConcediveis(GERAL, 'BAGL')).toEqual(['APR_OM', 'PUB_BI', 'AUD_OM', 'PNL_OM', 'ADM_OM'])
    expect(papeisConcediveis(GERAL, 'CENDOC')).toEqual(['APR_OM', 'PUB_BCA', 'PUB_BI', 'AUD_GER', 'PNL_GER', 'PNL_OM', 'ADM_GER', 'ADM_OM'])
  })

  it('o administrador da OM só vê os papéis locais -- o que ele não concede nem aparece', () => {
    expect(papeisConcediveis(LOCAL, 'BAGL')).toEqual(['APR_OM', 'PUB_BI', 'AUD_OM', 'PNL_OM'])
    const proibidos = ['ADM_GER', 'ADM_OM', 'PUB_BCA', 'AUD_GER', 'PNL_GER']
    for (const sigla of ['BAGL', 'CENDOC', undefined]) {
      for (const papel of proibidos) expect(papeisConcediveis(LOCAL, sigla)).not.toContain(papel)
    }
  })

  it('o administrador local do CENDOC concede os locais, menos o Auditor da OM (que não existe no CENDOC)', () => {
    expect(papeisConcediveis(LOCAL, 'CENDOC')).toEqual(['APR_OM', 'PUB_BI', 'PNL_OM'])
  })

  it('o local não administra outros administradores nem outra OM; o do CENDOC, só o CENDOC', () => {
    expect(podeAdministrar(LOCAL, 'BAGL', { omSigla: 'BAGL', papeis: ['ELB'] })).toBe(true)
    expect(podeAdministrar(LOCAL, 'BAGL', { omSigla: 'BAGL', papeis: ['ELB', 'ADM_OM'] })).toBe(false)
    expect(podeAdministrar(LOCAL, 'BAGL', { omSigla: 'BABE', papeis: ['ELB'] })).toBe(false)
    expect(podeAdministrar(LOCAL, 'CENDOC', { omSigla: 'CENDOC', papeis: ['ELB', 'PUB_BCA'] })).toBe(true)
    expect(podeAdministrar(LOCAL, 'CENDOC', { omSigla: 'CENDOC', papeis: ['ELB', 'ADM_GER'] })).toBe(false)
    expect(podeAdministrar(LOCAL, 'CENDOC', { omSigla: 'BAGL', papeis: ['ELB'] })).toBe(false)
    expect(podeAdministrar(GERAL, 'CENDOC', { omSigla: 'BABE', papeis: ['ELB', 'ADM_OM'] })).toBe(true)
  })
})
