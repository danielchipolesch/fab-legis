package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;

import java.util.List;

// Linha das telas de fila pessoal (RevisaoPage.vue/PublicacaoPage.vue) --
// deliberadamente mais enxuto que DocumentoResponseSemAnexoTextualDto: só o que
// a tabela mostra (código, título, autores, situação), ver
// DocumentoService.getMinhaRevisao/getMinhaPublicacao.
public record DocumentoFilaResponseDto(
        Long idDocumento,
        String codigoDocumento,
        String tituloDocumento,
        DocumentoStatusEnum statusDocumento,

        // Autor primeiro, seguido dos coautores (ver DocumentoCompartilhamento) --
        // nomes completos, mesma fonte que já aparece no resto do sistema.
        List<String> autores,

        // Se o documento já foi publicado antes -- o frontend usa isso pra saber se
        // aprovar resolve pra APROVADO ou ALTERADO, e devolver pra MINUTA ou
        // EM_ALTERACAO (mesmo discriminante de DocumentoStatusService.jaPublicadoAntes).
        boolean jaPublicadoAntes
) {
}
