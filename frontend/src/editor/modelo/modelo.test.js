import { describe, it, expect } from 'vitest'
import { getSchema } from '@tiptap/core'
import { montarExtensoes } from './extensoes.js'
import { TabelaBase, CelulaDeTabela, CabecalhoDeTabela } from './tabela.js'
import { FigureBase } from './figure.js'
import { editorExtensions, editorExtensionsColaborativas } from '../extensions.js'

// O servidor de colaboração (collab/schema.js) monta o schema com os nós "base" (sem view); o navegador, com os nós que têm
// view. Os dois têm de dar o MESMO schema -- senão o servidor descartaria o que o navegador grava.
const nosBase = { Tabela: TabelaBase, CelulaDeTabela, CabecalhoDeTabela, Figure: FigureBase }

// Forma comparável do schema: nome, conteúdo e atributos (com padrão) de cada nó e de cada marca.
function forma(extensoes) {
  const schema = getSchema(extensoes)
  const atributos = (tipo) => Object.fromEntries(Object.entries(tipo.spec.attrs ?? {}).map(([n, a]) => [n, a.default ?? null]))
  return {
    nos: Object.fromEntries(Object.entries(schema.nodes).map(([nome, t]) => [nome, { conteudo: t.spec.content ?? null, grupo: t.spec.group ?? null, atributos: atributos(t) }])),
    marcas: Object.fromEntries(Object.entries(schema.marks).map(([nome, t]) => [nome, atributos(t)])),
  }
}

describe('modelo do documento compartilhado com o servidor de colaboração', () => {
  it('o schema do navegador é o mesmo do collab (nós base)', () => {
    expect(forma(editorExtensions)).toEqual(forma(montarExtensoes(nosBase)))
  })

  it('o modo colaborativo (sem histórico) não muda o schema', () => {
    expect(forma(editorExtensionsColaborativas)).toEqual(forma(montarExtensoes({ ...nosBase, history: false })))
  })

  it('a tabela e a figura guardam os atributos de legenda e de estilo', () => {
    const { nos } = forma(montarExtensoes(nosBase))
    expect(Object.keys(nos.table.atributos)).toEqual(expect.arrayContaining(['titulo', 'fonte', 'estilo', 'cor']))
    expect(Object.keys(nos.tableCell.atributos)).toEqual(expect.arrayContaining(['fundo', 'alinhamentoVertical', 'colspan', 'rowspan', 'colwidth']))
    expect(Object.keys(nos.tableHeader.atributos)).toEqual(expect.arrayContaining(['fundo', 'alinhamentoVertical']))
    expect(Object.keys(nos.figure.atributos)).toEqual(['src', 'alt', 'titulo', 'fonte'])
  })

  it('valores de tabela desconhecidos voltam ao padrão ao ler o HTML', () => {
    const attrs = getSchema(montarExtensoes(nosBase)).nodes.table.spec.attrs
    expect(attrs.estilo.default).toBe('padrao')
    expect(attrs.cor.default).toBe('azul')
  })
})
