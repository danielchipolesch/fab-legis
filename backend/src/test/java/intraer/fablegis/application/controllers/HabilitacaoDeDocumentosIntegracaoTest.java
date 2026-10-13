package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.AssuntoBasicoRepository;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Habilitação de documentos de ponta a ponta (PostgreSQL de verdade): só as OMs habilitadas criam espécies convencionais; as
// demais consultam normalmente; o administrador gerencia. No primeiro boot só o GABAER e o CENDOC estão habilitados.
class HabilitacaoDeDocumentosIntegracaoTest extends IntegracaoComPostgresBase {

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired AssuntoBasicoRepository assuntoRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // O administrador padrão (ADMIN + EDIT), que nasce na OM do CENDOC.
    private Usuario administrador() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    // Uma pessoa com o papel EDIT numa OM comum (sem habilitação nenhuma).
    private Usuario editorDaOm(String siglaDaOm, String cpf) {
        return usuarioRepository.findByCpf(cpf).orElseGet(() -> {
            OrganizacaoMilitar om = omRepository.findBySigla(siglaDaOm).orElseThrow();
            var u = new Usuario();
            u.setNome("Editor " + siglaDaOm);
            u.setCpf(cpf);
            u.setSenhaHash("{noop}x");
            u.setOm(om);
            u.setPapeis(new HashSet<>(Set.of(PapelEnum.EDIT)));
            return usuarioRepository.save(u);
        });
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Long idDaEspecie(String sigla) {
        return especieRepository.findAll().stream().filter(e -> sigla.equals(e.getSigla())).findFirst().orElseThrow().getId();
    }

    private String novoDocumentoDca() {
        return "{\"idEspecieNormativa\": " + idDaEspecie("DCA") + ", \"idAssuntoBasico\": " + assuntoRepository.findAll().get(0).getId()
                + ", \"tituloDocumento\": \"Habilitação\"}";
    }

    // ─── Padrão do primeiro boot ─────────────────────────────────────────────

    @Test
    void noPrimeiroBootSoOGabaerEOCendocTemTodasAsEspeciesConvencionais() {
        Long convencionais = jdbc.queryForObject(
                "select count(*) from t_especie_normativa where st_tipo_especie = 'CONVENCIONAL'", Long.class);

        for (String sigla : new String[]{"GABAER", "CENDOC"}) {
            Long habilitadas = jdbc.queryForObject("select count(*) from t_om_especie_habilitada h join t_organizacao_militar o "
                    + "on o.id_organizacao_militar = h.organizacao_militar_id where o.sg_organizacao_militar = ?", Long.class, sigla);
            assertThat(habilitadas).as("habilitações do " + sigla).isEqualTo(convencionais);
        }
        Long deOutrasOms = jdbc.queryForObject("select count(*) from t_om_especie_habilitada h join t_organizacao_militar o "
                + "on o.id_organizacao_militar = h.organizacao_militar_id where o.sg_organizacao_militar not in ('GABAER','CENDOC')", Long.class);
        assertThat(deOutrasOms).isZero();
    }

    // ─── Criar (e o que continua livre) ──────────────────────────────────────

    @Test
    void umaOmHabilitadaCriaEspecieConvencional() throws Exception {
        mvc.perform(post("/v1/documentos").with(como(administrador())).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isCreated());
    }

    @Test
    void umaOmSemHabilitacaoNaoCriaEspecieConvencionalMasConsultaNormalmente() throws Exception {
        var criado = mvc.perform(post("/v1/documentos").with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content(novoDocumentoDca()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
        var semHabilitacao = editorDaOm("EMAER", "12345678909");

        mvc.perform(post("/v1/documentos").with(como(semHabilitacao)).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("não está habilitada")));
        mvc.perform(post("/v1/documentos/" + id + "/clonar").with(como(semHabilitacao)))
                .andExpect(status().isForbidden());                                     // clonar também cria
        mvc.perform(get("/v1/documentos/" + id).with(como(semHabilitacao)))
                .andExpect(status().isOk());                                            // consultar continua livre
    }

    // ─── O que cada pessoa vê de criar ───────────────────────────────────────

    @Test
    void aOmSemHabilitacaoSoPodeCriarNpaEOCendocPodeCriarTudo() throws Exception {
        mvc.perform(get("/v1/habilitacao-de-documentos/minhas").with(como(editorDaOm("EMAER", "12345678909"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[?(@.tipoDeEspecie == 'CONVENCIONAL')]").isEmpty());

        mvc.perform(get("/v1/habilitacao-de-documentos/minhas").with(como(administrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sigla == 'DCA')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.sigla == 'NPA')]").isNotEmpty());
    }

    // ─── Gestão pelo administrador ───────────────────────────────────────────

    @Test
    void oAdministradorHabilitaUmaOmEEntaoElaPassaACriar() throws Exception {
        var editor = editorDaOm("EMAER", "12345678909");
        Long omId = omRepository.findBySigla("EMAER").orElseThrow().getId();

        mvc.perform(put("/v1/habilitacao-de-documentos/" + omId).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especieIds\": [" + idDaEspecie("DCA") + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sigla").value("EMAER"))
                .andExpect(jsonPath("$.especieIds[0]").value(idDaEspecie("DCA")));

        mvc.perform(post("/v1/documentos").with(como(editor)).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isCreated());

        // e desabilitar volta a barrar
        mvc.perform(put("/v1/habilitacao-de-documentos/" + omId).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especieIds\": []}"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/documentos").with(como(editor)).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isForbidden());
    }

    @Test
    void soOAdministradorGerenciaAHabilitacao() throws Exception {
        var editor = editorDaOm("EMAER", "12345678909");
        Long omId = omRepository.findBySigla("EMAER").orElseThrow().getId();

        mvc.perform(get("/v1/habilitacao-de-documentos").with(como(editor))).andExpect(status().isForbidden());
        mvc.perform(put("/v1/habilitacao-de-documentos/" + omId).with(como(editor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especieIds\": [" + idDaEspecie("DCA") + "]}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/habilitacao-de-documentos").with(como(administrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sigla == 'CENDOC')]").isNotEmpty());
    }

    @Test
    void naoSeHabilitaANpaPoisTodaOmACria() throws Exception {
        Long omId = omRepository.findBySigla("EMAER").orElseThrow().getId();

        mvc.perform(put("/v1/habilitacao-de-documentos/" + omId).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especieIds\": [" + idDaEspecie("NPA") + "]}"))
                .andExpect(status().isNotAcceptable());   // InvalidInputException = 406 na API
    }

    // ─── Visão por espécie ───────────────────────────────────────────────────

    @Test
    void oAdministradorDefineAsOmsDeUmaEspecieEElasPassamACriar() throws Exception {
        var editor = editorDaOm("EMAER", "12345678909");
        Long emaer = omRepository.findBySigla("EMAER").orElseThrow().getId();
        Long cendoc = omRepository.findBySigla("CENDOC").orElseThrow().getId();
        Long gabaer = omRepository.findBySigla("GABAER").orElseThrow().getId();
        long dca = idDaEspecie("DCA");

        mvc.perform(put("/v1/habilitacao-de-documentos/por-especie/" + dca).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omIds\": [" + emaer + "," + cendoc + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sigla").value("DCA"))
                .andExpect(jsonPath("$.omIds.length()").value(2));
        mvc.perform(post("/v1/documentos").with(como(editor)).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isCreated());

        // lista vazia: ninguém mais cria DCA (nem a OM do administrador)
        mvc.perform(put("/v1/habilitacao-de-documentos/por-especie/" + dca).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omIds\": []}"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/documentos").with(como(editor)).contentType(MediaType.APPLICATION_JSON).content(novoDocumentoDca()))
                .andExpect(status().isForbidden());

        // devolve o padrão do primeiro boot (GABAER e CENDOC) para os outros testes
        mvc.perform(put("/v1/habilitacao-de-documentos/por-especie/" + dca).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"omIds\": [" + gabaer + "," + cendoc + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void aVisaoPorEspecieSoParaAdminENaoAceitaNpa() throws Exception {
        mvc.perform(put("/v1/habilitacao-de-documentos/por-especie/" + idDaEspecie("DCA")).with(como(editorDaOm("EMAER", "12345678909")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"omIds\": []}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/v1/habilitacao-de-documentos/por-especie/" + idDaEspecie("NPA")).with(como(administrador()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"omIds\": []}"))
                .andExpect(status().is4xxClientError());
    }
}
