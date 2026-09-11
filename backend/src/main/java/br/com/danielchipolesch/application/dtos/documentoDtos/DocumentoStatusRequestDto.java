package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DocumentoStatusRequestDto(

        @NotNull
        DocumentoStatusEnum status,

        // Obrigatório só ao enviar para revisão/análise de revogação (destino
        // EM_REVISAO/ANALISE_REVOGACAO) -- id de quem, com papel APROV na mesma OM, vai
        // revisar. Ver DocumentoStatusService/DocumentoAcessoService.
        Long revisorId,

        // Obrigatório só ao aprovar (destino APROVADO/ALTERADO, que já cascata para
        // EM_PUBLICACAO) ou ao aprovar uma revogação (destino EM_REVOGACAO) -- id de
        // quem, com papel PUBLIC na mesma OM, vai publicar/formalizar.
        Long publicadorId,

        // Obrigatórios apenas ao publicar (transição para PUBLICADO)
        String orgaoPortaria,
        String setorPortaria,
        String numeroPortaria,
        LocalDate dataPortaria,
        Integer numeroBca,
        LocalDate dataBca,

        // Também obrigatórios apenas ao publicar -- a parte preliminar do
        // documento (epígrafe/ementa/preâmbulo/fecho/assinatura) só existe de
        // fato a partir da publicação, então é coletada aqui, não durante a
        // edição (ver DocumentoStatusService.changeStatus). Cada campo é uma
        // string JSON no mesmo formato usado por "conteudo" em SecaoItemRequestDto.
        String epigrafe,
        String ementa,
        String preambulo,
        String fecho,
        String assinatura,

        // URL (MinIO) do PDF da portaria já enviado via POST .../portaria-pdf
        // antes deste request -- concatenado ao PDF gerado do documento.
        String portariaPdfUrl
) {
}
