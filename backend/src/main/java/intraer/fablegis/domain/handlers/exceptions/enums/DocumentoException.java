package intraer.fablegis.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentoException {

    NOT_FOUND("Documento não encontrado."),
    CANNOT_BE_UPDATED("Documento não está em MINUTA ou RASCUNHO."),
    CANNOT_BE_DELETED("Apenas documentos em RASCUNHO ou MINUTA podem ser excluídos."),
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
