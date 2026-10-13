package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Substituir" uma NPA publicada (docs/ciclo-de-vida.md): cria a NPA nova, em RASCUNHO, ligada à substituída.
@Tag("integration")
class SubstituicaoDeNpaIntegracaoTest extends IntegracaoComPostgresBase {

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Usuario administrador() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long novaNpa(String identificacao) throws Exception {
        long especie = especieRepository.findAll().stream().filter(e -> "NPA".equals(e.getSigla())).findFirst().orElseThrow().getId();
        String corpo = "{\"idEspecieNormativa\": " + especie + ", \"identificacao\": \"" + identificacao
                + "\", \"tituloDocumento\": \"Funcionamento\"}";
        var criado = mvc.perform(post("/v1/documentos").with(como(administrador())).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
    }

    private void publicar(long id) {
        jdbc.update("update t_documento set st_situacao_local = 'SEM_ETAPA', st_situacao_bca = 'PUBLICADO' where id_documento = ?", id);
    }

    @Test
    void substituirUmaNpaPublicadaCriaOutraEmRascunhoLigadaAAntiga() throws Exception {
        long antiga = novaNpa("NPA-SUBST-01");
        publicar(antiga);

        var resposta = mvc.perform(post("/v1/documentos/" + antiga + "/substituir").with(como(administrador())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacaoLocal").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        long nova = Long.parseLong(resposta.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
        assertThat(nova).isNotEqualTo(antiga);

        // Os dois lados da ligação.
        mvc.perform(get("/v1/documentos/" + nova + "/substituicao").with(como(administrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.substitui.id").value(antiga))
                .andExpect(jsonPath("$.substituidoPor").doesNotExist());
        mvc.perform(get("/v1/documentos/" + antiga + "/substituicao").with(como(administrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.substituidoPor.id").value(nova))
                .andExpect(jsonPath("$.substitui").doesNotExist());
    }

    @Test
    void naoSubstituiNpaQueAindaNaoFoiPublicada() throws Exception {
        long rascunho = novaNpa("NPA-SUBST-02");

        mvc.perform(post("/v1/documentos/" + rascunho + "/substituir").with(como(administrador())))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("publicada")));
    }

    @Test
    void naoSubstituiDuasVezesAMesmaNpaMasPodeDepoisDeCancelarASubstituta() throws Exception {
        long antiga = novaNpa("NPA-SUBST-03");
        publicar(antiga);
        var resposta = mvc.perform(post("/v1/documentos/" + antiga + "/substituir").with(como(administrador())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long nova = Long.parseLong(resposta.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));

        mvc.perform(post("/v1/documentos/" + antiga + "/substituir").with(como(administrador())))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já está sendo substituída")));

        jdbc.update("update t_documento set st_situacao_local = 'CANCELADO' where id_documento = ?", nova);
        mvc.perform(post("/v1/documentos/" + antiga + "/substituir").with(como(administrador())))
                .andExpect(status().isCreated());
    }

    @Test
    void umaEspecieConvencionalNaoSeSubstitui() throws Exception {
        long dca = especieRepository.findAll().stream().filter(e -> "DCA".equals(e.getSigla())).findFirst().orElseThrow().getId();
        long assunto = jdbc.queryForObject("select min(id_assunto_basico) from t_assunto_basico", Long.class);
        var criado = mvc.perform(post("/v1/documentos").with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEspecieNormativa\": " + dca + ", \"idAssuntoBasico\": " + assunto + ", \"tituloDocumento\": \"X\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
        publicar(id);

        mvc.perform(post("/v1/documentos/" + id + "/substituir").with(como(administrador())))
                .andExpect(status().is4xxClientError());
    }
}
