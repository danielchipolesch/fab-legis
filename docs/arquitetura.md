# Arquitetura

## Visão geral dos serviços

```mermaid
graph TB
    subgraph Cliente
        B[Navegador]
    end

    subgraph "Docker Compose"
        F["Frontend<br/>Vue 3 + Quasar<br/>:5173 dev · :80 prod"]
        A["Backend<br/>Spring Boot<br/>:8081"]
        C["collab<br/>Node.js + Hocuspocus (Yjs)<br/>:1234"]
        P[("PostgreSQL 16<br/>:5432")]
        M[("MinIO<br/>:9000 API · :9001 console")]
    end

    B -->|HTTP| F
    F -->|"REST /v1/*"| A
    A -->|JPA| P
    A -->|"S3 SDK"| M
    B -.->|"URL assinada (curta duração)"| M
    B -.->|"WebSocket (Yjs, edição colaborativa)"| C
    C -->|"REST /v1/* (com o JWT do próprio usuário)"| A
```

O serviço **collab** (`collab/`, `Node.js` + [Hocuspocus](https://tiptap.dev/docs/hocuspocus/introduction)) implementa a edição colaborativa em tempo real (CRDT/Yjs) do editor: cada elemento aberto para edição vira uma sala Yjs própria (`documento:{id}:elemento:{elementoId}`), sincronizada por WebSocket entre todos os navegadores conectados a ela — duas pessoas editando o mesmo elemento fazem *merge* automático caractere a caractere, sem bloqueio otimista. Existe como serviço separado (não embutido no backend Java) porque o Yjs só tem implementação madura em JavaScript; ele reaproveita o **mesmo schema do editor** (via `@tiptap/core`'s `getSchema`, aplicado à mesma lista de extensões de `frontend/src/editor/extensions.js`) para converter entre o `Y.Doc` e o JSON TipTap sem duplicar/divergir a definição do documento. Autenticação e autorização reaproveitam o backend: o `collab` valida o JWT do usuário via JWKS (`GET /oauth2/jwks`, mesma chave pública RSA que o Authorization Server embutido usa — ver `AuthorizationServerConfig`/`autenticacao.md`) e confirma a permissão de edição chamando `GET /v1/documentos/{id}/pode-editar` (mesma regra de `DocumentoAcessoService.podeEditar` usada em todo o resto da API) antes de aceitar a conexão a uma sala; toda leitura/escrita subsequente no backend (`GET /v1/documentos/{id}/elementos/{elementoId}/conteudo` para carregar o conteúdo inicial, `PATCH` na mesma rota para persistir, debounced) acontece com o token do próprio usuário conectado — não existe uma credencial de serviço à parte. Mudanças estruturais da árvore (criar/mover/excluir elemento) ficam fora do Yjs — continuam passando por `PATCH /v1/documentos/{id}/secoes` (que aplica um *diff* contra o que já está persistido, nunca reescrevendo `conteudo`) e são propagadas aos demais clientes conectados via o mesmo canal SSE de presença (`event: estrutura`).

O bucket do MinIO é **privado** — o navegador nunca acessa um objeto direto pela URL "canônica" devolvida no upload. Toda leitura (imagem de figura, PDF do documento, PDF de portaria) passa antes por `POST /v1/imagens/urls-assinadas` (autenticado, igual ao resto do `/v1/**`), que troca a URL canônica por uma URL assinada (S3 pre-signed, válida por 1h) — só essa é usada como `src`/`href` no navegador. O backend, por sua vez, nunca depende de acesso público: lê os objetos direto via SDK autenticado (`ImagemService.getImageAsDataUri`/`getObjectStream`), usado por exemplo na geração do PDF oficial (Apache FOP embute a imagem como *data URI*, sem depender de rede).

## Camadas do backend — arquitetura em cebola

O backend adota uma separação clara em três camadas, com a dependência sempre apontando para dentro (`infrastructure → application → domain`):

```
br.com.danielchipolesch
│
├── application/           ← Camada de aplicação (entrada/saída)
│   ├── controllers/       ← REST: Documento, Emenda, Anexo, EspecieNormativa,
│   │                        AssuntoBasico, Imagem, Auth, Usuario,
│   │                        OrganizacaoMilitar, Auditoria, Notificacao
│   ├── dtos/              ← Contratos de request/response por agregado — records Java
│   ├── validation/        ← @CpfValido (Bean Validation customizada)
│   └── helpers/           ← Montagem de EntityModel + links HATEOAS
│
├── domain/                ← Núcleo de negócio (sem dependência de framework web)
│   ├── entities/
│   │   ├── estruturaDocumento/   ← Documento, ItemPartePreliminar,
│   │   │                            ItemAnexoParteNormativa, ItemParteFinal,
│   │   │                            Anexo, EmendaHistorico, DocumentoCompartilhamento,
│   │   │                            PortariaPublicacao + enums
│   │   ├── numeracaoDocumento/   ← EspecieNormativa, AssuntoBasico
│   │   ├── usuario/              ← Usuario, OrganizacaoMilitar, PapelEnum
│   │   ├── auditoria/             ← LogAuditoria, AcaoAuditoriaEnum
│   │   └── notificacao/           ← Notificacao, TipoNotificacaoEnum
│   ├── services/          ← Regras de negócio (DocumentoService, DocumentoStatusService,
│   │                        DocumentoAcessoService, DocumentoConcorrenciaService,
│   │                        DocumentoParteNormativaService, EmendaService,
│   │                        PortariaPublicacaoService, UsuarioService,
│   │                        LogAuditoriaService, DocumentoBuscaService,
│   │                        NotificacaoService, DocumentoPresencaService,
│   │                        ImagemService, DocumentoPdfService, MapaAlteracaoPdfService,
│   │                        FopFactoryProvider…)
│   ├── builders/          ← DocumentoBuilder (construção fluente)
│   ├── util/tiptap/       ← TipTapNode + XslFoContentRenderer (JSON TipTap → XSL-FO)
│   ├── mappers/           ← Entidade ⇄ DTO
│   └── handlers/          ← GlobalExceptionHandler + exceções tipadas por domínio
│
└── infrastructure/        ← Detalhes técnicos
    ├── repositories/      ← Spring Data JPA
    ├── security/          ← UsuarioPrincipal, UsuarioDetailsService,
    │                        JwtToUsuarioAuthenticationConverter, SseBearerTokenResolver,
    │                        AutenticacaoUtil, DataSeeder (usuário admin padrão)
    ├── notificacao/       ← NotificacaoEmitterRegistry, DocumentoPresencaEmitterRegistry (SSE)
    ├── configurations/    ← Cors, Swagger, SecurityConfig (resource server),
    │                        AuthorizationServerConfig, Minio
    ├── enums/             ← Catálogos oficiais (espécies, assuntos, cabeçalho)
    └── runners/           ← Carga inicial das tabelas de referência
```

**Tratamento de erros centralizado:** um `GlobalExceptionHandler` converte exceções de domínio (`ResourceNotFoundException`, `StatusCannotBeUpdatedException`, `ResourceAlreadyExistsException`, `InvalidInputException`, `ResourceCannotBeUpdatedException`, `CredenciaisInvalidasException`, `ConflitoEdicaoException`, `AccessDeniedException` do Spring Security) em respostas JSON padronizadas — com `Content-Type` sempre explícito, mesmo quando a requisição original era um *stream* SSE, para nunca cair na negociação de conteúdo automática do Spring.

**Seed automático:** os `runners` (`EspecieNormativaRunner`, `AssuntoBasicoRunner`) populam na inicialização as espécies normativas e os assuntos básicos oficiais do COMAER, cada um com sua descrição normativa completa — o catálogo já nasce pronto para uso.

## Regras por espécie normativa — atrás de interfaces

Nem toda espécie normativa obedece às mesmas regras. Para o restante do sistema **nunca testar a espécie** de um documento, cada `EspecieNormativa` aponta para o **tipo de regras** que segue (`EspecieNormativa.tipoDeRegras`, coluna `st_tipo_regras`; hoje só `ATO_NORMATIVO`, o de DCA, ICA, NSCA...) e a implementação desse tipo entrega as regras por meio de interfaces — pacote `domain.regras`:

| Interface | O que a espécie decide | Implementação em `ATO_NORMATIVO` |
|---|---|---|
| `RegrasDeCriacaoDoDocumento` | o que a espécie exige para criar um documento e como ele se identifica (a `identificacao` é gravada na criação) | `CriacaoDeAtoNormativo` (assunto básico + sequencial: "DCA 11-3") |
| `CamposEspecificosDaEspecie` | o ciclo de vida dos dados que só algumas espécies têm, numa estrutura 1:1 com o documento (criar e copiar junto com ele; a exclusão é em cascata no banco) | `SemCamposEspecificos` (nenhum) · NPA: `CamposDeNpa` |
| `RegrasDeHierarquiaDosElementos` | quem pode ficar dentro de quem na parte normativa; o backend recusa, no salvamento, o que a espécie não permite | `HierarquiaDeAtoNormativo` (o editor impõe a ordem; o backend não recusa) |
| `CalculadoraDeNumeracaoDosElementos` | o rótulo de cada elemento da parte normativa | `NumeracaoService` |
| `EstruturaInicialDeNovoDocumento` | os elementos com que um documento novo já nasce | `CapitulosPadronizadosService` (NSCA 5-3) |
| `RotuloDosAnexos` | como os anexos são rotulados | `RotuloDeAnexoDeAtoNormativo` (ANEXO II, III…) |
| `LeiauteDoPdf` | a diagramação do PDF (XSL-FO) | `DocumentoFoBuilder` |
| `LeiauteDoHtml` | a diagramação do HTML | `LeiauteHtmlDeAtoNormativo` |
| `RegrasDeRegistroDaPublicacao` | o que se registra (e o que é obrigatório informar) ao publicar ou revogar oficialmente | `PublicacaoDeAtoNormativo` (portaria + BCA e, na 1ª publicação, a parte preliminar) · NPA: `PublicacaoDeNpa` (Boletim Interno) |
| `RegrasDoCicloDeVidaDoDocumento` | as mudanças de etapa permitidas (`AcaoDeEtapa`) | `CicloDeVidaDeAtoNormativo` |

`RegrasDaEspecieNormativa` reúne as regras de uma espécie (`RegrasDeAtoNormativo`, para as demais espécies, é a implementação atual; `RegrasDeNpa` virá a seguir) e `RegrasDasEspecies.para(especie)` devolve as regras — toda `RegrasDaEspecieNormativa` registrada como bean entra sozinha, então acrescentar uma espécie com regras próprias **não exige mexer no registro nem nos serviços**. Quem só precisa da regra depende da interface: `DocumentoService.create`/`clone` (criação, estrutura inicial e campos específicos), `DocumentoParteNormativaService` (hierarquia e numeração), `DocumentoPdfService` e `DocumentoHtmlService` (layouts, que continuam dono da parte que não depende da espécie: escolher a versão, gerar, armazenar e servir o arquivo) e `DocumentoStatusService` (ciclo de vida e registro da publicação).

**Regra para código novo:** um comportamento que varia por espécie entra como método de uma dessas interfaces (ou de uma interface nova), nunca como `if (espécie == …)` no serviço. Ver a proposta da NPA, a próxima espécie com regras próprias, no [Roadmap](roadmap.md#npa-norma-padrao-de-acao-proposta-de-implementacao).

## Camadas do frontend

```
frontend/src
│
├── pages/          ← LoginPage · HomePage · DocumentEditorPage · DocumentViewerPage ·
│                      ComparisonPage · UsersPage · AuditoriaPage
├── components/
│   ├── editor/     ← WysiwygEditor, EditorSidebar (com dialog de metadados),
│   │                 DocumentPreview, NormTreeItem, FigureView,
│   │                 CompartilharDialog, Lc95HelpDialog
│   ├── comparison/ ← DiffViewer
│   └── common/     ← AppTopBar (menu de usuário, sino de notificações),
│                      StatusBadge, NewDocumentDialog
├── stores/         ← Pinia: auth (sessão) · documents (acervo) · editor (documento em edição)
├── api/            ← client (fetch tipado, com renovação automática de token) +
│                      módulos por recurso (documents, auth, usuarios, auditoria,
│                      notificacoes, referencias, portarias…)
├── extensions/     ← Figure (nó customizado do TipTap)
├── utils/          ← numbering (regras legislativas), cpf (validação/máscara),
│                      textoSugeridoPortaria (geração do texto sugerido da portaria)
├── services/       ← pdfService (geração e download de PDF server-side)
└── router/         ← Rotas SPA, com guarda de autenticação e de papel (admin/auditor)
```

**Estado com Pinia — três stores complementares:**

- **`auth`** — sessão. Guarda o access token e o usuário logado (persistidos em `localStorage`), expõe getters de papel (`isEditor`/`isAprovador`/`isPublicador`/`isAdmin`/`isAuditor`, todos independentes entre si — nenhum papel implica outro) e o fluxo de renovação via refresh token, chamado automaticamente pelo `client.js` num 401.
- **`documents`** — o acervo. Busca, cria, clona, salva e transiciona documentos; gera o *template* inicial de seções ao criar um novo ato; também busca as portarias e o mapa de alteração de um documento.
- **`editor`** — o documento aberto. Mantém uma cópia profunda para edição isolada, controla o elemento selecionado, o flag `isDirty` (salvamento automático), a versão esperada para o bloqueio otimista e todas as operações de árvore, disparando a renumeração após cada mutação. `reload()` sempre busca a versão real no servidor (nunca do cache local) — importante após um `409` de conflito de edição. `aplicarEventosEstrutura()` faz o mesmo tipo de mutação de árvore, mas a partir de eventos recebidos via SSE (mudança feita por outra pessoa, ver [Autenticação e Colaboração](autenticacao.md)) — sempre um *patch* incremental (criar/reparentear/renomear/excluir um nó), nunca um `reload()`, porque isso preservaria a identidade local de todo elemento já aberto por quem estiver editando ao vivo no momento.

**Camada de API desacoplada:** o `client.js` encapsula `fetch` com verbos tipados (`get`/`post`/`put`/`patch`/`del`), injeta o header `Authorization` via um *getter* plugado pelo `auth` store (evita import circular) e tenta renovar o token automaticamente uma vez antes de repassar um `401`; os módulos por recurso fazem a **tradução entre a nomenclatura do backend e a do frontend** (`SECAO` ⇄ `secao_normativa`, `PARAGRAFO_NUMERADO` ⇄ `paragrafo`, `ITEM` ⇄ `sub_alinea`), de modo que uma mudança no contrato REST não vaza para os componentes. Conexões SSE (notificações, presença — que também carrega o evento `estrutura` de mudanças em tempo real na árvore, ver [Autenticação e Colaboração](autenticacao.md)) não passam pelo `client.js` — são `EventSource` nativas, com o token na *query string* (única forma de autenticar um `EventSource`, que não permite headers customizados). O `EventSource` reconecta automaticamente em caso de erro, mas hoje **sem nenhum handler ligado à store de autenticação** — se a sessão expirar, a conexão tenta reconectar silenciosamente em vez de forçar logout.

**Build otimizado:** o Vite separa *chunks* por vendor (`vendor-vue`, `vendor-quasar`, `vendor-tiptap`, `vendor-utils`, `vendor-dnd`) para maximizar o cache do navegador; o `pdfmake` é importado dinamicamente e fica fora do bundle inicial.

## Estrutura de pastas

```
fab-legis/
├── docker-compose.yml          # Orquestração: postgres, minio, backend, collab, frontend, docs
├── mkdocs.yml                  # Configuração da documentação técnica (este site)
├── docs/                       # Fonte da documentação técnica (MkDocs Material)
│   ├── Dockerfile              # Multi-stage: mkdocs build → Nginx Alpine
│   └── nginx.conf
├── collab/                     # Servidor de colaboração em tempo real (Node.js + Hocuspocus/Yjs)
│   ├── Dockerfile              # node:20-alpine, npm ci --omit=dev
│   ├── server.js               # Hooks onAuthenticate/onLoadDocument/onStoreDocument
│   └── schema.js                # Schema do editor (getSchema), espelha frontend/src/editor/extensions.js
├── backend/
│   ├── Dockerfile              # Multi-stage: Maven build → JRE Alpine (+ Carlito)
│   ├── pom.xml
│   └── src/main/
│       ├── java/br/com/danielchipolesch/
│       │   ├── application/    # Controllers, DTOs, helpers
│       │   ├── domain/         # Entidades, serviços, builders, mappers, exceções
│       │   └── infrastructure/ # Repositórios, configurações, enums, runners
│       └── resources/
│           ├── application*.properties
│           ├── fop-config.xml  # Registro de fontes (Carlito como "Calibri") para o Apache FOP
│           └── db/migration/   # Scripts SQL versionados (Flyway)
└── frontend/
    ├── Dockerfile              # Multi-stage: base → development | build → Nginx
    ├── nginx.conf
    ├── vite.config.js
    ├── package.json
    ├── public/                 # Brasões, favicon
    └── src/                    # pages, components, stores, api, utils, extensions
```

**Migrações de banco:** versionadas em `resources/db/migration` (Flyway), estritamente aditivas — nunca se edita uma migração já aplicada. Atualmente em **V17**, com histórico rastreável de toda mudança de esquema: da remoção de `FUNDAMENTACAO` (V1) ao rastreio de ciclo de emenda por publicação (V7/V8), passando pela introdução de usuários/OM/papéis (V9), refresh token (V10), auditoria (V11), notificações (V12) e o registro histórico de portarias por documento (V17).
