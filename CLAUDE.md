# FAB Legis

## Documentação técnica

Este projeto mantém a documentação técnica em `docs/` (MkDocs Material, servido via `mkdocs.yml`), separada do `README.md` (que fica enxuto — visão geral e quick start).

**Sempre que uma feature nova for implementada ou um comportamento documentado em `docs/` mudar (fluxo, endpoint, regra de negócio, papel/permissão, migração de banco relevante), atualize a página correspondente em `docs/` como parte da mesma tarefa** — não deixe para depois nem espere o usuário pedir. Páginas existentes:

| Página | Conteúdo |
|---|---|
| `docs/index.md` | Visão geral, stack |
| `docs/funcionalidades.md` | O que cada tela faz |
| `docs/arquitetura.md` | Camadas backend/frontend, estrutura de pastas |
| `docs/dominio.md` | Modelo do ato normativo, regras de numeração |
| `docs/autenticacao.md` | Login, papéis, colaboração, notificações, auditoria |
| `docs/ciclo-de-vida.md` | Status do documento, portarias/BCA, ciclo de emenda |
| `docs/exportacao-pdf.md` | Geração de PDF (Apache FOP) e HTML, fontes, texto sugerido de portaria, regra de consistência entre PDF/HTML/DOCX |
| `docs/api-rest.md` | Endpoints |
| `docs/instalacao.md` | Como rodar local/Docker, variáveis de ambiente |
| `docs/roadmap.md` | Perspectivas futuras — mova um item para cá quando virar decisão adiada, ou remova-o quando for implementado |

Se a mudança não se encaixa em nenhuma página existente, prefira estender a mais próxima a criar uma nova; só crie página nova para um assunto genuinamente novo, e nesse caso adicione a entrada em `mkdocs.yml` (`nav:`).

## Consistência entre formatos de exportação (PDF/HTML/DOCX)

O documento é exportado em três formatos (PDF já implementado, HTML já implementado, DOCX planejado — ver [`docs/exportacao-pdf.md`](docs/exportacao-pdf.md)), cada um gerado por um construtor próprio a partir do mesmo JSON TipTap — nenhum é derivado dos outros dois. **Qualquer mudança que altere elementos do documento — estrutura, formatação, numeração, regra de negócio da técnica legislativa — deve ser averiguada nos 3 formatos, não só naquele em que a mudança foi pedida**, ressalvadas as particularidades que a norma ou o próprio formato exigem (ex.: HTML não tem capa e usa alinhamento à esquerda no corpo, por exigência da própria NSCA 5-3 — não é uma inconsistência a corrigir). Isso vale tanto para mudanças pedidas explicitamente num formato quanto para bugs encontrados em um deles — sempre confira se o mesmo problema/regra se aplica aos outros antes de considerar a tarefa concluída.

## Convenção de nomenclatura (português vs. inglês)

O domínio deste sistema é a técnica legislativa brasileira (LC 95/1998, Decreto nº 12.002/2024, NSCA 5-3) — os termos de negócio **são** em português e não têm tradução que faça sentido (Documento, Artigo, Emenda, Portaria, Espécie Normativa, Assunto Básico...). A convenção adotada:

- **Infraestrutura/scaffolding genérico → inglês**: pacotes (`domain.services`, `application.controllers`), anotações e padrões de framework, utilitários sem relação com a lei em si.
- **Vocabulário de negócio → português**: entidades, serviços, DTOs, métodos e variáveis que implementam ou nomeiam um conceito da técnica legislativa. Isso vale tanto no backend (Java) quanto no frontend (Vue/JS).

Ao criar algo novo, mantenha o nome do conceito consistente com o que já existe no resto do código para o mesmo conceito — não traduza um nome de entidade/DTO/página já usado em outro lugar (ex.: se a entidade é `EspecieNormativa`, o DTO correspondente é `EspecieNormativaResponseDto`, não `DocumentationTypeResponseDto`).

