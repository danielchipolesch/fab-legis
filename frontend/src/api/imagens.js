import { post } from './client.js'

// Troca URLs "canônicas" do MinIO (bucket privado) por URLs assinadas de curta
// duração -- ver ImagemController.urlsAssinadas / ImagemService.gerarUrlsAssinadas.
export async function resolverUrlsAssinadas(urls) {
  if (!urls?.length) return {}
  return post('/imagens/urls-assinadas', { urls })
}
