import * as http from './client.js'

// Busca full-text sobre o CONTEÚDO dos dispositivos (Artigo/Parágrafo/
// Inciso/...), não só metadados -- ver DocumentoBuscaController. Devolve
// dispositivos, não documentos: o mesmo documento pode aparecer várias vezes
// se vários elementos seus baterem no termo.
export async function buscar(termo, page = 0, size = 20) {
  return http.get(`/documentos/busca?q=${encodeURIComponent(termo)}&page=${page}&size=${size}`)
}
