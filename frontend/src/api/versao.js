import * as http from './client.js'

// Versão do sistema em execução (pom.xml); pública, usada na tela de login. Falha em silêncio: sem versão, o rodapé some.
export async function getVersao() {
  try {
    return (await http.get('/versao')).versao ?? null
  } catch {
    return null
  }
}
