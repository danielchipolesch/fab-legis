// O perfil de uma espécie normativa no frontend: as regras que variam por espécie (hierarquia dos elementos,
// numeração, cabeçalho) atrás de uma interface só, escolhida pelo `tipoDeRegras` que o backend informa -- nunca pela
// sigla da espécie. Espelha as interfaces de domain.regras (backend); ver docs/arquitetura.md.
import * as atoNormativo from './atoNormativo.js'
import * as npa from './npa.js'

export const ATO_NORMATIVO = 'ATO_NORMATIVO'
export const NPA = 'NPA'

const PERFIS = {
  [ATO_NORMATIVO]: {
    tipo: ATO_NORMATIVO,
    ehNpa: false,
    filhosPermitidos: atoNormativo.filhosPermitidos,
    tiposDeAgrupamento: atoNormativo.TIPOS_DE_AGRUPAMENTO,
    renumerar: atoNormativo.renumerar,
    // Nos atos normativos o artigo (não agrupamento) entra antes do primeiro subagrupamento do pai.
    conteudoAntesDosAgrupamentos: true,
    // Promover/rebaixar um elemento (mudar o nível na hierarquia do artigo) só existe nos atos normativos.
    permitePromoverRebaixar: true,
    // Um ato normativo publicado pode ser alterado (ciclo de emenda).
    permiteAlteracao: true,
  },
  [NPA]: {
    tipo: NPA,
    ehNpa: true,
    filhosPermitidos: npa.filhosPermitidos,
    tiposDeAgrupamento: npa.TIPOS_DE_AGRUPAMENTO,
    renumerar: npa.renumerar,
    // Na NPA seção, subseção e parágrafo do mesmo pai dividem a sequência, em qualquer ordem.
    conteudoAntesDosAgrupamentos: false,
    permitePromoverRebaixar: false,
    // A NPA só é publicada e revogada, nunca alterada (docs/ciclo-de-vida.md).
    permiteAlteracao: false,
  },
}

export function perfilDe(tipoDeRegras) {
  return PERFIS[tipoDeRegras] ?? PERFIS[ATO_NORMATIVO]
}

// O perfil do documento do frontend (campo tipo_de_regras, ver api/documentos.js).
export function perfilDoDocumento(documento) {
  return perfilDe(documento?.tipo_de_regras)
}

// Renumera os elementos da parte normativa segundo as regras da espécie do documento.
export function renumerarElementos(elementos, documento) {
  perfilDoDocumento(documento).renumerar(elementos, documento)
}
