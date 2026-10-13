// Rótulos e cores da trilha dos acessos (espelha AcaoDeAcessoEnum, backend).
export const ACAO_DE_ACESSO = {
  CRIOU_USUARIO:     { label: 'Criou usuário',     color: 'green-8' },
  ALTEROU_PAPEIS:    { label: 'Alterou papéis',    color: 'primary' },
  ATIVOU_USUARIO:    { label: 'Ativou',            color: 'teal-8' },
  DESATIVOU_USUARIO: { label: 'Desativou',         color: 'negative' },
  TROCOU_OM:         { label: 'Trocou de OM',      color: 'deep-orange-8' },
  REDEFINIU_SENHA:   { label: 'Redefiniu a senha', color: 'brown-6' },
}

export const acaoDeAcessoLabel = (acao) => ACAO_DE_ACESSO[acao]?.label ?? acao
export const acaoDeAcessoCor = (acao) => ACAO_DE_ACESSO[acao]?.color ?? 'grey'

// Quem fez a mudança: o usuário, ou "Sistema" quando foi o próprio sistema (administrador padrão do primeiro boot).
export const atorDoAcesso = (registro) => registro.atorNome ?? 'Sistema'
