package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// O CPF é o login e o identificador do usuário: é único (docs/autenticacao.md), com ou sem máscara na digitação.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock OrganizacaoMilitarRepository organizacaoMilitarRepository;
    @Mock PostoGraduacaoRepository postoGraduacaoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UsuarioService service;

    private static UsuarioCreateRequestDto pedido(String cpf) {
        return new UsuarioCreateRequestDto("Fulano", null, cpf, null, null, "senha-forte-1", 1L, null);
    }

    @Test
    void cpfJaCadastradoEhRecusadoMesmoDigitadoComMascara() {
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(pedido("111.444.777-35")))
                .isInstanceOf(ResourceAlreadyExistsException.class).hasMessageContaining("CPF");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cpfNovoEhGravadoSoComOsDigitos() {
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(false);
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(new OrganizacaoMilitar()));
        when(passwordEncoder.encode("senha-forte-1")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criar(pedido("111.444.777-35"));

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getCpf()).isEqualTo("11144477735");
    }
}
