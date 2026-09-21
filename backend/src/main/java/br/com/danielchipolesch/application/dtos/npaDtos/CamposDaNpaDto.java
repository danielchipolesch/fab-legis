package br.com.danielchipolesch.application.dtos.npaDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Campos do cabeçalho e do fecho que só a NPA tem (ver CamposDaNpa). Serve de resposta e de pedido de atualização.
//
// `assinaturas` são os blocos que o autor escreve (Visto, Proposto por...). Os blocos "Elaborado por" e "Aprovado por"
// não são escritos: vêm do próprio documento e só aparecem na resposta (ver CamposDeNpa.camposParaLeiaute).
public record CamposDaNpaDto(

        @NotBlank @Size(max = 255)
        String setorEmissor,

        @NotBlank @Size(max = 120)
        String local,

        @Size(max = 10) @Valid
        List<AssinaturaDaNpaDto> assinaturas,

        // Só na resposta (ignorado ao atualizar): o Boletim Interno da revogação, quando a NPA foi revogada.
        String boletimDaRevogacao,

        // Só na resposta (ignorado ao atualizar): o autor e todos os coautores do documento, um por linha.
        List<String> elaboradoPor,

        // Só na resposta (ignorado ao atualizar): quem aprovou o documento (a pessoa escolhida ao enviá-lo para revisão);
        // vazio enquanto ele não foi aprovado -- o layout mostra uma máscara no lugar.
        List<String> aprovadoPor
) {

    public CamposDaNpaDto(String setorEmissor, String local, List<AssinaturaDaNpaDto> assinaturas) {
        this(setorEmissor, local, assinaturas, null, null, null);
    }

    public CamposDaNpaDto(String setorEmissor, String local, List<AssinaturaDaNpaDto> assinaturas,
                          String boletimDaRevogacao) {
        this(setorEmissor, local, assinaturas, boletimDaRevogacao, null, null);
    }
}
