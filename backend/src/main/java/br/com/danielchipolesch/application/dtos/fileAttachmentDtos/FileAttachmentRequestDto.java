package br.com.danielchipolesch.application.dtos.fileAttachmentDtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileAttachmentRequestDto {

    @NotNull
    private MultipartFile documentoBase64;
}
