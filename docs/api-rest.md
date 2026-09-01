# API REST

Documentação interativa completa em **`/swagger-ui.html`**. Todas as rotas abaixo (exceto `/v1/auth/**`) exigem `Authorization: Bearer <token>`; as que alteram posse/situação também passam por `@PreAuthorize` (ver [Autenticação e Colaboração](autenticacao.md)). Resumo dos endpoints:

## Autenticação — `/v1/auth` *(público)*

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/login` | Autentica por CPF + senha, devolve access token + refresh token |
| `POST` | `/refresh` | Troca um refresh token válido por um novo par (rotação) |
| `POST` | `/logout` | Revoga um refresh token |

## Documentos — `/v1/documentos`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Cria documento (calcula o número secundário) |
| `POST` | `/{id}/clonar` | Clona o documento em novo `RASCUNHO` |
| `GET` | `/{id}` | Obtém documento com anexo textual + links HATEOAS |
| `GET` | `/obter-todos` | Lista paginada (DTO enxuto, sem os itens da árvore) |
| `GET` | `/filtrar` | Filtra por espécie normativa e assunto básico |
| `PUT` | `/{id}` | Atualiza metadados (somente Rascunho/Minuta, autor/coautor) |
| `PATCH` | `/{id}/status` | Transição de status validada e autorizada por papel (publicar/alterar/revogar registram Portaria+BCA, ver [Ciclo de Vida](ciclo-de-vida.md)) |
| `PUT` | `/{id}/secoes` | Salva a árvore completa de seções (checagem de versão) |
| `PUT` | `/{idDocumento}/adicionar-item-anexo-parte-textual` | Adiciona item à parte normativa |
| `GET` | `/{id}/numeracao` | Numeração calculada da parte normativa |
| `GET` | `/{id}/pdf` | Gera o PDF oficial do documento sob demanda (Apache FOP) |
| `GET` | `/{id}/portarias` | Lista todas as portarias registradas do documento (edição, alterações numeradas, revogação) |
| `DELETE` | `/{id}` | Remove o documento e seus itens em cascata (somente Rascunho/Minuta, autor/coautor) |
| `GET`/`POST` | `/{id}/compartilhamentos` | Lista ou adiciona um coautor (só o autor) |
| `DELETE` | `/{id}/compartilhamentos/{usuarioId}` | Remove um coautor (só o autor) |
| `GET` | `/{id}/presenca/stream` | Conexão SSE: quem mais está editando este documento agora |

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
