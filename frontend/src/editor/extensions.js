import { Tabela, CelulaDeTabela, CabecalhoDeTabela } from '@/extensions/tabela.js'
import { Figure } from '@/extensions/figure.js'
import { montarExtensoes } from '@/editor/modelo/extensoes.js'

// A lista de extensões (e a ordem) vive em editor/modelo/extensoes.js, compartilhada com o servidor de colaboração
// (collab/schema.js) -- aqui só entram os nós com view do navegador.
const nosDoNavegador = { Tabela, CelulaDeTabela, CabecalhoDeTabela, Figure }

// Usado por WysiwygEditor.vue fora do modo colaborativo (elemento ainda sem id
// persistido) e por conteudoParaHtml() nas telas de leitura (ComparisonPage,
// DocumentoPreview, NpaPreview) -- essas nunca editam, então o histórico é irrelevante pra elas.
export const editorExtensions = montarExtensoes(nosDoNavegador)

// Usado só por WysiwygEditor.vue quando o elemento já tem sala Yjs (ver
// collab/server.js) -- Collaboration/CollaborationCursor são adicionadas no próprio
// WysiwygEditor.vue, não aqui, porque dependem do HocuspocusProvider da conexão.
export const editorExtensionsColaborativas = montarExtensoes({ ...nosDoNavegador, history: false })
