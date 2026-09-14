package br.com.danielchipolesch.application.dtos.buscaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

// Um dispositivo (Artigo/Parágrafo/Inciso/...) que bateu na busca full-text
// (ver BuscaRepository) -- não é um Documento inteiro, é o elemento específico
// onde o termo foi encontrado, com o trecho já destacado (ts_headline).
public record ItemBuscaResponseDto(
        Long documentoId,
        String siglaEspecieNormativa,
        String codigoAssuntoBasico,
        Integer numeroSecundario,
        String tituloDocumento,
        DocumentoStatusEnum statusDocumento,
        SecaoDocumentoEnum secao,
        ItemAnexoParteNormativaTipoEnum tipoItem,
        Long elementoId,
        String trecho
) {
}
