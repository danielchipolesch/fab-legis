// Paginação da prévia da NPA: distribui os blocos do documento (parágrafos, alíneas, fecho, assinaturas) em folhas,
// como o PDF faz. Só a conta -- quem mede a altura de cada bloco é o componente (NpaPreview).
//
// alturas         : altura de cada bloco, na ordem de leitura
// juntoComOProximo: por bloco, se ele não pode ficar separado do seguinte (título de capítulo/seção com o primeiro
//                   parágrafo; local e data do fecho com a primeira assinatura) -- mesma regra de keep-with-next do PDF
// primeira/demais : altura livre para blocos na primeira folha (que leva o cabeçalho) e nas demais
//
// Devolve, por folha, os índices dos blocos que ficam nela. Sempre há ao menos uma folha. Um bloco (ou grupo) mais alto
// que a folha inteira ocupa uma folha sozinho e passa do limite -- não há como partir.
export function paginar(alturas, { primeira, demais, juntoComOProximo = [] }) {
  const grupos = []
  for (let i = 0; i < alturas.length; i++) {
    const grupo = { indices: [i], altura: alturas[i] }
    while (juntoComOProximo[grupo.indices.at(-1)] && grupo.indices.at(-1) + 1 < alturas.length) {
      const proximo = grupo.indices.at(-1) + 1
      grupo.indices.push(proximo)
      grupo.altura += alturas[proximo]
    }
    grupos.push(grupo)
    i = grupo.indices.at(-1)
  }

  const folhas = [[]]
  let usado = 0
  for (const grupo of grupos) {
    const livre = (folhas.length === 1 ? primeira : demais) - usado
    if (folhas.at(-1).length > 0 && grupo.altura > livre) {
      folhas.push([])
      usado = 0
    }
    folhas.at(-1).push(...grupo.indices)
    usado += grupo.altura
  }
  return folhas
}
