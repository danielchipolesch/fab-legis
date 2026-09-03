import * as http from './client.js'
import { clientId } from '@/utils/clientId.js'

const SECAO_CONFIG = {
  PARTE_NORMATIVA:  { tipo: 'parte_normativa',  titulo: 'Parte Normativa',  ordem: 1 },
}

const SECAO_ENUM_MAP = {
  parte_normativa:  'PARTE_NORMATIVA',
}

function parseDtCriacao(dt) {
  if (!dt) return null
  if (typeof dt === 'number') return new Date(dt).toISOString()
  return String(dt)
}

// Ordena elementos por elementOrder; empates resolvidos por tipo + status:
// - REVOGADO (order=null → Infinity) fica sempre por último.
// - Mesmo tipo: INCLUIDO ANTES do INALTERADO — o elementOrder do INCLUIDO é calculado
//   como afterEl.order+1, que coincide com o order do próximo elemento existente; logo,
//   o INCLUIDO deve aparecer imediatamente ANTES desse próximo elemento.
// - Tipos diferentes: INALTERADO ANTES do INCLUIDO — estrutura existente tem prioridade
//   (ex.: CAPITULO INALTERADO antes de ARTIGO INCLUIDO com mesmo order).
function sortEmendaItens(itens) {
  if (!itens?.length) return itens ?? []
  return [...itens].sort((a, b) => {
    const oa = a.elementOrder ?? Infinity
    const ob = b.elementOrder ?? Infinity
    if (oa !== ob) return oa - ob
    // REVOGADO por último
    if (a.emendaStatus === 'REVOGADO' && b.emendaStatus !== 'REVOGADO') return 1
    if (b.emendaStatus === 'REVOGADO' && a.emendaStatus !== 'REVOGADO') return -1
    if (a.tipo === b.tipo) {
      // Mesmo tipo: INCLUIDO antes de INALTERADO/ALTERADO
      return (a.emendaStatus === 'INCLUIDO' ? 0 : 1) - (b.emendaStatus === 'INCLUIDO' ? 0 : 1)
    }
    // Tipos diferentes: INALTERADO/ALTERADO antes de INCLUIDO
    return (a.emendaStatus === 'INCLUIDO' ? 1 : 0) - (b.emendaStatus === 'INCLUIDO' ? 1 : 0)
  })
}

function apiItemParaFrontend(item) {
  return {
    id: String(item.id),
    tipo: (item.elementType ?? '').toLowerCase(),
    elementOrder: item.elementOrder ?? null,
    numero: null,
    titulo: item.elementTitle ?? null,
    conteudo: item.elementContent ?? null,
    fullTextContent: item.fullTextContent ?? null,
    emendaStatus: item.emendaStatus ?? 'INALTERADO',
    conteudoEmenda: item.conteudoEmenda ?? null,
    tituloEmenda: item.tituloEmenda ?? null,
    justificativaEmenda: item.justificativaEmenda ?? null,
    clausulaEmenda: item.clausulaEmenda ?? null,
    clausulaEmendaAnterior: item.clausulaEmendaAnterior ?? null,
    incluidoPorEmenda: item.incluidoPorEmenda ?? false,
    filhos: sortEmendaItens(item.children ?? []).map(apiItemParaFrontend),
  }
}

function buildSecao(secaoKey, itensApi) {
  return {
    ...SECAO_CONFIG[secaoKey],
    id: crypto.randomUUID(),
    elementos: sortEmendaItens(itensApi ?? []).map(apiItemParaFrontend),
  }
}

// Elemento novo (ainda não salvo) tem id gerado no cliente (crypto.randomUUID(), ver
// makeNormEl em stores/editor.js); elemento já persistido tem o id numérico vindo do
// backend, guardado como string (ver apiItemParaFrontend). O backend usa esse id pra
// aplicar um diff em vez de apagar/reinserir a árvore inteira -- ver
// DocumentoParteNormativaService.salvarItensNormativos.
export function idPersistido(id) {
  return /^\d+$/.test(id ?? '') ? Number(id) : null
}

