import { describe, it, expect } from 'vitest'
import { emailInstitucional } from './email.js'

describe('emailInstitucional', () => {
  it('aceita e-mail do domínio fab.mil.br, sem distinguir maiúsculas nem espaços nas pontas', () => {
    expect(emailInstitucional('fulano@fab.mil.br')).toBe(true)
    expect(emailInstitucional('  Fulano.Silva@FAB.MIL.BR ')).toBe(true)
  })
  it('recusa outros domínios, subdomínios e sufixos enganosos', () => {
    expect(emailInstitucional('fulano@gmail.com')).toBe(false)
    expect(emailInstitucional('fulano@mail.fab.mil.br')).toBe(false)
    expect(emailInstitucional('fulano@fab.mil.br.exemplo.com')).toBe(false)
    expect(emailInstitucional('fulano@xfab.mil.br')).toBe(false)
    expect(emailInstitucional('@fab.mil.br')).toBe(false)
  })
  it('vazio é válido (e-mail é opcional)', () => {
    expect(emailInstitucional('')).toBe(true)
    expect(emailInstitucional(null)).toBe(true)
  })
})
