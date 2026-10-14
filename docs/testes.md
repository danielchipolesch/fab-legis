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

`mvn test` roda **apenas os testes unitários**. Os **testes de integração** (`@Tag("integration")`) sobem o contexto inteiro do Spring sobre um **PostgreSQL de verdade num container (Testcontainers)** — migrations do Flyway, segurança por perfil, validação e JPA, com MockMvc — e por isso **precisam de Docker**; ficam de fora do `mvn test` padrão. Para incluí-los: `mvn test -DexcludedGroups=nenhum`. Rodando o Maven num container (como acima), o container precisa falar com o Docker do host:

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

`.github/workflows/ci.yml` roda a cada `push` (qualquer branch) e a cada `pull_request` para `master` — quatro jobs independentes (backend, integração, frontend e docs), cada um repetindo exatamente o que este documento já manda rodar manualmente: `mvn test` + SpotBugs (backend), `npm run lint` + `npm test` + `npm run build` (frontend) e `docker compose build docs` (o build estrito do MkDocs). Nenhum deploy — o workflow existe só para pegar cedo o que apareceu nesta mesma sessão (um commit com erro de compilação passando batido por a suíte não ter rodado até o fim).

## Servindo esta documentação técnica

O serviço `docs` do `docker compose.yml` empacota esta documentação (MkDocs Material) como um site estático servido por Nginx (`docs/Dockerfile`: builda com `mkdocs build --strict`, depois serve com `docs/nginx.conf`) — faz parte do `docker compose up -d`/`docker compose up --build` normal, junto com os demais serviços, disponível em `http://localhost:8000`.

!!! note "Alterou algo em `docs/`?"
    Como o conteúdo é compilado em build time (não montado como volume), rode `docker compose up -d --build docs` para reconstruir o site com as mudanças.

Para editar com recarregamento automático (sem rebuildar a imagem a cada alteração), rode o MkDocs diretamente via Python:

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

Abre em `http://localhost:8000` (pare o container `docs` antes, para não conflitar na porta). `mkdocs build` sozinho gera o site estático na pasta `site/`, útil para publicar em outro hosting (ex.: GitHub Pages).

## Versionamento

O sistema segue [SemVer](https://semver.org/lang/pt-BR/) com [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`, `fix:`, `feat!:`…). Enquanto não entra em produção a versão é `0.x.y`: `feat` e mudança incompatível sobem o MINOR, `fix` sobe o PATCH; o `1.0.0` é a primeira versão em produção (commit com `Release-As: 1.0.0` no rodapé) e, a partir dela, mudança incompatível sobe o MAJOR e migrations já publicadas não são mais reescritas.

- **Fonte única:** `<version>` do `backend/pom.xml`. O `frontend/package.json` acompanha.
- **Automação:** o workflow `release.yml` (release-please, configurado em `release-please-config.json`) mantém na `master` um PR de release com a próxima versão e o `CHANGELOG.md`; mergeá-lo cria a tag `vX.Y.Z` e a release no GitHub.
- **Versão visível:** `GET /v1/versao` (público; `VersaoController`, lê o `build-info` gravado no empacotamento) e o rodapé da tela de login ("v0.1.0").
