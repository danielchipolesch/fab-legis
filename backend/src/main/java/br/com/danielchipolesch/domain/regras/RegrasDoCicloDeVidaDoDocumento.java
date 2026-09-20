package br.com.danielchipolesch.domain.regras;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;

import java.util.Optional;

// Quais mudanças de etapa as regras da espécie permitem. Um ato normativo percorre o ciclo completo (incluindo o ciclo
// de emenda: alteração de um ato publicado); uma NPA só tem publicação e revogação. O que cada ação faz
// (notificar, gravar datas, gerar arquivos) continua no DocumentoStatusService.
public interface RegrasDoCicloDeVidaDoDocumento {

    // Vazio quando a transição não é permitida nessas regras para a situação atual.
    Optional<AcaoDeEtapa> acaoPara(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca);
}