**Isso é a linguagem ubíqua do projeto**: o termo usado para um conceito de domínio deve ser sempre o mesmo, em toda camada — entidade/DTO/serviço no backend, variável/componente/store no frontend, rótulo mostrado na tela e texto em `docs/` — nunca um sinônimo, abreviação ou tradução alternativa, mesmo que pareça mais natural num contexto específico. `docs/dominio.md` é o glossário de referência (Documento, Parte Preliminar, Parte Normativa, Anexos, Epígrafe, Ementa, Preâmbulo, Fecho, Assinatura, Capítulo, Seção, Subseção, Artigo, Parágrafo, Parágrafo único, Inciso, Alínea, Subalínea, Emenda, Espécie Normativa, Assunto Básico...), e espelha o vocabulário da própria LC 95/1998, do Decreto nº 12.002/2024 e da NSCA 5-3 — ao introduzir um conceito de domínio novo, confira primeiro se a norma já dá um nome a ele antes de inventar um. Vale também no sentido inverso: um nome já usado para um conceito não pode ser reaproveitado para outro conceito diferente, mesmo que pareça conveniente no momento.

---

## ⚠️ PLANO TEMPORÁRIO — migração JWT caseiro → OAuth2 (Spring Authorization Server)

> **Esta seção é uma anotação de trabalho em andamento, não uma convenção permanente do projeto.** Foi combinada em conversa (sessão de 2026-09-11) para retomar a implementação em outra máquina. **Apague esta seção inteira do `CLAUDE.md` assim que a migração estiver implementada e documentada em `docs/autenticacao.md`** — ela não deve virar documentação permanente aqui.

### Contexto e decisão já tomada

Hoje a autenticação é um JWT caseiro (HMAC, biblioteca `jjwt`) emitido/validado por [`JwtService.java`](backend/src/main/java/br/com/danielchipolesch/infrastructure/security/JwtService.java) e [`JwtAuthenticationFilter.java`](backend/src/main/java/br/com/danielchipolesch/infrastructure/security/JwtAuthenticationFilter.java), com claims já no formato Keycloak-like (`sub`, `preferred_username`, `realm_access.roles`) de propósito, para uma migração futura sem dor. Ver `docs/autenticacao.md` para o estado atual.

Decisão: em vez de ir direto para o Keycloak, adotar **Spring Authorization Server** (`spring-security-oauth2-authorization-server`) **embutido no próprio backend** (uma segunda `SecurityFilterChain`, sem novo serviço no `docker-compose.yml`). Motivo: quando um dia migrar para o Keycloak de fato, o esforço fica menor nessa rota do que ficando no JWT caseiro — nessa hora só se troca `issuer-uri`/JWKS e o client é recriado no console do Keycloak; frontend e `collab` não precisam mudar de novo, porque já vão falar OAuth2/JWKS padrão desde já.

A tela de login atual (Vue) **é reaproveitada**, não descartada — vira a `loginPage` customizada do Spring Security. A diferença técnica: hoje a API é 100% stateless; o Authorization Server precisa de uma sessão (cookie) curta só durante o handshake de login → `/oauth2/authorize` → redirect com `code`. Isso é inerente ao protocolo, não uma escolha.

### Passo a passo

1. **Dependências** (`backend/pom.xml`): adicionar `spring-security-oauth2-authorization-server` e `spring-boot-starter-oauth2-resource-server`.

2. **Par de chaves JWK** (`@Bean JWKSource<SecurityContext>`): gerar/carregar um par RSA (ou EC) — substitui o `jwt.secret` HMAC simétrico. Expor a chave pública automaticamente via `/oauth2/jwks`. Decidir: gerar em memória a cada boot (dev) vs. carregar de arquivo/variável de ambiente (produção).

3. **`RegisteredClientRepository`**: começar `InMemoryRegisteredClientRepository` com um único client `fab-legis-frontend` — público (`ClientAuthenticationMethod.NONE`), `authorization_code` + `refresh_token`, PKCE obrigatório (`requireProofKey(true)`), `redirectUri` apontando para a rota de callback da SPA.

4. **Segunda `SecurityFilterChain`** (`@Order(1)`, antes da chain de resource server) usando `OAuth2AuthorizationServerConfigurer` — expõe `/oauth2/authorize`, `/oauth2/token`, `/oauth2/jwks`, `/.well-known/openid-configuration`.

5. **`loginPage` customizada**: `.formLogin(form -> form.loginPage("/login"))` apontando para a rota de login já existente no Vue. Adaptar essa tela para autenticar dentro da sessão do Spring Security (não mais POST JSON stateless) — reaproveitar `UsuarioDetailsService` (já existe) + `BCryptPasswordEncoder` (já existe em `SecurityConfig.java`) como `AuthenticationProvider`. Nenhuma mudança em `Usuario`/`t_usuario`/hash de senha.

