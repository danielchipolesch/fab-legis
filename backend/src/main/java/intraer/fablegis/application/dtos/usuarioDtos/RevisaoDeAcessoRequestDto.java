package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

// O resultado da revisão: os perfis que a pessoa MANTÉM (os elevados que não vierem aqui são retirados). Nunca concede.
public record RevisaoDeAcessoRequestDto(@NotNull Set<PapelEnum> papeis) {
}
