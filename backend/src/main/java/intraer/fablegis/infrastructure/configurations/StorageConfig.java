package intraer.fablegis.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

// Armazenamento de objetos (API S3; hoje o SeaweedFS -- ver docs/infraestrutura.md). O sistema não depende de nenhuma
// extensão de fornecedor: só de put/get/delete de objetos, criação de bucket e URLs pré-assinadas.
@Configuration
public class StorageConfig {

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.public-url}")
    private String publicUrl;

    @Value("${storage.access-key}")
    private String accessKey;

    @Value("${storage.secret-key}")
    private String secretKey;

    // A região é fixa (um servidor S3 próprio não tem região de verdade): sem isso o SDK tentaria descobri-la pelo ambiente.
    @Value("${storage.region}")
    private String region;

    // Cliente "interno" -- toda operação servidor-a-servidor (upload, leitura de bytes para embutir em PDF/HTML), pelo
    // endereço da rede Docker.
    @Bean
    public S3Client s3Client() {
        return criarCliente(endpoint, accessKey, secretKey, region);
    }

    // Assinador de URLs -- usa o endereço PÚBLICO. A assinatura AWS SigV4 inclui o header Host; assinar com o endereço interno
    // e trocar o host depois a invalidaria. Por isso é o único jeito de a URL assinada ser alcançável pelo navegador.
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        return criarAssinador(publicUrl, accessKey, secretKey, region);
    }

    public static S3Client criarCliente(String endpoint, String accessKey, String secretKey, String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credenciais(accessKey, secretKey))
                // Endereçamento por caminho (endereco/bucket/chave), não por subdomínio (bucket.endereco): é o único que
                // funciona com um servidor próprio atrás do Nginx, e é o formato da URL canônica gravada no banco.
                .forcePathStyle(true)
                // O SDK recente manda, por padrão, um checksum em "trailer" (aws-chunked) em todo upload. Servidores
                // compatíveis com S3 que não são da AWS rejeitam isso (o Garage responde 400 "Invalid payload signature").
                // WHEN_REQUIRED só calcula checksum onde a própria API o exige. NÃO REMOVA -- ver StorageConfigTest.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    public static S3Presigner criarAssinador(String publicUrl, String accessKey, String secretKey, String region) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(publicUrl))
                .region(Region.of(region))
                .credentialsProvider(credenciais(accessKey, secretKey))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    // Credenciais fixas: nada de cadeia padrão do SDK, que consultaria variáveis, arquivos e o serviço de metadados da nuvem.
    private static StaticCredentialsProvider credenciais(String accessKey, String secretKey) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
