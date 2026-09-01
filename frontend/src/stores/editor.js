import { defineStore } from 'pinia'
import { renumberElements, removeById, findById, promoteType, demoteType, canDemoteSubtree, formatLabel } from '@/utils/numbering.js'
import { useDocumentosStore } from './documentos.js'

export const useEditorStore = defineStore('editor', {
  state: () => ({
    documentoId: null,
    documento: null,
    selectedElementId: null,
    isDirty: false,
    hasUserEdit: false,
    sidebarOpen: true,
  }),

  getters: {
    selectedElement(state) {
      if (!state.documento || !state.selectedElementId) return null
      for (const secao of state.documento.secoes) {
        const found = findInElements(secao.elementos, state.selectedElementId)
        if (found) return found
      }
      return null
    },

    normativaSecao(state) {
      return state.documento?.secoes?.find(s => s.tipo === 'parte_normativa') ?? null
    },
  },

  actions: {
    load(id) {
      const store = useDocumentosStore()
      const doc = store.getById(id)
      if (!doc) return false
      this.documento = JSON.parse(JSON.stringify(doc))
      this.documentoId = id
      this.selectedElementId = null
      this.isDirty = false
      this.hasUserEdit = false
      return true
    },

    // Busca o documento de novo NO SERVIDOR (nunca do cache local) -- é chamado
    // sobretudo após um 409 de conflito de edição (ver DocumentoEditorPage.vue),
    // e nesse caso o cache do documents store ainda reflete a versão antiga que
    // causou o conflito. Reler do cache ali reintroduziria a mesma versão
    // desatualizada, fazendo o próximo salvamento colidir de novo -- e de novo --
    // até um reload de página inteira (que força um fetch de verdade). await
    // aqui garante que isDirty só volta a false depois que a versão atual
    // realmente chegou.
    async reload() {
      if (!this.documentoId) return
      const store = useDocumentosStore()
      const doc = await store.fetchDocumento(this.documentoId)
      if (!doc) return
      const prevSelectedId = this.selectedElementId
      this.documento = JSON.parse(JSON.stringify(doc))
      this.selectedElementId = prevSelectedId
      this.isDirty = false
    },

    loadNew() {
      const store = useDocumentosStore()
      const doc = store.createDocumento({})
      this.documento = JSON.parse(JSON.stringify(doc))
      this.documentoId = doc.id
      this.selectedElementId = null
      this.isDirty = false
      this.hasUserEdit = false
    },

    markUserEdit() {
      this.isDirty = true
      this.hasUserEdit = true
    },

    selectElement(id) {
      this.selectedElementId = id
    },

    updateContent(elementId, html) {
      const el = this.findElement(elementId)
      if (el) { el.conteudo = html; this.markUserEdit() }
    },

    async save() {
      const store = useDocumentosStore()
      await store.saveDocumento(this.documento)
      if (this.hasUserEdit && this.documento?.status === 'RASCUNHO') {
        await store.changeStatus(this.documentoId, 'MINUTA')
        this.hasUserEdit = false
      }
      const atualizado = store.getById(this.documentoId)
      if (atualizado?.status && this.documento) {
        this.documento.status = atualizado.status
      }
      // Mantém a versão local em dia com a do banco após salvar -- é ela que
      // vai como versaoEsperada no próximo salvamento (ver
      // DocumentoConcorrenciaService no backend).
      if (atualizado?.versao != null && this.documento) {
        this.documento.versao = atualizado.versao
      }
      this.isDirty = false
    },

    addFilho(parentId, tipo) {
      const parent = this.findElement(parentId)
      if (!parent) return
      const novo = makeNormEl(tipo)
      const filhosAtual = parent.filhos ?? []
      // Se o novo elemento não é um agrupamento (ex.: artigo direto num capítulo/seção
      // que já tem subgrupos), insere ANTES do primeiro subgrupo em vez de no final —
      // evita que ele apareça, na renderização, como se pertencesse ao último subgrupo.
      if (GROUPING_TYPES.has(parent.tipo) && !GROUPING_TYPES.has(tipo)) {
        const firstGroupIdx = filhosAtual.findIndex(f => GROUPING_TYPES.has(f.tipo))
        parent.filhos = firstGroupIdx === -1
          ? [...filhosAtual, novo]
          : [...filhosAtual.slice(0, firstGroupIdx), novo, ...filhosAtual.slice(firstGroupIdx)]
      } else {
        parent.filhos = [...filhosAtual, novo]
      }
      const secao = this.normativaSecao
      if (secao) secao.elementos = [...secao.elementos]
      this.renumberNormativa()
      this.selectedElementId = novo.id
      this.markUserEdit()
    },

    addSibling(siblingId, tipo) {
      const secaoNorm = this.normativaSecao
      if (!secaoNorm) return
      const inserted = insertAfterInTree(secaoNorm.elementos, siblingId, makeNormEl(tipo))
      if (inserted) {
        secaoNorm.elementos = [...secaoNorm.elementos]
        this.renumberNormativa()
        this.markUserEdit()
      }
    },

    addCapitulo(titulo = '') {
      const secao = this.normativaSecao
      if (!secao) return
      const novo = { id: crypto.randomUUID(), tipo: 'capitulo', numero: 0, titulo, filhos: [] }
      secao.elementos = [...secao.elementos, novo]
      this.renumberNormativa()
      this.selectedElementId = novo.id
      this.markUserEdit()
    },

    updateTitulo(elementId, titulo) {
      const el = this.findElement(elementId)
      if (el) { el.titulo = titulo; this.markUserEdit() }
    },

    removeElement(id) {
      const normSecao = this.normativaSecao
      for (const secao of this.documento.secoes) {
        const removed = removeById(secao.elementos, id)
        if (removed) {
          if (normSecao) normSecao.elementos = [...normSecao.elementos]
          this.renumberNormativa()
          this.markUserEdit()
          if (this.selectedElementId === id) this.selectedElementId = null
          return
        }
      }
    },

    moveUp(id) {
      for (const secao of this.documento.secoes) {
        if (moveInTree(secao.elementos, id, -1)) {
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: true }
        }
      }
      return { ok: false }
    },

    moveDown(id) {
      for (const secao of this.documento.secoes) {
        if (moveInTree(secao.elementos, id, 1)) {
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: true }
        }
      }
      return { ok: false }
    },

    promote(id) {
      const normSecao = this.normativaSecao
      for (const secao of this.documento.secoes) {
        const result = promoteInTree(secao.elementos, id)
        if (result === 'ok') {
          if (normSecao) normSecao.elementos = [...normSecao.elementos]
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: true }
        }
        if (result) return { ok: false, reason: result }
      }
      return { ok: false }
    },

    demote(id) {
      const el = this.findElement(id)
      if (!el) return { ok: false }
      if (!canDemoteSubtree(el)) return { ok: false, reason: 'at-bottom' }
      const normSecao = this.normativaSecao
      for (const secao of this.documento.secoes) {
        const result = demoteInTree(secao.elementos, id)
        if (result === 'ok') {
          if (normSecao) normSecao.elementos = [...normSecao.elementos]
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: true }
        }
        if (result) return { ok: false, reason: result }
      }
      return { ok: false }
    },

    getMoveTargets(elementId) {
      const el = this.findElement(elementId)
      const normSecao = this.normativaSecao
      if (!el || !normSecao) return []
      const rule = VALID_MOVE_PARENT_TYPES[el.tipo]
      if (!rule) return []
      const searchRoots = lineageSearchRoots(normSecao.elementos, elementId, rule)
      const targets = []
      const collect = (elements) => {
        for (const node of elements) {
          if (GROUPING_TYPES.has(node.tipo) && rule.parents.has(node.tipo) && !isSameOrDescendant(el, node.id)) {
            targets.push({ id: node.id, label: formatLabel(node) })
          }
          if (node.filhos?.length) collect(node.filhos)
        }
      }
      collect(searchRoots)
      if (rule.root && !normSecao.elementos.some(n => n.id === el.id)) {
        targets.push({ id: null, label: 'Nível superior (Parte Normativa)' })
      }
      return targets
    },

    moveToParent(elementId, newParentId) {
      const normSecao = this.normativaSecao
      if (!normSecao) return { ok: false }
      const el = findById(normSecao.elementos, elementId)
      if (!el) return { ok: false }

      const rule = VALID_MOVE_PARENT_TYPES[el.tipo]
      if (!rule) return { ok: false, reason: 'not-movable' }

      if (newParentId) {
        if (isSameOrDescendant(el, newParentId)) return { ok: false, reason: 'cycle' }
        const parent = findById(normSecao.elementos, newParentId)
        if (!parent || !rule.parents.has(parent.tipo)) return { ok: false, reason: 'invalid-parent' }
        if (rule.scopeToLineage) {
          const lineageRoot = findLineageRoot(normSecao.elementos, elementId)
          if (lineageRoot && lineageRoot.id !== elementId && !isSameOrDescendant(lineageRoot, newParentId)) {
            return { ok: false, reason: 'invalid-parent' }
          }
        }
      } else if (!rule.root) {
        return { ok: false, reason: 'invalid-parent' }
      }

      const removed = removeById(normSecao.elementos, elementId)
      if (!removed) return { ok: false }

      if (newParentId) {
        const parent = findById(normSecao.elementos, newParentId)
        if (!parent) {
          normSecao.elementos = [...normSecao.elementos, removed]
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: false, reason: 'invalid-parent' }
        }
        parent.filhos = [...(parent.filhos ?? []), removed]
        normSecao.elementos = [...normSecao.elementos]
      } else {
        normSecao.elementos = [...normSecao.elementos, removed]
      }

      this.renumberNormativa()
      this.markUserEdit()
      return { ok: true }
    },

    renumberNormativa() {
      const secao = this.normativaSecao
      if (secao) renumberElements(secao.elementos)
    },

    findElement(id) {
      if (!this.documento) return null
      for (const secao of this.documento.secoes) {
        const found = findInElements(secao.elementos, id)
        if (found) return found
      }
      return null
    },

    findSiblings(id) {
      if (!this.documento) return null
      for (const secao of this.documento.secoes) {
        const found = findSiblingsInElements(secao.elementos, id)
        if (found) return found
      }
      return null
    },

    canMoveUp(id) {
      const siblings = this.findSiblings(id)
      if (!siblings) return false
      const idx = siblings.findIndex(el => el.id === id)
      if (idx <= 0) return false
      return canSwapSiblings(siblings, idx, idx - 1)
    },

    canMoveDown(id) {
      const siblings = this.findSiblings(id)
      if (!siblings) return false
      const idx = siblings.findIndex(el => el.id === id)
      if (idx === -1 || idx >= siblings.length - 1) return false
      return canSwapSiblings(siblings, idx, idx + 1)
    },

    // Reordenação de artigos INCLUIDO por emenda (ainda não aprovados) entre si —
    // única exceção à vedação de renumeração de artigos. Só é permitido quando o
    // vizinho na direção pedida também for um artigo INCLUIDO (nunca contra um
    // artigo original, cuja posição é fixa). Puramente de leitura: a troca real é
    // persistida via API (emenda de reordenação), não mutação local.
    canReorderIncluido(id, direction) {
      const siblings = this.findSiblings(id)
      if (!siblings) return false
      const idx = siblings.findIndex(el => el.id === id)
      if (idx === -1) return false
      const el = siblings[idx]
      // clausulaEmenda preenchida = já consolidado numa publicação anterior — a posição
      // (e portanto a numeração com sufixo de letra) já é definitiva e não pode mudar.
      if (el.tipo !== 'artigo' || el.emendaStatus !== 'INCLUIDO' || el.clausulaEmenda) return false
      const targetIdx = idx + direction
      if (targetIdx < 0 || targetIdx >= siblings.length) return false
      const target = siblings[targetIdx]
      return target.tipo === 'artigo' && target.emendaStatus === 'INCLUIDO' && !target.clausulaEmenda
    },
  },
})

