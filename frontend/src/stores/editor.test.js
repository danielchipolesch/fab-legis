import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useEditorStore } from './editor.js'
import { useDocumentosStore } from './documentos.js'

const CONVENCIONAL = 'CONVENCIONAL'
const NPA = 'COMUNICACAO_OFICIAL_PADRONIZADA'

// A árvore da parte normativa em edição: hierarquia (capítulo → seção → artigo → parágrafo → inciso...), mover, promover,
// rebaixar, os eventos estruturais de outras pessoas e a regra de reordenar artigos incluídos por emenda (LC 95/1998;
// docs/dominio.md). Os testes usam ids legíveis nos elementos.
const el = (id, tipo, filhos = [], extra = {}) => ({ id, tipo, numero: 0, filhos, ...extra })

function documentoCom(elementos, tipoDeEspecie = CONVENCIONAL) {
  return {
    id: '1',
    tipo_de_especie: tipoDeEspecie,
    situacao_local: 'MINUTA',
    secoes: [
      { tipo: 'parte_preliminar', elementos: [] },
      { tipo: 'parte_normativa', elementos },
    ],
  }
}

describe('editor store', () => {
  let store

  function abrir(elementos, tipoDeEspecie = CONVENCIONAL) {
    const docs = useDocumentosStore()
    docs.documentos = [documentoCom(elementos, tipoDeEspecie)]
    expect(store.load('1')).toBe(true)
  }

  const raiz = () => store.normativaSecao.elementos

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useEditorStore()
  })

  describe('carregar e marcar edição', () => {
    it('carrega uma cópia isolada do documento, limpa e sem edição do usuário', () => {
      abrir([el('c1', 'capitulo')])
      store.markUserEdit()

      abrir([el('c1', 'capitulo')])

      expect(store.documentoId).toBe('1')
      expect(store.isDirty).toBe(false)
      expect(store.hasUserEdit).toBe(false)
      expect(store.selectedElementId).toBeNull()
    })

    it('documento que não está no cache não carrega', () => {
      useDocumentosStore().documentos = []

      expect(store.load('99')).toBe(false)
    })

    it('editar o conteúdo ou o título suja o documento; a prévia ao vivo, não', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo', [], { conteudo: 'x' })])])

      store.setConteudoAoVivo('a1', 'ao vivo')
      expect(store.findElement('a1').conteudo).toBe('ao vivo')
      expect(store.isDirty).toBe(false)         // o Y.Doc é quem persiste, nada de autosave estrutural à toa

      store.updateContent('a1', 'digitado')
      expect(store.isDirty).toBe(true)
      expect(store.hasUserEdit).toBe(true)

      store.isDirty = false
      store.updateTitulo('c1', 'Disposições')
      expect(store.findElement('c1').titulo).toBe('Disposições')
      expect(store.isDirty).toBe(true)
    })

    it('o elemento selecionado vem da árvore, em qualquer profundidade', () => {
      abrir([el('c1', 'capitulo', [el('s1', 'secao_normativa', [el('a1', 'artigo')])])])

      store.selectElement('a1')

      expect(store.selectedElement.id).toBe('a1')
    })
  })

  describe('salvar', () => {
    it('a primeira edição de um RASCUNHO o leva para MINUTA, e a versão local acompanha a do servidor', async () => {
      const docs = useDocumentosStore()
      store.documento = { ...documentoCom([]), situacao_local: 'RASCUNHO', versao: 1 }
      store.documentoId = '1'
      store.hasUserEdit = true
      store.isDirty = true
      vi.spyOn(docs, 'saveDocumento').mockResolvedValue()
      const mudar = vi.spyOn(docs, 'changeStatus').mockResolvedValue()
      docs.documentos = [{ id: '1', situacao_bca: 'NAO_PUBLICADO', situacao_local: 'MINUTA', versao: 4 }]

      await store.save()

      expect(mudar).toHaveBeenCalledWith('1', 'MINUTA')
      expect(store.documento.situacao_local).toBe('MINUTA')
      expect(store.documento.versao).toBe(4)
      expect(store.isDirty).toBe(false)
      expect(store.hasUserEdit).toBe(false)
    })

    it('sem edição do usuário, um RASCUNHO continua RASCUNHO', async () => {
      const docs = useDocumentosStore()
      store.documento = { ...documentoCom([]), situacao_local: 'RASCUNHO' }
      store.documentoId = '1'
      vi.spyOn(docs, 'saveDocumento').mockResolvedValue()
      const mudar = vi.spyOn(docs, 'changeStatus').mockResolvedValue()
      docs.documentos = [{ id: '1' }]

      await store.save()

      expect(mudar).not.toHaveBeenCalled()
    })
  })

  describe('acrescentar elementos', () => {
    it('nas espécies convencionais, o conteúdo entra antes dos subgrupos do agrupamento', () => {
      abrir([el('c1', 'capitulo', [el('s1', 'secao_normativa'), el('s2', 'secao_normativa')])])

      store.addFilho('c1', 'artigo')

      const filhos = store.findElement('c1').filhos
      expect(filhos.map(f => f.tipo)).toEqual(['artigo', 'secao_normativa', 'secao_normativa'])
      expect(store.selectedElementId).toBe(filhos[0].id)
      expect(store.isDirty).toBe(true)
    })

    it('na NPA, seção, subseção e parágrafo dividem a sequência: o novo vai para o fim', () => {
      abrir([el('c1', 'capitulo', [el('s1', 'secao_normativa')])], NPA)

      store.addFilho('c1', 'paragrafo')

      expect(store.findElement('c1').filhos.map(f => f.tipo)).toEqual(['secao_normativa', 'paragrafo'])
    })

    it('um agrupamento novo vai sempre para o fim, mesmo nas convencionais', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo')])])

      store.addFilho('c1', 'secao_normativa')

      expect(store.findElement('c1').filhos.map(f => f.tipo)).toEqual(['artigo', 'secao_normativa'])
    })

    it('irmão entra logo depois do elemento indicado, em qualquer nível', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('a2', 'artigo')])])

      store.addSibling('a1', 'artigo')

      const filhos = store.findElement('c1').filhos
      expect(filhos).toHaveLength(3)
      expect(filhos[0].id).toBe('a1')
      expect(filhos[2].id).toBe('a2')
      expect(store.isDirty).toBe(true)
    })

    it('irmão de um elemento que não existe não muda nada', () => {
      abrir([el('c1', 'capitulo')])

      store.addSibling('nao-existe', 'artigo')

      expect(store.isDirty).toBe(false)
    })

    it('capítulo novo entra no fim da parte normativa e fica selecionado', () => {
      abrir([el('c1', 'capitulo')])

      store.addCapitulo('Das Disposições')

      expect(raiz()).toHaveLength(2)
      expect(raiz()[1].titulo).toBe('Das Disposições')
      expect(store.selectedElementId).toBe(raiz()[1].id)
    })
  })

  describe('remover', () => {
    it('remove o elemento e a seleção, se era ele', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('a2', 'artigo')])])
      store.selectElement('a1')

      store.removeElement('a1')

      expect(store.findElement('a1')).toBeNull()
      expect(store.findElement('c1').filhos.map(f => f.id)).toEqual(['a2'])
      expect(store.selectedElementId).toBeNull()
      expect(store.isDirty).toBe(true)
    })

    it('remover outro elemento mantém a seleção', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('a2', 'artigo')])])
      store.selectElement('a1')

      store.removeElement('a2')

      expect(store.selectedElementId).toBe('a1')
    })
  })

  describe('mover para cima e para baixo', () => {
    it('conteúdo troca com conteúdo e agrupamento com agrupamento', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('a2', 'artigo'), el('s1', 'secao_normativa')])])

      expect(store.moveDown('a1')).toEqual({ ok: true })
      expect(store.findElement('c1').filhos.map(f => f.id)).toEqual(['a2', 'a1', 's1'])
    })

    it('um artigo nunca passa por cima de uma seção nas convencionais (conteúdo antes dos agrupamentos)', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('s1', 'secao_normativa')])])

      expect(store.canMoveDown('a1')).toBe(false)
      expect(store.moveDown('a1')).toEqual({ ok: false })
      expect(store.canMoveUp('s1')).toBe(false)
      expect(store.findElement('c1').filhos.map(f => f.id)).toEqual(['a1', 's1'])
    })

    it('na NPA a troca é livre entre seção e parágrafo', () => {
      abrir([el('c1', 'capitulo', [el('p1', 'paragrafo'), el('s1', 'secao_normativa')])], NPA)

      expect(store.canMoveDown('p1')).toBe(true)
      expect(store.moveDown('p1')).toEqual({ ok: true })
      expect(store.findElement('c1').filhos.map(f => f.id)).toEqual(['s1', 'p1'])
    })

    it('nas pontas não há para onde mover', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo'), el('a2', 'artigo')])])

      expect(store.canMoveUp('a1')).toBe(false)
      expect(store.canMoveDown('a2')).toBe(false)
      expect(store.moveUp('a1')).toEqual({ ok: false })
    })
  })

  describe('promover e rebaixar', () => {
    it('rebaixar leva o elemento para dentro do irmão anterior e baixa o tipo dele e o dos filhos', () => {
      abrir([el('c1', 'capitulo', [
        el('a1', 'artigo'),
        el('a2', 'artigo', [el('p1', 'paragrafo')]),
      ])])

      expect(store.demote('a2')).toEqual({ ok: true })

      const a1 = store.findElement('a1')
      expect(a1.filhos.map(f => f.id)).toEqual(['a2'])
      expect(store.findElement('a2').tipo).toBe('paragrafo')
      expect(store.findElement('p1').tipo).toBe('inciso')
    })

    it('o primeiro elemento da lista não tem irmão anterior para recebê-lo', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo')])])

      expect(store.demote('a1')).toEqual({ ok: false, reason: 'no-prev' })
    })

    it('um artigo não vira filho de uma seção', () => {
      abrir([el('c1', 'capitulo', [el('s1', 'secao_normativa'), el('a1', 'artigo')])])

      expect(store.demote('a1')).toEqual({ ok: false, reason: 'invalid-sibling' })
    })

    it('a sub-alínea é o último nível: não rebaixa mais', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo', [el('p1', 'paragrafo', [el('i1', 'inciso', [
        el('l1', 'alinea'), el('l2', 'alinea', [el('sl1', 'sub_alinea')]),
      ])])])])])

      expect(store.demote('l2')).toEqual({ ok: false, reason: 'at-bottom' })
    })

    it('o parágrafo sob o artigo está no teto: não sobe para o agrupamento', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo', [el('p1', 'paragrafo', [el('i1', 'inciso')])])])])

      // O parágrafo está direto sob o artigo: o teto é o artigo (um parágrafo não vira filho de capítulo).
      expect(store.promote('p1')).toEqual({ ok: false, reason: 'ceiling' })
    })

    it('promover um inciso o faz irmão do parágrafo-pai, como parágrafo', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo', [
        el('p1', 'paragrafo', [el('i1', 'inciso', [el('l1', 'alinea')])]),
      ])])])

      expect(store.promote('i1')).toEqual({ ok: true })

      expect(store.findElement('a1').filhos.map(f => f.id)).toEqual(['p1', 'i1'])
      expect(store.findElement('i1').tipo).toBe('paragrafo')
      expect(store.findElement('l1').tipo).toBe('inciso')
    })

    it('promover um elemento de nível raiz não tem pai para onde subir', () => {
      abrir([el('c1', 'capitulo')])

      expect(store.promote('c1')).toEqual({ ok: false, reason: 'no-parent' })
    })

    it('na NPA não se promove nem se rebaixa (não há inciso nem alínea sob o artigo)', () => {
      abrir([el('c1', 'capitulo', [el('p1', 'paragrafo'), el('p2', 'paragrafo')])], NPA)

      expect(store.promote('p2')).toEqual({ ok: false, reason: 'nao-se-aplica' })
      expect(store.demote('p2')).toEqual({ ok: false, reason: 'nao-se-aplica' })
    })
  })

  describe('mover para outro agrupamento', () => {
    function arvore() {
      abrir([
        el('c1', 'capitulo', [el('s1', 'secao_normativa', [el('a1', 'artigo')]), el('s2', 'secao_normativa')]),
        el('c2', 'capitulo', [el('s3', 'secao_normativa')]),
      ])
    }

    it('um artigo só tem como destino os agrupamentos do próprio capítulo', () => {
      arvore()

      expect(store.getMoveTargets('a1').map(t => t.id)).toEqual(['c1', 's1', 's2'])
    })

    it('mover um artigo para a outra seção do capítulo', () => {
      arvore()

      expect(store.moveToParent('a1', 's2')).toEqual({ ok: true })

      expect(store.findElement('s1').filhos).toHaveLength(0)
      expect(store.findElement('s2').filhos.map(f => f.id)).toEqual(['a1'])
      expect(store.isDirty).toBe(true)
    })

    it('um artigo não salta para um capítulo distante', () => {
      arvore()

      expect(store.moveToParent('a1', 's3')).toEqual({ ok: false, reason: 'invalid-parent' })
      expect(store.findElement('s1').filhos.map(f => f.id)).toEqual(['a1'])
    })

    it('um elemento não pode ir para dentro de si mesmo nem dos seus descendentes', () => {
      arvore()

      expect(store.moveToParent('c1', 's1')).toEqual({ ok: false, reason: 'cycle' })
      expect(store.moveToParent('s1', 's1')).toEqual({ ok: false, reason: 'cycle' })
    })

    it('só capítulo existe solto no nível raiz; o artigo não', () => {
      arvore()

      expect(store.moveToParent('a1', null)).toEqual({ ok: false, reason: 'invalid-parent' })
      expect(store.moveToParent('s3', null)).toEqual({ ok: false, reason: 'invalid-parent' })
      expect(store.moveToParent('c1', null)).toEqual({ ok: true })
      expect(raiz().map(e => e.id)).toEqual(['c2', 'c1'])
    })

    it('parágrafo, inciso e alínea não são movidos sozinhos', () => {
      abrir([el('c1', 'capitulo', [el('a1', 'artigo', [el('p1', 'paragrafo')])])])

      expect(store.moveToParent('p1', 'c1')).toEqual({ ok: false, reason: 'not-movable' })
      expect(store.getMoveTargets('p1')).toEqual([])
    })

    it('na NPA o parágrafo pode ir para qualquer capítulo, seção ou subseção', () => {
      abrir([
        el('c1', 'capitulo', [el('p1', 'paragrafo')]),
        el('c2', 'capitulo', [el('s1', 'secao_normativa')]),
      ], NPA)

      expect(store.moveToParent('p1', 's1')).toEqual({ ok: true })
      expect(store.findElement('s1').filhos.map(f => f.id)).toEqual(['p1'])
    })
  })

  describe('reordenar artigos incluídos por emenda', () => {
    const incluido = (id, extra = {}) => el(id, 'artigo', [], { emendaStatus: 'INCLUIDO', ...extra })
    const original = id => el(id, 'artigo', [], { emendaStatus: 'INALTERADO' })

    it('dois incluídos ainda não publicados podem trocar de lugar entre si', () => {
      abrir([el('c1', 'capitulo', [original('a1'), incluido('a2'), incluido('a3')])])

      expect(store.canReorderIncluido('a3', -1)).toBe(true)
      expect(store.canReorderIncluido('a2', 1)).toBe(true)
    })

    it('nunca com um artigo original, que renumeraria', () => {
      abrir([el('c1', 'capitulo', [original('a1'), incluido('a2'), original('a3')])])

      expect(store.canReorderIncluido('a2', -1)).toBe(false)
      expect(store.canReorderIncluido('a2', 1)).toBe(false)
    })

    it('um incluído já publicado tem posição definitiva, e ninguém troca com ele', () => {
      abrir([el('c1', 'capitulo', [incluido('a1', { clausulaEmenda: '(incluído pela Portaria X)' }), incluido('a2')])])

      expect(store.canReorderIncluido('a1', 1)).toBe(false)
      expect(store.canReorderIncluido('a2', -1)).toBe(false)
    })

    it('só artigo, e nunca para fora da lista', () => {
      abrir([el('c1', 'capitulo', [
        el('p1', 'paragrafo', [], { emendaStatus: 'INCLUIDO' }),
        el('p2', 'paragrafo', [], { emendaStatus: 'INCLUIDO' }),
      ])])
      expect(store.canReorderIncluido('p1', 1)).toBe(false)

      abrir([el('c1', 'capitulo', [incluido('a1'), incluido('a2')])])
      expect(store.canReorderIncluido('a1', -1)).toBe(false)
      expect(store.canReorderIncluido('a2', 1)).toBe(false)
      expect(store.canReorderIncluido('nao-existe', 1)).toBe(false)
    })
  })

  describe('eventos estruturais de outras pessoas (tempo real)', () => {
    function comPersistidos() {
      abrir([el('1', 'capitulo', [el('2', 'artigo'), el('3', 'artigo')])])
    }

    it('criação remota insere um placeholder na posição indicada, sem conteúdo do servidor', () => {
      comPersistidos()

      store.aplicarEventosEstrutura([{ tipo: 'CRIADO', id: 9, parentId: 1, elementOrder: 2, elementType: 'ARTIGO' }])

      const filhos = store.findElement('1').filhos
      expect(filhos.map(f => f.id)).toEqual(['2', '9', '3'])
      expect(filhos[1].backendId).toBe(9)
      expect(filhos[1].tipo).toBe('artigo')
    })

    it('exclusão remota remove o elemento e avisa se era o selecionado', () => {
      comPersistidos()
      store.selectElement('3')

      const selecaoRemovida = store.aplicarEventosEstrutura([{ tipo: 'EXCLUIDO', id: 3 }])

      expect(selecaoRemovida).toBe(true)
      expect(store.findElement('3')).toBeNull()
      expect(store.selectedElementId).toBeNull()
    })

    it('exclusão de outro elemento não derruba a seleção', () => {
      comPersistidos()
      store.selectElement('2')

      expect(store.aplicarEventosEstrutura([{ tipo: 'EXCLUIDO', id: 3 }])).toBe(false)
      expect(store.selectedElementId).toBe('2')
    })

    it('atualização remota renomeia agrupamento e muda de pai só se o pai mudou', () => {
      abrir([el('1', 'capitulo', [el('2', 'artigo')]), el('4', 'capitulo')])

      store.aplicarEventosEstrutura([{ tipo: 'ATUALIZADO', id: 4, parentId: null, elementTitle: 'Novo título', elementOrder: 2 }])
      expect(store.findElement('4').titulo).toBe('Novo título')

      store.aplicarEventosEstrutura([{ tipo: 'ATUALIZADO', id: 2, parentId: 4, elementOrder: 1 }])
      expect(store.findElement('1').filhos).toHaveLength(0)
      expect(store.findElement('4').filhos.map(f => f.id)).toEqual(['2'])
    })

    it('evento sobre elemento que esta pessoa não tem na árvore é ignorado sem erro', () => {
      comPersistidos()

      expect(store.aplicarEventosEstrutura([{ tipo: 'EXCLUIDO', id: 777 }])).toBe(false)
      expect(store.aplicarEventosEstrutura([])).toBe(false)
    })
  })
})
