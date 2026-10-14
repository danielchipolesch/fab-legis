package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cache de PDF/HTML em tramitação no MinIO (docs/exportacao-pdf.md, docs/roadmap.md): a chave do
// cache tem de mudar a cada alteração de conteúdo -- a edição colaborativa (/elementos/{id}/conteudo)
// não toca Documento.dtAlteracao/versao (ver DocumentoConcorrenciaService), então é isso que este
// teste prende, não só o caminho feliz de cache hit/miss.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoRenderCacheServiceTest {

    @Mock MinioClient minioClient;
    @Mock ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;
    @Mock ItemPartePreliminarRepository itemPartePreliminarRepository;
    @Mock AnexoRepository anexoRepository;

    @InjectMocks DocumentoRenderCacheService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucket", "fab-legis-teste");
        ReflectionTestUtils.setField(service, "ttlDias", 2);

        when(itemPartePreliminarRepository.findIdsEDataAtualizacaoByDocumentoId(anyLong())).thenReturn(List.of());
        when(itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(anyLong())).thenReturn(List.of());
        when(anexoRepository.findByDocumentoIdOrderByOrdemAsc(anyLong())).thenReturn(List.of());
    }

    private static Documento documento(long id, int versao, long dtAlteracaoMillis) {
        Documento doc = new Documento();
        doc.setId(id);
        doc.setVersao(versao);
        doc.setDtAlteracao(new Timestamp(dtAlteracaoMillis));
        return doc;
    }

    @Test
    void cacheHitDevolveOsBytesArmazenados() throws Exception {
        byte[] bytes = "pdf-cacheado".getBytes();
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(new GetObjectResponse(
                new Headers.Builder().build(), "bucket", "region", "chave", new ByteArrayInputStream(bytes)));

        var resultado = service.buscar(documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        assertThat(resultado).contains(bytes);
    }

    @Test
    void cacheMissOuErroDeIoDevolveVazioSemLancar() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("objeto não encontrado"));

        var resultado = service.buscar(documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        assertThat(resultado).isEmpty();
    }

    @Test
    void salvarNuncaLancaMesmoSeOMinioFalhar() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("minio fora do ar"));

        assertThatCode(() -> service.salvar(
                documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF, "pdf".getBytes()))
                .doesNotThrowAnyException();
    }

    @Test
    void chaveMudaQuandoAVersaoOuADataDeAlteracaoDoDocumentoMuda() throws Exception {
        String chave1 = chaveUsada(documento(1L, 1, 1000L));
        String chave2 = chaveUsada(documento(1L, 2, 1000L));
        String chave3 = chaveUsada(documento(1L, 1, 2000L));

        assertThat(chave1).isNotEqualTo(chave2).isNotEqualTo(chave3);
    }

    @Test
    void chaveMudaQuandoUmItemDeConteudoEAtualizado() throws Exception {
        when(itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(1L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, LocalDateTime.of(2026, 1, 1, 0, 0)}));
        String chaveAntes = chaveUsada(documento(1L, 1, 1000L));

        when(itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(1L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, LocalDateTime.of(2026, 1, 2, 0, 0)}));
        String chaveDepois = chaveUsada(documento(1L, 1, 1000L));

        assertThat(chaveAntes).isNotEqualTo(chaveDepois);
    }

    @Test
    void chaveMudaQuandoUmAnexoEAdicionadoOuRemovido() throws Exception {
        String semAnexo = chaveUsada(documento(1L, 1, 1000L));

        Anexo anexo = new Anexo();
        anexo.setId(99L);
        when(anexoRepository.findByDocumentoIdOrderByOrdemAsc(1L)).thenReturn(List.of(anexo));
        String comAnexo = chaveUsada(documento(1L, 1, 1000L));

        assertThat(semAnexo).isNotEqualTo(comAnexo);
    }

    @Test
    void mesmoConteudoProduzAMesmaChaveEntreChamadas() throws Exception {
        String chave1 = chaveUsada(documento(1L, 1, 1000L));
        String chave2 = chaveUsada(documento(1L, 1, 1000L));

        assertThat(chave1).isEqualTo(chave2);
    }

    // Força um cache miss e devolve a chave (GetObjectArgs.object()) que o serviço calculou --
    // único jeito de observar o fingerprint, que é privado.
    private String chaveUsada(Documento documento) throws Exception {
        reset(minioClient);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("miss"));
        service.buscar(documento, DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        ArgumentCaptor<GetObjectArgs> captor = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(captor.capture());
        return captor.getValue().object();
    }
}
