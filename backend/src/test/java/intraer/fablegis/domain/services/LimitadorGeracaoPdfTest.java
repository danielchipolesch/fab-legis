package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.SistemaOcupadoException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Limite de renderizações simultâneas do FOP (docs/exportacao-pdf.md): nunca mais que N ao mesmo
// tempo, espera máxima com 503 (SistemaOcupadoException) e a vaga é sempre devolvida.
class LimitadorGeracaoPdfTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Test
    void devolveOResultadoDaGeracao() {
        var limitador = new LimitadorGeracaoPdf(2, ESPERA);

        assertThat(limitador.executar(() -> "pdf")).isEqualTo("pdf");
    }

    @Test
    void nuncaExecutaMaisQueOLimiteAoMesmoTempo() throws Exception {
        var limitador = new LimitadorGeracaoPdf(2, ESPERA);
        var emAndamento = new AtomicInteger();
        var pico = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                resultados.add(pool.submit(() -> limitador.executar(() -> {
                    int agora = emAndamento.incrementAndGet();
                    pico.accumulateAndGet(agora, Math::max);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    emAndamento.decrementAndGet();
                    return 1;
                })));
            }
            for (var r : resultados) r.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(pico.get()).isLessThanOrEqualTo(2);
        assertThat(pico.get()).isGreaterThanOrEqualTo(1);
        assertThat(limitador.emAndamento()).isZero();
    }

    @Test
    void comTodasAsVagasOcupadasEEsperaEsgotadaLancaSistemaOcupado() throws Exception {
        var limitador = new LimitadorGeracaoPdf(1, Duration.ofMillis(100));
        var ocupou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> limitador.executar(() -> {
                ocupou.countDown();
                try {
                    liberar.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertThat(ocupou.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(limitador.emAndamento()).isEqualTo(1);

            assertThatThrownBy(() -> limitador.executar(() -> "nunca"))
                    .isInstanceOf(SistemaOcupadoException.class)
                    .hasMessageContaining("Tente novamente")
                    .extracting(e -> ((SistemaOcupadoException) e).getTentarNovamenteEmSegundos())
                    .isEqualTo(10L);
        } finally {
            liberar.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void aVagaEDevolvidaMesmoQuandoAGeracaoFalha() {
        var limitador = new LimitadorGeracaoPdf(1, Duration.ofMillis(100));

        assertThatThrownBy(() -> limitador.executar(() -> { throw new IllegalStateException("FOP falhou"); }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(limitador.emAndamento()).isZero();
        assertThat(limitador.executar(() -> "ok")).isEqualTo("ok");
    }

    @Test
    void aEsperaPodeSerDiferenteParaAGeracaoArmazenada() throws Exception {
        var limitador = new LimitadorGeracaoPdf(1, Duration.ofMillis(50));
        var ocupou = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> limitador.executar(() -> {
                ocupou.countDown();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertThat(ocupou.await(5, TimeUnit.SECONDS)).isTrue();

            // Com a espera longa, aguarda a vaga liberar em vez de desistir.
            assertThat(limitador.executar(() -> "armazenado", Duration.ofSeconds(5))).isEqualTo("armazenado");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void oLimiteMinimoEUmaVaga() {
        assertThatThrownBy(() -> new LimitadorGeracaoPdf(0, ESPERA)).isInstanceOf(IllegalArgumentException.class);
    }
}
