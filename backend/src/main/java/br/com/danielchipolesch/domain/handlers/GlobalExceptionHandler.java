package br.com.danielchipolesch.domain.handlers;

import br.com.danielchipolesch.domain.handlers.exceptions.*;
import br.com.danielchipolesch.domain.handlers.exceptions.utils.ExceptionResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Content-Type sempre explícito (nunca deixado pra negociação por Accept
    // header): sem isso, um erro disparado dentro de um endpoint SSE (Accept:
    // text/event-stream, ex.: /v1/documentos/{id}/presenca/stream negado por
    // @PreAuthorize) faz o próprio handler de erro falhar com
    // HttpMediaTypeNotAcceptableException ao tentar negociar um Map como
    // text/event-stream -- o erro real (403) nunca chega ao cliente.
    private static ResponseEntity<Map<String, Object>> responder(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
        return responder(HttpStatus.BAD_REQUEST, body);
    }

    // Negação de @PreAuthorize (ex.: DocumentoAcessoService.podeEditar retornando
    // false) é lançada de dentro do método do controller, então chega aqui via
    // Spring MVC -- não pelo filtro de segurança -- e cairia no handler genérico
    // acima (400) sem este handler específico e mais concreto.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, "Acesso negado.", request);
        return responder(HttpStatus.FORBIDDEN, body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
        return responder(HttpStatus.NOT_FOUND, body);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(ResourceAlreadyExistsException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
        return responder(HttpStatus.CONFLICT, body);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInput(InvalidInputException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, e.getMessage(), request);
        return responder(HttpStatus.NOT_ACCEPTABLE, body);
    }

    @ExceptionHandler(ResourceCannotBeUpdatedException.class)
    public ResponseEntity<Map<String, Object>> handleResourceCannotBeUpdatedException(ResourceCannotBeUpdatedException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), request);
        return responder(HttpStatus.FORBIDDEN, body);
    }

    @ExceptionHandler(StatusCannotBeUpdatedException.class)
    public ResponseEntity<Map<String, Object>> handleStatusCannotBeUpdatedException(StatusCannotBeUpdatedException e, WebRequest request){
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), request);
        return responder(HttpStatus.FORBIDDEN, body);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredenciaisInvalidas(CredenciaisInvalidasException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
        return responder(HttpStatus.UNAUTHORIZED, body);
    }

    @ExceptionHandler(ConflitoEdicaoException.class)
    public ResponseEntity<Map<String, Object>> handleConflitoEdicao(ConflitoEdicaoException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request);
        return responder(HttpStatus.CONFLICT, body);
    }
}
