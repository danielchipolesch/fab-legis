package br.com.danielchipolesch.domain.entities.estruturaDocumento;

public enum DocumentoStatusEnum {
    RASCUNHO,
    MINUTA,
    APROVADO,
    PUBLICADO,
    EM_ALTERACAO,
    // Alteração aprovada, aguardando republicação. Distinto de APROVADO (que só
    // existe no fluxo normal RASCUNHO->MINUTA->APROVADO->PUBLICADO) justamente para
    // que "Retornar p/ Minuta" nunca seja oferecido/aceito para um documento que já
    // passou por EM_ALTERACAO — isso aplicaria a renumeração simples (sem sufixo de
    // letra) e corromperia o rastreio de emenda no próximo salvamento.
    ALTERADO,
    ARQUIVADO,
    CANCELADO,
    REVOGADO
}
