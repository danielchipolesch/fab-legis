package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.AssuntoBasicoRepository;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Os controllers de ponta a ponta -- segurança, validação, serviço, JPA e PostgreSQL de verdade, com as migrations do
// Flyway aplicadas --, no que os testes unitários (que mockam tudo) não enxergam: a regra de acesso por perfil, o corpo
// de erro no formato da API e o que realmente chega ao banco.
class ControllersIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_DO_LEITOR = "52998224725";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired AssuntoBasicoRepository assuntoRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Usuario administradorPadrao() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    // O administrador padrão que o DataSeeder cria no primeiro boot: ADMIN + EDIT.
    private RequestPostProcessor comoEditor() {
        return como(administradorPadrao());
    }

    // Um usuário real da mesma OM, mas sem nenhum perfil: só visualiza o acervo.
    private RequestPostProcessor comoSemPerfil() {
        var leitor = usuarioRepository.findByCpf(CPF_DO_LEITOR).orElseGet(() -> {
            var u = new Usuario();
            u.setNome("Leitor");
            u.setCpf(CPF_DO_LEITOR);
            u.setSenhaHash("{noop}x");
            u.setOm(administradorPadrao().getOm());
            u.setPerfis(new HashSet<>());
            return usuarioRepository.save(u);
        });
        return como(leitor);
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Long idDaEspecie(String sigla) {
        return especieRepository.findAll().stream().filter(e -> sigla.equals(e.getSigla())).findFirst().orElseThrow().getId();
    }

    private String corpoDeNovoDocumento() {
        Long assunto = assuntoRepository.findAll().get(0).getId();
        return "{\"idEspecieNormativa\": " + idDaEspecie("DCA") + ", \"idAssuntoBasico\": " + assunto
                + ", \"tituloDocumento\": \"Teste de integração\"}";
    }

    // ─── Banco e migrations ───────────────────────────────────────────────────

    @Test
    void asMigrationsDoFlywayForamTodasAplicadasNoPostgresDeVerdade() {
        Integer aplicadas = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        Integer falhas = jdbc.queryForObject("select count(*) from flyway_schema_history where not success", Integer.class);

        assertThat(aplicadas).isGreaterThanOrEqualTo(13);
        assertThat(falhas).isZero();
    }

    @Test
    void osDadosDeReferenciaEOAdministradorPadraoNascemNoPrimeiroBoot() {
        assertThat(especieRepository.findAll()).extracting("sigla").contains("DCA", "NPA");
        assertThat(assuntoRepository.findAll()).isNotEmpty();
        assertThat(usuarioRepository.findAll()).anyMatch(u -> !u.isSistema());
    }

    // ─── Segurança ────────────────────────────────────────────────────────────

    @Test
    void semAutenticacaoOAcervoNaoSeAbre() throws Exception {
        mvc.perform(get("/v1/documentos/resumo")).andExpect(status().isUnauthorized());
    }

    @Test
    void criarDocumentoExigeOPerfilEdit() throws Exception {
        mvc.perform(post("/v1/documentos").with(comoSemPerfil()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeNovoDocumento()))
                .andExpect(status().isForbidden());
    }

    // ─── Criar e ler um documento ─────────────────────────────────────────────

    @Test
    void oEditorCriaUmDocumentoQueDepoisSeLePeloId() throws Exception {
        var criado = mvc.perform(post("/v1/documentos").with(comoEditor()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeNovoDocumento()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siglaEspecieNormativa").value("DCA"))
                .andExpect(jsonPath("$.tituloDocumento").value("Teste de integração"))
                .andExpect(jsonPath("$.situacaoLocal").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));

        mvc.perform(get("/v1/documentos/" + id).with(comoEditor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idDocumento").value(id))
                .andExpect(jsonPath("$.tituloDocumento").value("Teste de integração"));

        Integer noBanco = jdbc.queryForObject("select count(*) from t_documento where id_documento = ?", Integer.class, id);
        assertThat(noBanco).isEqualTo(1);
    }

    @Test
    void tituloEmBrancoEhRecusadoComMensagemEmPortugues() throws Exception {
        mvc.perform(post("/v1/documentos").with(comoEditor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEspecieNormativa\": " + idDaEspecie("DCA") + ", \"tituloDocumento\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Dados inválidos")));
    }

    @Test
    void documentoInexistenteEhNaoEncontradoComOCorpoDeErroDaApi() throws Exception {
        mvc.perform(get("/v1/documentos/987654321").with(comoEditor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─── CPF único ────────────────────────────────────────────────────────────

    private String corpoDeNovoUsuario(String cpf) {
        return "{\"nome\": \"Fulana\", \"cpf\": \"" + cpf + "\", \"senha\": \"senha-forte-1\", \"omId\": "
                + administradorPadrao().getOm().getId() + "}";
    }

    @Test
    void doisUsuariosNaoPodemTerOMesmoCpf_comOuSemMascara() throws Exception {
        mvc.perform(post("/v1/usuarios").with(comoEditor()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeNovoUsuario("39053344705")))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/usuarios").with(comoEditor()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeNovoUsuario("390.533.447-05")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CPF")));
    }

    @Test
    void oBancoTambemBarraOCpfDuplicadoMesmoSemPassarPeloServico() {
        var existente = administradorPadrao();
        var duplicado = new Usuario();
        duplicado.setNome("Outro");
        duplicado.setCpf(existente.getCpf());
        duplicado.setSenhaHash("{noop}x");
        duplicado.setOm(existente.getOm());
        duplicado.setPerfis(new HashSet<>());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicado))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ─── Contrato da API (Swagger/OpenAPI) ────────────────────────────────────

    @Test
    void oContratoDaApiEPublicoMasDeclaraQueTodoEndpointExigeToken() throws Exception {
        mvc.perform(get("/v1/fab-legis-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists());
        mvc.perform(get("/v1/documentos/resumo")).andExpect(status().isUnauthorized());   // descrever não abre a API
    }

    // ─── CORS: só as origens configuradas (APP_CORS_ALLOWED_ORIGINS) ──────────

    @Test
    void oFrontendDeDesenvolvimentoPodeChamarAApiPeloNavegador() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/v1/documentos/resumo")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void umaOrigemQueNaoEstaNaListaNaoPodeChamarAApiPeloNavegador() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/v1/documentos/resumo")
                        .header("Origin", "https://site-qualquer.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
