package intraer.fablegis.application.dtos.habilitacaoDtos;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// A lista COMPLETA das OMs que passam a poder criar a espécie (substitui a anterior; vazia = nenhuma).
public record HabilitacaoDaEspecieRequestDto(@NotNull List<Long> omIds) {
}
