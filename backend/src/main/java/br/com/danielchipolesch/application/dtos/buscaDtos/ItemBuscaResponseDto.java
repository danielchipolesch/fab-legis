package br.com.danielchipolesch.application.dtos.buscaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

// Um dispositivo (Artigo/Parágrafo/Inciso/...) que bateu na busca full-text
// (ver BuscaRepository) -- não é um Documento inteiro, é o elemento específico
// onde o termo foi encontrado, com o trecho já destacado (ts_headline).
public record ItemBuscaResponseDto(
        Long documentoId,
        String siglaEspecieNormativa,
        // A identificação do documento ("ICA 5-3"; numa NPA, o texto livre informado na criação).
        String codigoDocumento,
        String tituloDocumento,
        SituacaoBcaEnum situacaoBca,
        SituacaoLocalEnum situacaoLocal,
        SecaoDocumentoEnum secao,
        ItemAnexoParteNormativaTipoEnum tipoItem,
        Long elementoId,
        String trecho
) {
}
