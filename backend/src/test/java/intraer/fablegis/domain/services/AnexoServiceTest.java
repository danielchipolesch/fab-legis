package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Anexo de imagem: a orientação da página (retrato/paisagem) é escolhida no upload; sem escolha, vem da proporção da
// imagem (docs/dominio.md). A orientação não mexe na numeração: Anexo.ordem segue sequencial.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnexoServiceTest {

    @Mock AnexoRepository anexoRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock ImagemService imagemService;
    @Mock DocumentoStatusService documentoStatusService;

    @InjectMocks AnexoService service;

    private Documento documento;

    @BeforeEach
    void preparar() throws Exception {
        documento = new Documento();
        documento.setId(7L);
        documento.setSituacaoLocal(SituacaoLocalEnum.MINUTA);
        when(documentoRepository.findById(7L)).thenReturn(Optional.of(documento));
        when(imagemService.uploadImagem(any())).thenReturn("http://minio/bucket/imagem.png");
        when(anexoRepository.findMaxOrdemByDocumentoId(7L)).thenReturn(0);
        when(anexoRepository.save(any(Anexo.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static MockMultipartFile png(int largura, int altura) throws Exception {
        var saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB), "png", saida);
        return new MockMultipartFile("arquivo", "imagem.png", "image/png", saida.toByteArray());
    }

    @Test
    void aOrientacaoEnviadaVaiParaOAnexo() throws Exception {
        // Imagem alta, mas o usuário escolheu paisagem: a escolha vale (sem diferenciar maiúsculas).
        AnexoResponseDto dto = service.adicionar(7L, "Organograma", png(100, 200), "paisagem");

        assertThat(dto.orientacao()).isEqualTo(OrientacaoDoAnexo.PAISAGEM);
    }

    @Test
    void semOrientacaoUmaImagemLargaFicaEmPaisagem() throws Exception {
        assertThat(service.adicionar(7L, "Fluxograma", png(400, 200), null).orientacao())
                .isEqualTo(OrientacaoDoAnexo.PAISAGEM);
        assertThat(service.adicionar(7L, "Fluxograma", png(400, 200), "  ").orientacao())
                .isEqualTo(OrientacaoDoAnexo.PAISAGEM);
    }

    @Test
    void semOrientacaoUmaImagemAltaOuQuadradaFicaEmRetrato() throws Exception {
        assertThat(service.adicionar(7L, "Alta", png(200, 400), null).orientacao()).isEqualTo(OrientacaoDoAnexo.RETRATO);
        assertThat(service.adicionar(7L, "Quadrada", png(300, 300), null).orientacao()).isEqualTo(OrientacaoDoAnexo.RETRATO);
    }

    @Test
    void semOrientacaoUmArquivoIlegivelFicaEmRetrato() throws Exception {
        var lixo = new MockMultipartFile("arquivo", "x.png", "image/png", new byte[]{1, 2, 3, 4});

        assertThat(service.adicionar(7L, "Ilegivel", lixo, null).orientacao()).isEqualTo(OrientacaoDoAnexo.RETRATO);
    }

    @Test
    void orientacaoInvalidaERejeitadaSemEnviarOArquivo() throws Exception {
        assertThatThrownBy(() -> service.adicionar(7L, "Anexo", png(100, 100), "DIAGONAL"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("RETRATO ou PAISAGEM");

        verify(imagemService, never()).uploadImagem(any());
        verify(anexoRepository, never()).save(any());
    }

    @Test
    void aOrientacaoNaoAlteraANumeracaoDosAnexos() throws Exception {
        when(anexoRepository.findMaxOrdemByDocumentoId(7L)).thenReturn(2);

        var paisagem = service.adicionar(7L, "Terceiro", png(400, 200), "PAISAGEM");

        // Já havia 2 anexos (ANEXO II e III nas convencionais): o novo é o 3º, em qualquer orientação.
        assertThat(paisagem.ordem()).isEqualTo(3);
        var salvo = ArgumentCaptor.forClass(Anexo.class);
        verify(anexoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getOrdem()).isEqualTo(3);
        assertThat(salvo.getValue().getTitulo()).isEqualTo("Terceiro");
    }

    @Test
    void anexarEmRascunhoMudaODocumentoParaMinuta() throws Exception {
        documento.setSituacaoLocal(SituacaoLocalEnum.RASCUNHO);

        service.adicionar(7L, "Anexo", png(100, 100), null);

        verify(documentoStatusService).changeStatus(eq(7L), any());
    }

    @Test
    void anexarForaDoRascunhoNaoMudaASituacao() throws Exception {
        service.adicionar(7L, "Anexo", png(100, 100), null);

        verify(documentoStatusService, never()).changeStatus(any(), any());
    }
}
