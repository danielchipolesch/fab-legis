export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

// Referência de módulo (não um import direto do store, para evitar ciclo
// api/client.js -> stores/auth.js -> api/auth.js -> api/client.js) --
// setAuthTokenGetter/setUnauthorizedHandler são chamados uma vez, no boot do
// app (ver stores/auth.js), plugando o client no estado real de autenticação.
let getToken = () => null
let onUnauthorized = () => {}

export function setAuthTokenGetter(fn) {
  getToken = fn
}

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

function headers(extra) {
  const token = getToken()
  return {
    'Accept': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...extra,
  }
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

export async function get(path) {
  const res = await fetch(`${BASE_URL}${path}`, { headers: headers(), cache: 'no-cache' })
  return handleResponse(res)
}

export async function post(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: headers({ 'Content-Type': 'application/json' }),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  return handleResponse(res)
}

export async function put(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
    headers: headers({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  })
  return handleResponse(res)
}

export async function patch(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
    headers: headers({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  })
  return handleResponse(res)
}

export async function del(path) {
  const res = await fetch(`${BASE_URL}${path}`, { method: 'DELETE', headers: headers() })
  return handleResponse(res)
}
