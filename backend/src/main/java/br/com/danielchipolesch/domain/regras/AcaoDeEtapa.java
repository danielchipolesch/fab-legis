package br.com.danielchipolesch.domain.regras;

// O que um pedido de mudança de etapa significa em termos de negócio -- o mesmo destino (ex.: SEM_ETAPA)
// quer dizer coisas diferentes conforme a origem. Quais ações existem e a partir de onde é decisão do
// tipo de regras (RegrasDoCicloDeVidaDoDocumento): uma NPA não tem INICIAR_ALTERACAO nem CANCELAR_ALTERACAO.
public enum AcaoDeEtapa {
    MINUTAR,              // RASCUNHO -> MINUTA
    ENVIAR_PARA_REVISAO,  // MINUTA/EM_ALTERACAO -> EM_REVISAO (escolhe o revisor)
    APROVAR,              // EM_REVISAO -> EM_PUBLICACAO (escolhe o publicador)
    DEVOLVER,             // EM_REVISAO/EM_PUBLICACAO -> MINUTA ou EM_ALTERACAO
    PUBLICAR,             // EM_PUBLICACAO -> SEM_ETAPA (registra a publicação)
    INICIAR_ALTERACAO,    // SEM_ETAPA -> EM_ALTERACAO (documento PUBLICADO)
    CANCELAR_ALTERACAO,   // EM_ALTERACAO -> SEM_ETAPA (só sem alterações pendentes)
    PEDIR_REVOGACAO,      // SEM_ETAPA -> ANALISE_REVOGACAO (documento PUBLICADO)
    APROVAR_REVOGACAO,    // ANALISE_REVOGACAO -> EM_REVOGACAO (escolhe o publicador)
    DEVOLVER_ANALISE,     // ANALISE_REVOGACAO -> SEM_ETAPA (o documento segue PUBLICADO)
    REVOGAR,              // EM_REVOGACAO -> SEM_ETAPA (registra a publicação; BCA = REVOGADO)
    CANCELAR_DOCUMENTO    // RASCUNHO/MINUTA -> CANCELADO
}
