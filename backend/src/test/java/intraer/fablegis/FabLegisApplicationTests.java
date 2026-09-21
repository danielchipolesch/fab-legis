package intraer.fablegis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sobe o contexto inteiro e precisa de PostgreSQL: fica fora do `mvn test` padrão
// (ver excludedGroups no pom.xml) para os testes unitários rodarem sem infra.
@Tag("integration")
@SpringBootTest
class FabLegisApplicationTests {

	@Test
	void contextLoads() {
	}

}
