# Réplicas e escalabilidade

Este documento responde a uma pergunta de implantação: **quais serviços do sistema podem rodar em várias instâncias (réplicas) — por exemplo num cluster Kubernetes — e quais não podem**, e por quê. A regra de ouro: um serviço só pode ter réplicas se **nenhuma informação de que ele depende vive só na memória de uma instância**. Hoje isso vale para alguns serviços e **não** vale para outros.

> [!NOTE]
> **Antes de tudo: réplica não é desempenho**
>
> Para a escala esperada (dezenas a poucas centenas de usuários), **uma instância de cada serviço** dá conta, e é a configuração mais simples e segura. Réplicas servem principalmente à **disponibilidade** (um pod cai, outro assume; atualização sem parar o serviço). Só vale a pena enfrentar a complexidade abaixo se a FAB exigir isso.

## Resumo

| Serviço | Réplicas? | Por quê |
|---|---|---|
| **Frontend (Nginx)** | **Sim, livremente** | só servem arquivos estáticos e repassam chamadas; não guardam estado |
| **Backend** | **Hoje, não** — só depois de resolver 3 pontos (abaixo) | guarda em memória o login em andamento, os códigos de autorização e as conexões de tempo real; a numeração tem uma corrida; a chave de assinatura precisa ser compartilhada |
| **Collab** (edição colaborativa) | **Não** (1 réplica) | cada documento aberto vive na memória de **uma** instância; duas instâncias dariam versões diferentes do mesmo texto |
| **PostgreSQL** | **Não como "réplicas do container"** | é um banco com um primário que aceita escrita; alta disponibilidade exige um operador/serviço próprio (ver abaixo) |
| **MinIO** | **Não como "réplicas do container"** | é um armazenamento com discos; o modo distribuído é outro tipo de implantação |

## Serviço sem estado: Frontend (Nginx)

Entrega arquivos prontos (a SPA compilada) e repassam as chamadas ao backend. Qualquer instância responde igual a qualquer outra: **escale à vontade** (`replicas: 2` ou mais num Deployment do Kubernetes, atrás do Service/Ingress).

## Backend (Spring Boot) — o que impede as réplicas hoje

O backend é *quase* sem estado: a API em si é protegida por token (`STATELESS`), o conteúdo vai para o PostgreSQL e os arquivos para o MinIO, e a edição concorrente é protegida por travas otimistas (`@Version`), que funcionam entre instâncias. Mas há **cinco** coisas que não:

### 1. Login em andamento e códigos de autorização (bloqueia réplicas)

O servidor de autorização embutido (Spring Authorization Server) guarda **na memória da instância**: a sessão HTTP do formulário de login (`JSESSIONID`) e os **códigos de autorização** emitidos até a SPA trocá-los por token (`InMemoryOAuth2AuthorizationService`). O fluxo de login passa por várias chamadas (`/oauth2/authorize` → `POST /login` → `/oauth2/authorize` → `/oauth2/token`); se cada uma cair numa réplica diferente, a seguinte **não encontra** o que a anterior guardou, e o login falha ("sessão perdida").

*Como resolver:* (a) **afinidade de sessão** (*sticky session*) no Ingress, por cookie — rápido, mas frágil se o pod cair no meio do login; ou, melhor, (b) guardar essas informações no PostgreSQL: `JdbcOAuth2AuthorizationService` (tabelas do próprio Spring Authorization Server) e **Spring Session JDBC** para a sessão do login. Depois disso, qualquer réplica atende qualquer passo.

### 2. Chave de assinatura dos tokens (resolvido, mas obrigatório)

