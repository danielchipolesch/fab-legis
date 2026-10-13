package intraer.fablegis.application.dtos.documentoDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;

// A ligação "Substituir" de uma NPA, vista dos dois lados: o documento que ela substitui e o que a substitui.
public record SubstituicaoDeDocumentoDto(Ponta substitui, Ponta substituidoPor) {

    public record Ponta(Long id, String identificacao, String titulo, SituacaoLocalEnum situacaoLocal, SituacaoBcaEnum situacaoBca) {
        public static Ponta de(Documento d) {
            return d == null ? null : new Ponta(d.getId(), d.getIdentificacao(), d.getTituloDocumento(), d.getSituacaoLocal(), d.getSituacaoBca());
        }
    }
}
