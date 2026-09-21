package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

public record EspecieNormativaResponseDto(
        Long id,
        String sigla,
        String nome,
        String descricao,

        // Qual conjunto de regras a espécie segue (CONVENCIONAL, COMUNICACAO_OFICIAL_PADRONIZADA); o frontend adapta a criação do documento por aqui.
        String tipoDeEspecie
) {
}
