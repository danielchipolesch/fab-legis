package intraer.fablegis.application.dtos.habilitacaoDtos;

import java.util.List;

// Uma OM e as espécies normativas convencionais que ela está habilitada a criar.
public record HabilitacaoDaOmResponseDto(Long omId, String sigla, String nome, List<Long> especieIds) {
}
