package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum AssuntoBasicoException {

    NOT_FOUND("Assunto básico não encontrado"),
    ALREADY_EXISTS("Assunto básico já existe");

    private final String message;

    AssuntoBasicoException(String message) {
        this.message = message;
    }
}
