package br.com.danielchipolesch.domain.regras;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;

import java.util.List;

// Quem pode ficar dentro de quem na parte normativa: num ato normativo o artigo é a unidade básica e os
// agrupamentos são capítulo/seção/subseção; numa NPA não há artigo -- o parágrafo (o dispositivo) fica direto sob
// capítulo, seção ou subseção, e a alínea só existe sob um parágrafo. O backend recusa, no salvamento, o que a
// hierarquia da espécie não permite (o editor já não oferece essas opções; isto protege a API).
public interface RegrasDeHierarquiaDosElementos {

    // pai == null é a raiz da parte normativa.
    boolean permite(ItemAnexoParteNormativaTipoEnum pai, ItemAnexoParteNormativaTipoEnum filho);

    // Tipos que podem ser filhos diretos de um elemento do tipo dado (pai == null: da raiz), na ordem em que o
    // editor os oferece. Usado para montar o menu "adicionar elemento" e para validar.
    List<ItemAnexoParteNormativaTipoEnum> filhosPermitidos(ItemAnexoParteNormativaTipoEnum pai);

    // Percorre a árvore recebida e recusa a primeira relação pai/filho que a espécie não permite.
    default void validar(List<SecaoItemRequestDto> raiz) {
        validar(null, raiz);
    }

    private void validar(ItemAnexoParteNormativaTipoEnum pai, List<SecaoItemRequestDto> filhos) {
        if (filhos == null) return;
        for (SecaoItemRequestDto filho : filhos) {
            if (!permite(pai, filho.tipo())) {
                throw new InvalidInputException(pai == null
                        ? "Um elemento do tipo " + filho.tipo() + " não pode ficar na raiz do documento desta espécie."
                        : "Um elemento do tipo " + filho.tipo() + " não pode ficar dentro de " + pai + " nesta espécie.");
            }
            validar(filho.tipo(), filho.filhos());
        }
    }
}
