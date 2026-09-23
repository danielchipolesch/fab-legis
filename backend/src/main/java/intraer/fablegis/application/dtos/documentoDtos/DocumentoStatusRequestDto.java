package intraer.fablegis.application.dtos.documentoDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Pedido de mudança de etapa (situação LOCAL) -- a situação BCA nunca é pedida: ela
// muda como CONSEQUÊNCIA de registrar uma portaria + BCA (ver DocumentoStatusService).
// SEM_ETAPA como destino significa "concluir a etapa em curso", e o que isso faz
// depende de onde o documento está:
//   EM_PUBLICACAO     -> publicar (edição ou alteração): exige portaria/BCA/PDF
//   EM_REVOGACAO      -> revogar (situação BCA passa a REVOGADO): exige portaria/BCA/PDF
//   ANALISE_REVOGACAO -> devolver o pedido de revogação (o documento segue PUBLICADO)
//   EM_ALTERACAO      -> cancelar a alteração (descarta o que estava pendente)
public record DocumentoStatusRequestDto(

        @NotNull
        SituacaoLocalEnum situacaoLocal,

        // Obrigatório só ao enviar para revisão/análise de revogação (destino
        // EM_REVISAO/ANALISE_REVOGACAO) -- id de quem, com papel APROV na mesma OM, vai
        // revisar. Ver DocumentoStatusService/DocumentoAcessoService.
        Long revisorId,

        // Obrigatório só ao aprovar (EM_REVISAO -> EM_PUBLICACAO) ou ao aprovar uma
        // revogação (ANALISE_REVOGACAO -> EM_REVOGACAO) -- id de quem, com papel PUBLIC na
        // mesma OM, vai publicar/formalizar.
        Long publicadorId,

        // Obrigatórios ao publicar (EM_PUBLICACAO -> SEM_ETAPA) e ao revogar
        // (EM_REVOGACAO -> SEM_ETAPA) -- não ao devolver nem ao cancelar.
        String orgaoPortaria,
        String setorPortaria,
        String numeroPortaria,
        LocalDate dataPortaria,
        Integer numeroBca,
        LocalDate dataBca,

        // Parte preliminar (epígrafe/ementa/preâmbulo/fecho/assinatura): obrigatória
        // SÓ na PRIMEIRA publicação (a portaria de publicação é a única que aparece nela e
        // nunca é substituída). Alteração e revogação ignoram estes campos -- aparecem por
        // cláusula em cada elemento (alteração) ou no selo REVOGADO (revogação total).
        // Cada campo é uma string JSON no mesmo formato usado por "conteudo" em
        // SecaoItemRequestDto.
        String epigrafe,
        String ementa,
        String preambulo,
        String fecho,
        String assinatura,

        // URL (MinIO) do PDF da portaria já enviado via POST .../portaria-pdf
        // antes deste request.
        String portariaPdfUrl,

        // Só numa NPA, ao publicar (EM_PUBLICACAO -> SEM_ETAPA) e ao revogar (EM_REVOGACAO -> SEM_ETAPA): o número e a
        // data do Boletim Interno em que a NPA foi publicada ou revogada. Não há portaria nem BCA.
        Integer numeroBoletimInterno,
        LocalDate dataBoletimInterno
) {

    // Forma sem o Boletim Interno: o pedido de um ato normativo (e todas as etapas que não registram publicação).
    public DocumentoStatusRequestDto(SituacaoLocalEnum situacaoLocal, Long revisorId, Long publicadorId,
                                     String orgaoPortaria, String setorPortaria, String numeroPortaria,
                                     LocalDate dataPortaria, Integer numeroBca, LocalDate dataBca,
                                     String epigrafe, String ementa, String preambulo, String fecho,
                                     String assinatura, String portariaPdfUrl) {
        this(situacaoLocal, revisorId, publicadorId, orgaoPortaria, setorPortaria, numeroPortaria, dataPortaria,
                numeroBca, dataBca, epigrafe, ementa, preambulo, fecho, assinatura, portariaPdfUrl, null, null);
    }
}
