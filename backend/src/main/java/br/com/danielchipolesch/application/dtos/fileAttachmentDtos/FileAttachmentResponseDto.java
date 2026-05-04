package br.com.danielchipolesch.application.dtos.fileAttachmentDtos;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileAttachmentResponseDto {

    private Long idAnexoArquivo;
    private String nomeAnexoArquivo;
//    private DocumentoResponseSemAnexoTextualDto documento;
    private byte[] arquivoBase64;
}
