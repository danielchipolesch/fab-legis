package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentoException {

    NOT_FOUND("Documento não encontrado."),
    CANNOT_BE_UPDATED("Documento não está em MINUTA ou RASCUNHO."),
    CANNOT_BE_DELETED("Apenas documentos em RASCUNHO ou MINUTA podem ser excluídos."),
    APROVADO ("Documento está aprovado e não pode ser alterado."),
    DOCUMENT_ACT_APROVADO("Para inserir um ato normativo, o documento precisa estar na situação aprovado.");


    private final String message;

    DocumentoException(String message) {
        this.message = message;
    }
}
