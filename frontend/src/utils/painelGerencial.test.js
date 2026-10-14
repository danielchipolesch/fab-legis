import { describe, it, expect } from 'vitest'
import { percentual, larguraDaBarra, maiorValor, rotuloDaEtapa, formatarDias, rotuloDoMes, periodoPadrao } from './painelGerencial.js'

describe('Painel Gerencial (apresentação)', () => {
  it('a largura da barra é proporcional ao maior valor', () => {
    expect(percentual(5, 10)).toBe(50)
    expect(percentual(10, 10)).toBe(100)
    expect(percentual(0, 10)).toBe(0)
    expect(percentual(3, 0)).toBe(0)
    expect(percentual(undefined, 10)).toBe(0)
    expect(percentual(30, 10)).toBe(100)
  })

  it('o maior valor de uma série', () => {
    expect(maiorValor([{ n: 3 }, { n: 9 }, { n: 4 }], 'n')).toBe(9)
    expect(maiorValor([], 'n')).toBe(0)
    expect(maiorValor(undefined, 'n')).toBe(0)
  })

  it('rótulos das etapas, dos meses e dos dias', () => {
    expect(rotuloDaEtapa('APROVACAO')).toBe('Aprovação')
    expect(rotuloDaEtapa('PUBLICACAO')).toBe('Publicação')
    expect(rotuloDoMes('2026-03')).toBe('mar/2026')
    expect(rotuloDoMes('2026-12')).toBe('dez/2026')
    expect(rotuloDoMes('xx')).toBe('xx')
    expect(formatarDias(null)).toBe('—')
    expect(formatarDias(1)).toBe('1 dia')
    expect(formatarDias(6.5)).toBe('6,5 dias')
    expect(formatarDias(0)).toBe('0 dias')
  })

  it('o período padrão são os últimos 12 meses até hoje', () => {
    expect(periodoPadrao(new Date(2026, 9, 13))).toEqual({ dataInicio: '2025-10-13', dataFim: '2026-10-13' })
  })
})

describe('larguraDaBarra', () => {
  it('tem mínimo visível quando há valor e some quando não há', () => {
    expect(larguraDaBarra(1, 1000)).toBe(2)
    expect(larguraDaBarra(0, 1000)).toBe(0)
    expect(larguraDaBarra(500, 1000)).toBe(50)
    expect(larguraDaBarra(5, 0)).toBe(0)
  })
})
