import { defineStore } from 'pinia'
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ffd8177 (Uso do HATEOAS)
import { v4 as uuidv4 } from 'uuid'
import { renumberElements } from '@/utils/numbering.js'
import * as api from '@/api/documents.js'

<<<<<<< HEAD
=======
// ---------- Helpers ----------

>>>>>>> ffd8177 (Uso do HATEOAS)
function makeElement(tipo, numero, conteudo = '', filhos = []) {
  return { id: uuidv4(), tipo, numero, conteudo, filhos }
}

function red(texto) {
  return `<span style="color: red; font-weight: bold">${texto}</span>`
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
<<<<<<< HEAD
  const especie   = doc.especie       ?? ''
  const numBasico = doc.numero_basico ?? ''
  const numSec    = doc.numero_secundario != null ? doc.numero_secundario : '?'
  const sigla     = `${especie} ${numBasico}-${numSec}`.trim()
  const nomeEsp   = ESPECIE_NOME[especie] ?? especie
=======
  const especie    = doc.especie       ?? ''
  const numBasico  = doc.numero_basico ?? ''
  const numSec     = doc.numero_secundario != null ? doc.numero_secundario : '?'
  const sigla      = `${especie} ${numBasico}-${numSec}`.trim()
  const nomeEsp    = ESPECIE_NOME[especie] ?? especie
>>>>>>> ffd8177 (Uso do HATEOAS)

  return [
    {
      id: uuidv4(),
      tipo: 'parte_preliminar',
      titulo: 'Parte Preliminar',
      ordem: 1,
      elementos: [
        makeElement('epigrafe', null,
          `<p><strong>${sigla}</strong></p><p>${red('[DD DE MÊS DE AAAA]')}</p>`
        ),
        makeElement('ementa', null,
          `<p>Dispõe sobre ${red('[descrição resumida do assunto]')} e dá outras providências.</p>`
        ),
        makeElement('preambulo', null,
          `<p>O <strong>COMANDANTE DA AERONÁUTICA</strong>, no uso das atribuições que lhe confere o art. 12 da Lei Complementar nº 97, de 9 de junho de 1999, tendo em vista o que consta do Processo nº ${red('[NÚMERO DO PROCESSO]')}, resolve:</p>`
        ),
        makeElement('fundamentacao', null,
          `<p>Considerando ${red('[justificativa ou motivação da norma]')};</p>`
        ),
      ],
    },
    {
      id: uuidv4(),
      tipo: 'parte_normativa',
      titulo: 'Parte Normativa',
      ordem: 2,
      elementos: [],
    },
    {
      id: uuidv4(),
      tipo: 'parte_final',
      titulo: 'Parte Final',
      ordem: 3,
      elementos: [
        makeElement('clausula_revogatoria', null,
          `<p>Ficam revogadas as disposições em contrário.</p>`
        ),
        makeElement('clausula_vigencia', null,
          `<p>Esta ${nomeEsp} entra em vigor na data de sua publicação no Boletim do Comando da Aeronáutica.</p>`
        ),
        makeElement('fecho', null,
          `<p>Brasília, ${red('[DD de mês de AAAA]')}.</p>`
        ),
        makeElement('assinatura', null,
          `<p>${red('[NOME DO COMANDANTE DA AERONÁUTICA]')}<br/>Tenente-Brigadeiro do Ar<br/>Comandante da Aeronáutica</p>`
        ),
        makeElement('referenda', null,
          `<p>${red('[NOME DO MINISTRO DE ESTADO DA DEFESA]')}<br/>Ministro de Estado da Defesa</p>`
        ),
      ],
    },
  ]
}
<<<<<<< HEAD
=======
import * as api from '@/api/documentos.js'
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======

function mockDoc(overrides = {}) {
  const base = {
    id: uuidv4(),
    especie: 'ICA',
    numero_basico: '21',
    numero_secundario: '1',
    assunto_basico: 'Organização do Sistema de Ensino da Aeronáutica',
    titulo: 'Regulamento do Sistema de Ensino da Aeronáutica',
    data_criacao: '2024-01-15',
    data_publicacao: null,
    status: 'RASCUNHO',
    versoes: [],
    itens: [],
    secoes: [],
    ...overrides,
  }
  base.secoes = gerarSecoesTemplate(base)
  return base
}

