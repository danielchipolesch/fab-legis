package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import java.util.List;

// Resposta de PATCH /{id}/secoes: a árvore persistida (com ids reais dos
// elementos recém-criados, ver DocumentoController.saveSecoes) + a numeração
// já recalculada pelo servidor para os mesmos elementos (capítulo/seção/
// subseção/artigo — ver NumeracaoService). O frontend usa "numeracao" para
// reconciliar o número/letra que `frontend/src/utils/numbering.js` já havia
// calculado localmente para feedback instantâneo durante a edição --
// eliminando o risco de as duas implementações divergirem silenciosamente
// (mesma regra, mantida hoje em dois lugares) sem trocar o cálculo local por
// uma chamada de rede a cada interação (isso sim, atrasaria visivelmente
// arrastar/promover/rebaixar elemento, que hoje é instantâneo).
public record SecoesSaveResponseDto(
        List<ItemAnexoParteNormativaResponseDto> itens,
        List<NumeracaoElementoResponseDto> numeracao
) {
}
