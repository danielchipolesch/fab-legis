package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.SistemaOcupadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

// O número de um ato normativo (espécie + assunto básico + sequencial) é o menor número livre, escolhido pela aplicação
// (CriacaoDeEspecieConvencional) e GARANTIDO pelo banco, em duas camadas: (1) quem cria trava a numeração daquela espécie + assunto
// (trava do PostgreSQL, vale entre réplicas), então a criação simultânea espera a outra terminar em vez de escolher o mesmo número;
// (2) o índice único ux_documento_numero (migração V25) recusa o número repetido mesmo que a trava falhe por algum motivo. Esta classe é
// a rede de segurança da camada 2: se o índice recusar, a criação inteira é refeita, cada tentativa na sua própria transação (a que
// falhou no banco não serve mais), e a nova leitura já enxerga o número da outra. Ver docs/replicas.md (numeração).
@Component
public class CriacaoComNumeracaoUnica {

    private static final Logger log = LoggerFactory.getLogger(CriacaoComNumeracaoUnica.class);

    static final String INDICE_DO_NUMERO = "ux_documento_numero";
    static final int TENTATIVAS = 5;
    private static final long TENTAR_NOVAMENTE_EM_SEGUNDOS = 2;

    private final TransactionTemplate transacao;

    public CriacaoComNumeracaoUnica(PlatformTransactionManager transactionManager) {
        this.transacao = new TransactionTemplate(transactionManager);
    }

    // Roda a criação (que lê os números existentes e grava o documento) numa transação; se o número escolhido já foi tomado por
    // outra criação, refaz tudo, até TENTATIVAS vezes. Qualquer outro erro sobe sem nova tentativa.
    public <T> T executar(Supplier<T> criacao) {
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                return transacao.execute(status -> criacao.get());
            } catch (DataIntegrityViolationException e) {
                if (!ehConflitoDeNumero(e)) throw e;
                log.warn("Número do documento já tomado por outra criação simultânea (tentativa {} de {}): escolhendo outro.",
                        tentativa, TENTATIVAS);
            }
        }
        throw new SistemaOcupadoException(
                "Muitos documentos estão sendo criados ao mesmo tempo. Tente novamente em instantes.", TENTAR_NOVAMENTE_EM_SEGUNDOS);
    }

    // A violação é do índice do número (e não de outra restrição, como a de um dado nulo): o nome do índice vem na mensagem do PostgreSQL.
    static boolean ehConflitoDeNumero(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            String mensagem = atual.getMessage();
            if (mensagem != null && mensagem.contains(INDICE_DO_NUMERO)) return true;
        }
        return false;
    }
}
