import { describe, it, expect } from 'vitest'
import { colchetesEmTexto, destacarColchetes, COR_TEXTO_MODELO } from './textoModelo.js'

describe('destacarColchetes (HTML)', () => {
  it('o trecho entre colchetes fica em vermelho, colchetes incluídos', () => {
    const html = destacarColchetes('<p>Esta ICA tem por finalidade [descrever a finalidade].</p>')
    expect(html).toContain(`<span class="texto-modelo" style="color:${COR_TEXTO_MODELO}">[descrever a finalidade]</span>`)
    expect(html.startsWith('<p>Esta ICA tem por finalidade ')).toBe(true)
    expect(html.endsWith('.</p>')).toBe(true)
  })

  it('vários trechos no mesmo texto', () => {
    const html = destacarColchetes('<p>[a] e [b]</p>')
    expect(html.match(/texto-modelo/g)).toHaveLength(2)
  })

  it('sem colchetes o HTML não muda', () => {
    const original = '<p>Texto <strong>normal</strong>.</p>'
    expect(destacarColchetes(original)).toBe(original)
  })

  it('colchete sem par não é destacado', () => {
    expect(destacarColchetes('<p>abre [ e nunca fecha</p>')).not.toContain('texto-modelo')
    expect(destacarColchetes('<p>só fecha ]</p>')).not.toContain('texto-modelo')
  })

  it('não mexe em atributos de tags que contenham colchetes', () => {
    const original = '<p data-x="[a]">texto</p>'
    expect(destacarColchetes(original)).toBe(original)
  })

  it('preserva a formatação ao redor (negrito)', () => {
    const html = destacarColchetes('<p><strong>[indicar]</strong> algo</p>')
    expect(html).toContain('<strong><span class="texto-modelo"')
  })

  it('texto que já tem cor própria do autor não é sobrescrito', () => {
    const original = '<p><span style="color: #0000FF">[azul de propósito]</span></p>'
    expect(destacarColchetes(original)).toBe(original)
  })

  it('depois de fechar um span com cor, volta a destacar', () => {
    const html = destacarColchetes('<p><span style="color:#00F">x</span> [y]</p>')
    expect(html).toContain('<span class="texto-modelo"')
  })

  it('vazio ou nulo', () => {
    expect(destacarColchetes('')).toBe('')
    expect(destacarColchetes(null)).toBe('')
  })
})

describe('colchetesEmTexto (texto puro)', () => {
  it('escapa o HTML e destaca os colchetes', () => {
    const html = colchetesEmTexto('a <b> [c] & d')
    expect(html).toContain('a &lt;b&gt; ')
    expect(html).toContain('>[c]</span>')
    expect(html).toContain('&amp; d')
  })
})
