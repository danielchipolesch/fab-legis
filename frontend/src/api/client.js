export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

// Referência de módulo (não um import direto do store, para evitar ciclo
// api/client.js -> stores/auth.js -> api/auth.js -> api/client.js) --
// setAuthTokenGetter/setUnauthorizedHandler/setRefreshHandler são chamados
// uma vez, no boot do app (ver stores/auth.js), plugando o client no estado
// real de autenticação.
let getToken = () => null
let onUnauthorized = () => {}
let refreshHandler = null // async () => boolean

export function setAuthTokenGetter(fn) {
  getToken = fn
}

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

export function setRefreshHandler(fn) {
  refreshHandler = fn
}

function headers(extra) {
  const token = getToken()
  return {
    'Accept': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...extra,
  }
}

// O access token agora vive pouco (15min); um 401 tenta uma única renovação
// silenciosa via refresh token antes de deslogar -- concorrente com outras
// chamadas que também tomem 401 ao mesmo tempo, todas compartilham a mesma
// promise em andamento para não disparar vários refresh em paralelo (o
// refresh token é rotacionado a cada uso, então o segundo invalidaria o
// primeiro).
let refreshEmAndamento = null

async function tentarRefresh() {
  if (!refreshHandler) return false
  if (!refreshEmAndamento) {
    refreshEmAndamento = refreshHandler().finally(() => { refreshEmAndamento = null })
  }
  return refreshEmAndamento
}

async function handleResponse(res) {
  if (res.status === 401) {
    onUnauthorized()
    const err = new Error('Sessão expirada. Entre novamente.')
    err.status = 401
    throw err
  }
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`
    try {
      const body = await res.json()
      if (body?.message) msg = body.message
    } catch { /* corpo não é JSON, mantém a mensagem padrão */ }
    const err = new Error(msg)
    err.status = res.status
    throw err
  }
  return res.status === 204 ? null : res.json()
}

async function request(method, path, extraHeaders, body, isRetry) {
  const opts = { method, headers: headers(extraHeaders) }
  if (body !== undefined) opts.body = JSON.stringify(body)
  if (method === 'GET') opts.cache = 'no-cache'

  const res = await fetch(`${BASE_URL}${path}`, opts)

  // /auth/** nunca tenta refresh sobre si mesmo -- senão um refresh token
  // expirado (401 em /auth/refresh) reentraria em tentarRefresh() e ficaria
  // esperando a própria promise em andamento resolver (deadlock).
  const elegivelParaRefresh = !isRetry && !path.startsWith('/auth/')
  if (res.status === 401 && elegivelParaRefresh && (await tentarRefresh())) {
    return request(method, path, extraHeaders, body, true)
  }
  return handleResponse(res)
}

export async function get(path) {
  return request('GET', path)
}

export async function post(path, body) {
  return request('POST', path, { 'Content-Type': 'application/json' }, body !== undefined ? body : undefined)
}

export async function put(path, body) {
  return request('PUT', path, { 'Content-Type': 'application/json' }, body)
}

export async function patch(path, body) {
  return request('PATCH', path, { 'Content-Type': 'application/json' }, body)
}

export async function del(path) {
  return request('DELETE', path)
}
