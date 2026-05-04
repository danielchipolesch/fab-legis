package br.com.danielchipolesch.application.dtos.portariaDtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PortariaDto {

    private Long idPortaria;
//    private DocumentResponseDto documento;
    private String nomePortaria;
    private byte[] dadoBase64;
}
