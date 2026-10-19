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

`.github/workflows/ci.yml` roda a cada `push` (qualquer branch) e a cada `pull_request` para `master` — três jobs independentes (backend, integração e frontend), cada um repetindo exatamente o que este documento já manda rodar manualmente: `mvn test` + SpotBugs (backend) e `npm run lint` + `npm test` + `npm run build` (frontend). Nenhum deploy — o workflow existe só para pegar cedo o que apareceu nesta mesma sessão (um commit com erro de compilação passando batido por a suíte não ter rodado até o fim).

## Lendo e editando esta documentação

A documentação técnica **não é publicada em lugar nenhum**: são arquivos Markdown na pasta `docs/`, lidos direto no repositório (no GitHub, ou no editor), a partir do [README](../README.md#documentação-técnica), que é o índice. Não há serviço, site gerado nem ferramenta para instalar. O GitHub renderiza as tabelas, os blocos de alerta (`> [!NOTE]`) e os diagramas Mermaid.

Ao editar, valem estas convenções (o que o GitHub entende, e nada além):

- links entre páginas são relativos, com a extensão (`[Arquitetura](arquitetura.md#estrutura-de-pastas)`); a âncora é o título em minúsculas, sem pontuação, com hífen no lugar de espaço — um título com travessão (` — `) gera dois hífens seguidos;
- avisos usam `> [!NOTE]` (ou `[!WARNING]`), nunca a sintaxe `!!!` do MkDocs; variantes em abas viram subtítulos;
- página nova entra na tabela do [README](../README.md#documentação-técnica) e em `CLAUDE.md`.

## Testes de carga e métricas

Os testes de carga dizem quanto o sistema aguenta e **onde ele satura primeiro**, medindo o que o mercado usa: vazão (requisições por segundo), latência em percentis (**p95** e **p99**, não a média), taxa de erros e a utilização dos recursos (CPU, memória, conexões do banco). Ficam em `loadtest/` (backend, com [k6](https://k6.io)) e em `collab/teste-de-carga.mjs` (colaboração em tempo real). **Rode sempre contra um ambiente de teste, nunca contra produção**: eles criam documentos (com o título "Teste de carga…") e os apagam no fim.

Os dois rodam em contêineres, dentro da rede do Docker Compose (nada para instalar), falando direto com o backend. Troque `CPF` e `SENHA` pelos de um usuário que possa criar documentos (o administrador padrão, em desenvolvimento):

```bash
# Backend (k6). PERFIL: leve (10 usuários), medio (50) ou pico (100); DURACAO em segundos
MSYS_NO_PATHCONV=1 docker run --rm --network fab-legis_default -v "$(pwd -W)/loadtest:/loadtest" \
  -e BASE_URL=http://backend:8081 -e CPF=<cpf> -e SENHA=<senha> -e PERFIL=medio -e DURACAO=60 \
  grafana/k6 run --quiet /loadtest/k6/api.js

# Collab: SALAS salas x CLIENTES por sala, todos digitando por DURACAO segundos
MSYS_NO_PATHCONV=1 docker run --rm --network fab-legis_default -v "$(pwd -W):/r" \
  -e CPF=<cpf> -e SENHA=<senha> -e SALAS=30 -e CLIENTES=4 -e DURACAO=30 node:20-alpine \
  sh -c "mkdir /t && cp -r /r/collab /r/loadtest /t/ && cd /t/collab && npm ci --silent && node teste-de-carga.mjs"
```

(O nome da rede, `fab-legis_default`, é o do projeto do Compose; em Linux use `$(pwd)` no lugar de `$(pwd -W)` e tire o `MSYS_NO_PATHCONV`.)

**O que o k6 exercita, ao mesmo tempo:** leitura (as telas do dia a dia: Área de Trabalho, listas, busca, abrir documento), gravação do conteúdo de um elemento (o que o collab faz a cada pausa de digitação), criação de documento (a numeração com trava), PDF em cache e PDF a gerar (o Apache FOP, o ponto que satura primeiro). As **metas** (`thresholds`) são as usuais de uma API interna: p95 < 500 ms na leitura, < 800 ms na gravação, < 1,5 s na criação, < 10 s na geração de PDF, e erros < 1%. O teste termina com falha se alguma não for cumprida. Um `503` na geração de PDF (o limite de gerações simultâneas esgotou a espera) é contado à parte (`pdf_ocupado_503`), porque é a condição que o sistema deve avisar, não um erro.

**O que o teste do collab mede:** quantas conexões abrem, o tempo de conexão, e a **latência de propagação** (da edição de um cliente até ela chegar aos outros da sala), com meta de **p95 < 100 ms**. Cada cliente tem o seu WebSocket, e as teclas vão para o parágrafo de verdade, de modo que o collab também grava no backend como em produção.

### Métricas para o Prometheus

- **Backend:** `/actuator/prometheus` (Spring Boot Actuator/Micrometer): `http_server_requests_seconds` (taxa, erros e p95/p99 por endpoint), `hikaricp_connections_*` (o pool do banco; `pending` > 0 é fila), `jvm_memory_*`, `jvm_gc_*` e `process_cpu_usage`. **Exige autenticação** (é protegido como o resto da API): configure a raspagem com um token, ou deixe-o acessível só pela rede interna.
- **Collab:** um serviço Node separado, que **não** passa pelo backend nem pelo frontend, então tem o **seu próprio** `/metrics`, na porta interna `METRICS_PORT` (padrão `9464`, não publicada pelo Compose nem repassada pelo Nginx — só a rede interna a alcança). Traz as métricas padrão do Node (CPU, memória, atraso do *event loop*, GC) e as do que importa à colaboração: `collab_conexoes_abertas`, `collab_salas_abertas`, `collab_autenticacoes_total{resultado}`, `collab_persistencias_total{resultado}` e os tempos de `collab_persistencia_segundos` e `collab_carregamento_segundos`.

O Prometheus raspa os dois endereços, e os dois aparecem juntos no mesmo painel (Grafana):

```yaml
scrape_configs:
  - job_name: fab-legis-backend
    metrics_path: /actuator/prometheus
    authorization: { credentials: <token> }
    static_configs: [{ targets: ['backend:8081'] }]
  - job_name: fab-legis-collab
    static_configs: [{ targets: ['collab:9464'] }]
```

**Sinais para olhar (método USE/RED):** p95 de `http_server_requests_seconds` acima de 500 ms; `hikaricp_connections_pending` acima de zero; CPU do backend acima de 70%; heap da JVM perto do limite do contêiner; no collab, atraso do *event loop* (`collab_node_eventloop_lag_p99_seconds`) acima de 100 ms e `collab_persistencias_total{resultado="erro"}` subindo.

## Versionamento

O sistema segue [SemVer](https://semver.org/lang/pt-BR/) com [Conventional Commits](https://www.conventionalcommits.org/pt-br/) (`feat:`, `fix:`, `feat!:`…). Enquanto não entra em produção a versão é `0.x.y`: `feat` e mudança incompatível sobem o MINOR, `fix` sobe o PATCH; o `1.0.0` é a primeira versão em produção (commit com `Release-As: 1.0.0` no rodapé) e, a partir dela, mudança incompatível sobe o MAJOR e migrations já publicadas não são mais reescritas.

- **Fonte única:** `<version>` do `backend/pom.xml`. O `frontend/package.json` acompanha.
- **Automação:** o workflow `release.yml` (release-please, configurado em `release-please-config.json`) mantém na `master` um PR de release com a próxima versão e o `CHANGELOG.md`; mergeá-lo cria a tag `vX.Y.Z` e a release no GitHub.
- **Versão visível:** `GET /v1/versao` (público; `VersaoController`, lê o `build-info` gravado no empacotamento) e o rodapé da tela de login ("v0.1.0").

### Fluxo de branches e releases

Há **uma branch só, a `master`**, protegida: só recebe Pull Request de branches curtas, com o CI verde (`backend`, `integracao` e `frontend`). A `master` é o código mais recente já revisado e sempre em condição de ser lançado, mas **não é a produção**: produção é uma **tag**. Fluxo de uma mudança:

1. branch curta a partir da `master` → PR para a `master` → CI verde → merge;
2. o release-please acumula o que entrou num PR de release aberto (`release-please--branches--master` → `master`), que só altera a versão e o `CHANGELOG.md`;
3. quando se decide lançar, mergeia-se o PR de release: o robô cria a tag e a release, e **se implanta aquela tag** (primeiro em homologação, se houver, e depois em produção, sempre a mesma tag);
4. correção urgente com a `master` já adiantada: branch de manutenção a partir da tag em produção (`release/1.x`), corrige-se nela e lança-se `1.x.y`.

O PR de release é aberto pelo `GITHUB_TOKEN` e por isso o CI não roda nele; o administrador do repositório o mergeia contornando a exigência de checks.
