import Table, { TableView, createColGroup } from '@tiptap/extension-table'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import { mergeAttributes } from '@tiptap/core'
import { columnResizing, tableEditing } from '@tiptap/pm/tables'
import {
  ALINHAMENTO_VERTICAL_PADRAO, COR_PADRAO, ESTILO_PADRAO,
  alinhamentoVerticalValido, corValida, estiloValido, fundoValido, temTitulo,
} from '@/utils/estiloDeTabela.js'

// Tabela do documento: além da grade (linhas, colunas, células mescladas, largura de coluna), tem legenda -- `titulo`
// ("Tabela N — título", numerada como as figuras) e `fonte` ("Fonte: …") --, `estilo` e `cor` (ver utils/estiloDeTabela.js).
// Precisa ficar em sincronia com collab/tabelaNode.js (mesmos atributos e valores padrão): o servidor de colaboração monta
// o schema sem a view e descartaria um atributo que não conhecesse.

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

export const Tabela = Table.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      titulo: {
        default: '',
        parseHTML: el => el.getAttribute('data-titulo') ?? '',
        renderHTML: attrs => (temTitulo(attrs.titulo) ? { 'data-titulo': attrs.titulo } : {}),
      },
      fonte: {
        default: '',
        parseHTML: el => el.getAttribute('data-fonte') ?? '',
        renderHTML: attrs => (attrs.fonte?.trim() ? { 'data-fonte': attrs.fonte } : {}),
      },
      estilo: {
        default: ESTILO_PADRAO,
        parseHTML: el => estiloValido(el.getAttribute('data-estilo')),
        renderHTML: attrs => ({ 'data-estilo': estiloValido(attrs.estilo) }),
      },
      cor: {
        default: COR_PADRAO,
        parseHTML: el => corValida(el.getAttribute('data-cor')),
        renderHTML: attrs => ({ 'data-cor': corValida(attrs.cor) }),
      },
    }
  },

  // A legenda (título numerado e fonte) é conteúdo do nó, não do HTML ao redor: ao ler HTML colado, os <p> dela são ignorados
  // (o título e a fonte voltam pelos data-*).
  parseHTML() {
    return [
      { tag: 'p.tabela-titulo', ignore: true },
      { tag: 'p.tabela-fonte', ignore: true },
      ...(this.parent?.() ?? []),
    ]
  },

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

  // Prévia e telas de leitura: <div class="doc-tabela"> com o título (se houver), a tabela e a fonte (se houver).
  renderHTML({ node, HTMLAttributes }) {
    const { colgroup, tableWidth, tableMinWidth } = createColGroup(node, this.options.cellMinWidth)
    const tabela = [
      'table',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        style: tableWidth ? `width: ${tableWidth}` : `min-width: ${tableMinWidth}`,
      }),
      colgroup,
      ['tbody', 0],
    ]
    const partes = []
    if (temTitulo(node.attrs.titulo)) partes.push(['p', { class: 'tabela-titulo' }, node.attrs.titulo.trim()])
    partes.push(tabela)
    if (node.attrs.fonte?.trim()) partes.push(['p', { class: 'tabela-fonte' }, `Fonte: ${node.attrs.fonte.trim()}`])
    return ['div', { class: 'doc-tabela' }, ...partes]
  },
}).configure({ resizable: true, View: TabelaView })

// ─── Células ─────────────────────────────────────────────────────────────────────────────────────────────────────────
// `fundo`: cor de fundo (#RRGGBB) escolhida para a célula, que vale mais que a cor do cabeçalho e o zebrado.
// `alinhamentoVertical`: top | middle | bottom.
function atributosDeCelula(pai) {
  return {
    ...pai,
    fundo: {
      default: null,
      parseHTML: el => fundoValido(el.getAttribute('data-fundo')),
      renderHTML: attrs => {
        const fundo = fundoValido(attrs.fundo)
        return fundo ? { 'data-fundo': fundo, style: `background-color: ${fundo}` } : {}
      },
    },
    alinhamentoVertical: {
      default: ALINHAMENTO_VERTICAL_PADRAO,
      parseHTML: el => alinhamentoVerticalValido(el.getAttribute('data-valign')),
      renderHTML: attrs => {
        const v = alinhamentoVerticalValido(attrs.alinhamentoVertical)
        return v === ALINHAMENTO_VERTICAL_PADRAO ? {} : { 'data-valign': v, style: `vertical-align: ${v}` }
      },
    },
  }
}

export const CelulaDeTabela = TableCell.extend({
  addAttributes() { return atributosDeCelula(this.parent?.()) },
})

export const CabecalhoDeTabela = TableHeader.extend({
  addAttributes() { return atributosDeCelula(this.parent?.()) },
})
