package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum RegulatoryActException {

    NOT_FOUND("Portaria não encontrada"),
    ALREADY_EXISTS("Portaria já existe");

    private final String message;

    RegulatoryActException(String message){
        this.message=message;
    }
}
