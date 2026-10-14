import { describe, it, expect } from 'vitest'
import { motivoDaRevisao, rotuloDoPapel, contarPendentes, textoDeQuemRevisa } from './revisaoDeAcessos.js'

describe('revisão de acessos', () => {
  it('cada motivo do backend tem rótulo; desconhecido vale "em dia"', () => {
    expect(motivoDaRevisao('REVISAO_VENCIDA').label).toBe('Revisão vencida')
    expect(motivoDaRevisao('TROCA_DE_OM').label).toBe('Trocou de OM')
    expect(motivoDaRevisao('CONTA_DORMENTE').explicacao).toMatch(/180 dias/)
    expect(motivoDaRevisao('NOVO').label).toBe('Em dia')
  })
  it('rótulos dos papéis elevados', () => {
    expect(rotuloDoPapel('APROV')).toBe('Aprovador')
    expect(rotuloDoPapel('XYZ')).toBe('XYZ')
  })
  it('conta as pendências', () => {
    expect(contarPendentes([{ pendente: true }, { pendente: false }, { pendente: true }])).toBe(2)
    expect(contarPendentes(undefined)).toBe(0)
  })
  it('explica quem o revisor confere', () => {
    expect(textoDeQuemRevisa(true)).toMatch(/administradores locais/)
    expect(textoDeQuemRevisa(false)).toMatch(/da sua OM/)
  })
})
