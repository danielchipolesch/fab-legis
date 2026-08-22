import * as http from './client.js'

const SECAO_CONFIG = {
  PARTE_PRELIMINAR: { tipo: 'parte_preliminar', titulo: 'Parte Preliminar', ordem: 1 },
  PARTE_NORMATIVA:  { tipo: 'parte_normativa',  titulo: 'Parte Normativa',  ordem: 2 },
}

const SECAO_ENUM_MAP = {
  parte_preliminar: 'PARTE_PRELIMINAR',
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

function converterElemento(el, secaoEnum, posicao) {
  return {
    secao: secaoEnum,
    tipo: (el.tipo ?? '').toUpperCase(),
    elementOrder: posicao,
    titulo: el.titulo ?? null,
    conteudo: el.conteudo ?? null,
    fullTextContent: el.fullTextContent ?? null,
    filhos: (el.filhos ?? []).map((f, i) => converterElemento(f, secaoEnum, i + 1)),
  }
}

export function backendParaFrontend(doc) {
  if (!doc) return null

  const preliminarItens = doc.itensPreliminares ?? []
  const normativaItens  = doc.itensNormativos   ?? []

  // Retorna null quando todas as seções estão vazias (documento novo, sem dados salvos)
  // para que o store gere o template e salve no banco
  const hasAnyData = preliminarItens.length > 0 || normativaItens.length > 0

  const secoes = hasAnyData ? [
    buildSecao('PARTE_PRELIMINAR', preliminarItens),
    buildSecao('PARTE_NORMATIVA',  normativaItens),
    { tipo: 'anexos', titulo: 'Anexos', ordem: 3, id: crypto.randomUUID(), elementos: [] },
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
    data_arquivamento: parseDtCriacao(doc.dtArquivamento),
    data_revogacao:    parseDtCriacao(doc.dtRevogacao),
    data_cancelamento: parseDtCriacao(doc.dtCancelamento),
    data_em_alteracao: parseDtCriacao(doc.dtEmAlteracao),
    data_alterado:     parseDtCriacao(doc.dtAlterado),
    status: doc.statusDocumento,
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

export async function listDocumentos() {
  const data = await http.get('/documentos/obter-todos?size=200&sortBy=id')
  const items = Array.isArray(data) ? data : []
  return items.map(backendParaFrontend)
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
    body.orgaoPortaria   = refs.orgaoPortaria ?? null
    body.setorPortaria   = refs.setorPortaria ?? null
    body.numeroPortaria  = refs.numeroPortaria ?? null
    body.dataPortaria    = refs.dataPortaria ?? null
    body.numeroBca       = refs.numeroBca ?? null
    body.dataBca         = refs.dataBca ?? null
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
  return http.put(`/documentos/${id}/secoes`, { itens, versaoEsperada: versaoEsperada ?? null })
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

// ── Presença de edição (aviso de edição concorrente) ────────────────────────────
// Não impede colisão por si só (ver DocumentoConcorrenciaService/versaoEsperada
// acima) -- só avisa "fulano também está editando agora".

export async function registrarPresenca(documentoId) {
  return http.post(`/documentos/${documentoId}/presenca`)
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

export async function deleteAnexo(documentoId, anexoId) {
  return http.del(`/documentos/${documentoId}/anexos/${anexoId}`)
}
