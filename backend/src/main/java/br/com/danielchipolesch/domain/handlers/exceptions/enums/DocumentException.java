package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentException {

    NOT_FOUND("Documento não encontrado."),
    CANNOT_BE_UPDATED("Documento não está em MINUTA ou RASCUNHO."),
    APROVADO ("Documentos está aprovado e não pode ser alterado."),
    DOCUMENT_ACT_APROVADO("Para inserir um ato normativo, o documento precisa estar aprovado.");


    private final String message;

    DocumentException(String message) {
        this.message = message;
    }
}
