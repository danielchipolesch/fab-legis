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
