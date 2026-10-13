import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { limparCachesDaSessao } from './limparSessao.js'
import { primeMinioUrlCache, resolveMinioUrl } from './minioUrls.js'

const useUmaStore = defineStore('uma', { state: () => ({ itens: [1, 2], usuario: 'fulano' }) })
const useOutraStore = defineStore('outra', { state: () => ({ cache: { a: 1 } }) })

describe('limparCachesDaSessao (logout)', () => {
  let pinia
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it('devolve todas as stores ao estado inicial', () => {
    const uma = useUmaStore()
    const outra = useOutraStore()
    uma.itens.push(3)
    uma.usuario = 'beltrano'
    outra.cache.b = 2

    limparCachesDaSessao(pinia)

    expect(uma.itens).toEqual([1, 2])
    expect(uma.usuario).toBe('fulano')
    expect(outra.cache).toEqual({ a: 1 })
  })

  it('esvazia o cache de URLs assinadas', async () => {
    primeMinioUrlCache('http://minio/b/x.png', 'http://minio/b/x.png?assinada=1')
    expect(await resolveMinioUrl('http://minio/b/x.png')).toBe('http://minio/b/x.png?assinada=1')

    limparCachesDaSessao(pinia)

    // sem o cache, resolveMinioUrl voltaria a pedir ao backend (fetch indisponível aqui) -- não devolve mais a URL guardada
    await expect(resolveMinioUrl('http://minio/b/x.png')).rejects.toBeDefined()
  })

  it('aceita ausência de pinia', () => {
    expect(() => limparCachesDaSessao(undefined)).not.toThrow()
  })
})
