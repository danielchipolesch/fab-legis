// Tira os <p> do HTML que o TipTap gera (generateHTML) e mantém o que há dentro deles -- cor, negrito, itálico,
// sublinhado, realce, quebra de linha --, para o conteúdo de um elemento entrar na mesma linha do rótulo ("Art. 1º",
// "I -"...) na prévia. Reduzir o conteúdo a texto puro perdia essas marcas (era o que fazia o texto de orientação entre
// colchetes, em vermelho, aparecer preto na prévia, embora o editor, o PDF e o HTML o mostrem em vermelho).
export function htmlEmLinha(html) {
  return (html ?? '').replace(/<\/?p\b[^>]*>/g, '').trim()
}
