// Revisão periódica de acessos: textos e regras da tela (espelha RevisaoDeAcesso, backend).
export const MOTIVO = {
  NENHUM:          { label: 'Em dia',                    color: 'green-2',  textColor: 'green-10', explicacao: '' },
  TROCA_DE_OM:     { label: 'Trocou de OM',              color: 'orange-3', textColor: 'orange-10', explicacao: 'A pessoa mudou de OM: reconfirme os papéis dela.' },
  REVISAO_VENCIDA: { label: 'Revisão vencida',           color: 'red-2',    textColor: 'red-10',    explicacao: 'Passaram-se 6 meses desde a última revisão.' },
  CONTA_DORMENTE:  { label: 'Sem entrar há 6 meses',     color: 'amber-3',  textColor: 'amber-10',  explicacao: 'A conta não entra no sistema há mais de 180 dias.' },
}

export const motivoDaRevisao = (motivo) => MOTIVO[motivo] ?? MOTIVO.NENHUM

export const ROTULO_DO_PAPEL = { APROV: 'Aprovador', PUBLIC: 'Publicador', ADMIN: 'Administrador', AUDITOR: 'Auditor' }

export const rotuloDoPapel = (papel) => ROTULO_DO_PAPEL[papel] ?? papel

// Quantas pendências há na lista (o resumo do alto da tela).
export const contarPendentes = (linhas) => (linhas ?? []).filter(l => l.pendente).length

// Quem revisa: o admin geral (CENDOC) confere os admins locais e o pessoal do CENDOC; o admin local, os demais da própria OM.
export const textoDeQuemRevisa = (temEscopoGeral) => temEscopoGeral
  ? 'Como administrador geral, você confere os administradores locais e o pessoal do CENDOC.'
  : 'Como administrador local, você confere os demais usuários da sua OM. Os administradores locais são conferidos pelo CENDOC.'
