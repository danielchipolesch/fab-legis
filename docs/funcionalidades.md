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

Extensão TipTap customizada (`extensions/figure.js`) com *NodeView* em Vue: upload da imagem para o MinIO, título, legenda e linha "Fonte:" embutida no HTML para garantir portabilidade na exportação. A numeração ("Figura 1", "Figura 2"…) usa CSS counters no preview web e é resolvida literalmente na exportação, de modo que o PDF e o HTML exportados saem com a numeração correta. O sumário inclui automaticamente a **Lista de Figuras**.

## Exportação de documentos

Geração de PDF **server-side** via **Apache FOP 2.10 / XSL-FO**, e de HTML **server-side** (`DocumentoHtmlService`) seguindo as regras específicas que a NSCA 5-3 dá para a versão eletrônica (capa dispensada, corpo alinhado à esquerda, cabeçalho com Brasão da República alinhado à esquerda). Botão **Exportar HTML** ao lado do botão de PDF em toda tela de edição/visualização de um documento. Detalhamento completo dos dois pipelines, das regras da NSCA 5-3 e da configuração de fontes em [Geração de PDF e Portarias](exportacao-pdf.md).

## Comparação de versões

Página dedicada (`ComparisonPage.vue`) alimentada pelo histórico real de emendas (`EmendaHistorico`, agrupado por ciclo de publicação) — não por snapshots do documento. Ver [Ciclo de emenda](ciclo-de-vida.md#ciclo-de-emenda-alterando-um-ato-ja-publicado) e [Quadro de Justificativas](ciclo-de-vida.md#quadro-de-justificativas-das-modificacoes-propostas).

Para documentos com uma alteração aguardando publicação (`PUBLICADO` + `EM_PUBLICACAO`), a mesma página (e também a tela de visualização) oferece o botão **Texto Sugerido**, que gera automaticamente um rascunho da portaria de alteração — ver [Texto sugerido da portaria (NSCA 5-3, Art. 22)](exportacao-pdf.md#texto-sugerido-da-portaria-nsca-5-3-art-22).

## Visualização do documento

A `DocumentViewerPage` exibe o documento em modo leitura com seções expansíveis:

| Seção | Conteúdo |
|---|---|
| **Informações do Documento** | Metadados (espécie, número, título, assunto, código, **situação BCA** e **situação local** — cada uma em seu próprio campo) e linha do tempo das transições (publicação, alteração publicada, alteração cancelada, revogação...) |
| **Portarias** | Lista de todas as portarias registradas para o documento — edição, alterações (numeradas sequencialmente) e revogação — cada uma com órgão/setor, número, data, BCA e link de download do PDF |
| **Visualização do Documento** | Iframe com o PDF, carregado só com a seção aberta. Mostra por padrão a **versão em tramitação** (se houver etapa local em curso) e oferece o alternador para a **versão vigente (BCA)**; sem etapa em curso, mostra direto a vigente. Ver [Versões do documento](ciclo-de-vida.md#versoes-do-documento-vigente-em-tramitacao) |
| **Anexos** | Upload/listagem/remoção de arquivos vinculados ao documento (`AnexoController`), incluídos como páginas próprias na exportação em PDF e em HTML |
| **Histórico de Versões** | Acesso direto à página de comparação de versões |

Ações disponíveis na topbar: baixar PDF e HTML (com menu para escolher a versão vigente ou em tramitação quando as duas existem), ver texto sugerido da portaria (quando há alteração aguardando publicação), clonar e navegar para a comparação de versões.

## Hub — Área de Trabalho

A tela inicial (`/`, `HubPage`) é o **ponto único de acesso** aos módulos e a "mesa" da pessoa: **quatro cards na mesma linha** com o trabalho dela e o dos outros, **de qualquer módulo**, e, abaixo, o **Acesso Rápido**. (Abaixo de 1024 px os cards passam a ficar um embaixo do outro.)

| Card | Documentos | Ação da linha |
|---|---|---|
| **Meus Documentos em Tramitação** | Autoria ou coautoria, em `RASCUNHO`, `MINUTA`, `EM_ALTERACAO`, `EM_REVISAO`, `EM_PUBLICACAO`, `ANALISE_REVOGACAO` ou `EM_REVOGACAO` | *Editar* (etapa de escrita e a pessoa é autora/coautora) ou *Acompanhar* (está com outra pessoa) |
| **Aguardando Minha Ação** | Atribuídos a mim: como revisor (`EM_REVISAO`, `ANALISE_REVOGACAO`) ou como publicador (`EM_PUBLICACAO`, `EM_REVOGACAO`). Sem nenhum: "Nenhuma ação pendente" | *Revisar* (abre o documento), *Analisar revogação*, *Publicar* ou *Revogar* (tela de Publicação), sempre com o aviso ⚠ |
| **Documentos em Tramitação nas OMs** | O que está em tramitação (as mesmas etapas do primeiro card), **de qualquer OM**, **exceto** onde sou autor ou coautor — esses já estão no primeiro card. É o panorama do que os outros estão fazendo | Sempre *Acompanhar*: **só visualizo** — editar exige ser autor ou coautor |
| **Publicados e Revogados** | `PUBLICADO` e `REVOGADO` **sem tramitação** (`SEM_ETAPA`), **de todas as OMs**, mais recentes primeiro | *Visualizar* |

**Cada documento fica num só lugar do hub** (com uma exceção: o card "Aguardando Minha Ação" é um recorte por ação e pode repetir um documento dos outros). Um documento publicado que está sendo alterado (ou em revogação) tem duas situações ao mesmo tempo — a oficial ("Publicado": a versão em vigor continua valendo) e a etapa interna (a alteração) —, e por isso aparece nos cards de **tramitação**, com **os dois selos** ("Publicado" e a etapa; `selosDaLinha`), e **não** no de publicados e revogados, para onde volta quando a etapa termina. Se a pessoa não é autora nem coautora, ele está em "Documentos em Tramitação nas OMs" — assim nenhum documento em tramitação some do hub. Um documento nunca publicado leva só o selo da etapa. Os cards de tramitação e o de publicados têm um tooltip (ⓘ) que explica o critério.

- **Só leva à ação, nunca a executa:** o link da linha abre direto o documento ou a tela da ação (Revisão, Publicação); nada é aprovado, publicado ou alterado no hub (`utils/painel.js`, com testes).
- **Cada linha se identifica pelo módulo** (ícone com o nome no tooltip) e pela identificação do documento (`DCA 5-1`, `NPA-AGO-01`); cada linha mostra também a **sigla da OM** do documento, nos quatro cards; cada card tem contagem, botão de atualizar e paginação própria, e o cabeçalho de cada um é levemente tingido com a cor da marca. O texto de apresentação sob o título é adaptado da Área de Trabalho do Compras.gov.br (o que a tela é, o que há em cada quadro e para onde ir). O backend serve os três em `/v1/painel/…` (ver [API REST](api-rest.md)).
- **⋮ Exibir detalhes:** abre um modal **só de consulta**, com o **stepper das etapas do ciclo** em que o documento está (publicação inicial, alteração ou revogação — `etapasDoCiclo`; a NPA nunca entra no de alteração) e as informações do documento: módulo, espécie, código, assunto básico (só nas convencionais), OM, autor, coautores, data de criação e última alteração; e, conforme o card, com quem está (nos cards de tramitação), o que se espera da pessoa (aguardando) e, para o que já foi publicado, a data da publicação (e da revogação), a portaria e o BCA nas convencionais ou o Boletim Interno na NPA. Para agir, o modal só oferece "Ir para o módulo".
- **Criar:** o menu tem as opções *Espécie Convencional* e *NPA*, que **apenas levam à tela inicial do módulo** — quem cria de fato é o botão de criar de lá (o usuário sempre passa pela tela do módulo).
- **Acesso Rápido:** um card quadrado, com borda, por módulo (`MODULOS`, em `perfis/index.js`), centralizados e do tamanho de que precisam — o card inteiro é o link. Um módulo novo entra aqui como mais um botão, sem card novo.
- **Breadcrumb:** em todas as telas o caminho passa pela Área de Trabalho: 🏠 → Área de Trabalho → módulo → documento (ex.: 🏠 → Área de Trabalho → NPA → `NPA-AGO-01`).
- **Estado no Pinia:** a página em que cada card está (`stores/painel.js`) e, por módulo, a aba, os filtros, a página e o modo de visualização da tela do módulo (`estadosPorModulo`, em `stores/documentos.js`) ficam guardados: sair (por exemplo, para abrir um documento) e voltar não recomeça do zero, e o que a pessoa fez num módulo não contamina o outro.

## Módulos

O sistema é dividido em **módulos**, um por tipo de espécie (`TipoDeEspecie`, com os nomes da NSCA 5-3): **Espécies Convencionais** (`/convencionais` — MCA, NSCA, ICA, ROCA, DCA…) e **NPA** (`/npa` — Norma Padrão de Ação, das Comunicações Oficiais Padronizadas). Os documentos de um módulo **nunca aparecem no outro**: cada um tem a sua tela inicial (`ModuloPage`, a mesma tela configurada pelo perfil do módulo em `perfis/index.js`), que lista só os documentos do tipo dele — a listagem e o resumo do backend recebem `tipoDeEspecie`. O botão de criar de cada módulo cria só espécies dele (na NPA, a espécie já vem escolhida). O que muda entre os módulos: na NPA a tabela não tem as colunas *Espécie* e *Assunto Básico*, o número chama-se *Identificação* e a situação real chama-se *Boletim Interno* (nas convencionais, *Situação BCA*); o filtro de espécie só aparece quando o módulo tem mais de uma. Um tipo de espécie novo ganha o seu módulo acrescentando um perfil.

## Gestão do acervo

A tela inicial de cada módulo (`ModuloPage`) organiza o acervo do módulo em quatro abas — **Meus Documentos**, **Documentos da Minha OM**, **Documentos de Outras OMs** e **Documentos Revogados** — cada uma com paginação e busca próprias e um contador de quantos documentos do módulo ela contém; a visualização em si é universal (qualquer usuário autenticado vê e baixa qualquer documento, independente de autoria, OM ou situação — ver [Autenticação e Colaboração](autenticacao.md)), as abas são só uma forma de navegar esse mesmo conjunto.

Dentro de cada aba: visão em tabela (ordenada por data de criação decrescente por padrão) ou cards, filtros por espécie (siglas do catálogo real de `EspecieNormativa`, não uma lista fixa), **situação BCA** e **situação local** (filtros e contagens separados), busca textual; a tabela mostra as duas situações em **colunas próprias** ("Situação BCA" e "Situação Local", cada uma ordenável), resumo quantitativo por situação e ações contextuais — editar (Rascunho/Minuta/Em Alteração), clonar e exportar em PDF. O botão de comparar versões não fica na tabela principal: está na página de comparação (ver seção seguinte). As transições de situação que dependem só de posse (enviar para Minuta, enviar para Revisão/Revogação escolhendo a pessoa, cancelar) ficam aqui, atrás de um menu "⋮" que só fica habilitado quando existe alguma ação disponível para a situação atual; revisar, aprovar, publicar e revogar acontecem nas telas dedicadas abaixo — ver [Papéis e posse de documento](autenticacao.md#papeis-e-posse-de-documento).

**Excluir** só aparece no menu de ações para documentos próprios (ou compartilhados) em Rascunho ou Minuta — outras combinações já são bloqueadas no backend (`DocumentoAcessoService.podeExcluir()`), e a opção nem é oferecida na interface.

## Busca Textual

A `BuscaPage` (`/busca`, aberta pelo botão **"Busca no conteúdo"** no card de filtros da tela do módulo, ao lado de "Limpar" e dos botões de lista/cards — sem papel específico, mesma regra de visualização universal do acervo) procura **dentro do texto** dos dispositivos de **todos os documentos, de qualquer aba** (o campo da tela do módulo filtra só a aba atual, por assunto ou número). O botão leva o **termo já digitado** no campo da tela do módulo: a busca abre com ele preenchido (`?q=`) e já executada. Os dispositivos são Artigo, Parágrafo, Inciso, Cláusula..., não só metadado do documento como na busca da tela do módulo. Cada resultado é um dispositivo específico, com o trecho onde o termo foi encontrado já destacado, e um clique abre o documento correspondente na tela de visualização.

Implementada com **`tsvector`/`GIN` do próprio PostgreSQL** (sem Elasticsearch): uma coluna gerada (`STORED`, recalculada automaticamente pelo banco a cada gravação, sem trigger) indexa `tx_conteudo_completo` — o texto puro de cada elemento, já mantido separadamente do JSON TipTap bruto (`DocumentoParteNormativaService.gerarFullTextContent`/`TipTapPlainTextExtractor`) — nas três tabelas de item (parte preliminar/normativa/final). Consulta via `websearch_to_tsquery` (sintaxe de caixa de busca comum: `"frase exata"`, `-excluir`) numa configuração de busca própria (`portuguese_unaccent`) que combina o *stemmer* `portuguese` com o dicionário `unaccent`, para que "publicacao" ache "publicação". Sem checagem de posse: como visualizar já é liberado a qualquer autenticado, a busca não é mais restritiva que abrir o documento diretamente.

## Revisão e Publicação

Duas telas dedicadas, cada uma restrita a quem tem o papel correspondente e mostrando só a **fila pessoal** de quem está logado (documentos atribuídos a ela, nunca o acervo inteiro):

- **Revisão** (`/revisao`, `RevisaoPage.vue`, papel Aprovador) — tabela com **código, título, autores, situação e ações**; documentos em `EM_REVISAO`/`ANALISE_REVOGACAO` atribuídos ao usuário. Abrir (editável enquanto `EM_REVISAO`), Aprovar (escolhendo pessoalmente quem publica) e Devolver.
- **Publicação** (`/publicacao`, `PublicacaoPage.vue`, papel Publicador) — mesma tabela; documentos em `EM_PUBLICACAO`/`EM_REVOGACAO` atribuídos ao usuário. Abrir (só leitura), Publicar/Revogar (formulário de Portaria/BCA e parte preliminar, `PublicarDialog.vue`) e Devolver (só para `EM_PUBLICACAO`; uma revogação aprovada só pode ser formalizada).

Cada uma tem seu próprio breadcrumb (Início → Área de Trabalho → Revisão/Publicação), e um documento aberto a partir de qualquer uma delas carrega essa origem consigo (`?origem=revisao|publicacao`) — o breadcrumb do editor/visualizador então volta para a fila de onde a pessoa veio, não para o acervo do módulo.

Ver [Ciclo de Vida do Documento](ciclo-de-vida.md) para o fluxo completo de atribuição pessoal.

## NPA (Norma Padrão de Ação)

A [NPA](dominio.md#npa-norma-padrao-de-acao) é um módulo próprio (`/npa`) e usa as mesmas telas de documento das convencionais, mas cada uma se adapta à espécie pelo **perfil** do documento (`frontend/src/perfis/`, escolhido pelo `tipoDeEspecie` que o backend informa — nunca pela sigla):

- **Novo documento:** ao escolher a espécie NPA o diálogo troca o *Assunto Básico* pela **Identificação** (texto livre, ex.: `NPA-AGO-01`) e o campo do título vira **Assunto**. O **código** (a identificação) pode ser corrigido depois, no diálogo de **metadados** do editor, enquanto a NPA está em Rascunho ou Minuta; nas convencionais o código é gerado e aparece só para leitura.
- **Editor:** a árvore mostra a numeração pelo caminho (`1`, `1.1`, `1.1.1.1`, `a)`); "Adicionar" oferece só o que a hierarquia da NPA permite (capítulo → seção/parágrafo; seção → subseção/parágrafo; parágrafo → alínea) e seção, subseção e parágrafo do mesmo pai trocam de lugar livremente. Não há promover/rebaixar, "Comparar versões" nem a ajuda da LC 95/1998. O botão **Cabeçalho e assinaturas** (`CamposDaNpaDialog`) edita o setor emissor, o local do fecho e os blocos de assinatura em texto livre — cada bloco com rótulo, até 6 linhas, e pode ser reordenado; um bloco novo entra **embaixo** dos existentes. "Elaborado por" (autor e coautores) e "Aprovado por" (quem aprova) não são digitados: saem do documento.
- **Prévia** (`NpaPreview`): **paginada como o PDF** — uma folha A4 por página, cada uma com a moldura até o fim, o cabeçalho só na primeira e o "n/total" da segunda em diante. A quebra é calculada no navegador (`utils/paginacaoDaNpa.js`): uma cópia invisível do conteúdo é medida bloco a bloco, e título de capítulo/seção fica sempre junto do bloco seguinte, como o `keep-with-next` do PDF. É aproximada (a quebra exata é a do FOP). Os textos vêm de `perfis/npa.js` (espelho de `CabecalhoDaNpa`, backend), os mesmos do PDF e do HTML.
- **Fluxo completo** (criar → minutar → revisão → aprovação → publicação → revogação, sem alteração): ver [Fluxo da NPA](ciclo-de-vida.md#fluxo-da-npa-norma-padrao-de-acao).
- **Publicação e revogação** (`PublicarNpaDialog`, na tela de *Publicação*): só o **número e a data do Boletim Interno**, sem portaria, BCA nem PDF de portaria.
- **Visualização:** sem "Assunto Básico", sem o painel de *Portarias* e sem "Versões"; a referência do Boletim Interno aparece em *Publicação*. Na tela do módulo, "Iniciar Alteração" não é oferecido para uma NPA.
