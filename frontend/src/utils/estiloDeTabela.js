// Estilo e cor de uma tabela do documento. São escolhas de apresentação gravadas no nó `table` (`estilo`, `cor`) e nas
// células (`fundo`, `alinhamentoVertical`); o editor, a prévia, o PDF (XslFoContentRenderer) e o HTML exportado
// (CssDoConteudo / LeiauteHtmlDeEspecieConvencional) leem os mesmos valores e a mesma paleta -- espelhada em
// EstiloDeTabela.java (ver docs/exportacao-pdf.md, "Tabelas").

export const ESTILOS_DE_TABELA = [
  { valor: 'padrao', rotulo: 'Padrão (grade completa)' },
  { valor: 'zebrado', rotulo: 'Zebrado (linhas alternadas)' },
  { valor: 'compacto', rotulo: 'Compacto (letra e margens menores)' },
  { valor: 'aberto', rotulo: 'Aberto (só linhas horizontais)' },
]

// cabecalho: fundo das células de cabeçalho; borda: cor das linhas.
export const CORES_DE_TABELA = [
  { valor: 'azul', rotulo: 'Azul', cabecalho: '#e8edf6', borda: '#999999' },
  { valor: 'cinza', rotulo: 'Cinza', cabecalho: '#e6e6e6', borda: '#999999' },
  { valor: 'verde', rotulo: 'Verde', cabecalho: '#e3f0e6', borda: '#8fa896' },
  { valor: 'vinho', rotulo: 'Vinho', cabecalho: '#f3e3e6', borda: '#a8909a' },
  { valor: 'sem', rotulo: 'Sem cor no cabeçalho', cabecalho: 'transparent', borda: '#999999' },
]

export const ALINHAMENTOS_VERTICAIS = [
  { valor: 'top', rotulo: 'Em cima', icone: 'mdi-format-vertical-align-top' },
  { valor: 'middle', rotulo: 'No meio', icone: 'mdi-format-vertical-align-center' },
  { valor: 'bottom', rotulo: 'Embaixo', icone: 'mdi-format-vertical-align-bottom' },
]

export const ESTILO_PADRAO = 'padrao'
export const COR_PADRAO = 'azul'
export const ALINHAMENTO_VERTICAL_PADRAO = 'top'

// Cor da lista fechada acima, ou o padrão: um valor desconhecido (documento de outra versão, dado corrompido) nunca vira
// estilo estranho.
export function estiloValido(valor) {
  return ESTILOS_DE_TABELA.some(e => e.valor === valor) ? valor : ESTILO_PADRAO
}

export function corValida(valor) {
  return CORES_DE_TABELA.some(c => c.valor === valor) ? valor : COR_PADRAO
}

export function alinhamentoVerticalValido(valor) {
  return ALINHAMENTOS_VERTICAIS.some(a => a.valor === valor) ? valor : ALINHAMENTO_VERTICAL_PADRAO
}

// Fundo de célula: só #RRGGBB (o seletor de cores do editor). Qualquer outra coisa é descartada -- o valor vai parar em
// `style` (editor/prévia) e em atributo do FO/HTML, então nada além de uma cor hexadecimal passa.
export function fundoValido(valor) {
  return typeof valor === 'string' && /^#[0-9a-fA-F]{6}$/.test(valor) ? valor.toLowerCase() : null
}

// A tabela só tem legenda numerada ("Tabela N — título") quando tem título; a fonte sozinha aparece sem número.
export function temTitulo(titulo) {
  return typeof titulo === 'string' && titulo.trim() !== ''
}
