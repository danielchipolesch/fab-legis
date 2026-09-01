package br.com.danielchipolesch.infrastructure.configurations;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-url}")
    private String publicUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    // Fixa a região no client em vez de deixar o SDK descobrir via GetBucketLocation
    // -- essa chamada extra falharia para minioClientPublico, que assina com o
    // endpoint público mas roda dentro do backend, sem alcançá-lo pela rede Docker.
    @Value("${minio.region}")
    private String region;

    // Cliente "interno" -- usado para toda operação servidor-a-servidor (upload,
    // leitura de bytes para embutir em PDF/HTML), via o endpoint da rede Docker.
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }

    // Cliente "público" -- usado só para assinar URLs (AWS SigV4 inclui o header
    // Host na assinatura; se assinássemos com o endpoint interno e trocássemos o
    // host depois, a assinatura ficaria inválida). Mesmas credenciais, endpoint
    // diferente -- é o único jeito de a URL assinada ser alcançável pelo navegador.
    @Bean
    public MinioClient minioClientPublico() {
        return MinioClient.builder()
                .endpoint(publicUrl)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}
