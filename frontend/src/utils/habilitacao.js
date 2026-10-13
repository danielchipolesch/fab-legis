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

// Tela de administração: as OMs que combinam com a busca (sigla ou nome, sem acento nem caixa) e, se pedido, só as que têm
// alguma espécie habilitada. `oms`: [{ omId, sigla, nome, especieIds }].
export function filtrarOms(oms, busca, soHabilitadas) {
  const normalizar = s => (s ?? '').toString().normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
  const termo = normalizar(busca)
  return (oms ?? []).filter(om => {
    if (soHabilitadas && !om.especieIds.length) return false
    return !termo || normalizar(om.sigla).includes(termo) || normalizar(om.nome).includes(termo)
  })
}
