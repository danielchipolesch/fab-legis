import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'
import { renumberElements } from '@/utils/numbering.js'

// ---------- Mock data helpers ----------

function makeElement(tipo, numero, conteudo = '', filhos = []) {
  return { id: uuidv4(), tipo, numero, conteudo, filhos }
}

function makeCapitulo(numero, titulo, filhos = []) {
  return { id: uuidv4(), tipo: 'capitulo', numero, titulo, filhos }
}

function makeSecao(numero, titulo, filhos = []) {
  return { id: uuidv4(), tipo: 'secao_normativa', numero, titulo, filhos }
}

function mockDocumento(overrides = {}) {
  return {
    id: uuidv4(),
    especie: 'ICA',
    numero_basico: '21',
    numero_secundario: '1',
    assunto_basico: 'Organização do Sistema de Ensino da Aeronáutica',
    data_criacao: '2024-01-15',
    data_publicacao: null,
    status: 'RASCUNHO',
    versoes: [],
    secoes: [
      {
        id: uuidv4(),
        tipo: 'parte_preliminar',
        titulo: 'Parte Preliminar',
        ordem: 1,
        elementos: [
          makeElement('epigrafe',      null, '<p><strong>ICA 21-1</strong><br/>15 DE JANEIRO DE 2024</p>'),
          makeElement('ementa',        null, '<p>Dispõe sobre a organização do Sistema de Ensino da Aeronáutica e dá outras providências.</p>'),
          makeElement('preambulo',     null, '<p>O <strong>COMANDANTE DA AERONÁUTICA</strong>, no uso das atribuições que lhe confere o art. 12 da Lei Complementar nº 97, de 9 de junho de 1999, resolve:</p>'),
          makeElement('fundamentacao', null, '<p>Considerando a necessidade de atualizar as normas relativas ao ensino no âmbito do Comando da Aeronáutica;</p>'),
        ],
      },
      {
        id: uuidv4(),
        tipo: 'parte_normativa',
        titulo: 'Parte Normativa',
        ordem: 2,
        elementos: [
          makeCapitulo(1, 'DISPOSIÇÕES PRELIMINARES', [
            makeSecao(1, 'Finalidade e conceituação', [
              {
                ...makeElement('artigo', 1, '<p>Esta Instrução estabelece as normas gerais de organização e funcionamento do Sistema de Ensino da Aeronáutica (SISEA).</p>'),
                filhos: [
                  {
                    ...makeElement('paragrafo_unico', null, '<p>As normas complementares serão estabelecidas em regulamentos específicos.</p>'),
                    filhos: [],
                  },
                ],
              },
            ]),
          ]),
          makeCapitulo(2, 'DISPOSIÇÕES GERAIS', [
            {
              ...makeElement('artigo', 2, '<p>O SISEA é composto pelos seguintes órgãos:</p>'),
              filhos: [
                makeElement('inciso', 1, '<p>Departamento de Ensino da Aeronáutica (DEPENS);</p>', []),
                makeElement('inciso', 2, '<p>Academia da Força Aérea (AFA);</p>', []),
                {
                  ...makeElement('inciso', 3, '<p>Escolas de especialistas, que compreendem:</p>'),
                  filhos: [
                    makeElement('alinea', 1, '<p>Escola de Especialistas de Aeronáutica (EEAR);</p>', []),
                    makeElement('alinea', 2, '<p>Escola de Aperfeiçoamento de Oficiais (EAOAR).</p>', []),
                  ],
                },
              ],
            },
            makeElement('artigo', 3, '<p>Os casos omissos serão decididos pelo Comandante da Aeronáutica.</p>', []),
          ]),
        ],
      },
      {
        id: uuidv4(),
        tipo: 'parte_final',
        titulo: 'Parte Final',
        ordem: 3,
        elementos: [
          makeElement('clausula_revogatoria', null, '<p>Ficam revogadas as disposições em contrário.</p>'),
          makeElement('clausula_vigencia',    null, '<p>Esta Instrução entra em vigor na data de sua publicação.</p>'),
          makeElement('fecho',               null, '<p>Brasília, 15 de janeiro de 2024.</p>'),
          makeElement('assinatura',          null, '<p><strong>MARCELO KANITZ DAMASCENO</strong><br/>Tenente-Brigadeiro do Ar<br/>Comandante da Aeronáutica</p>'),
          makeElement('referenda',           null, '<p><strong>JOSÉ MUCIO MONTEIRO</strong><br/>Ministro de Estado da Defesa</p>'),
        ],
      },
    ],
    ...overrides,
  }
}

