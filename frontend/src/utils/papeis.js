// Os perfis (papéis) de acesso, com o CÓDIGO e o NOME POR EXTENSO (espelha PapelEnum, backend, e a tabela t_papel; o teste papeis.test.js
// e o PapelEnumIntegracaoTest guardam o mesmo conjunto). O escopo geral/local está no próprio perfil: ADM_GER, AUD_GER e PUB_BCA só existem
// lotados no CENDOC; ADM_OM e AUD_OM, nas demais OMs.
export const OM_GERAL = 'CENDOC'

export const PAPEIS = {
  ELB:     { codigo: 'ELB',     nome: 'Elaborador',          cor: 'blue-2',        corDoTexto: 'secondary' },
  APR_OM:  { codigo: 'APR_OM',  nome: 'Aprovador da OM',     cor: 'teal-2',        corDoTexto: 'teal-10' },
  PUB_BCA: { codigo: 'PUB_BCA', nome: 'Publicador do BCA',   cor: 'purple-2',      corDoTexto: 'purple-10' },
  AUD_OM:  { codigo: 'AUD_OM',  nome: 'Auditor da OM',       cor: 'indigo-2',      corDoTexto: 'indigo-10' },
  AUD_GER: { codigo: 'AUD_GER', nome: 'Auditor Geral',       cor: 'indigo-3',      corDoTexto: 'indigo-10' },
  ADM_OM:  { codigo: 'ADM_OM',  nome: 'Administrador da OM', cor: 'deep-orange-2', corDoTexto: 'deep-orange-10' },
  ADM_GER: { codigo: 'ADM_GER', nome: 'Administrador Geral', cor: 'deep-orange-3', corDoTexto: 'deep-orange-10' },
}

export const nomeDoPapel = (codigo) => PAPEIS[codigo]?.nome ?? codigo

const tem = (papeis, ...codigos) => (papeis ?? []).some(p => codigos.includes(p))

export const ehAdmin = (papeis) => tem(papeis, 'ADM_OM', 'ADM_GER')
export const ehAdminGeral = (papeis) => tem(papeis, 'ADM_GER')
export const ehAuditor = (papeis) => tem(papeis, 'AUD_OM', 'AUD_GER')

// O perfil de administrador/auditor que vale para a lotação: no CENDOC, o geral; em qualquer outra OM, o da OM.
export const noCendoc = (siglaDaOm) => (siglaDaOm ?? '').toUpperCase() === OM_GERAL
export const papelDeAdmin = (siglaDaOm) => (noCendoc(siglaDaOm) ? 'ADM_GER' : 'ADM_OM')
export const papelDeAuditor = (siglaDaOm) => (noCendoc(siglaDaOm) ? 'AUD_GER' : 'AUD_OM')

// Os perfis que exigem revisão periódica: todos menos o Elaborador, que todo usuário tem.
export const papeisElevados = (papeis) => (papeis ?? []).filter(p => p !== 'ELB').sort()
