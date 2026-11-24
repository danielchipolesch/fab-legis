package intraer.fablegis.infrastructure.notificacao;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

// Avisos ao vivo (SSE): o frontend se atualiza sem recarregar a página (docs/autenticacao.md, "Atualização ao vivo"). Os avisos vão pela
// conexão de notificações de cada usuário e só DEPOIS do commit; o evento diz "mudou", não carrega o dado.
@ExtendWith(MockitoExtension.class)
class AvisosAoVivoTest {

    @Mock NotificacaoEmitterRegistry registro;

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    // ─── Documento ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void documentoQueMudouVaiParaTodosOsConectadosComAEtapaNova() {
        var doc = new Documento();
        doc.setId(42L);
        doc.setSituacaoLocal(SituacaoLocalEnum.MINUTA);
        doc.setSituacaoBca(SituacaoBcaEnum.NAO_PUBLICADO);

        new AvisosAoVivo(registro).documentoMudou(doc);

        var dados = ArgumentCaptor.forClass(Object.class);
        verify(registro).enviarATodosAposCommit(org.mockito.ArgumentMatchers.eq("documento"), dados.capture());
        assertThat((Map<String, Object>) dados.getValue())
                .containsEntry("id", 42L).containsEntry("situacaoLocal", "MINUTA").containsEntry("situacaoBca", "NAO_PUBLICADO");
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentoExcluidoVaiSemEtapa() {
        new AvisosAoVivo(registro).documentoMudou(42L, null, null);

        var dados = ArgumentCaptor.forClass(Object.class);
        verify(registro).enviarATodosAposCommit(org.mockito.ArgumentMatchers.eq("documento"), dados.capture());
        assertThat((Map<String, Object>) dados.getValue()).containsEntry("id", 42L).containsEntry("situacaoLocal", null);
    }

    // ─── Acesso (só para a pessoa) e habilitação (para as OMs afetadas) ───────────────────────────────

    @Test
    void acessoMudouVaiSoParaAPessoaEDizSeEstaAtiva() {
        var avisos = new AvisosAoVivo(registro);

        avisos.acessoMudou(7L, true);
        avisos.acessoMudou(8L, false);

        verify(registro).enviarEventoAposCommit(List.of(7L), "acesso", Map.of("ativo", true));
        verify(registro).enviarEventoAposCommit(List.of(8L), "acesso", Map.of("ativo", false));
    }

    @Test
    void habilitacaoMudouVaiParaQuemEDasOmsAfetadas() {
        new AvisosAoVivo(registro).habilitacaoMudou(List.of(1L, 2L));

        verify(registro).enviarEventoAposCommit(List.of(1L, 2L), "habilitacao", Map.of());
    }

    // ─── Só depois do commit ─────────────────────────────────────────────────

    @Test
    void comTransacaoEmCursoOEnvioFicaParaDepoisDoCommit() {
        var real = new NotificacaoEmitterRegistry();
        real.registrar(1L);
        TransactionSynchronizationManager.initSynchronization();

        real.enviarATodosAposCommit("documento", Map.of("id", 1L));
        real.enviarEventoAposCommit(List.of(1L), "acesso", Map.of("ativo", true));

        // nada foi enviado ainda: dois envios estão agendados para o afterCommit (um rollback os descarta)
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(2);
    }

    @Test
    void semUsuarioParaAvisarNaoAgendaNada() {
        var real = new NotificacaoEmitterRegistry();
        TransactionSynchronizationManager.initSynchronization();

        real.enviarEventoAposCommit(List.of(), "habilitacao", Map.of());

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }
}
