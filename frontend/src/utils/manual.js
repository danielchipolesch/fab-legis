// Manual do usuário: o conteúdo são arquivos Markdown em src/manual/ (NN-nome-da-pagina.md). Aqui ficam as funções puras que
// montam as páginas e as seções a partir deles e que fazem a busca; quem desenha é pages/ManualPage.vue.
import { PERFIS } from '@/utils/perfis.js'

// 'Área de Trabalho' -> 'area-de-trabalho' (o id de cada título, usado nos links com #)
export function slugDeTitulo(texto) {
  return String(texto ?? '')
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

// '/src/manual/03-elaborar-um-documento.md' -> 'elaborar-um-documento'
export function slugDaPagina(caminho) {
  const nome = String(caminho).split('/').pop().replace(/\.md$/, '')
  return nome.replace(/^\d+-/, '')
}

const COMENTARIO_DE_PERFIS = /^\s*<!--\s*perfis:\s*([^>]*?)\s*-->\s*\n?/

// Divide o Markdown de uma página nas suas seções (cada "## Título"); o que vem antes do primeiro "##" é a introdução.
// Os blocos de código não contam como título.
export function secoesDe(markdown) {
  const secoes = []
  let atual = { id: null, titulo: null, linhas: [] }
  let cerca = false
  for (const linha of markdown.split('\n')) {
    if (/^```/.test(linha)) cerca = !cerca
    const titulo = !cerca && linha.match(/^##\s+(.*?)\s*$/)
    if (titulo) {
      secoes.push(atual)
      atual = { id: slugDeTitulo(titulo[1]), titulo: titulo[1], linhas: [] }
    } else {
      atual.linhas.push(linha)
    }
  }
  secoes.push(atual)
  return secoes
    .map(s => ({ id: s.id, titulo: s.titulo, texto: s.linhas.join('\n').trim() }))
    .filter(s => s.titulo || s.texto)
}

// arquivos: { '/src/manual/01-x.md': 'texto…' } (o que o import.meta.glob devolve). A ordem é a do prefixo numérico do nome.
export function montarPaginas(arquivos) {
  return Object.keys(arquivos)
    .sort((a, b) => a.localeCompare(b))
    .map((caminho) => {
      let markdown = arquivos[caminho]
      const comentario = markdown.match(COMENTARIO_DE_PERFIS)
      const perfis = comentario ? comentario[1].split(/[\s,]+/).filter(Boolean) : []
      if (comentario) markdown = markdown.replace(COMENTARIO_DE_PERFIS, '')
      const titulo = markdown.match(/^#\s+(.*?)\s*$/m)?.[1] ?? slugDaPagina(caminho)
      const corpo = markdown.replace(/^#\s+.*\n?/m, '').trim()
      return { slug: slugDaPagina(caminho), titulo, perfis, markdown: corpo, secoes: secoesDe(corpo).filter(s => s.titulo) }
    })
}

// Os nomes por extenso dos perfis a que a página se dirige ('ELB' -> 'Elaborador'); código desconhecido é ignorado.
export function nomesDosPerfis(perfis) {
  return (perfis ?? []).map(codigo => PERFIS[codigo]?.nome).filter(Boolean)
}

// A página é para quem tem algum dos perfis dela? (uma página sem perfis é para todos)
export function ehParaMim(pagina, meusPerfis) {
  if (!pagina.perfis.length) return false
  return pagina.perfis.some(p => (meusPerfis ?? []).includes(p))
}

const normalizar = (texto) => String(texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

// Tira a marcação do Markdown para a busca e para o trecho mostrado.
const semMarcacao = (texto) => texto
  .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
  .replace(/[*`]/g, '')
  .replace(/[>|#]/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

// Busca no manual: todas as palavras do termo precisam estar na seção (sem distinguir maiúsculas nem acentos). Devolve as
// seções que casam, as de título que casa primeiro, cada uma com um trecho em volta da primeira palavra achada.
export function buscarNoManual(paginas, termo, limite = 20) {
  const palavras = normalizar(termo).split(/\s+/).filter(p => p.length >= 2)
  if (!palavras.length) return []
  const resultados = []
  for (const pagina of paginas) {
    const secoes = [{ id: null, titulo: pagina.titulo, texto: secoesDe(pagina.markdown).find(s => !s.titulo)?.texto ?? '' },
      ...secoesDe(pagina.markdown).filter(s => s.titulo)]
    for (const secao of secoes) {
      const texto = semMarcacao(secao.texto)
      const alvo = normalizar(`${pagina.titulo} ${secao.titulo ?? ''} ${texto}`)
      if (!palavras.every(p => alvo.includes(p))) continue
      const noTitulo = palavras.every(p => normalizar(`${pagina.titulo} ${secao.titulo ?? ''}`).includes(p))
      const posicao = normalizar(texto).indexOf(palavras[0])
      const inicio = Math.max(0, posicao - 40)
      const trecho = posicao < 0 ? texto.slice(0, 120) : `${inicio > 0 ? '…' : ''}${texto.slice(inicio, inicio + 140)}${inicio + 140 < texto.length ? '…' : ''}`
      resultados.push({
        pagina: pagina.slug, paginaTitulo: pagina.titulo, secaoId: secao.id, secaoTitulo: secao.titulo,
        trecho, pontos: noTitulo ? 2 : 1,
      })
    }
  }
  return resultados.sort((a, b) => b.pontos - a.pontos).slice(0, limite)
}
