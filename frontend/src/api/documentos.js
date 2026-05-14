import * as http from './client.js'

// ─── Normalização backend → frontend ─────────────────────────────────────────

function itemParaElemento(item) {
  return {
    id: String(item.id),
    tipo: (item.tipo ?? 'ARTIGO').toLowerCase(),
    numero: 0,
    titulo: item.titulo ?? '',
    conteudo: item.conteuto ?? '<p></p>',
    filhos: (item.children ?? []).map(c =>
      typeof c === 'object' ? itemParaElemento(c) : { id: String(c), tipo: 'artigo', numero: 0, titulo: '', conteudo: '<p></p>', filhos: [] }
    ),
  }
}

export function normalizar(doc) {
  if (!doc) return null

  const todosItens = doc.itens ?? []

  // Identifica itens raiz: aqueles que não aparecem como filhos de nenhum outro item
  const childIds = new Set()
  todosItens.forEach(item => {
    ;(item.children ?? []).forEach(c => {
      childIds.add(String(typeof c === 'object' ? c.id : c))
    })
  })
  const raiz = todosItens.filter(item => !childIds.has(String(item.id)))

  return {
    id: doc.idDocumento,
    especie: doc.siglaEspecieNormativa,
    numero_basico: doc.codigoAssuntoBasico,
    numero_secundario: doc.numeroSecundario != null ? String(doc.numeroSecundario) : null,
    assunto_basico: doc.nomeAssuntoBasico ?? doc.codigoAssuntoBasico,
    titulo: doc.tituloDocumento,
    codigo_documento: doc.codigoDocumento,
    data_criacao: doc.dtCriacao ? doc.dtCriacao.split('T')[0] : null,
    status: doc.statusDocumento,
    itens: todosItens,
    secoes: [
      { id: 'parte_preliminar', tipo: 'parte_preliminar', titulo: 'Parte Preliminar', ordem: 1, elementos: [] },
      { id: 'parte_normativa',  tipo: 'parte_normativa',  titulo: 'Parte Normativa',  ordem: 2, elementos: raiz.map(itemParaElemento) },
      { id: 'parte_final',      tipo: 'parte_final',      titulo: 'Parte Final',      ordem: 3, elementos: [] },
    ],
  }
}

// ─── Endpoints ────────────────────────────────────────────────────────────────

export async function listar() {
  const data = await http.get('/documentos/obter-todos?size=200&sortBy=id')
  return (Array.isArray(data) ? data : []).map(normalizar)
}

export async function obterPorId(id) {
  const data = await http.get(`/documentos/${id}`)
  return normalizar(data)
}

export async function criar(payload) {
  const data = await http.post('/documentos', payload)
  return normalizar(data)
}

export async function clonar(id) {
  const data = await http.post(`/documentos/${id}/clonar`)
  return normalizar(data)
}

export async function atualizar(id, titulo) {
  const data = await http.put(`/documentos/${id}`, { tituloDocumento: titulo })
  return normalizar(data)
}

export async function mudarStatus(id, status) {
  const data = await http.patch(`/documentos/${id}/status`, { status })
  return normalizar(data)
}

export async function excluir(id) {
  return http.del(`/documentos/${id}`)
}

export async function filtrar(idEspecie, idAssunto) {
  const data = await http.get(`/documentos/filtrar?especie-normativa=${idEspecie}&assunto-basico=${idAssunto}`)
  return (Array.isArray(data) ? data : []).map(normalizar)
}

export async function adicionarItem(idDocumento, item) {
  const data = await http.put(`/documentos/${idDocumento}/adicionar-item-anexo-parte-textual`, item)
  return normalizar(data)
}
