// O perfil de uma espécie normativa no frontend: as regras que variam por espécie (hierarquia dos elementos,
// numeração, cabeçalho) atrás de uma interface só, escolhida pelo `tipoDeEspecie` que o backend informa -- nunca pela
// sigla da espécie. Espelha as interfaces de domain.regras (backend); ver docs/arquitetura.md.
import * as convencional from './convencional.js'
import * as npa from './npa.js'

export const CONVENCIONAL = 'CONVENCIONAL'
export const COMUNICACAO_OFICIAL_PADRONIZADA = 'COMUNICACAO_OFICIAL_PADRONIZADA'

// O módulo do sistema que cuida de cada tipo de espécie: a tela inicial própria (rota e caminho), como aparece no hub
// (nome, subtítulo, ícone) e o que a listagem dele mostra. Um tipo de espécie novo ganha o seu módulo aqui -- o hub só
// acrescenta mais um atalho, sem mudar os cards.
//   rotuloDoCriar: a opção do botão Criar do hub, que só leva ao módulo (quem cria de fato é o botão do próprio módulo)
//   colunasOcultas: colunas da tabela que não fazem sentido para o módulo (a NPA não tem assunto básico e é uma só espécie)
//   rotuloDaSituacaoOficial: como o módulo chama a situação real do documento (BCA nas convencionais; Boletim Interno na NPA)
const MODULO_CONVENCIONAL = {
  tipoDeEspecie: 'CONVENCIONAL',
  rota: 'modulo-convencionais',
  caminho: '/convencionais',
  nome: 'Espécies Convencionais',
  subtitulo: 'MCA, NSCA, ICA, ROCA…',
  descricao: 'Gestão e acompanhamento das espécies convencionais do Comando da Aeronáutica',
  icone: 'mdi-book-open-page-variant-outline',
  rotuloDoBotaoNovo: 'Novo Documento',
  rotuloDoCriar: 'Criar Espécie Convencional',
  rotuloDaSituacaoOficial: 'Situação BCA',
  rotuloDoNumero: 'Número',
  colunasOcultas: [],
}

const MODULO_COMUNICACAO_OFICIAL_PADRONIZADA = {
  tipoDeEspecie: 'COMUNICACAO_OFICIAL_PADRONIZADA',
  rota: 'modulo-npa',
  caminho: '/npa',
  nome: 'NPA',
  subtitulo: 'Norma Padrão de Ação',
  descricao: 'Gestão e acompanhamento das Normas Padrão de Ação (Comunicações Oficiais Padronizadas, NSCA 5-3)',
  icone: 'mdi-clipboard-text-outline',
  rotuloDoBotaoNovo: 'Nova NPA',
  rotuloDoCriar: 'Criar NPA',
  rotuloDaSituacaoOficial: 'Boletim Interno',
  rotuloDoNumero: 'Identificação',
  colunasOcultas: ['especie', 'assunto_basico'],
}

const PERFIS = {
  [CONVENCIONAL]: {
    tipo: CONVENCIONAL,
    modulo: MODULO_CONVENCIONAL,
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
    modulo: MODULO_COMUNICACAO_OFICIAL_PADRONIZADA,
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

// Os módulos do sistema, na ordem em que aparecem no hub e no menu.
export const MODULOS = Object.values(PERFIS).map(p => p.modulo)

export function moduloDe(tipoDeEspecie) {
  return perfilDe(tipoDeEspecie).modulo
}

// O módulo a que um documento do frontend pertence.
export function moduloDoDocumento(documento) {
  return moduloDe(documento?.tipo_de_especie)
}

// O perfil do documento do frontend (campo tipo_de_especie, ver api/documentos.js).
export function perfilDoDocumento(documento) {
  return perfilDe(documento?.tipo_de_especie)
}

// Renumera os elementos da parte normativa segundo as regras da espécie do documento.
export function renumerarElementos(elementos, documento) {
  perfilDoDocumento(documento).renumerar(elementos, documento)
}
