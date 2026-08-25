import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'
import { renumberElements, renumberElementsEmAlteracao } from '@/utils/numbering.js'
import * as api from '@/api/documents.js'

function renumerarSecaoNormativa(doc) {
  const normativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
  if (!normativa?.elementos?.length) return
  if (doc.status === 'EM_ALTERACAO' || doc.status === 'ALTERADO') {
    renumberElementsEmAlteracao(normativa.elementos)
  } else {
    renumberElements(normativa.elementos)
  }
}

function makeElement(tipo, numero, conteudo = '', filhos = []) {
  return { id: uuidv4(), tipo, numero, conteudo, filhos }
}

function makeCapitulo(numero, titulo) {
  return { ...makeElement('capitulo', numero, '', []), titulo }
}

const CAPITULOS_DEFAULT = [
  'DISPOSIÇÕES PRELIMINARES',
  'DISPOSIÇÕES GERAIS',
  'DISPOSIÇÕES FINAIS',
  'DISPOSIÇÕES TRANSITÓRIAS',
]

// Exportados para reuso no formulário de publicação (HomePage.vue) -- é o
// mesmo envelope JSON (tipo ProseMirror/TipTap) que o WysiwygEditor e o
// backend já esperam em "conteudo", então textos simples digitados ali
// entram no mesmo contrato de dados sem precisar de um editor rico.
export function jText(text, marks = []) {
  return { type: 'text', text, ...(marks.length ? { marks } : {}) }
}
export function jPara(...nodes) {
  return { type: 'paragraph', content: nodes }
}
export function jDoc(...paragraphs) {
  return JSON.stringify({ type: 'doc', content: paragraphs })
}

// A parte preliminar (epígrafe/ementa/preâmbulo/fecho/assinatura) não faz
// mais parte da edição -- só existe de fato a partir da publicação em BCA,
// então passou a ser coletada no próprio formulário de publicação
// (HomePage.vue), não como uma seção editável aqui.
function gerarSecoesTemplate(doc) {
  return [
    {
      id: uuidv4(),
      tipo: 'parte_normativa',
      titulo: 'Parte Normativa',
      ordem: 1,
      elementos: CAPITULOS_DEFAULT.map((titulo, i) => makeCapitulo(i + 1, titulo)),
    },
    {
      id: uuidv4(),
      tipo: 'anexos',
      titulo: 'Anexos',
      ordem: 2,
      elementos: [],
    },
  ]
}

