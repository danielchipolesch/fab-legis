package intraer.fablegis.application.dtos.habilitacaoDtos;

import java.util.List;

// Uma espécie normativa convencional e as OMs habilitadas a criá-la.
public record HabilitacaoDaEspecieResponseDto(Long especieId, String sigla, List<Long> omIds) {
}