function cloneDocumento(doc) {
  return JSON.parse(JSON.stringify(doc))
}

// ---------- Store ----------

export const useDocumentsStore = defineStore('documents', {
  state: () => ({
    documentos: [
      mockDocumento({ status: 'PUBLICADO', especie: 'NSCA', numero_basico: '5', numero_secundario: '3', assunto_basico: 'Elaboração e Gestão de Atos Normativos do COMAER', data_publicacao: '2023-06-01' }),
      mockDocumento({ status: 'APROVADO',  especie: 'ICA',  numero_basico: '21', numero_secundario: '1', assunto_basico: 'Organização do Sistema de Ensino da Aeronáutica' }),
      mockDocumento({ status: 'MINUTA',    especie: 'Portaria', numero_basico: '314', numero_secundario: null, assunto_basico: 'Designação de Oficial para Função de Confiança' }),
      mockDocumento({ status: 'RASCUNHO',  especie: 'ICA',  numero_basico: '55', numero_secundario: '17', assunto_basico: 'Regulamento de Tráfego Aéreo' }),
      mockDocumento({ status: 'ARQUIVADO', especie: 'NSCA', numero_basico: '3', numero_secundario: '6', assunto_basico: 'Normas para Contratação de Serviços' }),
      mockDocumento({ status: 'CANCELADO', especie: 'ICA',  numero_basico: '100', numero_secundario: '2', assunto_basico: 'Procedimentos de Segurança de Voo' }),
      mockDocumento({ status: 'REVOGADO',  especie: 'Portaria', numero_basico: '201', numero_secundario: null, assunto_basico: 'Organização Interna da DIRENS' }),
    ],
    loading: false,
  }),

  getters: {
    getById: (state) => (id) => state.documentos.find(d => d.id === id) ?? null,

    // Retorna o próximo número básico sequencial para uma espécie
    getNextBasicNumber: (state) => (especie) => {
      const numeros = state.documentos
        .filter(d => d.especie === especie)
        .map(d => parseInt(d.numero_basico) || 0)
      return numeros.length ? String(Math.max(...numeros) + 1) : '1'
    },
  },

  actions: {
    createDocumento(payload) {
      const base = mockDocumento({})
      const novo = {
        ...base,
        id: uuidv4(),
        especie:          payload.especie          ?? base.especie,
        organizacao:      payload.organizacao       ?? '',
        assunto_basico:   payload.assunto_basico    ?? base.assunto_basico,
        numero_basico:    payload.numero_basico     ?? base.numero_basico,
        numero_secundario: null,
        data_criacao:     new Date().toISOString().slice(0, 10),
        data_publicacao:  null,
        status:           'RASCUNHO',
        versoes:          [],
      }
      this.documentos.unshift(novo)
      return novo
    },

    cloneDocumento(id) {
      const original = this.documentos.find(d => d.id === id)
      if (!original) return null

      // Maior número secundário entre documentos da mesma espécie + número básico
      const irmaos = this.documentos.filter(
        d => d.especie === original.especie && d.numero_basico === original.numero_basico
      )
      const maxSec = Math.max(0, ...irmaos
        .map(d => parseInt(d.numero_secundario) || 0)
        .filter(n => !isNaN(n))
      )

      const clone = JSON.parse(JSON.stringify(original))
      clone.id              = uuidv4()
      clone.numero_secundario = String(maxSec + 1)
      clone.status          = 'RASCUNHO'
      clone.versoes         = []
      clone.data_criacao    = new Date().toISOString().slice(0, 10)
      clone.data_publicacao = null

      this.documentos.unshift(clone)
      return clone
    },

    saveDocumento(documento) {
      const idx = this.documentos.findIndex(d => d.id === documento.id)
      if (idx !== -1) {
        this.documentos[idx] = cloneDocumento(documento)
      }
    },

    changeStatus(id, novoStatus) {
      const doc = this.documentos.find(d => d.id === id)
      if (!doc) return
      const old = cloneDocumento(doc)
      old.status = doc.status
      doc.versoes.push({ ...old, versao_id: uuidv4(), data_snapshot: new Date().toISOString() })
      doc.status = novoStatus
      if (novoStatus === 'PUBLICADO') doc.data_publicacao = new Date().toISOString().slice(0, 10)
    },

    deleteDocumento(id) {
      this.documentos = this.documentos.filter(d => d.id !== id)
    },

    // Adds an element to the normative section
    addElemento(documentoId, parentId, tipo) {
      const doc = this.documentos.find(d => d.id === documentoId)
      if (!doc) return

      const secaoNormativa = doc.secoes.find(s => s.tipo === 'parte_normativa')
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
  },
})
