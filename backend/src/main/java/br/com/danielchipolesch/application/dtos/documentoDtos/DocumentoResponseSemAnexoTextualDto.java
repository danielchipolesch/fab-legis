package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentoResponseSemAnexoTextualDto {

    private Long idDocumento;
    private String siglaEspecieNormativa;
    private String codigoAssuntoBasico;
    private Integer numeroSecundario;
    private String codigoDocumento;
    //    private String nomeAssuntoBasico;
    private String tituloDocumento;
    private DocumentoStatusEnum statusDocumento;
}
