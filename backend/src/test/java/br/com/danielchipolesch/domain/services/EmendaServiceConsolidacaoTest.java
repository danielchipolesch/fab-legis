package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.infrastructure.repositories.EmendaHistoricoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Publicar uma alteração congela a cláusula de cada elemento emendado e, para um parágrafo único
// em vigor que ganhou irmãos, a cláusula de renumeração (ver docs/ciclo-de-vida.md e
// NumeracaoService.unicoRenumerado).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmendaServiceConsolidacaoTest {

    private static final String PORTARIA = "Portaria COMAER/DIRAP n° 9, de 5 de março de 2026";
    private static final String BCA = "BCA n° 12, de 6 de março de 2026";

    @Mock ItemAnexoParteNormativaRepository normativaRepository;
    @Mock ItemPartePreliminarRepository preliminarRepository;
    @Mock ItemParteFinalRepository finalRepository;
    @Mock EmendaHistoricoRepository historicoRepository;
    @InjectMocks EmendaService service;

    private static ItemAnexoParteNormativa item(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                ElementoEmendaStatusEnum status, ItemAnexoParteNormativa pai) {
        var i = new ItemAnexoParteNormativa();
        i.setId(id);
        i.setTipo(tipo);
        i.setEmendaStatus(status);
        i.setChildren(new ArrayList<>());
        i.setParent(pai);
        if (pai != null) pai.getChildren().add(i);
        return i;
    }

    private void consolidar(ItemAnexoParteNormativa... itens) {
        when(normativaRepository.findAllByDocumentoId(1L)).thenReturn(List.of(itens));
        service.consolidarPublicacao(1L, PORTARIA, BCA);
    }

    @Test
    void unicoEmVigorQueGanhouUmParagrafoGuardaAClausulaDeRenumeracao() {
        var artigo = item(1, ItemAnexoParteNormativaTipoEnum.ARTIGO, ElementoEmendaStatusEnum.INALTERADO, null);
        var unico = item(2, ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, artigo);
        var novo = item(3, ItemAnexoParteNormativaTipoEnum.PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, artigo);

        consolidar(artigo, unico, novo);

        assertThat(unico.getClausulaRenumeracao())
                .isEqualTo("(redação dada pela " + PORTARIA + ", publicada no " + BCA + ")");
        // O novo parágrafo recebe a cláusula de inclusão normal, não a de renumeração.
        assertThat(novo.getClausulaRenumeracao()).isNull();
        assertThat(novo.getClausulaEmenda()).startsWith("(incluído pela ");
    }

    @Test
    void unicoSozinhoNaoRecebeClausulaDeRenumeracao() {
        var artigo = item(1, ItemAnexoParteNormativaTipoEnum.ARTIGO, ElementoEmendaStatusEnum.INALTERADO, null);
        var unico = item(2, ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, artigo);

        consolidar(artigo, unico);

        assertThat(unico.getClausulaRenumeracao()).isNull();
    }

    @Test
    void unicoIncluidoNestaMesmaAlteracaoNaoEstavaEmVigorEPodeSerRenumeradoSemClausula() {
        var artigo = item(1, ItemAnexoParteNormativaTipoEnum.ARTIGO, ElementoEmendaStatusEnum.INCLUIDO, null);
        var unico = item(2, ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INCLUIDO, artigo);
        var outro = item(3, ItemAnexoParteNormativaTipoEnum.PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, artigo);

        consolidar(artigo, unico, outro);

        assertThat(unico.getClausulaRenumeracao()).isNull();
    }

    @Test
    void aClausulaJaCongeladaNuncaEhSobrescritaEmNovaPublicacao() {
        var artigo = item(1, ItemAnexoParteNormativaTipoEnum.ARTIGO, ElementoEmendaStatusEnum.INALTERADO, null);
        var unico = item(2, ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, artigo);
        unico.setClausulaRenumeracao("(redação dada pela Portaria ANTIGA)");
        var outro = item(3, ItemAnexoParteNormativaTipoEnum.PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, artigo);
        outro.setClausulaEmenda("(incluído pela Portaria ANTIGA)");
        var novo = item(4, ItemAnexoParteNormativaTipoEnum.PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, artigo);

        consolidar(artigo, unico, outro, novo);

        assertThat(unico.getClausulaRenumeracao()).isEqualTo("(redação dada pela Portaria ANTIGA)");
    }
}
