package br.com.danielchipolesch.domain.handlers.exceptions;

public class StatusCannotBeUpdatedException extends RuntimeException{
    public StatusCannotBeUpdatedException(String message) {
        super(message);
    }
}
