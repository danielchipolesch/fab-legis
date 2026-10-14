package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// O número de um ato normativo é único no banco (V25) e a criação simultânea nunca repete número (docs/replicas.md, docs/dominio.md).
class NumeracaoUnicaIntegracaoTest extends IntegracaoComPostgresBase {

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // O administrador padrão (CENDOC, habilitado a criar todas as espécies convencionais) cria os documentos do teste.
    private RequestPostProcessor comoAutor() {
        Usuario autor = usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
        var principal = new UsuarioPrincipal(autor);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long idDaEspecie(String sigla) {
        return especieRepository.findAll().stream().filter(e -> sigla.equals(e.getSigla())).findFirst().orElseThrow().getId();
    }

    private long umAssunto() {
        return jdbc.queryForObject("select min(id_assunto_basico) from t_assunto_basico", Long.class);
    }

    @Test
    void obancoRecusaDoisDocumentosComOMesmoNumeroNaMesmaEspecieEAssunto() {
        long especie = idDaEspecie("DCA");
        long assunto = umAssunto();
        long autor = jdbc.queryForObject("select min(id_usuario) from t_usuario where fl_sistema = false", Long.class);
        long om = jdbc.queryForObject("select om_id from t_usuario where id_usuario = ?", Long.class, autor);
        String inserir = "insert into t_documento (especie_normativa_id, assunto_basico_id, nr_numero_secundario, nm_titulo_documento, "
                + "st_situacao_local, st_situacao_bca, nr_versao, autor_id, om_id, tx_identificacao) values (?, ?, 9001, 'T', 'RASCUNHO', 'NAO_PUBLICADO', 0, ?, ?, 'DCA x-9001')";
        jdbc.update(inserir, especie, assunto, autor, om);

        assertThatThrownBy(() -> jdbc.update(inserir, especie, assunto, autor, om))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_documento_numero");
    }

    @Test
    void umaNpaNaoTemNumeroNemEntraNaRestricao() {
        long npa = idDaEspecie("NPA");
        long autor = jdbc.queryForObject("select min(id_usuario) from t_usuario where fl_sistema = false", Long.class);
        long om = jdbc.queryForObject("select om_id from t_usuario where id_usuario = ?", Long.class, autor);
        String inserir = "insert into t_documento (especie_normativa_id, nm_titulo_documento, st_situacao_local, st_situacao_bca, nr_versao, autor_id, om_id, tx_identificacao) "
                + "values (?, 'T', 'RASCUNHO', 'NAO_PUBLICADO', 0, ?, ?, ?)";

        jdbc.update(inserir, npa, autor, om, "NPA-UNICA-01");
        jdbc.update(inserir, npa, autor, om, "NPA-UNICA-02"); // sem assunto e sem sequencial: nada a violar

        assertThat(jdbc.queryForObject("select count(*) from t_documento where tx_identificacao like 'NPA-UNICA-%'", Integer.class)).isEqualTo(2);
    }

    @Test
    void criacoesSimultaneasNuncaRepetemONumero() throws Exception {
        long dca = idDaEspecie("DCA");
        long assunto = umAssunto();
        int criacoes = 8;
        var largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(criacoes);
        var autor = comoAutor();
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < criacoes; i++) {
                Callable<Integer> criar = () -> {
                    largada.await(); // todas partem juntas, para disputar o mesmo "menor número livre"
                    return mvc.perform(post("/v1/documentos").with(autor).contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"idEspecieNormativa\": " + dca + ", \"idAssuntoBasico\": " + assunto
                                            + ", \"tituloDocumento\": \"Simultâneo\"}"))
                            .andReturn().getResponse().getStatus();
                };
                resultados.add(pool.submit(criar));
            }
            largada.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> resultado : resultados) estados.add(resultado.get());
            assertThat(estados).as("estados HTTP das criações").containsOnly(201);
        } finally {
            pool.shutdownNow();
        }

        List<Integer> numeros = jdbc.queryForList(
                "select nr_numero_secundario from t_documento where especie_normativa_id = ? and assunto_basico_id = ? and nm_titulo_documento = 'Simultâneo'",
                Integer.class, dca, assunto);
        assertThat(numeros).hasSize(criacoes).doesNotHaveDuplicates();
    }
}
