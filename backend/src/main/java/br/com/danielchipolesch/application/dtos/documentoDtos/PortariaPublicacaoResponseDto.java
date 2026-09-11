package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.PortariaPublicacao;

public record PortariaPublicacaoResponseDto(
        Long id,
        String tipo,
        Integer numeroSequencial,
        String orgao,
        String setor,
        String numeroPortaria,
        // Strings ISO (yyyy-MM-dd), não LocalDate -- LocalDate.toString() já
        // produz esse formato, e evita depender da serialização global do
        // Jackson pra java.time (que devolveria um array [ano,mes,dia] aqui).
        String dataPortaria,
        Integer numeroBca,
        String dataBca,
        String urlPdf
) {
    public static PortariaPublicacaoResponseDto from(PortariaPublicacao p) {
        return new PortariaPublicacaoResponseDto(
                p.getId(),
                p.getTipo().name(),
                p.getNumeroSequencial(),
                p.getOrgao(),
                p.getSetor(),
                p.getNumeroPortaria(),
                p.getDataPortaria().toString(),
                p.getNumeroBca(),
                p.getDataBca().toString(),
                p.getUrlPdf()
        );
    }
}
