import { describe, it, expect } from 'vitest'
import { paginar } from './paginacaoDaNpa.js'

describe('paginar (prévia da NPA)', () => {
  it('cabe tudo numa folha só', () => {
    expect(paginar([100, 100, 100], { primeira: 500, demais: 800 })).toEqual([[0, 1, 2]])
  })

  it('sem blocos ainda há uma folha', () => {
    expect(paginar([], { primeira: 500, demais: 800 })).toEqual([[]])
  })

  it('a primeira folha tem menos espaço (leva o cabeçalho) que as demais', () => {
    // primeira = 250: cabem dois de 100; o terceiro vai para a segunda folha, que tem 800.
    expect(paginar([100, 100, 100, 100], { primeira: 250, demais: 800 })).toEqual([[0, 1], [2, 3]])
  })

  it('um bloco que não cabe no que sobra vai inteiro para a folha seguinte', () => {
    expect(paginar([300, 300, 300], { primeira: 700, demais: 700 })).toEqual([[0, 1], [2]])
  })

  it('o bloco que enche a folha exatamente ainda fica nela', () => {
    expect(paginar([400, 400, 10], { primeira: 800, demais: 800 })).toEqual([[0, 1], [2]])
  })

  it('título fica junto do próximo bloco, mesmo que sozinho ele coubesse', () => {
    // O texto (0) ocupa 200; o título (1) ainda caberia nos 150 que sobram, mas o parágrafo dele (2) não: título e parágrafo
    // vão juntos para a folha seguinte.
    expect(paginar([200, 100, 100], { primeira: 350, demais: 800, juntoComOProximo: [false, true, false] }))
      .toEqual([[0], [1, 2]])
  })

  it('a cadeia de "junto com o próximo" vira um só grupo (fecho e assinatura)', () => {
    const r = paginar([200, 100, 100, 100], { primeira: 350, demais: 800, juntoComOProximo: [false, true, false, false] })
    // 0 ocupa 200; o grupo [1, 2] (200) não cabe nos 150 que sobram: vai junto para a segunda folha.
    expect(r).toEqual([[0], [1, 2, 3]])
  })

  it('um bloco mais alto que a folha ocupa uma folha sozinho', () => {
    expect(paginar([100, 2000, 100], { primeira: 500, demais: 800 })).toEqual([[0], [1], [2]])
  })

  it('o primeiro bloco fica na primeira folha mesmo que não caiba nela', () => {
    expect(paginar([600, 100], { primeira: 300, demais: 800 })).toEqual([[0], [1]])
  })
})
