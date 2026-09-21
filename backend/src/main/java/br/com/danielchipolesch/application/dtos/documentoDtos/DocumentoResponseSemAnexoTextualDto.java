package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
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
        String publicadorAtribuidoNome,

        // Autor OU coautor do documento (ver DocumentoAcessoService.podeEditar) --
        // só preenchido na listagem paginada (DocumentoController.getAll), onde
        // existe um usuário logado "espectador" cujo ponto de vista faz sentido
        // perguntar; null nos demais usos deste DTO (resposta de uma mutação que
        // o próprio usuário acabou de fazer, onde a pergunta não se aplica). O
        // ícone de editar da HomePage usa isto pra não ficar habilitado pra
        // RASCUNHO/MINUTA de outra pessoa só porque a OM bate -- posse nunca foi
        // (e não deveria ser) sobre pertencer à mesma OM, ver docs/autenticacao.md.
        Boolean ehAutorOuCoautor,

        // Qual conjunto de regras a espécie segue (CONVENCIONAL, COMUNICACAO_OFICIAL_PADRONIZADA) -- o frontend escolhe o perfil de edição,
        // prévia e publicação por aqui, nunca pela sigla da espécie.
        String tipoDeEspecie
) {
}