const MOCK_DOCUMENTOS = [
  mockDoc({ status: 'PUBLICADO', especie: 'NSCA', numero_basico: '5',   numero_secundario: '3',  assunto_basico: 'Elaboração e Gestão de Atos Normativos do COMAER', data_publicacao: '2023-06-01' }),
  mockDoc({ status: 'APROVADO',  especie: 'ICA',  numero_basico: '21',  numero_secundario: '1',  assunto_basico: 'Organização do Sistema de Ensino da Aeronáutica' }),
  mockDoc({ status: 'MINUTA',    especie: 'ICA',  numero_basico: '55',  numero_secundario: '3',  assunto_basico: 'Regulamento de Tráfego Aéreo' }),
  mockDoc({ status: 'RASCUNHO',  especie: 'ICA',  numero_basico: '55',  numero_secundario: '17', assunto_basico: 'Regulamento de Tráfego Aéreo' }),
  mockDoc({ status: 'ARQUIVADO', especie: 'NSCA', numero_basico: '3',   numero_secundario: '6',  assunto_basico: 'Normas para Contratação de Serviços' }),
  mockDoc({ status: 'CANCELADO', especie: 'ICA',  numero_basico: '100', numero_secundario: '2',  assunto_basico: 'Procedimentos de Segurança de Voo' }),
  mockDoc({ status: 'REVOGADO',  especie: 'ICA',  numero_basico: '21',  numero_secundario: '2',  assunto_basico: 'Organização Interna da DIRENS' }),
]

function cloneDoc(doc) {
  return JSON.parse(JSON.stringify(doc))
}

// ---------- Store ----------
>>>>>>> ffd8177 (Uso do HATEOAS)

