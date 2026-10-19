// Teste de carga da API (k6): quatro tipos de uso ao mesmo tempo, cada um com a sua meta -- leitura (as telas do dia a dia), gravação
// (o que o collab faz a cada pausa de digitação) e criação de documento (a numeração com trava), e geração de PDF (o que o Apache FOP
// faz, o ponto que satura primeiro), com PDF em cache e PDF a gerar. Ver docs/testes.md ("Testes de carga").
//
// Cria documentos de teste (título "Teste de carga k6") e os apaga no fim. Rode contra um ambiente de teste, nunca contra produção.
//   k6 run -e BASE_URL=http://backend:8081 -e CPF=... -e SENHA=... -e PERFIL=medio api.js
//   PERFIL: leve | medio | pico   DURACAO (segundos, padrão 60)
import http from 'k6/http'
import crypto from 'k6/crypto'
import encoding from 'k6/encoding'
import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://backend:8081'
const CPF = (__ENV.CPF || '').replace(/\D/g, '')
const SENHA = __ENV.SENHA || ''
const REDIRECT_URI = __ENV.REDIRECT_URI || 'http://127.0.0.1:5173/callback'
const PERFIL = __ENV.PERFIL || 'medio'
const DURACAO = Number(__ENV.DURACAO || 60)
const TITULO = 'Teste de carga k6'

const pdfOcupado = new Counter('pdf_ocupado_503')

// O que cada perfil de carga pede: usuários virtuais na leitura e requisições por segundo nas demais.
const PERFIS = {
  leve:  { leitura: 10,  gravacao: 2,  criacao: 0.5, pdfCache: 1,  pdfGerar: 0.2 },
  medio: { leitura: 50,  gravacao: 10, criacao: 2,   pdfCache: 5,  pdfGerar: 1 },
  pico:  { leitura: 100, gravacao: 30, criacao: 5,   pdfCache: 10, pdfGerar: 3 },
}
const carga = PERFIS[PERFIL] || PERFIS.medio

const taxa = (rps, grupo, funcao) => ({
  executor: 'constant-arrival-rate', exec: funcao, rate: Math.max(1, Math.round(rps * 10)), timeUnit: '10s',
  duration: `${DURACAO}s`, preAllocatedVUs: Math.max(5, Math.ceil(rps * 3)), maxVUs: Math.max(20, Math.ceil(rps * 20)),
  tags: { grupo }, startTime: '5s',
})

export const options = {
  scenarios: {
    leitura: {
      executor: 'ramping-vus', exec: 'leitura', startVUs: 1, tags: { grupo: 'leitura' }, startTime: '5s',
      stages: [{ duration: '10s', target: carga.leitura }, { duration: `${Math.max(DURACAO - 15, 5)}s`, target: carga.leitura }, { duration: '5s', target: 0 }],
    },
    gravacao: taxa(carga.gravacao, 'gravacao', 'gravacao'),
    criacao: taxa(carga.criacao, 'criacao', 'criacao'),
    pdf_cache: taxa(carga.pdfCache, 'pdf_cache', 'pdfEmCache'),
    pdf_gerar: taxa(carga.pdfGerar, 'pdf_gerar', 'pdfAGerar'),
  },
  thresholds: {
    // Metas de mercado para uma API interna: p95 abaixo de 500 ms na leitura, erros abaixo de 1%.
    'http_req_duration{grupo:leitura}': ['p(95)<500'],
    'http_req_duration{grupo:gravacao}': ['p(95)<800'],
    'http_req_duration{grupo:criacao}': ['p(95)<1500'],
    'http_req_duration{grupo:pdf_cache}': ['p(95)<1000'],
    'http_req_duration{grupo:pdf_gerar}': ['p(95)<10000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

// ─── Login (OAuth2 Authorization Code + PKCE, como o navegador) ─────────────────────────────────────────────────────────────────
function login() {
  const verifier = encoding.b64encode(crypto.randomBytes(32), 'rawurl')
  const challenge = encoding.b64encode(crypto.sha256(verifier, 'binary'), 'rawurl')
  const sem = { redirects: 0 }
  const params = `response_type=code&client_id=fab-legis-frontend&redirect_uri=${encodeURIComponent(REDIRECT_URI)}&scope=openid`
    + `&code_challenge=${challenge}&code_challenge_method=S256&state=k6`
  http.get(`${BASE_URL}/oauth2/authorize?${params}`, sem)
  const entrada = http.post(`${BASE_URL}/login`, { username: CPF, password: SENHA }, sem)
  const volta = entrada.headers.Location
  if (!volta || volta.indexOf('error') >= 0) throw new Error(`Login recusado: ${entrada.status} ${volta}`)
  const autorizar = http.get(volta.startsWith('http') ? volta : `${BASE_URL}${volta}`, sem)
  const code = /[?&]code=([^&]+)/.exec(autorizar.headers.Location || '')
  if (!code) throw new Error(`Sem code: ${autorizar.status} ${autorizar.headers.Location}`)
  const token = http.post(`${BASE_URL}/oauth2/token`, {
    grant_type: 'authorization_code', code: code[1], redirect_uri: REDIRECT_URI, client_id: 'fab-legis-frontend', code_verifier: verifier,
  })
  if (token.status !== 200) throw new Error(`Token: HTTP ${token.status}`)
  return token.json('access_token')
}

const cab = (token) => ({ headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } })
const achatar = (itens) => itens.reduce((acc, i) => acc.concat([i], achatar(i.children || [])), [])
const conteudo = (texto) => JSON.stringify({ conteudo: JSON.stringify({ type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: texto }] }] }) })

