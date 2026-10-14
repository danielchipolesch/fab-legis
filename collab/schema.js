import { getSchema } from '@tiptap/core'
import { montarExtensoes } from '../frontend/src/editor/modelo/extensoes.js'
import { TabelaBase, CelulaDeTabela, CabecalhoDeTabela } from '../frontend/src/editor/modelo/tabela.js'
import { FigureBase } from '../frontend/src/editor/modelo/figure.js'

// O schema do documento vem do MESMO código do editor do navegador (frontend/src/editor/modelo/): a lista de extensões, os
// atributos da tabela e da figura. Aqui entram só os nós "base" (sem view, que é do navegador), e getSchema() -- o helper do
// @tiptap/core para construir o Schema do prosemirror-model sem Editor, sem DOM e sem Vue -- monta o schema. Assim o que o
// navegador grava e o que este serviço entende não têm como divergir; não existe cópia para manter em sincronia.
// A imagem (collab/Dockerfile) leva essas pastas do frontend para o mesmo caminho relativo.
export const editorExtensions = montarExtensoes({
  Tabela: TabelaBase,
  CelulaDeTabela,
  CabecalhoDeTabela,
  Figure: FigureBase,
})

export const schema = getSchema(editorExtensions)
