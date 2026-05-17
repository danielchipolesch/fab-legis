import * as http from './client.js'

export function backendParaFrontend(doc) {
  if (!doc) return null
  return {
    id: doc.idDocumento,
    especie: doc.siglaEspecieNormativa,
    numero_basico: doc.codigoAssuntoBasico,
    numero_secundario: doc.numeroSecundario != null ? String(doc.numeroSecundario) : null,
    assunto_basico: doc.nomeAssuntoBasico ?? doc.codigoAssuntoBasico,
    titulo: doc.tituloDocumento,
    codigo_documento: doc.codigoDocumento,
    data_criacao: doc.dtCriacao ? String(doc.dtCriacao).slice(0, 10) : null,
    data_publicacao: null,
    status: doc.statusDocumento,
    versoes: [],
    itens: doc.itens ?? [],
    secoes: [],
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

export async function deleteDocumento(id) {
  return http.del(`/documentos/${id}`)
}