export function setup() {
  if (!CPF || !SENHA) throw new Error('Informe CPF e SENHA (-e CPF=... -e SENHA=...).')
  const token = login()
  const h = cab(token)
  const especies = http.get(`${BASE_URL}/v1/especie-normativa/obter-todos?size=50&sortBy=id`, h).json()
  const assuntos = http.get(`${BASE_URL}/v1/assunto-basico/obter-todos?size=1&sortBy=id`, h).json()
  const dca = especies.find((e) => e.sigla === 'DCA')
  const corpo = JSON.stringify({ idEspecieNormativa: dca.id, idAssuntoBasico: assuntos[0].idAssuntoBasico, tituloDocumento: TITULO })
  const criado = http.post(`${BASE_URL}/v1/documentos`, corpo, h).json()
  const doc = http.get(`${BASE_URL}/v1/documentos/${criado.idDocumento}`, h).json()
  const elementos = achatar(doc.itensNormativos).map((e) => e.id)
  return { token, docId: criado.idDocumento, elementos, especie: dca.id, assunto: assuntos[0].idAssuntoBasico }
}

// ─── Cenários ───────────────────────────────────────────────────────────────────────────────────────────────────────────────────
export function leitura(dados) {
  const h = cab(dados.token)
  const respostas = http.batch([
    ['GET', `${BASE_URL}/v1/painel/minhas-em-tramitacao?page=0&size=5`, null, h],
    ['GET', `${BASE_URL}/v1/painel/aguardando-acao?page=0&size=5`, null, h],
    ['GET', `${BASE_URL}/v1/painel/publicados-e-revogados?page=0&size=5`, null, h],
    ['GET', `${BASE_URL}/v1/documentos/obter-todos?aba=minha_om&page=0&size=10`, null, h],
  ])
  respostas.forEach((r) => check(r, { 'leitura 200': (x) => x.status === 200 }))
  check(http.get(`${BASE_URL}/v1/documentos/${dados.docId}`, h), { 'documento 200': (x) => x.status === 200 })
  check(http.get(`${BASE_URL}/v1/documentos/busca?q=artigo&page=0&size=10`, h), { 'busca 200': (x) => x.status === 200 })
  sleep(1 + Math.random() * 2) // pensa um pouco, como uma pessoa
}

// O que o collab faz: grava o conteúdo de um elemento (a cada pausa de 2 s de digitação).
export function gravacao(dados) {
  const el = dados.elementos[Math.floor(Math.random() * dados.elementos.length)]
  const r = http.patch(`${BASE_URL}/v1/documentos/${dados.docId}/elementos/${el}/conteudo`, conteudo(`Texto da carga ${Date.now()}`), cab(dados.token))
  check(r, { 'gravação 204': (x) => x.status === 204 })
}

// Criar um documento: o número é escolhido com trava (V25), então criações simultâneas esperam umas pelas outras.
export function criacao(dados) {
  const corpo = JSON.stringify({ idEspecieNormativa: dados.especie, idAssuntoBasico: dados.assunto, tituloDocumento: TITULO })
  const r = http.post(`${BASE_URL}/v1/documentos`, corpo, cab(dados.token))
  check(r, { 'criação 201': (x) => x.status === 201 })
}

function pegarPdf(dados) {
  const r = http.get(`${BASE_URL}/v1/documentos/${dados.docId}/pdf`, { ...cab(dados.token), responseType: 'none', responseCallback: http.expectedStatuses(200, 503) })
  if (r.status === 503) pdfOcupado.add(1) // o limite de gerações simultâneas esgotou a espera: a condição que o PDF deve avisar
  else check(r, { 'pdf 200': (x) => x.status === 200 })
}

// PDF de um documento que não mudou: sai do cache do MinIO.
export function pdfEmCache(dados) {
  pegarPdf(dados)
}

// PDF depois de mudar o conteúdo: o cache não vale e o FOP gera de verdade.
export function pdfAGerar(dados) {
  const el = dados.elementos[Math.floor(Math.random() * dados.elementos.length)]
  http.patch(`${BASE_URL}/v1/documentos/${dados.docId}/elementos/${el}/conteudo`, conteudo(`Mudança ${Date.now()} ${Math.random()}`), cab(dados.token))
  pegarPdf(dados)
}

export function teardown(dados) {
  const h = cab(dados.token)
  const achados = http.get(`${BASE_URL}/v1/documentos/obter-todos?aba=meus&busca=${encodeURIComponent(TITULO)}&page=0&size=500`, h)
  const lista = (achados.json('content') || []).map((d) => d.idDocumento)
  if (lista.indexOf(dados.docId) < 0) lista.push(dados.docId)
  lista.forEach((id) => http.del(`${BASE_URL}/v1/documentos/${id}`, null, h))
  console.log(`Documentos de teste apagados: ${lista.length}.`)
}
