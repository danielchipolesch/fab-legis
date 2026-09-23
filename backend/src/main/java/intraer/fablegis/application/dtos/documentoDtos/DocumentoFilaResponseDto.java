package intraer.fablegis.application.dtos.documentoDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;

import java.util.List;

// Linha das telas de fila pessoal (RevisaoPage.vue/PublicacaoPage.vue) --
// deliberadamente mais enxuto que DocumentoResponseSemAnexoTextualDto: só o que
// a tabela mostra (código, título, autores, situações), ver
// DocumentoService.getMinhaRevisao/getMinhaPublicacao.
public record DocumentoFilaResponseDto(
        Long idDocumento,
        String codigoDocumento,
        String tituloDocumento,

        // Situação real (BCA) e etapa interna (local) -- sempre mostradas juntas. A
        // situação BCA também diz ao frontend para onde "devolver" leva: documento
        // PUBLICADO volta para EM_ALTERACAO, NAO_PUBLICADO volta para MINUTA.
        SituacaoBcaEnum situacaoBca,
        SituacaoLocalEnum situacaoLocal,

        // Autor primeiro, seguido dos coautores (ver DocumentoCompartilhamento) --
        // nomes completos, mesma fonte que já aparece no resto do sistema.
        List<String> autores
) {
}
