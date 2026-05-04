package br.com.danielchipolesch.application.dtos.fileAttachmentDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileAttachmentUpdateRequestDto {

    @NotBlank
    private String textoAnexo;
}
