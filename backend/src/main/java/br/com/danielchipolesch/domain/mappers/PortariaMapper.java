package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseSemPdfDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Portaria;

public class PortariaMapper {

    public static PortariaResponseSemPdfDto regulatoryActToRegulatoryActResponseNoPdfDto(Portaria portaria){
        return new PortariaResponseSemPdfDto(
                portaria.getId(),
                portaria.getFileName()
        );
    }

    public static PortariaResponseDto regulatoryActToRegulatoryActResponseDto(Portaria portaria) {
        return new PortariaResponseDto(
                portaria.getId(),
//                DocumentMapper.documentToDocumentResponseDto(regulatoryAct.getDocument()),
                portaria.getFileName(),
                portaria.getData()
        );
    }

    public static PortariaDto regulatoryActToRegulatoryActDto(Portaria portaria){
        return new PortariaDto(
                portaria.getId(),
//                DocumentMapper.documentToDocumentResponseDto(regulatoryAct.getDocument()),
                portaria.getFileName(),
                portaria.getData()
        );
    }

    public static PortariaResponseSemPdfDto regulatoryActDtoToRegulatoryActResponseNoPdfDto(PortariaDto portariaDto){
        return new PortariaResponseSemPdfDto(
                portariaDto.getIdPortaria(),
                portariaDto.getNomePortaria()
        );
    }
}
