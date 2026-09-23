package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemParteFinal;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemPartePreliminar;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemParteFinalRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Só se cancela uma alteração sem alterações pendentes (docs/ciclo-de-vida.md): pendente = emenda
// ainda não publicada (status ≠ INALTERADO e sem cláusula congelada), em qualquer das 3 partes.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmendaServicePendentesTest {

    private static final long DOC = 3L;

    @Mock ItemAnexoParteNormativaRepository normativaRepository;
    @Mock ItemPartePreliminarRepository preliminarRepository;
    @Mock ItemParteFinalRepository finalRepository;
    @InjectMocks EmendaService service;

    private static ItemAnexoParteNormativa normativo(ElementoEmendaStatusEnum status, String clausula) {
        var i = new ItemAnexoParteNormativa();
        i.setEmendaStatus(status);
        i.setClausulaEmenda(clausula);
        return i;
    }

    @Test
    void semNenhumaEmendaNaoHaPendencia() {
        when(normativaRepository.findAllByDocumentoId(DOC)).thenReturn(List.of(normativo(ElementoEmendaStatusEnum.INALTERADO, null)));

        assertThat(service.temAlteracoesPendentes(DOC)).isFalse();
    }

    @Test
    void emendaJaPublicadaNaoEPendente() {
        when(normativaRepository.findAllByDocumentoId(DOC))
                .thenReturn(List.of(normativo(ElementoEmendaStatusEnum.ALTERADO, "(redação dada pela Portaria X)")));

        assertThat(service.temAlteracoesPendentes(DOC)).isFalse();
    }

    @Test
    void emendaSemClausulaCongeladaEPendente() {
        when(normativaRepository.findAllByDocumentoId(DOC)).thenReturn(List.of(normativo(ElementoEmendaStatusEnum.INCLUIDO, null)));

        assertThat(service.temAlteracoesPendentes(DOC)).isTrue();
    }

    @Test
    void pendenciaNaParteFinalOuPreliminarTambemConta() {
        var final_ = new ItemParteFinal();
        final_.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        when(finalRepository.findByDocumentoIdOrderByElementOrderAsc(DOC)).thenReturn(List.of(final_));
        assertThat(service.temAlteracoesPendentes(DOC)).isTrue();

        var prelim = new ItemPartePreliminar();
        prelim.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        when(finalRepository.findByDocumentoIdOrderByElementOrderAsc(DOC)).thenReturn(List.of());
        when(preliminarRepository.findByDocumentoIdOrderByElementOrderAsc(DOC)).thenReturn(List.of(prelim));
        assertThat(service.temAlteracoesPendentes(DOC)).isTrue();
    }
}
