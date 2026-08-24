import * as http from './client.js'

export async function listNaoLidas() {
  return http.get('/notificacoes/nao-lidas')
}

export async function listTodas(page = 0, size = 25) {
  return http.get(`/notificacoes?page=${page}&size=${size}`)
}

export async function marcarComoLida(id) {
  return http.patch(`/notificacoes/${id}/lida`)
}

export async function marcarTodasComoLidas() {
  return http.patch('/notificacoes/lidas')
}

// EventSource (SSE) não passa pelo client.js -- é uma conexão de longa
// duração própria do browser, sem Authorization header (ver
// JwtAuthenticationFilter.extrairToken), então o token vai na URL só aqui.
export function streamUrl(token) {
  return `${http.BASE_URL}/notificacoes/stream?token=${encodeURIComponent(token)}`
}
