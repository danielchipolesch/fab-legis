# Testes e qualidade

Como testar e analisar o sistema. A instalação (desenvolvimento e produção) e as variáveis de ambiente ficam no `README.md` da raiz do repositório.


## Backend

Os testes unitários do backend (JUnit 5 + Mockito, sem Spring e sem banco) cobrem as regras de negócio — a **numeração** (`NumeracaoServiceTest`), as **transições de situação** (`DocumentoStatusServiceTest`, que segue o diagrama de [Ciclo de Vida](ciclo-de-vida.md)), as **regras de acesso**, o **ciclo de emenda** (`EmendaServiceCicloTest`, `EmendaServiceConsolidacaoTest`, `EmendaServicePendentesTest`), a renderização de tabelas, o cache de PDF/HTML e as regras da NPA. Ficam em `backend/src/test/java`, no mesmo pacote da classe testada. Regra do projeto: mudou uma regra de negócio, o teste muda junto (ver `CLAUDE.md`).

Não é preciso ter JDK nem Maven instalados — o comando abaixo roda tudo num container descartável (a primeira execução baixa as dependências para o volume `fab-legis-m2-cache`; as seguintes são rápidas):

```bash
# Linux / macOS / CI
docker run --rm -v "$(pwd)/backend:/app" -v fab-legis-m2-cache:/root/.m2 -w /app \
  eclipse-temurin:25-jdk-alpine sh -c "apk add --no-cache maven >/dev/null && mvn -B test"

# Windows (Git Bash)
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)/backend:/app" -v fab-legis-m2-cache:/root/.m2 -w /app \
  eclipse-temurin:25-jdk-alpine sh -c "apk add --no-cache maven >/dev/null && mvn -B test"
```

Com JDK e Maven locais, basta `mvn test` dentro de `backend/`. Para rodar só uma classe: `mvn test -Dtest=NumeracaoServiceTest`.

`mvn test` roda **apenas os testes unitários**. Os **testes de integração** (`@Tag("integration")`) sobem o contexto inteiro do Spring sobre um **PostgreSQL de verdade num container (Testcontainers)** — migrations do Flyway, segurança por papel, validação e JPA, com MockMvc — e por isso **precisam de Docker**; ficam de fora do `mvn test` padrão. Para incluí-los: `mvn test -DexcludedGroups=nenhum`. Rodando o Maven num container (como acima), o container precisa falar com o Docker do host:

```bash
# Linux / macOS / CI (o socket do Docker; sem Ryuk, que não funciona de dentro de um container)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_RYUK_DISABLED=true \
  --add-host=host.docker.internal:host-gateway -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -v "$(pwd)/backend:/app" -v fab-legis-m2-cache:/root/.m2 -w /app \
  eclipse-temurin:25-jdk-alpine sh -c "apk add --no-cache maven >/dev/null && mvn -B test -DexcludedGroups=nenhum"
```

No Windows (Git Bash) é o mesmo comando com `MSYS_NO_PATHCONV=1` e `$(pwd -W)`. Com JDK e Maven locais e o Docker em execução, basta `mvn test -DexcludedGroups=nenhum`. Hoje: `FabLegisApplicationTests` (o contexto sobe) e `ControllersIntegracaoTest` (migrations, dados de referência, 401/403/404/400 e criar/ler um documento). O MinIO é trocado por mocks nesses testes (`IntegracaoComPostgresBase`).

## Frontend

Os testes do frontend usam **Vitest** (o runner de testes do ecossistema Vite/Vue — reaproveita a mesma configuração e o mesmo `@` de importação do app). Ficam ao lado do código (`frontend/src/utils/numbering.test.js`) e cobrem a numeração (`numbering.js`, que espelha o `NumeracaoService` do backend) e as regras do fluxo de situações que as telas decidem (`fluxoDocumento.js`: destinos de aprovar/devolver/publicar, versões, selo de revogado) e a árvore da parte normativa em edição (`stores/editor.test.js`: acrescentar, mover, promover/rebaixar, mover para outro agrupamento, reordenar incluídos e os eventos em tempo real). Com o Docker:

```bash
docker compose run --rm --build --no-deps frontend npm test
```

Sem Docker (com Node 22 local): `npm install` e `npm test` dentro de `frontend/`; `npm run test:watch` reexecuta a cada alteração.

Nenhum teste está desabilitado no momento. Quando um teste descrever uma regra documentada que o código ainda não cumpre, ele fica desabilitado (`@Disabled` no backend, `it.skip` no frontend) **com o motivo**, para o alvo não se perder — ver a regra no `CLAUDE.md`.

## Análise estática

Duas ferramentas, uma por lado — achar **bug**, não estilo (por isso não há Checkstyle nem Prettier configurados; ver `CLAUDE.md`):

