import { describe, it, expect } from 'vitest'
import { PERFIS, nomeDoPerfil, ehAdmin, ehAdminGeral, ehAuditor, ehPainelGerencial, ehPainelGerencialGeral, perfilDeAdmin, perfilDeAuditor, perfilDoPainelGerencial, perfisElevados } from './perfis.js'

describe('perfis', () => {
  it('cada perfil tem código e nome por extenso (o mesmo de PerfilEnum e da tabela t_perfil)', () => {
    expect(Object.keys(PERFIS).sort()).toEqual(['ADM_GER', 'ADM_OM', 'APR_OM', 'AUD_GER', 'AUD_OM', 'ELB', 'PNL_GER', 'PNL_OM', 'PUB_BCA'])
    expect(nomeDoPerfil('ELB')).toBe('Elaborador')
    expect(nomeDoPerfil('APR_OM')).toBe('Aprovador da OM')
    expect(nomeDoPerfil('PUB_BCA')).toBe('Publicador do BCA')
    expect(nomeDoPerfil('AUD_OM')).toBe('Auditor da OM')
    expect(nomeDoPerfil('AUD_GER')).toBe('Auditor Geral')
    expect(nomeDoPerfil('ADM_OM')).toBe('Administrador da OM')
    expect(nomeDoPerfil('ADM_GER')).toBe('Administrador Geral')
    expect(nomeDoPerfil('PNL_OM')).toBe('Painel Gerencial da OM')
    expect(nomeDoPerfil('PNL_GER')).toBe('Painel Gerencial Geral')
    expect(nomeDoPerfil('XYZ')).toBe('XYZ')
    for (const [codigo, perfil] of Object.entries(PERFIS)) expect(perfil.codigo).toBe(codigo)
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
    expect(perfilDeAdmin('CENDOC')).toBe('ADM_GER')
    expect(perfilDeAdmin('cendoc')).toBe('ADM_GER')
    expect(perfilDeAdmin('BAGL')).toBe('ADM_OM')
    expect(perfilDeAdmin(null)).toBe('ADM_OM')
    expect(perfilDeAuditor('CENDOC')).toBe('AUD_GER')
    expect(perfilDeAuditor('BAGL')).toBe('AUD_OM')
  })

  it('perfis elevados são todos menos o Elaborador', () => {
    expect(perfisElevados(['ELB', 'PUB_BCA', 'APR_OM'])).toEqual(['APR_OM', 'PUB_BCA'])
    expect(perfisElevados(['ELB'])).toEqual([])
    expect(perfisElevados(undefined)).toEqual([])
  })
})

describe('Perfis do Painel Gerencial', () => {
  it('o perfil do Painel Gerencial depende da lotação', () => {
    expect(perfilDoPainelGerencial('CENDOC')).toBe('PNL_GER')
    expect(perfilDoPainelGerencial('BAGL')).toBe('PNL_OM')
  })

  it('o perfil do Painel Gerencial, de qualquer escopo, e o geral', () => {
    expect(ehPainelGerencial(['ELB', 'PNL_OM'])).toBe(true)
    expect(ehPainelGerencial(['ELB', 'PNL_GER'])).toBe(true)
    expect(ehPainelGerencial(['ELB', 'ADM_GER', 'AUD_GER'])).toBe(false)
    expect(ehPainelGerencialGeral(['PNL_GER'])).toBe(true)
    expect(ehPainelGerencialGeral(['PNL_OM'])).toBe(false)
  })
})
