import * as http from './client.js'

// Habilitação de documentos (docs/autenticacao.md): quais espécies convencionais cada OM pode criar.

// As espécies que a OM de quem está logado pode criar (as que não dependem de habilitação, como a NPA, e as habilitadas).
export async function listMinhasEspeciesCriaveis() {
  return http.get('/habilitacao-de-documentos/minhas')
}

// Administrador (CENDOC): cada OM com as espécies convencionais habilitadas.
export async function listHabilitacoes() {
  return http.get('/habilitacao-de-documentos')
}

// Administrador: substitui as espécies habilitadas da OM pela lista pedida (vazia = nenhuma).
export async function definirHabilitacao(omId, especieIds) {
  return http.put(`/habilitacao-de-documentos/${omId}`, { especieIds })
}
