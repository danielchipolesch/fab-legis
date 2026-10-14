package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.EnumSet;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Escopo de acesso de ponta a ponta (docs/autenticacao.md): o admin geral (CENDOC) vê e gerencia todos; o admin local só a
// própria OM, sem conceder ADMIN nem PUBLIC e sem mexer em outros administradores; habilitação e DOM só do admin geral.
class EscopoDeAcessoIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_ADMIN_LOCAL = "33344455508";
    private static final String CPF_EDITOR_LOCAL = "44455566619";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Usuario geral() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    private Usuario usuarioDa(String sigla, String cpf, PapelEnum... papeis) {
        return usuarioRepository.findByCpf(cpf).orElseGet(() -> {
            var u = new Usuario();
            u.setNome("Pessoa " + sigla);
            u.setCpf(cpf);
            u.setSenhaHash("{noop}x");
            u.setOm(omRepository.findBySigla(sigla).orElseThrow());
            var conjunto = EnumSet.noneOf(PapelEnum.class);
            conjunto.addAll(java.util.List.of(papeis));
            u.setPapeis(conjunto);
            return usuarioRepository.save(u);
        });
    }

    private Usuario adminLocal() {
        return usuarioDa("EMAER", CPF_ADMIN_LOCAL, PapelEnum.ELB, PapelEnum.ADM_OM);
    }

    private long idDoEditorLocal() {
        return usuarioDa("EMAER", CPF_EDITOR_LOCAL, PapelEnum.ELB).getId();
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private String corpo(String cpf, long omId, String papeis) {
        return "{\"nome\":\"Novo\",\"cpf\":\"" + cpf + "\",\"senha\":\"senha-forte-1\",\"omId\":" + omId + ",\"papeis\":" + papeis + "}";
    }

    @Test
    void oAdminLocalListaSoAPropriaOmEOGeralListaTodas() throws Exception {
        adminLocal();
        idDoEditorLocal();

        mvc.perform(get("/v1/usuarios").with(como(adminLocal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].omSigla", everyItem(org.hamcrest.Matchers.is("EMAER"))));
        mvc.perform(get("/v1/usuarios").with(como(geral())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].omSigla", hasItem("CENDOC")))
                .andExpect(jsonPath("$[*].omSigla", hasItem("EMAER")));
    }

    @Test
    void oAdminLocalNaoVeNemAlteraUsuarioDeOutraOm() throws Exception {
        adminLocal();
        long idDoGeral = geral().getId();

        mvc.perform(get("/v1/usuarios/" + idDoGeral).with(como(adminLocal()))).andExpect(status().isNotFound());
        mvc.perform(put("/v1/usuarios/" + idDoGeral).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"omId\":" + omRepository.findBySigla("EMAER").orElseThrow().getId() + ",\"ativo\":true,\"papeis\":[\"ELB\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oAdminLocalCriaNaPropriaOmMasNaoEmOutraNemComAdminOuPublic() throws Exception {
        long emaer = omRepository.findBySigla("EMAER").orElseThrow().getId();
        long cendoc = omRepository.findBySigla("CENDOC").orElseThrow().getId();

        mvc.perform(post("/v1/usuarios").with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("55566677720", cendoc, "[\"ELB\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("própria OM")));
        mvc.perform(post("/v1/usuarios").with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("55566677720", emaer, "[\"ELB\",\"ADM_OM\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Administrador Geral")));
        mvc.perform(post("/v1/usuarios").with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("55566677720", emaer, "[\"ELB\",\"PUB_BCA\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("exclusivo do CENDOC")));
        mvc.perform(post("/v1/usuarios").with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("55566677720", emaer, "[\"ELB\",\"APR_OM\",\"AUD_OM\"]")))
                .andExpect(status().isCreated());
    }

    @Test
    void oAdminLocalConcedeAprovadorAUsuarioDaOmENaoMexeEmOutroAdmin() throws Exception {
        long emaer = omRepository.findBySigla("EMAER").orElseThrow().getId();
        long idEditor = idDoEditorLocal();
        mvc.perform(put("/v1/usuarios/" + idEditor).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa EMAER\",\"omId\":" + emaer + ",\"ativo\":true,\"papeis\":[\"ELB\",\"APR_OM\"]}"))
                .andExpect(status().isOk());

        // outro administrador da mesma OM: só o admin geral mexe
        var outroAdmin = usuarioDa("EMAER", "66677788830", PapelEnum.ELB, PapelEnum.ADM_OM);
        mvc.perform(put("/v1/usuarios/" + outroAdmin.getId()).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"omId\":" + emaer + ",\"ativo\":true,\"papeis\":[\"ELB\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("administradores são mantidos")));
        mvc.perform(put("/v1/usuarios/" + outroAdmin.getId()).with(como(geral())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"omId\":" + emaer + ",\"ativo\":true,\"papeis\":[\"ELB\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void habilitacaoEDomSaoSoDoAdminGeral() throws Exception {
        mvc.perform(get("/v1/habilitacao-de-documentos").with(como(adminLocal()))).andExpect(status().isForbidden());
        mvc.perform(get("/v1/habilitacao-de-documentos").with(como(geral()))).andExpect(status().isOk());
        long emaer = omRepository.findBySigla("EMAER").orElseThrow().getId();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/v1/organizacoes-militares/" + emaer + "/distintivo")
                        .with(como(adminLocal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void oAuditorLocalSoVeOsAcessosDaPropriaOm() throws Exception {
        var auditorLocal = usuarioDa("EMAER", "77788899941", PapelEnum.ELB, PapelEnum.AUD_OM);
        mvc.perform(post("/v1/usuarios").with(como(geral())).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("88899900078", omRepository.findBySigla("BAGL").orElseThrow().getId(), "[\"ELB\"]")))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/auditoria/acessos").with(como(auditorLocal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].omSigla", everyItem(org.hamcrest.Matchers.is("EMAER"))))
                .andExpect(jsonPath("$.content[*].alvoCpf", not(hasItem("88899900078"))));
    }
}
