import { nomeDoPerfil, perfisElevados } from '@/utils/perfis.js'

// Revisão periódica de acessos: textos e regras da tela de usuários (espelha RevisaoDeAcesso, backend). A revisão é automática:
// o servidor devolve, em cada usuário, o `motivoDaRevisao` e se quem pergunta o revisa (`revisavel`).
export const MOTIVO = {
  NENHUM:          { label: 'Em dia',                color: 'green-2',  textColor: 'green-10',  explicacao: '' },
  TROCA_DE_OM:     { label: 'Trocou de OM',          color: 'orange-3', textColor: 'orange-10', explicacao: 'A pessoa mudou de OM: reconfirme os perfis dela.' },
  REVISAO_VENCIDA: { label: 'Revisão vencida',       color: 'red-2',    textColor: 'red-10',    explicacao: 'Passaram-se 6 meses desde a última revisão.' },
  CONTA_DORMENTE:  { label: 'Sem entrar há 6 meses', color: 'amber-3',  textColor: 'amber-10',  explicacao: 'A conta não entra no sistema há mais de 180 dias.' },
}

export const motivoDaRevisao = (motivo) => MOTIVO[motivo] ?? MOTIVO.NENHUM

export const rotuloDoPerfil = nomeDoPerfil

// Os perfis que exigem revisão: todos menos o Elaborador, que todo usuário tem.
export const perfisElevadosDe = (usuario) => perfisElevados(usuario?.perfis)

export const temPerfilElevado = (usuario) => perfisElevadosDe(usuario).length > 0

// O acesso precisa de revisão e quem está logado é quem o revisa (a lista só marca o que dá para agir).
export const precisaDeRevisao = (usuario) => !!usuario?.revisavel && (usuario?.motivoDaRevisao ?? 'NENHUM') !== 'NENHUM'

export const contarPendentes = (usuarios) => (usuarios ?? []).filter(precisaDeRevisao).length
