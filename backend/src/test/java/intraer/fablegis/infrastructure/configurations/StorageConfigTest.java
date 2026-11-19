package intraer.fablegis.infrastructure.configurations;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// O sistema fala com um servidor S3 PRÓPRIO (SeaweedFS), não com a AWS. Estas configurações do cliente não são enfeite: sem elas
// o servidor recusa o upload (o Garage, por exemplo, responde 400 "Invalid payload signature"), ou a URL gravada no banco
// (endereço/bucket/chave) deixa de bater com a que o cliente monta. O teste observa o que o cliente realmente envia, contra um
// servidor HTTP local de mentira, sem Spring e sem Docker.
class StorageConfigTest {

    private HttpServer servidor;
    private final AtomicReference<String> caminho = new AtomicReference<>();
    private final Map<String, String> cabecalhos = new ConcurrentHashMap<>();

    @BeforeEach
    void subirServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            caminho.set(troca.getRequestURI().getRawPath());
            troca.getRequestHeaders().forEach((nome, valores) -> cabecalhos.put(nome.toLowerCase(), String.join(",", valores)));
            troca.getRequestBody().readAllBytes();
            troca.getResponseHeaders().add("ETag", "\"abc\"");
            troca.sendResponseHeaders(200, -1);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void derrubarServidor() {
        servidor.stop(0);
    }

    private S3Client cliente() {
        return StorageConfig.criarCliente(
                "http://127.0.0.1:" + servidor.getAddress().getPort(), "chave-de-teste", "segredo-de-teste-123", "us-east-1");
    }

    @Test
    void enderecaOObjetoPorCaminhoEntreOBucketEAChave() {
        try (S3Client s3 = cliente()) {
            s3.putObject(req -> req.bucket("fab-legis-imagens").key("pdf/doc-1.pdf"), RequestBody.fromString("conteudo"));
        }

        // Por caminho (endereço/bucket/chave), não por subdomínio (bucket.endereço): é o formato da URL canônica gravada no banco.
        assertThat(caminho.get()).isEqualTo("/fab-legis-imagens/pdf/doc-1.pdf");
    }

    @Test
    void naoMandaChecksumEmTrailerNemCodificacaoAwsChunked() {
        try (S3Client s3 = cliente()) {
            s3.putObject(req -> req.bucket("fab-legis-imagens").key("a.png"), RequestBody.fromString("conteudo"));
        }

        // Servidores S3 que não são da AWS rejeitam o upload com checksum em "trailer" (padrão do SDK recente; o Garage responde 400
        // "Invalid payload signature"). Em HTTP simples o SDK ainda usa aws-chunked com assinatura por pedaço -- isso os servidores aceitam;
        // o que não se pode mandar é o trailer.
        assertThat(cabecalhos).doesNotContainKeys("x-amz-trailer", "x-amz-sdk-checksum-algorithm", "x-amz-checksum-crc32");
        assertThat(cabecalhos.getOrDefault("x-amz-content-sha256", "")).doesNotContain("TRAILER");
    }

    // Controle: prova que a configuração acima É o que evita o trailer -- o cliente com os padrões do SDK o manda. Se este teste
    // passar a falhar, o SDK mudou o padrão e o ajuste em StorageConfig.criarCliente pode já não ser necessário.
    @Test
    void controle_oClienteComOsPadroesDoSdkMandaOTrailerQueOsServidoresRecusam() {
        try (S3Client padrao = S3Client.builder()
                .endpointOverride(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("chave-de-teste", "segredo-de-teste-123")))
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.create())
                .build()) {
            padrao.putObject(req -> req.bucket("fab-legis-imagens").key("a.png"), RequestBody.fromString("conteudo"));
        }

        assertThat(cabecalhos).containsKey("x-amz-trailer");
        assertThat(cabecalhos.get("x-amz-content-sha256")).contains("TRAILER");
    }

    @Test
    void assinaARequisicaoComSigV4() {
        try (S3Client s3 = cliente()) {
            s3.putObject(req -> req.bucket("fab-legis-imagens").key("a.png"), RequestBody.fromString("conteudo"));
        }

        assertThat(cabecalhos.get("authorization")).startsWith("AWS4-HMAC-SHA256 Credential=chave-de-teste/");
    }

    @Test
    void urlAssinadaUsaOEnderecoPublicoPorCaminhoEComValidadeDeUmaHora() {
        try (S3Presigner assinador = StorageConfig.criarAssinador(
                "https://s3.exemplo.mil.br", "chave-de-teste", "segredo-de-teste-123", "us-east-1")) {
            String url = assinador.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .getObjectRequest(req -> req.bucket("fab-legis-imagens").key("uuid.png"))
                    .build()).url().toString();

            // O host assinado é o PÚBLICO (a assinatura inclui o Host; trocá-lo depois a invalidaria).
            assertThat(url).startsWith("https://s3.exemplo.mil.br/fab-legis-imagens/uuid.png?");
            assertThat(url).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256").contains("X-Amz-Expires=3600");
            assertThat(url).contains("X-Amz-SignedHeaders=host");
        }
    }
}
