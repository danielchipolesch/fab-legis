import * as http from './client.js'

// Campos do cabeçalho e do fecho que só a NPA tem (ver NpaController no backend): setor emissor, local do fecho e
// blocos de assinatura em texto livre ({ rotulo, linhas }). Um documento de outra espécie responde 404.

export async function obterCamposDaNpa(documentoId) {
  return http.get(`/documentos/${documentoId}/npa`)
}

export async function salvarCamposDaNpa(documentoId, { setorEmissor, local, assinaturas }) {
  return http.put(`/documentos/${documentoId}/npa`, { setorEmissor, local, assinaturas })
}
