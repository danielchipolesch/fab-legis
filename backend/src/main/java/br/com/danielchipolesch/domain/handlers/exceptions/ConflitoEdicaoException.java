package br.com.danielchipolesch.domain.handlers.exceptions;

// Lançada quando o cliente tenta salvar uma alteração baseada numa versão do
// documento que não é mais a mais recente (outro usuário salvou primeiro) --
// ver DocumentoConcorrenciaService.
public class ConflitoEdicaoException extends RuntimeException {
    public ConflitoEdicaoException(String message) {
        super(message);
    }
}
