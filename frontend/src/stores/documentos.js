import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'
import { renumberElements, renumberElementsEmAlteracao } from '@/utils/numbering.js'
import * as api from '@/api/documentos.js'

function renumerarSecaoNormativa(doc) {
  const normativa = doc.secoes?.find(s => s.tipo === 'parte_normativa')
  if (!normativa?.elementos?.length) return
  // Documento já publicado (PUBLICADO ou REVOGADO): numeração por emenda (elemento em vigor nunca
  // é renumerado), em qualquer etapa local.
  if (doc.situacao_bca && doc.situacao_bca !== 'NAO_PUBLICADO') {
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

export const useDocumentosStore = defineStore('documents', {
  state: () => ({
    // Antes, "o acervo inteiro visível" (até 200 documentos, carregado uma vez). Agora
    // é só a página atual da HomePage -- getById continua funcionando pras outras telas
    // porque elas sempre chamam fetchDocumento(id) antes de ler por ali (ver
    // DocumentoViewerPage.vue/DocumentoEditorPage.vue/ComparisonPage.vue), nunca dependem
    // do array já estar populado por uma listagem anterior.
    documentos: [],
    totalElements: 0,
    resumoAbas: { meus: 0, minha_om: 0, outras_oms: 0, revogados: 0 },
    resumoSituacaoBca: {},
    resumoSituacaoLocal: {},
    loading: false,
    anexosPorDocumento: {},
    portariasPorDocumento: {},
    historicoPorDocumento: {},
    mapaAlteracaoPorDocumento: {},
    documentosComHistorico: [],
    // Persistido aqui (não um ref/reactive local em HomePage.vue) pra
    // sobreviver a sair e voltar pra Home dentro da mesma sessão (ex.: abrir
    // um documento e apertar "voltar") -- mesmo raciocínio de
    // stores/busca.js. Sem custo de rede extra: o onMounted da HomePage já
    // dispara uma busca de qualquer forma a cada montagem do componente
    // (não tem keep-alive); persistir só troca OS PARÂMETROS dessa mesma
    // busca (aba/filtro/ordenação de antes, em vez dos padrões), não
    // adiciona uma segunda chamada.
    viewMode: 'tabela',
    abaAtiva: 'meus',
    filtros: { busca: '', especie: null, situacaoBca: null, situacaoLocal: null },
    tablePagination: { page: 1, rowsPerPage: 15, sortBy: 'data_criacao', descending: true, rowsNumber: 0 },
    // Incrementado quando algo fora da própria tela (ex.: alguém te adicionou
    // como coautor -- ver notificação DOCUMENTO_COMPARTILHADO em
    // AppTopBar.vue) deveria mudar a listagem/contagem da HomePage sem
    // esperar o usuário trocar de aba ou recarregar a página. HomePage.vue
    // observa esse contador (watch) e refaz carregar() quando ele muda; um
    // número simples em vez de um evento porque Pinia não tem barramento de
    // eventos embutido, e o valor em si não importa, só a mudança.
    refreshSignal: 0,
  }),

  getters: {
    getById: (state) => (id) => state.documentos.find(d => String(d.id) === String(id)) ?? null,
    temVersoesComparaveis: (state) => (id) => state.documentosComHistorico.includes(String(id)),
  },

  actions: {
    sinalizarRefresh() {
      this.refreshSignal++
    },

    // Busca a página atual do acervo (filtrada por aba/busca/espécie/situação) --
    // substitui o antigo fetchAll(), que carregava tudo de uma vez e filtrava no
    // navegador. Chamada pela HomePage a cada troca de aba/filtro/página (ver
    // HomePage.vue).
    async fetchPagina(params) {
      this.loading = true
      try {
        const { items, totalElements } = await api.listDocumentosPaginado(params)
        this.documentos = items
        this.totalElements = totalElements
      } finally {
        this.loading = false
      }
    },

    async fetchResumo(params) {
      const resp = await api.getResumoDocumentos(params)
      this.resumoAbas = resp?.porAba ?? { meus: 0, minha_om: 0, outras_oms: 0, revogados: 0 }
      this.resumoSituacaoBca = resp?.porSituacaoBca ?? {}
      this.resumoSituacaoLocal = resp?.porSituacaoLocal ?? {}
    },

    async fetchComHistoricoEmenda() {
      this.documentosComHistorico = await api.listDocumentosComHistoricoEmenda()
    },

    async fetchDocumento(id) {
      const doc = await api.getDocumento(id)
      if (!doc) return null
      if (!doc.secoes) {
        doc.secoes = gerarSecoesTemplate(doc)
        doc._fromTemplate = true
      } else {
        doc._fromTemplate = false
        // Local primeiro (numbering.js): cobre TODOS os tipos, inclusive
        // parágrafo/inciso/alínea/subalínea, que NumeracaoService não
        // calcula (numerados localmente ao pai, fora do escopo dela -- ver
        // NumeracaoService). Servidor depois, por cima: reconcilia só o que
        // ele de fato calcula (capítulo/seção/subseção/artigo) com a fonte
        // de verdade -- cobre tanto a carga inicial quanto o retorno do
        // diálogo de emenda (emendar/incluirElementoEmenda/
        // reordenarElementoEmenda sempre recarregam por aqui).
        renumerarSecaoNormativa(doc)
        const secaoNormativa = doc.secoes.find(s => s.tipo === 'parte_normativa')
        if (secaoNormativa) {
          const numeracaoPorId = new Map((doc._numeracaoServidor ?? []).map(n => [n.elementoId, n]))
          api.aplicarNumeracaoPorId(secaoNormativa.elementos, numeracaoPorId)
        }
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
        const resposta = await api.saveSecoes(documento.id, documento.secoes, documento.versao)
        const secaoNormativa = documento.secoes.find(s => s.tipo === 'parte_normativa')
        if (secaoNormativa && resposta) {
          api.aplicarIdsPersistidos(secaoNormativa.elementos, resposta.itens ?? [])
          const numeracaoPorId = new Map((resposta.numeracao ?? []).map(n => [n.elementoId, n]))
          api.aplicarNumeracao(secaoNormativa.elementos, resposta.itens ?? [], numeracaoPorId)
        }
      }
      const atualizado = await api.updateDocumento(documento.id, documento)
      if (atualizado) {
        this.documentos[idx] = { ...this.documentos[idx], ...atualizado, secoes: documento.secoes }
      }
      return atualizado
    },

    async updateMetadados(id, { titulo, numero_secundario, om_id }) {
      const idx = this.documentos.findIndex(d => String(d.id) === String(id))
      if (idx === -1) return
      const atualizado = await api.updateDocumento(id, { titulo, numero_secundario, om_id })
      if (atualizado) {
        this.documentos[idx] = { ...this.documentos[idx], ...atualizado, secoes: this.documentos[idx].secoes }
      }
      return atualizado
    },

    async changeStatus(id, situacaoLocal, refs) {
      const atualizado = await api.changeDocumentoStatus(id, situacaoLocal, refs)
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
      if (doc && !['RASCUNHO', 'MINUTA'].includes(doc.situacao_local)) {
        throw new Error(`Não é possível excluir um documento com situação local "${doc.situacao_local}". Somente documentos em RASCUNHO ou MINUTA podem ser excluídos.`)
      }
      await api.deleteDocumento(id)
      this.documentos = this.documentos.filter(d => String(d.id) !== String(id))
    },

    async fetchAnexos(documentoId) {
      const lista = await api.listAnexos(documentoId)
      this.anexosPorDocumento[String(documentoId)] = lista ?? []
      return lista
    },

    async fetchPortarias(documentoId) {
      const lista = await api.listPortarias(documentoId)
      this.portariasPorDocumento[String(documentoId)] = lista ?? []
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
