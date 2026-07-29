import { defineStore } from 'pinia'
import { renumberElements, removeById, findById, promoteType, demoteType, canDemoteSubtree } from '@/utils/numbering.js'
import { useDocumentsStore } from './documents.js'

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
      const store = useDocumentsStore()
      const doc = store.getById(id)
      if (!doc) return false
      this.documento = JSON.parse(JSON.stringify(doc))
      this.documentoId = id
      this.selectedElementId = null
      this.isDirty = false
      this.hasUserEdit = false
      return true
    },

    loadNew() {
      const store = useDocumentsStore()
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
      const store = useDocumentsStore()
      await store.saveDocumento(this.documento)
      if (this.hasUserEdit && this.documento?.status === 'RASCUNHO') {
        await store.changeStatus(this.documentoId, 'MINUTA')
        this.hasUserEdit = false
      }
      const atualizado = store.getById(this.documentoId)
      if (atualizado?.status && this.documento) {
        this.documento.status = atualizado.status
      }
      this.isDirty = false
    },

    addFilho(parentId, tipo) {
      const parent = this.findElement(parentId)
      if (!parent) return
      const novo = makeNormEl(tipo)
      parent.filhos = parent.filhos ?? []
      parent.filhos.push(novo)
      this.renumberNormativa()
      this.selectedElementId = novo.id
      this.markUserEdit()
    },

    addSibling(siblingId, tipo) {
      const secaoNorm = this.normativaSecao
      if (!secaoNorm) return
      const inserted = insertAfterInTree(secaoNorm.elementos, siblingId, makeNormEl(tipo))
      if (inserted) {
        this.renumberNormativa()
        this.markUserEdit()
      }
    },

    addCapitulo(titulo = '') {
      const secao = this.normativaSecao
      if (!secao) return
      const novo = { id: crypto.randomUUID(), tipo: 'capitulo', numero: 0, titulo, filhos: [] }
      secao.elementos.push(novo)
      this.renumberNormativa()
      this.selectedElementId = novo.id
      this.markUserEdit()
    },

    updateTitulo(elementId, titulo) {
      const el = this.findElement(elementId)
      if (el) { el.titulo = titulo; this.markUserEdit() }
    },

    removeElement(id) {
      for (const secao of this.documento.secoes) {
        const removed = removeById(secao.elementos, id)
        if (removed) {
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
          return
        }
      }
    },

    moveDown(id) {
      for (const secao of this.documento.secoes) {
        if (moveInTree(secao.elementos, id, 1)) {
          this.renumberNormativa()
          this.markUserEdit()
          return
        }
      }
    },

    promote(id) {
      for (const secao of this.documento.secoes) {
        if (promoteInTree(secao.elementos, id)) {
          this.renumberNormativa()
          this.markUserEdit()
          return
        }
      }
    },

    demote(id) {
      const el = this.findElement(id)
      if (!el) return { ok: false }
      if (!canDemoteSubtree(el)) return { ok: false, reason: 'at-bottom' }
      for (const secao of this.documento.secoes) {
        if (demoteInTree(secao.elementos, id)) {
          this.renumberNormativa()
          this.markUserEdit()
          return { ok: true }
        }
      }
      return { ok: false }
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
  },
})

// ---------- Tree helpers ----------

const GROUPING_TYPES = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

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

function moveInTree(elements, id, direction) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      const newIdx = i + direction
      if (newIdx < 0 || newIdx >= elements.length) return false
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

function promoteInTree(elements, id, parent = null, parentList = null, parentIdx = null) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      if (!parent || !parentList) return false
      const el = elements.splice(i, 1)[0]
      promoteSubtreeTypes(el)
      parentList.splice(parentIdx + 1, 0, el)
      return true
    }
    if (elements[i].filhos?.length) {
      if (promoteInTree(elements[i].filhos, id, elements[i], elements, i)) return true
    }
  }
  return false
}

function demoteSubtreeTypes(el) {
  el.tipo = demoteType(el.tipo)
  for (const child of (el.filhos ?? [])) demoteSubtreeTypes(child)
}

function promoteSubtreeTypes(el) {
  el.tipo = promoteType(el.tipo)
  for (const child of (el.filhos ?? [])) promoteSubtreeTypes(child)
}

function demoteInTree(elements, id) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      if (i === 0) return false
      const prevSibling = elements[i - 1]
      const el = elements.splice(i, 1)[0]
      demoteSubtreeTypes(el)
      prevSibling.filhos = prevSibling.filhos ?? []
      prevSibling.filhos.push(el)
      return true
    }
    if (elements[i].filhos?.length && demoteInTree(elements[i].filhos, id)) return true
  }
  return false
}
