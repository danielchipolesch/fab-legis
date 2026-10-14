package intraer.fablegis.domain.handlers.exceptions;

// A pessoa está autenticada, mas a regra de negócio não lhe permite a operação (403 com a mensagem da regra), por exemplo:
// criar um documento de espécie para a qual a sua OM não está habilitada.
public class OperacaoNaoAutorizadaException extends RuntimeException {
    public OperacaoNaoAutorizadaException(String message) {
        super(message);
    }
}
