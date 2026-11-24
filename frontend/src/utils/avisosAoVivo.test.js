import { describe, it, expect, vi } from 'vitest'
import { ouvirAvisosAoVivo, juntarRajada, acaoDeAcesso, ehODocumentoAberto, documentoExcluido } from './avisosAoVivo.js'

// Avisos ao vivo (SSE): o frontend se atualiza sem recarregar a página quando o backend diz que algo mudou.
class EventSourceFalso {
  constructor() { this.ouvintes = {} }
  addEventListener(nome, fn) { (this.ouvintes[nome] ??= []).push(fn) }
  emitir(nome, data) { (this.ouvintes[nome] ?? []).forEach(fn => fn({ data })) }
}

describe('ouvirAvisosAoVivo', () => {
  it('cada evento chega ao seu handler, com o JSON já lido', () => {
    const es = new EventSourceFalso()
    const aoDocumento = vi.fn(), aoAcesso = vi.fn(), aoHabilitacao = vi.fn()
    ouvirAvisosAoVivo(es, { aoDocumento, aoAcesso, aoHabilitacao })

    es.emitir('documento', JSON.stringify({ id: 7, situacaoLocal: 'MINUTA', situacaoBca: 'NAO_PUBLICADO' }))
    es.emitir('acesso', JSON.stringify({ ativo: true }))
    es.emitir('habilitacao', '{}')

    expect(aoDocumento).toHaveBeenCalledWith({ id: 7, situacaoLocal: 'MINUTA', situacaoBca: 'NAO_PUBLICADO' })
    expect(aoAcesso).toHaveBeenCalledWith({ ativo: true })
    expect(aoHabilitacao).toHaveBeenCalledWith({})
  })

  it('dado vazio ou inválido não quebra: o handler recebe {}', () => {
    const es = new EventSourceFalso()
    const aoDocumento = vi.fn()
    ouvirAvisosAoVivo(es, { aoDocumento })
    es.emitir('documento', '')
    es.emitir('documento', 'não é json')
    expect(aoDocumento).toHaveBeenNthCalledWith(1, {})
    expect(aoDocumento).toHaveBeenNthCalledWith(2, {})
  })

  it('evento sem handler não é ligado', () => {
    const es = new EventSourceFalso()
    ouvirAvisosAoVivo(es, { aoDocumento: vi.fn() })
    expect(Object.keys(es.ouvintes)).toEqual(['documento'])
  })
})

describe('juntarRajada', () => {
  it('uma rajada de avisos vira uma chamada só, com o último argumento, depois que se acalma', () => {
    vi.useFakeTimers()
    const f = vi.fn()
    const junta = juntarRajada(f, 400)
    junta('a'); junta('b'); junta('c')
    vi.advanceTimersByTime(399)
    expect(f).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(f).toHaveBeenCalledTimes(1)
    expect(f).toHaveBeenCalledWith('c')
    vi.useRealTimers()
  })

  it('avisos separados por mais que a janela são chamadas separadas', () => {
    vi.useFakeTimers()
    const f = vi.fn()
    const junta = juntarRajada(f, 400)
    junta(); vi.advanceTimersByTime(500)
    junta(); vi.advanceTimersByTime(500)
    expect(f).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })

  it('cancelar descarta a chamada pendente (sair da tela não atualiza o que já não existe)', () => {
    vi.useFakeTimers()
    const f = vi.fn()
    const junta = juntarRajada(f, 400)
    junta()
    junta.cancelar()
    vi.advanceTimersByTime(1000)
    expect(f).not.toHaveBeenCalled()
    vi.useRealTimers()
  })
})

describe('regras dos avisos', () => {
  it('conta desativada encerra a sessão; qualquer outra mudança de acesso renova o token', () => {
    expect(acaoDeAcesso({ ativo: false })).toBe('encerrar')
    expect(acaoDeAcesso({ ativo: true })).toBe('renovar')
    expect(acaoDeAcesso({})).toBe('renovar')
  })

  it('reconhece o aviso do documento que a pessoa tem aberto (id como número ou texto)', () => {
    expect(ehODocumentoAberto({ id: 42 }, '42')).toBe(true)
    expect(ehODocumentoAberto({ id: '42' }, 42)).toBe(true)
    expect(ehODocumentoAberto({ id: 43 }, '42')).toBe(false)
    expect(ehODocumentoAberto({ id: 42 }, undefined)).toBe(false)
    expect(ehODocumentoAberto({}, '42')).toBe(false)
  })

  it('documento sem etapa no aviso foi excluído', () => {
    expect(documentoExcluido({ id: 42, situacaoLocal: null })).toBe(true)
    expect(documentoExcluido({ id: 42 })).toBe(true)
    expect(documentoExcluido({ id: 42, situacaoLocal: 'MINUTA' })).toBe(false)
    expect(documentoExcluido({})).toBe(false)
  })
})
