import { describe, it, expect } from 'vitest'
import { ocultarCpf, formatarCpf, validarCpf } from './cpf.js'

// CPF é dado pessoal: onde fica visível para o usuário, aparece como no Portal da Transparência
// (3 primeiros e 2 últimos dígitos) -- 111.***.***-35.
describe('ocultarCpf', () => {
  it('mostra só os 3 primeiros e os 2 últimos dígitos', () => {
    expect(ocultarCpf('11144477735')).toBe('111.***.***-35')
  })
  it('aceita o CPF já formatado', () => {
    expect(ocultarCpf('111.444.777-35')).toBe('111.***.***-35')
  })
  it('nunca deixa aparecer os dígitos do meio', () => {
    const oculto = ocultarCpf('11144477735')
    expect(oculto).not.toContain('444')
    expect(oculto).not.toContain('777')
  })
  it('vazio ou nulo devolve texto vazio', () => {
    expect(ocultarCpf('')).toBe('')
    expect(ocultarCpf(null)).toBe('')
    expect(ocultarCpf(undefined)).toBe('')
  })
  it('valor malformado nunca é devolvido: oculta tudo', () => {
    expect(ocultarCpf('12345')).toBe('***.***.***-**')
    expect(ocultarCpf('1234567890123')).toBe('***.***.***-**')
  })
})

describe('cpf (funções que continuam valendo para digitação/validação)', () => {
  it('formata e valida', () => {
    expect(formatarCpf('11144477735')).toBe('111.444.777-35')
    expect(validarCpf('11144477735')).toBe(true)
  })
})
