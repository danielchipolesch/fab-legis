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
    // no preview web e pelo backend ao exportar PDF/DOCX/HTML.
    // "Fonte: " é embutido no HTML para portabilidade sem CSS.
    return [
      'figure', { 'data-type': 'figura', class: 'doc-figure' },
      ['p', { class: 'figura-titulo' }, titulo ?? ''],
      ['img', { src: src ?? '', alt: alt ?? '', class: 'figura-img' }],
      ['p', { class: 'figura-fonte' }, fonte ? `Fonte: ${fonte}` : ''],
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(FigureView)
  },
})
