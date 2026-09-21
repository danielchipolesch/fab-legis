// Perfil dos atos normativos (DCA, ICA, NSCA...) no frontend: LC 95/1998, Decreto nº 12.002/2024 e NSCA 5-3.
// Espelha RegrasDeEspecieConvencional (backend). A numeração continua em utils/numbering.js.
import { renumberElements, renumberElementsEmAlteracao } from '@/utils/numbering.js'

const FILHOS_PERMITIDOS = {
  raiz:               [{ tipo: 'capitulo', label: 'Capítulo' }, { tipo: 'artigo', label: 'Artigo' }],
  capitulo:           [{ tipo: 'secao_normativa', label: 'Seção' }, { tipo: 'artigo', label: 'Artigo' }],
  secao_normativa:    [{ tipo: 'subsecao_normativa', label: 'Subseção' }, { tipo: 'artigo', label: 'Artigo' }],
  subsecao_normativa: [{ tipo: 'artigo', label: 'Artigo' }],
  artigo:             [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)' },
    { tipo: 'inciso',          label: 'Inciso' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

export const TIPOS_DE_AGRUPAMENTO = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

export function filhosPermitidos(pai) {
  return FILHOS_PERMITIDOS[pai ?? 'raiz'] ?? []
}

// Documento já publicado (PUBLICADO ou REVOGADO): numeração por emenda (elemento em vigor nunca é renumerado), em
// qualquer etapa local; senão, numeração livre.
export function renumerar(elementos, documento) {
  if (documento?.situacao_bca && documento.situacao_bca !== 'NAO_PUBLICADO') {
    renumberElementsEmAlteracao(elementos)
  } else {
    renumberElements(elementos)
  }
}
