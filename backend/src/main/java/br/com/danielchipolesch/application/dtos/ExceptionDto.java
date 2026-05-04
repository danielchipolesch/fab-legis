package br.com.danielchipolesch.application.dtos;

// Classe existente apenas para padronizar no Swagger o JSON do response de exceções.
public class ExceptionDto {

    private String timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
}
