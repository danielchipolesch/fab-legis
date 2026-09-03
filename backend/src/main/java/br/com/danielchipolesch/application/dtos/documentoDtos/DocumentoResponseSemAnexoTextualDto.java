package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

public record DocumentoResponseSemAnexoTextualDto(

        Long idDocumento,
        String siglaEspecieNormativa,
        String codigoAssuntoBasico,
        String nomeAssuntoBasico,
        Integer numeroSecundario,
        String codigoDocumento,
        String tituloDocumento,
        DocumentoStatusEnum statusDocumento,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtCriacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtAlteracao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtMinuta,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtAprovacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtPublicacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtRevogacao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtCancelamento,

        String urlPdf,

        int qtdReplicas,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtEmAlteracao,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtAlterado,

        String portariaReferencia,

        String bcaReferencia,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Sao_Paulo")
        Timestamp dtPortariaReferencia,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Sao_Paulo")
        Timestamp dtBcaReferencia,

        Integer versao,
        Long autorId,
        String autorNome,
        Long omId,
        String omNome,

        // Ver Documento.revisorAtribuido/publicadorAtribuido -- o frontend usa isso
        // pra saber se O USUÁRIO ATUAL pode agir/editar neste momento (ex.: editar
        // durante EM_REVISAO só se revisorAtribuidoId == id de quem está logado).
        Long revisorAtribuidoId,
        String revisorAtribuidoNome,
        Long publicadorAtribuidoId,
        String publicadorAtribuidoNome
) {
}
