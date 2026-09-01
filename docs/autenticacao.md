# Autenticação, Papéis e Colaboração

Fase 0 de um plano de autenticação que evolui depois para SSO institucional (Keycloak) — as *claims* do JWT já são nomeadas como o Keycloak nomeia as suas (`sub`, `preferred_username`, `realm_access.roles`) de propósito, para que a migração troque só o emissor do token, não a lógica de autorização.

## Login e sessão

- Login por **CPF + senha** (`POST /v1/auth/login`), validado com o algoritmo oficial de dígito verificador tanto no frontend (feedback imediato) quanto, de forma autoritativa, no backend (`CpfValidator`/`@CpfValido`).
- **Access token JWT de vida curta** (15 min) + **refresh token opaco** (hash SHA-256 armazenado, nunca o valor em si) de vida longa (7 dias), **rotacionado a cada uso** — um valor vazado só é reaproveitável uma vez antes de invalidar dos dois lados (`RefreshTokenService`). O `client.js` do frontend intercepta qualquer 401 e tenta renovar uma vez antes de deslogar, de forma transparente para quem está usando o sistema.
- Usuário administrador padrão criado automaticamente no primeiro boot (`DataSeeder`) — ver credenciais em [Instalação e Configuração](instalacao.md).

!!! note "Detecção de expiração é reativa, por design"
    A validade do token só é verificada no momento em que uma chamada real à API acontece através do `client.js`. Não há timer proativo de logout nem decodificação de expiração no guard de rota — se o usuário ficar numa página já carregada sem disparar nenhuma requisição, a sessão pode *parecer* válida até a próxima interação real. Essa é uma decisão deliberada, não um bug: o comportamento reativo foi mantido intencionalmente.

## Papéis e posse de documento

Todo usuário autenticado já é, implicitamente, **Redator**: cria documentos, edita e exclui (em Rascunho/Minuta) os que autorou ou dos quais é coautor, e **visualiza/baixa qualquer documento do acervo**, de qualquer OM, em qualquer situação. Por cima disso, papéis adicionais (atribuídos na tela **Manter Usuários**, restrita a Admin) concedem mais:

| Papel | Concede |
|---|---|
| **Aprovador** | Aprova, publica e demais transições sensíveis (ver [Ciclo de Vida do Documento](ciclo-de-vida.md)) — só de documentos da própria OM |
| **Admin** | Aprova/publica em qualquer OM, gerencia usuários e organizações militares |
| **Auditor** | Acesso de leitura à trilha de auditoria completa (tela **Auditoria**) — papel independente: Admin **não** enxerga a auditoria automaticamente, precisa ter o papel Auditor atribuído |

**Coautoria:** o autor de um documento pode adicionar outros CPFs como coautores (`DocumentoCompartilhamentoService`) — um coautor ganha os mesmos direitos de edição/exclusão do autor sobre aquele documento específico, mas só o autor pode gerenciar a lista de coautores.

## Edição concorrente

Duas peças deliberadamente separadas, uma para *avisar*, outra para *impedir* colisão real:

- **Presença ao vivo** — enquanto o editor está aberto, uma conexão **SSE** (`GET /{id}/presenca/stream`) informa em tempo real quem mais está editando o mesmo documento (banner + toast); "quem está editando" é literalmente "quem tem essa conexão aberta agora" — sem heartbeat, sem polling, sem tabela própria no banco.
- **Bloqueio otimista por versão** — cada documento carrega um número de versão (`nr_versao`); todo salvamento envia a versão que tinha em mãos, e o backend rejeita com `409` se ela não bater com a atual (`DocumentoConcorrenciaService`), evitando que uma edição sobrescreva silenciosamente outra. Ao colidir, o editor recarrega a versão real do servidor e avisa, sem perder o próprio trabalho não salvo.

## Notificações e auditoria

- **Notificações em tempo real** (sino no topbar, via SSE) — gravadas na mesma transação da ação que as gera e entregues ao vivo a quem estiver conectado: documento compartilhado com você, ou documento da sua OM entrando em situação que aguarda aprovação.
- **Trilha de auditoria** (`LogAuditoria`) — quem visualizou, criou, editou, excluiu, clonou, mudou situação ou (des)compartilhou cada documento, e quando; consultável e filtrável por quem tem o papel Auditor na tela **Auditoria**. O registro sobrevive à exclusão do documento (guarda um retrato do código/descrição no momento da ação, não uma referência viva).
