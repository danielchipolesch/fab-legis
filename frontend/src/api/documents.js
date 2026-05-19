import * as http from './client.js'

const SECAO_CONFIG = {
  PARTE_PRELIMINAR: { tipo: 'parte_preliminar', titulo: 'Parte Preliminar', ordem: 1 },
  PARTE_NORMATIVA:  { tipo: 'parte_normativa',  titulo: 'Parte Normativa',  ordem: 2 },
  PARTE_FINAL:      { tipo: 'parte_final',       titulo: 'Parte Final',      ordem: 3 },
}

const SECAO_ENUM_MAP = {
  parte_preliminar: 'PARTE_PRELIMINAR',
  parte_normativa:  'PARTE_NORMATIVA',
  parte_final:      'PARTE_FINAL',
}

function parseDtCriacao(dt) {
  if (!dt) return null
  if (typeof dt === 'number') return new Date(dt).toISOString().slice(0, 10)
  return String(dt).slice(0, 10)
}

function apiItemParaFrontend(item) {
  return {
    id: String(item.id),
    tipo: (item.elementType ?? '').toLowerCase(),
    elementOrder: item.elementOrder ?? null,
    numero: null,
    titulo: item.elementTitle ?? null,
    conteudo: item.elementContent ?? null,
    fullTextContent: item.fullTextContent ?? null,
    filhos: (item.children ?? []).map(apiItemParaFrontend),
  }
}

function buildSecao(secaoKey, itensApi) {
  if (!itensApi?.length) return null
  return {
    ...SECAO_CONFIG[secaoKey],
    id: crypto.randomUUID(),
    elementos: itensApi.map(apiItemParaFrontend),
  }
}

function converterElemento(el, secaoEnum) {
  return {
    secao: secaoEnum,
    tipo: (el.tipo ?? '').toUpperCase(),
    elementOrder: el.elementOrder ?? null,
    titulo: el.titulo ?? null,
    conteudo: el.conteudo ?? null,
    fullTextContent: el.fullTextContent ?? null,
    filhos: (el.filhos ?? []).map(f => converterElemento(f, secaoEnum)),
  }
}

export function backendParaFrontend(doc) {
  if (!doc) return null

  const secoes = []
  const preliminar = buildSecao('PARTE_PRELIMINAR', doc.itensPreliminares)
  const normativa  = buildSecao('PARTE_NORMATIVA',  doc.itensNormativos)
  const final_     = buildSecao('PARTE_FINAL',      doc.itensFinais)
  if (preliminar) secoes.push(preliminar)
  if (normativa)  secoes.push(normativa)
  if (final_)     secoes.push(final_)

  return {
    id: doc.idDocumento,
    especie: doc.siglaEspecieNormativa,
    numero_basico: doc.codigoAssuntoBasico,
    numero_secundario: doc.numeroSecundario != null ? String(doc.numeroSecundario) : null,
    assunto_basico: doc.nomeAssuntoBasico ?? doc.codigoAssuntoBasico,
    titulo: doc.tituloDocumento,
    codigo_documento: doc.codigoDocumento,
    data_criacao: parseDtCriacao(doc.dtCriacao),
    data_publicacao: null,
    status: doc.statusDocumento,
    versoes: [],
    secoes: secoes.length ? secoes : null,
  }
}

export function frontendParaBackendCreate(payload) {
  return {
    idEspecieNormativa: payload.idEspecieNormativa,
    idAssuntoBasico:    payload.idAssuntoBasico,
    tituloDocumento:    payload.tituloDocumento,
  }
}

export async function listDocumentos() {
  const data = await http.get('/documentos/obter-todos?size=200&sortBy=id')
  const items = Array.isArray(data) ? data : []
  return items.map(backendParaFrontend)
}

export async function getDocumento(id) {
  const data = await http.get(`/documentos/${id}`)
  return backendParaFrontend(data)
}

export async function createDocumento(payload) {
  const body = frontendParaBackendCreate(payload)
  const data = await http.post('/documentos', body)
  return backendParaFrontend(data)
}

export async function cloneDocumento(id) {
  const data = await http.post(`/documentos/${id}/clonar`)
  return backendParaFrontend(data)
}

export async function updateDocumento(id, data) {
  const body = { tituloDocumento: data.titulo ?? data.tituloDocumento }
  const result = await http.put(`/documentos/${id}`, body)
  return backendParaFrontend(result)
}

export async function changeDocumentoStatus(id, novoStatus) {
  const result = await http.patch(`/documentos/${id}/status`, { status: novoStatus })
  return backendParaFrontend(result)
}

export async function saveSecoes(id, secoes) {
  if (!secoes?.length) return null
  const itens = []
  for (const secao of secoes) {
    const secaoEnum = SECAO_ENUM_MAP[secao.tipo]
    if (!secaoEnum) continue
    for (const el of secao.elementos ?? []) {
      itens.push(converterElemento(el, secaoEnum))
    }
  }
  return http.put(`/documentos/${id}/secoes`, { itens })
}

export async function deleteDocumento(id) {
  return http.del(`/documentos/${id}`)
}
