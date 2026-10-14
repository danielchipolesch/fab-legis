import { describe, it, expect } from 'vitest'
import { acaoDeAcessoLabel, acaoDeAcessoCor, atorDoAcesso, ACAO_DE_ACESSO } from './auditoriaDeAcessos.js'

describe('auditoria de acessos', () => {
  it('tem rótulo e cor para cada ação do backend', () => {
    for (const acao of ['CRIOU_USUARIO', 'ALTEROU_PAPEIS', 'ATIVOU_USUARIO', 'DESATIVOU_USUARIO', 'TROCOU_OM', 'REDEFINIU_SENHA', 'REVISOU_ACESSO']) {
      expect(ACAO_DE_ACESSO[acao]).toBeTruthy()
    }
    expect(acaoDeAcessoLabel('ALTEROU_PAPEIS')).toBe('Alterou papéis')
    expect(acaoDeAcessoLabel('NOVA')).toBe('NOVA')
    expect(acaoDeAcessoCor('NOVA')).toBe('grey')
  })

  it('sem ator, quem fez foi o sistema', () => {
    expect(atorDoAcesso({ atorNome: null })).toBe('Sistema')
    expect(atorDoAcesso({ atorNome: 'Fulano' })).toBe('Fulano')
  })
})
