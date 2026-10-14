import { describe, it, expect } from 'vitest'
import { motivoDaRevisao, rotuloDoPapel, papeisElevadosDe, temPapelElevado, precisaDeRevisao, contarPendentes } from './revisaoDeAcessos.js'

describe('revisão de acessos (dentro de Manter Usuários)', () => {
  it('cada motivo do backend tem rótulo; desconhecido vale "em dia"', () => {
    expect(motivoDaRevisao('REVISAO_VENCIDA').label).toBe('Revisão vencida')
    expect(motivoDaRevisao('TROCA_DE_OM').label).toBe('Trocou de OM')
    expect(motivoDaRevisao('CONTA_DORMENTE').explicacao).toMatch(/180 dias/)
    expect(motivoDaRevisao('NOVO').label).toBe('Em dia')
    expect(motivoDaRevisao(undefined).label).toBe('Em dia')
  })

  it('rótulos dos papéis elevados', () => {
    expect(rotuloDoPapel('APROV')).toBe('Aprovador')
    expect(rotuloDoPapel('XYZ')).toBe('XYZ')
  })

  it('papéis elevados são todos menos o Editor', () => {
    expect(papeisElevadosDe({ papeis: ['EDIT', 'PUBLIC', 'APROV'] })).toEqual(['APROV', 'PUBLIC'])
    expect(papeisElevadosDe({ papeis: ['EDIT'] })).toEqual([])
    expect(papeisElevadosDe(undefined)).toEqual([])
    expect(temPapelElevado({ papeis: ['EDIT', 'AUDITOR'] })).toBe(true)
    expect(temPapelElevado({ papeis: ['EDIT'] })).toBe(false)
  })

  it('só marca o que quem está logado revisa e que tem motivo', () => {
    expect(precisaDeRevisao({ revisavel: true, motivoDaRevisao: 'REVISAO_VENCIDA' })).toBe(true)
    expect(precisaDeRevisao({ revisavel: true, motivoDaRevisao: 'NENHUM' })).toBe(false)
    expect(precisaDeRevisao({ revisavel: false, motivoDaRevisao: 'REVISAO_VENCIDA' })).toBe(false)
    expect(precisaDeRevisao(undefined)).toBe(false)
  })

  it('conta as pendências', () => {
    const lista = [
      { revisavel: true, motivoDaRevisao: 'TROCA_DE_OM' },
      { revisavel: true, motivoDaRevisao: 'NENHUM' },
      { revisavel: false, motivoDaRevisao: 'REVISAO_VENCIDA' },
      { revisavel: true, motivoDaRevisao: 'CONTA_DORMENTE' },
    ]
    expect(contarPendentes(lista)).toBe(2)
    expect(contarPendentes(undefined)).toBe(0)
  })
})
