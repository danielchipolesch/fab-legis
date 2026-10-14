import { VueNodeViewRenderer } from '@tiptap/vue-3'
import FigureView from '@/components/editor/FigureView.vue'
import { FigureBase } from '@/editor/modelo/figure.js'

// Figura do editor: o MODELO (atributos, parse/render do HTML) está em editor/modelo/figure.js, compartilhado com o servidor
// de colaboração; aqui entra só a view em Vue.
export const Figure = FigureBase.extend({
  addNodeView() {
    return VueNodeViewRenderer(FigureView)
  },
})