6. **`OAuth2TokenCustomizer<JwtEncodingContext>`**: replicar as claims que `JwtService.gerarToken()` já produz hoje — `preferred_username` (CPF), `nome`, `om_id`, `realm_access.roles` (a partir de `UsuarioPrincipal.getPapeis()`). Isso preserva `PapelEnum`/`@PreAuthorize("hasRole(...)")`/`DocumentoAcessoService` sem tocar neles.

7. **Resource server** (chain existente, `@Order(2)`): remover `JwtAuthenticationFilter` e `JwtService`; adicionar `spring-boot-starter-oauth2-resource-server` configurado com `spring.security.oauth2.resourceserver.jwt.issuer-uri` apontando para o próprio backend (issuer = si mesmo, já que o AS está embutido). Remover também `/v1/auth/login`, `/v1/auth/refresh`, `/v1/auth/logout` (`AuthController`) e a tabela/entidade `t_refresh_token`/`RefreshTokenService` — passam a ser responsabilidade nativa do Authorization Server (`OAuth2AuthorizationService`).

8. **Frontend**: reescrever o fluxo de login de "POST JSON + guardar token" para Authorization Code + PKCE (ex.: biblioteca `oidc-client-ts`, ou implementação manual do `code_verifier`/`code_challenge`). Precisa de uma rota de callback nova. Avaliar se a tela de login continua sendo uma rota Vue normal (servida pelo dev server) ou se passa a ser servida pelo backend nesse momento específico do fluxo — ambos funcionam, mas mudam onde o componente "mora".

9. **`collab/server.js`**: trocar a validação HMAC (`jwt.verify(token, JWT_SECRET, {algorithms:['HS384']})`, linha ~47) por verificação via JWKS remoto — biblioteca `jose` (`createRemoteJWKSet` + `jwtVerify`) apontando para `http://backend:8081/oauth2/jwks`. `JWT_SECRET` deixa de existir/ser compartilhado.

10. **`docker-compose.yml` / `.env`**: remover `JWT_SECRET` dos serviços `backend` e `collab` (ver [docker-compose.yml:62](docker-compose.yml:62) e [:122](docker-compose.yml:122)); adicionar variáveis novas conforme a implementação real pedir (ex.: caminho/seed da chave RSA, `redirect_uri` do client se for parametrizável).

11. **`docs/autenticacao.md`**: reescrever a seção de autenticação descrevendo o novo fluxo (Authorization Code + PKCE, JWKS, sessão do login). Depois disso, apagar esta seção do `CLAUDE.md`.

### Ordem recomendada de execução (incremental, testável a cada passo)

1. Passos 1–4 (infraestrutura do Authorization Server) rodando em paralelo à autenticação atual, sem nada dependendo dele ainda — só validar que `/oauth2/jwks` e `/.well-known/openid-configuration` respondem.
2. Passo 5–6 (login + claims) — testar via Postman/browser o fluxo `/oauth2/authorize` → login → `code` → `/oauth2/token`, comparando as claims do token emitido com as que `JwtService` produz hoje.
3. Passo 7 (resource server) — só depois do passo 2 validado, para não derrubar a autenticação atual no meio do caminho. Bom momento para rodar os dois lado a lado brevemente (feature flag ou branch) antes de remover o `JwtService`.
4. Passos 8–9 (frontend + collab) — os que mais precisam de teste manual ponta a ponta (login, colaboração em tempo real, refresh de token expirado).
5. Passo 10–11 por último.

### Coisas para não esquecer de verificar ao concluir

- Login, refresh (expiração do access token em 15 min ainda faz sentido? o Authorization Server tem seu próprio default), logout.
- Papéis (`EDIT`/`APROV`/`PUBLIC`/`ADMIN`/`AUDITOR`) continuam sendo respeitados em todos os endpoints protegidos por `@PreAuthorize`.
- Colaboração em tempo real (`collab`) continua autenticando/reautenticando corretamente, inclusive quando o access token expira em uma sessão de edição longa.
- SSE de notificações/presença (token via query param, ver `JwtAuthenticationFilter.PATH_PRESENCA_STREAM`) — precisa de equivalente na validação nova.
- `DataSeeder` (usuário administrador padrão) não depende de nada disso, deve continuar igual.
