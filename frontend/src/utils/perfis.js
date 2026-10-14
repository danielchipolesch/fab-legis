// Os perfis de acesso, com o CÓDIGO e o NOME POR EXTENSO (espelha PerfilEnum, backend, e a tabela t_perfil; o teste perfis.test.js
// e o PerfilEnumIntegracaoTest guardam o mesmo conjunto). O escopo geral/local está no próprio perfil: ADM_GER, AUD_GER e PUB_BCA só existem
// lotados no CENDOC; ADM_OM e AUD_OM, nas demais OMs.
export const OM_GERAL = 'CENDOC'

export const PERFIS = {
  ELB:     { codigo: 'ELB',     nome: 'Elaborador',          cor: 'blue-2',        corDoTexto: 'secondary' },
  APR_OM:  { codigo: 'APR_OM',  nome: 'Aprovador da OM',     cor: 'teal-2',        corDoTexto: 'teal-10' },
  PUB_BCA: { codigo: 'PUB_BCA', nome: 'Publicador do BCA',   cor: 'purple-2',      corDoTexto: 'purple-10' },
  AUD_OM:  { codigo: 'AUD_OM',  nome: 'Auditor da OM',       cor: 'indigo-2',      corDoTexto: 'indigo-10' },
  AUD_GER: { codigo: 'AUD_GER', nome: 'Auditor Geral',       cor: 'indigo-3',      corDoTexto: 'indigo-10' },
  ADM_OM:  { codigo: 'ADM_OM',  nome: 'Administrador da OM', cor: 'deep-orange-2', corDoTexto: 'deep-orange-10' },
  ADM_GER: { codigo: 'ADM_GER', nome: 'Administrador Geral', cor: 'deep-orange-3', corDoTexto: 'deep-orange-10' },
  PNL_OM:  { codigo: 'PNL_OM',  nome: 'Painel Gerencial da OM',        cor: 'cyan-2',        corDoTexto: 'cyan-10' },
  PNL_GER: { codigo: 'PNL_GER', nome: 'Painel Gerencial Geral',        cor: 'cyan-3',        corDoTexto: 'cyan-10' },
}

export const nomeDoPerfil = (codigo) => PERFIS[codigo]?.nome ?? codigo

const tem = (perfis, ...codigos) => (perfis ?? []).some(p => codigos.includes(p))

export const ehAdmin = (perfis) => tem(perfis, 'ADM_OM', 'ADM_GER')
export const ehAdminGeral = (perfis) => tem(perfis, 'ADM_GER')
export const ehAuditor = (perfis) => tem(perfis, 'AUD_OM', 'AUD_GER')

// O perfil de administrador/auditor que vale para a lotação: no CENDOC, o geral; em qualquer outra OM, o da OM.
export const noCendoc = (siglaDaOm) => (siglaDaOm ?? '').toUpperCase() === OM_GERAL
export const perfilDeAdmin = (siglaDaOm) => (noCendoc(siglaDaOm) ? 'ADM_GER' : 'ADM_OM')
export const perfilDeAuditor = (siglaDaOm) => (noCendoc(siglaDaOm) ? 'AUD_GER' : 'AUD_OM')
export const perfilDoPainelGerencial = (siglaDaOm) => (noCendoc(siglaDaOm) ? 'PNL_GER' : 'PNL_OM')

// Perfil do Painel Gerencial (PNL_OM ou PNL_GER), de qualquer escopo; o Painel Gerencial Geral também filtra por OM.
export const ehPainelGerencial = (perfis) => tem(perfis, 'PNL_OM', 'PNL_GER')
export const ehPainelGerencialGeral = (perfis) => tem(perfis, 'PNL_GER')

// Os perfis que exigem revisão periódica: todos menos o Elaborador, que todo usuário tem.
export const perfisElevados = (perfis) => (perfis ?? []).filter(p => p !== 'ELB').sort()
