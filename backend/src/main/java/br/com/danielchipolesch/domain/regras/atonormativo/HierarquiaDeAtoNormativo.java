package br.com.danielchipolesch.domain.regras.atonormativo;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.regras.RegrasDeHierarquiaDosElementos;
import org.springframework.stereotype.Component;

import java.util.List;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;

// Hierarquia dos atos normativos (LC 95/1998, art. 10; NSCA 5-3): capítulo > seção > subseção > artigo > parágrafo/
// inciso > alínea > subalínea. Hoje quem impõe essa ordem é o editor (promover/rebaixar, "adicionar elemento");
// o backend a aceita como vem, porque documentos já existentes e a edição em massa dependem dessa flexibilidade.
// O menu "adicionar elemento" usa filhosPermitidos para oferecer só o que cabe.
@Component
public class HierarquiaDeAtoNormativo implements RegrasDeHierarquiaDosElementos {

    @Override
    public boolean permite(ItemAnexoParteNormativaTipoEnum pai, ItemAnexoParteNormativaTipoEnum filho) {
        return true;
    }

    @Override
    public List<ItemAnexoParteNormativaTipoEnum> filhosPermitidos(ItemAnexoParteNormativaTipoEnum pai) {
        if (pai == null) return List.of(CAPITULO, ARTIGO);
        return switch (pai) {
            case CAPITULO -> List.of(SECAO_NORMATIVA, ARTIGO);
            case SECAO_NORMATIVA -> List.of(SUBSECAO_NORMATIVA, ARTIGO);
            case SUBSECAO_NORMATIVA -> List.of(ARTIGO);
            case ARTIGO -> List.of(PARAGRAFO, PARAGRAFO_UNICO, INCISO);
            case PARAGRAFO, PARAGRAFO_UNICO -> List.of(INCISO);
            case INCISO -> List.of(ALINEA);
            case ALINEA -> List.of(SUB_ALINEA);
            default -> List.of();
        };
    }
}
