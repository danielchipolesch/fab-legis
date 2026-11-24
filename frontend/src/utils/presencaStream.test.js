import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Presença de edição (docs/autenticacao.md, "Edição concorrente"): o toast "fulano começou a editar este documento agora" e a faixa de
// aviso chegam por uma conexão SSE (EventSource) aberta enquanto o editor está aberto.
//
// REGRA: a conexão tem de se manter viva. O EventSource só reconecta sozinho quando a queda é de rede (readyState CONNECTING); se o servidor
// responde 401 -- token vencido (30 min) ou backend reiniciado (a chave RSA é efêmera e invalida todos os tokens) --, ele fica FECHADO
// (readyState CLOSED) para sempre, e o toast some até alguém dar F5. Por isso, ao ver a conexão fechada, é preciso abrir outra com o
// token ATUAL da store (a URL do EventSource antigo leva o token velho).
//
// Estes testes descrevem o comportamento correto de `conectarPresenca` (utils/presencaStream.js), que ainda NÃO existe: hoje a lógica está
// dentro de DocumentoEditorPage.vue (iniciarPresenca) e não reconecta. Ficam em it.skip -- com o import dinâmico, para a suíte não quebrar
// -- até a função ser extraída com este contrato; aí é só trocar `it.skip` por `it`:
//
//   conectarPresenca({ urlAtual, aoPresenca, aoEstrutura, criarEventSource?, esperaMs? }) -> { fechar }
//     urlAtual():   devolve a URL do stream com o token de agora (chamada a cada (re)conexão)
//     aoPresenca(lista), aoEstrutura(payload): recebem o JSON dos eventos 'presenca' e 'estrutura'
//     criarEventSource(url): padrão `new EventSource(url)` (injetável para teste)
//     esperaMs: pausa antes de reabrir uma conexão fechada (padrão 2000)

const CONECTANDO = 0
const ABERTA = 1
const FECHADA = 2

class EventSourceFalso {
  static instancias = []
  constructor(url) {
    this.url = url
    this.readyState = CONECTANDO
    this.ouvintes = {}
    this.fechadoPorQuemUsa = false
    EventSourceFalso.instancias.push(this)
  }
  addEventListener(tipo, fn) { (this.ouvintes[tipo] ??= []).push(fn) }
  close() { this.readyState = FECHADA; this.fechadoPorQuemUsa = true }
  // ── controle do teste ──
  emitir(tipo, dados) { (this.ouvintes[tipo] ?? []).forEach(fn => fn({ data: JSON.stringify(dados) })) }
  // Dispara o erro tanto para `onerror` quanto para addEventListener('error'), como o EventSource de verdade.
  falhar(readyState) {
    this.readyState = readyState
    const evento = { type: 'error' }
    this.onerror?.(evento)
    ;(this.ouvintes.error ?? []).forEach(fn => fn(evento))
  }
}

const ESPERA = 2000

async function conectar(extra = {}) {
  const { conectarPresenca } = await import('./presencaStream.js')
  const token = { atual: 'token-1' }
  const aoPresenca = vi.fn()
  const aoEstrutura = vi.fn()
  const conexao = conectarPresenca({
    urlAtual: () => `http://api/v1/documentos/42/presenca/stream?token=${token.atual}`,
    aoPresenca,
    aoEstrutura,
    criarEventSource: (url) => new EventSourceFalso(url),
    esperaMs: ESPERA,
    ...extra,
  })
  return { conexao, token, aoPresenca, aoEstrutura }
}

describe('presença de edição: a conexão SSE se mantém viva', () => {
  beforeEach(() => {
    EventSourceFalso.instancias = []
    vi.useFakeTimers()
  })
  afterEach(() => vi.useRealTimers())

  it.skip('abre a conexão com o token de agora e repassa os eventos presenca e estrutura', async () => {
    const { aoPresenca, aoEstrutura } = await conectar()
    expect(EventSourceFalso.instancias).toHaveLength(1)
    const es = EventSourceFalso.instancias[0]
    expect(es.url).toContain('token=token-1')

    es.emitir('presenca', [{ usuarioId: 7, nome: 'Marina' }])
    es.emitir('estrutura', { origem: 'outro', eventos: [] })
    expect(aoPresenca).toHaveBeenCalledWith([{ usuarioId: 7, nome: 'Marina' }])
    expect(aoEstrutura).toHaveBeenCalledWith({ origem: 'outro', eventos: [] })
  })

  it.skip('conexão fechada pelo servidor (401: token vencido ou backend reiniciado) é reaberta com o token ATUAL', async () => {
    const { token } = await conectar()
    const primeira = EventSourceFalso.instancias[0]

    token.atual = 'token-2' // a sessão foi renovada (ou a pessoa entrou de novo) enquanto a conexão estava de pé
    primeira.falhar(FECHADA)
    expect(EventSourceFalso.instancias).toHaveLength(1) // espera um pouco antes de tentar (não martela o servidor)

    vi.advanceTimersByTime(ESPERA)
    expect(EventSourceFalso.instancias).toHaveLength(2)
    expect(EventSourceFalso.instancias[1].url).toContain('token=token-2')
    expect(EventSourceFalso.instancias[1].url).not.toContain('token-1')
  })

  it.skip('depois de reaberta, os eventos da nova conexão chegam (o toast volta a aparecer sem F5)', async () => {
    const { aoPresenca } = await conectar()
    EventSourceFalso.instancias[0].falhar(FECHADA)
    vi.advanceTimersByTime(ESPERA)

    EventSourceFalso.instancias[1].emitir('presenca', [{ usuarioId: 9, nome: 'Rafael' }])
    expect(aoPresenca).toHaveBeenCalledWith([{ usuarioId: 9, nome: 'Rafael' }])
  })

  it.skip('queda de rede (readyState CONNECTING) é do navegador: ele reconecta sozinho, nenhuma conexão extra é aberta', async () => {
    await conectar()
    EventSourceFalso.instancias[0].falhar(CONECTANDO)
    vi.advanceTimersByTime(ESPERA * 5)
    expect(EventSourceFalso.instancias).toHaveLength(1)
  })

  it.skip('continua tentando enquanto a conexão for recusada (backend ainda subindo)', async () => {
    await conectar()
    for (let i = 0; i < 3; i++) {
      EventSourceFalso.instancias.at(-1).falhar(FECHADA)
      vi.advanceTimersByTime(ESPERA)
    }
    expect(EventSourceFalso.instancias).toHaveLength(4)
  })

  it.skip('fechar() encerra a conexão e cancela uma reabertura que estava agendada (sair do editor não deixa conexão órfã)', async () => {
    const { conexao } = await conectar()
    const es = EventSourceFalso.instancias[0]
    es.falhar(FECHADA) // reabertura agendada...
    conexao.fechar() // ...mas a pessoa saiu do editor

    vi.advanceTimersByTime(ESPERA * 5)
    expect(EventSourceFalso.instancias).toHaveLength(1)

    const { conexao: outra } = await conectar()
    const aberta = EventSourceFalso.instancias.at(-1)
    aberta.readyState = ABERTA
    outra.fechar()
    expect(aberta.fechadoPorQuemUsa).toBe(true)
  })
})
