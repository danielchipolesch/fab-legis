package intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;

public record ItemAnexoParteNormativaRequestDto(
        Long parentId,
        ItemAnexoParteNormativaTipoEnum tipo,
        String titulo,
        String conteudo
) {
}
