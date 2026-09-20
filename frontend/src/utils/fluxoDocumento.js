// Regras do fluxo do documento que as telas precisam decidir (espelham DocumentoStatusService no
// backend; ver docs/ciclo-de-vida.md). Cada função recebe o documento no formato do frontend
// (`situacao_bca`, `situacao_local`) e devolve o que mandar/mostrar -- sem tocar em Vue nem em API,
// para poderem ser testadas (fluxoDocumento.test.js).
import { temEtapaEmCurso, situacaoBcaMeta, situacaoLocalMeta } from './statusDocumento.js'

// Publicar ou revogar = concluir a etapa (EM_PUBLICACAO/EM_REVOGACAO -> SEM_ETAPA); é aqui que
// portaria + BCA são registrados e que a Situação BCA muda (PUBLICADO ou REVOGADO).
export const DESTINO_DE_CONCLUSAO = 'SEM_ETAPA'

// Destino (Situação Local) ao aprovar: o texto vai para publicação; uma análise de revogação, para
// a revogação em si.
export function destinoDeAprovacao(doc) {
  return doc.situacao_local === 'ANALISE_REVOGACAO' ? 'EM_REVOGACAO' : 'EM_PUBLICACAO'
}

// Devolver leva ao começo do trabalho: uma análise de revogação simplesmente encerra a etapa (o
// documento segue PUBLICADO); um documento já PUBLICADO volta à alteração que estava fazendo; um
// nunca publicado, à minuta. SEM_ETAPA = "concluir a etapa".
export function destinoDeDevolucao(doc) {
  if (doc.situacao_local === 'ANALISE_REVOGACAO') return 'SEM_ETAPA'
  return doc.situacao_bca === 'PUBLICADO' ? 'EM_ALTERACAO' : 'MINUTA'
}

export function ehRevogacao(doc) {
  return doc.situacao_local === 'EM_REVOGACAO'
}

// A parte preliminar (epígrafe, ementa, preâmbulo, fecho, assinatura) só é coletada na primeira
// publicação: a portaria de publicação é a única que aparece nela e nunca é substituída.
export function ehPrimeiraPublicacao(doc) {
  return doc.situacao_bca === 'NAO_PUBLICADO' && doc.situacao_local === 'EM_PUBLICACAO'
}

// Publicar um documento que já estava PUBLICADO é uma alteração (a portaria aparece como cláusula
// nos elementos alterados).
export function ehAlteracaoPublicada(doc) {
  return doc.situacao_bca === 'PUBLICADO' && doc.situacao_local === 'EM_PUBLICACAO'
}

// Uma revogação não pode ser devolvida: a única saída de EM_REVOGACAO é REVOGADO.
export function podeDevolverPublicacao(doc) {
  return doc.situacao_local === 'EM_PUBLICACAO'
}

// Versões do documento: a VIGENTE existe desde a primeira publicação; a EM TRAMITAÇÃO existe
// enquanto há etapa local em curso.
export function temVersaoVigente(doc) {
  return !!doc.situacao_bca && doc.situacao_bca !== 'NAO_PUBLICADO'
}

export function temVersaoEmTramitacao(doc) {
  return temEtapaEmCurso(doc.situacao_local)
}

// Ao abrir a visualização, a versão em tramitação (se houver) é a padrão; senão, a vigente.
export function versaoPadrao(doc) {
  return temVersaoEmTramitacao(doc) ? 'TRAMITACAO' : 'VIGENTE'
}

// Evento da linha do tempo de um registro de histórico (t_historico_documento). SEM_ETAPA como
// destino é "concluir a etapa", e o que isso significou depende da origem -- e, para publicar e
// revogar, da mudança de Situação BCA que consta na descrição ("(situação BCA: A → B)").
export function eventoDoHistorico(h) {
  if (h.statusNovo !== 'SEM_ETAPA') {
    const m = situacaoLocalMeta(h.statusNovo)
    return { titulo: m.label, icon: m.icon, color: m.color }
  }
  const descricao = h.descricao ?? ''
  if (/→\s*REVOGADO/.test(descricao)) {
    const m = situacaoBcaMeta('REVOGADO')
    return { titulo: 'Revogado', icon: m.icon, color: m.color }
  }
  if (/NAO_PUBLICADO\s*→\s*PUBLICADO/.test(descricao) || /^[A-Z_]+\s*→\s*PUBLICADO$/.test(descricao)) {
    const m = situacaoBcaMeta('PUBLICADO')
    return { titulo: 'Publicado', icon: m.icon, color: m.color }
  }
  if (h.statusAnterior === 'EM_PUBLICACAO') {
    const m = situacaoBcaMeta('PUBLICADO')
    return { titulo: 'Alteração publicada', icon: m.icon, color: m.color }
  }
  if (h.statusAnterior === 'EM_ALTERACAO') {
    const m = situacaoLocalMeta('EM_ALTERACAO')
    return { titulo: 'Alteração cancelada', icon: 'mdi-close-circle-outline', color: m.color }
  }
  if (h.statusAnterior === 'ANALISE_REVOGACAO') {
    const m = situacaoLocalMeta('ANALISE_REVOGACAO')
    return { titulo: 'Revogação devolvida', icon: 'mdi-undo', color: m.color }
  }
  const m = situacaoLocalMeta('SEM_ETAPA')
  return { titulo: m.label, icon: m.icon, color: m.color }
}

// A Portaria (epígrafe, ementa, preâmbulo, fecho, assinatura) só existe depois da 1ª publicação: um
// documento ainda NAO_PUBLICADO não a exibe (prévia, PDF, HTML). Espelha VersoesDocumento.exibePortaria.
export function exibePortaria(doc) {
  return !!doc?.situacao_bca && doc.situacao_bca !== 'NAO_PUBLICADO'
}

// Revogação total: nenhum elemento é tachado -- só o selo vermelho "REVOGADO" no canto superior
// direito da página da parte preliminar. Aparece quando a Situação BCA já é REVOGADO e também na
// versão em tramitação da revogação aprovada (EM_REVOGACAO). Espelha VersoesDocumento.exibeSeloRevogado
// no backend; a revogação PARCIAL é outra coisa (por elemento, via emenda).
export function exibeSeloRevogado(doc) {
  return doc?.situacao_bca === 'REVOGADO' || doc?.situacao_local === 'EM_REVOGACAO'
}
