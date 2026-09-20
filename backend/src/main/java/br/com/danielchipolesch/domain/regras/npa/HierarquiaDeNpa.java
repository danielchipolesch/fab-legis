package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.regras.RegrasDeHierarquiaDosElementos;
import org.springframework.stereotype.Component;

import java.util.List;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;

// Gramática de elementos da NPA (Anexo XII da NSCA 5-3): capítulo na raiz; seção sob capítulo; subseção sob seção;
// o PARÁGRAFO -- o dispositivo em si, com o texto -- fica direto sob capítulo, seção ou subseção; a ALÍNEA só
// existe sob um parágrafo. Não há artigo, inciso, parágrafo único nem subalínea.
@Component
public class HierarquiaDeNpa implements RegrasDeHierarquiaDosElementos {

    @Override
    public boolean permite(ItemAnexoParteNormativaTipoEnum pai, ItemAnexoParteNormativaTipoEnum filho) {
        return filhosPermitidos(pai).contains(filho);
    }

    @Override
    public List<ItemAnexoParteNormativaTipoEnum> filhosPermitidos(ItemAnexoParteNormativaTipoEnum pai) {
        if (pai == null) return List.of(CAPITULO);
        return switch (pai) {
            case CAPITULO -> List.of(SECAO_NORMATIVA, PARAGRAFO);
            case SECAO_NORMATIVA -> List.of(SUBSECAO_NORMATIVA, PARAGRAFO);
            case SUBSECAO_NORMATIVA -> List.of(PARAGRAFO);
            case PARAGRAFO -> List.of(ALINEA);
            default -> List.of();
        };
    }
}
