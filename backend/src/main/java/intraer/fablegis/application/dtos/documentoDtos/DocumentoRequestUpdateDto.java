package intraer.fablegis.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentoRequestUpdateDto(

        @NotBlank
        String tituloDocumento,

        Integer numeroSecundario,

        // Opcional -- omitido/null (ou igual à OM atual, como no autosave
        // estrutural do editor, que reenvia o documento inteiro) não tem
        // efeito. Só muda de fato quando difere da OM atual E o documento está
        // em RASCUNHO/MINUTA (ver DocumentoService.update); fora desses status
        // é rejeitado, não ignorado silenciosamente.
        Long omId,

        // Opcional -- só a NPA aceita um valor diferente do atual (a das espécies convencionais é gerada); ver
        // RegrasDeCriacaoDoDocumento.novaIdentificacao.
        @Size(max = 120)
        String identificacao
) {
}
