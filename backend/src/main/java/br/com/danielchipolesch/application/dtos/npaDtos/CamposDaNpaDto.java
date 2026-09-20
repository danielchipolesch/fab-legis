package br.com.danielchipolesch.application.dtos.npaDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Campos do cabeçalho e do fecho que só a NPA tem (ver CamposDaNpa). Serve de resposta e de pedido de atualização.
public record CamposDaNpaDto(

        @NotBlank @Size(max = 255)
        String setorEmissor,

        @NotBlank @Size(max = 120)
        String local,

        @Size(max = 10) @Valid
        List<AssinaturaDaNpaDto> assinaturas,

        // Só na resposta (ignorado ao atualizar): o Boletim Interno da revogação, quando a NPA foi revogada.
        String boletimDaRevogacao
) {

    public CamposDaNpaDto(String setorEmissor, String local, List<AssinaturaDaNpaDto> assinaturas) {
        this(setorEmissor, local, assinaturas, null);
    }
}