// ---------- Tree helpers ----------

const GROUPING_TYPES = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

// Regras de "mover para" (reparenteamento sem alterar tipo): para cada tipo de
// elemento, `parents` lista os tipos de agrupamento aos quais ele pode ser filho
// direto, `root` indica se ele pode existir solto no nível raiz da Parte
// Normativa (só capítulo — nunca artigo nem qualquer outro tipo), e
// `scopeToLineage` restringe os destinos ao capítulo em que o elemento já está
// (evita que um artigo "pule" para um capítulo distante e sem relação, mantendo
// o "mover para" como reorganização local). Isso espelha estritamente a
// hierarquia capítulo → seção → subseção → artigo — nunca pode ser violada, sob
// pena de o elemento ficar sem pai válido. Elementos ausentes deste mapa não são
// movíveis (ex.: parágrafo, inciso, alínea e sub-alínea só existem dentro da
// cadeia de um artigo).
const VALID_MOVE_PARENT_TYPES = {
  artigo:              { parents: new Set(['capitulo', 'secao_normativa', 'subsecao_normativa']), root: false, scopeToLineage: true },
  capitulo:            { parents: new Set([]), root: true },
  secao_normativa:     { parents: new Set(['capitulo']), root: false },
  subsecao_normativa:  { parents: new Set(['secao_normativa']), root: false },
}

