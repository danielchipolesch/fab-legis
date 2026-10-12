package intraer.fablegis;

import io.minio.MinioClient;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Base dos testes de integração: o contexto inteiro da aplicação (segurança, Flyway, JPA, controllers) sobre um PostgreSQL
// de verdade num container (Testcontainers). Precisam de Docker, então ficam fora do `mvn test` padrão (@Tag("integration"));
// para rodá-los: mvn test -DexcludedGroups=nenhum (ver docs/testes.md).
//
// O MinIO é trocado por mocks: nenhum destes testes trata de armazenamento de objetos (o cache de PDF/HTML tem o seu teste
// unitário), e assim não há segundo container nem rede para o boot.
@Tag("integration")
@Testcontainers
@SpringBootTest
public abstract class IntegracaoComPostgresBase {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @MockitoBean(name = "minioClient")
    MinioClient minioClient;

    @MockitoBean(name = "minioClientPublico")
    MinioClient minioClientPublico;
}
