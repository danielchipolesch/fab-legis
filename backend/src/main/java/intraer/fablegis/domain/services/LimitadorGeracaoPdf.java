package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.SistemaOcupadoException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// Limita quantos PDFs são renderizados AO MESMO TEMPO nesta instância. O Apache FOP monta o
// documento inteiro em memória e consome CPU: sem limite, uma rajada de visualizações ao vivo
// (versão em tramitação) ou de publicações derruba o backend. Semaphore e não um pool de threads:
// o trabalho já roda numa thread do Spring, e uma fila de pool sem limite anularia a proteção.
//
// Fila justa (FIFO) e espera máxima: passada a espera, lança SistemaOcupadoException (503 +
// Retry-After) em vez de segurar a requisição indefinidamente. O limite vale por instância -- com
// várias réplicas, cada uma se protege sozinha e o total escala com o número de réplicas.
@Component
public class LimitadorGeracaoPdf {

    private final Semaphore vagas;
    private final int maxConcorrentes;
    private final Duration esperaMaxima;

    @Autowired
    public LimitadorGeracaoPdf(
            @Value("${app.pdf.max-concorrentes:2}") int maxConcorrentes,
            @Value("${app.pdf.espera-maxima-segundos:30}") long esperaMaximaSegundos,
            MeterRegistry metricas) {
        this(maxConcorrentes, Duration.ofSeconds(esperaMaximaSegundos));
        Gauge.builder("fab.pdf.geracoes.em.andamento", this, LimitadorGeracaoPdf::emAndamento).register(metricas);
        Gauge.builder("fab.pdf.geracoes.aguardando", this, LimitadorGeracaoPdf::aguardando).register(metricas);
    }

    // Sem métricas (testes unitários).
    public LimitadorGeracaoPdf(int maxConcorrentes, Duration esperaMaxima) {
        if (maxConcorrentes < 1) {
            throw new IllegalArgumentException("app.pdf.max-concorrentes deve ser pelo menos 1.");
        }
        this.maxConcorrentes = maxConcorrentes;
        this.esperaMaxima = esperaMaxima;
        this.vagas = new Semaphore(maxConcorrentes, true);
    }

    public int emAndamento() {
        return maxConcorrentes - vagas.availablePermits();
    }

    public int aguardando() {
        return vagas.getQueueLength();
    }

    // Geração com a espera padrão -- pedidos de usuário (alguém está olhando a tela).
    public <T> T executar(Supplier<T> geracao) {
        return executar(geracao, esperaMaxima);
    }

    // Espera própria -- ex.: a geração armazenada depois de aprovar/publicar não tem ninguém
    // esperando o resultado e pode aguardar mais.
    public <T> T executar(Supplier<T> geracao, Duration espera) {
        boolean obteve;
        try {
            obteve = vagas.tryAcquire(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SistemaOcupadoException("A geração do PDF foi interrompida.", 5);
        }
        if (!obteve) {
            throw new SistemaOcupadoException(
                    "O sistema está gerando outros documentos no momento. Tente novamente em instantes.", 10);
        }
        try {
            return geracao.get();
        } finally {
            vagas.release();
        }
    }
}
