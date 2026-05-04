package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateItemParteNormativaDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComPortariaDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;

import java.util.ArrayList;
import java.util.List;

public class DocumentoMapper {

    public static DocumentoResponseSemAnexoTextualDto documentoToDocumentoSemAnexoTextualResponseDto(Documento documento){
        return new DocumentoResponseSemAnexoTextualDto(
                documento.getId(),
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getNumeroSecundario(),
                String.format("%s %s-%d", documento.getEspecieNormativa().getSigla(), documento.getAssuntoBasico().getCodigo(), documento.getNumeroSecundario()),
                documento.getTituloDocumento(),
                documento.getDocumentoStatus()
        );
    }

//    private List<ItemAnexoParteNormativaResponseDto> convertList(List<ItemAnexoParteNormativa> itens){
//
//         itens.forEach(ItemAnexoParteNormativaMapper::itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto);
//    }

    public static DocumentoResponseComAnexoTextualDto documentoToDocumentoComAnexoTextualResponseDto(Documento documento) {

        return new DocumentoResponseComAnexoTextualDto(
                documento.getId(),
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getNumeroSecundario(),
                String.format("%s %s-%d", documento.getEspecieNormativa().getSigla(), documento.getAssuntoBasico().getCodigo(), documento.getNumeroSecundario()),
                documento.getTituloDocumento(),
                documento.getDocumentoStatus(),
                documento.getItens()
//                documento.getItens().stream().map(ItemAnexoParteNormativaMapper::itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto).toList()
        );
    }

    public static DocumentoResponseSemAnexoTextualDto documentoDtoToDocumentoSemAnexoTextualResponseDto(DocumentoResponseComPortariaDto documentoResponseComPortariaDto){
        return new DocumentoResponseSemAnexoTextualDto(
                documentoResponseComPortariaDto.getIdDocumento(),
                documentoResponseComPortariaDto.getSiglaEspecieNormativa(),
                documentoResponseComPortariaDto.getCodigoAssuntoBasico(),
                documentoResponseComPortariaDto.getNumeroSecundario(),
                documentoResponseComPortariaDto.getCodigoDocumento(),
//                documentDto.getNomeAssuntoBasico(),
                documentoResponseComPortariaDto.getTituloDocumento(),
                documentoResponseComPortariaDto.getStatusDocumento()
        );
    }

//    public static ItemAnexoParteNormativa documentoRequestUpdateItemParteNormativaDtoToItemAnexoParteNormativa(DocumentoRequestUpdateItemParteNormativaDto documentoRequestUpdateItemParteNormativaDto){
//        return new ItemAnexoParteNormativa(
//                null,
//                documentoRequestUpdateItemParteNormativaDto.getParentId(),
//                documentoRequestUpdateItemParteNormativaDto.getTipo(),
//                documentoRequestUpdateItemParteNormativaDto.getTitulo(),
//                documentoRequestUpdateItemParteNormativaDto.getConteuto(),
//                new ArrayList<>()
//        );
//    }
}
