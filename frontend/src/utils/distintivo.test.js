import { describe, it, expect } from 'vitest'
import { erroDoDistintivo, filtrarOmsDoDistintivo } from './distintivo.js'

describe('erroDoDistintivo', () => {
  it('aceita PNG e JPEG de até 1 MB', () => {
    expect(erroDoDistintivo({ type: 'image/png', size: 1000 })).toBeNull()
    expect(erroDoDistintivo({ type: 'image/jpeg', size: 1024 * 1024 })).toBeNull()
  })
  it('recusa outro formato, vazio e acima de 1 MB', () => {
    expect(erroDoDistintivo({ type: 'image/gif', size: 10 })).toMatch(/PNG ou JPEG/)
    expect(erroDoDistintivo({ type: 'application/pdf', size: 10 })).toMatch(/PNG ou JPEG/)
    expect(erroDoDistintivo({ type: 'image/png', size: 0 })).toMatch(/vazio/)
    expect(erroDoDistintivo({ type: 'image/png', size: 1024 * 1024 + 1 })).toMatch(/1 MB/)
    expect(erroDoDistintivo(null)).toMatch(/PNG ou JPEG/)
  })
})

describe('filtrarOmsDoDistintivo', () => {
  const oms = [
    { id: 1, sigla: 'CENDOC', nome: 'Centro de Documentação', urlDistintivo: 'http://x/a.png' },
    { id: 2, sigla: 'GABAER', nome: 'Gabinete do Comandante', urlDistintivo: null },
  ]
  it('filtra por sigla ou nome, sem distinguir maiúsculas', () => {
    expect(filtrarOmsDoDistintivo(oms, 'gab', false).map(o => o.id)).toEqual([2])
    expect(filtrarOmsDoDistintivo(oms, 'documentação', false).map(o => o.id)).toEqual([1])
  })
  it('só as que têm distintivo, quando pedido', () => {
    expect(filtrarOmsDoDistintivo(oms, '', true).map(o => o.id)).toEqual([1])
    expect(filtrarOmsDoDistintivo(oms, '', false)).toHaveLength(2)
  })
})
