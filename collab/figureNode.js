import { Node } from '@tiptap/core'

// Espelha frontend/src/extensions/figure.js -- mesmo nome, mesmos atributos,
// mesmo parseHTML/renderHTML, para que o Schema construído aqui (via
// @tiptap/core getSchema) seja idêntico ao do editor no navegador e a
// conversão Y.Doc <-> JSON TipTap não corrompa nós `figure`. Sem addNodeView:
// esta cópia só existe para montar o Schema num processo Node headless, nunca
// para renderizar -- a versão com VueNodeViewRenderer continua sendo a única
// usada pelo editor de verdade.
export const Figure = Node.create({
  name: 'figure',
  group: 'block',
  atom: true,
  draggable: true,
  selectable: true,

  addAttributes() {
    return {
      src:    { default: null },
      alt:    { default: '' },
      titulo: { default: '' },
      fonte:  { default: '' },
    }
  },

  parseHTML() {
    return [{
      tag: 'figure[data-type="figura"]',
      getAttrs: (dom) => {
        const fonteRaw = dom.querySelector('.figura-fonte')?.textContent?.trim() ?? ''
        return {
          src:    dom.querySelector('img')?.getAttribute('src') ?? null,
          alt:    dom.querySelector('img')?.getAttribute('alt') ?? '',
          titulo: dom.querySelector('.figura-titulo')?.textContent?.trim() ?? '',
          fonte:  fonteRaw.replace(/^Fonte:\s*/i, ''),
        }
      },
    }]
  },

  renderHTML({ node }) {
    const { src, alt, titulo, fonte } = node.attrs
    return [
      'figure', { 'data-type': 'figura', class: 'doc-figure' },
      ['p', { class: 'figura-titulo' }, titulo ?? ''],
      ['img', { src: src ?? '', alt: alt ?? '', class: 'figura-img' }],
      ['p', { class: 'figura-fonte' }, fonte ? `Fonte: ${fonte}` : ''],
    ]
  },
})
