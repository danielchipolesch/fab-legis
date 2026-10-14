import * as http from './client.js'

// Indicadores do Painel Gerencial (docs/funcionalidades.md). O servidor decide a OM: para o Gestor da OM vale sempre a própria
// (pedir outra é recusado); só o Gestor Geral filtra por OM. Datas no formato AAAA-MM-DD; sem elas valem os últimos 365 dias.
export async function getPainelGerencial(filtros = {}) {
  const params = new URLSearchParams()
  if (filtros.omId) params.set('omId', filtros.omId)
  if (filtros.especieId) params.set('especieId', filtros.especieId)
  if (filtros.dataInicio) params.set('dataInicio', filtros.dataInicio)
  if (filtros.dataFim) params.set('dataFim', filtros.dataFim)
  const consulta = params.toString()
  return http.get(`/painel-gerencial${consulta ? `?${consulta}` : ''}`)
}
