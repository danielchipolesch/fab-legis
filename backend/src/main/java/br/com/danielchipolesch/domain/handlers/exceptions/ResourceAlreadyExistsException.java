package br.com.danielchipolesch.domain.handlers.exceptions;

public class ResourceAlreadyExistsException extends  RuntimeException{
    public ResourceAlreadyExistsException(String message){
        super(message);
    }
}
