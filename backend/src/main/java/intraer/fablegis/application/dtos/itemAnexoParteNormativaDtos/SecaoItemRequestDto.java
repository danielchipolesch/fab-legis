package intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

import java.util.List;

public record SecaoItemRequestDto(
        // Id do elemento já persistido (ver ItemAnexoParteNormativa.id); null indica
        // elemento novo. Usado por DocumentoParteNormativaService.salvarItensNormativos
        // para aplicar um diff em vez de apagar/reinserir a árvore inteira -- ver
        // comentário no método sobre por que isso importa para a edição colaborativa.
        Long id,
        SecaoDocumentoEnum secao,
        ItemAnexoParteNormativaTipoEnum tipo,
        Integer elementOrder,
        String titulo,
        String conteudo,
        String fullTextContent,
        List<SecaoItemRequestDto> filhos
) {
}
