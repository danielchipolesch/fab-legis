package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
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
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cache de PDF/HTML em tramitação no armazenamento de objetos (docs/exportacao-pdf.md, docs/roadmap.md): a chave do
// cache tem de mudar a cada alteração de conteúdo -- a edição colaborativa (/elementos/{id}/conteudo)
// não toca Documento.dtAlteracao/versao (ver DocumentoConcorrenciaService), então é isso que este
// teste prende, não só o caminho feliz de cache hit/miss. E o cache expira: tudo em cache/ mais velho que o
// prazo (app.pdf.cache.ttl-dias) é apagado pela limpeza diária.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoRenderCacheServiceTest {

    @Mock S3Client s3Client;
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
    void cacheHitDevolveOsBytesArmazenados() {
        byte[] bytes = "pdf-cacheado".getBytes();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

        var resultado = service.buscar(documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        assertThat(resultado).contains(bytes);
    }

    @Test
    void cacheMissOuErroDeIoDevolveVazioSemLancar() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("objeto não encontrado"));

        var resultado = service.buscar(documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        assertThat(resultado).isEmpty();
    }

    @Test
    void salvarNuncaLancaMesmoSeOArmazenamentoFalhar() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("armazenamento fora do ar"));

        assertThatCode(() -> service.salvar(
                documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.PDF, "pdf".getBytes()))
                .doesNotThrowAnyException();
    }

    @Test
    void salvarGravaSobOPrefixoCacheComOTipoDeConteudoDoFormato() {
        service.salvar(documento(1L, 1, 1000L), DocumentoRenderCacheService.TipoDeRenderizacao.HTML, "<p>x</p>".getBytes());

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("fab-legis-teste");
        assertThat(captor.getValue().key()).startsWith("cache/html/1/").endsWith(".html");
        assertThat(captor.getValue().contentType()).isEqualTo("text/html; charset=UTF-8");
    }

    @Test
    void chaveMudaQuandoAVersaoOuADataDeAlteracaoDoDocumentoMuda() {
        String chave1 = chaveUsada(documento(1L, 1, 1000L));
        String chave2 = chaveUsada(documento(1L, 2, 1000L));
        String chave3 = chaveUsada(documento(1L, 1, 2000L));

        assertThat(chave1).isNotEqualTo(chave2).isNotEqualTo(chave3);
    }

    @Test
    void chaveMudaQuandoUmItemDeConteudoEAtualizado() {
        when(itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(1L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, LocalDateTime.of(2026, 1, 1, 0, 0)}));
        String chaveAntes = chaveUsada(documento(1L, 1, 1000L));

        when(itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(1L))
                .thenReturn(List.<Object[]>of(new Object[]{10L, LocalDateTime.of(2026, 1, 2, 0, 0)}));
        String chaveDepois = chaveUsada(documento(1L, 1, 1000L));

        assertThat(chaveAntes).isNotEqualTo(chaveDepois);
    }

    @Test
    void chaveMudaQuandoUmAnexoEAdicionadoOuRemovido() {
        String semAnexo = chaveUsada(documento(1L, 1, 1000L));

        Anexo anexo = new Anexo();
        anexo.setId(99L);
        when(anexoRepository.findByDocumentoIdOrderByOrdemAsc(1L)).thenReturn(List.of(anexo));
        String comAnexo = chaveUsada(documento(1L, 1, 1000L));

        assertThat(semAnexo).isNotEqualTo(comAnexo);
    }

    @Test
    void mesmoConteudoProduzAMesmaChaveEntreChamadas() {
        String chave1 = chaveUsada(documento(1L, 1, 1000L));
        String chave2 = chaveUsada(documento(1L, 1, 1000L));

        assertThat(chave1).isEqualTo(chave2);
    }

    // ── Expiração do cache (prazo em dias: app.pdf.cache.ttl-dias) ─────────────────────────────────────────────

    private static S3Object objeto(String chave, Instant ultimaModificacao) {
        return S3Object.builder().key(chave).lastModified(ultimaModificacao).build();
    }

    private void listagemDoCache(S3Object... objetos) {
        SdkIterable<S3Object> conteudo = () -> List.of(objetos).iterator();
        ListObjectsV2Iterable pagina = mock(ListObjectsV2Iterable.class);
        when(pagina.contents()).thenReturn(conteudo);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(pagina);
    }

    @Test
    void limpezaApagaDoCacheSoOQueTemMaisDoQueOPrazoEmDias() {
        Instant agora = Instant.now();
        listagemDoCache(
                objeto("cache/pdf/1/velho.pdf", agora.minus(3, ChronoUnit.DAYS)),
                objeto("cache/html/2/velho.html", agora.minus(10, ChronoUnit.DAYS)),
                objeto("cache/pdf/3/recente.pdf", agora.minus(1, ChronoUnit.DAYS)));

        service.removerExpirados();

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(2)).deleteObject(captor.capture());
        assertThat(captor.getAllValues()).extracting(DeleteObjectRequest::key)
                .containsExactlyInAnyOrder("cache/pdf/1/velho.pdf", "cache/html/2/velho.html");
        assertThat(captor.getAllValues()).extracting(DeleteObjectRequest::bucket).containsOnly("fab-legis-teste");
    }

    @Test
    void limpezaSoOlhaOPrefixoCache() {
        listagemDoCache();

        service.removerExpirados();

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2Paginator(captor.capture());
        assertThat(captor.getValue().prefix()).isEqualTo("cache/");
        assertThat(captor.getValue().bucket()).isEqualTo("fab-legis-teste");
    }

    @Test
    void limpezaRespeitaOPrazoConfigurado() {
        ReflectionTestUtils.setField(service, "ttlDias", 7);
        Instant agora = Instant.now();
        listagemDoCache(
                objeto("cache/pdf/1/de-5-dias.pdf", agora.minus(5, ChronoUnit.DAYS)),
                objeto("cache/pdf/2/de-8-dias.pdf", agora.minus(8, ChronoUnit.DAYS)));

        service.removerExpirados();

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("cache/pdf/2/de-8-dias.pdf");
    }

    @Test
    void limpezaNuncaLancaMesmoSeOArmazenamentoFalhar() {
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenThrow(new RuntimeException("fora do ar"));

        assertThatCode(() -> service.removerExpirados()).doesNotThrowAnyException();
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    // Força um cache miss e devolve a chave (GetObjectRequest.key()) que o serviço calculou --
    // único jeito de observar o fingerprint, que é privado.
    private String chaveUsada(Documento documento) {
        reset(s3Client);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("miss"));
        service.buscar(documento, DocumentoRenderCacheService.TipoDeRenderizacao.PDF);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        return captor.getValue().key();
    }
}
