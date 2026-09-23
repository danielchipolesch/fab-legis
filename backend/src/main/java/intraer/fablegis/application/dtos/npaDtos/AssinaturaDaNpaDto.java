package intraer.fablegis.application.dtos.npaDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Um bloco de assinatura, em texto livre: um rótulo ("Elaborado por", "Visto", "Proposto por", "Aprovo") e as
// linhas que vêm abaixo dele (nome, posto, função...).
public record AssinaturaDaNpaDto(

        @NotBlank @Size(max = 60)
        String rotulo,

        @Size(max = 6)
        List<@Size(max = 200) String> linhas
) {
}
