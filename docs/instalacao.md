# Instalação e Configuração

## Pré-requisitos

- **Docker** e **Docker Compose** (caminho recomendado), ou
- **JDK 25** + **Node.js 22** + **PostgreSQL 16** para execução local.

## Opção 1 — Docker Compose (recomendado)

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis

# Opcional: personaliza credenciais/portas sem tocar no docker-compose.yml --
# sem este arquivo, o compose usa os mesmos valores de dev documentados abaixo
cp .env.example .env

# Sobe PostgreSQL, MinIO, backend, collab (colaboração em tempo real), frontend
# e a documentação técnica
docker compose up -d

# Acompanhar os logs
docker compose logs -f backend frontend
```

O `.env` na raiz (fora do git, só o `.env.example` é versionado) é lido automaticamente pelo `docker compose` para preencher os `${VAR:-padrão}` do `docker-compose.yml` — sem ele, cada variável cai no próprio padrão já embutido no compose, então o comportamento é idêntico ao de antes deste arquivo existir. É o **mesmo arquivo** usado pela Opção 2 (frontend rodando fora do Docker via `npm run dev`) — ver nota na aba "Frontend" abaixo.

**Serviços disponíveis:**

| Serviço | URL | Credenciais |
|---|---|---|
| Frontend (dev) | http://127.0.0.1:5173 | — |
| Backend (API) | http://127.0.0.1:8081 | — |
| Swagger UI | http://localhost:8081/swagger-ui.html | — |
| Collab (colaboração em tempo real) | ws://localhost:1234 | — |
| OpenAPI JSON | http://localhost:8081/v1/fab-legis-api | — |
| Documentação técnica | http://localhost:8000 | — |
| PostgreSQL | `localhost:5432` | `postgres` / `123456` |
| MinIO (console) | http://localhost:9001 | `minioadmin` / `minioadmin123` |

**Usuário administrador padrão** — criado automaticamente pelo `DataSeeder` no primeiro boot, só se nenhum usuário real ainda existir. CPF `111.444.777-35` (dígitos: `11144477735`), senha `Admin@123`. Troque a senha (ou os valores de `APP_ADMIN_CPF`/`APP_ADMIN_SENHA` antes do primeiro boot) assim que possível em qualquer ambiente que não seja local.

**Perfil de produção** (frontend compilado e servido por Nginx na porta 80):

```bash
docker compose --profile production up -d
```

Isso sobe o serviço `frontend-prod` em vez do `frontend` de desenvolvimento — mas **subir com esse perfil sozinho não é suficiente pra funcionar** fora de `localhost`/`127.0.0.1`. Ver [Deploy em produção](#deploy-em-producao) abaixo para o passo a passo completo (HTTPS, proxy reverso, variáveis de ambiente).

## Opção 2 — execução local

=== "Backend"
    ```bash
    # Suba apenas as dependências de infraestrutura
    docker compose up -d postgres minio

    cd backend
    ./mvnw spring-boot:run          # Linux/macOS
    mvnw.cmd spring-boot:run        # Windows
    ```

    A API sobe em `http://127.0.0.1:8081` com o perfil `dev` ativo.

=== "Frontend"
    ```bash
    cp .env.example .env            # na raiz do repo, se ainda não existir — ajuste
                                     # VITE_API_BASE_URL se necessário (ver variáveis abaixo)
    cd frontend
    npm install
    npm run dev                     # http://127.0.0.1:5173
    ```

    O `.env` lido aqui é o mesmo da raiz do repositório (usado também pelo Docker
    Compose) — `frontend/vite.config.js` aponta o `envDir` do Vite pra lá, então
    não existe um `.env`/`.env.example` separado dentro de `frontend/`.

    Scripts disponíveis:

    | Comando | Descrição |
    |---|---|
    | `npm run dev` | Servidor de desenvolvimento com HMR |
    | `npm run build` | Build de produção em `dist/` |
    | `npm run preview` | Pré-visualização do build |

## Variáveis de ambiente

