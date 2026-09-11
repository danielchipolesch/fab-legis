# Instalação e Configuração

## Pré-requisitos

- **Docker** e **Docker Compose** (caminho recomendado), ou
- **JDK 25** + **Node.js 22** + **PostgreSQL 16** para execução local.

## Opção 1 — Docker Compose (recomendado)

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis

# Sobe PostgreSQL, MinIO, backend, collab (colaboração em tempo real), frontend
# e a documentação técnica
docker compose up -d

# Acompanhar os logs
docker compose logs -f backend frontend
```

**Serviços disponíveis:**

| Serviço | URL | Credenciais |
|---|---|---|
| Frontend (dev) | http://localhost:5173 | — |
| Backend (API) | http://localhost:8081 | — |
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

    A API sobe em `http://localhost:8081` com o perfil `dev` ativo.

=== "Frontend"
    ```bash
    cd frontend
    cp .env.example .env.local      # ajuste VITE_API_BASE_URL se necessário
    npm install
    npm run dev                     # http://localhost:5173
    ```

    Scripts disponíveis:

    | Comando | Descrição |
    |---|---|
    | `npm run dev` | Servidor de desenvolvimento com HMR |
    | `npm run build` | Build de produção em `dist/` |
    | `npm run preview` | Pré-visualização do build |

## Variáveis de ambiente

**Backend**

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `fab-legis-dev` | Conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `123456` | Credenciais do banco |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Endpoint S3 do MinIO |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | `minioadmin` / `minioadmin123` | Credenciais do MinIO |
| `MINIO_BUCKET` | `fab-legis-imagens` | Bucket das imagens |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL pública para servir as imagens |
| `JWT_SECRET` | *(dev, troque em produção)* | Segredo de assinatura do access token JWT |
| `JWT_EXPIRATION_MS` | `900000` (15 min) | Validade do access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 dias) | Validade do refresh token |
| `APP_ADMIN_CPF` / `APP_ADMIN_SENHA` / `APP_ADMIN_NOME` | ver acima | Usuário administrador padrão, criado só no primeiro boot |

**Collab** (serviço de colaboração em tempo real, ver [Arquitetura](arquitetura.md))

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | *(mesmo valor do backend)* | **Precisa ser idêntico** ao `JWT_SECRET` do backend — o `collab` valida o mesmo token, não emite o seu próprio |
| `BACKEND_URL` | `http://backend:8081/v1` | Onde o `collab` busca/persiste o conteúdo de cada elemento (`GET`/`PATCH /documentos/...`) |
| `PORT` | `1234` | Porta do servidor WebSocket (Hocuspocus) |

**Frontend**

| Variável | Padrão | Descrição |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8081/v1` | URL base da API |
| `VITE_COLLAB_URL` | `ws://127.0.0.1:1234` | URL do serviço de colaboração em tempo real (WebSocket) |
| `VITE_APP_ENV` | `development` | Ambiente (`development` \| `staging` \| `production`) |

!!! warning "Atenção"
    As credenciais acima são valores de desenvolvimento. Em produção, substitua todas por *secrets* gerenciados fora do repositório.

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
