package intraer.fablegis.domain.entities.estruturaDocumento;

// Situação Local: a etapa INTERNA em que o documento está neste sistema. Não existe na
// vida real (o repositório oficial não conhece "minuta"), então NUNCA substitui a
// situação real -- os dois aparecem juntos (ver SituacaoBcaEnum). Um documento tem no
// máximo uma etapa local por vez: não se inicia uma revogação durante uma alteração.
//
// Combinações válidas (situação BCA + situação local):
//   NAO_PUBLICADO + RASCUNHO -> MINUTA -> EM_PUBLICACAO (1ª publicação)
//                              (a MINUTA devolvida pela autoridade volta a RASCUNHO)
//   NAO_PUBLICADO + CANCELADO   (só sai de RASCUNHO)
//   NAO_PUBLICADO + ARQUIVADO   (só sai de MINUTA, por quem aprova, com justificativa)
//   PUBLICADO     + SEM_ETAPA                                          (em vigor, parado)
//   PUBLICADO     + EM_ALTERACAO -> MINUTA -> EM_PUBLICACAO             (alteração em curso)
//   PUBLICADO     + ANALISE_REVOGACAO -> EM_REVOGACAO                   (revogação em curso)
//   REVOGADO      + SEM_ETAPA
public enum SituacaoLocalEnum {
    // O documento em elaboração: o autor e os coautores o editam à vontade, sem mudar de etapa. Só ele pode ser cancelado ou
    // excluído -- para excluir um documento que já foi para aprovação, a autoridade o devolve aos autores (volta a RASCUNHO).
    RASCUNHO,
    // O documento em APROVAÇÃO ou em conferência: foi enviado pelos autores e espera a decisão de qualquer Aprovador da OM
    // (ninguém é escolhido), que é quem o edita nesta etapa -- nem o autor edita. A autoridade aprova (EM_PUBLICACAO), devolve
    // aos autores (RASCUNHO; EM_ALTERACAO se o ato já é PUBLICADO) ou arquiva, com justificativa (ARQUIVADO, só se nunca publicado).
    MINUTA,
    // Aprovado e aguardando o registro da portaria/BCA -- aprovar cascateia direto para
    // cá (não existem estados "aprovado"/"alterado" parados). Qualquer publicador da espécie
    // pode publicar. O conteúdo está congelado: ninguém mais edita. Publicar
    // (registrar portaria/BCA) leva a SEM_ETAPA e atualiza a situação BCA.
    EM_PUBLICACAO,
    // Alteração de um ato já PUBLICADO em elaboração: o texto vigente continua sendo o
    // publicado até o registro da portaria/BCA da alteração.
    EM_ALTERACAO,
    // Pedido de revogação em análise por qualquer Aprovador da OM. Devolver
    // volta a SEM_ETAPA: o documento nunca deixou de ser PUBLICADO.
    ANALISE_REVOGACAO,
    // Revogação aprovada, aguardando a portaria/BCA de revogação (publicador). A única
    // saída é REVOGADO (situação BCA): não há devolução.
    EM_REVOGACAO,
    // Documento em RASCUNHO cancelado pelos autores. Só para quem nunca foi publicado.
    CANCELADO,
    // Documento arquivado pela autoridade (quem aprova), a partir da MINUTA, com justificativa registrada (Documento.justificativaDoArquivamento).
    // Só para quem nunca foi publicado; é um fim, como o cancelamento, mas decidido por quem aprova e não pelos autores.
    ARQUIVADO,
    // Nenhuma etapa interna em curso: o documento está parado na sua situação BCA.
    SEM_ETAPA;

    // Em quais etapas o documento aceita edição do que o compõe -- metadados (DocumentoService.update), estrutura, texto e anexos
    // (AnexoService). É a barreira por ETAPA, sem saber quem pede; quem pode pedir, e com que regra em cada etapa, é
    // DocumentoAcessoService.podeEditar (que usa esta e é a checagem que os controllers aplicam). A MINUTA entra porque quem aprova
    // edita nela; de EM_PUBLICACAO em diante (e em todo o fluxo de revogação) o conteúdo está congelado: ninguém edita, nem o autor.
    public boolean aceitaEdicao() {
        return this == RASCUNHO || this == MINUTA || this == EM_ALTERACAO;
    }
}
