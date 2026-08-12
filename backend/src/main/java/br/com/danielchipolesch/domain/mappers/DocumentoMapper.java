package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;

import java.util.List;

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
                documento.getDtCriacao(),
                documento.getDtAlteracao(),
                documento.getDtMinuta(),
                documento.getDtAprovacao(),
                documento.getDtPublicacao(),
                documento.getDtArquivamento(),
                documento.getDtRevogacao(),
                documento.getDtCancelamento(),
                documento.getUrlPdf(),
                documento.getQtdReplicas(),
                documento.getDtEmAlteracao(),
                documento.getPortariaReferencia(),
                documento.getBcaReferencia(),
                documento.getDtPortariaReferencia(),
                documento.getDtBcaReferencia()
        );
    }

    public static DocumentoResponseComAnexoTextualDto documentoToDocumentoComAnexoTextualResponseDto(
            Documento documento,
            List<ItemPartePreliminarResponseDto> preliminares,
            List<ItemAnexoParteNormativaResponseDto> normativos,
            List<ItemParteFinalResponseDto> finais) {
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
                documento.getDtAlteracao(),
                documento.getDtMinuta(),
                documento.getDtAprovacao(),
                documento.getDtPublicacao(),
                documento.getDtArquivamento(),
                documento.getDtRevogacao(),
                documento.getDtCancelamento(),
                documento.getUrlPdf(),
                documento.getQtdReplicas(),
                documento.getDtEmAlteracao(),
                documento.getPortariaReferencia(),
                documento.getBcaReferencia(),
                documento.getDtPortariaReferencia(),
                documento.getDtBcaReferencia(),
                preliminares,
                normativos,
                finais
        );
    }
}