function isSameOrDescendant(el, id) {
  if (el.id === id) return true
  return (el.filhos ?? []).some(child => isSameOrDescendant(child, id))
}

// Retorna o elemento de nível raiz (dentro de `rootElements`) que é o próprio
// `id` ou contém `id` em sua subárvore — ou seja, o "topo da linhagem" dele.
function findLineageRoot(rootElements, id) {
  for (const node of rootElements) {
    if (node.id === id || isSameOrDescendant(node, id)) return node
  }
  return null
}

// Escopo de busca de destinos para "mover para": se a regra exige ficar na
// mesma linhagem e o elemento já está aninhado dentro de um item de nível
// raiz (ex.: um capítulo), restringe a busca à subárvore desse item. Se o
// elemento já É de nível raiz (ex.: artigo solto), ou a regra não exige
// escopo, mantém a busca em todo o documento.
function lineageSearchRoots(rootElements, id, rule) {
  if (!rule.scopeToLineage) return rootElements
  const lineageRoot = findLineageRoot(rootElements, id)
  if (!lineageRoot || lineageRoot.id === id) return rootElements
  return [lineageRoot]
}

function makeNormEl(tipo) {
  return GROUPING_TYPES.has(tipo)
    ? { id: crypto.randomUUID(), tipo, numero: 0, titulo: '', filhos: [] }
    : { id: crypto.randomUUID(), tipo, numero: 0, conteudo: '{"type":"doc","content":[{"type":"paragraph"}]}', filhos: [] }
}

