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
