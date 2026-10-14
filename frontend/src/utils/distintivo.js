// Espelha DistintivoDaOm (backend): o DOM da OM é uma imagem PNG ou JPEG de até 1 MB.
export const TAMANHO_MAXIMO_DO_DISTINTIVO = 1024 * 1024
const TIPOS = ['image/png', 'image/jpeg']

// Devolve a mensagem de erro, ou null se o arquivo serve.
export function erroDoDistintivo(arquivo) {
  if (!arquivo || !TIPOS.includes(String(arquivo.type).toLowerCase())) return 'O distintivo deve ser uma imagem PNG ou JPEG.'
  if (!arquivo.size) return 'O arquivo do distintivo está vazio.'
  if (arquivo.size > TAMANHO_MAXIMO_DO_DISTINTIVO) return 'O distintivo deve ter no máximo 1 MB.'
  return null
}

// Filtro da lista de OMs da tela (sigla ou nome; só as que já têm distintivo, se pedido).
export function filtrarOmsDoDistintivo(oms, busca, soComDistintivo) {
  const termo = (busca ?? '').trim().toLowerCase()
  return (oms ?? []).filter(om =>
    (!soComDistintivo || !!om.urlDistintivo) &&
    (!termo || om.sigla.toLowerCase().includes(termo) || om.nome.toLowerCase().includes(termo)))
}