- **Backend — [SpotBugs](https://spotbugs.github.io/)**: `mvn spotbugs:spotbugs` (dentro de `backend/`) gera o relatório em `target/spotbugsXml.xml`/`target/spotbugsHtml.html`. É o goal `spotbugs` (não `check`): **nunca falha o build**, mesmo com achados — não faz parte de `mvn test` nem tem `<executions>` no `pom.xml`, só roda quando chamado assim ou pelo CI.
- **Frontend — [ESLint](https://eslint.org/)** com o preset `flat/essential` do [`eslint-plugin-vue`](https://eslint.vuejs.org/) (a camada "evita erro real" — v-for sem `:key`, mutar prop direto, chave duplicada… — não a camada de estilo do plugin, que fica desligada de propósito): `npm run lint` dentro de `frontend/` (ou via Docker, mesmo padrão dos testes). Falha o build se achar algo — diferente do SpotBugs, porque já roda limpo.

Sem regra customizada em nenhuma das duas — é o primeiro passo deliberadamente mínimo; afinar limiar/filtro ou adicionar mais ferramentas (Checkstyle, PMD, cobertura) é uma decisão futura, feita uma de cada vez.

### SonarQube/SonarLint (opcional, sob demanda)

O projeto não depende do Sonar (não está no CI nem no `pom.xml`), mas dá para conferir os achados que o SonarLint do VS Code mostra com uma análise completa, num servidor descartável — o SonarLint só olha os arquivos abertos, então o total do editor é menor que o do projeto inteiro:

```bash
# servidor local (porta 9100, para não colidir com o MinIO em 9000) e um token de análise
docker run -d --name sonar-tmp -p 9100:9000 -e SONAR_SEARCH_JAVAADDITIONALOPTS="-Dnode.store.allow_mmap=false" sonarqube:community
# backend (Java): dentro do container do Maven, com o token gerado em http://127.0.0.1:9100 (admin/admin no primeiro acesso)
mvn -B compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.host.url=http://host.docker.internal:9100 -Dsonar.token=<token> -Dsonar.projectKey=fab-legis-backend
# frontend (JS/Vue) e collab: o scanner em container, na raiz do repositório
docker run --rm -v "$PWD:/usr/src" -e SONAR_HOST_URL=http://host.docker.internal:9100 -e SONAR_TOKEN=<token> sonarsource/sonar-scanner-cli -Dsonar.projectKey=fab-legis-frontend -Dsonar.sources=frontend/src,collab/server.js -Dsonar.exclusions="**/node_modules/**,**/*.test.js"
```

Achados que são **ruído deste projeto**, não defeito: `java:S1135` ("TODO") dispara em "todo/toda" — palavra comum em português nos comentários; `java:S120` (nome de pacote) contesta pacotes como `documentoDtos`, que seguem a convenção do projeto; `java:S4502`/`S5122` (CSRF desligado, CORS) são intencionais numa API sem sessão, autenticada por token Bearer (JWT); `javascript:S5332` (`http://`) são os endereços padrão da rede interna do Docker em `collab/server.js`, trocados por variáveis de ambiente em produção. Para silenciar o primeiro no editor, `"sonarlint.rules": {"java:S1135": {"level": "off"}}` nas configurações do VS Code.


## Integração contínua (GitHub Actions)

`.github/workflows/ci.yml` roda a cada `push` (qualquer branch) e a cada `pull_request` para `master` — quatro jobs independentes (backend, integração, frontend e docs), cada um repetindo exatamente o que este documento já manda rodar manualmente: `mvn test` + SpotBugs (backend), `npm run lint` + `npm test` + `npm run build` (frontend) e o build estrito do MkDocs (`mkdocs build --strict`, num container). Nenhum deploy — o workflow existe só para pegar cedo o que apareceu nesta mesma sessão (um commit com erro de compilação passando batido por a suíte não ter rodado até o fim).

## Servindo esta documentação técnica

A documentação (MkDocs Material) fica em **`/docs/`** do próprio sistema, no mesmo host — por exemplo `fab-legis.intraer/docs/`:

- **Produção:** o site é compilado dentro da imagem do Nginx da borda (`frontend/Dockerfile`, estágio `docs-build`, com `mkdocs build --strict`) e servido por ele em `/docs/`; não existe um segundo servidor para a documentação.
- **Desenvolvimento:** o serviço `docs` do `docker-compose.yml` roda `mkdocs serve` (imagem `squidfunk/mkdocs-material`) com `mkdocs.yml` e `docs/` **montados como volume**: editou uma página, o site atualiza sozinho, sem rebuild. O proxy do Vite o expõe em `http://127.0.0.1:5173/docs/`, o mesmo caminho de produção; a porta `8000` (`http://localhost:8000`) abre o servidor direto e recarrega o navegador a cada edição.

Para checar o que o CI checa (links e âncoras quebrados viram erro), rode o build estrito:

```bash
docker run --rm -v "$(pwd):/docs" squidfunk/mkdocs-material:9.5.44 build --strict --site-dir /tmp/site
# Windows (Git Bash): MSYS_NO_PATHCONV=1 e $(pwd -W) no lugar de $(pwd)
```

Sem Docker, `pip install -r requirements-docs.txt` e `mkdocs serve` (abre em `http://127.0.0.1:8000`; pare antes o container `docs` para não conflitar na porta) ou `mkdocs build`, que gera o site estático em `site/` — útil para publicar em outro hosting (ex.: GitHub Pages).
