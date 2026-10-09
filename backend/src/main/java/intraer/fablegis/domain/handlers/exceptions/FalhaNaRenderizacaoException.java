package intraer.fablegis.domain.handlers.exceptions;

// A geração de um arquivo (PDF) a partir do documento falhou -- problema do servidor, não de quem pediu. O GlobalExceptionHandler a responde
// com 500 e a mensagem de causa.
public class FalhaNaRenderizacaoException extends RuntimeException {
    public FalhaNaRenderizacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
