import { describe, it, expect } from 'vitest'
import { rotaBuscaConteudo, termoDaQuery } from './buscaTextual.js'

describe('rotaBuscaConteudo', () => {
  it('leva o termo digitado na query', () => {
    expect(rotaBuscaConteudo('licença')).toEqual({ name: 'busca', query: { q: 'licença' } })
  })
  it('remove espaços das pontas', () => {
    expect(rotaBuscaConteudo('  pagamento de pessoal  ')).toEqual({ name: 'busca', query: { q: 'pagamento de pessoal' } })
  })
  it('sem termo, abre a busca vazia (sem query)', () => {
    expect(rotaBuscaConteudo('')).toEqual({ name: 'busca' })
    expect(rotaBuscaConteudo('   ')).toEqual({ name: 'busca' })
    expect(rotaBuscaConteudo(null)).toEqual({ name: 'busca' })
    expect(rotaBuscaConteudo(undefined)).toEqual({ name: 'busca' })
  })
  it('preserva aspas e o operador de exclusão da própria busca', () => {
    expect(rotaBuscaConteudo('"frase exata" -excluir').query.q).toBe('"frase exata" -excluir')
  })
})

describe('termoDaQuery', () => {
  it('lê o termo da URL', () => {
    expect(termoDaQuery({ q: 'licença' })).toBe('licença')
  })
  it('sem termo ou vazio devolve null', () => {
    expect(termoDaQuery({})).toBeNull()
    expect(termoDaQuery({ q: '   ' })).toBeNull()
    expect(termoDaQuery(undefined)).toBeNull()
  })
  it('parâmetro repetido usa o primeiro', () => {
    expect(termoDaQuery({ q: ['a', 'b'] })).toBe('a')
  })
})
