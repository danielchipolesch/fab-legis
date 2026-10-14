import * as http from './client.js'

// Revisão periódica de acessos (docs/autenticacao.md): os usuários com papel elevado que quem pergunta revisa, e o resultado da revisão.
export async function listRevisaoDeAcessos() {
  return http.get('/revisao-de-acessos')
}

// `papeis`: os papéis que a pessoa MANTÉM (os elevados que não vierem são retirados; nunca concede).
export async function revisarAcesso(usuarioId, papeis) {
  return http.post(`/revisao-de-acessos/${usuarioId}`, { papeis })
}
