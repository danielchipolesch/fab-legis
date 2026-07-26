import { Node } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import FigureView from '@/components/editor/FigureView.vue'

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
        // Strip os prefixos fixos ao reler o HTML serializado
        const tituloRaw = dom.querySelector('.figura-titulo')?.textContent?.trim() ?? ''
        const fonteRaw  = dom.querySelector('.figura-fonte')?.textContent?.trim() ?? ''
        return {
          src:    dom.querySelector('img')?.getAttribute('src') ?? null,
          alt:    dom.querySelector('img')?.getAttribute('alt') ?? '',
          titulo: tituloRaw.replace(/^Figura\s+\S+\s*[—\-]\s*/i, ''),
          fonte:  fonteRaw.replace(/^Fonte:\s*/i, ''),
        }
      },
    }]
  },

  renderHTML({ node }) {
    const { src, alt, titulo, fonte } = node.attrs
    // "Figura N" e "Fonte: " são sempre embutidos no HTML serializado para
    // garantir portabilidade ao gerar PDF nativo, HTML e DOCX no backend.
    // O backend substituirá "Figura N" pelo número real durante a exportação.
    return [
      'figure', { 'data-type': 'figura', class: 'doc-figure' },
      ['p', { class: 'figura-titulo' }, `Figura N — ${titulo ?? ''}`],
      ['img', { src: src ?? '', alt: alt ?? '', class: 'figura-img' }],
      ['p', { class: 'figura-fonte' }, fonte ? `Fonte: ${fonte}` : ''],
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(FigureView)
  },
})
