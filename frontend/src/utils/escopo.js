// Escopo de acesso (espelha EscopoDeAcesso, backend; docs/autenticacao.md): só o CENDOC tem o escopo GERAL; toda outra OM tem escopo LOCAL, e o
// escopo está no próprio papel (ADM_GER/AUD_GER/PUB_BCA só no CENDOC; ADM_OM/AUD_OM nas demais OMs). Funções puras sobre a sigla da OM de
// quem age e os papéis.
import { OM_GERAL, noCendoc, ehAdmin as ehAdminDeQualquerEscopo } from '@/utils/papeis.js'

export { OM_GERAL }

export const ehOmGeral = (sigla) => noCendoc(sigla)

export const temEscopoGeral = (siglaDaOmDoAtor) => ehOmGeral(siglaDaOmDoAtor)

// As OMs que quem age pode escolher ao criar/editar um usuário: todas (geral) ou só a própria (local).
export function omsDoEscopo(oms, siglaDoAtor) {
  return temEscopoGeral(siglaDoAtor) ? (oms ?? []) : (oms ?? []).filter(om => om.sigla === siglaDoAtor)
}

// Quais papéis o ator pode conceder ao usuário que ficará na OM de destino. Elaborador é sempre; Aprovador da OM, dentro do escopo; Auditor e
// Administrador existem em duas versões (geral no CENDOC, da OM nas demais) e o admin da OM só concede o Auditor da OM; Administrador só o
// geral; Publicador do BCA só no CENDOC (e só o geral concede); Publicador do Boletim Interno (das NPAs da própria OM), em qualquer OM e
// concedido também pelo admin da OM.
export function papeisConcediveis(siglaDoAtor, siglaDaOmDeDestino) {
  const geral = temEscopoGeral(siglaDoAtor)
  return {
    elaborador: true,
    aprovador: true,
    auditor: true,
    painel: true,
    admin: geral,
    publicador: geral && ehOmGeral(siglaDaOmDeDestino),
    publicadorBi: true,
  }
}

// O ator administra este usuário (editar, redefinir senha)? O geral, todos; o local, os da própria OM que não são administradores.
export function podeAdministrar(siglaDoAtor, alvo) {
  if (temEscopoGeral(siglaDoAtor)) return true
  return alvo?.omSigla === siglaDoAtor && !ehAdminDeQualquerEscopo(alvo?.papeis)
}
