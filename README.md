<div align="center">

# 📘 FAB Legis

**Sistema de elaboração, padronização e gestão de atos normativos do Comando da Aeronáutica**

*Do rascunho à publicação — com numeração automática, editor WYSIWYG colaborativo em tempo real e exportação em PDF e HTML*

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![OAuth2](https://img.shields.io/badge/OAuth2-Authorization%20Code%20%2B%20PKCE-3C3C3D)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![Quasar](https://img.shields.io/badge/Quasar-2.17-1976D2)
![TipTap](https://img.shields.io/badge/TipTap-2.x%20%2B%20Yjs-0F0F0F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791)
![MinIO](https://img.shields.io/badge/MinIO-on--premise-C72E49)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![License](https://img.shields.io/badge/License-GPLv3-blue.svg)

📖 **[Documentação técnica completa](docs/index.md)**

</div>

---

## O que é

O FAB Legis trata o ato normativo (ICA, NSCA, MCA, RCA, DCA, PCA, OCA, RICA, ROCA, TCA, FCA, além da **NPA** — Norma Padrão de Ação, espécie de Comunicação Oficial Padronizada com regras próprias) não como um arquivo de texto, mas como uma estrutura de dados hierárquica: cada capítulo, artigo, parágrafo, inciso e alínea é um nó em uma árvore. Isso permite:

- **numeração automática** conforme a técnica legislativa (LC 95/1998, Decreto nº 12.002/2024, NSCA 5-3), e novos documentos já nascendo com os capítulos padronizados da NSCA 5-3;
- **edição colaborativa em tempo real** (CRDT/Yjs) por elemento, com preview lado a lado, tabelas, figuras e aviso de presença;
- **exportação fiel em PDF e HTML**, a partir do mesmo conteúdo, e **comparação de versões** para o ciclo de emenda;
- **fluxo de revisão e publicação** com papéis (Editor, Aprovador, Publicador, Auditor, Admin), atribuição pessoal por etapa, situação BCA × situação local, notificações e trilha de auditoria;
- **busca textual** no conteúdo de todos os documentos;
- **autenticação OAuth2** (Authorization Code + PKCE) com login por CPF, emitida pelo próprio backend.

Para o detalhamento de funcionalidades, arquitetura, modelo de domínio, autenticação, ciclo de vida do documento e API, veja a **[documentação técnica](docs/index.md)** — sobe junto com `docker compose up -d` (ver abaixo), disponível em `http://localhost:8000`. A instalação (desenvolvimento e produção) e a configuração ficam **aqui, neste README**; a documentação técnica trata do sistema em si e de como testá-lo.

## Instalação em desenvolvimento

Ambiente de quem vai **desenvolver ou avaliar** o sistema, na própria máquina. Para colocá-lo no ar para outras pessoas, ver [Instalação em produção](#instalação-em-produção).

**Pré-requisitos** — um dos dois caminhos:

- **Docker** e **Docker Compose** (recomendado: nada mais precisa estar instalado); ou
- **JDK 25** + **Node.js 22** + **PostgreSQL 18** + MinIO (os dois últimos podem subir pelo Docker, ver o caminho B).

### Caminho A — tudo no Docker Compose (recomendado)

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis
cp .env.example .env   # opcional: sem ele, o compose usa os mesmos valores de desenvolvimento
docker compose up -d   # PostgreSQL, MinIO, backend, collab, frontend e a documentação técnica
docker compose logs -f backend frontend   # acompanhar os logs
```

O `.env` da raiz (fora do git; só o `.env.example` é versionado) é lido automaticamente pelo Compose para preencher os `${VAR:-padrão}` do `docker-compose.yml` e é o **mesmo arquivo** usado pelo frontend rodando fora do Docker (caminho B).

| Serviço | URL | Credenciais |
|---|---|---|
| Frontend | http://127.0.0.1:5173 | — |
| Backend (API) | http://127.0.0.1:8081 | — |
| Swagger UI | http://127.0.0.1:8081/swagger-ui.html | — |
| OpenAPI JSON | http://127.0.0.1:8081/v1/fab-legis-api | — |
| Colaboração em tempo real (WebSocket) | ws://127.0.0.1:1234 | — |
| Documentação técnica | http://localhost:8000 | — |
| PostgreSQL | `localhost:5432` | `postgres` / `123456` |
| MinIO (console) | http://localhost:9001 | `minioadmin` / `minioadmin123` |

> **Acesse o frontend por `127.0.0.1`, nunca por `localhost`.** O login OAuth2 (`redirect_uri`, cookie de sessão e `sessionStorage`) é configurado para `127.0.0.1`; por `localhost` o fluxo trava em `{"message":"Não autenticado."}`. As variáveis `APP_OAUTH2_ISSUER`, `APP_OAUTH2_REDIRECT_URI`, `APP_FRONTEND_LOGIN_URL`, `VITE_API_BASE_URL` e `VITE_COLLAB_URL` usam `127.0.0.1` de propósito — se mudar uma, mude todas juntas, mantendo o mesmo host.

**Usuário administrador padrão** — criado pelo `DataSeeder` no primeiro boot, só se ainda não existir nenhum usuário com login: CPF `111.444.777-35` (dígitos `11144477735`), senha `Admin@123`, papéis **Admin + Editor** (o suficiente para gerenciar usuários e já criar o primeiro documento; Admin sozinho não bastaria — ver [Autenticação](docs/autenticacao.md#papeis-e-posse-de-documento)). Troque a senha (ou `APP_ADMIN_CPF`/`APP_ADMIN_SENHA`, antes do primeiro boot) em qualquer ambiente que não seja local.

### Caminho B — backend e frontend fora do Docker

Útil para depurar. Sobe só a infraestrutura pelo Docker e roda o código na máquina:

```bash
docker compose up -d postgres minio

# Backend — API em http://127.0.0.1:8081, perfil dev
cd backend
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows

# Frontend — http://127.0.0.1:5173 (outro terminal)
cp .env.example .env            # na raiz, se ainda não existir; ajuste VITE_API_BASE_URL se preciso
cd frontend
npm install
npm run dev
```

O `.env` lido pelo frontend é o da raiz (`frontend/vite.config.js` aponta o `envDir` do Vite para lá); não existe `.env` separado dentro de `frontend/`. Para a colaboração em tempo real e a documentação, suba também `docker compose up -d collab docs`.

| Comando do frontend | Descrição |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com HMR |
| `npm run build` | Build de produção em `dist/` |
| `npm run preview` | Pré-visualização do build |

### Cuidados do ambiente de desenvolvimento

- **Migrations reescritas.** O sistema ainda não está em produção, então as migrations (`backend/src/main/resources/db/migration`) são *reescritas* — números reaproveitados. Depois de um `git pull` que mexa nelas, recrie o volume do Postgres, senão o backend não sobe (`checksum mismatch`): `docker compose rm -sf postgres && docker volume rm fab-legis_postgres_18_data` (`docker compose down -v` apaga também o volume do MinIO). Faça backup antes se houver dado que importe.
- **A chave RSA do Authorization Server é efêmera** (gerada a cada boot): reiniciar o backend invalida todos os tokens — é preciso logar de novo.
- **Vindo do PostgreSQL 16?** O Compose usa `postgres:18-alpine` com o volume `postgres_18_data`, montado em `/var/lib/postgresql` (a partir da versão 18 o `PGDATA` da imagem é `/var/lib/postgresql/18/docker`). Um volume da versão 16 (`postgres_data`) **não é compatível** e fica intocado. Para levar os dados: com o container antigo no ar, `docker exec fab-legis-postgres pg_dump -U postgres --no-owner fab-legis-dev > backup.sql`; suba o novo e restaure com `psql -U postgres -d fab-legis-dev < backup.sql`; depois de conferir, `docker volume rm fab-legis_postgres_data`.
- **Git Bash no Windows:** `docker exec` com caminho absoluto precisa de `MSYS_NO_PATHCONV=1`.

---

## Instalação em produção

Para expor o sistema a usuários reais. **Subir o Compose com o perfil `production` sozinho não basta** — o frontend compilado (Nginx, porta 80) só funciona fora de `127.0.0.1` depois dos passos abaixo. Todos são obrigatórios, não opcionais.

```bash
cp .env.example .env     # preencha com os valores de produção (passo 3) e com segredos próprios
docker compose --profile production up -d --build
```

O perfil sobe o serviço `frontend-prod` (build estático servido por Nginx) no lugar do `frontend` de desenvolvimento.

**Contexto:** o login (OAuth2 Authorization Code + PKCE, ver [Autenticação](docs/autenticacao.md)) foi validado em desenvolvimento com `127.0.0.1`, nunca `localhost`, porque o cookie de sessão do Spring Security (`SameSite=Lax`) e a API `crypto.subtle` (PKCE) só se comportam de forma confiável com tudo na mesma origem. Em produção isso não se resolve trocando `127.0.0.1` por um hostname: exige os pré-requisitos a seguir.

### 1. HTTPS é obrigatório

`crypto.subtle` (usado em `frontend/src/utils/pkce.js` para o `code_challenge`) só existe em **contextos seguros**: HTTPS, ou HTTP em `localhost`/`127.0.0.1`. Um domínio acessado por `http://` puro (comum em intranets) **não é** contexto seguro: `crypto.subtle` fica `undefined` e o login falha para todo mundo, sem configuração que contorne. Numa intranet sem TLS, resolva isso primeiro — um certificado interno/CA da organização serve; não precisa ser público.

### 2. Mesma origem (proxy reverso)

Em desenvolvimento, o proxy do Vite (`server.proxy` em `frontend/vite.config.js`) faz `/oauth2/**`, `/login`, `/logout` e `/.well-known/**` parecerem, para o navegador, a mesma origem do frontend — é isso que faz o cookie de sessão sobreviver ao login. **Esse proxy só existe no dev server:** o `frontend/nginx.conf` de produção serve só os arquivos estáticos. É preciso replicá-lo no Nginx: tudo atrás de **um único host público**, roteando por path. Exemplo para acrescentar ao `frontend/nginx.conf` (ajuste `backend` ao endereço real):

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

Duas pegadinhas já resolvidas em desenvolvimento:

- **`GET /login` tem de continuar servindo a SPA** (rota do Vue Router); só o `POST /login` é do backend, e um `location` por prefixo pegaria os dois. No Nginx é preciso separar por método — teste explicitamente que `GET /login` ainda abre a tela de login depois de configurar.
- **Não reescreva o `Host`** (`proxy_set_header Host $host`, não `Host backend:8081`): o Spring Security usa esse header para montar URLs de redirect, e sobrescrevê-lo vaza o hostname interno para o navegador, que não consegue chegar lá.

### 3. Variáveis de ambiente — tudo apontando para o domínio público, com HTTPS

O **`.env` da raiz** é o único arquivo a editar para trocar `127.0.0.1` pelo host real em **todos** os serviços (backend, `collab` e frontend); o `docker-compose.yml` interpola cada `${VAR:-padrão}` a partir dele. As únicas exceções, e nenhuma é hostname: `PROXY_BACKEND_TARGET` (só existe para o proxy do Vite em desenvolvimento) e o `frontend/nginx.conf` do passo 2 (arquivo de configuração). A lista completa está em [Variáveis de ambiente](#variáveis-de-ambiente). Supondo `https://fab-legis.exemplo.mil.br` como o único host público:

| Variável | Valor em produção |
|---|---|
| `APP_OAUTH2_ISSUER` | `https://fab-legis.exemplo.mil.br` |
| `APP_OAUTH2_REDIRECT_URI` | `https://fab-legis.exemplo.mil.br/callback` |
| `APP_FRONTEND_LOGIN_URL` | `https://fab-legis.exemplo.mil.br/login` |
| `OAUTH2_ISSUER` (collab) | `https://fab-legis.exemplo.mil.br` — tem de bater com `APP_OAUTH2_ISSUER` (é comparado ao claim `iss` do token) |
| `OAUTH2_JWKS_URL` (collab) | pode continuar interno (`http://backend:8081/oauth2/jwks`) — só de onde o `collab` busca as chaves; nunca chega ao navegador |
| `VITE_API_BASE_URL` | `https://fab-legis.exemplo.mil.br/v1` |
| `VITE_COLLAB_URL` | `wss://fab-legis.exemplo.mil.br/collab` (ou outro caminho/porta, conforme a exposição do `collab`) — também atrás de TLS: uma página HTTPS não abre WebSocket `ws://` |

> **`VITE_*` é de build, não de execução — reconstrua depois de mudar o `.env`.** Diferente das variáveis do backend (lidas a cada boot), as `VITE_*` são embutidas no JavaScript durante `npm run build`. O `docker-compose.yml` já as passa como *build args* ao `frontend-prod`, então editar o `.env` basta em termos de **onde** configurar, mas só surte efeito depois de `docker compose --profile production up -d --build frontend-prod`. Mudar a variável de um container já construído não tem efeito.

Troque também **todas** as credenciais de desenvolvimento (banco, MinIO, administrador padrão) por segredos gerenciados fora do repositório.

### 4. `application-prod.properties` ainda é um rascunho

`backend/src/main/resources/application-prod.properties` tem placeholders (CORS genérico tipo `localhost:4200`, sem `app.oauth2.*`, sem conexão real de banco). **Preencha** antes de rodar com `SPRING_PROFILES_ACTIVE=prod`, com o equivalente de produção do que `application-dev.properties` já tem (`app.oauth2.issuer`, `app.oauth2.redirect-uri`, `app.frontend.login-url`, conexão real do PostgreSQL).

### 5. Chave de assinatura RSA — efêmera por padrão

O par de chaves RSA do Authorization Server é gerado em memória a cada boot (`AuthorizationServerConfig.gerarChaveRsa()`): todo restart do backend invalida os tokens e sessões emitidos antes, e todos precisam logar de novo. Em produção, se os restarts forem frequentes (deploy, escala automática), considere carregar a chave de um arquivo ou variável persistente — não implementado ainda.

### 6. Observabilidade — Actuator/Micrometer, sem coletor incluso

O backend expõe o **Spring Boot Actuator** com **Micrometer**, mas só quatro endpoints ficam acessíveis por HTTP (`management.endpoints.web.exposure.include=health,info,metrics,prometheus`) — nunca `*`: `/actuator/env`, `/beans`, `/heapdump` etc. vazam detalhe demais.

| Endpoint | Acesso | Uso |
|---|---|---|
| `GET /actuator/health` | **Sem autenticação** | Healthcheck do `docker-compose.yml` (o orquestrador não envia token). `show-details=never`: a resposta é só `{"status":"UP"}` |
| `GET /actuator/metrics` | Autenticado (JWT) | Métricas em JSON |
| `GET /actuator/prometheus` | Autenticado | Mesmas métricas, em formato texto Prometheus |
| `GET /actuator/info` | Autenticado | Metadados da build (vazio hoje) |

Nenhum coletor/visualizador (Prometheus, Grafana, Zabbix…) faz parte deste repositório: a escolha e a hospedagem são decisão de infraestrutura de quem hospeda. Como `/actuator/metrics` e `/actuator/prometheus` exigem o login OAuth2 e um coletor automático não consegue fazer login interativo, ao conectar um coletor real é preciso escolher entre (a) isolar esses endpoints numa rede interna que o coletor alcance sem passar pelo proxy público (mais comum — ex.: `management.server.port` numa porta não exposta) ou (b) implementar uma credencial de serviço dedicada. Nenhuma das duas está implementada.

### Checklist antes de expor ao público

1. HTTPS ativo no domínio de produção (certificado interno serve).
2. Proxy reverso configurado (`/oauth2/**`, `/login` POST, `/logout`, `/.well-known/**`, `/v1/**` → backend; o resto → estático), sem `Host` sobrescrito.
3. Variáveis do passo 3 apontando para o domínio público com `https://`/`wss://`; credenciais de desenvolvimento trocadas.
4. Frontend reconstruído (não só reiniciado) depois de qualquer mudança em `VITE_*`.
5. `application-prod.properties` preenchido com a configuração real.
6. Fluxo de login testado de ponta a ponta (não só que a tela carrega) — o único jeito de pegar um `Host` sobrescrito ou uma rota de proxy faltando antes de um usuário real esbarrar nisso.
7. Se for conectar um coletor de métricas, decidir antes do deploy como ele vai autenticar (passo 6).

---

## Variáveis de ambiente

Valem para o desenvolvimento e para a produção. Via Docker Compose, todas (exceto `PORT` do collab, que não tem override no compose) são ajustadas pelo `.env` da raiz, sem editar o `docker-compose.yml`. Os padrões abaixo são de **desenvolvimento**.

**Backend**

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `fab-legis-dev` | Conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `123456` | Credenciais do banco |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Endpoint S3 do MinIO |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | `minioadmin` / `minioadmin123` | Credenciais do MinIO |
| `MINIO_BUCKET` | `fab-legis-imagens` | Bucket das imagens |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL pública para servir as imagens |
| `APP_PDF_MAX_CONCORRENTES` / `APP_PDF_ESPERA_MAXIMA_SEGUNDOS` | `2` / `30` | Renderizações de PDF simultâneas por instância e quanto um pedido espera por uma vaga antes do `503` — ver [Limite de gerações simultâneas de PDF](docs/exportacao-pdf.md#limite-de-geracoes-simultaneas-de-pdf) |
| `APP_PDF_CACHE_TTL_DIAS` | `2` | Dias até o cache de PDF/HTML em tramitação expirar no MinIO — ver [Cache de PDF/HTML em tramitação](docs/exportacao-pdf.md#cache-de-pdfhtml-em-tramitacao) |
| `APP_OAUTH2_ISSUER` | `http://127.0.0.1:8081` | Endereço que o navegador usa para o Authorization Server (vira o claim `iss` do token — collab e frontend precisam concordar) |
| `APP_OAUTH2_REDIRECT_URI` | `http://127.0.0.1:5173/callback` | Rota de callback da SPA (troca o `code` por token) |
| `APP_FRONTEND_LOGIN_URL` | `http://127.0.0.1:5173/login` | Tela de login (Vue) para onde o Authorization Server manda usuários não autenticados |
| `APP_ADMIN_CPF` / `APP_ADMIN_SENHA` / `APP_ADMIN_NOME` | `11144477735` / `Admin@123` / `Administrador` | Usuário administrador padrão, criado só no primeiro boot |

**Collab** (colaboração em tempo real, ver [Arquitetura](docs/arquitetura.md))

| Variável | Padrão | Descrição |
|---|---|---|
| `OAUTH2_ISSUER` | `http://127.0.0.1:8081` | Tem de bater com `APP_OAUTH2_ISSUER` do backend — é checado contra o claim `iss` do token |
| `OAUTH2_JWKS_URL` | `http://backend:8081/oauth2/jwks` | Endereço **interno** (rede Docker) de onde o `collab` busca as chaves públicas; pode diferir do `OAUTH2_ISSUER` |
| `BACKEND_URL` | `http://backend:8081/v1` | Onde o `collab` busca e persiste o conteúdo de cada elemento |
| `PORT` | `1234` | Porta do servidor WebSocket (Hocuspocus) |

**Frontend**

| Variável | Padrão | Descrição |
|---|---|---|
| `VITE_API_BASE_URL` | `http://127.0.0.1:8081/v1` | URL base da API |
| `PROXY_BACKEND_TARGET` | `http://127.0.0.1:8081` | **Não** é `VITE_*` de propósito: só o processo Node do Vite (dev) a usa para proxiar `/oauth2/**`, `/login`, `/logout`; nunca chega ao navegador. No Docker vale `http://backend:8081` |
| `VITE_COLLAB_URL` | `ws://127.0.0.1:1234` | URL do serviço de colaboração em tempo real (WebSocket) |
| `VITE_USE_MOCK_API` | `false` | Usa API mockada em vez do backend real |
| `VITE_APP_ENV` | `development` | `development` \| `staging` \| `production` — só afeta o build local (sourcemap em `staging`); o build via Docker sempre roda como `development` |
| `VITE_APP_VERSION` | `1.0.0` | Versão exibida em logs/telas de diagnóstico |

---

## Testes

Backend (JUnit 5 + Mockito; os de integração usam PostgreSQL num container) e frontend (Vitest). Os comandos — inclusive o do backend, que roda num container descartável, sem exigir JDK/Maven — estão em **[Testes e qualidade](docs/testes.md)**. Para o frontend:

```bash
docker compose run --rm --build --no-deps frontend npm test
```

## Contribuindo

O desenvolvimento ocorre na branch **`desenvolvimento`**. Ao contribuir:

1. Crie sua branch a partir de `desenvolvimento`;
2. Mantenha o padrão de camadas do backend e a separação de responsabilidades do frontend (ver [Arquitetura](docs/arquitetura.md)); o que varia por espécie normativa entra por interface (`domain.regras` no backend, `frontend/src/perfis/` no frontend), nunca por `if` na sigla da espécie;
3. Mudou uma regra de negócio? Atualize na mesma tarefa o **teste unitário** correspondente e a **página de `docs/`** que a descreve — as convenções (linguagem ubíqua em português para o domínio, consistência entre PDF, HTML e prévia) estão no [`CLAUDE.md`](CLAUDE.md);
4. Escreva mensagens de commit descritivas em português;
5. Abra um Pull Request para `desenvolvimento`.

## Licença

Copyright (C) 2026 Comando da Aeronáutica (FAB).

Este programa é software livre: você pode redistribuí-lo e/ou modificá-lo sob os termos da
**Licença Pública Geral GNU (GPL)**, conforme publicada pela Free Software Foundation, na versão 3
da licença, ou (a seu critério) qualquer versão posterior.

Este programa é distribuído na esperança de que seja útil, mas **SEM NENHUMA GARANTIA**; nem mesmo a
garantia implícita de **COMERCIALIZAÇÃO** ou **ADEQUAÇÃO A UM PROPÓSITO ESPECÍFICO**. Veja a Licença
Pública Geral GNU para mais detalhes.

O texto completo da licença está em [`LICENSE`](LICENSE) (também disponível em
[gnu.org/licenses/gpl-3.0](https://www.gnu.org/licenses/gpl-3.0.html)).

---

<div align="center">

**FAB Legis** — padronizando a atividade normativa do Comando da Aeronáutica.

</div>
