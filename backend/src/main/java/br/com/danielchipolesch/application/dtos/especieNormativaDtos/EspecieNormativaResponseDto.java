package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

public record EspecieNormativaResponseDto(
        Long id,
        String sigla,
        String nome,
        String descricao,

        // Qual conjunto de regras a espécie segue (ATO_NORMATIVO, NPA); o frontend adapta a criação do documento por aqui.
        String tipoDeRegras
) {
}
