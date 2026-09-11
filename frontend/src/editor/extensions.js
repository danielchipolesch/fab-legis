import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import { Figure } from '@/extensions/figure.js'

// history: false quando colaborativo -- a extensão Collaboration (Yjs) traz seu
// próprio undo/redo consciente do CRDT; manter o History padrão do StarterKit junto
// causaria dois sistemas de undo brigando pela mesma pilha. Precisa ficar em sincronia
// com collab/schema.js (mesmas extensões, mesma versão de pacote) -- ver Fase 3 do
// plano de colaboração em tempo real.
function baseExtensions({ history = true } = {}) {
  return [
    StarterKit.configure({ history }),
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    TextStyle,
    Color,
    Highlight.configure({ multicolor: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableHeader,
    TableCell,
    Figure,
  ]
}

// Usado por WysiwygEditor.vue fora do modo colaborativo (elemento ainda sem id
// persistido) e por generateHTML() nas telas de leitura (ComparisonPage,
// DocumentoPreview) -- essas nunca editam, então o histórico é irrelevante pra elas.
export const editorExtensions = baseExtensions()

// Usado só por WysiwygEditor.vue quando o elemento já tem sala Yjs (ver
// collab/server.js) -- Collaboration/CollaborationCursor são adicionadas no próprio
// WysiwygEditor.vue, não aqui, porque dependem do HocuspocusProvider da conexão.
export const editorExtensionsColaborativas = baseExtensions({ history: false })
