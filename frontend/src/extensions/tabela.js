import { TableView } from '@tiptap/extension-table'
import { columnResizing, tableEditing } from '@tiptap/pm/tables'
import { estiloValido, corValida, temTitulo } from '@/utils/estiloDeTabela.js'
import { TabelaBase, CelulaDeTabela, CabecalhoDeTabela } from '@/editor/modelo/tabela.js'

// Tabela do editor: o MODELO (atributos, parse/render do HTML) está em editor/modelo/tabela.js, compartilhado com o servidor
// de colaboração; aqui entra só o que é do navegador -- a TabelaView (legenda editável) e os plugins de redimensionamento.

// ─── Editor: a tabela ganha o título acima e a fonte abaixo, como na figura (FigureView.vue) ─────────────────────────────
// O número ("Tabela N —") sai de um contador CSS (.tiptap-editor em WysiwygEditor.vue), contando só as tabelas com título.
export class TabelaView extends TableView {
  constructor(node, cellMinWidth, view) {
    super(node, cellMinWidth)
    this.view = view
    this.dom.classList.add('doc-tabela')

    this.tituloEl = this.criarLinha('tabela-titulo-editor', 'Título da tabela (em branco = sem legenda numerada)', 'titulo', 'Título da tabela', null)
    this.fonteEl = this.criarLinha('tabela-fonte-editor', 'Fonte (opcional)', 'fonte', 'Fonte da tabela', 'Fonte:')
    this.dom.insertBefore(this.tituloEl.linha, this.table)
    this.dom.appendChild(this.fonteEl.linha)
    this.aplicar(node)
  }

  criarLinha(classe, placeholder, atributo, rotuloAcessivel, textoDoRotulo) {
    const linha = document.createElement('div')
    linha.className = `tabela-legenda-editor ${classe}`
    linha.contentEditable = 'false'
    const rotulo = document.createElement('span')
    rotulo.className = 'tabela-rotulo'
    if (textoDoRotulo) rotulo.textContent = textoDoRotulo
    const input = document.createElement('input')
    input.type = 'text'
    input.placeholder = placeholder
    input.setAttribute('aria-label', rotuloAcessivel)
    input.addEventListener('focus', () => { input.readOnly = !this.view.editable })
    input.addEventListener('input', () => this.gravar(atributo, input.value))
    linha.append(rotulo, input)
    return { linha, input }
  }

  // Grava o atributo no nó desta tabela (achado pela identidade do nó no documento atual).
  gravar(atributo, valor) {
    if (!this.view.editable) return
    let pos = null
    this.view.state.doc.descendants((n, p) => {
      if (pos === null && n === this.node) pos = p
      return pos === null
    })
    if (pos === null) return
    this.view.dispatch(this.view.state.tr.setNodeAttribute(pos, atributo, valor))
  }

  aplicar(node) {
    const { titulo, fonte, estilo, cor } = node.attrs
    this.dom.classList.toggle('com-titulo', temTitulo(titulo))
    this.table.setAttribute('data-estilo', estiloValido(estilo))
    this.table.setAttribute('data-cor', corValida(cor))
    for (const [campo, valor] of [[this.tituloEl, titulo], [this.fonteEl, fonte]]) {
      // Não mexe no campo em que a pessoa está digitando (o valor já é o que ela digitou) -- só no que veio de fora
      // (outra pessoa editando, desfazer).
      if (campo.input.value !== (valor ?? '') && document.activeElement !== campo.input) campo.input.value = valor ?? ''
    }
  }

  update(node) {
    if (!super.update(node)) return false
    this.aplicar(node)
    return true
  }

  stopEvent(event) {
    return this.tituloEl.linha.contains(event.target) || this.fonteEl.linha.contains(event.target)
  }

  ignoreMutation(mutation) {
    if (mutation.target === this.dom || this.tituloEl.linha.contains(mutation.target) || this.fonteEl.linha.contains(mutation.target)) return true
    return super.ignoreMutation(mutation)
  }
}

export { CelulaDeTabela, CabecalhoDeTabela }

export const Tabela = TabelaBase.extend({
  // O editor colaborativo nasce `editable: false` e só libera após o primeiro `synced` (WysiwygEditor.vue). A extensão
  // original só registra o redimensionamento de colunas (e a nossa TabelaView) se o editor já estiver editável na criação --
  // e os plugins não são refeitos quando ele passa a ser editável --, então em elementos colaborativos não havia como
  // arrastar a largura da coluna. O plugin do prosemirror-tables ignora o mouse com a view não editável, então registrá-lo
  // sempre é seguro.
  addProseMirrorPlugins() {
    return [
      columnResizing({
        handleWidth: this.options.handleWidth,
        cellMinWidth: this.options.cellMinWidth,
        defaultCellMinWidth: this.options.cellMinWidth,
        View: this.options.View,
        lastColumnResizable: this.options.lastColumnResizable,
      }),
      tableEditing({ allowTableNodeSelection: this.options.allowTableNodeSelection }),
    ]
  },

  // A legenda (TabelaView) também não pode depender do plugin acima.
  addNodeView() {
    return ({ node, view }) => new TabelaView(node, this.options.cellMinWidth, view)
  },
}).configure({ resizable: true, View: TabelaView })
