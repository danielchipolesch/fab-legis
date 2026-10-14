import * as http from './client.js'
import { onlyDigits } from '@/utils/cpf.js'

export async function listUsuarios() {
  return http.get('/usuarios')
}

export async function getUsuario(id) {
  return http.get(`/usuarios/${id}`)
}

export async function createUsuario(payload) {
  return http.post('/usuarios', {
    nome: payload.nome,
    nomeGuerra: payload.nomeGuerra || null,
    cpf: onlyDigits(payload.cpf),
    email: payload.email || null,
    postoGraduacaoId: payload.postoGraduacaoId ?? null,
    senha: payload.senha,
    omId: payload.omId,
    perfis: payload.perfis ?? [],
  })
}

export async function updateUsuario(id, payload) {
  return http.put(`/usuarios/${id}`, {
    nome: payload.nome,
    nomeGuerra: payload.nomeGuerra || null,
    email: payload.email || null,
    postoGraduacaoId: payload.postoGraduacaoId ?? null,
    omId: payload.omId,
    ativo: payload.ativo,
    perfis: payload.perfis ?? [],
  })
}

export async function redefinirSenha(id, novaSenha) {
  return http.patch(`/usuarios/${id}/senha`, { novaSenha })
}

// Busca de coautor por nome/nome de guerra (CompartilharDialog.vue) -- sem
// filtro de OM, já que coautoria não é restrita a isso (ver
// UsuarioController.buscar). Só busca com 2+ caracteres.
export async function buscarUsuariosPorNome(q) {
  if (!q || q.trim().length < 2) return []
  return http.get(`/usuarios/buscar?q=${encodeURIComponent(q.trim())}`)
}

export async function listOrganizacoesMilitares() {
  return http.get('/organizacoes-militares')
}

export async function listPostosGraduacoes() {
  return http.get('/postos-graduacoes')
}

// Revisão periódica de acessos (docs/autenticacao.md): o resultado da revisão feita por quem a confere (o aviso de quem precisa de
// revisão vem na própria lista de usuários, `motivoDaRevisao`). `perfis`: os perfis que a pessoa MANTÉM (os elevados que não
// vierem são retirados; nunca concede). Devolve o usuário atualizado.
export async function revisarAcesso(usuarioId, perfis) {
  return http.post(`/revisao-de-acessos/${usuarioId}`, { perfis })
}
