import Table, { createColGroup } from '@tiptap/extension-table'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import { mergeAttributes } from '@tiptap/core'
import {
  ALINHAMENTO_VERTICAL_PADRAO, COR_PADRAO, ESTILO_PADRAO,
  alinhamentoVerticalValido, corValida, estiloValido, fundoValido, temTitulo,
} from '../../utils/estiloDeTabela.js'

// Modelo da tabela do documento: o que fica gravado (atributos, valores padrão, parse/render do HTML). Fonte ÚNICA, usada pelo
// editor (extensions/tabela.js acrescenta a TabelaView e os plugins do navegador) e pelo servidor de colaboração
// (collab/schema.js, que monta o schema sem DOM). Nada aqui pode importar Vue, componente ou o alias "@/": o collab carrega
// este arquivo fora do Vite -- por isso o import relativo acima.
//
// Além da grade (linhas, colunas, células mescladas, largura de coluna), a tabela tem legenda -- `titulo` ("Tabela N — título",
// numerada como as figuras) e `fonte` ("Fonte: …") --, `estilo` e `cor` (ver utils/estiloDeTabela.js). Nas células: `fundo`
// (#RRGGBB, vale mais que a cor do cabeçalho e o zebrado) e `alinhamentoVertical` (top | middle | bottom).

export const TabelaBase = Table.extend({
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
}).configure({ resizable: true })

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