Via Docker Compose, todas as variáveis abaixo (exceto `PORT` do collab, que não tem override no compose) podem ser ajustadas pelo `.env` na raiz do repositório (`cp .env.example .env`), sem editar `docker-compose.yml` — ver Opção 1 acima.

**Backend**

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `fab-legis-dev` | Conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `123456` | Credenciais do banco |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Endpoint S3 do MinIO |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | `minioadmin` / `minioadmin123` | Credenciais do MinIO |
| `MINIO_BUCKET` | `fab-legis-imagens` | Bucket das imagens |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL pública para servir as imagens |
| `APP_OAUTH2_ISSUER` | `http://127.0.0.1:8081` | Endereço que o navegador usa para o Authorization Server (vira o claim `iss` do token — collab e frontend precisam concordar com esse valor). Usa `127.0.0.1`, não `localhost`, de propósito — ver aviso abaixo |
| `APP_OAUTH2_REDIRECT_URI` | `http://127.0.0.1:5173/callback` | Rota de callback da SPA (troca o `code` por token) |
| `APP_FRONTEND_LOGIN_URL` | `http://127.0.0.1:5173/login` | Tela de login (Vue) para onde o Authorization Server manda usuários não-autenticados |
| `APP_ADMIN_CPF` / `APP_ADMIN_SENHA` / `APP_ADMIN_NOME` | ver acima | Usuário administrador padrão, criado só no primeiro boot |

**Collab** (serviço de colaboração em tempo real, ver [Arquitetura](arquitetura.md))

| Variável | Padrão | Descrição |
|---|---|---|
| `OAUTH2_ISSUER` | `http://127.0.0.1:8081` | Precisa bater com `APP_OAUTH2_ISSUER` do backend — é o valor checado contra o claim `iss` do token |
| `OAUTH2_JWKS_URL` | `http://backend:8081/oauth2/jwks` | Endereço **interno** (rede Docker) de onde o `collab` busca as chaves públicas para validar o token — pode ser diferente do `OAUTH2_ISSUER` |
| `BACKEND_URL` | `http://backend:8081/v1` | Onde o `collab` busca/persiste o conteúdo de cada elemento (`GET`/`PATCH /documentos/.../elementos/.../conteudo`) |
| `PORT` | `1234` | Porta do servidor WebSocket (Hocuspocus) |

**Frontend**

| Variável | Padrão | Descrição |
|---|---|---|
| `VITE_API_BASE_URL` | `http://127.0.0.1:8081/v1` | URL base da API |
| `PROXY_BACKEND_TARGET` | `http://127.0.0.1:8081` | **Não** é `VITE_*` de propósito — só o processo Node do Vite (lado servidor) usa isto para saber para onde proxiar `/oauth2/**`, `/login`, `/logout` (ver `server.proxy` em `vite.config.js`); nunca chega ao bundle do navegador. No Docker vale `http://backend:8081` (nome do serviço, só resolvível dentro da rede Docker) |
| `VITE_COLLAB_URL` | `ws://127.0.0.1:1234` | URL do serviço de colaboração em tempo real (WebSocket) |
| `VITE_USE_MOCK_API` | `false` | Usa API mockada em vez do backend real |
| `VITE_APP_ENV` | `development` | Ambiente (`development` \| `staging` \| `production`) — só afeta o build local (sourcemap em `staging`, ver `vite.config.js`); o build via Docker sempre roda como `development` |
| `VITE_APP_VERSION` | `1.0.0` | Versão exibida em logs/telas de diagnóstico |

!!! warning "Atenção"
    As credenciais acima são valores de desenvolvimento. Em produção, substitua todas por *secrets* gerenciados fora do repositório.

!!! warning "Acesse sempre via `127.0.0.1`, nunca `localhost`"
    `APP_OAUTH2_ISSUER`/`APP_OAUTH2_REDIRECT_URI`/`APP_FRONTEND_LOGIN_URL`/`VITE_API_BASE_URL`/`VITE_COLLAB_URL` usam `127.0.0.1` de propósito, não `localhost` — para o navegador são origens diferentes (cookie de sessão e `sessionStorage` não atravessam), e o `redirect_uri` do client OAuth2 só está registrado para `127.0.0.1`. Abrir o frontend em `http://localhost:5173` quebra o login logo no primeiro redirect para `/oauth2/authorize`. Se mudar um desses valores, mude todos juntos, mantendo o mesmo host.

