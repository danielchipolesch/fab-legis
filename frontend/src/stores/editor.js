import { defineStore } from 'pinia'
import { renumberElements, removeById, promoteType, demoteType } from '@/utils/numbering.js'
import { useDocumentsStore } from './documents.js'
import * as apiDocs from '@/api/documentos.js'
import { tipoParaBackend } from '@/api/documentos.js'

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
      for (const secao of state.documento.secoes ?? []) {
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
    async load(id) {
      const docsStore = useDocumentsStore()
      const doc = await docsStore.fetchDocumento(id)
      if (!doc) return false
      this.documento = JSON.parse(JSON.stringify(doc))
      this.documentoId = id
      this.selectedElementId = null
      this.isDirty = false
      return true
    },

    selectElement(id) {
      this.selectedElementId = id
    },

    updateContent(elementId, html) {
      const el = this.findElement(elementId)
      if (el) { el.conteudo = html; this.isDirty = true }
    },

    updateTitulo(elementId, titulo) {
      const el = this.findElement(elementId)
      if (el) { el.titulo = titulo; this.isDirty = true }
    },

    async save() {
      const store = useDocumentsStore()
      await store.saveDocumento(this.documento)
      this.isDirty = false
    },

    async addFilho(parentId, tipo) {
      const updatedDoc = await apiDocs.adicionarItem(this.documentoId, {
        parentId: parseInt(parentId),
        tipo: tipoParaBackend(tipo),
        titulo: null,
        conteuto: '<p></p>',
      })
      if (updatedDoc) this._aplicarItens(updatedDoc)
    },

    async addSibling(siblingId, tipo) {
      const rawParentId = this._encontrarParentId(siblingId)
      const updatedDoc = await apiDocs.adicionarItem(this.documentoId, {
        parentId: rawParentId !== null ? parseInt(rawParentId) : null,
        tipo: tipoParaBackend(tipo),
        titulo: null,
        conteuto: '<p></p>',
      })
      if (updatedDoc) this._aplicarItens(updatedDoc)
    },

    async addArtigo() {
      const updatedDoc = await apiDocs.adicionarItem(this.documentoId, {
        parentId: null,
        tipo: 'ARTIGO',
        titulo: null,
        conteuto: '<p></p>',
      })
      if (updatedDoc) this._aplicarItens(updatedDoc)
    },

    async addCapitulo(titulo = '') {
      const updatedDoc = await apiDocs.adicionarItem(this.documentoId, {
        parentId: null,
        tipo: 'CAPITULO',
        titulo: titulo || null,
        conteuto: null,
      })
      if (updatedDoc) this._aplicarItens(updatedDoc)
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
      for (const secao of this.documento.secoes) {
        if (promoteInTree(secao.elementos, id)) {
          this.renumberNormativa()
          this.isDirty = true
          return
        }
      }
    },

    demote(id) {
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
      for (const secao of this.documento.secoes ?? []) {
        const found = findInElements(secao.elementos, id)
        if (found) return found
      }
      return null
    },

    _aplicarItens(updatedDoc) {
      const normativa = updatedDoc.secoes?.find(s => s.tipo === 'parte_normativa')
      const local = this.normativaSecao
      if (normativa && local) {
        local.elementos = normativa.elementos
        renumberElements(local.elementos)
        // Seleciona o elemento com maior ID (recém adicionado pelo banco)
        const todos = flattenElements(local.elementos)
        if (todos.length > 0) {
          const ultimo = todos.reduce((max, el) =>
            parseInt(el.id) > parseInt(max.id) ? el : max, todos[0])
          this.selectedElementId = ultimo.id
        }
      }
      this.isDirty = false
    },

    _encontrarParentId(childId) {
      for (const secao of this.documento?.secoes ?? []) {
        const pid = findParentId(secao.elementos, childId)
        if (pid !== undefined) return pid
      }
      return null
    },
  },
})

// ─── Helpers de árvore ────────────────────────────────────────────────────────

function findInElements(elements, id) {
  if (!elements) return null
  for (const el of elements) {
    if (el.id === id) return el
    const found = findInElements(el.filhos, id)
    if (found) return found
  }
  return null
}

function findParentId(elements, targetId) {
  for (const el of elements) {
    if ((el.filhos ?? []).some(c => c.id === targetId)) return el.id
    const found = findParentId(el.filhos ?? [], targetId)
    if (found !== undefined) return found
  }
  return undefined
}

function flattenElements(elements) {
  const result = []
  for (const el of elements ?? []) {
    result.push(el)
    if (el.filhos?.length) result.push(...flattenElements(el.filhos))
  }
  return result
}

function moveInTree(elements, id, direction) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) {
      const newIdx = i + direction
      if (newIdx < 0 || newIdx >= elements.length) return false
      const tmp = elements[i]; elements[i] = elements[newIdx]; elements[newIdx] = tmp
      return true
    }
    if (elements[i].filhos?.length && moveInTree(elements[i].filhos, id, direction)) return true
  }
  return false
}

function insertAfterInTree(elements, afterId, newEl) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === afterId) { elements.splice(i + 1, 0, newEl); return true }
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
