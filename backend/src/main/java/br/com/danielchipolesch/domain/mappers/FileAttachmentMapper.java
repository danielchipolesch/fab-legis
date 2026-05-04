package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.fileAttachmentDtos.FileAttachmentResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.FileAttachment;

public class FileAttachmentMapper {

    public static FileAttachmentResponseDto documentFileAttachmentToDocumentFileAttachmentResponseDto(FileAttachment fileAttachment){
        return new FileAttachmentResponseDto(
                fileAttachment.getId(),
                fileAttachment.getFileName(),
//                DocumentoMapper.documentoDtoToDocumentoSemAnexoTextualResponseDto(fileAttachment.getDocumento()),
                fileAttachment.getData()
        );
    }
}
