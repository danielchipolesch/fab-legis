package br.com.danielchipolesch.application.dtos.emendaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.EmendaHistorico;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

import java.time.LocalDateTime;

// Uma linha do Quadro de Justificativas das Modificações Propostas (NSCA 5-3, Anexo
// XXIV): texto em vigor / texto proposto / justificativa, por elemento emendado.
// cicloReferencia nulo significa que a linha pertence ao ciclo de alteração ainda em
// andamento (não publicado) — ver EmendaService.consolidarPublicacao().
public record MapaAlteracaoItemResponseDto(
        Long id,
        SecaoDocumentoEnum secao,
        Long elementoId,
        EmendaAcaoEnum acao,
        String textoAnterior,
        String textoNovo,
        String tituloAnterior,
        String tituloNovo,
        String justificativa,
        LocalDateTime dtEmenda,
        String cicloReferencia
) {
    public static MapaAlteracaoItemResponseDto from(EmendaHistorico h) {
        return new MapaAlteracaoItemResponseDto(
                h.getId(),
                h.getSecao(),
                h.getElementoId(),
                h.getAcao(),
                h.getConteudoAnterior(),
                h.getConteudoNovo(),
                h.getTituloAnterior(),
                h.getTituloNovo(),
                h.getJustificativa(),
                h.getDtEmenda(),
                h.getCicloReferencia()
        );
    }
}
