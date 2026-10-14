import { describe, it, expect } from 'vitest'
import { opcaoPorMes, opcaoPorEspecie, opcaoSituacaoAtual, opcaoTempos, opcaoPorOm, opcaoFila } from './painelGerencialGraficos.js'

describe('Painel Gerencial (opções dos gráficos)', () => {
  it('por mês: rótulos "mmm/aaaa" e uma série de criados e outra de publicados', () => {
    const o = opcaoPorMes([{ mes: '2026-03', criados: 4, publicados: 1 }, { mes: '2026-04', criados: 2, publicados: 2 }])
    expect(o.xAxis.data).toEqual(['mar/2026', 'abr/2026'])
    expect(o.series.map(s => s.name)).toEqual(['Criados', 'Publicados'])
    expect(o.series[0].data).toEqual([4, 2])
    expect(o.series[1].data).toEqual([1, 2])
  })

  it('por espécie: fatias por sigla e o total no centro', () => {
    const o = opcaoPorEspecie([{ sigla: 'MCA', criados: 3 }, { sigla: 'NPA', criados: 2 }])
    expect(o.series[0].data).toEqual([{ name: 'MCA', value: 3 }, { name: 'NPA', value: 2 }])
    expect(o.title.text).toBe('5')
    expect(opcaoPorEspecie([{ sigla: 'MCA', criados: 1 }]).title.subtext).toBe('documento')
  })

  it('situação atual: a maior fica em cima e o código vira rótulo', () => {
    const o = opcaoSituacaoAtual([{ situacao: 'A', quantidade: 1 }, { situacao: 'B', quantidade: 9 }], s => `rótulo ${s}`)
    expect(o.yAxis.data).toEqual(['rótulo A', 'rótulo B'])
    expect(o.series[0].data).toEqual([1, 9])
  })

  it('tempos: mediana e 90% por etapa; etapa sem dado fica vazia', () => {
    const o = opcaoTempos({ elaboracao: { mediana: 2, p90: 5 }, aprovacao: { mediana: 1, p90: 3 }, publicacao: { mediana: null, p90: null } })
    expect(o.xAxis.data).toEqual(['Elaboração', 'Aprovação', 'Publicação'])
    expect(o.series[0].data).toEqual([2, 1, null])
    expect(o.series[1].data).toEqual([5, 3, null])
  })

  it('por OM: a OM com mais criados fica em cima', () => {
    const o = opcaoPorOm([{ sigla: 'BANT', criados: 5, publicados: 2, emTramitacao: 1 }, { sigla: 'BAFL', criados: 1, publicados: 0, emTramitacao: 1 }])
    expect(o.yAxis.data).toEqual(['BAFL', 'BANT'])
    expect(o.series[0].data).toEqual([1, 5])
  })

  it('fila: o medidor tem escala mínima de 10', () => {
    expect(opcaoFila(3, 4).series[0].max).toBe(10)
    expect(opcaoFila(3, 40).series[0].max).toBe(40)
    expect(opcaoFila(3, 4).series[0].data[0].value).toBe(3)
  })
})
