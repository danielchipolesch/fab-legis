import * as http from './client.js'
import { onlyDigits } from '@/utils/cpf.js'

export async function login(cpf, senha) {
  return http.post('/auth/login', { cpf: onlyDigits(cpf), senha })
}

export async function refresh(refreshToken) {
  return http.post('/auth/refresh', { refreshToken })
}

export async function logout(refreshToken) {
  return http.post('/auth/logout', { refreshToken })
}
