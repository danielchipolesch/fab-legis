import { describe, it, expect } from 'vitest'
import {
  ehAdministradorDaOmDoDocumento, podeDesvincular, rotuloDeDesvincular, podeAtribuirAutoriaASiMesmo,
} from './autoria.js'

// Espelha DocumentoAcessoServiceTest (backend): quem atribui e quem desvincula um autor de um documento.
const OM_DO_DOC = '10'
const admLocal = { id: 1, omId: 10, papeis: ['ELB', 'ADM_OM'] }
const admDeOutraOm = { id: 2, omId: 99, papeis: ['ELB', 'ADM_OM'] }
const admGeral = { id: 3, omId: 10, papeis: ['ELB', 'ADM_GER'] }
const daOm = { id: 4, omId: 10, papeis: ['ELB'] }
const transferido = { id: 5, omId: 99, papeis: ['ELB'] }

describe('Administrador da OM do documento', () => {
  it('só o administrador local da OM do documento', () => {
    expect(ehAdministradorDaOmDoDocumento(admLocal, OM_DO_DOC)).toBe(true)
    expect(ehAdministradorDaOmDoDocumento(admDeOutraOm, OM_DO_DOC)).toBe(false)
    expect(ehAdministradorDaOmDoDocumento(daOm, OM_DO_DOC)).toBe(false)
  })

  it('o Administrador Geral não atribui autoria nem desvincula autor', () => {
    expect(ehAdministradorDaOmDoDocumento(admGeral, OM_DO_DOC)).toBe(false)
    expect(podeDesvincular(4, admGeral, OM_DO_DOC)).toBe(false)
  })
})

describe('desvincular autor', () => {
  it('o administrador local desvincula qualquer autor', () => {
    expect(podeDesvincular(4, admLocal, OM_DO_DOC)).toBe(true)
    expect(podeDesvincular(5, admLocal, OM_DO_DOC)).toBe(true)
  })

  it('o autor transferido para outra OM pode se desvincular, mas não desvincula outro autor', () => {
    expect(podeDesvincular(5, transferido, OM_DO_DOC)).toBe(true)
    expect(podeDesvincular(4, transferido, OM_DO_DOC)).toBe(false)
  })

  it('quem ainda é da OM do documento não se desvincula: a opção nem aparece', () => {
    expect(podeDesvincular(4, daOm, OM_DO_DOC)).toBe(false)
    expect(podeDesvincular(5, daOm, OM_DO_DOC)).toBe(false)
  })

  it('o texto do botão: desvincular-se ou desvincular outro autor', () => {
    expect(rotuloDeDesvincular(5, transferido)).toBe('Desvincular-me do documento')
    expect(rotuloDeDesvincular(4, admLocal)).toBe('Desvincular autor')
  })
})

describe('atribuir a autoria a si mesmo', () => {
  it('o administrador local que ainda não é autor pode; quem já é autor, não', () => {
    expect(podeAtribuirAutoriaASiMesmo([{ usuarioId: 4 }], admLocal, OM_DO_DOC)).toBe(true)
    expect(podeAtribuirAutoriaASiMesmo([{ usuarioId: 1 }, { usuarioId: 4 }], admLocal, OM_DO_DOC)).toBe(false)
  })

  it('quem não é administrador local não vê a opção', () => {
    expect(podeAtribuirAutoriaASiMesmo([], daOm, OM_DO_DOC)).toBe(false)
    expect(podeAtribuirAutoriaASiMesmo([], admDeOutraOm, OM_DO_DOC)).toBe(false)
    expect(podeAtribuirAutoriaASiMesmo([], admGeral, OM_DO_DOC)).toBe(false)
  })
})
