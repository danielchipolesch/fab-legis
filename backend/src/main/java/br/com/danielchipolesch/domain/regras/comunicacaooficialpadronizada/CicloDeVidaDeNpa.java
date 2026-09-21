package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.regras.AcaoDeEtapa;
import br.com.danielchipolesch.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;

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
}
