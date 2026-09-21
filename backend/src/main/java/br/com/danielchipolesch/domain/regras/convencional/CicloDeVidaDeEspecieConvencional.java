package br.com.danielchipolesch.domain.regras.convencional;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.regras.AcaoDeEtapa;
import br.com.danielchipolesch.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;

// Ciclo de vida completo de um ato normativo (docs/ciclo-de-vida.md): elaboração, revisão, publicação e o
// ciclo de emenda (alteração de um ato já publicado), além da revogação em três etapas.
//
// Tabela de transições. Só existe UMA etapa local por vez: por isso não há saída de EM_ALTERACAO para
// ANALISE_REVOGACAO -- para revogar, primeiro conclui-se ou cancela-se a alteração. EM_REVOGACAO só sai
// para REVOGADO.
@Component
public class CicloDeVidaDeEspecieConvencional implements RegrasDoCicloDeVidaDoDocumento {

    // Devolver leva ao começo do trabalho: um documento já PUBLICADO volta para a alteração que estava
    // fazendo; um nunca publicado, para a minuta.
    private static SituacaoLocalEnum destinoDeDevolucao(SituacaoBcaEnum bca) {
        return bca == SituacaoBcaEnum.PUBLICADO ? EM_ALTERACAO : MINUTA;
    }

    @Override
    public Optional<AcaoDeEtapa> acaoPara(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca) {
        return Optional.ofNullable(switch (atual) {
            case RASCUNHO          -> destino == MINUTA ? AcaoDeEtapa.MINUTAR
                                    : destino == CANCELADO ? AcaoDeEtapa.CANCELAR_DOCUMENTO : null;
            case MINUTA            -> destino == EM_REVISAO ? AcaoDeEtapa.ENVIAR_PARA_REVISAO
                                    : destino == CANCELADO ? AcaoDeEtapa.CANCELAR_DOCUMENTO : null;
            case EM_REVISAO        -> destino == EM_PUBLICACAO ? AcaoDeEtapa.APROVAR
                                    : destino == destinoDeDevolucao(bca) ? AcaoDeEtapa.DEVOLVER : null;
            case EM_PUBLICACAO     -> destino == SEM_ETAPA ? AcaoDeEtapa.PUBLICAR
                                    : destino == destinoDeDevolucao(bca) ? AcaoDeEtapa.DEVOLVER : null;
            case EM_ALTERACAO      -> destino == EM_REVISAO ? AcaoDeEtapa.ENVIAR_PARA_REVISAO
                                    : destino == SEM_ETAPA ? AcaoDeEtapa.CANCELAR_ALTERACAO : null;
            case ANALISE_REVOGACAO -> destino == EM_REVOGACAO ? AcaoDeEtapa.APROVAR_REVOGACAO
                                    : destino == SEM_ETAPA ? AcaoDeEtapa.DEVOLVER_ANALISE : null;
            case EM_REVOGACAO      -> destino == SEM_ETAPA ? AcaoDeEtapa.REVOGAR : null;
            case SEM_ETAPA         -> bca != SituacaoBcaEnum.PUBLICADO ? null
                                    : destino == EM_ALTERACAO ? AcaoDeEtapa.INICIAR_ALTERACAO
                                    : destino == ANALISE_REVOGACAO ? AcaoDeEtapa.PEDIR_REVOGACAO : null;
            case CANCELADO         -> null;
        });
    }
}
