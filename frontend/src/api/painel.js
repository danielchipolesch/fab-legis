import * as http from './client.js'
import { backendParaFrontend } from './documentos.js'

// Os três cards do hub (ver PainelController no backend): a página pedida de cada um, com os documentos no formato do
// frontend. Mais recentemente mexidos primeiro.
const CAMINHO_DO_CARTAO = {
  em_andamento: 'em-andamento',
  aguardando: 'aguardando-acao',
  publicados: 'publicados-e-revogados',
}

export async function listarCartao(cartao, { page = 0, size = 8 } = {}) {
  const resp = await http.get(`/painel/${CAMINHO_DO_CARTAO[cartao]}?page=${page}&size=${size}`)
  return {
    items: (resp?.content ?? []).map(backendParaFrontend),
    totalElements: resp?.totalElements ?? 0,
  }
}
