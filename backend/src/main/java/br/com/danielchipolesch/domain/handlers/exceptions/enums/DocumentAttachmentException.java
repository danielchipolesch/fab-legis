package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentAttachmentException {

    NOT_FOUND("Anexo não encontrado"),
    ALREADY_EXISTS("Anexo já existe");

    private final String message;

    DocumentAttachmentException(String message) {
        this.message = message;
    }
}
