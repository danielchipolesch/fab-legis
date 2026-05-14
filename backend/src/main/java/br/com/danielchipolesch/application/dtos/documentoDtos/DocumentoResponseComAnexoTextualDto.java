package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
public class DocumentoResponseComAnexoTextualDto {

    private Long idDocumento;
    private String siglaEspecieNormativa;
    private String codigoAssuntoBasico;
    private String nomeAssuntoBasico;
    private Integer numeroSecundario;
    private String codigoDocumento;
    private String tituloDocumento;
    private DocumentoStatusEnum statusDocumento;
    private Timestamp dtCriacao;
    private List<ItemAnexoParteNormativa> itens;
}