## Deploy em produção

O fluxo de login (OAuth2 Authorization Code + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi validado em desenvolvimento usando `127.0.0.1`, nunca `localhost` — não por preferência estética, mas porque o cookie de sessão do Spring Security (`SameSite=Lax`) e a API `crypto.subtle` (usada pelo PKCE) só se comportam de forma confiável quando tudo está na mesma origem. Isso tem implicações diretas pra produção: **não é só trocar `127.0.0.1` por um hostname público** — alguns pré-requisitos são obrigatórios, não configuráveis.

### 1. HTTPS é obrigatório — não um "nice to have"

`crypto.subtle` (usado por `frontend/src/utils/pkce.js` para gerar o `code_challenge`) só existe em **contextos seguros**: HTTPS, ou HTTP em `localhost`/`127.0.0.1`. Um domínio de produção acessado por `http://` puro (comum em intranets internas) **não é** um contexto seguro — `crypto.subtle` fica `undefined` e o login falha para todo mundo, sem alternativa de configuração. Se o ambiente de destino é uma intranet sem TLS hoje, isso precisa ser resolvido primeiro (certificado interno/CA próprio da organização serve — não precisa ser público) antes de qualquer outro passo abaixo fazer diferença.

### 2. Mesma origem (proxy reverso) — o mesmo motivo do `127.0.0.1` em dev

Em desenvolvimento, o proxy do próprio Vite (`server.proxy` em `frontend/vite.config.js`) faz `/oauth2/**`, `/login`, `/logout` e `/.well-known/**` parecerem, pro navegador, a mesma origem do frontend — é isso que faz o cookie de sessão sobreviver ao fluxo de login (ver a explicação completa nos comentários daquele arquivo). **Esse proxy só existe no dev server do Vite — o build de produção (`frontend-prod`, Nginx) não tem nada equivalente hoje** (`frontend/nginx.conf` serve só os arquivos estáticos da SPA, sem proxiar nada pro backend). Sem corrigir isso, produção reproduziria exatamente o mesmo bug de cookie cross-origin que foi corrigido em desenvolvimento.

A correção é replicar a mesma ideia no Nginx de produção: tudo atrás de **um único host público**, com o Nginx roteando por path para o backend ou para os arquivos estáticos. Exemplo de bloco a adicionar em `frontend/nginx.conf` (ajustar `backend` para o hostname/endereço real do backend nesse ambiente):

```nginx
location ~ ^/(oauth2|login|logout|\.well-known)(/|$) {
    proxy_pass http://backend:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /v1/ {
    proxy_pass http://backend:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Duas pegadinhas já resolvidas em dev que valem repetir aqui:

- **`GET /login` precisa continuar servindo a SPA** (rota do Vue Router), só o `POST /login` é do backend — um bloco `location` só por prefixo de path pegaria os dois. Em Nginx isso exige separar por método (`limit_except` invertido, ou um bloco `location` condicionado ao método via `if`, já que Nginx não tem um equivalente direto ao `bypass()` do Vite) — teste explicitamente que `GET /login` ainda abre a tela de login da SPA depois de configurar isso.
- **Não reescreva o header `Host`** (`proxy_set_header Host $host`, como acima — não `proxy_set_header Host backend:8081`). O Spring Security usa esse header pra reconstruir certas URLs de redirect; sobrescrevê-lo vaza o hostname interno (só resolvível dentro da rede Docker/interna) para o navegador real, que não consegue navegar até lá.

### 3. Variáveis de ambiente — apontar tudo pro domínio público, com HTTPS

!!! tip "Lugar único: o `.env` da raiz do repositório"
    **`.env`** (raiz do repositório, fora do git — `cp .env.example .env`) é o único arquivo que a equipe DevSecOps precisa editar para trocar `127.0.0.1`/`localhost` pelo hostname real de produção em **todos** os serviços de uma vez — backend, `collab` e frontend (dev e produção). `docker-compose.yml` interpola cada `${VAR:-padrão}` a partir dele; nenhuma variável de host fica hardcoded em outro lugar. As únicas duas exceções, e nenhuma delas é hostname: `PROXY_BACKEND_TARGET` (só existe pro proxy do Vite em dev, sem efeito em produção — ver nota no fim desta seção) e o proxy reverso do Nginx do passo 2 acima (`frontend/nginx.conf`, arquivo de configuração, não variável de ambiente).

Todas já são parametrizáveis (ver tabelas acima), só precisam do valor certo. Supondo `https://fab-legis.exemplo.mil.br` como o único host público (backend e frontend atrás do mesmo proxy do passo 2):

| Variável | Valor em produção |
|---|---|
| `APP_OAUTH2_ISSUER` | `https://fab-legis.exemplo.mil.br` |
| `APP_OAUTH2_REDIRECT_URI` | `https://fab-legis.exemplo.mil.br/callback` |
| `APP_FRONTEND_LOGIN_URL` | `https://fab-legis.exemplo.mil.br/login` |
| `OAUTH2_ISSUER` (collab) | `https://fab-legis.exemplo.mil.br` — precisa bater com `APP_OAUTH2_ISSUER` (comparado contra o claim `iss` do token) |
| `OAUTH2_JWKS_URL` (collab) | pode continuar interno (`http://backend:8081/oauth2/jwks`) — é só de onde o `collab` busca as chaves, nunca é exposto ao navegador |
| `VITE_API_BASE_URL` | `https://fab-legis.exemplo.mil.br/v1` |
| `VITE_COLLAB_URL` | `wss://fab-legis.exemplo.mil.br/collab` (ou outro caminho/porta, dependendo de como o `collab` for exposto — ele também precisa estar atrás de TLS, já que a página é servida por HTTPS e não pode abrir um WebSocket `ws://` sem criptografia a partir dela) |

!!! warning "`VITE_*` é build-time, não runtime — sempre reconstrua depois de mudar o `.env`"
    Diferente das variáveis do backend (lidas a cada boot), as `VITE_*` são embutidas no JavaScript estático durante `npm run build`. `docker-compose.yml` já passa `VITE_API_BASE_URL`/`VITE_COLLAB_URL`/`VITE_USE_MOCK_API` do `.env` como *build args* pro `frontend-prod` (`build.args`, lido por `frontend/Dockerfile`) — então editar o `.env` já é o suficiente em termos de **onde** configurar, mas só surte efeito depois de reconstruir a imagem: mudar a variável de um container **já construído**, sem rebuild, não tem efeito nenhum.
    ```bash
    docker compose --profile production up -d --build frontend-prod
    ```

`PROXY_BACKEND_TARGET` não se aplica em produção — é uma variável exclusiva do dev server do Vite (ver comentário em `.env.example`); o Nginx de produção usa o `proxy_pass` fixo no `nginx.conf` (passo 2), não uma variável de ambiente.

### 4. `application-prod.properties` ainda é um rascunho

`backend/src/main/resources/application-prod.properties` hoje tem placeholders de um scaffold anterior a este projeto (origens de CORS genéricas tipo `localhost:4200`, sem `app.oauth2.*`, sem conexão de banco real) — **precisa ser preenchido** antes de rodar com `SPRING_PROFILES_ACTIVE=prod`, com o equivalente de produção do que `application-dev.properties` já tem hoje (`app.oauth2.issuer`, `app.oauth2.redirect-uri`, `app.frontend.login-url`, conexão real do PostgreSQL).

### 5. Chave de assinatura RSA — efêmera por padrão

O par de chaves RSA do Authorization Server é gerado em memória a cada boot (`AuthorizationServerConfig.gerarChaveRsa()`) — todo restart do backend invalida instantaneamente todos os tokens/sessões emitidos antes dele, forçando login de novo em todo mundo. Em dev isso é aceitável; em produção, se restarts forem frequentes (deploy, escala automática), vale considerar carregar a chave de um arquivo/variável de ambiente persistente em vez de gerar uma nova a cada vez — não implementado ainda, fica registrado aqui como próximo passo caso incomode.

### 6. Observabilidade — Actuator/Micrometer, sem coletor incluso

O backend expõe **Spring Boot Actuator** com **Micrometer** (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`), mas só quatro endpoints ficam acessíveis via HTTP (`management.endpoints.web.exposure.include=health,info,metrics,prometheus` em `application.properties`) — nunca `*`: `/actuator/env`, `/actuator/beans`, `/actuator/heapdump` etc. vazam detalhe interno demais pra ficar expostos, mesmo atrás de autenticação, e não são necessários pra observabilidade externa.

| Endpoint | Acesso | Uso |
|---|---|---|
| `GET /actuator/health` | **Sem autenticação** (ver `SecurityConfig`) | Usado pelo healthcheck do `docker-compose.yml` — o orquestrador não tem como enviar um token. `management.endpoint.health.show-details=never` garante que a resposta seja só `{"status":"UP"}`, nunca o detalhe de cada dependência (banco, disco etc.), mesmo sem login |
| `GET /actuator/metrics` | Exige autenticação (JWT, igual a qualquer endpoint de `/v1/**`) | Métricas em JSON, navegáveis uma a uma |
| `GET /actuator/prometheus` | Exige autenticação | Mesmas métricas, formato texto Prometheus — pronto pra qualquer coletor Prometheus-compatible fazer *scrape* |
| `GET /actuator/info` | Exige autenticação | Metadados da build (vazio hoje, sem `spring-boot-maven-plugin` build-info configurado) |

!!! warning "Nenhum coletor/visualizador faz parte deste repositório de propósito"
    Prometheus, Grafana, Zabbix, Datadog ou o que a organização já usa pra monitorar outros sistemas — a escolha e a hospedagem desse serviço são decisão de infraestrutura de quem hospeda o FAB Legis, o mesmo raciocínio de "lugar único pra configurar hostname" já explicado acima. Este projeto só garante o lado de **expor** dados de forma padrão (`/actuator/prometheus`); **coletar/visualizar** é responsabilidade externa.

    `/actuator/metrics` e `/actuator/prometheus` hoje exigem o mesmo login OAuth2 de qualquer usuário do sistema — não há uma credencial de serviço dedicada. Um coletor automatizado (ex.: um *scraper* do Prometheus) **não consegue fazer login interativo**, então, ao conectar um coletor de verdade, quem administra a infraestrutura precisa escolher entre: (a) isolar esses endpoints numa rede interna que o coletor já alcança sem passar pelo proxy público (mais comum — ex.: `management.server.port` numa porta separada, não exposta no `docker-compose.yml`), ou (b) implementar uma credencial de serviço dedicada. Nenhuma das duas está implementada — decisão para quando houver um coletor real a conectar.

### Checklist antes de expor ao público

1. HTTPS ativo no domínio de produção (certificado interno serve).
2. Proxy reverso configurado (`/oauth2/**`, `/login` POST, `/logout`, `/.well-known/**`, `/v1/**` → backend; resto → estático), sem `Host` sobrescrito.
3. Todas as variáveis da tabela acima apontando para o domínio público com `https://`/`wss://`.
4. Frontend reconstruído (não só reiniciado) depois de qualquer mudança em `VITE_*`.
5. `application-prod.properties` preenchido com configuração real.
6. Testar o fluxo de login completo (não só que a tela carrega) — é o único jeito de pegar um `Host` sobrescrito ou uma rota de proxy faltando antes que um usuário real esbarre nisso.
7. Se for conectar um coletor de métricas externo, decidir como ele vai autenticar contra `/actuator/prometheus` (ver seção 6) — não deixar a decisão pra depois do deploy.

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
