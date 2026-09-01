import { resolverUrlsAssinadas } from '@/api/imagens.js'

// Cache local: URL canônica -> { assinada, expiraEm } -- evita re-resolver a mesma
// URL repetidamente na mesma sessão do navegador. TTL um pouco menor que a validade
// real da URL assinada no backend (60 min, ver ImagemService.EXPIRY_MINUTES), pra
// nunca entregar algo prestes a expirar.
const TTL_MS = 50 * 60 * 1000
const cache = new Map()

// Só URLs absolutas http(s) podem ser do MinIO -- caminhos relativos (assets do
// próprio frontend, ex. /brasao-fab.png) e data: URIs não precisam passar pelo
// backend; devolvidos como estão.
function precisaResolver(url) {
  return typeof url === 'string' && /^https?:\/\//.test(url)
}

// Resolve várias URLs de uma vez (um único round-trip). Devolve um Map na mesma
// ordem de entrada; URLs que não são do MinIO (ou sem correspondência no backend)
// voltam inalteradas.
export async function resolveMinioUrls(urls) {
  const agora = Date.now()
  const pendentes = []
  const resultado = new Map()

  for (const url of new Set(urls)) {
    if (!precisaResolver(url)) {
      resultado.set(url, url)
      continue
    }
    const cached = cache.get(url)
    if (cached && cached.expiraEm > agora) {
      resultado.set(url, cached.assinada)
      continue
    }
    pendentes.push(url)
  }

  if (pendentes.length) {
    const assinadas = await resolverUrlsAssinadas(pendentes)
    for (const url of pendentes) {
      const assinada = assinadas[url] ?? url
      cache.set(url, { assinada, expiraEm: agora + TTL_MS })
      resultado.set(url, assinada)
    }
  }

  return resultado
}

export async function resolveMinioUrl(url) {
  const mapa = await resolveMinioUrls([url])
  return mapa.get(url) ?? url
}
