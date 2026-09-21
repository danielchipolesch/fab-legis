# API REST

Documentação interativa completa em **`/swagger-ui.html`**. Todas as rotas abaixo (exceto os endpoints de autenticação) exigem `Authorization: Bearer <token>`; as que alteram posse/situação também passam por `@PreAuthorize` (ver [Autenticação e Colaboração](autenticacao.md)). Resumo dos endpoints:

## Autenticação — Authorization Server *(raiz do backend, não sob `/v1`, público)*

Endpoints padrão do Spring Authorization Server (`AuthorizationServerConfig`), não específicos deste domínio — ver [Autenticação e Colaboração](autenticacao.md) para o fluxo completo (Authorization Code + PKCE).

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/oauth2/authorize` | Início do fluxo de autorização (navegação de página, não fetch) |
| `GET`/`POST` | `/login` | Formulário de login (submetido pela `LoginPage.vue` via POST HTML tradicional) |
| `POST` | `/oauth2/token` | Troca o `code` (+ `code_verifier`) por um access token |
| `GET` | `/oauth2/jwks` | Chaves públicas RSA para validação do token (backend e `collab` usam) |
| `GET` | `/.well-known/openid-configuration` | Metadata OIDC do Authorization Server |
| `GET`/`POST` | `/logout` | Encerra a sessão de login (navegação de página, não fetch — ver nota de `SameSite` em autenticacao.md) |

## Documentos — `/v1/documentos`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Cria documento já com a estrutura inicial da espécie. Ato normativo: exige `idAssuntoBasico` (calcula o número secundário) e nasce com os [capítulos padronizados da NSCA 5-3](dominio.md#capitulos-padronizados-nsca-5-3). [NPA](dominio.md#npa-norma-padrao-de-acao): exige `identificacao` (texto livre), não usa assunto básico e nasce com a estrutura do Anexo XII |
| `POST` | `/{id}/clonar` | Clona o documento em novo `RASCUNHO` |
| `GET` | `/{id}` | Obtém documento com anexo textual + links HATEOAS — inclui `numeracao` (capítulo/seção/subseção/artigo já calculados pelo servidor, ver [Modelo de Domínio](dominio.md#numeracao-automatica-conforme-a-tecnica-legislativa)) |
| `GET` | `/obter-todos` | Lista paginada (DTO enxuto, sem os itens da árvore); filtros `aba`, `busca`, `tipoDeEspecie` (o módulo: `CONVENCIONAL` ou `COMUNICACAO_OFICIAL_PADRONIZADA`), `especieSigla`, `situacaoBca`, `situacaoLocal`. Cada item traz `situacaoBca` e `situacaoLocal`. `GET /resumo` devolve as contagens `porAba`, `porSituacaoBca` e `porSituacaoLocal` |
| `GET` | `/filtrar` | Filtra por espécie normativa e assunto básico |
| `GET` | `/busca?q=&page=&size=` | Busca full-text no **conteúdo** dos dispositivos (`tsvector`/PostgreSQL, ver [Funcionalidades](funcionalidades.md#busca-textual)) — resultado paginado por dispositivo (não por documento), com trecho destacado |
| `PUT` | `/{id}` | Atualiza metadados (somente Rascunho/Minuta, autor/coautor). `identificacao` (o código do documento) só é aceita numa NPA e em Rascunho/Minuta; nas convencionais um valor diferente do atual é recusado |
| `PATCH` | `/{id}/status` | Muda a **Situação Local** (corpo: `situacaoLocal` + `revisorId`/`publicadorId` conforme a etapa; `SEM_ETAPA` = concluir a etapa em curso). Transição validada e autorizada por papel; publicar/revogar registram Portaria+BCA (a parte preliminar só na 1ª publicação) — ou, numa NPA, só `numeroBoletimInterno` + `dataBoletimInterno` e são os únicos a mudar a Situação BCA — ver [Ciclo de Vida](ciclo-de-vida.md) |
| `GET` | `/{id}/pdf?versao=` | `/{id}/html?versao=` | PDF/HTML do documento. `versao=VIGENTE` (a da Situação BCA, armazenada) ou `TRAMITACAO` (a da etapa local em curso); sem o parâmetro, a em tramitação se houver, senão a vigente. `404` se a versão pedida não existe |
| `PATCH` | `/{id}/secoes` | Salva a árvore de seções por *diff* contra o que já está persistido (checagem de versão) — nunca reescreve `conteudo` de elemento existente, criado/atualizado/excluído propagam via SSE (`event: estrutura`); resposta é `{ itens, numeracao }`, não só a árvore |
| `PATCH` | `/{id}/elementos/{elementoId}/conteudo` | Grava só o `conteudo` de um elemento — usado pelo serviço `collab` a cada persistência da edição colaborativa |
| `GET` | `/{id}/pode-editar` | 204 se o usuário autenticado pode editar o documento, 403 caso contrário — usado pelo `collab` para autorizar a conexão a uma sala |
| `PUT` | `/{idDocumento}/adicionar-item-anexo-parte-textual` | Adiciona item à parte normativa |
| `GET` | `/{id}/numeracao` | Numeração calculada da parte normativa |
| `GET` | `/{id}/pdf` | Gera o PDF oficial do documento sob demanda (Apache FOP) |
| `GET` | `/{id}/html` | Gera o HTML oficial do documento sob demanda (regras da NSCA 5-3 para a versão eletrônica — ver [Exportação HTML](exportacao-pdf.md#exportacao-html)) |
| `GET` | `/{id}/portarias` | Lista todas as portarias registradas do documento (edição, alterações numeradas, revogação) |
| `DELETE` | `/{id}` | Remove o documento e seus itens em cascata (somente Rascunho/Minuta, autor/coautor) |
| `GET`/`POST` | `/{id}/compartilhamentos` | Lista ou adiciona um coautor (só o autor) |
| `DELETE` | `/{id}/compartilhamentos/{usuarioId}` | Remove um coautor (só o autor) |
| `GET` | `/{id}/presenca/stream` | Conexão SSE: quem mais está editando este documento agora (`event: presenca`) e mudanças estruturais da árvore em tempo real (`event: estrutura`) |

## Emendas — `/v1/documentos` (ciclo de alteração)

| Método | Rota | Descrição |
|---|---|---|
| `PATCH` | `/{docId}/emendar/{secao}/{elementoId}` | Aplica `ALTERAR` \| `REVOGAR` \| `DESFAZER` a um elemento (documento deve estar `EM_ALTERACAO`) |
| `POST` | `/{docId}/emendar/{secao}` | Inclui um novo elemento por emenda |
| `PATCH` | `/{docId}/emendar/{secao}/{elementoId}/reordenar` | Reordena um artigo incluído por emenda entre outros artigos incluídos ainda não publicados |
| `GET` | `/{id}/historico` | Histórico de transições de status do documento |
| `GET` | `/{id}/mapa-alteracao` | Quadro de Justificativas por ciclo (elemento atual + todos os já publicados) |
| `POST` | `/{id}/mapa-alteracao/pdf` | Exporta o quadro de um ciclo em PDF (A4 paisagem) |
| `GET` | `/com-historico-emenda` | IDs de documentos com pelo menos uma emenda registrada — usado para habilitar "Comparar versões" na home |

## NPA — `/v1/documentos/{id}/npa`

Campos do cabeçalho e do fecho que só a [NPA](dominio.md#npa-norma-padrao-de-acao) tem. Documento de outra espécie responde `404`.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Setor emissor, local do fecho, blocos de assinatura escritos (`rotulo` + `linhas`, texto livre) e, só na resposta, `elaboradoPor` (autor e coautores) e `aprovadoPor` (quem aprovou; vazio antes da aprovação) — qualquer usuário autenticado |
| `PUT` | `/` | Grava esses campos (só quem pode editar; recusado depois que a NPA é publicada, e se um bloco usar o rótulo "Elaborado por" ou "Aprovado por", que são automáticos) |

## Painel — `/v1/painel`

Os três cards do hub, de qualquer módulo; qualquer usuário autenticado. Devolvem `Page` do mesmo DTO da listagem (`page`, `size`, padrão 8), mais recentemente mexidos primeiro.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/em-andamento` | Meus documentos (autoria ou coautoria) em etapa de trabalho |
| `GET` | `/aguardando-acao` | Documentos atribuídos a mim como revisor (`EM_REVISAO`, `ANALISE_REVOGACAO`) ou publicador (`EM_PUBLICACAO`, `EM_REVOGACAO`) |
| `GET` | `/publicados-e-revogados` | `PUBLICADO` e `REVOGADO` sem etapa em andamento (`SEM_ETAPA`), de todas as OMs |

