package intraer.fablegis.application.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// Regra de acesso (docs/autenticacao.md): tudo o que altera o conteúdo do documento passa pela MESMA checagem,
// DocumentoAcessoService.podeEditar -- que barra pela etapa --, e o PDF da portaria é um passo à parte, do publicador
// atribuído. Sem Spring: confere as anotações por reflexão (as regras em si são testadas em DocumentoAcessoServiceTest).
class DocumentoControllerAcessoTest {

    private static String regraDe(Class<?> controller, String metodo) {
        Method m = Arrays.stream(controller.getDeclaredMethods())
                .filter(x -> x.getName().equals(metodo)).findFirst().orElseThrow();
        var regra = m.getAnnotation(PreAuthorize.class);
        assertThat(regra).as("@PreAuthorize em %s.%s", controller.getSimpleName(), metodo).isNotNull();
        return regra.value();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "saveSecoes", "atualizarConteudoElemento", "obterConteudoElemento",
            "addItemAnexoParteNormativa", "streamPresenca", "podeEditar"})
    void editarOConteudoDoDocumentoExigePodeEditar(String metodo) {
        assertThat(regraDe(DocumentoController.class, metodo))
                .startsWith("@documentoAcessoService.podeEditar(");
    }

    @Test
    void emendarExigePodeEditar() {
        assertThat(regraDe(EmendaController.class, "emendar")).startsWith("@documentoAcessoService.podeEditar(");
    }

    @Test
    void oCabecalhoDaNpaExigePodeEditar() {
        assertThat(regraDe(NpaController.class, "atualizar")).startsWith("@documentoAcessoService.podeEditar(");
    }

    @Test
    void oPdfDaPortariaEDoPublicadorAtribuidoNaoDeQuemEdita() {
        assertThat(regraDe(DocumentoController.class, "uploadPortariaPdf"))
                .isEqualTo("@documentoAcessoService.podeEnviarPortaria(#id, authentication)");
    }

    // Habilitação de documentos: só o administrador (ADMIN, o CENDOC) gerencia quais OMs criam quais espécies; qualquer pessoa
    // autenticada consulta o que a própria OM pode criar.
    @Test
    void gerirAHabilitacaoDeDocumentosExigeAdmin() {
        assertThat(regraDe(HabilitacaoDeDocumentosController.class, "listar")).isEqualTo("hasRole('ADMIN')");
        assertThat(regraDe(HabilitacaoDeDocumentosController.class, "definir")).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void consultarOQueAPropriaOmPodeCriarNaoExigePapel() {
        var minhas = Arrays.stream(HabilitacaoDeDocumentosController.class.getDeclaredMethods())
                .filter(x -> x.getName().equals("minhas")).findFirst().orElseThrow();
        assertThat(minhas.getAnnotation(PreAuthorize.class)).isNull();
    }
}
