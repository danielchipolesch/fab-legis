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
- **fluxo de revisão e publicação** com perfis (Editor, Aprovador, Publicador, Auditor, Admin), envio para aprovação sem atribuição pessoal, situação BCA × situação local, notificações e trilha de auditoria;
- **busca textual** no conteúdo de todos os documentos;
- **autenticação OAuth2** (Authorization Code + PKCE) com login por CPF, emitida pelo próprio backend.

Para o detalhamento de funcionalidades, arquitetura, modelo de domínio, autenticação, ciclo de vida do documento e API, veja a **[documentação técnica](docs/index.md)** — sobe junto com `docker compose up -d` (ver abaixo) e fica em `http://localhost:8000` (serviço próprio, com o seu Nginx; a tela de login tem um link para ela — ver `VITE_DOCS_URL`). A instalação (desenvolvimento e produção) e a configuração ficam **aqui, neste README**; a documentação técnica trata do sistema em si e de como testá-lo.

## Instalação em desenvolvimento

Ambiente de quem vai **desenvolver ou avaliar** o sistema, na própria máquina. Para colocá-lo no ar para outras pessoas, ver [Instalação em produção](#instalação-em-produção); todas as variáveis estão em [Variáveis de ambiente](#variáveis-de-ambiente).

### Pré-requisitos

- **Docker** e **Docker Compose** (caminho recomendado), ou
- **JDK 25** + **Node.js 22** + **PostgreSQL 18** para execução local.

> **Vindo do PostgreSQL 16?**
>
> O Compose usa `postgres:18-alpine` com o volume `postgres_18_data`, montado em `/var/lib/postgresql` (a partir da versão 18 o `PGDATA` da imagem é `/var/lib/postgresql/18/docker`). Um volume criado pela versão 16 (`postgres_data`) **não é compatível** com a 18 e fica intocado: para levar os dados, faça `pg_dump` do banco antigo (`docker exec fab-legis-postgres pg_dump -U postgres --no-owner fab-legis-dev > backup.sql`, com o container antigo ainda no ar), suba o novo e restaure com `psql -U postgres -d fab-legis-dev < backup.sql`. Depois de conferir, remova o volume antigo (`docker volume rm fab-legis_postgres_data`).
>
### Caminho A — tudo no Docker Compose (recomendado)

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis

# O .env deste ambiente (fora do git; o modelo versionado é o .env.example). O compose lê os valores dele; para
# desenvolvimento os padrões servem, e é aqui que se ajusta credenciais e portas sem tocar no docker-compose.yml
cp .env.example .env

# Sobe PostgreSQL, MinIO, backend, collab (colaboração em tempo real), frontend
# e a documentação técnica
docker compose up -d

# Acompanhar os logs
docker compose logs -f backend frontend
```

O `.env` na raiz (fora do git, só o `.env.example` é versionado) é lido automaticamente pelo `docker compose` para preencher os `${VAR:-padrão}` do `docker-compose.yml` — sem ele, cada variável cai no próprio padrão já embutido no compose. É o **mesmo arquivo** usado pelo Caminho B (frontend rodando fora do Docker via `npm run dev`) — ver abaixo.

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

> **Acesse o frontend por `127.0.0.1`, nunca por `localhost`.** O login OAuth2 (`redirect_uri`, cookie de sessão e `sessionStorage`) é configurado para `127.0.0.1`; por `localhost` o fluxo trava em `{"message":"Não autenticado."}`. Ver o aviso completo em [Variáveis de ambiente](#variáveis-de-ambiente).

**Habilitação de documentos:** só o GABAER e o CENDOC criam as espécies convencionais no primeiro boot (a NPA, qualquer OM); o administrador habilita as demais OMs na tela *Manter Habilitação de Espécies Normativas por OM*. O administrador padrão nasce na OM do CENDOC, por isso já cria documentos. A antiga OM provisória "SISTEMA" deixou de existir (migração `V15`: quem estava nela foi para o CENDOC).

**Usuário administrador padrão** — criado automaticamente pelo `DataSeeder` no primeiro boot, só se nenhum usuário real ainda existir. CPF `111.444.777-35` (dígitos: `11144477735`), senha `Admin@123`, posto CP, nome de guerra CHIPOLESCH, e-mail `chipoleschdaca@fab.mil.br`, na OM CENDOC, perfis **Administrador Geral (`ADM_GER`) + Elaborador (`ELB`) + Gestor Geral (`GES_GER`, o Painel Gerencial de todas as OMs)** (o suficiente para gerenciar usuários e já criar o primeiro documento, sem precisar se auto-conceder um perfil antes de usar o sistema — ver [Autenticação e Colaboração](docs/autenticacao.md#perfis-e-posse-de-documento) sobre por que o administrador sozinho não bastaria). Troque a senha (ou os valores de `APP_ADMIN_CPF`/`APP_ADMIN_SENHA` antes do primeiro boot) assim que possível em qualquer ambiente que não seja local.

### Caminho B — backend e frontend fora do Docker

**Backend**

```bash
# Suba apenas as dependências de infraestrutura
docker compose up -d postgres minio

cd backend
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

A API sobe em `http://127.0.0.1:8081`, com os valores padrão de desenvolvimento (não há perfis `dev`/`prod`: a configuração é uma só, em `application.properties`, e tudo que varia vem de variáveis de ambiente).

**Frontend**

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


### Cuidados do ambiente de desenvolvimento

- **Migrations reescritas.** O sistema ainda não está em produção, então as migrations (`backend/src/main/resources/db/migration`) são *reescritas* — números reaproveitados. Depois de um `git pull` que mexa nelas, recrie o volume do Postgres, senão o backend não sobe (`checksum mismatch`): `docker compose rm -sf postgres && docker volume rm fab-legis_postgres_18_data` (`docker compose down -v` apaga também o volume do MinIO). Faça backup antes se houver dado que importe.
- **A chave RSA do Authorization Server é efêmera** (gerada a cada boot): reiniciar o backend invalida todos os tokens — é preciso logar de novo.
- **Git Bash no Windows:** `docker exec` com caminho absoluto precisa de `MSYS_NO_PATHCONV=1`.

---

---

## Instalação em produção

Para expor o sistema a usuários reais. A diferença entre os ambientes está em **dois arquivos de configuração por ambiente, fora do git**, escritos a partir de modelos versionados — o resto (código, imagens, `docker-compose.yml`) é o mesmo:

| Arquivo (fora do git) | Modelo (versionado) | O que decide |
|---|---|---|
| `.env` | `.env.example` | endereços, credenciais, opções, e **quais serviços sobem** (`COMPOSE_PROFILES`) |
| `segredos/tls/` | — | certificado e chave privada do **HTTPS** (só em produção); lidos só pelo Nginx |
| `segredos/jwt/` | — | a chave RSA que **assina os tokens de login** (`jwt-rsa.pem`); lida só pelo backend |

**A pasta `segredos/`** reúne tudo o que é segredo de um ambiente e não pode ir para o git: dentro dela, `tls/` e `jwt/` ficam **separadas de propósito** (cada container monta só a sua — privilégio mínimo — e os dois têm ciclos de vida diferentes: renovar o certificado não derruba sessões; trocar a chave dos tokens, sim). Só as pastas e os `.gitkeep` são versionados; o conteúdo é ignorado pelo git (`segredos/*/*`).

**Perfis do Compose.** Cada serviço do `docker-compose.yml` sobe sempre, menos o frontend, que tem versão de desenvolvimento (`dev`: Vite com recarga) e de produção (`production`: arquivos compilados atrás do Nginx). Quem escolhe é o `COMPOSE_PROFILES` do `.env` — em desenvolvimento `dev` (o padrão do `.env.example`), em produção `production`. Assim `docker compose up -d` faz a coisa certa em cada ambiente e o frontend de desenvolvimento nunca sobe em produção. Não há segundo arquivo de Compose.

**Portas — por que só em `127.0.0.1`.** Por padrão, uma porta publicada pelo Docker (`ports: "5432:5432"`) abre para **todas as interfaces da máquina**, ou seja, para qualquer um na rede — e o Docker ainda **contorna o firewall do sistema** (ufw/firewalld) ao fazer isso. Antes, o Compose publicava assim o PostgreSQL (com a senha de desenvolvimento `123456`), o console do MinIO, a API (que permitiria falar com o backend sem passar pelo Nginx) e o `collab`: qualquer pessoa na rede podia tentar entrar neles. Era uma falha real de segurança, e por isso as portas internas agora são publicadas só em `127.0.0.1` (`"127.0.0.1:5432:5432"`): servem à depuração **na própria máquina** e não existem para a rede. **Você não precisa mudar nada** — em desenvolvimento o navegador está na mesma máquina e continua funcionando. Em produção o único caminho de fora para dentro é o Nginx (80/443); os serviços conversam entre si pela rede interna do Docker (`backend:8081`, `minio:9000`...), que não depende dessas portas publicadas. A única exceção possível é `DOCS_BIND=0.0.0.0`, se a documentação (porta 8000) for pública.

```bash
cp .env.example .env                          # ajuste: COMPOSE_PROFILES=production e os valores do passo 3
cp nginx/nginx.https.conf.example nginx/nginx.conf   # (ou nginx.conf.example, se o TLS terminar fora, num balanceador)
# coloque o certificado em segredos/tls/ (passo 1) e então:
docker compose up -d --build
```

**Subir assim sozinho não basta** para funcionar fora de `localhost`/`127.0.0.1`: os passos 1 a 5 abaixo são obrigatórios (o 6 trata de monitoramento), e o [checklist](#checklist-antes-de-expor-ao-público) no fim confere tudo.

O fluxo de login (OAuth2 Authorization Code + PKCE, ver [Autenticação e Colaboração](docs/autenticacao.md)) foi validado em desenvolvimento usando `127.0.0.1`, nunca `localhost` — não por preferência estética, mas porque o cookie de sessão do Spring Security (`SameSite=Lax`) e a API `crypto.subtle` (usada pelo PKCE) só se comportam de forma confiável quando tudo está na mesma origem. Isso tem implicações diretas pra produção: **não é só trocar `127.0.0.1` por um hostname público** — alguns pré-requisitos são obrigatórios, não configuráveis.

### 1. HTTPS é obrigatório — e como configurar

`crypto.subtle` (usado por `frontend/src/utils/pkce.js` para gerar o `code_challenge`) só existe em **contextos seguros**: HTTPS, ou HTTP em `localhost`/`127.0.0.1`. Um domínio acessado por `http://` puro (comum em intranets) **não é** contexto seguro: `crypto.subtle` fica `undefined` e o login falha para todo mundo, sem configuração que contorne. Numa intranet sem TLS, resolva isso primeiro — um certificado interno/CA da organização serve; não precisa ser público.

**Opção A — o Nginx do sistema termina o TLS** (a mais simples; o que o repositório já traz pronto):

1. **Nome do host:** peça à rede o nome (ex.: `fab-legis.intraer`) apontando para o servidor, e um certificado para esse nome — da CA interna da FAB (ou Let's Encrypt, se o servidor for público).
2. **Certificado:** coloque em `segredos/tls/` o `fullchain.pem` (certificado + intermediárias, nessa ordem) e o `privkey.pem` (chave privada). A pasta está no `.gitignore`: a chave **nunca** vai para o git.
3. **Configuração:** `cp nginx/nginx.https.conf.example nginx/nginx.conf` e troque `server_name _;` pelo nome do host. Ela escuta em 8443 (publicada como 443), redireciona o HTTP (80) para HTTPS e manda o cabeçalho HSTS.
4. **Endereços no `.env`:** todos com `https://` e `wss://` (passo 3).
5. `docker compose up -d --build` e teste `https://fab-legis.intraer/login`.

Para **experimentar** sem certificado de verdade, gere um autoassinado (o navegador vai reclamar; serve para testar o fluxo):

```bash
docker run --rm -v "$(pwd)/segredos/tls:/certs" alpine/openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
  -keyout /certs/privkey.pem -out /certs/fullchain.pem -subj "/CN=fab-legis.intraer"
```

**Opção B — o TLS termina antes** (um balanceador ou proxy da FAB já faz o HTTPS): use `nginx/nginx.conf.example` (só HTTP) e deixe o balanceador repassar para a porta 80 mandando `X-Forwarded-Proto: https` e **sem reescrever o `Host`**. Os endereços do `.env` continuam `https://`.

**Imagens do MinIO (e por que ele precisa de HTTPS também).** O navegador baixa as imagens **direto do MinIO**, por URLs assinadas geradas pelo backend (`MINIO_PUBLIC_URL`). Numa página HTTPS, essa URL **também precisa ser HTTPS**; senão o navegador bloqueia a imagem (conteúdo misto). Existem dois caminhos:

| | (a) TLS no Nginx do sistema, com nome próprio para o MinIO | (b) TLS no próprio MinIO |
|---|---|---|
| Como | um segundo `server` no Nginx, com o nome `s3.fab-legis.intraer`, repassa para `minio:9000` | o certificado vai em `/root/.minio/certs` dentro do MinIO |
| Certificados | **um lugar só** (o `segredos/tls/`), que cobre os dois nomes | um segundo conjunto para o MinIO, com a sua própria renovação |
| Porta 9000 | **não precisa ser publicada** (só o Nginx fala com o MinIO, na rede interna) | precisa ser publicada para o mundo |
| Renovação do certificado | uma só, e recarrega o Nginx | outra, e reinicia o MinIO |
| Complexidade | baixa: já vem pronto no `nginx.https.conf.example` | média: mais um serviço com TLS |

**Recomendação: (a).** O mesmo Nginx que já termina o TLS do sistema termina o do MinIO: um único certificado, uma única renovação, e o MinIO continua em HTTP puro, escondido na rede do Docker (a porta 9000 fica só em `127.0.0.1`). O `nginx/nginx.https.conf.example` já traz o bloco; falta só:

1. **DNS:** peça à rede um segundo nome (ex.: `s3.fab-legis.intraer`) apontando para o mesmo servidor.
2. **Certificado:** ele precisa **cobrir os dois nomes** — um certificado com os dois no campo SAN, ou um curinga (`*.fab-legis.intraer`).
3. **Nginx:** em `nginx/nginx.conf`, troque o `server_name s3.exemplo.intraer;` do segundo bloco pelo nome real.
4. **`.env`:** `MINIO_PUBLIC_URL=https://s3.fab-legis.intraer`.

O bloco **preserva o `Host`** (`proxy_set_header Host $http_host`): a assinatura das URLs inclui o `Host`, e se ele chegasse trocado ao MinIO a imagem daria `403 SignatureDoesNotMatch`. Testei o roteamento (o MinIO responde através do Nginx, em HTTPS, por esse nome); a imagem real de ponta a ponta, com certificado de verdade, fica a validar no ambiente.

### 2. Mesma origem (proxy reverso) — o `nginx/nginx.conf` de cada ambiente

Em desenvolvimento, o proxy do Vite (`server.proxy` em `frontend/vite.config.js`) faz `/oauth2/**`, `/login`, `/logout` e `/.well-known/**` parecerem, para o navegador, a mesma origem do frontend — é isso que faz o cookie de sessão sobreviver ao login. O frontend de produção tem o **equivalente no próprio Nginx, versionado no repositório**: não há arquivo para criar nem editar no deploy. O que muda de um ambiente para outro é só o `.env`. Assim como o `.env`, o Nginx tem um **arquivo real por ambiente, fora do git** (`nginx/nginx.conf`), criado a partir de um modelo versionado (`nginx/nginx.conf.example`) — o mesmo padrão de `.env.example` → `.env`. Já vem com o proxy pronto: nada a escrever, só copiar e, se o ambiente exigir (hosts diferentes, TLS), ajustar.

| Arquivo | Perfil |
|---|---|
| `nginx/comum.conf` | O que é comum a qualquer ambiente (versionado): serve a SPA e faz o proxy reverso para o backend (`backend:8081`) e o `collab` (`collab:1234`), na rede do Compose |
| `nginx/nginx.conf.example` / `nginx/nginx.https.conf.example` | **Modelos** da configuração do ambiente: só HTTP (TLS terminado fora) ou com HTTPS direto no Nginx. Ambos incluem o `comum.conf` |
| `nginx/nginx.conf` | A configuração **real** do ambiente — copiada de um dos modelos; está no `.gitignore`. O Compose a monta em `/etc/nginx/conf.d/default.conf` (precisa existir antes do `up`) |
| `frontend/Dockerfile` (estágio `production`) | Compila a SPA para a imagem do Nginx; a configuração vem do `nginx/nginx.conf` montado |

O que o Nginx roteia (tudo atrás de **um único host público**):

| Caminho | Destino |
|---|---|
| `/oauth2/**`, `/logout`, `/.well-known/**` | backend (`backend:8081`) |
| `POST /login` | backend |
| `GET /login` | a SPA (a tela de login do Vue Router) |
| `/v1/**` | backend (a API e o JSON do OpenAPI) |
| `/swagger-ui.html`, `/swagger-ui/**` | backend (Swagger UI) |
| `/collab/**` | `collab` (`collab:1234`), com *upgrade* de WebSocket |
| `/health` | resposta do próprio Nginx (`ok`) |
| o resto | arquivos estáticos da SPA (`index.html` para rotas do Vue) |

O contrato da API (Swagger UI em `/swagger-ui.html` e o JSON em `/v1/fab-legis-api`) também sai por esse Nginx, para quem for consumir a API: ele só **descreve** os endpoints — todos continuam exigindo o access token (o botão *Authorize* da Swagger UI aceita o Bearer). `APP_SWAGGER_ENABLED=false` no `.env` desliga a UI e o JSON, se um ambiente preferir não publicar o contrato. O Actuator **não** é exposto pelo Nginx (só o healthcheck interno). O `Host` não é reescrito: o Spring Security o usa para montar URLs de redirect, e sobrescrevê-lo vazaria o hostname interno ao navegador. Só edite os endereços `backend:8081`/`collab:1234` no `nginx/nginx.conf` se o backend ou o `collab` não estiverem na rede do Compose.

Para **exercitar localmente o mesmo caminho de produção**, suba o perfil `production` (Nginx na porta 80) com o `.env` apontando para `http://127.0.0.1` em vez de `:8081`/`:5173` (bloco comentado no `.env.example`).

### 3. Variáveis de ambiente — apontar tudo pro domínio público, com HTTPS

> **Lugar único: o `.env` da raiz do repositório**
>
> **`.env`** (raiz do repositório, fora do git — `cp .env.example .env`) é o único arquivo que a equipe DevSecOps precisa editar para trocar `127.0.0.1`/`localhost` pelo hostname real de produção em **todos** os serviços de uma vez — backend, `collab` e frontend (dev e produção). `docker-compose.yml` interpola cada `${VAR:-padrão}` a partir dele; nenhuma variável de host fica hardcoded em outro lugar. As únicas duas exceções, e nenhuma delas é hostname: `PROXY_BACKEND_TARGET` (só existe pro proxy do Vite em dev, sem efeito em produção — ver nota no fim desta seção) e o `nginx/nginx.conf` do passo 2 (os endereços internos `backend:8081`/`collab:1234`, que não são hostname público).
>
Todas já são parametrizáveis (ver tabelas acima), só precisam do valor certo. Supondo `https://fab-legis.exemplo.mil.br` como o único host público (backend e frontend atrás do mesmo proxy do passo 2):

| Variável | Valor em produção |
|---|---|
| `COMPOSE_PROFILES` | `production` |
| `APP_OAUTH2_CHAVE_OBRIGATORIA` | `true` — o backend só sobe com `segredos/jwt/jwt-rsa.pem` (passo 4) |
| `MINIO_PUBLIC_URL` | `https://s3.fab-legis.exemplo.mil.br` — o nome próprio do MinIO com TLS (passo 1) |
| `APP_CORS_ALLOWED_ORIGINS` | *(vazio)* — com tudo na mesma origem pelo Nginx, nenhum site precisa de CORS |
| `APP_OAUTH2_ISSUER` | `https://fab-legis.exemplo.mil.br` |
| `APP_OAUTH2_REDIRECT_URI` | `https://fab-legis.exemplo.mil.br/callback` |
| `APP_FRONTEND_LOGIN_URL` | `https://fab-legis.exemplo.mil.br/login` |
| `OAUTH2_ISSUER` (collab) | `https://fab-legis.exemplo.mil.br` — precisa bater com `APP_OAUTH2_ISSUER` (comparado contra o claim `iss` do token) |
| `OAUTH2_JWKS_URL` (collab) | pode continuar interno (`http://backend:8081/oauth2/jwks`) — é só de onde o `collab` busca as chaves, nunca é exposto ao navegador |
| `VITE_API_BASE_URL` | `https://fab-legis.exemplo.mil.br/v1` |
| `VITE_DOCS_URL` | onde a documentação estiver publicada (ex.: `https://docs.fab-legis.exemplo.mil.br`) ou vazio para não mostrar o link |
| `VITE_COLLAB_URL` | `wss://fab-legis.exemplo.mil.br/collab` (ou outro caminho/porta, dependendo de como o `collab` for exposto — ele também precisa estar atrás de TLS, já que a página é servida por HTTPS e não pode abrir um WebSocket `ws://` sem criptografia a partir dela) |

> **`VITE_*` é build-time, não runtime — sempre reconstrua depois de mudar o `.env`**
>
> Diferente das variáveis do backend (lidas a cada boot), as `VITE_*` são embutidas no JavaScript estático durante `npm run build`. `docker-compose.yml` já passa `VITE_API_BASE_URL`/`VITE_COLLAB_URL`/`VITE_USE_MOCK_API` do `.env` como *build args* pro `frontend-prod` (`build.args`, lido por `frontend/Dockerfile`) — então editar o `.env` já é o suficiente em termos de **onde** configurar, mas só surte efeito depois de reconstruir a imagem: mudar a variável de um container **já construído**, sem rebuild, não tem efeito nenhum.
> ```bash
> docker compose --profile production up -d --build frontend-prod
> ```
>
`PROXY_BACKEND_TARGET` não se aplica em produção — é exclusiva do dev server do Vite (ver comentário em `.env.example`); o Nginx de produção usa os endereços do `nginx/nginx.conf`.

### 4. Chave de assinatura dos tokens (RSA)

Ao entrar, o usuário recebe um *access token* (JWT) **assinado** pelo backend com uma chave RSA; o backend e o `collab` conferem a assinatura com a chave pública (`/oauth2/jwks`). Não é um certificado e não tem a ver com HTTPS — é só o par de chaves das assinaturas.

**Como funciona aqui:**

| Situação | O que acontece |
|---|---|
| Existe o arquivo `segredos/jwt/jwt-rsa.pem` no servidor | é a chave (**persistente**): reiniciar o backend, atualizar a imagem ou recriar o container **não derruba as sessões** |
| O arquivo **não** existe (padrão em desenvolvimento) | o backend usa uma chave **efêmera**, criada na memória a cada subida; cada restart invalida os tokens e todos precisam entrar de novo (o token dura 30 minutos, de qualquer jeito) |
| O arquivo não existe **e** `APP_OAUTH2_CHAVE_OBRIGATORIA=true` | o backend **se recusa a subir**, com a mensagem de como gerar a chave — é o que a produção deve usar, para nunca cair em silêncio na efêmera |

O sistema **nunca grava chave em disco por conta própria**: a chave persistente é um arquivo que o ambiente fornece. Ela também **não vai dentro da imagem do Docker**: quem tivesse a imagem teria a chave, e todas as instalações dividiriam a mesma. A pasta `segredos/jwt/` está no `.gitignore` e é montada no container só para leitura (`./segredos/jwt:/app/chaves:ro`).

**Passo a passo — chave gerada por você (uma vez, em produção):**

1. Gere a chave (RSA de 2048 bits, já no formato PKCS#8 que o sistema lê):
   ```bash
   # Linux / macOS / Windows com Docker (Git Bash: prefixe com MSYS_NO_PATHCONV=1 e use $(pwd -W))
   docker run --rm -v "$(pwd)/segredos/jwt:/c" alpine/openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /c/jwt-rsa.pem
   # (ou, com openssl instalado: openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out segredos/jwt/jwt-rsa.pem)
   ```
2. Restrinja o acesso: `chmod 600 segredos/jwt/jwt-rsa.pem` (quem lê esse arquivo emite tokens válidos — trate-o como uma senha).
3. No `.env` de produção: `APP_OAUTH2_CHAVE_OBRIGATORIA=true`.
4. `docker compose up -d --build backend` e confira no log: `chave RSA persistente carregada de /app/chaves/jwt-rsa.pem (kid …)`.
5. **Guarde uma cópia do arquivo** junto com o backup do `.env` (passo 5): se for perdido, o sistema sobe com uma chave nova e todos precisam entrar de novo — não é perda de dados, só um novo login.

**Passo a passo — chave externa (fornecida pela segurança da informação da FAB):** a mesma coisa, a partir do passo 2, com o arquivo da organização salvo como `segredos/jwt/jwt-rsa.pem`. Requisitos: **RSA de no mínimo 2048 bits**, em **PEM PKCS#8** (começa com `-----BEGIN PRIVATE KEY-----`). Se a chave vier como `-----BEGIN RSA PRIVATE KEY-----` (PKCS#1), converta: `openssl pkcs8 -topk8 -nocrypt -in chave-original.pem -out segredos/jwt/jwt-rsa.pem`. Só a chave **privada** é necessária (a pública é derivada dela). Para usar outra pasta, defina `CHAVES_JWT=/caminho/da/pasta` no `.env`.

**Conferir:** `curl http://127.0.0.1:8081/oauth2/jwks` mostra o `kid` (a impressão digital da chave). Reinicie o backend e consulte de novo: com chave persistente o `kid` **não muda**; com a efêmera, muda.

**Várias réplicas do backend** (Kubernetes): todas **precisam usar a mesma chave**, senão um token emitido por uma réplica seria recusado por outra. Monte o mesmo arquivo em todas (um *Secret* do Kubernetes) — ver [Réplicas e escalabilidade](docs/replicas.md).

**Trocar a chave (rotação):** substitua o arquivo e reinicie o backend; todos os tokens antigos deixam de valer e as pessoas entram de novo. Faça num horário tranquilo.

### 5. Backup — não existe rotina pronta

Os dados de verdade estão em dois lugares: o **PostgreSQL** (documentos, usuários, tudo) e o **MinIO** (imagens, PDFs armazenados, portarias). Perder qualquer um é perder dados; **nada disto é feito automaticamente** — configure antes de entrar em produção.

```bash
# PostgreSQL: um dump completo (rode do servidor; agende no cron, ex.: toda noite)
docker exec fab-legis-postgres pg_dump -U postgres -Fc fab-legis-dev > backup-$(date +%F).dump
# restaurar num banco vazio:
docker exec -i fab-legis-postgres pg_restore -U postgres -d fab-legis-dev --clean --if-exists < backup-2026-10-12.dump

# MinIO: copiar o bucket para outro lugar (cliente mc, ou rclone/rsync sobre o volume)
docker run --rm --network fab-legis_default -v "$(pwd)/backup-minio:/dados" minio/mc sh -c \
  "mc alias set origem http://minio:9000 USUARIO_DO_MINIO SENHA_DO_MINIO && mc mirror origem/fab-legis-imagens /dados"   # valores do .env (MINIO_ROOT_USER/PASSWORD)
```

Guarde os arquivos **fora do servidor** (outra máquina ou storage da FAB), mantenha várias gerações (diário por uma semana, semanal por um mês) e, principalmente, **teste a restauração** de tempos em tempos: backup que nunca foi restaurado não é garantia. Os volumes do Docker (`postgres_18_data`, `minio_data`) também podem ser copiados com o container parado, mas o `pg_dump` é o caminho mais seguro para o banco.

### 6. Observabilidade — Actuator/Micrometer, sem coletor incluso

O backend expõe **Spring Boot Actuator** com **Micrometer** (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`), mas só quatro endpoints ficam acessíveis via HTTP (`management.endpoints.web.exposure.include=health,info,metrics,prometheus` em `application.properties`) — nunca `*`: `/actuator/env`, `/actuator/beans`, `/actuator/heapdump` etc. vazam detalhe interno demais pra ficar expostos, mesmo atrás de autenticação, e não são necessários pra observabilidade externa.

| Endpoint | Acesso | Uso |
|---|---|---|
| `GET /actuator/health` | **Sem autenticação** (ver `SecurityConfig`) | Usado pelo healthcheck do `docker-compose.yml` — o orquestrador não tem como enviar um token. `management.endpoint.health.show-details=never` garante que a resposta seja só `{"status":"UP"}`, nunca o detalhe de cada dependência (banco, disco etc.), mesmo sem login |
| `GET /actuator/metrics` | Exige autenticação (JWT, igual a qualquer endpoint de `/v1/**`) | Métricas em JSON, navegáveis uma a uma |
| `GET /actuator/prometheus` | Exige autenticação | Mesmas métricas, formato texto Prometheus — pronto pra qualquer coletor Prometheus-compatible fazer *scrape* |
| `GET /actuator/info` | Exige autenticação | Metadados da build (vazio hoje, sem `spring-boot-maven-plugin` build-info configurado) |

> **Nenhum coletor/visualizador faz parte deste repositório de propósito**
>
> Prometheus, Grafana, Zabbix, Datadog ou o que a organização já usa pra monitorar outros sistemas — a escolha e a hospedagem desse serviço são decisão de infraestrutura de quem hospeda o FAB Legis, o mesmo raciocínio de "lugar único pra configurar hostname" já explicado acima. Este projeto só garante o lado de **expor** dados de forma padrão (`/actuator/prometheus`); **coletar/visualizar** é responsabilidade externa.
>
> `/actuator/metrics` e `/actuator/prometheus` hoje exigem o mesmo login OAuth2 de qualquer usuário do sistema — não há uma credencial de serviço dedicada. Um coletor automatizado (ex.: um *scraper* do Prometheus) **não consegue fazer login interativo**, então, ao conectar um coletor de verdade, quem administra a infraestrutura precisa escolher entre: (a) isolar esses endpoints numa rede interna que o coletor já alcança sem passar pelo proxy público (mais comum — ex.: `management.server.port` numa porta separada, não exposta no `docker-compose.yml`), ou (b) implementar uma credencial de serviço dedicada. Nenhuma das duas está implementada — decisão para quando houver um coletor real a conectar.
>
### Checklist antes de expor ao público

1. HTTPS ativo no domínio de produção (certificado interno serve; `nginx/nginx.https.conf.example` + `segredos/tls/`), inclusive para as imagens do MinIO.
2. `.env` com `COMPOSE_PROFILES=production` e Nginx funcionando (`nginx/nginx.conf`, copiado de um dos modelos; confira que `GET /login` abre a SPA, `POST /login` chega ao backend e `/collab` aceita WebSocket) e, se houver outro proxy/balanceador na frente (TLS), sem reescrever o `Host`.
3. Todas as variáveis da tabela acima (inclusive `APP_CORS_ALLOWED_ORIGINS` vazio) apontando para o domínio público com `https://`/`wss://`.
4. Frontend reconstruído (não só reiniciado) depois de qualquer mudança em `VITE_*`.
5. Testar o fluxo de login completo (não só que a tela carrega) — é o único jeito de pegar um `Host` sobrescrito ou uma rota de proxy faltando antes que um usuário real esbarre nisso.
6. Se for conectar um coletor de métricas externo, decidir como ele vai autenticar contra `/actuator/prometheus` (ver passo 5) — não deixar a decisão pra depois do deploy.

---

## Variáveis de ambiente

Via Docker Compose, todas as variáveis abaixo (exceto `PORT` do collab, que não tem override no compose) podem ser ajustadas pelo `.env` na raiz do repositório (`cp .env.example .env`), sem editar `docker-compose.yml` — ver o Caminho A acima.

**Backend**

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `fab-legis-dev` | Conexão PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `postgres` / `123456` | Credenciais do banco |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Endpoint S3 do MinIO |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | `minioadmin` / `minioadmin123` | Credenciais do MinIO |
| `MINIO_BUCKET` | `fab-legis-imagens` | Bucket das imagens |
| `MINIO_PUBLIC_URL` | `http://localhost:9000` | URL pública para servir as imagens (em produção, `https://`) |
| `APP_PDF_MAX_CONCORRENTES` / `APP_PDF_ESPERA_MAXIMA_SEGUNDOS` | `2` / `30` | Renderizações de PDF simultâneas por instância e quanto um pedido espera por uma vaga antes do `503` — ver [Limite de gerações simultâneas de PDF](docs/exportacao-pdf.md#limite-de-geracoes-simultaneas-de-pdf) |
| `APP_PDF_CACHE_TTL_DIAS` | `2` | Dias até o cache de PDF/HTML em tramitação expirar no MinIO — ver [Cache de PDF/HTML em tramitação](docs/exportacao-pdf.md#cache-de-pdfhtml-em-tramitacao) |
| `COMPOSE_PROFILES` | `dev` | Quais serviços o `docker compose up` sobe: `dev` (frontend Vite) ou `production` (frontend compilado atrás do Nginx). Lida pelo próprio Compose |
| `MINIO_BIND` / `DOCS_BIND` | `127.0.0.1` | Em que interface o Docker publica as portas do MinIO (9000) e da documentação (8000). `127.0.0.1` = só a própria máquina. Com o MinIO atrás do Nginx (passo 1, recomendado), `MINIO_BIND` fica assim também em produção; `0.0.0.0` só se o navegador for chegar ao MinIO direto |
| `APP_OAUTH2_CHAVE_PRIVADA` | `/app/chaves/jwt-rsa.pem` | Caminho, dentro do container, da chave RSA de assinatura dos tokens; se o arquivo existe é a chave persistente, senão a chave é efêmera (passo 4) |
| `APP_OAUTH2_CHAVE_OBRIGATORIA` | `false` | `true` faz o backend recusar a subida sem o arquivo da chave (produção) |
| `CHAVES_JWT` | `./segredos/jwt` | Pasta do servidor com o `jwt-rsa.pem`, montada somente para leitura no backend |
| `APP_OAUTH2_ISSUER` | `http://127.0.0.1:8081` | Endereço que o navegador usa para o Authorization Server (vira o claim `iss` do token — collab e frontend precisam concordar com esse valor). Usa `127.0.0.1`, não `localhost`, de propósito — ver aviso abaixo |
| `APP_OAUTH2_REDIRECT_URI` | `http://127.0.0.1:5173/callback` | Rota de callback da SPA (troca o `code` por token) |
| `APP_FRONTEND_LOGIN_URL` | `http://127.0.0.1:5173/login` | Tela de login (Vue) para onde o Authorization Server manda usuários não-autenticados |
| `APP_CORS_ALLOWED_ORIGINS` | `http://127.0.0.1:5173,http://localhost:5173` | Origens do navegador autorizadas a chamar a API (CORS), separadas por vírgula; aceita padrões como `https://*.exemplo.mil.br`. **Em produção, com tudo atrás do Nginx na mesma origem, deixe vazio** (`APP_CORS_ALLOWED_ORIGINS=` no `.env`): não há chamada cross-origin. `*` libera qualquer site — evite |
| `APP_SWAGGER_ENABLED` | `true` | Publica a Swagger UI e o JSON do OpenAPI; `false` desliga os dois |
| `APP_ADMIN_CPF` / `APP_ADMIN_SENHA` / `APP_ADMIN_NOME` / `APP_ADMIN_NOME_GUERRA` / `APP_ADMIN_EMAIL` / `APP_ADMIN_POSTO` | ver acima | Usuário administrador padrão, criado só no primeiro boot |

**Collab** (serviço de colaboração em tempo real, ver [Arquitetura](docs/arquitetura.md))

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
| `VITE_DOCS_URL` | `http://localhost:8000` | Endereço da documentação técnica, para o link da tela de login (build-time); vazio esconde o link |
| `VITE_USE_MOCK_API` | `false` | Usa API mockada em vez do backend real |
| `VITE_APP_ENV` | `development` | Ambiente (`development` \| `staging` \| `production`) — só afeta o build local (sourcemap em `staging`, ver `vite.config.js`); o build via Docker sempre roda como `development` |
| `VITE_APP_VERSION` | `1.0.0` | Versão exibida em logs/telas de diagnóstico |

> **Atenção**
>
> As credenciais acima são valores de desenvolvimento. Em produção, substitua todas por *secrets* gerenciados fora do repositório.
>
> **Acesse sempre via `127.0.0.1`, nunca `localhost`**
>
> `APP_OAUTH2_ISSUER`/`APP_OAUTH2_REDIRECT_URI`/`APP_FRONTEND_LOGIN_URL`/`VITE_API_BASE_URL`/`VITE_COLLAB_URL` usam `127.0.0.1` de propósito, não `localhost` — para o navegador são origens diferentes (cookie de sessão e `sessionStorage` não atravessam), e o `redirect_uri` do client OAuth2 só está registrado para `127.0.0.1`. Abrir o frontend em `http://localhost:5173` quebra o login logo no primeiro redirect para `/oauth2/authorize`. Se mudar um desses valores, mude todos juntos, mantendo o mesmo host.
>
---

## Testes

Backend (JUnit 5 + Mockito; os de integração usam PostgreSQL num container) e frontend (Vitest). Não é preciso JDK nem Maven instalados — o backend roda num container descartável (a primeira execução baixa as dependências para o volume `fab-legis-m2-cache`):

```bash
# Backend — testes unitários (Linux / macOS / CI)
docker run --rm -v "$(pwd)/backend:/app" -v fab-legis-m2-cache:/root/.m2 -w /app \
  eclipse-temurin:25-jdk-alpine sh -c "apk add --no-cache maven >/dev/null && mvn -B test"

# Backend — Windows (Git Bash)
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)/backend:/app" -v fab-legis-m2-cache:/root/.m2 -w /app \
  eclipse-temurin:25-jdk-alpine sh -c "apk add --no-cache maven >/dev/null && mvn -B test"

# Frontend — testes e lint
docker compose run --rm --build --no-deps frontend npm test
docker compose run --rm --no-deps frontend npm run lint
```

Os testes de integração (PostgreSQL de verdade via Testcontainers, precisam de Docker), a análise estática e o CI estão em **[Testes e qualidade](docs/testes.md)**.

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
