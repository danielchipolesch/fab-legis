import { describe, it, expect } from 'vitest'
import {
  CORES_DE_TABELA, ESTILOS_DE_TABELA, estiloValido, corValida, alinhamentoVerticalValido, fundoValido, temTitulo,
} from './estiloDeTabela.js'

describe('estiloDeTabela', () => {
  it('reconhece só os estilos, cores e alinhamentos da lista fechada; o resto vira o padrão', () => {
    expect(estiloValido('zebrado')).toBe('zebrado')
    expect(estiloValido('neon')).toBe('padrao')
    expect(estiloValido(undefined)).toBe('padrao')
    expect(corValida('vinho')).toBe('vinho')
    expect(corValida('rosa')).toBe('azul')
    expect(alinhamentoVerticalValido('middle')).toBe('middle')
    expect(alinhamentoVerticalValido('baseline')).toBe('top')
  })

  it('o fundo da célula só aceita #RRGGBB, em minúsculas', () => {
    expect(fundoValido('#FFD600')).toBe('#ffd600')
    expect(fundoValido('red')).toBeNull()
    expect(fundoValido('#fff')).toBeNull()
    expect(fundoValido('#ffd600; background:url(x)')).toBeNull()
    expect(fundoValido(null)).toBeNull()
  })

  it('só uma tabela com título tem legenda numerada', () => {
    expect(temTitulo('Efetivo por posto')).toBe(true)
    expect(temTitulo('   ')).toBe(false)
    expect(temTitulo('')).toBe(false)
    expect(temTitulo(null)).toBe(false)
  })

  it('a paleta tem a cor padrão e cada cor tem cabeçalho e borda', () => {
    expect(CORES_DE_TABELA.map(c => c.valor)).toEqual(['azul', 'cinza', 'verde', 'vinho', 'sem'])
    expect(ESTILOS_DE_TABELA.map(e => e.valor)).toEqual(['padrao', 'zebrado', 'compacto', 'aberto'])
    for (const c of CORES_DE_TABELA) {
      expect(c.cabecalho).toBeTruthy()
      expect(c.borda).toMatch(/^#[0-9a-f]{6}$/)
    }
  })
})
