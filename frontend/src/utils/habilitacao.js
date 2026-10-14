// Habilitação de documentos: quais espécies a OM da pessoa pode CRIAR. O backend (GET /habilitacao-de-documentos/minhas) devolve a
// lista de espécies que a OM cria -- as que não dependem de habilitação (a NPA) e as convencionais habilitadas pelo
// administrador. Quem não cria uma espécie continua a consultá-la e a editá-la como coautor; só o botão de CRIAR (e o de clonar,
// que também cria) deixa de aparecer. Funções puras, sobre essa lista de { sigla, tipoDeEspecie }.

// A OM cria alguma espécie deste tipo (módulo)? Decide se o botão de criar do módulo aparece.
export function podeCriarNoModulo(especiesCriaveis, tipoDeEspecie) {
  return (especiesCriaveis ?? []).some(e => e.tipoDeEspecie === tipoDeEspecie)
}

// A OM cria esta espécie (pela sigla)? Decide se o botão de clonar um documento dela aparece.
export function podeCriarEspecie(especiesCriaveis, sigla) {
  return (especiesCriaveis ?? []).some(e => e.sigla === sigla)
}

// Só as espécies do sistema que a OM cria, na ordem original (alimenta o seletor de espécie do diálogo de novo documento).
export function apenasCriaveis(especiesDoSistema, especiesCriaveis) {
  const siglas = new Set((especiesCriaveis ?? []).map(e => e.sigla))
  return (especiesDoSistema ?? []).filter(e => siglas.has(e.sigla))
}

// Tela de administração, visão por espécie: de "cada OM com as suas espécies" (o que o backend lista) para "cada espécie com as
// suas OMs" -- { [especieId]: [omId, ...] }. `habilitacoes`: [{ omId, especieIds }].
export function omIdsPorEspecie(habilitacoes) {
  const porEspecie = {}
  for (const h of habilitacoes ?? []) {
    for (const especieId of h.especieIds ?? []) (porEspecie[especieId] ??= []).push(h.omId)
  }
  return porEspecie
}

// O seletor de OMs de uma espécie busca pela sigla ou pelo nome (sem acento nem caixa), mas mostra só a sigla.
export function filtrarOpcoesDeOm(oms, termo) {
  const normalizar = s => (s ?? '').toString().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
  const t = normalizar(termo)
  return (oms ?? []).filter(om => !t || normalizar(om.sigla).includes(t) || normalizar(om.nome).includes(t))
}

// Quantas das espécies a OM pode criar (resumo do filtro por OM): [{ id }] e { [especieId]: [omId] }.
export function especiesDaOm(especies, omIdsPorEspecieMapa, omId) {
  return (especies ?? []).filter(e => (omIdsPorEspecieMapa?.[e.id] ?? []).includes(omId))
}
