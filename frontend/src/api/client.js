const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd

const JSON_HEADERS = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
}

<<<<<<< HEAD
<<<<<<< HEAD
export async function get(path) {
=======
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

// ─── Métodos públicos ─────────────────────────────────────────────────────────

export async function get(path) {
  const url = `${BASE_URL}${path}`
  const cached = getCached(url)

  const headers = { Accept: 'application/json' }
  if (cached?.etag) headers['If-None-Match'] = cached.etag

  const res = await fetch(url, { headers })

  if (res.status === 304 && cached) return cached.data
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)

  const data = await res.json()
  const etag = res.headers.get('ETag')
  if (etag) setCached(url, etag, data)
  return data
}
=======
>>>>>>> 1e3a004 (Correção de bugs)

const JSON_HEADERS = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
}

export async function get(path) {
=======
export async function get(path) {
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Accept': 'application/json' },
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export async function post(path, body) {
<<<<<<< HEAD
>>>>>>> c23bdb3 (Corrige criação de documento: @NoArgsConstructor, tipo enum mapping e error handling)
  const res = await fetch(`${BASE_URL}${path}`, {
<<<<<<< HEAD
    headers: { 'Accept': 'application/json' },
=======
    method: 'POST',
<<<<<<< HEAD
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
>>>>>>> 40c3ff6 (Adiciona Accept: application/json nos métodos POST, PUT e PATCH do client.js)
=======
    headers: JSON_HEADERS,
    body: body !== undefined ? JSON.stringify(body) : undefined,
>>>>>>> ffd8177 (Uso do HATEOAS)
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.status === 204 ? null : res.json()
}

export async function post(path, body) {
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.status === 204 ? null : res.json()
}

export async function put(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    headers: JSON_HEADERS,
=======
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
>>>>>>> 40c3ff6 (Adiciona Accept: application/json nos métodos POST, PUT e PATCH do client.js)
=======
    headers: JSON_HEADERS,
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
    headers: JSON_HEADERS,
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.status === 204 ? null : res.json()
}

export async function patch(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    headers: JSON_HEADERS,
=======
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
>>>>>>> 40c3ff6 (Adiciona Accept: application/json nos métodos POST, PUT e PATCH do client.js)
=======
    headers: JSON_HEADERS,
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
    headers: JSON_HEADERS,
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.status === 204 ? null : res.json()
}

export async function del(path) {
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'DELETE',
    headers: { 'Accept': 'application/json' },
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
<<<<<<< HEAD
=======
  const res = await fetch(`${BASE_URL}${path}`, { method: 'DELETE' })
  if (!res.ok) await throwHttpError(res)
>>>>>>> c23bdb3 (Corrige criação de documento: @NoArgsConstructor, tipo enum mapping e error handling)
=======
>>>>>>> ffd8177 (Uso do HATEOAS)
  return null
}
