import { describe, it, expect } from 'vitest'
import { htmlEmLinha } from './htmlEmLinha.js'

describe('htmlEmLinha', () => {
  it('mantém a cor do texto de orientação entre colchetes (textStyle do TipTap)', () => {
    const html = '<p>Assunto tratado: <span style="color: #FF0000">[descrever o assunto]</span>.</p>'
    expect(htmlEmLinha(html)).toBe('Assunto tratado: <span style="color: #FF0000">[descrever o assunto]</span>.')
  })

  it('mantém negrito, itálico, sublinhado e realce', () => {
    const html = '<p><strong>a</strong> <em>b</em> <u>c</u> <mark data-color="#FFD600">d</mark></p>'
    expect(htmlEmLinha(html)).toBe('<strong>a</strong> <em>b</em> <u>c</u> <mark data-color="#FFD600">d</mark>')
  })

  it('remove o <p> mesmo com atributos (alinhamento)', () => {
    expect(htmlEmLinha('<p style="text-align: justify">texto</p>')).toBe('texto')
  })

  it('junta vários parágrafos sem separador, como o texto puro fazia', () => {
    expect(htmlEmLinha('<p>um</p><p>dois</p>')).toBe('umdois')
  })

  it('mantém a quebra de linha (hardBreak) dentro do parágrafo', () => {
    expect(htmlEmLinha('<p>linha 1<br>linha 2</p>')).toBe('linha 1<br>linha 2')
  })

  it('não mexe em outras tags que começam com "p" nem em entidades escapadas', () => {
    expect(htmlEmLinha('<p>a &lt;b&gt; c</p>')).toBe('a &lt;b&gt; c')
    expect(htmlEmLinha('<pre>x</pre>')).toBe('<pre>x</pre>')
  })

  it('devolve vazio para conteúdo ausente', () => {
    expect(htmlEmLinha('')).toBe('')
    expect(htmlEmLinha(null)).toBe('')
    expect(htmlEmLinha(undefined)).toBe('')
  })
})