function converterElemento(el, secaoEnum, posicao) {
  // backendId cobre o elemento criado nesta sessão e já salvo uma vez (ver
  // aplicarIdsPersistidos) -- sem isso, cada autosave subsequente o trataria como
  // novo de novo (id ainda é o UUID local) e duplicaria a linha no banco.
  const idBackend = el.backendId ?? idPersistido(el.id)
  // Elemento já persistido tem sala Yjs própria (ver WysiwygEditor.vue) -- o
  // `conteudo`/`fullTextContent` locais não são mais atualizados por ela (o Yjs
  // nunca escreve de volta no Pinia, só no Y.Doc e no Postgres via Hocuspocus), e o
  // backend já ignora esses campos pra um item existente de qualquer forma (ver
  // DocumentoParteNormativaService.aplicarItemNormativoRecursivo). Mandar aqui uma
  // cópia local potencialmente desatualizada seria, na melhor das hipóteses, bytes
  // ignorados a cada autosave estrutural -- então nem manda.
  const ehNovo = idBackend == null
  return {
    id: idBackend,
    secao: secaoEnum,
    tipo: (el.tipo ?? '').toUpperCase(),
    elementOrder: posicao,
    titulo: el.titulo ?? null,
    conteudo: ehNovo ? (el.conteudo ?? null) : null,
    fullTextContent: ehNovo ? (el.fullTextContent ?? null) : null,
    filhos: (el.filhos ?? []).map((f, i) => converterElemento(f, secaoEnum, i + 1)),
  }
}

// Depois de salvar, anota em cada elemento recém-criado (id local ainda é o UUID do
// cliente) o id real atribuído pelo backend, SEM substituir `el.id` -- trocar `el.id`
// remontaria o WysiwygEditor no meio de uma digitação, porque a seleção do elemento
// ativo usa `el.id` como :key (ver DocumentoEditorPage.vue). A ordem dos dois lados
// coincide porque elementOrder enviado é sequencial (i+1) e a resposta vem ordenada
// por elementOrder ASC.
export function aplicarIdsPersistidos(locais, resposta) {
  if (!locais || !resposta) return
  const n = Math.min(locais.length, resposta.length)
  for (let i = 0; i < n; i++) {
    if (idPersistido(locais[i].id) == null && locais[i].backendId == null) {
      locais[i].backendId = resposta[i].id
    }
    aplicarIdsPersistidos(locais[i].filhos, resposta[i].children)
  }
}

export function backendParaFrontend(doc) {
  if (!doc) return null

  const preliminarItens = doc.itensPreliminares ?? []
  const normativaItens  = doc.itensNormativos   ?? []

  // A parte preliminar (epígrafe/ementa/preâmbulo/fecho/assinatura) não é
  // mais mostrada na edição -- só existe de fato a partir da publicação (ver
  // formulário de publicação em HomePage.vue), então itensPreliminares nunca
  // vira uma seção aqui, mesmo quando presente (documento já publicado).
  // Ainda conta para "hasAnyData" para não re-templatizar um documento já
  // publicado que, por algum motivo, não tenha itens de parte normativa.
  const hasAnyData = preliminarItens.length > 0 || normativaItens.length > 0

  const secoes = hasAnyData ? [
    buildSecao('PARTE_NORMATIVA',  normativaItens),
    { tipo: 'anexos', titulo: 'Anexos', ordem: 2, id: crypto.randomUUID(), elementos: [] },
  ] : null

  return {
    id: doc.idDocumento,
    especie: doc.siglaEspecieNormativa,
    numero_basico: doc.codigoAssuntoBasico,
    numero_secundario: doc.numeroSecundario != null ? String(doc.numeroSecundario) : null,
    assunto_basico: doc.nomeAssuntoBasico ?? doc.codigoAssuntoBasico,
    titulo: doc.tituloDocumento,
    codigo_documento: doc.codigoDocumento,
    data_criacao:      parseDtCriacao(doc.dtCriacao),
    data_alteracao:    parseDtCriacao(doc.dtAlteracao),
    data_minuta:       parseDtCriacao(doc.dtMinuta),
    data_aprovacao:    parseDtCriacao(doc.dtAprovacao),
    data_publicacao:   parseDtCriacao(doc.dtPublicacao),
    data_revogacao:    parseDtCriacao(doc.dtRevogacao),
    data_cancelamento: parseDtCriacao(doc.dtCancelamento),
    data_em_alteracao: parseDtCriacao(doc.dtEmAlteracao),
    data_alterado:     parseDtCriacao(doc.dtAlterado),
    status: doc.statusDocumento,
    // Ver Documento.revisorAtribuido/publicadorAtribuido no backend -- quem pode
    // agir/editar o documento agora, enquanto ele estiver em EM_REVISAO/
    // EM_PUBLICACAO/ANALISE_REVOGACAO/EM_REVOGACAO.
    revisor_atribuido_id: doc.revisorAtribuidoId != null ? String(doc.revisorAtribuidoId) : null,
    revisor_atribuido_nome: doc.revisorAtribuidoNome ?? null,
    publicador_atribuido_id: doc.publicadorAtribuidoId != null ? String(doc.publicadorAtribuidoId) : null,
    publicador_atribuido_nome: doc.publicadorAtribuidoNome ?? null,
    url_pdf: doc.urlPdf ?? null,
    qtd_replicas: doc.qtdReplicas ?? 0,
    portaria_referencia: doc.portariaReferencia ?? null,
    bca_referencia: doc.bcaReferencia ?? null,
    data_portaria_referencia: parseDtCriacao(doc.dtPortariaReferencia),
    data_bca_referencia:      parseDtCriacao(doc.dtBcaReferencia),
    versao: doc.versao ?? null,
    autor_id: doc.autorId != null ? String(doc.autorId) : null,
    autor_nome: doc.autorNome ?? null,
    om_id: doc.omId != null ? String(doc.omId) : null,
    om_nome: doc.omNome ?? null,
    versoes: [],
    secoes,
  }
}

