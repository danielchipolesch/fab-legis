package intraer.fablegis.domain.handlers.exceptions;

// Uma regra do domínio impediu a operação pedida (ex.: "elemento revogado é permanente", "justificativa obrigatória",
// "documento publicado só muda por emenda") ou o pedido traz um valor que não existe (ex.: seção desconhecida). Erro de quem
// pediu, não do servidor: o GlobalExceptionHandler responde 400 com a mensagem. Antes essas regras eram lançadas como
// IllegalStateException/IllegalArgumentException, que o handler genérico transformava em 400 sem distinguir de um bug.
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String message) {
        super(message);
    }
}
