import * as http from './client.js'

// ─── Normalização backend → frontend ─────────────────────────────────────────

export function normalizar(a) {
  return {
    id: a.idAssuntoBasico ?? a.id,
    codigo: a.codigo,
    nome: a.nome,
    descricao: a.descricao,
    label: `${a.codigo} — ${a.nome}`,
  }
}

// ─── Endpoints ────────────────────────────────────────────────────────────────

export async function listar() {
  const data = await http.get('/assunto-basico/obter-todos?size=200&sortBy=id')
  return (Array.isArray(data) ? data : []).map(normalizar)
}

export async function obterPorId(id) {
  const data = await http.get(`/assunto-basico/${id}`)
  return normalizar(data)
}

export async function criar(payload) {
  // payload: { codigo, nome, descricao }
  return http.post('/assunto-basico', payload)
}

export async function atualizar(id, payload) {
  return http.put(`/assunto-basico/${id}`, payload)
}

export async function excluir(id) {
  return http.del(`/assunto-basico/${id}`)
}
