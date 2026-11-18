import { onMounted, onUpdated } from 'vue'
import { resolverUrlsArmazenadas } from '@/utils/urlsArmazenadas.js'

/**
 * Troca, na prévia, a URL de cada imagem do armazenamento pela URL assinada (o bucket é privado).
 *
 * Figuras embutidas via v-html (conteudoParaHtml) e os <img> de anexo (:src="anexo.urlImagem") acabam, os dois, como
 * <img src="http..."> reais no DOM depois da renderização. É mais simples resolver aqui, uma vez por atualização do DOM
 * inteiro, do que interceptar cada ponto que gera HTML (são vários). O atributo data-resolved evita reprocessar o que já
 * foi trocado.
 *
 * @param raizRef  ref do elemento que contém as imagens
 * @param aoResolver  chamado depois de trocar as URLs (a NPA refaz a paginação: a imagem carregada muda a altura do bloco)
 * @returns a função que resolve na hora, para quem precisar chamá-la fora dos ganchos
 */
export function useImagensResolvidas(raizRef, aoResolver) {
  async function resolverImagens() {
    const raiz = raizRef.value
    if (!raiz) return
    const imgs = Array.from(raiz.querySelectorAll('img[src^="http"]:not([data-resolved])'))
    if (!imgs.length) return
    const mapa = await resolverUrlsArmazenadas(imgs.map(img => img.getAttribute('src')))
    for (const img of imgs) {
      const resolvido = mapa.get(img.getAttribute('src'))
      if (resolvido) img.setAttribute('src', resolvido)
      img.setAttribute('data-resolved', '1')
    }
    aoResolver?.()
  }

  onMounted(resolverImagens)
  onUpdated(resolverImagens)
  return resolverImagens
}