export function frontendParaBackendCreate(payload) {
  return {
    idEspecieNormativa: payload.idEspecieNormativa,
    idAssuntoBasico:    payload.idAssuntoBasico,
    tituloDocumento:    payload.tituloDocumento,
  }
}

// Paginação de verdade (ver DocumentoController.getAll): antes disso, listDocumentos()
// chamava isso uma vez com size=200 e a HomePage filtrava/paginava tudo no navegador --
// acima de 200 documentos no acervo, o resto nunca aparecia. Mesmo padrão de
// listAuditoria em api/auditoria.js: devolve o Page cru ({content, totalElements, ...}),
// só mapeando os itens de content pro formato do frontend.
export async function listDocumentosPaginado({
  aba, busca, especieSigla, status, page = 0, size = 15, sortBy = 'dtCriacao', descending = true,
} = {}) {
  const params = new URLSearchParams()
  if (aba) params.set('aba', aba)
  if (busca) params.set('busca', busca)
  if (especieSigla) params.set('especieSigla', especieSigla)
  if (status) params.set('status', status)
  params.set('page', page)
  params.set('size', size)
  params.set('sortBy', sortBy)
  params.set('descending', descending)
  const resp = await http.get(`/documentos/obter-todos?${params.toString()}`)
  return {
    items: (resp?.content ?? []).map(backendParaFrontend),
    totalElements: resp?.totalElements ?? 0,
  }
}

// Contagens pros badges das 4 abas e chips de situação da HomePage -- mesmos filtros de
// busca/espécie/aba da listagem acima, pra ficar em sincronia com o que ela está
// mostrando no momento.
export async function getResumoDocumentos({ aba, busca, especieSigla } = {}) {
  const params = new URLSearchParams()
  if (aba) params.set('aba', aba)
  if (busca) params.set('busca', busca)
  if (especieSigla) params.set('especieSigla', especieSigla)
  return http.get(`/documentos/resumo?${params.toString()}`)
}

// Fila pessoal das telas de Revisão/Publicação -- documentos atribuídos a QUEM
// está chamando (ver Documento.revisorAtribuido/publicadorAtribuido no backend).
export async function listMinhaRevisao() {
  const resp = await http.get('/documentos/minha-revisao')
  return (resp ?? []).map(backendParaFrontend)
}

export async function listMinhaPublicacao() {
  const resp = await http.get('/documentos/minha-publicacao')
  return (resp ?? []).map(backendParaFrontend)
}

export async function listDocumentosComHistoricoEmenda() {
  const data = await http.get('/documentos/com-historico-emenda')
  return Array.isArray(data) ? data.map(String) : []
}

export async function getDocumento(id) {
  const data = await http.get(`/documentos/${id}`)
  return backendParaFrontend(data)
}

export async function createDocumento(payload) {
  const body = frontendParaBackendCreate(payload)
  const data = await http.post('/documentos', body)
  return backendParaFrontend(data)
}

export async function cloneDocumento(id) {
  const data = await http.post(`/documentos/${id}/clonar`)
  return backendParaFrontend(data)
}

export async function updateDocumento(id, data) {
  const body = {
    tituloDocumento: data.titulo ?? data.tituloDocumento,
    ...(data.numero_secundario != null && { numeroSecundario: parseInt(data.numero_secundario, 10) || undefined }),
  }
  const result = await http.put(`/documentos/${id}`, body)
  return backendParaFrontend(result)
}

export async function changeDocumentoStatus(id, novoStatus, refs) {
  const body = { status: novoStatus }
  if (refs) {
    // revisorId: quem vai revisar (destino EM_REVISAO/ANALISE_REVOGACAO), escolhido
    // pelo Editor. publicadorId: quem vai publicar (destino APROVADO/ALTERADO, que já
    // cascateia pra EM_PUBLICACAO no backend, ou EM_REVOGACAO), escolhido pelo
    // Aprovador -- ver SelecionarPessoaDialog.vue/DocumentoStatusRequestDto.
    body.revisorId       = refs.revisorId ?? null
    body.publicadorId    = refs.publicadorId ?? null
    body.orgaoPortaria   = refs.orgaoPortaria ?? null
    body.setorPortaria   = refs.setorPortaria ?? null
    body.numeroPortaria  = refs.numeroPortaria ?? null
    body.dataPortaria    = refs.dataPortaria ?? null
    body.numeroBca       = refs.numeroBca ?? null
    body.dataBca         = refs.dataBca ?? null
    body.epigrafe        = refs.epigrafe ?? null
    body.ementa          = refs.ementa ?? null
    body.preambulo       = refs.preambulo ?? null
    body.fecho           = refs.fecho ?? null
    body.assinatura      = refs.assinatura ?? null
    body.portariaPdfUrl  = refs.portariaPdfUrl ?? null
  }
  const result = await http.patch(`/documentos/${id}/status`, body)
  return backendParaFrontend(result)
}

