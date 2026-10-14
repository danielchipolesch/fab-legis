package intraer.fablegis;

import org.junit.jupiter.api.Test;

// Sobe o contexto inteiro sobre um PostgreSQL de verdade (ver IntegracaoComPostgresBase): fica fora do `mvn test` padrão
// (@Tag("integration"), excludedGroups no pom.xml) porque precisa de Docker.
class FabLegisApplicationTests extends IntegracaoComPostgresBase {

	@Test
	void contextLoads() {
	}

}
