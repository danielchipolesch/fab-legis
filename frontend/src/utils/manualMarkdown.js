// Markdown -> HTML do manual do usuário. Os arquivos são do próprio projeto (não vêm de usuário), mas mesmo assim o HTML cru fica
// desligado. Duas particularidades: cada título ganha um `id` (para os links com #) e um link sem endereço de site
// ("npa", "revisao-e-publicacao#aprovar-aprovador-da-om") vira link para outra página do manual (data-interno, para o
// ManualPage.vue navegar sem recarregar).
import MarkdownIt from 'markdown-it'
import { slugDeTitulo } from '@/utils/manual.js'

const md = new MarkdownIt({ html: false, linkify: false, typographer: false })

md.renderer.rules.heading_open = (tokens, i, opcoes, _env, self) => {
  const texto = tokens[i + 1]?.content ?? ''
  tokens[i].attrSet('id', slugDeTitulo(texto))
  return self.renderToken(tokens, i, opcoes)
}

const linkAberto = md.renderer.rules.link_open ?? ((tokens, i, opcoes, _env, self) => self.renderToken(tokens, i, opcoes))
md.renderer.rules.link_open = (tokens, i, opcoes, env, self) => {
  const href = tokens[i].attrGet('href') ?? ''
  if (/^(https?:|mailto:)/.test(href)) {
    tokens[i].attrSet('target', '_blank')
    tokens[i].attrSet('rel', 'noopener')
  } else if (!href.startsWith('#')) {
    tokens[i].attrSet('href', `/manual/${href}`)
    tokens[i].attrSet('data-interno', '')
  }
  return linkAberto(tokens, i, opcoes, env, self)
}

// Tabela dentro de um contêiner que rola na horizontal (celular).
md.renderer.rules.table_open = () => '<div class="tabela-rolavel"><table>'
md.renderer.rules.table_close = () => '</table></div>'

export function renderizarManual(markdown) {
  return md.render(markdown)
}
