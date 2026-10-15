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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Painel Gerencial de ponta a ponta (docs/funcionalidades.md): o Painel Gerencial Geral vê todas as OMs e filtra por uma; o Painel Gerencial da OM, só a
// própria, sem visão geral; quem não tem o papel do painel não vê nada.
class PainelGerencialIntegracaoTest extends IntegracaoComPostgresBase {

    private static final String CPF_PNL_DA_OM = "50617283916";
    private static final String CPF_SEM_PAPEL = "61728394027";

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired OrganizacaoMilitarRepository omRepository;
    @Autowired EspecieNormativaRepository especieRepository;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    long bant;
    long bafl;
    String identificacaoEmAprovacao;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        bant = omRepository.findBySigla("BANT").orElseThrow().getId();
        bafl = omRepository.findBySigla("BAFL").orElseThrow().getId();
        // Cenário determinístico nas duas OMs do teste: o que um teste anterior da classe deixou nelas vai para o CENDOC.
        long cendoc = omRepository.findBySigla("CENDOC").orElseThrow().getId();
        jdbc.update("update t_documento set om_id = ? where om_id in (?, ?)", cendoc, bant, bafl);
        // Três espécies convencionais (publicada, em aprovação e rascunho) e duas NPAs (uma em cada OM): as NPAs são de uso interno da OM e
        // não entram no Painel Gerencial Geral.
        long publicada = novoDocumento("DCA", null);
        long emAprovacao = novoDocumento("DCA", null);
        long rascunho = novoDocumento("DCA", null);
        long npaDaBant = novoDocumento("NPA", "NPA-PAINEL-01");
        long npaDaBafl = novoDocumento("NPA", "NPA-PAINEL-02");
        identificacaoEmAprovacao = jdbc.queryForObject("select tx_identificacao from t_documento where id_documento = ?", String.class, emAprovacao);
        jdbc.update("update t_documento set om_id = ?, st_situacao_local = 'SEM_ETAPA', st_situacao_bca = 'PUBLICADO',"
                + " dt_criacao = now() - interval '10 days', dt_em_revisao = now() - interval '8 days', dt_aprovacao = now() - interval '6 days',"
                + " dt_em_publicacao = now() - interval '6 days', dt_publicacao = now() - interval '4 days' where id_documento = ?", bant, publicada);
        jdbc.update("update t_documento set om_id = ?, st_situacao_local = 'EM_REVISAO', dt_criacao = now() - interval '5 days',"
                + " dt_em_revisao = now() - interval '3 days' where id_documento = ?", bant, emAprovacao);
        jdbc.update("update t_documento set om_id = ?, dt_criacao = now() - interval '2 days' where id_documento = ?", bafl, rascunho);
        jdbc.update("update t_documento set om_id = ? where id_documento = ?", bant, npaDaBant);
        jdbc.update("update t_documento set om_id = ? where id_documento = ?", bafl, npaDaBafl);
    }

    private Usuario painelGeral() {
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

    private long assuntoBasico() {
        return jdbc.queryForObject("select min(id_assunto_basico) from t_assunto_basico", Long.class);
    }

    private static RequestPostProcessor como(Usuario usuario) {
        var principal = new UsuarioPrincipal(usuario);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // Cria um documento da espécie pela API: convencional (com um assunto básico) ou NPA (com a identificação em texto livre).
    private long novoDocumento(String sigla, String identificacao) throws Exception {
        long especie = especieRepository.findAll().stream().filter(e -> sigla.equals(e.getSigla())).findFirst().orElseThrow().getId();
        String corpo = identificacao != null
                ? "{\"idEspecieNormativa\": " + especie + ", \"identificacao\": \"" + identificacao + "\", \"tituloDocumento\": \"Teste\"}"
                : "{\"idEspecieNormativa\": " + especie + ", \"idAssuntoBasico\": " + assuntoBasico() + ", \"tituloDocumento\": \"Teste\"}";
        var criado = mvc.perform(post("/v1/documentos").with(como(painelGeral())).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(criado.replaceAll("(?s).*\"idDocumento\":(\\d+).*", "$1"));
    }

    @Test
    void oPapelGeralDoPainelVeUmaOmEFiltraPorEla() throws Exception {
        // As NPAs são de uso interno da OM: o Painel Gerencial Geral nunca as conta, nem filtrando por uma OM -- só as 2 convencionais da BANT.
        mvc.perform(get("/v1/painel-gerencial").param("omId", String.valueOf(bant)).with(como(painelGeral())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo.omSigla").value("BANT"))
                .andExpect(jsonPath("$.escopo.podeFiltrarPorOm").value(true))
                .andExpect(jsonPath("$.escopo.podeFiltrarPorTipo").value(false))
                .andExpect(jsonPath("$.totais.criados").value(2))
                .andExpect(jsonPath("$.totais.publicados").value(1))
                .andExpect(jsonPath("$.totais.emTramitacao").value(1))
                .andExpect(jsonPath("$.totais.vigentes").value(1))
                .andExpect(jsonPath("$.filas.aguardandoAprovacao").value(1))
                .andExpect(jsonPath("$.filas.aguardandoPublicacao").value(0))
                .andExpect(jsonPath("$.tempos.elaboracao.n").value(2))
                .andExpect(jsonPath("$.tempos.elaboracao.mediana").value(2.0))
                .andExpect(jsonPath("$.tempos.aprovacao.n").value(1))
                .andExpect(jsonPath("$.tempos.aprovacao.mediana").value(2.0))
                .andExpect(jsonPath("$.tempos.publicacao.mediana").value(2.0))
                .andExpect(jsonPath("$.gargalos[0].identificacao").value(identificacaoEmAprovacao))
                .andExpect(jsonPath("$.gargalos[0].etapa").value("APROVACAO"))
                .andExpect(jsonPath("$.gargalos[0].dias").value(3))
                .andExpect(jsonPath("$.porEspecie[0].sigla").value("DCA"))
                .andExpect(jsonPath("$.porEspecie.length()").value(1))
                .andExpect(jsonPath("$.porOm.length()").value(1));
    }

    @Test
    void semFiltroOPapelGeralVeTodasAsOmsEAPorOmListaAsDuas() throws Exception {
        mvc.perform(get("/v1/painel-gerencial").with(como(painelGeral())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo.omId").doesNotExist())
                .andExpect(jsonPath("$.porOm[*].sigla", hasItem("BANT")))
                .andExpect(jsonPath("$.porOm[*].sigla", hasItem("BAFL")))
                .andExpect(jsonPath("$.porEspecie[*].sigla", not(hasItem("NPA"))));
    }

    @Test
    void oPapelGeralNaoPedeOModuloDasNpas() throws Exception {
        mvc.perform(get("/v1/painel-gerencial").param("tipoDeEspecie", "COMUNICACAO_OFICIAL_PADRONIZADA").with(como(painelGeral())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/painel-gerencial").param("tipoDeEspecie", "CONVENCIONAL").with(como(painelGeral())))
                .andExpect(status().isOk());
    }

    @Test
    void oPapelDaOmDoPainelVeSoAPropriaOmComAsNpasEFiltraPorModulo() throws Exception {
        var daOm = pessoa("BANT", CPF_PNL_DA_OM, PapelEnum.ELB, PapelEnum.PNL_OM);

        // as 2 convencionais e a NPA da BANT (a NPA da BAFL não é dele)
        mvc.perform(get("/v1/painel-gerencial").with(como(daOm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo.omSigla").value("BANT"))
                .andExpect(jsonPath("$.escopo.podeFiltrarPorOm").value(false))
                .andExpect(jsonPath("$.escopo.podeFiltrarPorTipo").value(true))
                .andExpect(jsonPath("$.totais.criados").value(3))
                .andExpect(jsonPath("$.porEspecie[*].sigla", hasItem("NPA")))
                .andExpect(jsonPath("$.porOm.length()").value(1));
        mvc.perform(get("/v1/painel-gerencial").param("tipoDeEspecie", "COMUNICACAO_OFICIAL_PADRONIZADA").with(como(daOm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo.tipoDeEspecie").value("COMUNICACAO_OFICIAL_PADRONIZADA"))
                .andExpect(jsonPath("$.totais.criados").value(1))
                .andExpect(jsonPath("$.porEspecie[0].sigla").value("NPA"));
        mvc.perform(get("/v1/painel-gerencial").param("tipoDeEspecie", "CONVENCIONAL").with(como(daOm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totais.criados").value(2))
                .andExpect(jsonPath("$.porEspecie[*].sigla", not(hasItem("NPA"))));

        // pedir a própria OM é aceito; pedir outra (a visão geral não existe para ele), não
        mvc.perform(get("/v1/painel-gerencial").param("omId", String.valueOf(bant)).with(como(daOm))).andExpect(status().isOk());
        mvc.perform(get("/v1/painel-gerencial").param("omId", String.valueOf(bafl)).with(como(daOm))).andExpect(status().isForbidden());
    }

    @Test
    void quemNaoTemOPapelDoPainelNaoAcessa() throws Exception {
        mvc.perform(get("/v1/painel-gerencial").with(como(pessoa("BANT", CPF_SEM_PAPEL, PapelEnum.ELB, PapelEnum.ADM_OM, PapelEnum.AUD_OM))))
                .andExpect(status().isForbidden());
    }

    @Test
    void periodoInvertidoEhRecusado() throws Exception {
        mvc.perform(get("/v1/painel-gerencial").param("dataInicio", "2026-05-02").param("dataFim", "2026-05-01").with(como(painelGeral())))
                .andExpect(status().is4xxClientError());
    }
}
