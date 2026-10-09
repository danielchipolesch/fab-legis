package intraer.fablegis.application.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// Regra de acesso: adicionar e remover anexo têm o mesmo rigor de editar o documento (parte normativa, metadados) --
// @PreAuthorize podeEditar, a mesma checagem de posse de DocumentoController. Listar é visualização: livre para qualquer
// usuário autenticado. Sem Spring: confere a anotação por reflexão (a regra em si, podeEditar, é de DocumentoAcessoService).
class AnexoControllerTest {

    private static Method metodo(String nome) {
        return Arrays.stream(AnexoController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nome)).findFirst().orElseThrow();
    }

    @Test
    void adicionarExigeAMesmaPosseDeEditarODocumento() {
        var regra = metodo("adicionar").getAnnotation(PreAuthorize.class);

        assertThat(regra).isNotNull();
        assertThat(regra.value()).isEqualTo("@documentoAcessoService.podeEditar(#documentoId, authentication)");
    }

    @Test
    void removerExigeAMesmaPosseDeEditarODocumento() {
        var regra = metodo("remover").getAnnotation(PreAuthorize.class);

        assertThat(regra).isNotNull();
        assertThat(regra.value()).isEqualTo("@documentoAcessoService.podeEditar(#documentoId, authentication)");
    }

    @Test
    void listarEVisualizacaoELivreParaQualquerUsuarioAutenticado() {
        assertThat(metodo("listar").getAnnotation(PreAuthorize.class)).isNull();
    }
}
