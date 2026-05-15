/**
 * Cliente HTTP com suporte a ETag / If-None-Match.
 *
 * Fluxo:
 *   1ª requisição  → servidor responde 200 + ETag → cache local {etag, data}
 *   Requisições seguintes → envia If-None-Match → servidor responde:
 *     304 Not Modified → retorna dado em cache (zero transferência de corpo)
 *     200 OK           → dado mudou; atualiza cache e retorna novo dado
 *
 * Cache: localStorage['fab-legis-http-cache'] = { [url]: { etag, data } }
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const CACHE_KEY = 'fab-legis-http-cache'

function readCache() {
  try { return JSON.parse(localStorage.getItem(CACHE_KEY) ?? '{}') } catch { return {} }
}

function writeCache(cache) {
  try { localStorage.setItem(CACHE_KEY, JSON.stringify(cache)) } catch {}
}

function getCached(url) {
  return readCache()[url] ?? null
}

function setCached(url, etag, data) {
  const cache = readCache()
  cache[url] = { etag, data }
  writeCache(cache)
}

export function clearCache() {
  try { localStorage.removeItem(CACHE_KEY) } catch {}
}

async function throwHttpError(res) {
  let msg = `${res.status} ${res.statusText}`
  try {
    const body = await res.json()
    if (body?.message) msg = body.message
    else if (body?.error) msg = body.error
    else if (typeof body === 'string') msg = body
  } catch { /* ignora falha ao parsear o body */ }
  throw new Error(msg)
}

// ─── Métodos públicos ─────────────────────────────────────────────────────────

export async function get(path) {
  const url = `${BASE_URL}${path}`
  const cached = getCached(url)

  const headers = { Accept: 'application/json' }
  if (cached?.etag) headers['If-None-Match'] = cached.etag

  const res = await fetch(url, { headers })

  if (res.status === 304 && cached) return cached.data
  if (!res.ok) await throwHttpError(res)

  const data = await res.json()
  const etag = res.headers.get('ETag')
  if (etag) setCached(url, etag, data)
  return data
}

export async function post(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) await throwHttpError(res)
  return res.json()
}

export async function put(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) await throwHttpError(res)
  return res.status === 204 ? null : res.json()
}

export async function patch(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) await throwHttpError(res)
  return res.status === 204 ? null : res.json()
}

export async function del(path) {
  const res = await fetch(`${BASE_URL}${path}`, { method: 'DELETE' })
  if (!res.ok) await throwHttpError(res)
  return null
}
