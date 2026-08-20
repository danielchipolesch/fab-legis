package br.com.danielchipolesch.application.dtos.emendaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.EmendaHistorico;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import lombok.Data;

import java.time.LocalDateTime;

// Uma linha do Quadro de Justificativas das Modificações Propostas (NSCA 5-3, Anexo
// XXIV): texto em vigor / texto proposto / justificativa, por elemento emendado.
// cicloReferencia nulo significa que a linha pertence ao ciclo de alteração ainda em
// andamento (não publicado) — ver EmendaService.consolidarPublicacao().
@Data
public class MapaAlteracaoItemResponseDto {

    private Long id;
    private SecaoDocumentoEnum secao;
    private Long elementoId;
    private EmendaAcaoEnum acao;
    private String textoAnterior;
    private String textoNovo;
    private String tituloAnterior;
    private String tituloNovo;
    private String justificativa;
    private LocalDateTime dtEmenda;
    private String cicloReferencia;

    public static MapaAlteracaoItemResponseDto from(EmendaHistorico h) {
        MapaAlteracaoItemResponseDto dto = new MapaAlteracaoItemResponseDto();
        dto.setId(h.getId());
        dto.setSecao(h.getSecao());
        dto.setElementoId(h.getElementoId());
        dto.setAcao(h.getAcao());
        dto.setTextoAnterior(h.getConteudoAnterior());
        dto.setTextoNovo(h.getConteudoNovo());
        dto.setTituloAnterior(h.getTituloAnterior());
        dto.setTituloNovo(h.getTituloNovo());
        dto.setJustificativa(h.getJustificativa());
        dto.setDtEmenda(h.getDtEmenda());
        dto.setCicloReferencia(h.getCicloReferencia());
        return dto;
    }
}
