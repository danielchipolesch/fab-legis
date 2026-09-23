// Fonte única de rótulo/ícone/cor por situação do documento. Há DUAS situações independentes
// (ver SituacaoBcaEnum/SituacaoLocalEnum no backend e docs/ciclo-de-vida.md):
//   - Situação BCA: a REAL, espelha o repositório oficial (NAO_PUBLICADO/PUBLICADO/REVOGADO);
//     só muda quando portaria + BCA são registrados. É o chip principal.
//   - Situação Local: a etapa interna em curso (rascunho, revisão, alteração...), inexistente na
//     vida real; SEM_ETAPA quando não há nenhuma -- e então não é mostrada.
// Cada situação tem UMA família de cor, sem repetir entre si -- badges de situações diferentes
// não podem parecer a mesma coisa à primeira vista.
//
// `bg`/`fg` (par claro/escuro da mesma família) alimenta chips "tonal" (StatusBadge.vue, filtros
// da HomePage); `color` (nome sólido do Quasar, sem sufixo de tom) alimenta badges sólidos/ícones
// (DocumentoViewerPage.vue, BuscaPage.vue).
export const SITUACAO_BCA_META = {
  NAO_PUBLICADO: { label: 'Não publicado', icon: 'mdi-file-clock-outline',  bg: 'grey-3',      fg: 'grey-9',       color: 'grey'      },
  PUBLICADO:     { label: 'Publicado',     icon: 'mdi-publish',             bg: 'indigo-2',    fg: 'indigo-10',    color: 'indigo'    },
  REVOGADO:      { label: 'Revogado',      icon: 'mdi-file-remove-outline', bg: 'blue-grey-2', fg: 'blue-grey-10', color: 'blue-grey' },
}

export const SITUACAO_LOCAL_META = {
  RASCUNHO:          { label: 'Rascunho',             icon: 'mdi-pencil-outline',         bg: 'grey-3',        fg: 'grey-9',         color: 'grey'        },
  MINUTA:            { label: 'Minuta',               icon: 'mdi-file-edit-outline',      bg: 'orange-2',      fg: 'orange-10',      color: 'orange'      },
  EM_REVISAO:        { label: 'Em Revisão',           icon: 'mdi-account-search-outline', bg: 'amber-2',       fg: 'amber-10',       color: 'amber'       },
  EM_PUBLICACAO:     { label: 'Em Publicação',        icon: 'mdi-timer-sand',             bg: 'blue-2',        fg: 'blue-10',        color: 'blue'        },
  EM_ALTERACAO:      { label: 'Em Alteração',         icon: 'mdi-pencil-lock-outline',    bg: 'deep-orange-2', fg: 'deep-orange-10', color: 'deep-orange' },
  ANALISE_REVOGACAO: { label: 'Análise de Revogação', icon: 'mdi-file-search-outline',    bg: 'brown-2',       fg: 'brown-10',       color: 'brown'       },
  EM_REVOGACAO:      { label: 'Em Revogação',         icon: 'mdi-timer-sand',             bg: 'deep-purple-2', fg: 'deep-purple-10', color: 'deep-purple' },
  CANCELADO:         { label: 'Cancelado',            icon: 'mdi-cancel',                 bg: 'red-2',         fg: 'red-10',         color: 'negative'    },
  SEM_ETAPA:         { label: 'Sem etapa em curso',   icon: 'mdi-minus-circle-outline',   bg: 'grey-2',        fg: 'grey-7',         color: 'grey'        },
}

const DESCONHECIDA = { icon: 'mdi-help', bg: 'grey-3', fg: 'grey-9', color: 'grey' }

export function situacaoBcaMeta(situacao) {
  return SITUACAO_BCA_META[situacao] ?? { ...DESCONHECIDA, label: situacao }
}

export function situacaoLocalMeta(situacao) {
  return SITUACAO_LOCAL_META[situacao] ?? { ...DESCONHECIDA, label: situacao }
}

// Há etapa local em curso (e portanto uma versão em tramitação do documento).
export function temEtapaEmCurso(situacaoLocal) {
  return !!situacaoLocal && situacaoLocal !== 'SEM_ETAPA'
}