export const useDocumentsStore = defineStore('documents', {
  state: () => ({
    documentos: [],
    loading: false,
    anexosPorDocumento: {},
    historicoPorDocumento: {},
    mapaAlteracaoPorDocumento: {},
    documentosComHistorico: [],
  }),

  getters: {
    getById: (state) => (id) => state.documentos.find(d => String(d.id) === String(id)) ?? null,
    temVersoesComparaveis: (state) => (id) => state.documentosComHistorico.includes(String(id)),
  },

  actions: {
    async fetchAll() {
      if (this.loading) return
      this.loading = true
      try {
        const [docs, comHistorico] = await Promise.all([
          api.listDocumentos(),
          api.listDocumentosComHistoricoEmenda(),
        ])
        this.documentos = docs.map(d => ({ ...d }))
        this.documentosComHistorico = comHistorico
      } finally {
        this.loading = false
      }
    },

    async fetchDocumento(id) {
      const doc = await api.getDocumento(id)
      if (!doc) return null
      if (!doc.secoes) {
        doc.secoes = gerarSecoesTemplate(doc)
        doc._fromTemplate = true
      } else {
        doc._fromTemplate = false
        renumerarSecaoNormativa(doc)
      }
      const idx = this.documentos.findIndex(d => String(d.id) === String(id))
      if (idx !== -1) this.documentos[idx] = doc
      else this.documentos.push(doc)
      return doc
    },

    async createDocumento(payload) {
      const novo = await api.createDocumento(payload)
      novo.secoes = gerarSecoesTemplate(novo)
      novo._fromTemplate = true
      this.documentos.unshift(novo)
      return novo
    },

    async cloneDocumento(id) {
      const clone = await api.cloneDocumento(id)
      if (clone) {
        clone.secoes = gerarSecoesTemplate(clone)
        clone._fromTemplate = true
        this.documentos.unshift(clone)
        const original = this.documentos.find(d => String(d.id) === String(id))
        if (original) original.qtd_replicas = (original.qtd_replicas ?? 0) + 1
      }
      return clone
    },

    // Sequencial de propósito, não Promise.all: saveSecoes checa
    // versaoEsperada contra o banco (DocumentoConcorrenciaService) e bumpa a
    // versão; se updateDocumento rodasse em paralelo, os dois partiriam da
    // mesma versão lida e um dos bumps "desapareceria" da resposta que o
    // frontend vê. Rodando em sequência, updateDocumento sempre lê o estado
    // já pós-saveSecoes, então atualizado.versao reflete a versão real final.
    async saveDocumento(documento) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(documento.id))
      if (idx === -1) return
      if (documento.secoes) {
        await api.saveSecoes(documento.id, documento.secoes, documento.versao)
      }
      const atualizado = await api.updateDocumento(documento.id, documento)
      if (atualizado) {
        this.documentos[idx] = { ...this.documentos[idx], ...atualizado, secoes: documento.secoes }
      }
      return atualizado
    },

    async updateMetadados(id, { titulo, numero_secundario }) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(id))
      if (idx === -1) return
      const atualizado = await api.updateDocumento(id, { titulo, numero_secundario })
      if (atualizado) {
        this.documentos[idx] = { ...this.documentos[idx], ...atualizado, secoes: this.documentos[idx].secoes }
      }
      return atualizado
    },

    async changeStatus(id, novoStatus, refs) {
      const atualizado = await api.changeDocumentoStatus(id, novoStatus, refs)
      if (atualizado) {
        const idx = this.documentos.findIndex(d => String(d.id) === String(id))
        if (idx !== -1) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
      }
    },

    async emendar(docId, secao, elementoId, acao, novoConteudo, novoTitulo, justificativa, versaoEsperada) {
      await api.emendar(docId, secao, elementoId, acao, novoConteudo, novoTitulo, justificativa, versaoEsperada)
      return this.fetchDocumento(docId)
    },

    async incluirElementoEmenda(docId, secao, tipo, titulo, conteudo, parentId, elementOrder, justificativa, versaoEsperada) {
      await api.incluirElementoEmenda(docId, secao, tipo, titulo, conteudo, parentId, elementOrder, justificativa, versaoEsperada)
      return this.fetchDocumento(docId)
    },

    async reordenarElementoEmenda(docId, secao, elementoId, direcao) {
      await api.reordenarElementoEmenda(docId, secao, elementoId, direcao)
      return this.fetchDocumento(docId)
    },

    async deleteDocumento(id) {
      const doc = this.documentos.find(d => String(d.id) === String(id))
      if (doc && !['RASCUNHO', 'MINUTA'].includes(doc.status)) {
        throw new Error(`Não é possível excluir um documento com situação "${doc.status}". Somente documentos em RASCUNHO ou MINUTA podem ser excluídos.`)
      }
      await api.deleteDocumento(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },

    async fetchAnexos(documentoId) {
      const lista = await api.listAnexos(documentoId)
      this.anexosPorDocumento[String(documentoId)] = lista ?? []
      return lista
    },

    async fetchHistorico(documentoId) {
      const lista = await api.listHistorico(documentoId)
      this.historicoPorDocumento[String(documentoId)] = lista ?? []
      return lista
    },

    async fetchMapaAlteracao(documentoId) {
      const lista = await api.listMapaAlteracao(documentoId)
      this.mapaAlteracaoPorDocumento[String(documentoId)] = lista ?? []
      return lista
    },

    async removeAnexo(documentoId, anexoId) {
      await api.deleteAnexo(documentoId, anexoId)
      const key = String(documentoId)
      if (this.anexosPorDocumento[key]) {
        this.anexosPorDocumento[key] = this.anexosPorDocumento[key].filter(a => String(a.id) !== String(anexoId))
      }
    },

    addElemento(documentoId, parentId, tipo) {
      const doc = this.documentos.find(d => String(d.id) === String(documentoId))
      if (!doc) return
      const secaoNormativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
      if (!secaoNormativa) return

      const novoEl = makeElement(tipo, 0, jDoc(jPara()), [])

      if (!parentId) {
        secaoNormativa.elementos.push(novoEl)
      } else {
        const addToParent = (elements) => {
          for (const el of elements) {
            if (el.id === parentId) { el.filhos.push(novoEl); return true }
            if (el.filhos?.length && addToParent(el.filhos)) return true
          }
          return false
        }
        addToParent(secaoNormativa.elementos)
      }

      renumberElements(secaoNormativa.elementos)
    },
  },
})
