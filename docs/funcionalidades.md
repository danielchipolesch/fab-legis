# Funcionalidades

## Editor estrutural WYSIWYG

O coração do sistema. Construído sobre **TipTap/ProseMirror**, apresenta três painéis integrados:

| Painel | O que faz |
|---|---|
| **Sidebar (árvore)** | Navegação pela estrutura do documento via `q-tree` do Quasar, com ícones por tipo de elemento, indicador visual (⚠️ amarelo) de seções vazias e **dialog de metadados** (edição de título e número secundário diretamente na sidebar) |
| **Editor central** | Edição rica do elemento selecionado — negrito, itálico, sublinhado, alinhamento, cor, realce, tabelas e figuras |
| **Preview** | Renderização fiel do documento final, atualizada em tempo real, já com toda a numeração aplicada |

O conteúdo de cada elemento é armazenado no banco como **JSON TipTap** (formato ProseMirror), garantindo fidelidade na serialização para XSL-FO (PDF) e HTML sem depender de parsing de HTML.

Operações estruturais disponíveis: adicionar filho, adicionar irmão, **promover** e **rebaixar** elementos na hierarquia (com validação de subárvore), mover para cima/baixo, remover e reordenar por *drag and drop* (persistido no backend).

Um documento novo já abre com os **capítulos padronizados da NSCA 5-3** (Disposições Preliminares, Gerais, Transitórias e Finais, com suas seções e artigos de orientação) — o autor preenche os textos, insere os capítulos do assunto e exclui os de aplicação eventual que não precisar. Detalhes em [Modelo de Domínio](dominio.md#capitulos-padronizados-nsca-5-3).

Regras de numeração automática aplicadas pelo editor estão detalhadas em [Modelo de Domínio](dominio.md#numeracao-automatica-conforme-a-tecnica-legislativa).

## Figuras com numeração sequencial

Extensão TipTap customizada (`extensions/figure.js`) com *NodeView* em Vue: upload da imagem para o MinIO, título, legenda e linha "Fonte:" embutida no HTML para garantir portabilidade na exportação. A numeração ("Figura 1", "Figura 2"…) usa CSS counters no preview web e é resolvida literalmente na exportação, de modo que o mesmo HTML gera PDF e DOCX corretos. O sumário inclui automaticamente a **Lista de Figuras**.

## Exportação de documentos

Geração de PDF **server-side** via **Apache FOP 2.10 / XSL-FO**, e de HTML **server-side** (`DocumentoHtmlService`) seguindo as regras específicas que a NSCA 5-3 dá para a versão eletrônica (capa dispensada, corpo alinhado à esquerda, cabeçalho com Brasão da República alinhado à esquerda). Botão **Exportar HTML** ao lado do botão de PDF em toda tela de edição/visualização de um documento. Detalhamento completo dos dois pipelines, das regras da NSCA 5-3 e da configuração de fontes em [Geração de PDF e Portarias](exportacao-pdf.md).

## Comparação de versões

Página dedicada (`ComparisonPage.vue`) alimentada pelo histórico real de emendas (`EmendaHistorico`, agrupado por ciclo de publicação) — não por snapshots do documento. Ver [Ciclo de emenda](ciclo-de-vida.md#ciclo-de-emenda-alterando-um-ato-ja-publicado) e [Quadro de Justificativas](ciclo-de-vida.md#quadro-de-justificativas-das-modificacoes-propostas).

Para documentos em `ALTERADO`, a mesma página (e também a tela de visualização) oferece o botão **Texto Sugerido**, que gera automaticamente um rascunho da portaria de alteração — ver [Texto sugerido da portaria (NSCA 5-3, Art. 22)](exportacao-pdf.md#texto-sugerido-da-portaria-nsca-5-3-art-22).

## Visualização do documento

A `DocumentViewerPage` exibe o documento em modo leitura com seções expansíveis:

| Seção | Conteúdo |
|---|---|
| **Informações do Documento** | Metadados (espécie, número, título, assunto, código, status) e linha do tempo de datas por status |
| **Portarias** | Lista de todas as portarias registradas para o documento — edição, alterações (numeradas sequencialmente) e revogação — cada uma com órgão/setor, número, data, BCA e link de download do PDF |
| **Visualização do Documento** | Iframe com o PDF armazenado (disponível a partir de `APROVADO`) ou mensagem de indisponibilidade; exibe `q-inner-loading` enquanto o PDF carrega |
| **Anexos** | Upload/listagem/remoção de arquivos vinculados ao documento (`AnexoController`), incluídos como páginas próprias na exportação em PDF e em HTML |
| **Histórico de Versões** | Acesso direto à página de comparação de versões |

Ações disponíveis na topbar: baixar PDF (rascunho gerado sob demanda), ver texto sugerido da portaria (quando `ALTERADO`), clonar e navegar para a comparação de versões.

## Gestão do acervo

A `HomePage` organiza o acervo em quatro abas — **Meus Documentos**, **Documentos da Minha OM**, **Documentos de Outras OMs** e **Documentos Revogados** — cada uma com paginação e busca próprias e um contador de quantos documentos ela contém; a visualização em si é universal (qualquer usuário autenticado vê e baixa qualquer documento, independente de autoria, OM ou situação — ver [Autenticação e Colaboração](autenticacao.md)), as abas são só uma forma de navegar esse mesmo conjunto.

Dentro de cada aba: visão em tabela (ordenada por data de criação decrescente por padrão) ou cards, filtros por espécie (siglas do catálogo real de `EspecieNormativa`, não uma lista fixa) e situação, busca textual, resumo quantitativo por situação e ações contextuais — editar (Rascunho/Minuta/Em Alteração), clonar e exportar em PDF. O botão de comparar versões saiu da tabela principal (segue disponível na página de comparação em si, ver seção seguinte). As transições de situação que dependem só de posse (enviar para Minuta, enviar para Revisão/Revogação escolhendo a pessoa, cancelar) ficam aqui, atrás de um menu "⋮" que só fica habilitado quando existe alguma ação disponível para a situação atual; revisar/aprovar/publicar/revogar de fato saíram da `HomePage` e viraram as telas dedicadas abaixo — ver [Papéis e posse de documento](autenticacao.md#papeis-e-posse-de-documento).

**Excluir** só aparece no menu de ações para documentos próprios (ou compartilhados) em Rascunho ou Minuta — outras combinações já são bloqueadas no backend (`DocumentoAcessoService.podeExcluir()`), e a opção nem é oferecida na interface.

## Busca Textual

A `BuscaPage` (`/busca`, item "Busca Textual" sempre visível no menu do usuário — sem papel específico, mesma regra de visualização universal do acervo) procura **dentro do texto** dos dispositivos (Artigo, Parágrafo, Inciso, Cláusula...), não só por metadado do documento como a busca da `HomePage`. Cada resultado é um dispositivo específico, com o trecho onde o termo foi encontrado já destacado, e um clique abre o documento correspondente na tela de visualização.

Implementada com **`tsvector`/`GIN` do próprio PostgreSQL** (sem Elasticsearch): uma coluna gerada (`STORED`, recalculada automaticamente pelo banco a cada gravação, sem trigger) indexa `tx_conteudo_completo` — o texto puro de cada elemento, já mantido separadamente do JSON TipTap bruto (`DocumentoParteNormativaService.gerarFullTextContent`/`TipTapPlainTextExtractor`) — nas três tabelas de item (parte preliminar/normativa/final). Consulta via `websearch_to_tsquery` (sintaxe de caixa de busca comum: `"frase exata"`, `-excluir`) numa configuração de busca própria (`portuguese_unaccent`) que combina o *stemmer* `portuguese` com o dicionário `unaccent`, para que "publicacao" ache "publicação". Sem checagem de posse: como visualizar já é liberado a qualquer autenticado, a busca não é mais restritiva que abrir o documento diretamente.

## Revisão e Publicação

Duas telas dedicadas, cada uma restrita a quem tem o papel correspondente e mostrando só a **fila pessoal** de quem está logado (documentos atribuídos a ela, nunca o acervo inteiro):

- **Revisão** (`/revisao`, `RevisaoPage.vue`, papel Aprovador) — tabela com **código, título, autores, situação e ações**; documentos em `EM_REVISAO`/`ANALISE_REVOGACAO` atribuídos ao usuário. Abrir (editável enquanto `EM_REVISAO`), Aprovar (escolhendo pessoalmente quem publica) e Devolver.
- **Publicação** (`/publicacao`, `PublicacaoPage.vue`, papel Publicador) — mesma tabela; documentos em `EM_PUBLICACAO`/`EM_REVOGACAO` atribuídos ao usuário. Abrir (só leitura), Publicar/Revogar (formulário de Portaria/BCA, `PublicarDialog.vue`) e Devolver.

Cada uma tem seu próprio breadcrumb (Início → Revisão/Publicação), e um documento aberto a partir de qualquer uma delas carrega essa origem consigo (`?origem=revisao|publicacao`) — o breadcrumb do editor/visualizador então volta para a fila de onde a pessoa veio, não para o acervo geral (`HomePage`).

Ver [Ciclo de Vida do Documento](ciclo-de-vida.md) para o fluxo completo de atribuição pessoal.