export const useDocumentsStore = defineStore('documents', {
  state: () => {
    const stored = api.loadInitial()
    if (stored === null) api.persist(MOCK_DOCUMENTOS)
    return {
      documentos: stored ?? MOCK_DOCUMENTOS,
      loading: false,
    }
  },

  getters: {
    getById: (state) => (id) => state.documentos.find(d => String(d.id) === String(id)) ?? null,

    getNextBasicNumber: (state) => (especie) => {
      const numeros = state.documentos
        .filter(d => d.especie === especie)
        .map(d => parseInt(d.numero_basico) || 0)
      return numeros.length ? String(Math.max(...numeros) + 1) : '1'
    },
  },

  actions: {
    async fetchAll() {
      if (api.USE_MOCK) return
      if (this.loading) return
      this.loading = true
      try {
<<<<<<< HEAD
<<<<<<< HEAD
        const docs = await api.listDocumentos()
        this.documentos = docs.map(d => ({ ...d, secoes: gerarSecoesTemplate(d) }))
=======
        this.documentos = await api.listar()
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
        const docs = await api.listDocumentos()
        this.documentos = docs.map(d => ({ ...d, secoes: gerarSecoesTemplate(d) }))
>>>>>>> ffd8177 (Uso do HATEOAS)
      } finally {
        this.loading = false
      }
    },

    async fetchDocumento(id) {
      const doc = await api.getDocumento(id)
      if (!doc) return null
<<<<<<< HEAD
      if (!doc.secoes) doc.secoes = gerarSecoesTemplate(doc)
=======
      doc.secoes = gerarSecoesTemplate(doc)
>>>>>>> ffd8177 (Uso do HATEOAS)
      const idx = this.documentos.findIndex(d => String(d.id) === String(id))
      if (idx !== -1) this.documentos[idx] = doc
      else this.documentos.push(doc)
      return doc
    },

    async createDocumento(payload) {
<<<<<<< HEAD
<<<<<<< HEAD
      const novo = await api.createDocumento(payload)
      novo.secoes = gerarSecoesTemplate(novo)
=======
      const novo = await api.criar(payload)
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
      if (api.USE_MOCK) {
        const especie     = payload.especie       ?? ''
        const numBasico   = payload.numero_basico  ?? ''
        const irmaos      = this.documentos.filter(
          d => d.especie === especie && d.numero_basico === numBasico
        )
        const maxSec      = Math.max(0, ...irmaos.map(d => parseInt(d.numero_secundario) || 0))
        const novo = {
          id:                uuidv4(),
          especie,
          assunto_basico:    payload.assunto_basico ?? '',
          numero_basico:     numBasico,
          numero_secundario: String(maxSec + 1),
          titulo:            payload.titulo         ?? '',
          data_criacao:      new Date().toISOString().slice(0, 10),
          data_publicacao:   null,
          status:            'RASCUNHO',
          versoes:           [],
          itens:             [],
          secoes:            [],
        }
        novo.secoes = gerarSecoesTemplate(novo)
        this.documentos.unshift(novo)
        api.persist(this.documentos)
        return novo
      }
      const novo = await api.createDocumento(payload)
      novo.secoes = gerarSecoesTemplate(novo)
>>>>>>> ffd8177 (Uso do HATEOAS)
      this.documentos.unshift(novo)
      return novo
    },

    async cloneDocumento(id) {
<<<<<<< HEAD
<<<<<<< HEAD
      const clone = await api.cloneDocumento(id)
      if (clone) {
        clone.secoes = gerarSecoesTemplate(clone)
        this.documentos.unshift(clone)
      }
=======
      const clone = await api.clonar(id)
=======
      if (api.USE_MOCK) {
        const original = this.documentos.find(d => String(d.id) === String(id))
        if (!original) return null

        const irmaos = this.documentos.filter(
          d => d.especie === original.especie && d.numero_basico === original.numero_basico
        )
        const maxSec = Math.max(0, ...irmaos
          .map(d => parseInt(d.numero_secundario) || 0)
          .filter(n => !isNaN(n))
        )

        const clone = cloneDoc(original)
        clone.id = uuidv4()
        clone.numero_secundario = String(maxSec + 1)
        clone.status = 'RASCUNHO'
        clone.versoes = []
        clone.data_criacao    = new Date().toISOString().slice(0, 10)
        clone.data_publicacao = null

        this.documentos.unshift(clone)
        api.persist(this.documentos)
        return clone
      }
      const clone = await api.cloneDocumento(id)
>>>>>>> ffd8177 (Uso do HATEOAS)
      if (clone) this.documentos.unshift(clone)
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
      return clone
    },

    async saveDocumento(documento) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(documento.id))
      if (idx === -1) return
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
      const atualizado = await api.atualizar(documento.id, documento.titulo ?? documento.tituloDocumento)
=======

      if (api.USE_MOCK) {
        this.documentos[idx] = cloneDoc(documento)
        api.persist(this.documentos)
        return
      }

      const atualizado = await api.updateDocumento(documento.id, documento)
>>>>>>> ffd8177 (Uso do HATEOAS)
      if (atualizado) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
    },

    async changeStatus(id, novoStatus) {
<<<<<<< HEAD
      const atualizado = await api.mudarStatus(id, novoStatus)
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
      if (api.USE_MOCK) {
        const doc = this.documentos.find(d => String(d.id) === String(id))
        if (!doc) return
        const snapshot = cloneDoc(doc)
        snapshot.status = doc.status
        doc.versoes.push({ ...snapshot, versao_id: uuidv4(), data_snapshot: new Date().toISOString() })
        doc.status = novoStatus
        if (novoStatus === 'PUBLICADO') doc.data_publicacao = new Date().toISOString().slice(0, 10)
        api.persist(this.documentos)
        return
      }

      const atualizado = await api.changeDocumentoStatus(id, novoStatus)
>>>>>>> ffd8177 (Uso do HATEOAS)
      if (atualizado) {
        const idx = this.documentos.findIndex(d => String(d.id) === String(id))
        if (idx !== -1) this.documentos[idx] = { ...this.documentos[idx], ...atualizado }
      }
    },

    async deleteDocumento(id) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ffd8177 (Uso do HATEOAS)
      const doc = this.documentos.find(d => String(d.id) === String(id))
      if (doc && !['RASCUNHO', 'MINUTA'].includes(doc.status)) {
        throw new Error(`Não é possível excluir um documento com status "${doc.status}". Somente documentos em RASCUNHO ou MINUTA podem ser excluídos.`)
      }
<<<<<<< HEAD
      await api.deleteDocumento(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },

    addElemento(documentoId, parentId, tipo) {
      const doc = this.documentos.find(d => String(d.id) === String(documentoId))
      if (!doc) return
      const secaoNormativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
      if (!secaoNormativa) return

      const novoEl = makeElement(tipo, 0, '<p></p>', [])

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
=======
      await api.excluir(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
      if (api.USE_MOCK) {
        this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
        api.persist(this.documentos)
        return
      }
      await api.deleteDocumento(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },

    addElemento(documentoId, parentId, tipo) {
      const doc = this.documentos.find(d => String(d.id) === String(documentoId))
      if (!doc) return
      const secaoNormativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
      if (!secaoNormativa) return

      const novoEl = makeElement(tipo, 0, '<p></p>', [])

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
      api.persist(this.documentos)
    },
>>>>>>> ffd8177 (Uso do HATEOAS)
  },
})
