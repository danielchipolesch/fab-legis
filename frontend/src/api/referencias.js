import * as http from './client.js'

export async function listEspeciesNormativas() {
  const data = await http.get('/especie-normativa/obter-todos?size=50&sortBy=id')
  return Array.isArray(data) ? data : []
}

export async function listAssuntosBasicos() {
  const data = await http.get('/assunto-basico/obter-todos?size=200&sortBy=id')
  return Array.isArray(data) ? data : []
}

export function normalizeEspecie(e) {
  return {
    id:    e.id,
    sigla: e.acronym ?? e.sigla,
    nome:  e.name    ?? e.nome,
    label: `${e.acronym ?? e.sigla} — ${e.name ?? e.nome}`,
  }
}

export function normalizeAssunto(a) {
  return {
    id:     a.idAssuntoBasico ?? a.id,
    codigo: a.codigo,
    nome:   a.nome,
    label:  `${a.codigo} — ${a.nome}`,
  }
}