Todas as réplicas **precisam usar a mesma chave RSA**; senão um token emitido por uma réplica seria recusado por outra. O sistema já suporta isso: a chave é um arquivo (`segredos/jwt/jwt-rsa.pem`). No Kubernetes, monte o **mesmo** *Secret* em todas as réplicas e ligue `APP_OAUTH2_CHAVE_OBRIGATORIA=true`. Sem essa chave persistente, cada réplica geraria a sua chave efêmera. Ver [README, passo 4 da instalação em produção](https://github.com/danielchipolesch/fab-legis#4-chave-de-assinatura-dos-tokens-rsa).

### 3. Conexões em tempo real (SSE): presença, notificações e estrutura (bloqueia o tempo real entre réplicas)

As conexões SSE (`SseEmitter`) com o navegador — o aviso "fulano também está editando", as notificações e a atualização da estrutura do documento — ficam num registro **em memória** (`DocumentoPresencaEmitterRegistry`, `NotificacaoEmitterRegistry`). Um evento gerado numa réplica só chega às pessoas **conectadas àquela réplica**. Com várias réplicas, quem está noutra deixa de ver a presença e as mudanças de estrutura, e perde notificações.

*Como resolver:* um **barramento** entre as réplicas (Redis *pub/sub* ou o `LISTEN/NOTIFY` do próprio PostgreSQL): cada réplica publica o evento e todas o repassam às conexões que têm. Ou, como paliativo, afinidade de sessão por usuário (não resolve se duas pessoas no mesmo documento caírem em réplicas diferentes).

### 4. Numeração dos documentos (corrida — já é um risco hoje)

O número sequencial de um documento novo é escolhido **na aplicação** ("o menor número livre", `CriacaoDeEspecieConvencional.proximoSequencial`): lê os números existentes e escolhe o próximo. Duas criações **simultâneas** (em réplicas diferentes, ou mesmo na mesma instância) podem escolher o **mesmo número**, e não há restrição de unicidade no banco que impeça. Com uma só instância e poucos usuários é raro, mas é uma falha real.

*Como resolver:* uma restrição **única** no banco para (espécie, assunto, número) e, ao violá-la, tentar de novo com o número seguinte; ou obter o número de uma sequência/trava do próprio PostgreSQL. **Deve ser feito antes de qualquer réplica** (e é recomendável mesmo sem elas).

### 5. Limite de geração de PDF (funciona, mas é "por réplica")

O limite de renderizações simultâneas (`APP_PDF_MAX_CONCORRENTES`, ver [Exportação](exportacao-pdf.md#limite-de-gerações-simultâneas-de-pdf)) é um semáforo **dentro de cada instância**. Com *N* réplicas, o total simultâneo no cluster é *N × o limite*: ajuste o valor pensando na memória de **cada pod**, não do cluster. Não impede réplicas.

### O que já funciona entre réplicas

- **Conflito de edição** (`@Version`/trava otimista): vale no banco, entre qualquer número de instâncias.
- **Cache de PDF/HTML em tramitação:** fica no MinIO, compartilhado.
- **Migrations (Flyway):** o Flyway trava o banco ao migrar; se várias réplicas sobem juntas, uma migra e as outras esperam. O ideal é rodar a migração **uma vez** (um *Job* ou *init container*) antes de escalar.
- **Criação do administrador padrão** (`DataSeeder`): só age se não existe nenhum usuário; subindo várias réplicas ao mesmo tempo no primeiro boot há uma pequena chance de corrida — suba **uma** réplica primeiro.

## Collab (edição colaborativa em tempo real)

O servidor de colaboração (Hocuspocus/Yjs) mantém **cada elemento aberto na memória** de uma instância e distribui as alterações entre as pessoas conectadas **àquela instância**. Com duas réplicas, duas pessoas editando o mesmo elemento podem ser levadas a instâncias diferentes: cada uma enxerga uma versão que não inclui a da outra, e o que for gravado por último **sobrescreve** o outro — **perda de edição**, sem aviso. Por isso o `collab` deve ficar em **uma única réplica**.

Um processo Node atende muitas salas ao mesmo tempo, então a capacidade de uma instância é grande. Se um dia for preciso mais, há dois caminhos: a extensão oficial **`@hocuspocus/extension-redis`** (as instâncias sincronizam os documentos por um Redis) ou **roteamento por documento** (todas as conexões de um mesmo elemento sempre na mesma instância). Ambos exigem trabalho e infraestrutura adicionais. No Kubernetes: `replicas: 1` e `strategy: Recreate` (para nunca haver duas instâncias ao mesmo tempo durante uma atualização).

## PostgreSQL

O banco tem **um primário** que aceita escrita; "ter duas réplicas do container" não cria um banco replicado — criaria dois bancos independentes, com dados diferentes. Alta disponibilidade de banco se faz com um **operador** (CloudNativePG, Patroni, Zalando) ou com o **serviço de banco gerenciado** da infraestrutura da FAB, que mantém um primário e réplicas de leitura/failover. O backend continua falando com **um** endereço (o primário). Se a FAB já oferece PostgreSQL como serviço, esse é o caminho.

## MinIO

Também é um armazenamento de dados, não um serviço sem estado: réplicas do container não compartilham os arquivos. O **modo distribuído** do MinIO (vários nós e discos, com *erasure coding*) é uma implantação própria, com requisitos de discos; ou use o **armazenamento de objetos S3** que a infraestrutura já tenha — o sistema só precisa de uma API S3 (ver a contingência no [Roadmap](roadmap.md)). O backend aponta para **um** endereço.

## Como ficaria no Kubernetes

| Recurso | Tipo | Réplicas | Observações |
|---|---|---|---|
| Frontend (Nginx) | `Deployment` | 2+ | *readiness* em `/health` |
| Backend | `Deployment` | **1** hoje; 2+ só após os pontos 1, 3 e 4 acima | *readiness/liveness* em `/actuator/health`; `Secret` com a chave RSA; `Job` de migração |
| Collab | `Deployment` | **1** (`strategy: Recreate`) | WebSocket: *timeouts* longos no Ingress |
| PostgreSQL | operador ou serviço gerenciado | conforme o operador | o backend usa o endereço do primário |
| MinIO / S3 | serviço de armazenamento da FAB, ou MinIO distribuído | — | `MINIO_PUBLIC_URL` com HTTPS |
| Entrada | `Ingress` com TLS | — | `/` → frontend; `/v1`, `/oauth2`, `/login`, `/.well-known` → backend; `/collab` → collab (com *upgrade* de WebSocket) |

O `.env` vira `ConfigMap` (valores) e `Secret` (senhas e a chave RSA); o `nginx.conf` do Compose é substituído pelas regras do Ingress, que fazem o mesmo roteamento por caminho — as mesmas regras descritas no README, no passo "Mesma origem (proxy reverso)".

## Para ir além de uma réplica do backend: o roteiro

1. **Numeração:** restrição única + nova tentativa (ponto 4) — vale mesmo sem réplicas.
2. **Login:** `JdbcOAuth2AuthorizationService` + Spring Session JDBC (ponto 1).
3. **Tempo real:** barramento entre réplicas (ponto 3).
4. **Chave RSA** compartilhada por *Secret* e `APP_OAUTH2_CHAVE_OBRIGATORIA=true` (ponto 2).
5. Só então `replicas: 2` no backend, com testes de login e de edição concorrente passando por réplicas diferentes.
