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
| `docs/exportacao-pdf.md` | Geração de PDF (Apache FOP), fontes, texto sugerido de portaria |
| `docs/api-rest.md` | Endpoints |
| `docs/instalacao.md` | Como rodar local/Docker, variáveis de ambiente |
| `docs/roadmap.md` | Perspectivas futuras — mova um item para cá quando virar decisão adiada, ou remova-o quando for implementado |

Se a mudança não se encaixa em nenhuma página existente, prefira estender a mais próxima a criar uma nova; só crie página nova para um assunto genuinamente novo, e nesse caso adicione a entrada em `mkdocs.yml` (`nav:`).
