// Nome/nome de guerra de pessoa sempre aparece em caixa alta no frontend,
// independente de como foi digitado/gravado no banco -- decisão de exibição, não
// de dado (o valor enviado de volta ao backend em formulários de edição continua
// exatamente como a pessoa digitou).
export function caixaAlta(s) {
  return s ? s.toLocaleUpperCase('pt-BR') : s
}

// Remove acentos para comparação de busca "conforme digita" (ex.: filtro local
// de OM) -- não altera o texto exibido, só a comparação.
export function normalizarBusca(s) {
  return (s ?? '').toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[̀-ͯ]/g, '')
}
