// Imprime um access token (para usar à mão, ex.: collab/test-two-clients.mjs). Uso: node obter-token.mjs
//   BASE_URL (padrão http://backend:8081)   CPF   SENHA
import { obterToken } from './lib/autenticacao.mjs'

const { BASE_URL = 'http://backend:8081', CPF, SENHA } = process.env
if (!CPF || !SENHA) { console.error('Informe CPF e SENHA nas variáveis de ambiente.'); process.exit(1) }
console.log(await obterToken({ baseUrl: BASE_URL, cpf: CPF, senha: SENHA }))
