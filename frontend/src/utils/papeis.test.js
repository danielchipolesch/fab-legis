import { describe, it, expect } from 'vitest'
import { PAPEIS, nomeDoPapel, ehAdmin, ehAdminGeral, ehAuditor, ehPublicador, ehPainelGerencial, ehPainelGerencialGeral, papelDeAdmin, papelDeAuditor, papelDoPainelGerencial, papeisElevados } from './papeis.js'

describe('papéis', () => {
  it('cada papel tem código e nome por extenso (o mesmo de PapelEnum e da tabela t_papel)', () => {
    expect(Object.keys(PAPEIS).sort()).toEqual(['ADM_GER', 'ADM_OM', 'APR_OM', 'AUD_GER', 'AUD_OM', 'ELB', 'PNL_GER', 'PNL_OM', 'PUB_BCA', 'PUB_BI'])
    expect(nomeDoPapel('ELB')).toBe('Elaborador')
    expect(nomeDoPapel('APR_OM')).toBe('Aprovador da OM')
    expect(nomeDoPapel('PUB_BCA')).toBe('Publicador do BCA')
    expect(nomeDoPapel('PUB_BI')).toBe('Publicador do Boletim Interno')
    expect(nomeDoPapel('AUD_OM')).toBe('Auditor da OM')
    expect(nomeDoPapel('AUD_GER')).toBe('Auditor Geral')
    expect(nomeDoPapel('ADM_OM')).toBe('Administrador da OM')
    expect(nomeDoPapel('ADM_GER')).toBe('Administrador Geral')
    expect(nomeDoPapel('PNL_OM')).toBe('Painel Gerencial da OM')
    expect(nomeDoPapel('PNL_GER')).toBe('Painel Gerencial Geral')
    expect(nomeDoPapel('XYZ')).toBe('XYZ')
    for (const [codigo, papel] of Object.entries(PAPEIS)) expect(papel.codigo).toBe(codigo)
  })

  it('publica quem tem o papel do BCA (convencionais) ou o do Boletim Interno (NPA da própria OM)', () => {
    expect(ehPublicador(['ELB', 'PUB_BCA'])).toBe(true)
    expect(ehPublicador(['ELB', 'PUB_BI'])).toBe(true)
    expect(ehPublicador(['ELB', 'APR_OM', 'ADM_GER'])).toBe(false)
    expect(ehPublicador(undefined)).toBe(false)
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

  it('o papel de administrador e de auditor depende da lotação', () => {
    expect(papelDeAdmin('CENDOC')).toBe('ADM_GER')
    expect(papelDeAdmin('cendoc')).toBe('ADM_GER')
    expect(papelDeAdmin('BAGL')).toBe('ADM_OM')
    expect(papelDeAdmin(null)).toBe('ADM_OM')
    expect(papelDeAuditor('CENDOC')).toBe('AUD_GER')
    expect(papelDeAuditor('BAGL')).toBe('AUD_OM')
  })

  it('papéis elevados são todos menos o Elaborador', () => {
    expect(papeisElevados(['ELB', 'PUB_BCA', 'APR_OM'])).toEqual(['APR_OM', 'PUB_BCA'])
    expect(papeisElevados(['ELB'])).toEqual([])
    expect(papeisElevados(undefined)).toEqual([])
  })
})

describe('Papéis do Painel Gerencial', () => {
  it('o papel do Painel Gerencial depende da lotação', () => {
    expect(papelDoPainelGerencial('CENDOC')).toBe('PNL_GER')
    expect(papelDoPainelGerencial('BAGL')).toBe('PNL_OM')
  })

  it('o papel do Painel Gerencial, de qualquer escopo, e o geral', () => {
    expect(ehPainelGerencial(['ELB', 'PNL_OM'])).toBe(true)
    expect(ehPainelGerencial(['ELB', 'PNL_GER'])).toBe(true)
    expect(ehPainelGerencial(['ELB', 'ADM_GER', 'AUD_GER'])).toBe(false)
    expect(ehPainelGerencialGeral(['PNL_GER'])).toBe(true)
    expect(ehPainelGerencialGeral(['PNL_OM'])).toBe(false)
  })
})
