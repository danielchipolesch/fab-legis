import * as http from './client.js'
import { onlyDigits } from '@/utils/cpf.js'

export async function login(cpf, senha) {
  return http.post('/auth/login', { cpf: onlyDigits(cpf), senha })
}
