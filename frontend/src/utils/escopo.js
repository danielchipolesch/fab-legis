// Escopo de acesso (espelha EscopoDeAcesso, backend; docs/papeis-e-permissoes.md): quem tem o escopo GERAL é o Administrador Geral (ADM_GER, só no
// CENDOC); todo outro administrador é LOCAL e só vê e gerencia a própria OM -- o do CENDOC inclusive: o CENDOC também é uma unidade, com o seu
// administrador local ao lado do geral. O escopo está no próprio papel (ADM_GER/AUD_GER/PUB_BCA/PNL_GER só no CENDOC; AUD_OM nas demais OMs;
// ADM_OM, PNL_OM e PUB_BI em qualquer OM). Funções puras sobre a sigla da OM e os papéis de quem age.
import { OM_GERAL, noCendoc, ehAdmin as ehAdminDeQualquerEscopo, ehAdminGeral } from '@/utils/papeis.js'

export { OM_GERAL }

export const ehOmGeral = (sigla) => noCendoc(sigla)

// Lotado no CENDOC (a OM do escopo geral). Quem administra com escopo geral é outra pergunta: ehAdminGeral(papeis).
export const temEscopoGeral = (siglaDaOmDoAtor) => ehOmGeral(siglaDaOmDoAtor)

// As OMs que quem age pode escolher ao criar/editar um usuário: todas (administrador geral) ou só a própria (administrador local, o do CENDOC inclusive).
export function omsDoEscopo(oms, siglaDoAtor, papeisDoAtor) {
  return ehAdminGeral(papeisDoAtor) ? (oms ?? []) : (oms ?? []).filter(om => om.sigla === siglaDoAtor)
}

// Os papéis (além do Elaborador, que todo usuário tem) que o ator pode conceder ao usuário que ficará na OM de destino, na ordem em que a tela os
// mostra. O que ele NÃO pode conceder nem aparece (acesso mínimo: o servidor também recusa, com 403).
//   - Administrador Geral: Aprovador da OM, Publicador do BCA (só no CENDOC), Publicador do Boletim Interno, Auditor (Geral no CENDOC, da OM nas
//     demais), Painel Gerencial Geral (só no CENDOC) e da OM, Administrador Geral (só no CENDOC) e Administrador da OM (em qualquer OM).
//   - Administrador da OM: só os papéis locais -- Aprovador da OM, Publicador do Boletim Interno, Auditor da OM (não existe no CENDOC) e Painel
//     Gerencial da OM.
export function papeisConcediveis(papeisDoAtor, siglaDaOmDeDestino) {
  const noCendocDestino = ehOmGeral(siglaDaOmDeDestino)
  if (ehAdminGeral(papeisDoAtor)) {
    return [
      'APR_OM',
      ...(noCendocDestino ? ['PUB_BCA'] : []),
      'PUB_BI',
      noCendocDestino ? 'AUD_GER' : 'AUD_OM',
      ...(noCendocDestino ? ['PNL_GER'] : []),
      'PNL_OM',
      ...(noCendocDestino ? ['ADM_GER'] : []),
      'ADM_OM',
    ]
  }
  return ['APR_OM', 'PUB_BI', ...(noCendocDestino ? [] : ['AUD_OM']), 'PNL_OM']
}

// O ator administra este usuário (editar, redefinir senha)? O geral, todos; o local, os da própria OM que não são administradores.
export function podeAdministrar(papeisDoAtor, siglaDoAtor, alvo) {
  if (ehAdminGeral(papeisDoAtor)) return true
  return alvo?.omSigla === siglaDoAtor && !ehAdminDeQualquerEscopo(alvo?.papeis)
}
