package intraer.fablegis.domain.handlers.exceptions;

public class ResourceCannotBeUpdatedException extends RuntimeException{
    public ResourceCannotBeUpdatedException(String message){
        super(message);
    }
}
