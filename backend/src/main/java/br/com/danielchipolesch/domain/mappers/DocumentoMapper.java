package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComPortariaDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;

public class DocumentoMapper {

    public static DocumentoResponseSemAnexoTextualDto documentoToDocumentoSemAnexoTextualResponseDto(Documento documento) {
        return new DocumentoResponseSemAnexoTextualDto(
                documento.getId(),
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getAssuntoBasico().getNome(),
                documento.getNumeroSecundario(),
                String.format("%s %s-%d",
                        documento.getEspecieNormativa().getSigla(),
                        documento.getAssuntoBasico().getCodigo(),
                        documento.getNumeroSecundario()),
                documento.getTituloDocumento(),
                documento.getDocumentoStatus(),
                documento.getDtCriacao()
        );
    }

    public static DocumentoResponseComAnexoTextualDto documentoToDocumentoComAnexoTextualResponseDto(Documento documento) {
        return new DocumentoResponseComAnexoTextualDto(
                documento.getId(),
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getAssuntoBasico().getNome(),
                documento.getNumeroSecundario(),
                String.format("%s %s-%d",
                        documento.getEspecieNormativa().getSigla(),
                        documento.getAssuntoBasico().getCodigo(),
                        documento.getNumeroSecundario()),
                documento.getTituloDocumento(),
                documento.getDocumentoStatus(),
                documento.getDtCriacao(),
                documento.getItens()
        );
    }

    public static DocumentoResponseSemAnexoTextualDto documentoDtoToDocumentoSemAnexoTextualResponseDto(DocumentoResponseComPortariaDto dto) {
        return new DocumentoResponseSemAnexoTextualDto(
                dto.getIdDocumento(),
                dto.getSiglaEspecieNormativa(),
                dto.getCodigoAssuntoBasico(),
                dto.getNomeAssuntoBasico(),
                dto.getNumeroSecundario(),
                dto.getCodigoDocumento(),
                dto.getTituloDocumento(),
                dto.getStatusDocumento(),
                null
        );
    }
}
