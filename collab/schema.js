import { getSchema } from '@tiptap/core'
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
import { Figure } from './figureNode.js'

// Precisa ficar em sincronia com frontend/src/editor/extensions.js -- mesma
// lista de extensões, mesma versão do pacote @tiptap/* (ver package.json), na
// mesma ordem. getSchema() é o helper que o próprio @tiptap/core expõe para
// construir um Schema do prosemirror-model sem precisar de um Editor rodando
// (sem DOM, sem Vue) -- é o que permite reaproveitar exatamente o mesmo
// schema do editor aqui, em vez de reescrever um schema paralelo por conta
// própria (risco real de divergência silenciosa entre o que o navegador
// grava e o que este serviço entende).
export const editorExtensions = [
  StarterKit,
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

export const schema = getSchema(editorExtensions)
