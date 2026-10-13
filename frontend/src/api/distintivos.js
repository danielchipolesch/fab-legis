import * as http from './client.js'

// DOM (distintivo) de cada OM, mostrado no cabeçalho da NPA -- cadastro do administrador (docs/dominio.md).

// Envia (ou troca) o distintivo da OM; devolve a OM atualizada (com urlDistintivo).
export async function definirDistintivo(omId, arquivo) {
  const form = new FormData()
  form.append('arquivo', arquivo)
  return http.putForm(`/organizacoes-militares/${omId}/distintivo`, form)
}

export async function removerDistintivo(omId) {
  return http.del(`/organizacoes-militares/${omId}/distintivo`)
}
