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
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![MinIO](https://img.shields.io/badge/MinIO-on--premise-C72E49)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

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

Para o detalhamento de funcionalidades, arquitetura, modelo de domínio, autenticação, ciclo de vida do documento e API, veja a **[documentação técnica](docs/index.md)** — sobe junto com `docker compose up -d` (ver abaixo), disponível em `http://localhost:8000`.

## Como executar

**Pré-requisito:** Docker e Docker Compose.

```bash
git clone https://github.com/danielchipolesch/fab-legis.git
cd fab-legis
cp .env.example .env   # opcional: sem ele, o compose usa os mesmos valores de desenvolvimento
docker compose up -d
```

| Serviço | URL |
|---|---|
| Frontend | http://127.0.0.1:5173 |
| Backend (API + Swagger em `/swagger-ui.html`) | http://127.0.0.1:8081 |
| Colaboração em tempo real (WebSocket) | ws://localhost:1234 |
| Documentação técnica | http://localhost:8000 |
| MinIO (console) | http://localhost:9001 |

> **Acesse o frontend por `127.0.0.1`, não por `localhost`.** O login OAuth2 (redirect e cookie de sessão) é configurado para `127.0.0.1`; por `localhost` o fluxo trava em `{"message":"Não autenticado."}`.

Um usuário administrador (papéis **Admin + Editor**) é criado automaticamente no primeiro boot — em desenvolvimento, CPF `111.444.777-35` e senha `Admin@123`; troque-os em qualquer ambiente que não seja local. Como o `DataSeeder` só age quando ainda não existe nenhum usuário com login, mudanças nas migrations (`backend/src/main/resources/db/migration`) exigem recriar o volume do Postgres em bancos de dev já existentes (`docker compose down -v` apaga também o volume do MinIO; para só o Postgres: `docker compose rm -sf postgres && docker volume rm fab-legis_postgres_data`). Detalhes de variáveis de ambiente, deploy e execução local (sem Docker) estão na **[documentação técnica → Instalação e Configuração](docs/instalacao.md)**.

## Testes

Backend (JUnit 5 + Mockito, sem Spring nem banco) e frontend (Vitest). Os comandos — inclusive o do backend, que roda num container descartável, sem exigir JDK/Maven — estão em **[Instalação e Configuração → Testes](docs/instalacao.md#testes)**. Para o frontend:

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

---

<div align="center">

**FAB Legis** — padronizando a atividade normativa do Comando da Aeronáutica.

</div>
