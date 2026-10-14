// Escopo de acesso (espelha EscopoDeAcesso, backend; docs/autenticacao.md): só o CENDOC tem o escopo GERAL; toda outra OM tem
// escopo LOCAL. Funções puras sobre a sigla da OM de quem age e os papéis.
export const OM_GERAL = 'CENDOC'

export const ehOmGeral = (sigla) => (sigla ?? '').toUpperCase() === OM_GERAL

export const temEscopoGeral = (siglaDaOmDoAtor) => ehOmGeral(siglaDaOmDoAtor)

// Admin GERAL = ADMIN no CENDOC (mantém os admins locais, cadastra habilitação e DOM). Admin LOCAL = ADMIN em outra OM.
export const ehAdminGeral = (papeis, sigla) => (papeis ?? []).includes('ADMIN') && temEscopoGeral(sigla)

// As OMs que quem age pode escolher ao criar/editar um usuário: todas (geral) ou só a própria (local).
export function omsDoEscopo(oms, siglaDoAtor) {
  return temEscopoGeral(siglaDoAtor) ? (oms ?? []) : (oms ?? []).filter(om => om.sigla === siglaDoAtor)
}

// Quais papéis o ator pode conceder ao usuário que ficará na OM de destino. Editor é sempre; Aprovador e Auditor, dentro do
// escopo; Administrador só o geral; Publicador só no CENDOC (a publicação no BCA é do CENDOC) e só o geral concede.
export function papeisConcediveis(siglaDoAtor, siglaDaOmDeDestino) {
  const geral = temEscopoGeral(siglaDoAtor)
  return {
    editor: true,
    aprovador: true,
    auditor: true,
    admin: geral,
    publicador: geral && ehOmGeral(siglaDaOmDeDestino),
  }
}

// O ator administra este usuário (editar, redefinir senha)? O geral, todos; o local, os da própria OM que não são administradores.
export function podeAdministrar(siglaDoAtor, alvo) {
  if (temEscopoGeral(siglaDoAtor)) return true
  return alvo?.omSigla === siglaDoAtor && !(alvo?.papeis ?? []).includes('ADMIN')
}
