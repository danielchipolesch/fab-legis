package intraer.fablegis;

import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// O enum PerfilEnum e a tabela t_perfil (migração V21) dizem a mesma coisa: cada perfil com o código e o nome por extenso.
class PerfilEnumIntegracaoTest extends IntegracaoComPostgresBase {

    @Autowired JdbcTemplate jdbc;

    @Test
    void aTabelaDePerfisTemOMesmoCodigoENomeDoEnum() {
        var daTabela = jdbc.queryForList("select sg_perfil, nm_perfil, ds_perfil from t_perfil order by sg_perfil");

        assertThat(daTabela).hasSize(PerfilEnum.values().length);
        for (var linha : daTabela) {
            var perfil = PerfilEnum.valueOf((String) linha.get("sg_perfil"));
            assertThat(linha.get("nm_perfil")).as("nome de %s", perfil).isEqualTo(perfil.nome());
            assertThat(linha.get("ds_perfil")).as("descrição de %s", perfil).isEqualTo(perfil.descricao());
        }
        assertThat(daTabela).extracting(l -> l.get("sg_perfil"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PerfilEnum.values()).map(PerfilEnum::codigo).toList());
    }

    @Test
    void oAdministradorPadraoNasceComoAdministradorGeralNoCendoc() {
        var perfis = jdbc.queryForList(
                "select p.sg_perfil from t_usuario_perfil p join t_usuario u on u.id_usuario = p.usuario_id where u.nr_cpf = '11144477735'", String.class);
        assertThat(perfis).contains("ADM_GER", "ELB", "PNL_GER");
    }
}
