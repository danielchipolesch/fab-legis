package intraer.fablegis.domain.handlers;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.handlers.exceptions.*;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// O status de cada tipo de erro da API (docs/api-rest.md, "Erros"): erro de quem pediu é 4xx com o status certo; o que é
// falha do servidor é 500, com mensagem neutra (sem texto interno) -- e nunca mais 400 por padrão. Sem Spring Boot nem banco:
// MockMvc "standalone" com o GlobalExceptionHandler e um controller de mentira que lança cada exceção.
class GlobalExceptionHandlerTest {

    record Dados(@NotBlank String nome) {}

    @RestController
    @RequestMapping("/t")
    @Validated
    static class Falso {

        @PostMapping("/validar")
        void validar(@RequestBody @jakarta.validation.Valid Dados dados) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam String q) {
        }

        @GetMapping("/numero/{id}")
        void numero(@PathVariable Long id) {
        }

        @GetMapping("/lanca/{tipo}")
        void lanca(@PathVariable String tipo) throws Exception {
            switch (tipo) {
                case "regra" -> throw new RegraDeNegocioException("Elemento revogado é permanente.");
                case "argumento" -> throw new IllegalArgumentException("Page index must not be less than zero");
                case "estado" -> throw new IllegalStateException("Nenhuma regra registrada para o tipo X.");
                case "npe" -> throw new NullPointerException("detalhe interno que não pode vazar: tabela t_usuario");
                case "ordenacao" -> PropertyPath.from("naoExiste", String.class);
                case "ordenacao-embrulhada" -> {
                    try {
                        PropertyPath.from("naoExiste", String.class);
                    } catch (PropertyReferenceException e) {
                        throw new InvalidDataAccessApiUsageException("embrulhada", e);
                    }
                }
                case "uso-indevido" -> throw new InvalidDataAccessApiUsageException("Transação obrigatória");
                case "grande-demais" -> throw new DataIntegrityViolationException("x", new SQLException("valor muito longo", "22001"));
                case "duplicado" -> throw new DataIntegrityViolationException("x", new SQLException("duplicado", "23505"));
                case "versao" -> throw new ObjectOptimisticLockingFailureException(Documento.class, 1L);
                case "upload" -> throw new MaxUploadSizeExceededException(10_000_000L);
                case "desconexao" -> throw new IOException("Broken pipe");
                case "nao-encontrado" -> throw new ResourceNotFoundException("Documento não encontrado");
                case "entrada" -> throw new InvalidInputException("Orientação do anexo inválida.");
                case "congelado" -> throw new StatusCannotBeUpdatedException("Documento congelado.");
                case "negado" -> throw new AccessDeniedException("negado");
                case "conflito" -> throw new ConflitoEdicaoException("alterado por outro usuário");
                case "ocupado" -> throw new SistemaOcupadoException("ocupado", 5);
                case "renderizacao" -> throw new FalhaNaRenderizacaoException("Erro ao renderizar PDF: x", new RuntimeException());
                default -> throw new UnsupportedOperationException(tipo);
            }
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new Falso()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private org.springframework.test.web.servlet.ResultActions lanca(String tipo) throws Exception {
        return mvc.perform(get("/t/lanca/" + tipo));
    }

    // ─── Erros do próprio Spring MVC: o status certo, em português e sem texto interno ────────────

    @Test
    void validacaoDoCorpoE400ComOCampoNaMensagem() throws Exception {
        mvc.perform(post("/t/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Dados inválidos: nome:")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("org.springframework"))));
    }

    @Test
    void jsonMalformadoE400SemTextoDoJackson() throws Exception {
        mvc.perform(post("/t/validar").contentType(MediaType.APPLICATION_JSON).content("{nao-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O corpo da requisição está ilegível ou em formato inválido."));
    }

    @Test
    void parametroObrigatorioAusenteE400() throws Exception {
        mvc.perform(get("/t/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: q."));
    }

    @Test
    void valorDeTipoErradoNoCaminhoE400() throws Exception {
        mvc.perform(get("/t/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    @Test
    void metodoNaoSuportadoE405ComOHeaderAllow() throws Exception {
        mvc.perform(post("/t/parametro"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("O método POST não é suportado nesta rota."));
    }

    @Test
    void tipoDeConteudoNaoSuportadoE415() throws Exception {
        mvc.perform(post("/t/validar").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Tipo de conteúdo não suportado."));
    }

    @Test
    void rotaInexistenteE404() throws Exception {
        mvc.perform(get("/t/nao-existe/mesmo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rota não encontrada."));
    }

    @Test
    void uploadAcimaDoLimiteE413EmPortugues() throws Exception {
        lanca("upload").andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("O arquivo enviado é maior que o limite permitido."));
    }

    // ─── Erro de quem pediu que só estoura mais adiante ───────────────────────────────────────────

    @Test
    void regraDeNegocioE400ComAMensagemDaRegra() throws Exception {
        lanca("regra").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Elemento revogado é permanente."));
    }

    @Test
    void argumentoInvalidoE400ComAMensagemDeQuemLancou() throws Exception {
        lanca("argumento").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
    }

    @Test
    void ordenarPorPropriedadeInexistenteE400ComOSemSerEmbrulhadaNoSpringData() throws Exception {
        lanca("ordenacao").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Propriedade de ordenação ou filtro inválida."));
        lanca("ordenacao-embrulhada").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Propriedade de ordenação ou filtro inválida."));
    }

    @Test
    void valorGrandeDemaisParaOBancoE400() throws Exception {
        lanca("grande-demais").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Algum valor enviado é inválido: grande demais ou ausente."));
    }

    @Test
    void valorDuplicadoNoBancoE409() throws Exception {
        lanca("duplicado").andExpect(status().isConflict());
    }

    @Test
    void duasPessoasGravandoOMesmoDocumentoE409() throws Exception {
        lanca("versao").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Recarregue")));
    }

    // ─── Falha nossa: 500, mensagem neutra ────────────────────────────────────────────────────────

    @Test
    void bugInesperadoE500SemVazarODetalheInterno() throws Exception {
        lanca("npe").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.MENSAGEM_ERRO_INTERNO))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("t_usuario"))));
    }

    @Test
    void estadoInvalidoInternoE500() throws Exception {
        lanca("estado").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.MENSAGEM_ERRO_INTERNO));
    }

    @Test
    void mauUsoDaCamadaDeDadosQueNaoEDeOrdenacaoE500() throws Exception {
        lanca("uso-indevido").andExpect(status().isInternalServerError());
    }

    @Test
    void qualquerOutraExcecaoDesconhecidaE500() throws Exception {
        lanca("qualquer-coisa").andExpect(status().isInternalServerError());
    }

    @Test
    void falhaDeRenderizacaoE500ComAMensagemDeCausa() throws Exception {
        lanca("renderizacao").andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro ao renderizar PDF: x"));
    }

    @Test
    void clienteQueDesconectouNaoViraErroNemResposta() throws Exception {
        var resposta = lanca("desconexao").andReturn().getResponse();

        assertThat(resposta.getContentAsString()).isEmpty();
    }

    // ─── O que já tinha handler próprio continua com o mesmo status ───────────────────────────────

    @Test
    void asExcecoesDoDominioMantemSeusStatus() throws Exception {
        lanca("nao-encontrado").andExpect(status().isNotFound());
        lanca("entrada").andExpect(status().isNotAcceptable());
        lanca("congelado").andExpect(status().isForbidden());
        lanca("negado").andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Acesso negado."));
        lanca("conflito").andExpect(status().isConflict());
        lanca("ocupado").andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "5"));
    }
}
