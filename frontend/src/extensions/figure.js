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
      getAttrs: (dom) => ({
        src:    dom.querySelector('img')?.getAttribute('src') ?? null,
        alt:    dom.querySelector('img')?.getAttribute('alt') ?? '',
        titulo: dom.querySelector('.figura-titulo')?.textContent?.trim() ?? '',
        fonte:  dom.querySelector('.figura-fonte')?.textContent?.trim() ?? '',
      }),
    }]
  },

  renderHTML({ node }) {
    const { src, alt, titulo, fonte } = node.attrs
    return [
      'figure', { 'data-type': 'figura', class: 'doc-figure' },
      ['p', { class: 'figura-titulo' }, titulo ?? ''],
      ['img', { src: src ?? '', alt: alt ?? '', class: 'figura-img' }],
      ['p', { class: 'figura-fonte' }, fonte ?? ''],
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(FigureView)
  },
})
