# FAB Legis

Sistema de elaboração, padronização e gestão de atos normativos do Comando da Aeronáutica — do rascunho à publicação, com numeração automática, editor WYSIWYG estrutural e exportação em PDF/DOCX/HTML.

## O que é o FAB Legis

Elaborar um ato normativo no COMAER é um trabalho meticuloso: a numeração precisa seguir a Espécie Normativa e o Assunto Básico corretos, a estrutura (Parte Preliminar → Parte Normativa → Parte Final) obedece a regras rígidas de redação legislativa, artigos e incisos devem ser renumerados a cada alteração, figuras precisam de legenda sequencial e fonte, e o documento ainda tem de sair em PDF com o brasão, as margens e a marca d'água certas para cada estágio de aprovação.

Hoje, boa parte disso é feita à mão em editores de texto genéricos — o que gera retrabalho, inconsistência de formatação e erros de numeração que só aparecem na revisão final.

**O FAB Legis resolve isso.** É uma plataforma web que trata o ato normativo não como um arquivo de texto, mas como uma **estrutura de dados hierárquica**. Cada capítulo, artigo, parágrafo, inciso e alínea é um nó em uma árvore. A partir daí, o sistema consegue:

- **numerar tudo automaticamente** — inclusive reordenando quando você move um artigo;
- **impedir estruturas inválidas** — uma alínea não pode existir fora de um inciso;
- **mostrar o documento final em tempo real**, lado a lado com o editor;
- **exportar o mesmo conteúdo** em PDF, DOCX e HTML sem reformatar nada;
- **comparar versões** e mostrar exatamente o que mudou entre elas;
- **gerar o texto sugerido** da portaria de alteração, seguindo o Art. 22 da NSCA 5-3;
- **controlar o ciclo de vida** (Rascunho → Minuta → Aprovado → Publicado…) com transições validadas no servidor **e autorizadas por papel**;
- **coordenar edição concorrente** — login por CPF, papéis (Editor/Aprovador/Publicador/Auditor/Admin) com atribuição pessoal por etapa, coautoria, aviso de presença e notificações em tempo real.

!!! info "Público-alvo"
    Seções de legislação, assessorias jurídicas e órgãos centrais de sistema responsáveis pela elaboração de ICA, NSCA, MCA, RCA, DCA, PCA, OCA, RICA, ROCA, TCA e FCA.

## Por onde começar

| Se você quer... | Vá para... |
|---|---|
| Entender o que cada tela faz | [Funcionalidades](funcionalidades.md) |
| Entender como o sistema é construído por dentro | [Arquitetura](arquitetura.md) |
| Entender como um ato normativo é modelado (partes, numeração) | [Modelo de Domínio](dominio.md) |
| Entender login, papéis e colaboração em tempo real | [Autenticação e Colaboração](autenticacao.md) |
| Entender os status de um documento e o ciclo de emenda | [Ciclo de Vida do Documento](ciclo-de-vida.md) |
| Entender como o PDF/portaria são gerados | [Geração de PDF e Portarias](exportacao-pdf.md) |
| Consultar os endpoints da API | [API REST](api-rest.md) |
| Rodar o projeto localmente | [Instalação e Configuração](instalacao.md) |
| Ver o que está planejado para o futuro | [Roadmap](roadmap.md) |

## Stack

=== "Backend"
    | Tecnologia | Versão | Papel |
    |---|---|---|
    | Java | 25 | Linguagem |
    | Spring Boot | 3.5.0 | Framework de aplicação |
    | Spring Data JPA / Hibernate | — | Persistência e mapeamento objeto-relacional |
    | Spring Web (MVC) | — | API REST, incluindo *streams* SSE (notificações e presença) |
    | Spring HATEOAS | — | Links de navegação nos recursos |
    | Spring Security | 6.5 | Autenticação stateless via JWT, autorização por método (`@PreAuthorize`) |
    | jjwt | 0.12.6 | Emissão e validação do access token JWT |
    | PostgreSQL | 16 | Banco de dados relacional |
    | MinIO SDK | 8.5.12 | Armazenamento de objetos (imagens e PDFs) on-premise |
    | Apache FOP | 2.10 | Geração de PDF server-side via XSL-FO |
    | Apache PDFBox | 3.x | Utilitário de manipulação de PDF (dependência disponível para usos futuros) |
    | Flyway | — | Migrações de banco versionadas e auditáveis |
    | Lombok | 1.18.38 | Redução de boilerplate nas *entities* (os DTOs de `application/dtos/**` são **records** Java, imutáveis, sem Lombok) |
    | SpringDoc OpenAPI | 2.6.0 | Documentação Swagger |
    | Maven | Wrapper | Build |

=== "Frontend"
    | Tecnologia | Versão | Papel |
    |---|---|---|
    | Vue | 3.5 (Composition API) | Framework de UI |
    | Quasar | 2.17 | Biblioteca de componentes e design system |
    | Vite | 5.4 | Build tool e dev server com HMR |
    | Pinia | 2.3 | Gerenciamento de estado |
    | Vue Router | 4.5 | Roteamento SPA |
    | TipTap / ProseMirror | 2.10 | Editor WYSIWYG estrutural |
    | diff | 5.2 | Comparação textual entre versões |
    | vuedraggable | 4.1 | Reordenação por drag and drop |
    | Sass | — | Estilos e variáveis do Quasar |

=== "Infraestrutura"
    | Tecnologia | Papel |
    |---|---|
    | Docker / Docker Compose | Orquestração dos serviços |
    | Nginx | Servidor estático do frontend em produção |
    | MinIO | Object storage compatível com S3, 100% on-premise |
    | Multi-stage builds | Imagens enxutas (JRE Alpine no backend, Nginx Alpine no frontend) |
    | Carlito (via Alpine `font-carlito`) | Fonte usada nos PDFs gerados, metricamente compatível com Calibri |
