import { defineStore } from 'pinia'
import { renumberElements, removeById, findById, promoteType, demoteType } from '@/utils/numbering.js'
import { useDocumentsStore } from './documents.js'

export const useEditorStore = defineStore('editor', {
  state: () => ({
    documentoId: null,
    documento: null,
    selectedElementId: null,
    isDirty: false,
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
      return true
    },

    loadNew() {
      const store = useDocumentsStore()
      const doc = store.createDocumento({})
      this.documento = JSON.parse(JSON.stringify(doc))
      this.documentoId = doc.id
      this.selectedElementId = null
      this.isDirty = false
    },

    selectElement(id) {
      this.selectedElementId = id
    },

    updateContent(elementId, html) {
      const el = this.findElement(elementId)
      if (el) { el.conteudo = html; this.isDirty = true }
    },

    save() {
      const store = useDocumentsStore()
      store.saveDocumento(this.documento)
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
      this.isDirty = true
    },

    addSibling(siblingId, tipo) {
      const secaoNorm = this.normativaSecao
      if (!secaoNorm) return
      const inserted = insertAfterInTree(secaoNorm.elementos, siblingId, makeNormEl(tipo))
      if (inserted) {
        this.renumberNormativa()
        this.isDirty = true
      }
    },

    addCapitulo(titulo = '') {
      const secao = this.normativaSecao
      if (!secao) return
      const novo = { id: crypto.randomUUID(), tipo: 'capitulo', numero: 0, titulo, filhos: [] }
      secao.elementos.push(novo)
      this.renumberNormativa()
      this.selectedElementId = novo.id
      this.isDirty = true
    },

    updateTitulo(elementId, titulo) {
      const el = this.findElement(elementId)
      if (el) { el.titulo = titulo; this.isDirty = true }
    },

    removeElement(id) {
      for (const secao of this.documento.secoes) {
        const removed = removeById(secao.elementos, id)
        if (removed) {
          this.renumberNormativa()
          this.isDirty = true
          if (this.selectedElementId === id) this.selectedElementId = null
          return
        }
      }
    },

    moveUp(id) {
      for (const secao of this.documento.secoes) {
        if (moveInTree(secao.elementos, id, -1)) {
          this.renumberNormativa()
          this.isDirty = true
          return
        }
      }
    },

    moveDown(id) {
      for (const secao of this.documento.secoes) {
        if (moveInTree(secao.elementos, id, 1)) {
          this.renumberNormativa()
          this.isDirty = true
          return
        }
      }
    },

    promote(id) {
      // Move element up one level in the hierarchy (becomes sibling of its parent)
      for (const secao of this.documento.secoes) {
        if (promoteInTree(secao.elementos, id)) {
          this.renumberNormativa()
          this.isDirty = true
          return
        }
      }
    },

    demote(id) {
      // Move element down one level (becomes child of its previous sibling)
      for (const secao of this.documento.secoes) {
        if (demoteInTree(secao.elementos, id)) {
          this.renumberNormativa()
          this.isDirty = true
          return
        }
      }
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
    : { id: crypto.randomUUID(), tipo, numero: 0, conteudo: '<p></p>', filhos: [] }
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
      el.tipo = promoteType(el.tipo)
      parentList.splice(parentIdx + 1, 0, el)
      return true
    }
    if (elements[i].filhos?.length) {
      if (promoteInTree(elements[i].filhos, id, elements[i], elements, i)) return true
    }
  }
  return false
}

function demoteInTree(elements, id) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      if (i === 0) return false
      const prevSibling = elements[i - 1]
      const el = elements.splice(i, 1)[0]
      el.tipo = demoteType(el.tipo)
      prevSibling.filhos = prevSibling.filhos ?? []
      prevSibling.filhos.push(el)
      return true
    }
    if (elements[i].filhos?.length && demoteInTree(elements[i].filhos, id)) return true
  }
  return false
}
