/**
 * Camada de acesso a dados de documentos.
 *
 * Modo mock  (VITE_USE_MOCK_API=true  — padrão de desenvolvimento):
 *   Lê e grava no localStorage; não faz requisições de rede.
 *
 * Modo real  (VITE_USE_MOCK_API=false — com backend rodando):
 *   Delega para client.js, que aplica cache ETag/304 automaticamente.
 *   VITE_API_BASE_URL deve apontar para http://localhost:8081/v1
 */

import * as http from './client.js'

export const USE_MOCK = import.meta.env.VITE_USE_MOCK_API !== 'false'

// ─── Mock: persistência local ─────────────────────────────────────────────────

const STORAGE_KEY = 'fab-legis-documents'

function readDocs() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') } catch { return null }
}

function writeDocs(docs) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(docs)) } catch {}
}

// ─── Mapeamento backend ↔ frontend ────────────────────────────────────────────

/**
 * Converte um documento retornado pelo backend para o modelo do frontend.
 * Suporta tanto DocumentoResponseSemAnexoTextualDto quanto Com (inclui itens).
 */
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
    data_criacao: doc.dtCriacao ? doc.dtCriacao.split('T')[0] : null,
    data_publicacao: null,
    status: doc.statusDocumento,
    versoes: [],
    itens: doc.itens ?? [],
    secoes: [],
  }
}

/**
 * Converte o payload do frontend para o formato esperado pelo backend na criação.
 */
export function frontendParaBackendCreate(payload) {
  return {
    idEspecieNormativa: payload.idEspecieNormativa ?? payload.IdEspecieNormativa,
    idAssuntoBasico:    payload.idAssuntoBasico    ?? payload.IdAssuntoBasico,
    tituloDocumento:    payload.tituloDocumento,
  }
}

// ─── Interface pública ────────────────────────────────────────────────────────

export function loadInitial() {
  if (!USE_MOCK) return null
  return readDocs()
}

export function persist(docs) {
  if (USE_MOCK) writeDocs(docs)
}

// ─── CRUD assíncrono ──────────────────────────────────────────────────────────

export async function listDocumentos() {
  if (USE_MOCK) return readDocs() ?? []
  const data = await http.get('/documentos/obter-todos?size=100&sortBy=id')
  const items = Array.isArray(data) ? data : []
  return items.map(backendParaFrontend)
}

export async function getDocumento(id) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    return docs.find(d => d.id === id) ?? null
  }
  const data = await http.get(`/documentos/${id}`)
  return backendParaFrontend(data)
}

export async function createDocumento(payload) {
  if (USE_MOCK) {
    throw new Error('createDocumento mock deve ser chamado pelo store')
  }
  const body = frontendParaBackendCreate(payload)
  const data = await http.post('/documentos', body)
  return backendParaFrontend(data)
}

export async function cloneDocumento(id) {
  if (USE_MOCK) {
    throw new Error('cloneDocumento mock deve ser chamado pelo store')
  }
  const data = await http.post(`/documentos/${id}/clonar`)
  return backendParaFrontend(data)
}

export async function updateDocumento(id, data) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    const idx = docs.findIndex(d => d.id === id)
    if (idx !== -1) { docs[idx] = data; writeDocs(docs) }
    return data
  }
  const body = { tituloDocumento: data.titulo ?? data.tituloDocumento }
  const result = await http.put(`/documentos/${id}`, body)
  return backendParaFrontend(result)
}

export async function changeDocumentoStatus(id, novoStatus) {
  if (USE_MOCK) {
    throw new Error('changeDocumentoStatus mock deve ser chamado pelo store')
  }
  const result = await http.patch(`/documentos/${id}/status`, { status: novoStatus })
  return backendParaFrontend(result)
}

export async function deleteDocumento(id) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    writeDocs(docs.filter(d => d.id !== id))
    return null
  }
  return http.del(`/documentos/${id}`)
}
