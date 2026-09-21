package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// O módulo do sistema (docs/funcionalidades.md, "Módulos"): cada tela de módulo lista só os documentos cuja espécie é do
// tipo dele -- os de um módulo nunca aparecem na tabela do outro.
class DocumentoSpecificationsTest {

    @SuppressWarnings("unchecked")
    @Test
    void oModuloListaSoOsDocumentosDaquelesTipoDeEspecie() {
        Root<Documento> root = mock(Root.class);
        Path<Object> especie = mock(Path.class);
        Path<Object> tipoDaEspecie = mock(Path.class);
        when(root.get("especieNormativa")).thenReturn(especie);
        when(especie.get("tipoDeEspecie")).thenReturn(tipoDaEspecie);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate igual = mock(Predicate.class);
        when(cb.equal(tipoDaEspecie, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)).thenReturn(igual);

        var predicado = DocumentoSpecifications.tipoDeEspecie(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        assertThat(predicado).isSameAs(igual);
    }

    @SuppressWarnings("unchecked")
    @Test
    void semModuloNaoRestringeNada() {
        var predicado = DocumentoSpecifications.tipoDeEspecie(null)
                .toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));

        assertThat(predicado).isNull();
    }
}
