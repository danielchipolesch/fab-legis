import { describe, it, expect } from 'vitest'
import { PAPEIS, nomeDoPapel, ehAdmin, ehAdminGeral, ehAuditor, ehGestor, ehGestorGeral, papelDeAdmin, papelDeAuditor, papelDeGestor, papeisElevados } from './papeis.js'

describe('perfis (perfis)', () => {
  it('cada perfil tem código e nome por extenso (o mesmo de PapelEnum e da tabela t_papel)', () => {
    expect(Object.keys(PAPEIS).sort()).toEqual(['ADM_GER', 'ADM_OM', 'APR_OM', 'AUD_GER', 'AUD_OM', 'ELB', 'GES_GER', 'GES_OM', 'PUB_BCA'])
    expect(nomeDoPapel('ELB')).toBe('Elaborador')
    expect(nomeDoPapel('APR_OM')).toBe('Aprovador da OM')
    expect(nomeDoPapel('PUB_BCA')).toBe('Publicador do BCA')
    expect(nomeDoPapel('AUD_OM')).toBe('Auditor da OM')
    expect(nomeDoPapel('AUD_GER')).toBe('Auditor Geral')
    expect(nomeDoPapel('ADM_OM')).toBe('Administrador da OM')
    expect(nomeDoPapel('ADM_GER')).toBe('Administrador Geral')
    expect(nomeDoPapel('GES_OM')).toBe('Gestor da OM')
    expect(nomeDoPapel('GES_GER')).toBe('Gestor Geral')
    expect(nomeDoPapel('XYZ')).toBe('XYZ')
    for (const [codigo, papel] of Object.entries(PAPEIS)) expect(papel.codigo).toBe(codigo)
  })

  it('administrador e auditor, de qualquer escopo', () => {
    expect(ehAdmin(['ELB', 'ADM_OM'])).toBe(true)
    expect(ehAdmin(['ELB', 'ADM_GER'])).toBe(true)
    expect(ehAdmin(['ELB', 'APR_OM'])).toBe(false)
    expect(ehAdminGeral(['ADM_GER'])).toBe(true)
    expect(ehAdminGeral(['ADM_OM'])).toBe(false)
    expect(ehAuditor(['AUD_OM'])).toBe(true)
    expect(ehAuditor(['AUD_GER'])).toBe(true)
    expect(ehAuditor(undefined)).toBe(false)
  })

  it('o perfil de administrador e de auditor depende da lotação', () => {
    expect(papelDeAdmin('CENDOC')).toBe('ADM_GER')
    expect(papelDeAdmin('cendoc')).toBe('ADM_GER')
    expect(papelDeAdmin('BAGL')).toBe('ADM_OM')
    expect(papelDeAdmin(null)).toBe('ADM_OM')
    expect(papelDeAuditor('CENDOC')).toBe('AUD_GER')
    expect(papelDeAuditor('BAGL')).toBe('AUD_OM')
  })

  it('perfis elevados são todos menos o Elaborador', () => {
    expect(papeisElevados(['ELB', 'PUB_BCA', 'APR_OM'])).toEqual(['APR_OM', 'PUB_BCA'])
    expect(papeisElevados(['ELB'])).toEqual([])
    expect(papeisElevados(undefined)).toEqual([])
  })
})

describe('Gestor (Painel Gerencial)', () => {
  it('o perfil de gestor depende da lotação', () => {
    expect(papelDeGestor('CENDOC')).toBe('GES_GER')
    expect(papelDeGestor('BAGL')).toBe('GES_OM')
  })

  it('gestor de qualquer escopo e o geral', () => {
    expect(ehGestor(['ELB', 'GES_OM'])).toBe(true)
    expect(ehGestor(['ELB', 'GES_GER'])).toBe(true)
    expect(ehGestor(['ELB', 'ADM_GER', 'AUD_GER'])).toBe(false)
    expect(ehGestorGeral(['GES_GER'])).toBe(true)
    expect(ehGestorGeral(['GES_OM'])).toBe(false)
  })
})
