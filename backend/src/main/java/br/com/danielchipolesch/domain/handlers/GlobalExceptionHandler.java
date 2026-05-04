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
}