// ── Emenda de elementos ───────────────────────────────────────────────────────

export async function emendar(docId, secao, elementoId, acao, novoConteudo, novoTitulo, justificativa, versaoEsperada) {
  return http.patch(`/documentos/${docId}/emendar/${secao}/${elementoId}`, {
    acao,
    novoConteudo: novoConteudo ?? null,
    novoTitulo: novoTitulo ?? null,
    justificativa: justificativa ?? null,
    versaoEsperada: versaoEsperada ?? null,
  })
}

export async function reordenarElementoEmenda(docId, secao, elementoId, direcao) {
  return http.patch(`/documentos/${docId}/emendar/${secao}/${elementoId}/reordenar?direcao=${direcao}`, {})
}

export async function incluirElementoEmenda(docId, secao, tipo, titulo, conteudo, parentId, elementOrder, justificativa, versaoEsperada) {
  return http.post(`/documentos/${docId}/emendar/${secao}`, {
    tipo,
    titulo: titulo ?? null,
    conteudo: conteudo ?? null,
    parentId: parentId ?? null,
    elementOrder: elementOrder ?? null,
    justificativa,
    versaoEsperada: versaoEsperada ?? null,
  })
}

export async function saveSecoes(id, secoes, versaoEsperada) {
  if (!secoes?.length) return null
  const itens = []
  for (const secao of secoes) {
    const secaoEnum = SECAO_ENUM_MAP[secao.tipo]
    if (!secaoEnum) continue
    const elementos = secao.elementos ?? []
    for (let i = 0; i < elementos.length; i++) {
      itens.push(converterElemento(elementos[i], secaoEnum, i + 1))
    }
  }
  // X-Client-Id: devolvido no broadcast SSE (event: estrutura) -- é assim que
  // DocumentoEditorPage.vue reconhece e ignora o próprio eco (já aplicou a mudança
  // localmente antes de mandar esta requisição).
  return http.patch(`/documentos/${id}/secoes`, { itens, versaoEsperada: versaoEsperada ?? null }, { 'X-Client-Id': clientId })
}

export async function deleteDocumento(id) {
  return http.del(`/documentos/${id}`)
}

// ── Histórico ─────────────────────────────────────────────────────────────────

export async function listHistorico(documentoId) {
  return http.get(`/documentos/${documentoId}/historico`)
}

// ── Mapa de alteração (Quadro de Justificativas, NSCA 5-3 Anexo XXIV) ──────────

export async function listMapaAlteracao(documentoId) {
  return http.get(`/documentos/${documentoId}/mapa-alteracao`)
}

// ── Presença de edição (aviso de edição concorrente, via SSE) ───────────────────
// Não impede colisão por si só (ver DocumentoConcorrenciaService/versaoEsperada
// acima) -- só avisa "fulano também está editando agora". "Quem está editando"
// é literalmente "quem tem esta conexão aberta agora" (ver
// DocumentoPresencaEmitterRegistry no backend), por isso é uma URL de stream,
// não uma chamada avulsa.

export function presencaStreamUrl(documentoId, token) {
  return `${http.BASE_URL}/documentos/${documentoId}/presenca/stream?token=${encodeURIComponent(token)}`
}

// ── Compartilhamento (coautoria) ─────────────────────────────────────────────

export async function listCompartilhamentos(documentoId) {
  return http.get(`/documentos/${documentoId}/compartilhamentos`)
}

export async function compartilharDocumento(documentoId, cpf) {
  return http.post(`/documentos/${documentoId}/compartilhamentos`, { cpf })
}

export async function removerCompartilhamento(documentoId, usuarioId) {
  return http.del(`/documentos/${documentoId}/compartilhamentos/${usuarioId}`)
}

// ── Anexos ────────────────────────────────────────────────────────────────────

export async function listAnexos(documentoId) {
  return http.get(`/documentos/${documentoId}/anexos`)
}

// ── Portarias (edição, alterações e revogação do documento) ────────────────────

export async function listPortarias(documentoId) {
  return http.get(`/documentos/${documentoId}/portarias`)
}

export async function deleteAnexo(documentoId, anexoId) {
  return http.del(`/documentos/${documentoId}/anexos/${anexoId}`)
}
