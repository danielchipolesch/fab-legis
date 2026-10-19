// Teste de carga do collab: N salas (elementos de documentos reais), M clientes por sala, todos digitando ao mesmo tempo, e mede o que
// importa para a edição colaborativa -- quanto leva para a edição de um cliente chegar aos outros da sala (latência de propagação),
// quanto leva para conectar e se alguma conexão é recusada. Cada cliente tem o seu WebSocket (conexões de verdade).
//
// A latência vem de um Y.Map "carga" na sala: a cada tecla o cliente grava nele a hora do envio e os demais, ao receber, medem a
// diferença (mesmo processo, mesmo relógio). Esse mapa NÃO é gravado no banco (o collab só persiste o fragmento do documento); as teclas
// em si vão para o parágrafo de verdade, então o collab também grava no backend (PATCH .../conteudo) como em produção.
//
// Cria os documentos de teste pela API e os apaga no fim (LIMPAR=0 para manter). Rode contra um ambiente de teste, não contra produção.
// Uso (rede do Docker Compose): ver docs/testes.md ("Testes de carga").
//   COLLAB_URL (ws://collab:1234)  BASE_URL (http://backend:8081)  CPF  SENHA
//   SALAS (20)  CLIENTES (3, por sala)  DURACAO (30, segundos)  TECLAS_POR_S (3, por cliente)  META_P95_MS (100)
import { HocuspocusProvider, HocuspocusProviderWebsocket } from '@hocuspocus/provider'
import WebSocket from 'ws'
import * as Y from 'yjs'
import { obterToken } from '../loadtest/lib/autenticacao.mjs'

const {
  COLLAB_URL = 'ws://collab:1234', BASE_URL = 'http://backend:8081', CPF, SENHA,
  SALAS = '20', CLIENTES = '3', DURACAO = '30', TECLAS_POR_S = '3', META_P95_MS = '100', LIMPAR = '1',
} = process.env
if (!CPF || !SENHA) { console.error('Informe CPF e SENHA nas variáveis de ambiente.'); process.exit(1) }
const salas = Number(SALAS), clientes = Number(CLIENTES), duracao = Number(DURACAO), teclas = Number(TECLAS_POR_S)

const dormir = (ms) => new Promise((r) => setTimeout(r, ms))
const percentil = (v, p) => (v.length ? [...v].sort((a, b) => a - b)[Math.min(v.length - 1, Math.floor((p / 100) * v.length))] : NaN)
const fmt = (n) => (Number.isFinite(n) ? n.toFixed(1) : '—')

const token = await obterToken({ baseUrl: BASE_URL, cpf: CPF, senha: SENHA })
const cab = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
const api = async (caminho, opcoes = {}) => {
  const r = await fetch(`${BASE_URL}/v1${caminho}`, { headers: cab, ...opcoes })
  if (!r.ok && r.status !== 204) throw new Error(`${opcoes.method ?? 'GET'} ${caminho}: HTTP ${r.status}`)
  return r.status === 204 ? null : r.json()
}

// ─── Preparação: documentos reais (DCA) e os elementos deles viram as salas ───────────────────────────────────────────────────
const especies = await api('/especie-normativa/obter-todos?size=50&sortBy=id')
const dca = especies.find((e) => e.sigla === 'DCA')
const assunto = (await api('/assunto-basico/obter-todos?size=1&sortBy=id'))[0]
const achatar = (itens) => itens.flatMap((i) => [i, ...achatar(i.children ?? [])])

const documentos = []
const todasAsSalas = []
while (todasAsSalas.length < salas) {
  const criado = await api('/documentos', {
    method: 'POST',
    body: JSON.stringify({ idEspecieNormativa: dca.id, idAssuntoBasico: assunto.idAssuntoBasico, tituloDocumento: 'Teste de carga do collab' }),
  })
  documentos.push(criado.idDocumento)
  const doc = await api(`/documentos/${criado.idDocumento}`)
  for (const el of achatar(doc.itensNormativos)) todasAsSalas.push(`documento:${criado.idDocumento}:elemento:${el.id}`)
}
const nomesDasSalas = todasAsSalas.slice(0, salas)
console.log(`Preparado: ${documentos.length} documento(s), ${nomesDasSalas.length} sala(s) x ${clientes} cliente(s) = ${nomesDasSalas.length * clientes} conexões.`)

// ─── Conexões ───────────────────────────────────────────────────────────────────────────────────────────────────────────────────
const temposDeConexao = []
const latencias = []
let recusadas = 0
let teclasEnviadas = 0
const todos = []

