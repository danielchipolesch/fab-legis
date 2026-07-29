import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'
import { renumberElements } from '@/utils/numbering.js'
import * as api from '@/api/documents.js'

function renumerarSecaoNormativa(doc) {
  const normativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
  if (normativa?.elementos?.length) renumberElements(normativa.elementos)
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

function jText(text, marks = []) {
  return { type: 'text', text, ...(marks.length ? { marks } : {}) }
}
function jBold(text) {
  return jText(text, [{ type: 'bold' }])
}
function jRed(text) {
  return jText(text, [{ type: 'textStyle', attrs: { color: '#CC0000' } }, { type: 'bold' }])
}
function jPara(...nodes) {
  return { type: 'paragraph', content: nodes }
}
function jDoc(...paragraphs) {
  return JSON.stringify({ type: 'doc', content: paragraphs })
}

const ESPECIE_NOME = {
  ICA:  'Instrução do Comando da Aeronáutica',
  NSCA: 'Norma de Sistema do Comando da Aeronáutica',
  MCA:  'Manual do Comando da Aeronáutica',
  RCA:  'Regulamento do Comando da Aeronáutica',
  DCA:  'Diretriz do Comando da Aeronáutica',
  PCA:  'Portaria do Comando da Aeronáutica',
  OCA:  'Ordem do Comando da Aeronáutica',
  TCA:  'Técnica do Comando da Aeronáutica',
}

function gerarSecoesTemplate(doc) {
  const especie   = doc.especie       ?? ''
  const numBasico = doc.numero_basico ?? ''
  const numSec    = doc.numero_secundario != null ? doc.numero_secundario : '?'
  const sigla     = `${especie} ${numBasico}-${numSec}`.trim()
  const nomeEsp   = ESPECIE_NOME[especie] ?? especie

  return [
    {
      id: uuidv4(),
      tipo: 'parte_preliminar',
      titulo: 'Parte Preliminar',
      ordem: 1,
      elementos: [
        makeElement('epigrafe', null, jDoc(
          jPara(jBold(sigla)),
          jPara(jRed('[DD DE MÊS DE AAAA]'))
        )),
        makeElement('ementa', null, jDoc(
          jPara(
            jText('Dispõe sobre '),
            jRed('[descrição resumida do assunto]'),
            jText(' e dá outras providências.')
          )
        )),
        makeElement('preambulo', null, jDoc(
          jPara(
            jText('O '),
            jBold('COMANDANTE DA AERONÁUTICA'),
            jText(', no uso das atribuições que lhe confere o art. 12 da Lei Complementar nº 97, de 9 de junho de 1999, tendo em vista o que consta do Processo nº '),
            jRed('[NÚMERO DO PROCESSO]'),
            jText(', resolve:')
          )
        )),
        makeElement('fundamentacao', null, jDoc(
          jPara(
            jText('Considerando '),
            jRed('[justificativa ou motivação da norma]'),
            jText(';')
          )
        )),
      ],
    },
    {
      id: uuidv4(),
      tipo: 'parte_normativa',
      titulo: 'Parte Normativa',
      ordem: 2,
      elementos: CAPITULOS_DEFAULT.map((titulo, i) => makeCapitulo(i + 1, titulo)),
    },
    {
      id: uuidv4(),
      tipo: 'parte_final',
      titulo: 'Parte Final',
      ordem: 3,
      elementos: [
        makeElement('clausula_revogatoria', null, jDoc(
          jPara(jText('Ficam revogadas as disposições em contrário.'))
        )),
        makeElement('clausula_vigencia', null, jDoc(
          jPara(jText(`Esta ${nomeEsp} entra em vigor na data de sua publicação no Boletim do Comando da Aeronáutica.`))
        )),
        makeElement('fecho', null, jDoc(
          jPara(jText('Brasília, '), jRed('[DD de mês de AAAA]'), jText('.'))
        )),
        makeElement('assinatura', null, jDoc(
          jPara(jRed('[NOME DO COMANDANTE DA AERONÁUTICA]')),
          jPara(jText('Tenente-Brigadeiro do Ar')),
          jPara(jText('Comandante da Aeronáutica'))
        )),
        makeElement('referenda', null, jDoc(
          jPara(jRed('[NOME DO MINISTRO DE ESTADO DA DEFESA]')),
          jPara(jText('Ministro de Estado da Defesa'))
        )),
      ],
    },
  ]
}

export const useDocumentsStore = defineStore('documents', {
  state: () => ({
    documentos: [],
    loading: false,
  }),

  getters: {
    getById: (state) => (id) => state.documentos.find(d => String(d.id) === String(id)) ?? null,
  },

  actions: {
    async fetchAll() {
      if (this.loading) return
      this.loading = true
      try {
        const docs = await api.listDocumentos()
        this.documentos = docs.map(d => ({ ...d }))
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

    async saveDocumento(documento) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(documento.id))
      if (idx === -1) return
      const [atualizado] = await Promise.all([
        api.updateDocumento(documento.id, documento),
        documento.secoes ? api.saveSecoes(documento.id, documento.secoes) : Promise.resolve(null),
      ])
      if (atualizado) {
        this.documentos[idx] = { ...this.documentos[idx], ...atualizado, secoes: documento.secoes }
      }
    },

    async changeStatus(id, novoStatus) {
      const atualizado = await api.changeDocumentoStatus(id, novoStatus)
      if (atualizado) {
        const idx = this.documentos.findIndex(d => String(d.id) === String(id))
        if (idx !== -1) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
      }
    },

    async deleteDocumento(id) {
      const doc = this.documentos.find(d => String(d.id) === String(id))
      if (doc && !['RASCUNHO', 'MINUTA'].includes(doc.status)) {
        throw new Error(`Não é possível excluir um documento com status "${doc.status}". Somente documentos em RASCUNHO ou MINUTA podem ser excluídos.`)
      }
      await api.deleteDocumento(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
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
