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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Revisão periódica de acessos de ponta a ponta (docs/autenticacao.md): o admin geral confere os admins locais; o admin local, os
// demais da própria OM; manter ou retirar grava a data e vai para a auditoria de acessos; nada se concede por aqui.
class RevisaoDeAcessoIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_ADMIN_LOCAL = "99900011112";
    private static final String CPF_APROVADOR_LOCAL = "12312312387";
    private static final String CPF_APROVADOR_OUTRA_OM = "32132132178";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;
    @Autowired JdbcTemplate jdbc;

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
            u.setNome("Pessoa " + sigla + " " + cpf);
            u.setCpf(cpf);
            u.setSenhaHash("{noop}x");
            u.setOm(omRepository.findBySigla(sigla).orElseThrow());
            var conjunto = EnumSet.noneOf(PapelEnum.class);
            conjunto.addAll(java.util.List.of(papeis));
            u.setPapeis(conjunto);
            u.setDtRevisaoAcesso(new java.sql.Timestamp(System.currentTimeMillis()));
            return usuarioRepository.save(u);
        });
    }

    private Usuario adminLocal() {
        return usuarioDa("BAGL", CPF_ADMIN_LOCAL, PapelEnum.ELB, PapelEnum.ADM_OM);
    }

    private Usuario aprovadorLocal() {
        return usuarioDa("BAGL", CPF_APROVADOR_LOCAL, PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.AUD_OM);
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // A lista de usuários já diz, para quem pergunta, quem ele revisa (revisavel) e o motivo da pendência: nada a pedir.
    @Test
    void aListaDeUsuariosMarcaSoQuemCadaRevisorConferee() throws Exception {
        adminLocal();
        aprovadorLocal();
        usuarioDa("BABE", CPF_APROVADOR_OUTRA_OM, PapelEnum.ELB, PapelEnum.APR_OM);

        // o admin geral confere o admin local, não os aprovadores das outras OMs
        mvc.perform(get("/v1/usuarios").with(como(geral())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_ADMIN_LOCAL + "')].revisavel").value(true))
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_APROVADOR_LOCAL + "')].revisavel").value(false))
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_APROVADOR_OUTRA_OM + "')].revisavel").value(false));

        // o admin local confere os demais da própria OM (não ele mesmo, e nem enxerga a outra OM)
        mvc.perform(get("/v1/usuarios").with(como(adminLocal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_APROVADOR_LOCAL + "')].revisavel").value(true))
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_ADMIN_LOCAL + "')].revisavel").value(false))
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_APROVADOR_OUTRA_OM + "')]").isEmpty());
    }

    @Test
    void oMotivoDaRevisaoVemDaListaDeUsuariosSemNingemPedirNada() throws Exception {
        var aprovador = aprovadorLocal();
        jdbc.update("update t_usuario set dt_revisao_acesso = now() - interval '200 days' where id_usuario = ?", aprovador.getId());

        mvc.perform(get("/v1/usuarios").with(como(adminLocal())))
                .andExpect(jsonPath("$[?(@.cpf == '" + CPF_APROVADOR_LOCAL + "')].motivoDaRevisao").value("REVISAO_VENCIDA"));

        // revisar (manter) volta a "em dia", e continua revisável por quem o confere
        mvc.perform(post("/v1/revisao-de-acessos/" + aprovador.getId()).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\",\"APR_OM\",\"AUD_OM\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoDaRevisao").value("NENHUM"))
                .andExpect(jsonPath("$.revisavel").value(true));
    }

    @Test
    void retirarUmPapelNaRevisaoVaiParaAAuditoriaEQuemNaoRevisaNaoPode() throws Exception {
        var aprovador = aprovadorLocal();

        // quem não é o revisor dele (o admin geral, ou outro aprovador) leva 403
        mvc.perform(post("/v1/revisao-de-acessos/" + aprovador.getId()).with(como(geral())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\",\"APR_OM\",\"AUD_OM\"]}"))
                .andExpect(status().isForbidden());

        // o admin local mantém o aprovador e retira o auditor
        mvc.perform(post("/v1/revisao-de-acessos/" + aprovador.getId()).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\",\"APR_OM\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis", hasItem("APR_OM")))
                .andExpect(jsonPath("$.papeis", not(hasItem("AUD_OM"))));

        var papeis = jdbc.queryForList("select sg_papel from t_usuario_papel where usuario_id = ?", String.class, aprovador.getId());
        assertThat(papeis).containsExactlyInAnyOrder("ELB", "APR_OM");

        mvc.perform(get("/v1/auditoria/acessos").param("cpf", CPF_APROVADOR_LOCAL).param("acao", "REVISOU_ACESSO")
                        .with(como(usuarioDa("CENDOC", "11122233396", PapelEnum.ELB, PapelEnum.AUD_GER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].detalhe").value(containsString("Retirou: AUD_OM")))
                .andExpect(jsonPath("$.content[0].atorId").value(adminLocal().getId()));
    }

    @Test
    void aRevisaoNuncaConcedeUmPapel() throws Exception {
        var aprovador = aprovadorLocal();
        mvc.perform(post("/v1/revisao-de-acessos/" + aprovador.getId()).with(como(adminLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\",\"APR_OM\",\"PUB_BCA\"]}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(containsString("só mantém ou retira")));
    }

    @Test
    void ninguemRevisaOProprioAcessoEQuemNaoEAdminNaoRevisa() throws Exception {
        var admin = adminLocal();
        mvc.perform(post("/v1/revisao-de-acessos/" + admin.getId()).with(como(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\",\"ADM_OM\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/revisao-de-acessos/" + admin.getId()).with(como(aprovadorLocal())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papeis\":[\"ELB\"]}"))
                .andExpect(status().isForbidden());
    }
}
