// Ponte entre o campo de busca da homepage (filtra só a aba atual, por assunto ou número) e a Busca
// Textual (procura no conteúdo dos dispositivos de todos os documentos, de qualquer OM): o botão
// "Busca no conteúdo" leva o termo já digitado, e a BuscaPage o lê da URL (`?q=`) e executa a busca.

// Rota da Busca Textual, com o termo (sem espaços nas pontas) na query -- ou sem query se não há termo.
export function rotaBuscaConteudo(termo) {
  const q = (termo ?? '').trim()
  return q ? { name: 'busca', query: { q } } : { name: 'busca' }
}

// Termo vindo da URL da Busca Textual (`?q=`), ou null. Aceita o parâmetro repetido (usa o primeiro).
export function termoDaQuery(query) {
  const bruto = Array.isArray(query?.q) ? query.q[0] : query?.q
  const q = (bruto ?? '').toString().trim()
  return q || null
}
