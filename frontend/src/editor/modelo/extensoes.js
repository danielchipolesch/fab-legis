import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import TableRow from '@tiptap/extension-table-row'

// A lista de extensões do editor -- as mesmas, na mesma ordem -- é definida SÓ aqui. O editor (editor/extensions.js) a monta
// com os nós que têm view (tabela e figura com NodeView); o servidor de colaboração (collab/schema.js) a monta com os nós
// "base" (sem view), só para obter o schema. O schema resultante é o mesmo nos dois lados, de modo que a conversão
// Y.Doc <-> JSON TipTap do servidor nunca descarta o que o navegador grava. Adicionou uma extensão? Entra aqui, e vale para
// os dois.
//
// history: false quando colaborativo -- a extensão Collaboration (Yjs) traz seu próprio undo/redo consciente do CRDT; manter
// o History padrão do StarterKit junto causaria dois sistemas de undo brigando pela mesma pilha.
export function montarExtensoes({ Tabela, CelulaDeTabela, CabecalhoDeTabela, Figure, history = true }) {
  return [
    StarterKit.configure({ history }),
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    TextStyle,
    Color,
    Highlight.configure({ multicolor: true }),
    Tabela,
    TableRow,
    CabecalhoDeTabela,
    CelulaDeTabela,
    Figure,
  ]
}
