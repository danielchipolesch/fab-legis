package br.com.danielchipolesch.domain.handlers.exceptions;

// Lançada quando todas as vagas de geração de PDF estão ocupadas e a espera máxima se esgotou --
// ver LimitadorGeracaoPdf. Vira 503 com Retry-After (GlobalExceptionHandler): é uma condição
// passageira, o cliente deve tentar de novo.
public class SistemaOcupadoException extends RuntimeException {

    private final long tentarNovamenteEmSegundos;

    public SistemaOcupadoException(String message, long tentarNovamenteEmSegundos) {
        super(message);
        this.tentarNovamenteEmSegundos = tentarNovamenteEmSegundos;
    }

    public long getTentarNovamenteEmSegundos() {
        return tentarNovamenteEmSegundos;
    }
}