## Anexos — `/v1/documentos/{documentoId}/anexos`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` | Lista os anexos do documento |
| `POST` | `/` | Envia um arquivo (multipart) como anexo |
| `DELETE` | `/{anexoId}` | Remove um anexo |

## Espécies normativas — `/v1/especie-normativa`

`POST` · `GET /{id}` · `GET /obter-todos` · `PUT /{id}` · `DELETE /{id}`

## Assuntos básicos — `/v1/assunto-basico`

`POST` · `GET /{id}` · `GET /obter-por-codigo-assunto-basico/{code}` · `GET /obter-todos` · `PUT /{id}` · `DELETE /{id}`

## Imagens — `/v1/imagens`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/upload` | Envia imagem (multipart, máx. 10 MB) para o MinIO e devolve a URL "canônica" do objeto |
| `POST` | `/urls-assinadas` | Troca uma lista de URLs canônicas por URLs assinadas (S3 pre-signed, válidas por 1h) — necessário porque o bucket é privado |

O `ImagemService` cria o bucket sob demanda na primeira execução; o bucket é **privado** (sem política de leitura pública) — a URL devolvida no upload não é diretamente acessível pelo navegador, só serve como referência estável a ser resolvida via `/urls-assinadas` no momento de exibir a imagem/PDF. Ver [Arquitetura](arquitetura.md#visao-geral-dos-servicos).

## Usuários — `/v1/usuarios` *(Admin)*

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/` · `/{id}` | Lista ou obtém um usuário |
| `POST` | `/` | Cria usuário (CPF, senha, OM, papéis) |
| `PUT` | `/{id}` | Atualiza nome, OM, papéis e situação (ativo/inativo) |
| `PATCH` | `/{id}/senha` | Redefine a senha de um usuário |

Sem exclusão definitiva — usuários são autores de documento (FK sem `ON DELETE`), então o ciclo de vida é ativar/desativar, nunca apagar.

Duas rotas sob `/v1/usuarios` sobrescrevem a restrição de Admin da classe (`isAuthenticated()`) — alimentam os seletores de pessoa por nome (CPF é dado pessoal, quase nunca sabido de cor, ver [Autenticação e Colaboração](autenticacao.md#papeis-e-posse-de-documento)):

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/elegiveis?papel=&q=` | Candidatos a revisor/publicador — escopado à OM de quem chama; `q` opcional filtra por nome/nome de guerra |
| `GET` | `/buscar?q=` | Candidatos a coautor — **sem** filtro de OM/papel (coautoria não é restrita a isso); exige 2+ caracteres |

## Organizações militares — `/v1/organizacoes-militares`

`GET /` — lista simples, usada para popular o seletor de OM na tela de usuários.

## Auditoria — `/v1/auditoria` *(Auditor)*

`GET /` — trilha paginada e filtrável por documento, usuário, ação e período. Requer o papel Auditor especificamente (Admin não tem acesso automático — ver [Autenticação e Colaboração](autenticacao.md)).

## Notificações — `/v1/notificacoes`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/stream` | Conexão SSE: notificações ao vivo do usuário autenticado |
| `GET` | `/nao-lidas` · `/` | Notificações não lidas, ou histórico completo paginado |
| `PATCH` | `/{id}/lida` · `/lidas` | Marca uma notificação, ou todas, como lida |
