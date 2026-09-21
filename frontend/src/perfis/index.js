// O perfil de uma espécie normativa no frontend: as regras que variam por espécie (hierarquia dos elementos,
// numeração, cabeçalho) atrás de uma interface só, escolhida pelo `tipoDeEspecie` que o backend informa -- nunca pela
// sigla da espécie. Espelha as interfaces de domain.regras (backend); ver docs/arquitetura.md.
import * as convencional from './convencional.js'
import * as npa from './npa.js'

export const CONVENCIONAL = 'CONVENCIONAL'
export const COMUNICACAO_OFICIAL_PADRONIZADA = 'COMUNICACAO_OFICIAL_PADRONIZADA'

const PERFIS = {
  [CONVENCIONAL]: {
    tipo: CONVENCIONAL,
    ehNpa: false,
    filhosPermitidos: convencional.filhosPermitidos,
    tiposDeAgrupamento: convencional.TIPOS_DE_AGRUPAMENTO,
    renumerar: convencional.renumerar,
    // Nas espécies convencionais o artigo (não agrupamento) entra antes do primeiro subagrupamento do pai.
    conteudoAntesDosAgrupamentos: true,
    // Promover/rebaixar um elemento (mudar o nível na hierarquia do artigo) só existe nas espécies convencionais.
    permitePromoverRebaixar: true,
    // Um ato de espécie convencional publicado pode ser alterado (ciclo de emenda).
    permiteAlteracao: true,
  },
  [COMUNICACAO_OFICIAL_PADRONIZADA]: {
    tipo: COMUNICACAO_OFICIAL_PADRONIZADA,
    ehNpa: true,
    filhosPermitidos: npa.filhosPermitidos,
    tiposDeAgrupamento: npa.TIPOS_DE_AGRUPAMENTO,
    renumerar: npa.renumerar,
    // Na comunicação oficial padronizada (NPA) seção, subseção e parágrafo do mesmo pai dividem a sequência, em qualquer ordem.
    conteudoAntesDosAgrupamentos: false,
    permitePromoverRebaixar: false,
    // A NPA só é publicada e revogada, nunca alterada (docs/ciclo-de-vida.md).
    permiteAlteracao: false,
  },
}

export function perfilDe(tipoDeEspecie) {
  return PERFIS[tipoDeEspecie] ?? PERFIS[CONVENCIONAL]
}

// O perfil do documento do frontend (campo tipo_de_especie, ver api/documentos.js).
export function perfilDoDocumento(documento) {
  return perfilDe(documento?.tipo_de_especie)
}

// Renumera os elementos da parte normativa segundo as regras da espécie do documento.
export function renumerarElementos(elementos, documento) {
  perfilDoDocumento(documento).renumerar(elementos, documento)
}
