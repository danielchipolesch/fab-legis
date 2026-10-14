import { describe, it, expect } from 'vitest'
import { ehOmGeral, temEscopoGeral, omsDoEscopo, papeisConcediveis, podeAdministrar } from './escopo.js'

describe('escopo de acesso', () => {
  it('só o CENDOC tem escopo geral', () => {
    expect(ehOmGeral('CENDOC')).toBe(true)
    expect(ehOmGeral('cendoc')).toBe(true)
    expect(temEscopoGeral('GABAER')).toBe(false)
    expect(temEscopoGeral(null)).toBe(false)
  })

  it('o admin local só escolhe a própria OM; o geral, todas', () => {
    const oms = [{ id: 1, sigla: 'CENDOC' }, { id: 2, sigla: 'BAGL' }, { id: 3, sigla: 'BABE' }]
    expect(omsDoEscopo(oms, 'CENDOC').map(o => o.id)).toEqual([1, 2, 3])
    expect(omsDoEscopo(oms, 'BAGL').map(o => o.id)).toEqual([2])
    expect(omsDoEscopo(undefined, 'BAGL')).toEqual([])
  })

  it('o local concede aprovador e auditor; admin só o geral; publicador só no CENDOC', () => {
    expect(papeisConcediveis('BAGL', 'BAGL')).toEqual({ elaborador: true, aprovador: true, auditor: true, gestor: true, admin: false, publicador: false })
    expect(papeisConcediveis('CENDOC', 'BAGL')).toEqual({ elaborador: true, aprovador: true, auditor: true, gestor: true, admin: true, publicador: false })
    expect(papeisConcediveis('CENDOC', 'CENDOC').publicador).toBe(true)
    expect(papeisConcediveis('BAGL', 'CENDOC').publicador).toBe(false)
  })

  it('o local não administra outros administradores nem outra OM', () => {
    expect(podeAdministrar('BAGL', { omSigla: 'BAGL', papeis: ['ELB'] })).toBe(true)
    expect(podeAdministrar('BAGL', { omSigla: 'BAGL', papeis: ['ELB', 'ADM_OM'] })).toBe(false)
    expect(podeAdministrar('BAGL', { omSigla: 'BABE', papeis: ['ELB'] })).toBe(false)
    expect(podeAdministrar('CENDOC', { omSigla: 'BABE', papeis: ['ELB', 'ADM_OM'] })).toBe(true)
  })
})
