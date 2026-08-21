package br.com.danielchipolesch.domain.handlers;

import br.com.danielchipolesch.domain.handlers.exceptions.*;
import br.com.danielchipolesch.domain.handlers.exceptions.utils.ExceptionResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Negação de @PreAuthorize (ex.: DocumentoAcessoService.podeEditar retornando
    // false) é lançada de dentro do método do controller, então chega aqui via
    // Spring MVC -- não pelo filtro de segurança -- e cairia no handler genérico
    // acima (400) sem este handler específico e mais concreto.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, "Acesso negado.", request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(ResourceAlreadyExistsException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInput(InvalidInputException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ResourceCannotBeUpdatedException.class)
    public ResponseEntity<Map<String, Object>> handleResourceCannotBeUpdatedException(ResourceCannotBeUpdatedException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(StatusCannotBeUpdatedException.class)
    public  ResponseEntity<Map<String, Object>> handleStatusCannotBeUpdatedException(StatusCannotBeUpdatedException e, WebRequest request){
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredenciaisInvalidas(CredenciaisInvalidasException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ConflitoEdicaoException.class)
    public ResponseEntity<Map<String, Object>> handleConflitoEdicao(ConflitoEdicaoException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
}
