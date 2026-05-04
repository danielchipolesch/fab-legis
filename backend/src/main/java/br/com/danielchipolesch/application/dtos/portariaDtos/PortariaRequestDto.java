package br.com.danielchipolesch.application.dtos.portariaDtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PortariaRequestDto {

    // TODO Colocar os nomes em língua portuguesa
    @NotNull
    private MultipartFile file;
}
