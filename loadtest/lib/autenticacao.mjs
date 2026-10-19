// Login do sistema para os scripts de carga: o mesmo fluxo OAuth2 Authorization Code + PKCE que o navegador faz (docs/autenticacao.md),
// sem navegador -- GET /oauth2/authorize, POST /login (formulário), GET de novo, troca do code em POST /oauth2/token. Devolve o access token.
// Os scripts rodam dentro da rede do Docker Compose, falando direto com o backend (http://backend:8081); o redirect_uri só é lido da
// resposta, ninguém o visita, mas precisa ser o registrado no backend (APP_OAUTH2_REDIRECT_URI).
import { createHash, randomBytes } from 'node:crypto'

const CLIENT_ID = 'fab-legis-frontend'
const base64url = (buf) => Buffer.from(buf).toString('base64url')

// Cookies de uma sessão de login (só o JSESSIONID importa): guarda o que o servidor mandar e reenvia.
function jarra() {
  const cookies = new Map()
  return {
    guardar(resposta) {
      for (const linha of resposta.headers.getSetCookie?.() ?? []) {
        const [par] = linha.split(';')
        const i = par.indexOf('=')
        cookies.set(par.slice(0, i).trim(), par.slice(i + 1))
      }
    },
    cabecalho() {
      return [...cookies].map(([k, v]) => `${k}=${v}`).join('; ')
    },
  }
}

export async function obterToken({ baseUrl, cpf, senha, redirectUri = 'http://127.0.0.1:5173/callback' }) {
  const verifier = base64url(randomBytes(32))
  const challenge = base64url(createHash('sha256').update(verifier).digest())
  const state = base64url(randomBytes(12))
  const sessao = jarra()
  const passo = async (url, opcoes = {}) => {
    const resposta = await fetch(url, { redirect: 'manual', ...opcoes, headers: { ...(opcoes.headers ?? {}), Cookie: sessao.cabecalho() } })
    sessao.guardar(resposta)
    return resposta
  }

  const parametros = new URLSearchParams({
    response_type: 'code', client_id: CLIENT_ID, redirect_uri: redirectUri, scope: 'openid',
    code_challenge: challenge, code_challenge_method: 'S256', state,
  })
  await passo(`${baseUrl}/oauth2/authorize?${parametros}`)

  const login = await passo(`${baseUrl}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username: String(cpf).replace(/\D/g, ''), password: senha }),
  })
  const volta = login.headers.get('location')
  if (!volta || /error/.test(volta)) throw new Error(`Login recusado (CPF/senha?): ${login.status} ${volta ?? ''}`)

  const autorizar = await passo(new URL(volta, baseUrl).toString())
  const destino = autorizar.headers.get('location') ?? ''
  const code = new URL(destino, baseUrl).searchParams.get('code')
  if (!code) throw new Error(`O servidor não devolveu o code: ${autorizar.status} ${destino}`)

  const token = await fetch(`${baseUrl}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'authorization_code', code, redirect_uri: redirectUri, client_id: CLIENT_ID, code_verifier: verifier }),
  })
  if (!token.ok) throw new Error(`Troca do code por token falhou: HTTP ${token.status}`)
  return (await token.json()).access_token
}
