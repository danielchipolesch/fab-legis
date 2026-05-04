package br.com.danielchipolesch.infrastructure.enums;

import lombok.Getter;

@Getter
public enum DocumentHeaderEnum {

    MD("MINISTÉRIO DA DEFESA"),
    COMAER("COMANDO DA AERONÁUTICA");

    private final String description;

    DocumentHeaderEnum(String description) {
        this.description = description;
    }
}
