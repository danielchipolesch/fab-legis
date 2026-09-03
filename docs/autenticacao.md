# Autenticação, Papéis e Colaboração

Fase 0 de um plano de autenticação que evolui depois para SSO institucional (Keycloak) — as *claims* do JWT já são nomeadas como o Keycloak nomeia as suas (`sub`, `preferred_username`, `realm_access.roles`) de propósito, para que a migração troque só o emissor do token, não a lógica de autorização.

## Login e sessão

- Login por **CPF + senha** (`POST /v1/auth/login`), validado com o algoritmo oficial de dígito verificador tanto no frontend (feedback imediato) quanto, de forma autoritativa, no backend (`CpfValidator`/`@CpfValido`).
- **Access token JWT de vida curta** (15 min) + **refresh token opaco** (hash SHA-256 armazenado, nunca o valor em si) de vida longa (7 dias), **rotacionado a cada uso** — um valor vazado só é reaproveitável uma vez antes de invalidar dos dois lados (`RefreshTokenService`). O `client.js` do frontend intercepta qualquer 401 e tenta renovar uma vez antes de deslogar, de forma transparente para quem está usando o sistema.
- Usuário administrador padrão criado automaticamente no primeiro boot (`DataSeeder`) — ver credenciais em [Instalação e Configuração](instalacao.md).

!!! note "Detecção de expiração é reativa, por design"
    A validade do token só é verificada no momento em que uma chamada real à API acontece através do `client.js`. Não há timer proativo de logout nem decodificação de expiração no guard de rota — se o usuário ficar numa página já carregada sem disparar nenhuma requisição, a sessão pode *parecer* válida até a próxima interação real. Essa é uma decisão deliberada, não um bug: o comportamento reativo foi mantido intencionalmente.

## Papéis e posse de documento

Nenhum poder sobre o ciclo de vida de um documento é implícito — todos os papéis são atribuídos explicitamente na tela **Manter Usuários** (restrita a Admin), em `PapelEnum`. Uma pessoa sem papel nenhum só visualiza/baixa qualquer documento do acervo, de qualquer OM, em qualquer situação.

| Papel | Concede |
|---|---|
| **Editor** (`EDIT`) | Cria documentos e edita/exclui (em Rascunho/Minuta) os que autorou ou dos quais é coautor; move o documento entre as etapas do fluxo escolhendo, pessoalmente, quem revisa ou quem publica a seguir (ver abaixo) |
| **Aprovador** (`APROV`) | Age só nos documentos que lhe foram atribuídos pessoalmente como revisor (`EM_REVISAO`/`ANALISE_REVOGACAO`): aprova (escolhendo o Publicador) ou devolve. Também pode, livremente, reabrir para alteração qualquer documento publicado da própria OM (`PUBLICADO → EM_ALTERACAO`) |
| **Publicador** (`PUBLIC`) | Age só nos documentos que lhe foram atribuídos pessoalmente como publicador (`EM_PUBLICACAO`/`EM_REVOGACAO`): publica ou revoga (com portaria/BCA) ou devolve |
| **Admin** | Papel puramente administrativo — gerencia usuários e organizações militares. Não edita, revisa nem publica documento nenhum, mesmo o próprio |
| **Auditor** | Acesso de leitura à trilha de auditoria completa (tela **Auditoria**) — papel independente: Admin **não** enxerga a auditoria automaticamente, precisa ter o papel Auditor atribuído |

**Atribuição pessoal, não um pool por OM:** ao enviar um documento para revisão (ou para análise de revogação), o Editor escolhe uma pessoa específica com papel Aprovador da mesma OM (`GET /usuarios/elegiveis?papel=APROV`) — só ela pode agir naquele documento a partir daí, não qualquer Aprovador da OM. Da mesma forma, ao aprovar, o Aprovador escolhe pessoalmente o Publicador. Essa escolha fica registrada em `Documento.revisorAtribuido`/`publicadorAtribuido` e é o que autoriza (`DocumentoAcessoService.podeMudarStatus`) cada passo seguinte — ver [Ciclo de Vida do Documento](ciclo-de-vida.md) para o fluxo completo.

