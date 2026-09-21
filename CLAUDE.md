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

## Testes unitários acompanham as regras de negócio

**Toda vez que uma regra de negócio for criada, alterada ou excluída, o teste unitário correspondente deve ser criado, atualizado ou removido na mesma tarefa** — do mesmo jeito que a documentação em `docs/`. Não deixe para depois nem espere o usuário pedir. São regras de negócio: numeração, transições de situação (status), validações de publicação/revogação, estrutura padrão de novos documentos, regras de acesso/permissão, ciclo de emenda — tudo o que a técnica legislativa ou o fluxo do sistema determina.

- **Onde ficam:** `backend/src/test/java`, no mesmo pacote da classe testada e com o nome `<Classe>Test` (ex.: `NumeracaoServiceTest`, `DocumentoStatusServiceTest`).
- **Sem infraestrutura:** teste unitário não sobe Spring nem banco — instancia a classe direto ou usa Mockito para os colaboradores. Um teste que precise de contexto ou de banco é de integração e leva `@Tag("integration")` (fica fora do `mvn test` padrão).
- **O teste descreve a regra documentada, não o que o código faz hoje.** A fonte da regra é `docs/` (e a norma: LC 95/1998, Decreto 12.002/2024, NSCA 5-3). Se o teste falhar porque o código diverge da regra documentada, isso é um **bug a reportar ao usuário** — não ajuste o teste para "passar". Se a correção não puder ser feita na tarefa, deixe o teste com `@Disabled("motivo")`, descrevendo o comportamento correto, para o alvo não se perder.
- **Regra excluída, teste excluído:** ao remover uma regra, remova (ou reescreva) o teste dela em vez de deixá-lo quebrado ou `@Disabled` sem motivo.
- **Rode antes de concluir a tarefa** (não há JDK/Maven exigido na máquina; usa-se um container): comando em [`docs/instalacao.md`](docs/instalacao.md#testes).
- **Frontend:** Vitest (`npm test`), com os testes ao lado do código, no formato `<arquivo>.test.js` (ex.: `frontend/src/utils/numbering.test.js`). Vale a mesma regra do backend: lógica de negócio no frontend (hoje, a numeração em `numbering.js` e as regras do fluxo em `fluxoDocumento.js`) muda junto com o teste. `numbering.js` espelha `NumeracaoService.java` (ver `docs/dominio.md`): ao mudar a regra de numeração, altere as duas implementações e os dois testes (`NumeracaoServiceTest` e `numbering.test.js`) — os cenários de emenda são os mesmos de propósito. Comando em [`docs/instalacao.md`](docs/instalacao.md#testes).

## Consistência entre formatos de exportação (PDF/HTML/DOCX)

O documento é exportado em três formatos (PDF já implementado, HTML já implementado, DOCX planejado; a NPA tem layout próprio nos mesmos formatos — `DocumentoFoNpaBuilder`, `LeiauteHtmlDeNpa` e a prévia `NpaPreview`, todos alimentados por `CabecalhoDaNpa` — ver [`docs/exportacao-pdf.md`](docs/exportacao-pdf.md)), cada um gerado por um construtor próprio a partir do mesmo JSON TipTap — nenhum é derivado dos outros dois. **Qualquer mudança que altere elementos do documento — estrutura, formatação, numeração, regra de negócio da técnica legislativa — deve ser averiguada nos 3 formatos, não só naquele em que a mudança foi pedida**, ressalvadas as particularidades que a norma ou o próprio formato exigem (ex.: HTML não tem capa e usa alinhamento à esquerda no corpo, por exigência da própria NSCA 5-3 — não é uma inconsistência a corrigir). Isso vale tanto para mudanças pedidas explicitamente num formato quanto para bugs encontrados em um deles — sempre confira se o mesmo problema/regra se aplica aos outros antes de considerar a tarefa concluída.

## Convenção de nomenclatura (português vs. inglês)

O domínio deste sistema é a técnica legislativa brasileira (LC 95/1998, Decreto nº 12.002/2024, NSCA 5-3) — os termos de negócio **são** em português e não têm tradução que faça sentido (Documento, Artigo, Emenda, Portaria, Espécie Normativa, Assunto Básico...). A convenção adotada:

- **Infraestrutura/scaffolding genérico → inglês**: pacotes (`domain.services`, `application.controllers`), anotações e padrões de framework, utilitários sem relação com a lei em si.
- **Vocabulário de negócio → português**: entidades, serviços, DTOs, métodos e variáveis que implementam ou nomeiam um conceito da técnica legislativa. Isso vale tanto no backend (Java) quanto no frontend (Vue/JS).

Ao criar algo novo, mantenha o nome do conceito consistente com o que já existe no resto do código para o mesmo conceito — não traduza um nome de entidade/DTO/página já usado em outro lugar (ex.: se a entidade é `EspecieNormativa`, o DTO correspondente é `EspecieNormativaResponseDto`, não `DocumentationTypeResponseDto`).

**Isso é a linguagem ubíqua do projeto**: o termo usado para um conceito de domínio deve ser sempre o mesmo, em toda camada — entidade/DTO/serviço no backend, variável/componente/store no frontend, rótulo mostrado na tela e texto em `docs/` — nunca um sinônimo, abreviação ou tradução alternativa, mesmo que pareça mais natural num contexto específico. `docs/dominio.md` é o glossário de referência (Documento, Parte Preliminar, Parte Normativa, Anexos, Epígrafe, Ementa, Preâmbulo, Fecho, Assinatura, Capítulo, Seção, Subseção, Artigo, Parágrafo, Parágrafo único, Inciso, Alínea, Subalínea, Emenda, Espécie Normativa, Assunto Básico...), e espelha o vocabulário da própria LC 95/1998, do Decreto nº 12.002/2024 e da NSCA 5-3 — ao introduzir um conceito de domínio novo, confira primeiro se a norma já dá um nome a ele antes de inventar um. Vale também no sentido inverso: um nome já usado para um conceito não pode ser reaproveitado para outro conceito diferente, mesmo que pareça conveniente no momento.

## Regras que variam por espécie entram por interface

Cada `EspecieNormativa` tem um **tipo de espécie** (`TipoDeEspecie`), com os nomes da NSCA 5-3: `CONVENCIONAL` (Espécies Convencionais: MCA, NSCA, ICA, ROCA, DCA…) e `COMUNICACAO_OFICIAL_PADRONIZADA` (Espécies de Comunicações Oficiais Padronizadas, Cap. VIII, Seção VIII — a NPA se enquadra aqui). Cada tipo segue um conjunto de regras. Um comportamento que varia por espécie — numeração, estrutura inicial, layout de PDF/HTML, rótulo dos anexos, ciclo de vida, campos do cabeçalho — **entra como método de uma interface do pacote `domain.regras`** (ou de uma interface nova ali), implementada por `RegrasDeEspecieConvencional` e `RegrasDeComunicacaoOficialPadronizada` e obtida por `RegrasDasEspecies.para(especie)`. **Nunca** teste a sigla ou o nome da espécie num serviço (`if (especie == NPA)`). No frontend vale o mesmo: o que varia por espécie sai de `frontend/src/perfis/` (escolhido por `tipo_de_especie`), e as regras que o frontend espelha da NPA (`perfis/npa.js`) mudam junto com o backend e com os testes (`npa.test.js` ↔ `NumeracaoDeNpaTest`, `HierarquiaDeNpaTest`, `CabecalhoDaNpaTest`). Ver [`docs/arquitetura.md`](docs/arquitetura.md#regras-por-especie-normativa-atras-de-interfaces).
