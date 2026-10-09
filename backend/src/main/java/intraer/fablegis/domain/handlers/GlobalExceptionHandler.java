package intraer.fablegis.domain.handlers;

import intraer.fablegis.domain.handlers.exceptions.*;
import intraer.fablegis.domain.handlers.exceptions.utils.ExceptionResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

// Todo erro da API sai como JSON no mesmo formato ({timestamp, status, error, message, path}), com o status certo:
//   - exceções do domínio (pacote exceptions): cada uma tem o seu handler abaixo;
//   - erros do próprio Spring MVC (corpo ilegível, validação, parâmetro ausente, rota inexistente, método ou tipo de conteúdo
//     não suportado, upload grande demais...): o ResponseEntityExceptionHandler decide o status (400/404/405/413/415...) e
//     handleExceptionInternal os escreve no nosso formato, em português e sem texto interno do framework;
//   - pedidos mal formados que só estouram mais adiante (página negativa, ordenação por propriedade que não existe, valor que
//     viola uma restrição do banco): 400/409, é erro de quem pediu;
//   - o que sobrar é falha NOSSA: 500, registrada no log com a pilha, e uma mensagem neutra a quem pediu. (Antes, tudo isso
//     virava 400 -- inclusive bug de verdade, sem deixar rastro no log.)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String MENSAGEM_ERRO_INTERNO = "Erro interno do servidor. Tente novamente; se o problema persistir, avise o suporte.";

    // Content-Type sempre explícito (nunca deixado pra negociação por Accept
    // header): sem isso, um erro disparado dentro de um endpoint SSE (Accept:
    // text/event-stream, ex.: /v1/documentos/{id}/presenca/stream negado por
    // @PreAuthorize) faz o próprio handler de erro falhar com
    // HttpMediaTypeNotAcceptableException ao tentar negociar um Map como
    // text/event-stream -- o erro real (403) nunca chega ao cliente.
    private static ResponseEntity<Map<String, Object>> responder(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static ResponseEntity<Map<String, Object>> responder(HttpStatus status, String mensagem, WebRequest request) {
        return responder(status, ExceptionResponseUtil.buildErrorResponse(status, mensagem, request));
    }

    // ─── Erros do próprio Spring MVC ──────────────────────────────────────────────────────────────

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        if (status.is5xxServerError()) {
            log.error("Erro do framework em {}", request.getDescription(false), ex);
        }
        Map<String, Object> corpo = ExceptionResponseUtil.buildErrorResponse(status, mensagemDoFramework(ex, status), request);
        // Os headers do Spring (Allow no 405, Accept no 415...) são mantidos; o Content-Type é sempre o nosso (ver responder).
        return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }

    private static String mensagemDoFramework(Exception ex, HttpStatus status) {
        return switch (ex) {
            case MethodArgumentNotValidException e -> "Dados inválidos: " + camposInvalidos(e.getBindingResult());
            case BindException e -> "Dados inválidos: " + camposInvalidos(e.getBindingResult());
            case HandlerMethodValidationException ignored -> "Dados inválidos.";
            case HttpMessageNotReadableException ignored -> "O corpo da requisição está ilegível ou em formato inválido.";
            case MissingServletRequestParameterException e -> "Parâmetro obrigatório ausente: " + e.getParameterName() + ".";
            case MissingServletRequestPartException e -> "Parte obrigatória ausente: " + e.getRequestPartName() + ".";
            case TypeMismatchException e -> "Valor inválido para o parâmetro '" + e.getPropertyName() + "'.";
            case HttpRequestMethodNotSupportedException e -> "O método " + e.getMethod() + " não é suportado nesta rota.";
            case HttpMediaTypeNotSupportedException ignored -> "Tipo de conteúdo não suportado.";
            case NoResourceFoundException ignored -> "Rota não encontrada.";
            case NoHandlerFoundException ignored -> "Rota não encontrada.";
            case MaxUploadSizeExceededException ignored -> "O arquivo enviado é maior que o limite permitido.";
            default -> status.is5xxServerError() ? MENSAGEM_ERRO_INTERNO : "Requisição inválida.";
        };
    }

    private static String camposInvalidos(BindingResult resultado) {
        String campos = resultado.getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return campos.isBlank() ? "verifique os campos enviados." : campos;
    }

    // ─── Pedido mal formado que só estoura mais adiante: erro de quem pediu ───────────────────────

    // Parâmetro fora do intervalo aceito (página negativa, tamanho zero...) e afins: IllegalArgumentException é, por convenção,
    // "argumento inválido". Sempre foi 400 aqui; a mensagem segue a de quem lançou.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentoInvalido(IllegalArgumentException e, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // Regra do domínio violada (RegraDeNegocioException): 400, com a mensagem da regra.
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleRegraDeNegocio(RegraDeNegocioException e, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // Ordenar por uma propriedade que não existe (?sortBy=xyz) é erro de quem pediu; qualquer outro mau uso da camada de dados
    // é nosso.
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<Map<String, Object>> handleUsoDeDados(Exception e, WebRequest request) {
        if (e instanceof PropertyReferenceException || temCausa(e, PropertyReferenceException.class)) {
            return responder(HttpStatus.BAD_REQUEST, "Propriedade de ordenação ou filtro inválida.", request);
        }
        return handleInesperada(e, request);
    }

    // Violação de restrição do banco: valor grande demais ou nulo onde não pode (400, quem pediu mandou o dado errado) ou
    // duplicado / referência que não existe (409, o dado conflita com o que já está gravado).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridadeDeDados(DataIntegrityViolationException e, WebRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getDescription(false), e.getMostSpecificCause().getMessage());
        String estado = e.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        boolean dadoInvalido = "22001".equals(estado) || "23502".equals(estado) || "22003".equals(estado); // grande demais / nulo / fora do intervalo
        if (dadoInvalido) {
            return responder(HttpStatus.BAD_REQUEST, "Algum valor enviado é inválido: grande demais ou ausente.", request);
        }
        return responder(HttpStatus.CONFLICT,
                "Os dados enviados conflitam com o que já está gravado (valor duplicado ou referência inexistente).", request);
    }

    // Duas pessoas gravando o mesmo documento ao mesmo tempo (@Version): o front recarrega ao receber 409 (ver ConflitoEdicaoException).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConflitoOtimista(ObjectOptimisticLockingFailureException e, WebRequest request) {
        return responder(HttpStatus.CONFLICT,
                "Este documento foi alterado por outro usuário desde que você o abriu. Recarregue antes de salvar.", request);
    }

    private static boolean temCausa(Throwable e, Class<? extends Throwable> tipo) {
        for (Throwable atual = e; atual != null; atual = atual.getCause()) {
            if (tipo.isInstance(atual)) return true;
        }
        return false;
    }

    // ─── Falha nossa ──────────────────────────────────────────────────────────────────────────────

    // O que nenhum handler reconhece é bug ou falha de infraestrutura: 500, com a pilha no log e uma mensagem neutra (o detalhe
    // interno não vai para quem pediu). Cliente que desconectou no meio de uma resposta longa (SSE, PDF em streaming) não é
    // erro: não há a quem responder.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInesperada(Exception e, WebRequest request) {
        if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
            log.debug("Cliente desconectou em {}", request.getDescription(false));
            return null;
        }
        log.error("Erro inesperado em {}", request.getDescription(false), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_ERRO_INTERNO, request);
    }

    // ─── Exceções do domínio ──────────────────────────────────────────────────────────────────────

    // Negação de @PreAuthorize (ex.: DocumentoAcessoService.podeEditar retornando
    // false) é lançada de dentro do método do controller, então chega aqui via
    // Spring MVC -- não pelo filtro de segurança -- e cairia no handler genérico
    // (500) sem este handler específico e mais concreto.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, "Acesso negado.", request);
        return responder(HttpStatus.FORBIDDEN, body);
    }

    // Todas as vagas de geração de PDF ocupadas (LimitadorGeracaoPdf): condição passageira.
    @ExceptionHandler(SistemaOcupadoException.class)
    public ResponseEntity<Map<String, Object>> handleSistemaOcupado(SistemaOcupadoException e, WebRequest request) {
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(e.getTentarNovamenteEmSegundos()))
                .contentType(MediaType.APPLICATION_JSON).body(body);
    }

    @ExceptionHandler(FalhaNaRenderizacaoException.class)
    public ResponseEntity<Map<String, Object>> handleFalhaNaRenderizacao(FalhaNaRenderizacaoException e, WebRequest request) {
        log.error("Falha de renderização em {}", request.getDescription(false), e);
        Map<String, Object> body = ExceptionResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, body);
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
