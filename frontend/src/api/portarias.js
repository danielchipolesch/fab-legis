import * as http from './client.js'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

// ─── Endpoints ────────────────────────────────────────────────────────────────

export async function listar() {
  const data = await http.get('/portarias/obter-todas?size=100&sortBy=id')
  return Array.isArray(data) ? data : []
}

export async function obterPorId(id) {
  return http.get(`/portarias/${id}`)
}

export async function obterPdfPorDocumento(idDocumento) {
  return http.get(`/portarias/documento/${idDocumento}/pdf`)
}

export async function incluirPdf(idDocumento, file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await fetch(`${BASE_URL}/portarias/documento/${idDocumento}/incluir-pdf`, {
    method: 'POST',
    body: formData,
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}
