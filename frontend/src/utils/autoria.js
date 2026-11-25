// Autoria do documento (docs/papeis-e-permissoes.md): os autores são todos iguais -- quem criou o documento e quem foi acrescentado depois.
// Qualquer autor atribui a autoria a outra pessoa. Quem DESVINCULA um autor é o Administrador da OM do documento (a de qualquer autor, a
// qualquer tempo, e atribui a autoria a si mesmo) ou o próprio autor, ao se desvincular -- e só depois de sair da OM do documento.
// Espelha DocumentoAcessoService (backend), que é quem decide de verdade; aqui só se decide o que a tela oferece.

const mesmaOm = (omDoUsuario, omDoDocumento) =>
  omDoUsuario != null && omDoDocumento != null && String(omDoUsuario) === String(omDoDocumento)

// O Administrador da OM do documento (o Administrador Geral não: a função dele é cuidar dos administradores locais).
export function ehAdministradorDaOmDoDocumento(usuario, omIdDoDocumento) {
  return (usuario?.papeis ?? []).includes('ADM_OM') && mesmaOm(usuario?.omId, omIdDoDocumento)
}

// A tela de autores oferece "desvincular" ao lado de um autor?
export function podeDesvincular(autorId, usuario, omIdDoDocumento) {
  if (ehAdministradorDaOmDoDocumento(usuario, omIdDoDocumento)) return true
  return String(autorId) === String(usuario?.id) && !mesmaOm(usuario?.omId, omIdDoDocumento)
}

// O texto do botão: a própria pessoa "se desvincula"; o administrador "desvincula" outro autor.
export function rotuloDeDesvincular(autorId, usuario) {
  return String(autorId) === String(usuario?.id) ? 'Desvincular-me do documento' : 'Desvincular autor'
}

// O Administrador da OM pode atribuir a autoria a si mesmo -- a opção só aparece se ele ainda não é autor.
export function podeAtribuirAutoriaASiMesmo(autores, usuario, omIdDoDocumento) {
  return ehAdministradorDaOmDoDocumento(usuario, omIdDoDocumento)
    && !(autores ?? []).some(a => String(a.usuarioId) === String(usuario?.id))
}
