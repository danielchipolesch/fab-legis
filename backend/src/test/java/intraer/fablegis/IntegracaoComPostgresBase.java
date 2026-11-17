package intraer.fablegis;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// Base dos testes de integração: o contexto inteiro da aplicação (segurança, Flyway, JPA, controllers) sobre um PostgreSQL
// de verdade num container (Testcontainers). Precisam de Docker, então ficam fora do `mvn test` padrão (@Tag("integration"));
// para rodá-los: mvn test -DexcludedGroups=nenhum (ver docs/testes.md).
//
// O container sobe UMA vez para todas as classes de integração (padrão "contêiner singleton"): o Spring guarda o contexto em
// cache entre elas, e um contêiner por classe deixaria o contexto em cache apontando para um banco já desligado. Ele é
// encerrado quando a JVM termina.
//
// O armazenamento de objetos é trocado por mocks: nenhum destes testes trata de armazenamento (o cache de PDF/HTML e o
// ImagemService têm os seus testes unitários), e assim não há segundo container nem rede para o boot.
@Tag("integration")
@SpringBootTest
public abstract class IntegracaoComPostgresBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propriedadesDoBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    S3Client s3Client;

    @MockitoBean
    S3Presigner s3Presigner;
}
