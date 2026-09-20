// O texto entre colchetes ("[descrever a finalidade da publicação]") é o que o modelo de novo
// documento deixa para o autor preencher: aparece em vermelho no PDF, no HTML e na prévia. Espelha
// TextoEntreColchetes (backend). Decidido na hora de renderizar, não gravado no conteúdo -- o
// vermelho some sozinho quando o autor substitui o trecho e apaga os colchetes.
export const COR_TEXTO_MODELO = '#FF0000'

const PADRAO = /\[[^[\]]*\]/g

function escapar(texto) {
  return texto.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const ABRE = `<span class="texto-modelo" style="color:${COR_TEXTO_MODELO}">`

// Texto puro -> HTML seguro, com os trechos entre colchetes em vermelho.
export function colchetesEmTexto(texto) {
  return escapar(texto ?? '').replace(PADRAO, trecho => `${ABRE}${trecho}</span>`)
}

// HTML -> HTML com os trechos entre colchetes (só nos textos, nunca dentro de tags) em vermelho.
// Um trecho que já está dentro de um <span style="color:..."> (cor escolhida pelo autor) fica como está.
export function destacarColchetes(html) {
  if (!html || !html.includes('[')) return html ?? ''
  const pilhaSpans = [] // true = span com cor própria
  let comCor = 0
  return html.split(/(<[^>]+>)/).map(parte => {
    if (parte.startsWith('<')) {
      if (/^<span\b/i.test(parte)) {
        const cor = /style="[^"]*\bcolor\s*:/i.test(parte)
        pilhaSpans.push(cor)
        if (cor) comCor++
      } else if (/^<\/span>/i.test(parte)) {
        if (pilhaSpans.pop()) comCor--
      }
      return parte
    }
    return comCor > 0 ? parte : parte.replace(PADRAO, trecho => `${ABRE}${trecho}</span>`)
  }).join('')
}
