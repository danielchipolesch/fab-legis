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
| `docs/exportacao-pdf.md` | Geração de PDF (Apache FOP) e HTML, fontes, texto sugerido de portaria, regra de consistência entre PDF e HTML |
| `docs/api-rest.md` | Endpoints |
| `docs/instalacao.md` | Como rodar local/Docker, variáveis de ambiente |
| `docs/roadmap.md` | Perspectivas futuras — mova um item para cá quando virar decisão adiada, ou remova-o quando for implementado |

Se a mudança não se encaixa em nenhuma página existente, prefira estender a mais próxima a criar uma nova; só crie página nova para um assunto genuinamente novo, e nesse caso adicione a entrada em `mkdocs.yml` (`nav:`).

## Testes unitários acompanham as regras de negócio

**Toda vez que uma regra de negócio for criada, alterada ou excluída, o teste unitário correspondente deve ser criado, atualizado ou removido na mesma tarefa** — do mesmo jeito que a documentação em `docs/`. Não deixe para depois nem espere o usuário pedir. São regras de negócio: numeração, transições de situação (status), validações de publicação/revogação, estrutura padrão de novos documentos, regras de acesso/permissão, ciclo de emenda — tudo o que a técnica legislativa ou o fluxo do sistema determina.

- **Onde ficam:** `backend/src/test/java`, no mesmo pacote da classe testada e com o nome `<Classe>Test` (ex.: `NumeracaoServiceTest`, `DocumentoStatusServiceTest`).
- **Sem infraestrutura:** teste unitário não sobe Spring nem banco — instancia a classe direto ou usa Mockito para os colaboradores. Um teste que precise de contexto ou de banco é de integração e leva `@Tag("integration")` (fica fora do `mvn test` padrão).
- **O teste descreve a regra documentada, não o que o código faz hoje.** A fonte da regra é `docs/` (e a norma: LC 95/1998, Decreto 12.002/2024, NSCA 5-3). Se o teste falhar porque o código diverge da regra documentada, isso é um **bug a reportar ao usuário** — não ajuste o teste para "passar". Se a correção não puder ser feita na tarefa, deixe o teste com `@Disabled("motivo")`, descrevendo o comportamento correto, para o alvo não se perder.
- **Regra excluída, teste excluído:** ao remover uma regra, remova (ou reescreva) o teste dela em vez de deixá-lo quebrado ou `@Disabled` sem motivo.
- **Rode antes de concluir a tarefa** (não há JDK/Maven exigido na máquina; usa-se um container): comando em [`docs/instalacao.md`](docs/instalacao.md#testes).
- **Frontend:** Vitest (`npm test`), com os testes ao lado do código, no formato `<arquivo>.test.js` (ex.: `frontend/src/utils/numbering.test.js`). Vale a mesma regra do backend: lógica de negócio no frontend (hoje, a numeração em `numbering.js` e as regras do fluxo em `fluxoDocumento.js`) muda junto com o teste. `numbering.js` espelha `NumeracaoService.java` (ver `docs/dominio.md`): ao mudar a regra de numeração, altere as duas implementações e os dois testes (`NumeracaoServiceTest` e `numbering.test.js`) — os cenários de emenda são os mesmos de propósito. Comando em [`docs/instalacao.md`](docs/instalacao.md#testes).

## Consistência entre formatos de exportação (PDF e HTML) e a prévia

O documento é exportado em dois formatos, PDF e HTML, e o editor mostra uma prévia (a NPA tem layout próprio nos três — `DocumentoFoNpaBuilder`, `LeiauteHtmlDeNpa` e a prévia `NpaPreview`, todos alimentados por `CabecalhoDaNpa` — ver [`docs/exportacao-pdf.md`](docs/exportacao-pdf.md)). Cada um é gerado por um construtor próprio a partir do mesmo JSON TipTap — nenhum é derivado dos outros. **Qualquer mudança que altere elementos do documento — estrutura, formatação, numeração, regra de negócio da técnica legislativa — deve ser averiguada nos 3 lugares (PDF, HTML e prévia), não só naquele em que a mudança foi pedida**, ressalvadas as particularidades que a norma ou o próprio formato exigem (ex.: HTML não tem capa e usa alinhamento à esquerda no corpo, por exigência da própria NSCA 5-3 — não é uma inconsistência a corrigir). Isso vale tanto para mudanças pedidas explicitamente num formato quanto para bugs encontrados em um deles — sempre confira se o mesmo problema/regra se aplica aos outros antes de considerar a tarefa concluída.

## Convenção de nomenclatura (português vs. inglês)

O domínio deste sistema é a técnica legislativa brasileira (LC 95/1998, Decreto nº 12.002/2024, NSCA 5-3) — os termos de negócio **são** em português e não têm tradução que faça sentido (Documento, Artigo, Emenda, Portaria, Espécie Normativa, Assunto Básico...). A convenção adotada:

- **Infraestrutura/scaffolding genérico → inglês**: pacotes (`domain.services`, `application.controllers`), anotações e padrões de framework, utilitários sem relação com a lei em si.
- **Vocabulário de negócio → português**: entidades, serviços, DTOs, métodos e variáveis que implementam ou nomeiam um conceito da técnica legislativa. Isso vale tanto no backend (Java) quanto no frontend (Vue/JS).

Ao criar algo novo, mantenha o nome do conceito consistente com o que já existe no resto do código para o mesmo conceito — não traduza um nome de entidade/DTO/página já usado em outro lugar (ex.: se a entidade é `EspecieNormativa`, o DTO correspondente é `EspecieNormativaResponseDto`, não `DocumentationTypeResponseDto`).

**Isso é a linguagem ubíqua do projeto**: o termo usado para um conceito de domínio deve ser sempre o mesmo, em toda camada — entidade/DTO/serviço no backend, variável/componente/store no frontend, rótulo mostrado na tela e texto em `docs/` — nunca um sinônimo, abreviação ou tradução alternativa, mesmo que pareça mais natural num contexto específico. `docs/dominio.md` é o glossário de referência (Documento, Parte Preliminar, Parte Normativa, Anexos, Epígrafe, Ementa, Preâmbulo, Fecho, Assinatura, Capítulo, Seção, Subseção, Artigo, Parágrafo, Parágrafo único, Inciso, Alínea, Subalínea, Emenda, Espécie Normativa, Assunto Básico...), e espelha o vocabulário da própria LC 95/1998, do Decreto nº 12.002/2024 e da NSCA 5-3 — ao introduzir um conceito de domínio novo, confira primeiro se a norma já dá um nome a ele antes de inventar um. Vale também no sentido inverso: um nome já usado para um conceito não pode ser reaproveitado para outro conceito diferente, mesmo que pareça conveniente no momento.

## Regras que variam por espécie entram por interface

Cada `EspecieNormativa` tem um **tipo de espécie** (`TipoDeEspecie`), com os nomes da NSCA 5-3: `CONVENCIONAL` (Espécies Convencionais: MCA, NSCA, ICA, ROCA, DCA…) e `COMUNICACAO_OFICIAL_PADRONIZADA` (Espécies de Comunicações Oficiais Padronizadas, Cap. VIII, Seção VIII — a NPA se enquadra aqui). Cada tipo segue um conjunto de regras. Um comportamento que varia por espécie — numeração, estrutura inicial, layout de PDF/HTML, rótulo dos anexos, ciclo de vida, campos do cabeçalho — **entra como método de uma interface do pacote `domain.regras`** (ou de uma interface nova ali), implementada por `RegrasDeEspecieConvencional` e `RegrasDeComunicacaoOficialPadronizada` e obtida por `RegrasDasEspecies.para(especie)`. **Nunca** teste a sigla ou o nome da espécie num serviço (`if (especie == NPA)`). No frontend vale o mesmo: o que varia por espécie sai de `frontend/src/perfis/` (escolhido por `tipo_de_especie`), e as regras que o frontend espelha da NPA (`perfis/npa.js`) mudam junto com o backend e com os testes (`npa.test.js` ↔ `NumeracaoDeNpaTest`, `HierarquiaDeNpaTest`, `CabecalhoDaNpaTest`). Ver [`docs/arquitetura.md`](docs/arquitetura.md#regras-por-especie-normativa-atras-de-interfaces).

## Armadilhas conhecidas e decisões deliberadas

O que **não dá para descobrir lendo o código** e já custou tempo. O "como funciona" está em `docs/`; aqui ficam só as armadilhas e as decisões tomadas de propósito — antes de "corrigir" uma delas, pergunte ao usuário.

### Ambiente de desenvolvimento

- **Acesse o frontend por `http://127.0.0.1:5173`, nunca por `localhost`.** O issuer e o redirect do OAuth2 são `127.0.0.1`; por `localhost` o login trava em `{"message":"Não autenticado."}`.
- **Depois de um `git pull` que mexa em `backend/src/main/resources/db/migration`, recrie o volume do Postgres** — o sistema ainda não está em produção, então as migrations são *reescritas* (números reaproveitados), e um banco criado antes acusa `checksum mismatch` e o backend não sobe. Só o Postgres: `docker compose rm -sf postgres && docker volume rm fab-legis_postgres_data` (`docker compose down -v` apaga também o MinIO). Faça backup antes se houver dado que importe (`pg_dump` via `docker exec`).
- **A chave RSA do Authorization Server é efêmera** (gerada a cada boot): reiniciar o backend invalida todos os tokens — é preciso logar de novo. Ver `docs/instalacao.md` (chave de assinatura) antes de ir para produção.
- **Git Bash no Windows:** `docker exec` com caminho absoluto precisa de `MSYS_NO_PATHCONV=1`. O `curl` do host não alcança `127.0.0.1` nesta máquina — para chamar a API à mão, use `docker exec fab-legis-backend wget ... http://localhost:8081/...`.
- **Testes:** backend em container descartável e frontend com `docker exec fab-legis-frontend npx vitest run` — comandos em [`docs/instalacao.md`](docs/instalacao.md#testes). Um `ERROR` no log de `DocumentoStatusServiceTest` é o teste de falha simulada de PDF, não uma falha real; vale o resumo `BUILD SUCCESS`.

### Editor e prévia (TipTap)

- **Não use `generateHTML()` de `@tiptap/html` nas telas de leitura**: ele descarta todo `style` (cor, realce, alinhamento), porque o `prosemirror-model` aplica `style` por `dom.style.cssText` e o `zeed-dom` não implementa isso. Use `frontend/src/editor/conteudoParaHtml.js` (DOM do navegador). O conteúdo *inline* de um elemento na prévia passa por `htmlEmLinha` (tira só os `<p>`, mantém as marcas) — nunca reduza a texto puro.
- **O texto de orientação entre `[]`** dos modelos de documento novo é `textStyle` `#FF0000` gravado só no modelo (`ConteudoTipTapComOrientacao`); depois disso é texto do autor, que muda a cor como quiser. Deve aparecer vermelho no editor, na prévia, no PDF e no HTML.
- **O editor colaborativo nasce `editable: false` e só libera após o primeiro `synced`** (`WysiwygEditor.vue`): sem isso, o texto digitado no editor local antes do remount para o modo Yjs era descartado silenciosamente.
- **Tabela exige `table-layout: fixed`** (editor, prévia e HTML exportado) — senão uma imagem dentro de uma célula estoura a borda. No PDF, `XslFoContentRenderer.renderTable` calcula a largura da coluna e a repassa como teto da figura. Não existe atalho de teclado para excluir tabela: a exclusão (e inserir/remover linha e coluna) é pela toolbar contextual do `WysiwygEditor.vue`.

### Decisões deliberadas

- **Comentários em linha foram removidos de propósito** (complexidade sem retorno). Não reintroduza sem alinhar com o usuário.
- **Número de página no PDF:** Espécie Convencional **não tem** (navega-se pelo artigo/capítulo, do sumário); a **NPA tem** ("n/total", `DocumentoFoNpaBuilder`); anexos de imagem (ANEXO II em diante) têm rodapé próprio. Cada um é gerado por um construtor à parte — não "unifique".
- **`ADMIN` não implica `EDIT`/`APROV`/`PUBLIC`** (papel puramente administrativo). O único caso em que os dois se juntam é o admin seed do `DataSeeder` (`ADMIN` + `EDIT`), para o primeiro usuário já conseguir criar um documento.
- **A NPA não passa por alteração** (sem "Iniciar Alteração", sem `EM_ALTERACAO`) — para mudar uma NPA publicada, cria-se outra e revoga-se a anterior.
- **Navegação em módulos:** a *Área de Trabalho* é o hub (cards por etapa, atravessando os módulos); *Espécies Convencionais* e *NPA* são módulos com listas próprias. Revisão e Publicação são filas por papel, não módulos. Use "Módulo de Espécies Convencionais" / "Módulo de NPA", não "subsistema".
