import { Node } from '@tiptap/core'

// Modelo da figura: o que fica gravado no documento (nome do nó, atributos, parse/render do HTML). Fonte ÚNICA, usada pelo
// editor (que acrescenta a view em extensions/figure.js) e pelo servidor de colaboração (collab/schema.js, que monta o
// schema sem DOM). Nada aqui pode importar Vue, componente ou o alias "@/": o collab carrega este arquivo fora do Vite.
export const FigureBase = Node.create({
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
    // titulo armazenado limpo (sem número) — o número é gerado por CSS counter
    // no preview web e pelo backend ao exportar PDF/HTML.
    // "Fonte: " é embutido no HTML para portabilidade sem CSS.
    return [
      'figure', { 'data-type': 'figura', class: 'doc-figure' },
      ['p', { class: 'figura-titulo' }, titulo ?? ''],
      ['img', { src: src ?? '', alt: alt ?? '', class: 'figura-img' }],
      ['p', { class: 'figura-fonte' }, fonte ? `Fonte: ${fonte}` : ''],
    ]
  },
})
