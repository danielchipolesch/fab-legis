package intraer.fablegis.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequestCreateDto(

        @NotNull
        Long idEspecieNormativa,

        // Exigido só pelas espécies que se numeram por assunto básico (atos normativos) -- quem o exige é
        // RegrasDeCriacaoDoDocumento da espécie, não a validação do DTO.
        Long idAssuntoBasico,

        // Texto livre; exigido só pelas espécies que se identificam assim (NPA).
        String identificacao,

        @NotBlank
        String tituloDocumento
) {
}
