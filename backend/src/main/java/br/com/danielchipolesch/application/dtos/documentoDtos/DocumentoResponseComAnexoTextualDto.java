package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.mappers.ItemAnexoParteNormativaMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentoResponseComAnexoTextualDto {

    private Long idDocumento;
    private String siglaEspecieNormativa;
    private String codigoAssuntoBasico;
    private Integer numeroSecundario;
    private String codigoDocumento;
    private String tituloDocumento;
    private DocumentoStatusEnum statusDocumento;
    private List<ItemAnexoParteNormativa> itens;
//    private List<ItemAnexoParteNormativaResponseDto> itens;



//    public DocumentoResponseComAnexoTextualDto(Long idDocumento, String siglaEspecieNormativa, String codigoAssuntoBasico, Integer numeroSecundario, String codigoDocumento, String tituloDocumento, DocumentoStatusEnum statusDocumento, List<ItemAnexoParteNormativa> itens) {
//        this.idDocumento = idDocumento;
//        this.siglaEspecieNormativa = siglaEspecieNormativa;
//        this.codigoAssuntoBasico = codigoAssuntoBasico;
//        this.numeroSecundario = numeroSecundario;
//        this.codigoDocumento = codigoDocumento;
//        this.tituloDocumento = tituloDocumento;
//        this.statusDocumento = statusDocumento;
//        this.itens = itens.stream().map(ItemAnexoParteNormativaMapper::itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto).toList();
//    }
}
