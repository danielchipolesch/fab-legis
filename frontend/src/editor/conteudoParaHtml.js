import { getSchema, getHTMLFromFragment } from '@tiptap/vue-3'
import { Node } from '@tiptap/pm/model'
import { editorExtensions } from '@/editor/extensions.js'

// JSON TipTap (string, como gravado em `conteudo`) -> HTML, para as telas de leitura (prévia, comparação).
//
// Não usa generateHTML() de '@tiptap/html': ele serializa com zeed-dom, e o prosemirror-model (1.25) aplica o atributo
// `style` via `dom.style.cssText`, que o zeed-dom não implementa -- todo `style` sumia do HTML: cor do texto (o texto de
// orientação entre colchetes, em vermelho), realce e alinhamento apareciam sem formatação na prévia, embora o editor, o
// PDF e o HTML exportado (que têm serializador próprio) os mostrem. getHTMLFromFragment() do @tiptap/core usa o DOM
// do navegador, que trata `style` normalmente.
let schema = null

export function conteudoParaHtml(conteudo) {
  if (!conteudo) return ''
  try {
    schema ??= getSchema(editorExtensions)
    const doc = Node.fromJSON(schema, JSON.parse(conteudo))
    return getHTMLFromFragment(doc.content, schema)
  } catch {
    return ''
  }
}
