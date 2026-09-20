package br.com.danielchipolesch.domain.entities.estruturaDocumento;

// Situação Local: a etapa INTERNA em que o documento está neste sistema. Não existe na
// vida real (o repositório oficial não conhece "em revisão"), então NUNCA substitui a
// situação real -- os dois aparecem juntos (ver SituacaoBcaEnum). Um documento tem no
// máximo uma etapa local por vez: não se inicia uma revogação durante uma alteração.
//
// Combinações válidas (situação BCA + situação local):
//   NAO_PUBLICADO + RASCUNHO -> MINUTA -> EM_REVISAO -> EM_PUBLICACAO (1ª publicação)
//   NAO_PUBLICADO + CANCELADO
//   PUBLICADO     + SEM_ETAPA                                          (em vigor, parado)
//   PUBLICADO     + EM_ALTERACAO -> EM_REVISAO -> EM_PUBLICACAO         (alteração em curso)
//   PUBLICADO     + ANALISE_REVOGACAO -> EM_REVOGACAO                   (revogação em curso)
//   REVOGADO      + SEM_ETAPA
public enum SituacaoLocalEnum {
    RASCUNHO,
    MINUTA,
    // Atribuído a uma pessoa específica com papel APROV (Documento.revisorAtribuido),
    // escolhida por quem enviou (EDIT) -- ver DocumentoStatusService. Alcançável a
    // partir de MINUTA (1ª publicação) ou EM_ALTERACAO (alteração); só a pessoa
    // atribuída decide o próximo passo (aprovar ou devolver), e só ela pode editar o
    // documento enquanto ele estiver aqui.
    EM_REVISAO,
    // Aprovado e aguardando o registro da portaria/BCA -- aprovar cascateia direto para
    // cá (não existem estados "aprovado"/"alterado" parados). Atribuído a uma pessoa
    // específica com papel PUBLIC (Documento.publicadorAtribuido), escolhida pelo APROV
    // no mesmo ato de aprovar. O conteúdo está congelado: ninguém mais edita. Publicar
    // (registrar portaria/BCA) leva a SEM_ETAPA e atualiza a situação BCA.
    EM_PUBLICACAO,
    // Alteração de um ato já PUBLICADO em elaboração: o texto vigente continua sendo o
    // publicado até o registro da portaria/BCA da alteração.
    EM_ALTERACAO,
    // Pedido de revogação em análise pelo APROV atribuído (revisorAtribuido). Devolver
    // volta a SEM_ETAPA: o documento nunca deixou de ser PUBLICADO.
    ANALISE_REVOGACAO,
    // Revogação aprovada, aguardando a portaria/BCA de revogação (publicador). A única
    // saída é REVOGADO (situação BCA): não há devolução.
    EM_REVOGACAO,
    // Só para quem nunca foi publicado (RASCUNHO/MINUTA).
    CANCELADO,
    // Nenhuma etapa interna em curso: o documento está parado na sua situação BCA.
    SEM_ETAPA
}
