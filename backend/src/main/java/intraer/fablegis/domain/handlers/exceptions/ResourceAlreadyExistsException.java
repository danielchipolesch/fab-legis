package intraer.fablegis.domain.handlers.exceptions;

public class ResourceAlreadyExistsException extends  RuntimeException{
    public ResourceAlreadyExistsException(String message){
        super(message);
    }
}
