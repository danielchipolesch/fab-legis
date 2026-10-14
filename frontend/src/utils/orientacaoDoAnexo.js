// Orientação da página A4 de um anexo de imagem. Espelha OrientacaoDoAnexo.sugeridaPara (backend): paisagem só se a imagem
// é mais larga que alta; quadrada e dimensões inválidas ficam em retrato. Serve para pré-selecionar o seletor no upload --
// o backend continua autoritativo (se o campo não vier, ele mesmo detecta pela imagem). Testes: orientacaoDoAnexo.test.js
// e OrientacaoDoAnexoTest (mesmos cenários).
export const RETRATO = 'RETRATO'
export const PAISAGEM = 'PAISAGEM'

export function orientacaoSugerida(largura, altura) {
  return largura > 0 && altura > 0 && largura > altura ? PAISAGEM : RETRATO
}

// Lê as dimensões de um arquivo de imagem no navegador (sem enviá-lo). Se não der para ler, retrato, o padrão.
export async function orientacaoSugeridaDoArquivo(arquivo) {
  try {
    const imagem = await createImageBitmap(arquivo)
    const orientacao = orientacaoSugerida(imagem.width, imagem.height)
    imagem.close?.()
    return orientacao
  } catch {
    return RETRATO
  }
}