function conectar(nomeDaSala, indice) {
  return new Promise((resolve) => {
    const inicio = performance.now()
    const websocketProvider = new HocuspocusProviderWebsocket({ url: COLLAB_URL, WebSocketPolyfill: WebSocket })
    let resolvido = false
    const concluir = (ok) => { if (!resolvido) { resolvido = true; resolve(ok ? provider : null) } }
    const provider = new HocuspocusProvider({
      websocketProvider, name: nomeDaSala, token,
      onAuthenticationFailed: () => { recusadas++; concluir(false) },
      onSynced: () => { temposDeConexao.push(performance.now() - inicio); concluir(true) },
    })
    setTimeout(() => concluir(false), 20000)
    provider.__indice = indice
  })
}

// Em lotes, para não abrir tudo no mesmo instante (o navegador de verdade também não abre).
const pedidos = nomesDasSalas.flatMap((sala) => Array.from({ length: clientes }, (_, i) => ({ sala, i })))
let proximo = 0
async function operario() {
  while (proximo < pedidos.length) {
    const { sala, i } = pedidos[proximo++]
    const provider = await conectar(sala, i)
    if (provider) todos.push(provider)
  }
}
await Promise.all(Array.from({ length: 25 }, operario))
console.log(`Conectados: ${todos.length}/${pedidos.length}  (recusados na autenticação: ${recusadas}, sem conexão: ${pedidos.length - todos.length - recusadas}).`)
if (!todos.length) { console.error('Nenhuma conexão: abortando.'); process.exit(2) }

// ─── Digitação ────────────────────────────────────────────────────────────────────────────────────────────────────────────────
const agora = () => performance.timeOrigin + performance.now()
const digitadores = todos.map((provider, k) => {
  const doc = provider.document
  const mapa = doc.getMap('carga')
  const meuId = `c${k}`
  mapa.observe((evento) => {
    if (evento.transaction.local) return
    for (const chave of evento.keysChanged) {
      const v = mapa.get(chave)
      if (v && typeof v.t === 'number') latencias.push(agora() - v.t)
    }
  })
  const paragrafo = doc.getXmlFragment('default').get(0)
  const texto = new Y.XmlText()
  doc.transact(() => paragrafo.insert(paragrafo.length, [texto]))
  let n = 0
  return () => {
    n++
    teclasEnviadas++
    doc.transact(() => {
      texto.insert(texto.length, 'x')
      mapa.set(meuId, { t: agora(), n })
    })
  }
})

console.log(`Digitando por ${duracao}s (${teclas} tecla/s por cliente)...`)
const timers = digitadores.map((tecla) => {
  const t = setInterval(tecla, 1000 / teclas)
  return t
})
await dormir(duracao * 1000)
timers.forEach(clearInterval)
await dormir(3500) // deixa as últimas atualizações chegarem e o collab gravar (debounce de 2 s) antes de apagar os documentos

// ─── Resultado ────────────────────────────────────────────────────────────────────────────────────────────────────────────────
const p95 = percentil(latencias, 95)
console.log('\n── Resultado ──────────────────────────────────────────────')
console.log(`Conexões abertas        : ${todos.length}  (salas: ${nomesDasSalas.length}, clientes por sala: ${clientes})`)
console.log(`Tempo de conexão (ms)   : p50 ${fmt(percentil(temposDeConexao, 50))}   p95 ${fmt(percentil(temposDeConexao, 95))}   máx ${fmt(Math.max(...temposDeConexao))}`)
console.log(`Teclas enviadas         : ${teclasEnviadas}  (${fmt(teclasEnviadas / duracao)}/s no total)`)
console.log(`Propagação (ms)         : p50 ${fmt(percentil(latencias, 50))}   p95 ${fmt(p95)}   p99 ${fmt(percentil(latencias, 99))}   máx ${fmt(Math.max(...latencias))}   (${latencias.length} medições)`)
console.log(`Meta                    : p95 < ${META_P95_MS} ms  ->  ${p95 < Number(META_P95_MS) ? 'ATINGIDA' : 'NÃO atingida'}`)

todos.forEach((p) => { try { p.destroy() } catch { /* já fechado */ } })
await dormir(1500)
if (LIMPAR !== '0') {
  for (const id of documentos) await api(`/documentos/${id}`, { method: 'DELETE' }).catch(() => {})
  console.log(`Documentos de teste apagados: ${documentos.length}.`)
}
process.exit(p95 < Number(META_P95_MS) && recusadas === 0 ? 0 : 1)