function findInElements(elements, id) {
  if (!elements) return null
  for (const el of elements) {
    if (el.id === id) return el
    const found = findInElements(el.filhos, id)
    if (found) return found
  }
  return null
}

// Retorna o array que contém o elemento de id `id` (ou seja, a lista de "irmãos"
// dele, incluindo o próprio), para permitir checar sua posição relativa.
function findSiblingsInElements(elements, id) {
  if (!elements) return null
  if (elements.some(el => el.id === id)) return elements
  for (const el of elements) {
    const found = findSiblingsInElements(el.filhos, id)
    if (found) return found
  }
  return null
}

// Uma troca entre irmãos adjacentes só é válida se ambos forem do mesmo "tipo"
// (agrupamento ou não) — trocar um artigo solto com um capítulo/seção/subseção
// vizinho inverteria a regra de que elementos soltos sempre precedem os subgrupos.
function canSwapSiblings(elements, i, newIdx) {
  return GROUPING_TYPES.has(elements[i].tipo) === GROUPING_TYPES.has(elements[newIdx].tipo)
}

function moveInTree(elements, id, direction) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      const newIdx = i + direction
      if (newIdx < 0 || newIdx >= elements.length) return false
      if (!canSwapSiblings(elements, i, newIdx)) return false
      const tmp = elements[i]
      elements[i] = elements[newIdx]
      elements[newIdx] = tmp
      return true
    }
    if (elements[i].filhos?.length && moveInTree(elements[i].filhos, id, direction)) return true
  }
  return false
}

function insertAfterInTree(elements, afterId, newEl) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === afterId) {
      elements.splice(i + 1, 0, newEl)
      return true
    }
    if (elements[i].filhos?.length && insertAfterInTree(elements[i].filhos, afterId, newEl)) return true
  }
  return false
}

// Retorna 'ok' em caso de sucesso, ou um código de motivo de bloqueio ('no-parent',
// 'ceiling'), ou null se o id não foi encontrado nesta subárvore.
function promoteInTree(elements, id, parent = null, parentList = null, parentIdx = null) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      if (!parent || !parentList) return 'no-parent'
      // Se o pai imediato é um artigo, promover o elemento o colocaria como filho
      // direto de um agrupamento (capítulo/seção/subseção), o que é inválido —
      // parágrafo/inciso/alínea/sub-alínea só podem existir dentro da cadeia do artigo.
      if (parent.tipo === 'artigo') return 'ceiling'
      const el = elements.splice(i, 1)[0]
      promoteSubtreeTypes(el)
      parentList.splice(parentIdx + 1, 0, el)
      return 'ok'
    }
    if (elements[i].filhos?.length) {
      const result = promoteInTree(elements[i].filhos, id, elements[i], elements, i)
      if (result) return result
    }
  }
  return null
}

function demoteSubtreeTypes(el) {
  el.tipo = demoteType(el.tipo)
  for (const child of (el.filhos ?? [])) demoteSubtreeTypes(child)
}

function promoteSubtreeTypes(el) {
  el.tipo = promoteType(el.tipo)
  for (const child of (el.filhos ?? [])) promoteSubtreeTypes(child)
}

// Retorna 'ok' em caso de sucesso, ou um código de motivo de bloqueio ('no-prev',
// 'invalid-sibling'), ou null se o id não foi encontrado nesta subárvore.
function demoteInTree(elements, id) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      if (i === 0) return 'no-prev'
      const prevSibling = elements[i - 1]
      // Um parágrafo/inciso/alínea/sub-alínea só pode ser filho de outro elemento
      // da cadeia de artigo — nunca de um agrupamento (capítulo/seção/subseção).
      if (GROUPING_TYPES.has(prevSibling.tipo)) return 'invalid-sibling'
      const el = elements.splice(i, 1)[0]
      demoteSubtreeTypes(el)
      prevSibling.filhos = prevSibling.filhos ?? []
      prevSibling.filhos.push(el)
      return 'ok'
    }
    if (elements[i].filhos?.length) {
      const result = demoteInTree(elements[i].filhos, id)
      if (result) return result
    }
  }
  return null
}
