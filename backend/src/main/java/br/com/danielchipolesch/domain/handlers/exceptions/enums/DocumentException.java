package br.com.danielchipolesch.domain.handlers.exceptions.enums;

import lombok.Getter;

@Getter
public enum DocumentException {

    NOT_FOUND("Documento não encontrado."),
    ALREADY_EXISTS("Documento já existe."),
    CANNOT_BE_UPDATED("Documento não está em MINUTA ou RASCUNHO."),
    RASCUNHO("Para serem aprovados os documentos devem estar como rascunho ou minuta."),
    MINUTA("Para serem aprovados os documentos devem estar como rascunho ou minuta."),
    APROVADO ("Documentos está aprovado e não pode ser alterado."),
    DOCUMENT_ACT_APROVADO("Para inserir um ato normativo, o documento precisa estar aprovado."),
    PUBLICADO("Documentos publicados não podem ser alterados."),
    ARQUIVADO("Documentos arquivados não podem ser alterados."),
    CANCELADO("Documentos cancelados não podem ser alterados."),
    REVOGADO("Documento está revogado");


    private final String message;

    DocumentException(String message) {
        this.message = message;
    }
}
