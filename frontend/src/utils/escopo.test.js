import { describe, it, expect } from 'vitest'
import { ehOmGeral, temEscopoGeral, omsDoEscopo, perfisConcediveis, podeAdministrar } from './escopo.js'

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
    expect(perfisConcediveis('BAGL', 'BAGL')).toEqual({ elaborador: true, aprovador: true, auditor: true, painel: true, admin: false, publicador: false })
    expect(perfisConcediveis('CENDOC', 'BAGL')).toEqual({ elaborador: true, aprovador: true, auditor: true, painel: true, admin: true, publicador: false })
    expect(perfisConcediveis('CENDOC', 'CENDOC').publicador).toBe(true)
    expect(perfisConcediveis('BAGL', 'CENDOC').publicador).toBe(false)
  })

  it('o local não administra outros administradores nem outra OM', () => {
    expect(podeAdministrar('BAGL', { omSigla: 'BAGL', perfis: ['ELB'] })).toBe(true)
    expect(podeAdministrar('BAGL', { omSigla: 'BAGL', perfis: ['ELB', 'ADM_OM'] })).toBe(false)
    expect(podeAdministrar('BAGL', { omSigla: 'BABE', perfis: ['ELB'] })).toBe(false)
    expect(podeAdministrar('CENDOC', { omSigla: 'BABE', perfis: ['ELB', 'ADM_OM'] })).toBe(true)
  })
})
