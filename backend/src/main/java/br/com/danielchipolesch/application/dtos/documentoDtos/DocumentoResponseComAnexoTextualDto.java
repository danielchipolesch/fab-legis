package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Sao_Paulo")
    private Timestamp dtCriacao;

    private List<ItemPartePreliminarResponseDto> itensPreliminares;
    private List<ItemAnexoParteNormativaResponseDto> itensNormativos;
    private List<ItemParteFinalResponseDto> itensFinais;
}
