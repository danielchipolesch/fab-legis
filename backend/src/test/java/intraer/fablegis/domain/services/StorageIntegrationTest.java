package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.infrastructure.configurations.StorageConfig;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// O armazenamento de objetos contra um SeaweedFS DE VERDADE num container (Testcontainers): é aqui que se prova que o
// ImagemService e o cache de PDF/HTML funcionam com o servidor que a FAB roda, e não só com mocks. Precisa de Docker, então
// fica fora do `mvn test` padrão (@Tag("integration")) -- ver docs/testes.md.
//
// A versão da imagem é a do docker-compose.yml (serviço "armazenamento"): mudou uma, mude a outra.
@Tag("integration")
class StorageIntegrationTest {

    private static final String BUCKET = "fab-legis-teste";
    private static final String CHAVE = "chave-de-teste";
    private static final String SEGREDO = "segredo-de-teste-123";

    static final GenericContainer<?> SEAWEEDFS = new GenericContainer<>("chrislusf/seaweedfs:4.54")
            .withCommand("server", "-dir=/data", "-s3", "-s3.port=9000", "-master.volumeSizeLimitMB=128", "-master.telemetry=false")
            .withEnv("AWS_ACCESS_KEY_ID", CHAVE)
            .withEnv("AWS_SECRET_ACCESS_KEY", SEGREDO)
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/healthz").forPort(9000).forStatusCode(200));

    static S3Client s3;
    static S3Presigner assinador;
    static ImagemService imagemService;
    static DocumentoRenderCacheService cache;
    static String endereco;

    @BeforeAll
    static void subirServidor() {
        SEAWEEDFS.start();
        endereco = "http://" + SEAWEEDFS.getHost() + ":" + SEAWEEDFS.getMappedPort(9000);
        s3 = StorageConfig.criarCliente(endereco, CHAVE, SEGREDO, "us-east-1");
        assinador = StorageConfig.criarAssinador(endereco, CHAVE, SEGREDO, "us-east-1");

        imagemService = new ImagemService(s3, assinador);
        ReflectionTestUtils.setField(imagemService, "bucket", BUCKET);
        ReflectionTestUtils.setField(imagemService, "publicUrl", endereco);

        var anexos = mock(ItemAnexoParteNormativaRepository.class);
        var preliminares = mock(ItemPartePreliminarRepository.class);
        var anexosDeImagem = mock(AnexoRepository.class);
        when(anexos.findIdsEDataAtualizacaoByDocumentoId(1L)).thenReturn(List.of());
        when(preliminares.findIdsEDataAtualizacaoByDocumentoId(1L)).thenReturn(List.of());
        when(anexosDeImagem.findByDocumentoIdOrderByOrdemAsc(1L)).thenReturn(List.of());
        cache = new DocumentoRenderCacheService(s3, anexos, preliminares, anexosDeImagem);
        ReflectionTestUtils.setField(cache, "bucket", BUCKET);
        ReflectionTestUtils.setField(cache, "ttlDias", 2);
    }

    @AfterAll
    static void derrubarServidor() {
        if (assinador != null) assinador.close();
        if (s3 != null) s3.close();
        SEAWEEDFS.stop();
    }

    private static byte[] bytesAleatorios(int tamanho, long semente) {
        byte[] bytes = new byte[tamanho];
        new Random(semente).nextBytes(bytes);
        return bytes;
    }

