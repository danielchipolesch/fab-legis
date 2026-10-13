package intraer.fablegis.application.dtos.habilitacaoDtos;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// A lista COMPLETA das espécies que a OM passa a poder criar (substitui a anterior; vazia = nenhuma).
public record HabilitacaoDaOmRequestDto(@NotNull List<Long> especieIds) {
}
