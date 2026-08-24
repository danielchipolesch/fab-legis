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
    cpf: onlyDigits(payload.cpf),
    senha: payload.senha,
    omId: payload.omId,
    papeis: payload.papeis ?? [],
  })
}

export async function updateUsuario(id, payload) {
  return http.put(`/usuarios/${id}`, {
    nome: payload.nome,
    omId: payload.omId,
    ativo: payload.ativo,
    papeis: payload.papeis ?? [],
  })
}

export async function redefinirSenha(id, novaSenha) {
  return http.patch(`/usuarios/${id}/senha`, { novaSenha })
}

export async function listOrganizacoesMilitares() {
  return http.get('/organizacoes-militares')
}
