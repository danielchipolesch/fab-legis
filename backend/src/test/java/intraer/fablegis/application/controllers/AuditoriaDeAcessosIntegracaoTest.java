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

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Auditoria dos acessos de ponta a ponta (docs/autenticacao.md): criar usuário, conceder papel e redefinir senha deixam
// rastro, que só o AUDITOR lê.
class AuditoriaDeAcessosIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_NOVO = "22233344405";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private Usuario administrador() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    // Auditor GERAL (no CENDOC): o auditor de outra OM só enxerga a própria (ver EscopoDeAcessoIntegracaoTest).
    private Usuario auditor() {
        return usuarioRepository.findByCpf("11122233396").orElseGet(() -> {
            var u = new Usuario();
            u.setNome("Auditor");
            u.setCpf("11122233396");
            u.setSenhaHash("{noop}x");
            u.setOm(omRepository.findBySigla("CENDOC").orElseThrow());
            u.setPapeis(new HashSet<>(Set.of(PapelEnum.EDIT, PapelEnum.AUDITOR)));
            return usuarioRepository.save(u);
        });
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void criarConcederERedefinirSenhaFicamRegistradosEQuemNaoEAuditorNaoLe() throws Exception {
        long omId = omRepository.findBySigla("EMAER").orElseThrow().getId();
        var criado = mvc.perform(post("/v1/usuarios").with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Beltrano\",\"cpf\":\"" + CPF_NOVO + "\",\"senha\":\"senha-forte-1\",\"omId\":" + omId + ",\"papeis\":[\"EDIT\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(criado.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mvc.perform(put("/v1/usuarios/" + id).with(como(administrador())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Beltrano\",\"omId\":" + omId + ",\"ativo\":true,\"papeis\":[\"EDIT\",\"APROV\"]}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/v1/usuarios/" + id + "/senha")
                        .with(como(administrador())).contentType(MediaType.APPLICATION_JSON).content("{\"novaSenha\":\"outra-senha-2\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/auditoria/acessos").param("cpf", CPF_NOVO).with(como(auditor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.acao == 'CRIOU_USUARIO')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.acao == 'ALTEROU_PAPEIS')].detalhe").value(org.hamcrest.Matchers.hasItem(containsString("Concedido(s): APROV"))))
                .andExpect(jsonPath("$.content[?(@.acao == 'REDEFINIU_SENHA')]").isNotEmpty())
                .andExpect(jsonPath("$.content[0].atorId").value(administrador().getId()))
                .andExpect(jsonPath("$.content[0].omSigla").value("EMAER"));

        // Quem não é auditor (nem o administrador) não lê a trilha dos acessos.
        mvc.perform(get("/v1/auditoria/acessos").with(como(administrador()))).andExpect(status().isForbidden());
    }

    @Test
    void oAdministradorPadraoDoPrimeiroBootTemSeuRegistroDeCriacaoPeloSistema() throws Exception {
        mvc.perform(get("/v1/auditoria/acessos").param("cpf", administrador().getCpf()).param("acao", "CRIOU_USUARIO").with(como(auditor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].atorId").doesNotExist());
    }
}
