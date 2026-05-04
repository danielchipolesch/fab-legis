package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum BasicSubjectException {

    NOT_FOUND("Assunto básico não encontrado"),
    ALREADY_EXISTS("Assunto básico já existe");

    private final String message;

    BasicSubjectException(String message) {
        this.message = message;
    }
}
