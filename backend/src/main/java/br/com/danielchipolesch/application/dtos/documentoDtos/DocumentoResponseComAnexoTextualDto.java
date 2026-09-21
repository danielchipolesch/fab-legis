package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.NumeracaoElementoResponseDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.List;

public record DocumentoResponseComAnexoTextualDto(

        Long idDocumento,
        String siglaEspecieNormativa,
        String codigoAssuntoBasico,
        String nomeAssuntoBasico,
        Integer numeroSecundario,
        String codigoDocumento,
        String tituloDocumento,
        SituacaoBcaEnum situacaoBca,
        SituacaoLocalEnum situacaoLocal,

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

        List<ItemPartePreliminarResponseDto> itensPreliminares,
        List<ItemAnexoParteNormativaResponseDto> itensNormativos,
        List<ItemParteFinalResponseDto> itensFinais,

        // Numeração de itensNormativos já calculada pelo servidor (mesma regra de
        // frontend/src/utils/numbering.js, ver NumeracaoService) -- carregada em
        // TODA leitura de documento (não só logo após salvar/emendar) pra que o
        // editor reconcilie o número/letra exibido com a fonte de verdade sempre
        // que a árvore é (re)carregada, cobrindo inclusive o diálogo de emenda
        // (EmendaController: emendar/incluirElementoEmenda/reordenarElementoEmenda
        // não devolvem numeração própria, mas o frontend sempre recarrega via
        // GET /{id} logo depois -- ver DocumentosStore.fetchDocumento).
        List<NumeracaoElementoResponseDto> numeracao,

        // versao alimenta a checagem de conflito de edição concorrente -- o editor
        // manda de volta como versaoEsperada em cada salvamento (SecoesSaveRequestDto,
        // EmendaElementoRequestDto, EmendaIncluirRequestDto). Ver DocumentoConcorrenciaService.
        Integer versao,
        Long autorId,
        String autorNome,
        Long omId,
        String omNome,

        Long revisorAtribuidoId,
        String revisorAtribuidoNome,
        Long publicadorAtribuidoId,
        String publicadorAtribuidoNome,

        // Ver DocumentoResponseSemAnexoTextualDto.tipoDeEspecie.
        String tipoDeEspecie
) {
}
