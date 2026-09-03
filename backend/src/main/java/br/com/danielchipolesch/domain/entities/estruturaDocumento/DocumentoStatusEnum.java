package br.com.danielchipolesch.domain.entities.estruturaDocumento;

public enum DocumentoStatusEnum {
    RASCUNHO,
    MINUTA,
    // Atribuído a uma pessoa específica com papel APROV (Documento.revisorAtribuido),
    // escolhida por quem enviou (EDIT) -- ver DocumentoStatusService. Alcançável a
    // partir de MINUTA (fluxo normal) ou EM_ALTERACAO (ciclo de emenda); só a pessoa
    // atribuída decide o próximo passo (aprovar ou devolver), e só ela pode editar o
    // documento enquanto ele estiver aqui.
    EM_REVISAO,
    APROVADO,
    // Atribuído a uma pessoa específica com papel PUBLIC (Documento.publicadorAtribuido),
    // escolhida pelo APROV no mesmo ato de aprovar -- ver DocumentoStatusService. A
    // partir daqui o documento não é mais editável por ninguém. Alcançável a partir de
    // APROVADO ou ALTERADO.
    EM_PUBLICACAO,
    PUBLICADO,
    EM_ALTERACAO,
    // Alteração aprovada, aguardando republicação. Distinto de APROVADO (que só
    // existe no fluxo normal RASCUNHO->MINUTA->...->APROVADO->PUBLICADO) justamente
    // para que a renumeração simples (sem sufixo de letra) nunca seja aplicada a um
    // documento que já passou por EM_ALTERACAO — corromperia o rastreio de emenda no
    // próximo salvamento.
    ALTERADO,
    CANCELADO,
    // Revogação em 3 etapas, mesmo padrão de atribuição pessoal do fluxo normal:
    // EDIT envia (escolhe o APROV) -> APROV aprova a revogação (escolhe o PUBLIC) ->
    // PUBLIC formaliza com portaria/BCA -> REVOGADO.
    ANALISE_REVOGACAO,
    EM_REVOGACAO,
    REVOGADO
}