**Coautoria:** o autor de um documento pode adicionar outros CPFs como coautores (`DocumentoCompartilhamentoService`) — um coautor ganha os mesmos direitos de edição/exclusão do autor sobre aquele documento específico, mas só o autor pode gerenciar a lista de coautores.

## Edição concorrente

Em RASCUNHO/MINUTA, duas peças complementares — uma para o *conteúdo* de cada elemento, outra para a *estrutura* da árvore:

- **Colaboração ao vivo por elemento (CRDT/Yjs)** — o texto de cada artigo/parágrafo/inciso é editado através do serviço `collab` (Node.js + [Hocuspocus](https://tiptap.dev/docs/hocuspocus/introduction), ver [Arquitetura](arquitetura.md)): duas pessoas no mesmo elemento veem as letras uma da outra em tempo real, com *merge* automático, sem tela de conflito. A persistência no Postgres acontece em paralelo (debounced 2s, até 10s sob digitação contínua de qualquer um dos presentes), mas a visualização entre colaboradores não depende de salvar. Elementos ainda não persistidos (recém-criados, sem id do backend) usam um modo local simples até o primeiro salvamento.
    - O indicador "Salvando…"/"Salvo" do topo reflete o momento real da persistência no Postgres, não o ACK (quase instantâneo) do WebSocket — o `collab` avisa os navegadores conectados via mensagem *stateless* a cada alteração e a cada gravação concluída (ou falha).
    - A prévia do documento (painel PDF aproximado) também se atualiza em tempo real conforme qualquer pessoa digita no elemento aberto, sem esperar o debounce.
    - Uma falha ao persistir (ex.: token expirado em sessões de edição muito longas) é registrada e sinalizada ao navegador ("Não salvo"), mas nunca derruba o serviço `collab` inteiro — um erro de um elemento não pode tirar a colaboração de todos os outros documentos do ar.
- **Mudanças estruturais em tempo real** — criar, mover ou excluir um elemento (não é edição de texto, é mudança na árvore) é salvo por *diff* contra o que já está persistido (`PATCH /{id}/secoes`, nunca reescreve o `conteudo` de um elemento existente) e propagado aos demais navegadores conectados via o mesmo canal SSE de presença (`event: estrutura`) — a árvore de quem mais está editando se atualiza sozinha, por *patch* incremental (nunca um recarregamento completo, que interromperia quem estiver digitando ao vivo em outro elemento). Cada aba gera um id próprio (`clientId`) enviado junto com o salvamento e devolvido no evento, para que quem originou a mudança ignore o próprio eco. Se o elemento excluído era o que você tinha aberto, o editor fecha sozinho com um aviso.
- **Presença ao vivo** — enquanto o editor está aberto, a mesma conexão **SSE** (`GET /{id}/presenca/stream`) informa em tempo real quem mais está editando o mesmo documento (banner + toast, `event: presenca`); "quem está editando" é literalmente "quem tem essa conexão aberta agora" — sem heartbeat, sem polling, sem tabela própria no banco.
- **Bloqueio otimista por versão** — reservado a metadados do documento (título) e, na prática, raramente acionado hoje que conteúdo e estrutura têm seus próprios mecanismos de concorrência acima; cada documento carrega um número de versão (`nr_versao`), e o backend rejeita com `409` se a versão enviada não bater com a atual (`DocumentoConcorrenciaService`).

## Notificações e auditoria

- **Notificações em tempo real** (sino no topbar, via SSE) — gravadas na mesma transação da ação que as gera e entregues ao vivo a quem estiver conectado: documento compartilhado com você, ou documento atribuído pessoalmente a você para revisar ou publicar (ver papéis acima).
- **Trilha de auditoria** (`LogAuditoria`) — quem visualizou, criou, editou, excluiu, clonou, mudou situação ou (des)compartilhou cada documento, e quando; consultável e filtrável por quem tem o papel Auditor na tela **Auditoria**. O registro sobrevive à exclusão do documento (guarda um retrato do código/descrição no momento da ação, não uma referência viva).
