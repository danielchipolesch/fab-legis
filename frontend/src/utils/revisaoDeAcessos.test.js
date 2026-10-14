import { describe, it, expect } from 'vitest'
import { motivoDaRevisao, rotuloDoPerfil, perfisElevadosDe, temPerfilElevado, precisaDeRevisao, contarPendentes } from './revisaoDeAcessos.js'

describe('revisão de acessos (dentro de Manter Usuários)', () => {
  it('cada motivo do backend tem rótulo; desconhecido vale "em dia"', () => {
    expect(motivoDaRevisao('REVISAO_VENCIDA').label).toBe('Revisão vencida')
    expect(motivoDaRevisao('TROCA_DE_OM').label).toBe('Trocou de OM')
    expect(motivoDaRevisao('CONTA_DORMENTE').explicacao).toMatch(/180 dias/)
    expect(motivoDaRevisao('NOVO').label).toBe('Em dia')
    expect(motivoDaRevisao(undefined).label).toBe('Em dia')
  })

  it('rótulos dos perfis elevados', () => {
    expect(rotuloDoPerfil('APR_OM')).toBe('Aprovador da OM')
    expect(rotuloDoPerfil('XYZ')).toBe('XYZ')
  })

  it('perfis elevados são todos menos o Editor', () => {
    expect(perfisElevadosDe({ perfis: ['ELB', 'PUB_BCA', 'APR_OM'] })).toEqual(['APR_OM', 'PUB_BCA'])
    expect(perfisElevadosDe({ perfis: ['ELB'] })).toEqual([])
    expect(perfisElevadosDe(undefined)).toEqual([])
    expect(temPerfilElevado({ perfis: ['ELB', 'AUD_OM'] })).toBe(true)
    expect(temPerfilElevado({ perfis: ['ELB'] })).toBe(false)
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
