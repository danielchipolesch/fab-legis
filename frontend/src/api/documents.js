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
  return {
    ...SECAO_CONFIG[secaoKey],
    id: crypto.randomUUID(),
    elementos: (itensApi ?? []).map(apiItemParaFrontend),
  }
}

function converterElemento(el, secaoEnum, posicao) {
  return {
    secao: secaoEnum,
    tipo: (el.tipo ?? '').toUpperCase(),
    elementOrder: posicao,
    titulo: el.titulo ?? null,
    conteudo: el.conteudo ?? null,
    fullTextContent: el.fullTextContent ?? null,
    filhos: (el.filhos ?? []).map((f, i) => converterElemento(f, secaoEnum, i + 1)),
  }
}

export function backendParaFrontend(doc) {
  if (!doc) return null

  const preliminarItens = doc.itensPreliminares ?? []
  const normativaItens  = doc.itensNormativos   ?? []
  const finalItens      = doc.itensFinais        ?? []

  // Retorna null quando todas as seções estão vazias (documento novo, sem dados salvos)
  // para que o store gere o template e salve no banco
  const hasAnyData = preliminarItens.length > 0 || normativaItens.length > 0 || finalItens.length > 0

  const secoes = hasAnyData ? [
    buildSecao('PARTE_PRELIMINAR', preliminarItens),
    buildSecao('PARTE_NORMATIVA',  normativaItens),
    buildSecao('PARTE_FINAL',      finalItens),
  ] : null

  return {
    id: doc.idDocumento,
    especie: doc.siglaEspecieNormativa,
    numero_basico: doc.codigoAssuntoBasico,
    numero_secundario: doc.numeroSecundario != null ? String(doc.numeroSecundario) : null,
    assunto_basico: doc.nomeAssuntoBasico ?? doc.codigoAssuntoBasico,
    titulo: doc.tituloDocumento,
    codigo_documento: doc.codigoDocumento,
    data_criacao:      parseDtCriacao(doc.dtCriacao),
    data_alteracao:    parseDtCriacao(doc.dtAlteracao),
    data_minuta:       parseDtCriacao(doc.dtMinuta),
    data_aprovacao:    parseDtCriacao(doc.dtAprovacao),
    data_publicacao:   parseDtCriacao(doc.dtPublicacao),
    data_arquivamento: parseDtCriacao(doc.dtArquivamento),
    data_revogacao:    parseDtCriacao(doc.dtRevogacao),
    data_cancelamento: parseDtCriacao(doc.dtCancelamento),
    status: doc.statusDocumento,
    url_pdf: doc.urlPdf ?? null,
    qtd_replicas: doc.qtdReplicas ?? 0,
    versoes: [],
    secoes,
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
    const elementos = secao.elementos ?? []
    for (let i = 0; i < elementos.length; i++) {
      itens.push(converterElemento(elementos[i], secaoEnum, i + 1))
    }
  }
  return http.put(`/documentos/${id}/secoes`, { itens })
}

export async function deleteDocumento(id) {
  return http.del(`/documentos/${id}`)
}
