package intraer.fablegis;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// O enum PapelEnum e a tabela t_papel (migração V21) dizem a mesma coisa: cada papel com o código e o nome por extenso.
class PapelEnumIntegracaoTest extends IntegracaoComPostgresBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void aTabelaDePapeisTemOMesmoCodigoENomeDoEnum() {
        var daTabela = jdbc.queryForList("select sg_papel, nm_papel, ds_papel from t_papel order by sg_papel");

        assertThat(daTabela).hasSize(PapelEnum.values().length);
        for (var linha : daTabela) {
            var papel = PapelEnum.valueOf((String) linha.get("sg_papel"));
            assertThat(linha.get("nm_papel")).as("nome de %s", papel).isEqualTo(papel.nome());
            assertThat(linha.get("ds_papel")).as("descrição de %s", papel).isEqualTo(papel.descricao());
        }
        assertThat(daTabela).extracting(l -> l.get("sg_papel"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PapelEnum.values()).map(PapelEnum::codigo).toList());
    }

    @Test
    void oAdministradorPadraoNasceComoAdministradorGeralNoCendoc() {
        var papeis = jdbc.queryForList(
                "select p.sg_papel from t_usuario_papel p join t_usuario u on u.id_usuario = p.usuario_id where u.nr_cpf = '11144477735'", String.class);
        assertThat(papeis).contains("ADM_GER", "ELB", "PNL_GER");
    }
}
