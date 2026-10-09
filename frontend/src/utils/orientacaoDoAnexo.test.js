import { describe, it, expect } from 'vitest'
import { orientacaoSugerida, orientacaoSugeridaDoArquivo, PAISAGEM, RETRATO } from './orientacaoDoAnexo.js'

// Mesmos cenários de OrientacaoDoAnexoTest (backend): a regra é espelhada nos dois lados.
describe('orientacaoSugerida', () => {
  it('imagem mais larga que alta sugere paisagem', () => {
    expect(orientacaoSugerida(1600, 900)).toBe(PAISAGEM)
    expect(orientacaoSugerida(101, 100)).toBe(PAISAGEM)
  })

  it('imagem mais alta que larga sugere retrato', () => {
    expect(orientacaoSugerida(900, 1600)).toBe(RETRATO)
    expect(orientacaoSugerida(100, 101)).toBe(RETRATO)
  })

  it('imagem quadrada fica em retrato', () => {
    expect(orientacaoSugerida(500, 500)).toBe(RETRATO)
  })

  it('dimensões inválidas ficam em retrato', () => {
    expect(orientacaoSugerida(0, 0)).toBe(RETRATO)
    expect(orientacaoSugerida(-10, -20)).toBe(RETRATO)
    expect(orientacaoSugerida(800, 0)).toBe(RETRATO)
    expect(orientacaoSugerida(0, 600)).toBe(RETRATO)
  })
})

describe('orientacaoSugeridaDoArquivo', () => {
  it('se o arquivo não puder ser lido, fica em retrato', async () => {
    // Em ambiente de teste (node) não há createImageBitmap: o erro é engolido e vale o padrão.
    expect(await orientacaoSugeridaDoArquivo({})).toBe(RETRATO)
  })
})
