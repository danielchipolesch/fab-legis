/**
 * Camada de acesso a dados de documentos.
 *
 * Modo mock  (VITE_USE_MOCK_API=true  — padrão de desenvolvimento):
 *   Lê e grava no localStorage; não faz requisições de rede.
 *
 * Modo real  (VITE_USE_MOCK_API=false — quando o backend estiver disponível):
 *   Delega para client.js, que aplica cache ETag/304 automaticamente.
 *
 * Para ativar o backend:
 *   1. Defina VITE_USE_MOCK_API=false em .env.local
 *   2. Defina VITE_API_BASE_URL=http://seu-backend em .env.local
 *   3. Os dados do localStorage mock não são migrados automaticamente.
 */

import * as http from './client.js'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API !== 'false'

// ─── Mock: persistência local ─────────────────────────────────────────────────

const STORAGE_KEY = 'fab-legis-documents'

function readDocs() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') } catch { return null }
}

function writeDocs(docs) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(docs)) } catch {}
}

// ─── Interface pública ────────────────────────────────────────────────────────
// Todas as funções retornam Promise para interface uniforme entre modos.

/**
 * Carrega os dados iniciais de forma síncrona (mock) ou retorna null (real).
 * Usado pelo store para popular o estado antes do primeiro render.
 */
export function loadInitial() {
  if (!USE_MOCK) return null
  return readDocs()
}

/**
 * Persiste o array completo de documentos (mock only).
 * No modo real, a persistência é por operação via CRUD abaixo.
 */
export function persist(docs) {
  if (USE_MOCK) writeDocs(docs)
}

// ─── CRUD assíncrono ──────────────────────────────────────────────────────────

export async function listDocumentos() {
  if (USE_MOCK) return readDocs() ?? []
  // ETag cache aplicado automaticamente por client.get()
  return http.get('/documentos')
}

export async function getDocumento(id) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    return docs.find(d => d.id === id) ?? null
  }
  // 304 → retorna dado em cache sem transferência de corpo
  return http.get(`/documentos/${id}`)
}

export async function createDocumento(payload) {
  if (USE_MOCK) {
    // Implementação mock mantida no store (depende de lógica de negócio local)
    throw new Error('createDocumento mock deve ser chamado pelo store')
  }
  return http.post('/documentos', payload)
}

export async function updateDocumento(id, data) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    const idx = docs.findIndex(d => d.id === id)
    if (idx !== -1) { docs[idx] = data; writeDocs(docs) }
    return data
  }
  return http.put(`/documentos/${id}`, data)
}

export async function changeDocumentoStatus(id, novoStatus) {
  if (USE_MOCK) {
    // Implementação mock mantida no store (depende de lógica de negócio local)
    throw new Error('changeDocumentoStatus mock deve ser chamado pelo store')
  }
  return http.patch(`/documentos/${id}/status`, { status: novoStatus })
}

export async function deleteDocumento(id) {
  if (USE_MOCK) {
    const docs = readDocs() ?? []
    writeDocs(docs.filter(d => d.id !== id))
    return null
  }
  return http.del(`/documentos/${id}`)
}
