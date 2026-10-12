import Table, { createColGroup } from '@tiptap/extension-table'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import { mergeAttributes } from '@tiptap/core'

// Espelha frontend/src/extensions/tabela.js -- mesmos atributos (titulo, fonte, estilo, cor na tabela; fundo e
// alinhamentoVertical nas células) e mesmos valores padrão, para o Schema construído aqui (getSchema, sem DOM nem view) ser
// idêntico ao do editor e a conversão Y.Doc <-> JSON TipTap não descartar nenhum deles. Sem a TabelaView: ela é só do
// navegador.
const ESTILOS = ['padrao', 'zebrado', 'compacto', 'aberto']
const CORES = ['azul', 'cinza', 'verde', 'vinho', 'sem']
const ALINHAMENTOS = ['top', 'middle', 'bottom']
const estilo = v => (ESTILOS.includes(v) ? v : 'padrao')
const cor = v => (CORES.includes(v) ? v : 'azul')
const alinhamento = v => (ALINHAMENTOS.includes(v) ? v : 'top')
const fundo = v => (typeof v === 'string' && /^#[0-9a-fA-F]{6}$/.test(v) ? v.toLowerCase() : null)
const temTitulo = t => typeof t === 'string' && t.trim() !== ''

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
        default: 'padrao',
        parseHTML: el => estilo(el.getAttribute('data-estilo')),
        renderHTML: attrs => ({ 'data-estilo': estilo(attrs.estilo) }),
      },
      cor: {
        default: 'azul',
        parseHTML: el => cor(el.getAttribute('data-cor')),
        renderHTML: attrs => ({ 'data-cor': cor(attrs.cor) }),
      },
    }
  },

  parseHTML() {
    return [
      { tag: 'p.tabela-titulo', ignore: true },
      { tag: 'p.tabela-fonte', ignore: true },
      ...(this.parent?.() ?? []),
    ]
  },

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
}).configure({ resizable: true })

function atributosDeCelula(pai) {
  return {
    ...pai,
    fundo: {
      default: null,
      parseHTML: el => fundo(el.getAttribute('data-fundo')),
      renderHTML: attrs => {
        const f = fundo(attrs.fundo)
        return f ? { 'data-fundo': f, style: `background-color: ${f}` } : {}
      },
    },
    alinhamentoVertical: {
      default: 'top',
      parseHTML: el => alinhamento(el.getAttribute('data-valign')),
      renderHTML: attrs => {
        const v = alinhamento(attrs.alinhamentoVertical)
        return v === 'top' ? {} : { 'data-valign': v, style: `vertical-align: ${v}` }
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
