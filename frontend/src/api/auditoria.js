import * as http from './client.js'

export async function listAuditoria(filtros = {}) {
  const params = new URLSearchParams()
  if (filtros.documentoId) params.set('documentoId', filtros.documentoId)
  if (filtros.usuarioId) params.set('usuarioId', filtros.usuarioId)
  if (filtros.acao) params.set('acao', filtros.acao)
  if (filtros.dataInicio) params.set('dataInicio', `${filtros.dataInicio}T00:00:00`)
  if (filtros.dataFim) params.set('dataFim', `${filtros.dataFim}T23:59:59`)
  params.set('page', filtros.page ?? 0)
  params.set('size', filtros.size ?? 25)
  return http.get(`/auditoria?${params.toString()}`)
}
