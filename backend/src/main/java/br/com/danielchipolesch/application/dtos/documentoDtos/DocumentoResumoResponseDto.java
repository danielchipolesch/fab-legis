package br.com.danielchipolesch.application.dtos.documentoDtos;

import java.util.Map;

// Alimenta os badges das 4 abas (porAba, sempre com busca/especieSigla aplicados, nunca
// a própria aba nem a situação) e os chips de situação da aba ativa (porStatus, com
// aba/busca/especieSigla aplicados, nunca a própria situação) na HomePage -- ver
// DocumentoService.getResumo.
public record DocumentoResumoResponseDto(
        Map<String, Long> porAba,
        Map<String, Long> porStatus
) {
}
