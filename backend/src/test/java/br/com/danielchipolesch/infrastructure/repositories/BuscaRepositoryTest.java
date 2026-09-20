package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// A busca no conteúdo devolve o dispositivo onde o termo foi achado, com a identificação do documento -- "ICA 5-3" num
// ato normativo, o texto livre numa NPA (que não tem assunto básico nem sequencial). O teste fixa a ordem das colunas
// do SQL, que o mapeamento lê por posição.
class BuscaRepositoryTest {

    private static Object[] linha(String identificacao) {
        return new Object[]{7L, "NPA", identificacao, "Funcionamento da Divisão", "PUBLICADO", "SEM_ETAPA",
                "PARTE_NORMATIVA", "PARAGRAFO", 42L, 0.5f, "trecho <b>destacado</b>"};
    }

    @Test
    void mapeiaCadaColunaParaOCampoCerto() {
        var item = BuscaRepository.mapear(linha("NPA-AGO-01"));

        assertThat(item.documentoId()).isEqualTo(7L);
        assertThat(item.siglaEspecieNormativa()).isEqualTo("NPA");
        assertThat(item.codigoDocumento()).isEqualTo("NPA-AGO-01");
        assertThat(item.tituloDocumento()).isEqualTo("Funcionamento da Divisão");
        assertThat(item.situacaoBca()).isEqualTo(SituacaoBcaEnum.PUBLICADO);
        assertThat(item.situacaoLocal()).isEqualTo(SituacaoLocalEnum.SEM_ETAPA);
        assertThat(item.secao()).isEqualTo(SecaoDocumentoEnum.PARTE_NORMATIVA);
        assertThat(item.tipoItem()).isEqualTo(ItemAnexoParteNormativaTipoEnum.PARAGRAFO);
        assertThat(item.elementoId()).isEqualTo(42L);
        assertThat(item.trecho()).isEqualTo("trecho <b>destacado</b>");
    }

    @Test
    void aIdentificacaoDeUmAtoNormativoTemOFormatoEspecieAssuntoSequencial() {
        assertThat(BuscaRepository.mapear(linha("ICA 5-3")).codigoDocumento()).isEqualTo("ICA 5-3");
    }
}
