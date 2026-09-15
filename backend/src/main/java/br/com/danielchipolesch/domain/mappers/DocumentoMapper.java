package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.NumeracaoElementoResponseDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;

import java.util.List;

public class DocumentoMapper {

    public static DocumentoResponseSemAnexoTextualDto documentoToDocumentoSemAnexoTextualResponseDto(Documento documento) {
        return documentoToDocumentoSemAnexoTextualResponseDto(documento, null);
    }

    // ehAutorOuCoautor: ver comentário do campo em DocumentoResponseSemAnexoTextualDto --
    // só a listagem paginada (DocumentoController.getAll) chama esta variante.
    public static DocumentoResponseSemAnexoTextualDto documentoToDocumentoSemAnexoTextualResponseDto(
            Documento documento, Boolean ehAutorOuCoautor) {
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
                documento.getDtRevogacao(),
                documento.getDtCancelamento(),
                documento.getUrlPdf(),
                documento.getQtdReplicas(),
                documento.getDtEmAlteracao(),
                documento.getDtAlterado(),
                documento.getPortariaReferencia(),
                documento.getBcaReferencia(),
                documento.getDtPortariaReferencia(),
                documento.getDtBcaReferencia(),
                documento.getVersao(),
                documento.getAutor().getId(),
                documento.getAutor().getNome(),
                documento.getOm().getId(),
                documento.getOm().getNome(),
                documento.getRevisorAtribuido() != null ? documento.getRevisorAtribuido().getId() : null,
                documento.getRevisorAtribuido() != null ? documento.getRevisorAtribuido().getNome() : null,
                documento.getPublicadorAtribuido() != null ? documento.getPublicadorAtribuido().getId() : null,
                documento.getPublicadorAtribuido() != null ? documento.getPublicadorAtribuido().getNome() : null,
                ehAutorOuCoautor
        );
    }

    public static DocumentoResponseComAnexoTextualDto documentoToDocumentoComAnexoTextualResponseDto(
            Documento documento,
            List<ItemPartePreliminarResponseDto> preliminares,
            List<ItemAnexoParteNormativaResponseDto> normativos,
            List<ItemParteFinalResponseDto> finais,
            List<NumeracaoElementoResponseDto> numeracao) {
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
                documento.getDtRevogacao(),
                documento.getDtCancelamento(),
                documento.getUrlPdf(),
                documento.getQtdReplicas(),
                documento.getDtEmAlteracao(),
                documento.getDtAlterado(),
                documento.getPortariaReferencia(),
                documento.getBcaReferencia(),
                documento.getDtPortariaReferencia(),
                documento.getDtBcaReferencia(),
                preliminares,
                normativos,
                finais,
                numeracao,
                documento.getVersao(),
                documento.getAutor().getId(),
                documento.getAutor().getNome(),
                documento.getOm().getId(),
                documento.getOm().getNome(),
                documento.getRevisorAtribuido() != null ? documento.getRevisorAtribuido().getId() : null,
                documento.getRevisorAtribuido() != null ? documento.getRevisorAtribuido().getNome() : null,
                documento.getPublicadorAtribuido() != null ? documento.getPublicadorAtribuido().getId() : null,
                documento.getPublicadorAtribuido() != null ? documento.getPublicadorAtribuido().getNome() : null
        );
    }
}