    private static HttpResponse<byte[]> baixar(String url) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void imagemEnviadaVoltaIdenticaPeloBackendEPelaUrlAssinada() throws Exception {
        byte[] bytes = bytesAleatorios(300_000, 1);

        String url = imagemService.uploadImagem(new MockMultipartFile("arquivo", "foto.PNG", "image/png", bytes));

        assertThat(url).startsWith(endereco + "/" + BUCKET + "/").endsWith(".png");
        // Leitura pelo backend (embutir no PDF/HTML): data URI com os mesmos bytes.
        String dataUri = imagemService.getImageAsDataUri(url);
        assertThat(dataUri).startsWith("data:image/png;base64,");
        assertThat(java.util.Base64.getDecoder().decode(dataUri.substring("data:image/png;base64,".length()))).isEqualTo(bytes);
        // Leitura pelo navegador: URL assinada.
        var resposta = baixar(imagemService.gerarUrlAssinada(url));
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).isEqualTo(bytes);
    }

    @Test
    void pdfEHtmlSaoGuardadosComOTipoDeConteudoEPodemSerTransmitidos() throws Exception {
        byte[] pdf = bytesAleatorios(800_000, 2);
        byte[] html = "<html><body>olá, ação</body></html>".getBytes("UTF-8");

        String urlPdf = imagemService.uploadPdf(pdf, "doc-1.pdf");
        String urlHtml = imagemService.uploadHtml(html, "doc-1.html");

        assertThat(urlPdf).isEqualTo(endereco + "/" + BUCKET + "/pdf/doc-1.pdf");
        var respostaPdf = baixar(imagemService.gerarUrlAssinada(urlPdf));
        assertThat(respostaPdf.headers().firstValue("Content-Type")).hasValue("application/pdf");
        assertThat(respostaPdf.body()).isEqualTo(pdf);
        var respostaHtml = baixar(imagemService.gerarUrlAssinada(urlHtml));
        assertThat(respostaHtml.headers().firstValue("Content-Type").orElse("")).containsIgnoringCase("text/html");
        assertThat(respostaHtml.body()).isEqualTo(html);
        // Transmissão em stream (DocumentoPdfService.streamPdf).
        try (InputStream stream = imagemService.getObjectStream(urlPdf)) {
            assertThat(stream.readAllBytes()).isEqualTo(pdf);
        }
    }

    @Test
    void arquivoGrandeDe20MbSobeEDesceIntegro() throws Exception {
        byte[] grande = bytesAleatorios(20_000_000, 3);

        String url = imagemService.uploadPdf(grande, "grande.pdf");

        try (InputStream stream = imagemService.getObjectStream(url)) {
            assertThat(Arrays.equals(stream.readAllBytes(), grande)).isTrue();
        }
    }

    @Test
    void removerApagaOObjetoEAUrlAssinadaPassaAdarNaoEncontrado() throws Exception {
        String url = imagemService.uploadImagem(new MockMultipartFile("arquivo", "x.jpg", "image/jpeg", bytesAleatorios(1000, 4)));
        String assinada = imagemService.gerarUrlAssinada(url);
        assertThat(baixar(assinada).statusCode()).isEqualTo(200);

        imagemService.remover(url);

        assertThat(imagemService.getImageAsDataUri(url)).isNull();
        assertThat(baixar(assinada).statusCode()).isEqualTo(404);
    }

    @Test
    void urlAssinadaAdulteradaEhRecusada() throws Exception {
        String url = imagemService.uploadImagem(new MockMultipartFile("arquivo", "y.png", "image/png", bytesAleatorios(1000, 5)));
        String adulterada = imagemService.gerarUrlAssinada(url).replaceAll("X-Amz-Signature=[0-9a-f]{8}", "X-Amz-Signature=00000000");

        assertThat(baixar(adulterada).statusCode()).isEqualTo(403);
    }

    @Test
    void garantirBucketEIdempotenteEMantemOBucketPrivado() throws Exception {
        imagemService.uploadPdf("a".getBytes(), "a.pdf");
        imagemService.uploadPdf("b".getBytes(), "b.pdf");

        // Sem URL assinada, o objeto não é acessível: o bucket é privado.
        var anonimo = baixar(endereco + "/" + BUCKET + "/pdf/a.pdf");
        assertThat(anonimo.statusCode()).isEqualTo(403);
    }

    @Test
    void cacheGuardaBuscaEExpira() {
        Documento doc = new Documento();
        doc.setId(1L);
        doc.setVersao(1);
        doc.setDtAlteracao(new Timestamp(1000L));
        byte[] conteudo = bytesAleatorios(50_000, 6);

        assertThat(cache.buscar(doc, DocumentoRenderCacheService.TipoDeRenderizacao.PDF)).isEmpty();
        cache.salvar(doc, DocumentoRenderCacheService.TipoDeRenderizacao.PDF, conteudo);
        assertThat(cache.buscar(doc, DocumentoRenderCacheService.TipoDeRenderizacao.PDF)).contains(conteudo);

        // Nada com menos de 2 dias é apagado...
        assertThat(cache.removerExpiradosAntesDe(Instant.now().minusSeconds(2L * 24 * 3600))).isZero();
        assertThat(cache.buscar(doc, DocumentoRenderCacheService.TipoDeRenderizacao.PDF)).isPresent();
        // ...e o que passou do prazo é.
        assertThat(cache.removerExpiradosAntesDe(Instant.now().plusSeconds(60))).isGreaterThanOrEqualTo(1);
        assertThat(cache.buscar(doc, DocumentoRenderCacheService.TipoDeRenderizacao.PDF)).isEmpty();
    }
}
