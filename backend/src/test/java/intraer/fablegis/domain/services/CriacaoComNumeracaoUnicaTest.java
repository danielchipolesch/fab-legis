package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.SistemaOcupadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// O número de um documento é único no banco (índice ux_documento_numero). Quando duas criações simultâneas escolhem o mesmo,
// a segunda é recusada e a criação é refeita (cada tentativa numa transação própria), até achar um número livre.
class CriacaoComNumeracaoUnicaTest {

    private PlatformTransactionManager transacoes;
    private CriacaoComNumeracaoUnica criacao;

    @BeforeEach
    void preparar() {
        transacoes = mock(PlatformTransactionManager.class);
        when(transacoes.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        criacao = new CriacaoComNumeracaoUnica(transacoes);
    }

    private static DataIntegrityViolationException numeroJaTomado() {
        var banco = new SQLException("ERROR: duplicate key value violates unique constraint \"ux_documento_numero\"", "23505");
        return new DataIntegrityViolationException("could not execute statement", banco);
    }

    @Test
    void semConflitoCriaNaPrimeiraTentativa() {
        var chamadas = new AtomicInteger();

        String resultado = criacao.executar(() -> { chamadas.incrementAndGet(); return "DCA 11-1"; });

        assertThat(resultado).isEqualTo("DCA 11-1");
        assertThat(chamadas).hasValue(1);
        verify(transacoes, times(1)).commit(any());
    }

    @Test
    void numeroTomadoPorOutraCriacaoRefazATentativaComOSeguinte() {
        var chamadas = new AtomicInteger();

        String resultado = criacao.executar(() -> {
            int tentativa = chamadas.incrementAndGet();
            if (tentativa < 3) throw numeroJaTomado(); // as duas primeiras escolhem um número que outra criação acabou de tomar
            return "DCA 11-" + tentativa;
        });

        assertThat(resultado).isEqualTo("DCA 11-3");
        assertThat(chamadas).hasValue(3);
        // cada tentativa tem a sua transação: as que falharam desfazem (rollback) e só a última confirma
        verify(transacoes, times(2)).rollback(any());
        verify(transacoes, times(1)).commit(any());
    }

    @Test
    void desisteDepoisDeTentarVariasVezesEAvisaQueEstaOcupado() {
        var chamadas = new AtomicInteger();

        assertThatThrownBy(() -> criacao.executar(() -> { chamadas.incrementAndGet(); throw numeroJaTomado(); }))
                .isInstanceOf(SistemaOcupadoException.class)
                .hasMessageContaining("ao mesmo tempo");
        assertThat(chamadas).hasValue(CriacaoComNumeracaoUnica.TENTATIVAS);
    }

    @Test
    void outraViolacaoDoBancoNaoEhTentadaDeNovo() {
        var chamadas = new AtomicInteger();
        var outra = new DataIntegrityViolationException("x", new SQLException("null value in column \"nm_titulo_documento\"", "23502"));

        assertThatThrownBy(() -> criacao.executar(() -> { chamadas.incrementAndGet(); throw outra; })).isSameAs(outra);
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void erroDeNegocioSobeSemNovaTentativa() {
        var chamadas = new AtomicInteger();

        assertThatThrownBy(() -> criacao.executar(() -> { chamadas.incrementAndGet(); throw new InvalidInputException("Informe o assunto básico do documento."); }))
                .isInstanceOf(InvalidInputException.class);
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void reconheceOConflitoDoNumeroEmQualquerNivelDaCadeiaDeCausas() {
        assertThat(CriacaoComNumeracaoUnica.ehConflitoDeNumero(numeroJaTomado())).isTrue();
        assertThat(CriacaoComNumeracaoUnica.ehConflitoDeNumero(
                new RuntimeException("envolvido", numeroJaTomado()))).isTrue();
        assertThat(CriacaoComNumeracaoUnica.ehConflitoDeNumero(new RuntimeException("outra coisa"))).isFalse();
        assertThat(CriacaoComNumeracaoUnica.ehConflitoDeNumero(new RuntimeException((String) null))).isFalse();
    }
}
