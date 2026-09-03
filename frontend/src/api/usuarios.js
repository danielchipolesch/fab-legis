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
    papeis: payload.papeis ?? [],
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
    papeis: payload.papeis ?? [],
  })
}

export async function redefinirSenha(id, novaSenha) {
  return http.patch(`/usuarios/${id}/senha`, { novaSenha })
}

// Candidatos pro seletor de "escolher pessoa" (enviar para revisão/revogação,
// aprovar escolhendo o publicador) -- sempre restrito à própria OM de quem
// pede, resolvida no backend a partir do token (ver UsuarioController.elegiveis).
export async function listUsuariosElegiveis(papel) {
  return http.get(`/usuarios/elegiveis?papel=${papel}`)
}

export async function listOrganizacoesMilitares() {
  return http.get('/organizacoes-militares')
}

export async function listPostosGraduacoes() {
  return http.get('/postos-graduacoes')
}
