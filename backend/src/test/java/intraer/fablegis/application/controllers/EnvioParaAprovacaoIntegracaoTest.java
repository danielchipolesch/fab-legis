package intraer.fablegis.application.controllers;

import intraer.fablegis.IntegracaoComPostgresBase;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
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

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Enviar para aprovação" sem escolher ninguém, de ponta a ponta (docs/ciclo-de-vida.md): qualquer aprovador da OM do documento
// aprova (não o de outra OM), e qualquer publicador (CENDOC) enxerga a fila de publicação.
class EnvioParaAprovacaoIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_APROVADOR = "10120230364";
    private static final String CPF_APROVADOR_DE_OUTRA_OM = "20230340431";
    private static final String CPF_PUBLICADOR = "30340450509";
    private static final String CPF_PUBLICADOR_DO_BCA = "40450560600";
    private static final String CPF_PUBLICADOR_DE_OUTRA_OM = "50560670701";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // O administrador padrão (ADMIN + EDIT, do CENDOC) é o autor do documento do teste.
    private Usuario autor() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isSistema()).findFirst().orElseThrow();
    }

    private Usuario pessoa(String sigla, String cpf, PapelEnum... papeis) {
        return usuarioRepository.findByCpf(cpf).orElseGet(() -> {
            var u = new Usuario();
            u.setNome("Pessoa " + cpf);
            u.setCpf(cpf);
            u.setSenhaHash("{noop}x");
            u.setOm(omRepository.findBySigla(sigla).orElseThrow());
            var conjunto = EnumSet.noneOf(PapelEnum.class);
            conjunto.addAll(java.util.List.of(papeis));
            u.setPapeis(conjunto);
            return usuarioRepository.save(u);
        });
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long novaNpaMinutada(String identificacao) throws Exception {
        long npa = especieRepository.findAll().stream().filter(e -> "NPA".equals(e.getSigla())).findFirst().orElseThrow().getId();
        var criado = mvc.perform(post("/v1/documentos").with(como(autor())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEspecieNormativa\": " + npa + ", \"identificacao\": \"" + identificacao + "\", \"tituloDocumento\": \"Teste\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(autor())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"MINUTA\"}"))
                .andExpect(status().isOk());
        return id;
    }

    private String situacaoDe(long id) {
        return jdbc.queryForObject("select st_situacao_local from t_documento where id_documento = ?", String.class, id);
    }

    @Test
    void enviarParaAprovacaoNaoEscolheNinguemEQualquerAprovadorDaOmAprova() throws Exception {
        long id = novaNpaMinutada("NPA-APROV-01");
        var aprovador = pessoa("CENDOC", CPF_APROVADOR, PapelEnum.ELB, PapelEnum.APR_OM);
        var deOutraOm = pessoa("EMAER", CPF_APROVADOR_DE_OUTRA_OM, PapelEnum.ELB, PapelEnum.APR_OM);

        // sem revisorId nenhum: "Enviar para aprovação"
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(autor())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"EM_REVISAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoLocal").value("EM_REVISAO"));

        // a fila de revisão é por OM: o aprovador da OM do documento vê; o de outra OM, não
        mvc.perform(get("/v1/documentos/minha-revisao").with(como(aprovador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idDocumento", hasItem((int) id)));
        mvc.perform(get("/v1/documentos/minha-revisao").with(como(deOutraOm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idDocumento", not(hasItem((int) id))));

        // o aprovador de outra OM não aprova; o da OM do documento, sim
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(deOutraOm)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"EM_PUBLICACAO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(aprovador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"EM_PUBLICACAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoLocal").value("EM_PUBLICACAO"))
                .andExpect(jsonPath("$.aprovadorId").value(aprovador.getId().intValue()));
        assertThat(situacaoDe(id)).isEqualTo("EM_PUBLICACAO");
    }

    // A NPA é de uso interno da OM: quem a publica é o Publicador do Boletim Interno da PRÓPRIA OM do documento -- não o Publicador do BCA
    // (CENDOC) nem o de outra OM.
    @Test
    void aNpaAprovadaVaiParaAFilaDoPublicadorDoBoletimInternoDaPropriaOm() throws Exception {
        long id = novaNpaMinutada("NPA-APROV-02");
        var aprovador = pessoa("CENDOC", CPF_APROVADOR, PapelEnum.ELB, PapelEnum.APR_OM);
        var publicadorDaOm = pessoa("CENDOC", CPF_PUBLICADOR, PapelEnum.ELB, PapelEnum.PUB_BI);
        var publicadorDoBca = pessoa("CENDOC", CPF_PUBLICADOR_DO_BCA, PapelEnum.ELB, PapelEnum.PUB_BCA);
        var publicadorDeOutraOm = pessoa("EMAER", CPF_PUBLICADOR_DE_OUTRA_OM, PapelEnum.ELB, PapelEnum.PUB_BI);
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(autor())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"EM_REVISAO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(aprovador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacaoLocal\":\"EM_PUBLICACAO\"}"))
                .andExpect(status().isOk());

        // a fila de publicação: só o publicador do Boletim Interno da OM do documento a tem
        mvc.perform(get("/v1/documentos/minha-publicacao").with(como(publicadorDaOm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idDocumento", hasItem((int) id)));
        for (var semAcesso : new Usuario[]{publicadorDoBca, publicadorDeOutraOm, aprovador}) {
            mvc.perform(get("/v1/documentos/minha-publicacao").with(como(semAcesso)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].idDocumento", not(hasItem((int) id))));
        }

        // e só ele publica (Boletim Interno: número e data), nunca o do BCA nem o de outra OM
        String publicar = "{\"situacaoLocal\":\"SEM_ETAPA\",\"numeroBoletimInterno\":15,\"dataBoletimInterno\":\"2026-10-14\"}";
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(publicadorDoBca)).contentType(MediaType.APPLICATION_JSON).content(publicar))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(publicadorDeOutraOm)).contentType(MediaType.APPLICATION_JSON).content(publicar))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/v1/documentos/" + id + "/status").with(como(publicadorDaOm)).contentType(MediaType.APPLICATION_JSON).content(publicar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoBca").value("PUBLICADO"));
    }

    // Já o Publicador do BCA (CENDOC) vê e publica as espécies convencionais: a fila dele não traz NPA (coberto acima) e a do Publicador do
    // Boletim Interno não traz convencional (DocumentoSpecificationsTest e DocumentoAcessoServiceTest).
}
