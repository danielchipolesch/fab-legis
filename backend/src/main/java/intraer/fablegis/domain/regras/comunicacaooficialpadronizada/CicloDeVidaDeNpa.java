package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.regras.AcaoDeEtapa;
import intraer.fablegis.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;

// Ciclo de vida da NPA: elaboração, revisão, publicação (no Boletim Interno) e revogação -- SEM alteração. Para mudar
// uma NPA publicada cria-se outra e revoga-se a anterior, então não existem INICIAR_ALTERACAO, CANCELAR_ALTERACAO
// nem a etapa EM_ALTERACAO. Devolver leva sempre à minuta (nunca há alteração em curso para onde voltar).
@Component
public class CicloDeVidaDeNpa implements RegrasDoCicloDeVidaDoDocumento {

    @Override
    public Optional<AcaoDeEtapa> acaoPara(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca) {
        return Optional.ofNullable(switch (atual) {
            case RASCUNHO          -> destino == MINUTA ? AcaoDeEtapa.MINUTAR
                                    : destino == CANCELADO ? AcaoDeEtapa.CANCELAR_DOCUMENTO : null;
            case MINUTA            -> destino == EM_REVISAO ? AcaoDeEtapa.ENVIAR_PARA_REVISAO
                                    : destino == CANCELADO ? AcaoDeEtapa.CANCELAR_DOCUMENTO : null;
            case EM_REVISAO        -> destino == EM_PUBLICACAO ? AcaoDeEtapa.APROVAR
                                    : destino == MINUTA ? AcaoDeEtapa.DEVOLVER : null;
            case EM_PUBLICACAO     -> destino == SEM_ETAPA ? AcaoDeEtapa.PUBLICAR
                                    : destino == MINUTA ? AcaoDeEtapa.DEVOLVER : null;
            case ANALISE_REVOGACAO -> destino == EM_REVOGACAO ? AcaoDeEtapa.APROVAR_REVOGACAO
                                    : destino == SEM_ETAPA ? AcaoDeEtapa.DEVOLVER_ANALISE : null;
            case EM_REVOGACAO      -> destino == SEM_ETAPA ? AcaoDeEtapa.REVOGAR : null;
            case SEM_ETAPA         -> bca == SituacaoBcaEnum.PUBLICADO && destino == ANALISE_REVOGACAO
                                    ? AcaoDeEtapa.PEDIR_REVOGACAO : null;
            case EM_ALTERACAO, CANCELADO -> null;
        });
    }

    // Só uma NPA já publicada e parada (sem etapa em curso: nem pedido de revogação, nem revogação) pode ser substituída.
    // Já revogada, não: não há mais o que substituir. Em elaboração, edita-se a própria NPA.
    @Override
    public boolean podeSerSubstituido(SituacaoLocalEnum atual, SituacaoBcaEnum bca) {
        return atual == SEM_ETAPA && bca == SituacaoBcaEnum.PUBLICADO;
    }

    // Quem publica e revoga: a própria OM, no Boletim Interno (não o CENDOC).
    @Override
    public PapelEnum papelQuePublica() {
        return PapelEnum.PUB_BI;
    }
}
