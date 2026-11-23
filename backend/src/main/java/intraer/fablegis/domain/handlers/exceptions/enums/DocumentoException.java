package intraer.fablegis.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentoException {

    NOT_FOUND("Documento não encontrado."),
    CANNOT_BE_UPDATED("O documento não está numa etapa em que possa ser editado."),
    CANNOT_BE_DELETED("Apenas documentos em RASCUNHO podem ser excluídos: se já foi para aprovação, a autoridade precisa devolvê-lo aos autores."),
    CANNOT_BE_REPLACED("Só uma NPA publicada, sem etapa em curso (nem pedido de revogação), pode ser substituída."),
    ANEXOS_CANNOT_BE_UPDATED("Os anexos só podem ser alterados enquanto o documento está em edição "
            + "(Rascunho, Minuta, Em Alteração ou Em Revisão)."),
    APROVADO ("Documento está aprovado e não pode ser alterado."),
    DOCUMENT_ACT_APROVADO("Para inserir um ato normativo, o documento precisa estar na situação aprovado.");


    private final String message;

    DocumentoException(String message) {
        this.message = message;
    }
}
