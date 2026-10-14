import { limparMinioUrlCache } from '@/utils/minioUrls.js'

// Tudo o que a sessão de quem saiu guardou no navegador, para a próxima pessoa não herdar: o estado de TODAS as stores do
// Pinia (documentos, painel, busca, editor, a própria autenticação…) volta ao inicial e o cache de URLs assinadas do MinIO
// é esvaziado. Percorre as stores registradas em vez de listá-las, para uma store nova já entrar na limpeza sem esquecimento.
// Chamada pelo logout, depois de apagar o token do armazenamento local (o estado inicial da autenticação lê dele).
export function limparCachesDaSessao(pinia) {
  pinia?._s?.forEach(store => store.$reset())
  limparMinioUrlCache()
}
