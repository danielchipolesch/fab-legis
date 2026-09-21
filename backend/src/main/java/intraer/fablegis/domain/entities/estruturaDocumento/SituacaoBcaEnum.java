package intraer.fablegis.domain.entities.estruturaDocumento;

// Situação BCA: a situação REAL do ato, a que espelha o repositório oficial. Este
// sistema não é o repositório oficial, mas precisa acompanhá-lo: um documento nunca
// pode aparecer aqui com uma situação diferente da oficial. Só muda quando uma
// portaria + BCA são registrados (DocumentoStatusService), nunca por uma etapa
// interna -- para as etapas, ver SituacaoLocalEnum.
public enum SituacaoBcaEnum {

    // Nenhuma portaria de edição registrada ainda (rascunho, minuta, em revisão...).
    NAO_PUBLICADO,

    // Portaria de edição registrada. Continua PUBLICADO durante uma alteração ou um
    // pedido de revogação em curso, e depois de cada alteração publicada.
    PUBLICADO,

    // Portaria de revogação registrada. Estado final.
    REVOGADO
}
