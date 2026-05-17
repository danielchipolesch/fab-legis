import * as http from './client.js'

// ─── Normalização backend → frontend ─────────────────────────────────────────

export function normalizar(e) {
  return {
    id: e.id,
    sigla: e.acronym ?? e.sigla,
    nome: e.name ?? e.nome,
    descricao: e.description ?? e.descricao,
    label: `${e.acronym ?? e.sigla} — ${e.name ?? e.nome}`,
  }
}

// ─── Endpoints ────────────────────────────────────────────────────────────────

export async function listar() {
  const data = await http.get('/especie-normativa/obter-todos?size=100&sortBy=id')
  return (Array.isArray(data) ? data : []).map(normalizar)
}

export async function obterPorId(id) {
  const data = await http.get(`/especie-normativa/${id}`)
  return normalizar(data)
}

export async function criar(payload) {
  // payload: { acronym, name, description }
  return http.post('/especie-normativa', payload)
}

export async function atualizar(id, payload) {
  return http.put(`/especie-normativa/${id}`, payload)
}

export async function excluir(id) {
  return http.del(`/especie-normativa/${id}`)
}
