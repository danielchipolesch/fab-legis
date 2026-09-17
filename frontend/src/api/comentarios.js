import * as http from './client.js'

// Comentários em linha sobre um elemento (artigo, parágrafo, inciso...) -- revisão
// assíncrona sem editar o texto do elemento (ver ComentarioElementoController).

export async function listComentarios(documentoId) {
  return http.get(`/documentos/${documentoId}/comentarios`)
}

export async function criarComentario(documentoId, { elementoId, secao, texto, parentId }) {
  return http.post(`/documentos/${documentoId}/comentarios`, {
    elementoId, secao, texto, parentId: parentId ?? null,
  })
}

export async function resolverComentario(documentoId, comentarioId) {
  return http.patch(`/documentos/${documentoId}/comentarios/${comentarioId}/resolver`)
}

export async function reabrirComentario(documentoId, comentarioId) {
  return http.patch(`/documentos/${documentoId}/comentarios/${comentarioId}/reabrir`)
}
