# Exportação do Documento (PDF, HTML) e Portarias

## Pipeline de geração do PDF oficial

Geração de PDF **server-side** via **Apache FOP 2.10 / XSL-FO**, seguindo o padrão da **NSCA 5-3**:

- margens A4 oficiais;
- cabeçalho com brasão da República (Portaria de Aprovação) e Gádio Alado (Capa);
- estrutura de três páginas: **Portaria de Aprovação → Capa → Sumário + Corpo normativo**;
- **capa conforme NSCA 5-3, Art. 17**: Ministério da Defesa/Comando da Aeronáutica, nome por extenso da organização militar que elaborou o ato (`Documento.om` — por padrão a OM do autor na criação, mas alterável na tela de Metadados do editor enquanto o documento estiver em `RASCUNHO`/`MINUTA`, ver `DocumentoService.update`), gládio alado, assunto básico e legenda com sigla/número/título/ano (`DocumentoFoFrontMatterBuilder.buildCapaSequence`); a prévia do editor (`DocumentoPreview.vue`) espelha o mesmo layout;
- **sumário automático** com intervalos de artigos por capítulo/seção/subseção e hiperlinks internos;
- **marca d'água por status** — `RASCUNHO`/`MINUTA` em vermelho ("RASCUNHO"/"MINUTA"), `EM_ALTERACAO` em laranja ("EM ALTERAÇÃO"), `EM_REVISAO` em azul ("EM REVISÃO"), `APROVADO`/`ALTERADO` em verde ("APROVADO") — nas demais situações (`PUBLICADO`, `REVOGADO`...) o PDF final não leva marca d'água nenhuma (`DocumentoFoContext.buildStaticContentWatermark`); a prévia aproximada do editor (`DocumentoPreview.vue`) espelha as mesmas cores/textos;
- renderização de imagens embutidas via MinIO, tabelas e figuras.

O pipeline de geração é: conteúdo JSON TipTap → `XslFoContentRenderer` (serializa inlines: negrito, itálico, cor, links, imagens) → `DocumentoFoBuilder` (monta o XSL-FO completo) → Apache FOP → bytes PDF. Ao entrar em `APROVADO`, `ALTERADO`, `PUBLICADO` ou `REVOGADO`, o PDF é gerado e armazenado no MinIO (`urlPdf`) — a prévia embutida na página de visualização usa essa cópia quando disponível (`DocumentoPdfService.STATUS_COM_PDF_ARMAZENADO`). `APROVADO`/`ALTERADO` nunca ficam parados como situação atual do documento (cascateiam direto para `EM_PUBLICACAO` na mesma transação que gera o PDF — ver [Ciclo de Vida do Documento](ciclo-de-vida.md)), então é `EM_PUBLICACAO` quem efetivamente carrega essa cópia (com a marca d'água "APROVADO") enquanto o documento aguarda a publicação de fato — por isso também consta no conjunto armazenado. **`EM_REVISAO` é deliberadamente excluído desse armazenamento**: como o revisor atribuído pode editar o conteúdo nessa etapa, uma cópia gravada ficaria desatualizada assim que ele digitasse algo — o documento é sempre renderizado **ao vivo** enquanto estiver nessa situação (e em qualquer outra fora do conjunto acima), sem persistir nada. O botão **Baixar PDF** também sempre chama a geração ao vivo (`GET /{id}/pdf`), independente de já existir uma cópia armazenada (ver [Roadmap](roadmap.md) — cache do PDF gerado).

A portaria (PDF enviado pelo usuário no momento de publicar/alterar/revogar) **não é mesclada** ao PDF do documento — permanece um arquivo próprio e íntegro, registrado em `PortariaPublicacao` (ver [Portaria, BCA e registro de publicações](ciclo-de-vida.md#portaria-bca-e-registro-de-publicacoes)). O Apache PDFBox permanece como dependência do projeto (não usado no momento, mas mantido disponível para necessidades futuras, como manipulação de PDF em outros fluxos).

## Exportação HTML

Geração de HTML **server-side** (`DocumentoHtmlService.java`, a partir do mesmo conteúdo JSON TipTap que alimenta o PDF — nenhum dos dois é gerado a partir do outro, os dois partem em paralelo do mesmo dado, ver nota de consistência abaixo), seguindo as regras específicas que a **NSCA 5-3** dá para a versão eletrônica — diferentes do PDF em pontos que a própria norma distingue explicitamente:

- **Art. 8, XXI** — corpo do ato normativo formatado para A4, **alinhado à esquerda** (não justificado como no PDF), exceto os agrupamentos de artigos (capítulo/seção/subseção), que continuam **centralizados**, igual ao PDF;
- **Art. 17, V, §1º** — a **capa é dispensada** na versão HTML (o PDF continua tendo capa normalmente);
- **Art. 18** — o cabeçalho antecede a epígrafe com **Brasão da República alinhado à esquerda** e os demais elementos (Ministério da Defesa, Comando da Aeronáutica, e a OM que elaborou o ato como "órgão secundário", já que ela não aparece mais na capa dispensada) **centralizados**, com entrelinhas simples (1,0) — no PDF, todo o cabeçalho/capa é centralizado, brasão incluso.

Estrutura HTML: **Portaria de Aprovação → Sumário + Corpo normativo** (sem a página de Capa que o PDF tem). Botão **HTML** ao lado do botão **PDF** em toda tela que já tinha exportação de PDF do documento, exceto a página principal (acervo) — que nunca teve exportação por linha, só pelas telas de edição/visualização de um documento específico.

Assim como no PDF (âncoras internas via `fo:basic-link`/`id`), cada linha do Sumário é um link (`<a href="#norm-<id>">`) para o elemento correspondente no corpo (`id="norm-<id>"` no `<div>` de capítulo/seção/subseção, ou numa âncora vazia logo antes do artigo) — clicar no capítulo/seção/artigo do sumário rola a página até lá.

Os **Anexos** (arquivos vinculados ao documento, `AnexoController`) também viram páginas próprias no HTML, uma por anexo, numeradas em romano na sequência do Sumário (que é sempre "ANEXO I") — mesma numeração e mesma imagem (resolvida via `ImagemService.getImageAsDataUri`, cliente interno do MinIO) que o PDF já usava em `DocumentoFoFrontMatterBuilder.buildAnexoSequence`.

**Geração/armazenamento seguem exatamente a mesma regra do PDF** (`DocumentoHtmlService.STATUS_COM_HTML_ARMAZENADO`, idêntico a `DocumentoPdfService.STATUS_COM_PDF_ARMAZENADO`): gerado e salvo no MinIO (`urlHtml`) nas mesmas transições de status (`APROVADO`/`ALTERADO`/`PUBLICADO`/`REVOGADO`, sempre junto com o PDF — ver `DocumentoStatusService.regenerarPdf`/`regenerarHtml`), servido dessa cópia quando disponível, renderizado ao vivo fora dessas situações (nunca em `EM_REVISAO`, pelo mesmo motivo do PDF: o revisor atribuído pode editar o conteúdo nessa etapa).

**Aviso "Esta versão não substitui a publicada no BCA."**: só no HTML (`.aviso-nao-substitui`, vermelho, centralizado) — o PDF é a cópia oficial mantida em `urlPdf`, servida sem ressalva; o HTML é uma cópia de leitura conveniente e precisa deixar isso explícito. Fica dentro do mesmo `<div class="page-break">` da Portaria, logo após a assinatura — acompanha a Portaria antes da quebra de página que leva ao "ANEXO I", nunca gruda na página seguinte nem aparece no corpo do ato.

## Consistência entre PDF, HTML e DOCX (planejado, ver [Roadmap](roadmap.md))

**Qualquer mudança que altere elementos do documento exportado — estrutura, formatação, regra de negócio da técnica legislativa (NSCA 5-3/LC 95/1998/Decreto nº 12.002/2024) — deve ser averiguada nos 3 formatos (PDF, HTML e, quando implementado, DOCX), não só naquele em que a mudança foi originalmente pedida.** Os três nunca são gerados um a partir do outro (cada um tem seu próprio construtor: `DocumentoFoBuilder` para PDF, `DocumentoHtmlService` para HTML, e a Rota 1 planejada para DOCX é também um construtor próprio, direto do JSON TipTap, não uma conversão do HTML — ver Roadmap) — então uma regra corrigida em um não se propaga sozinha para os outros dois; cada um precisa da própria correção, ressalvadas as particularidades que a norma ou o próprio formato exigem (ex.: alinhamento do corpo e capa dispensada só valem para HTML, Art. 8 XXI/17 V §1º da NSCA 5-3).

## Fonte: Calibri (via Carlito)

Todo o documento — editor, prévia em tela, HTML exportado e PDF gerado — usa **Calibri** como fonte. Como o Calibri é proprietário da Microsoft e não pode ser redistribuído livremente, o sistema usa **Carlito**, uma fonte metricamente compatível (mesmas larguras de glifo) e livremente redistribuível, registrada sob o nome "Calibri" em toda a stack:

- **Editor e prévia web** (`WysiwygEditor.vue`, `DocumentPreview.vue`, `DiffViewer.vue`, `pdfExport.js`) — usam a pilha CSS `'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif`.
- **HTML exportado** (`DocumentoHtmlService.java`) — mesma pilha de fontes no CSS embutido.
- **PDF gerado pelo Apache FOP** — requer configuração explícita, já que simplesmente declarar `font-family="Calibri"` no XSL-FO não é suficiente (o FOP silenciosamente cai para um dos 14 fontes-base padrão se o nome não estiver registrado):
    1. `backend/Dockerfile` instala o pacote `font-carlito` do Alpine na imagem de runtime (`apk add --no-cache font-carlito`).
    2. `backend/src/main/resources/fop-config.xml` registra os 4 arquivos TTF da Carlito (`Regular`, `Bold`, `Italic`, `BoldItalic`) como *font-triplets* sob o nome `"Calibri"`.
    3. `FopFactoryProvider` (singleton compartilhado por `DocumentoPdfService` e `MapaAlteracaoPdfService`) carrega essa configuração via `FopConfParser` na inicialização do `FopFactory`.
    4. Cada `<fo:page-sequence>` do XSL-FO declara `font-family="Calibri"` individualmente (páginas-sequência irmãs não herdam fonte entre si) — em `DocumentoFoFrontMatterBuilder.java`, `DocumentoFoCorpoBuilder.java` e `MapaAlteracaoPdfService.java`.

O PDF gerado embute de fato os glifos da Carlito (confirmável inspecionando os bytes do PDF por entradas `BaseFont` como `Carlito`, `Carlito-Bold`, `Carlito-Italic`).

## Texto sugerido da portaria (NSCA 5-3, Art. 22)

Quando um documento está em `EM_PUBLICACAO` e já foi publicado antes (ciclo de emenda aprovado, aguardando a portaria que vai republicá-lo — `ALTERADO` no histórico, mas nunca como situação atual, já que cascateia direto pra `EM_PUBLICACAO`), o sistema gera um **texto sugerido** — um rascunho para revisão humana, não um documento jurídico final — que ajuda a redigir a portaria de alteração, seguindo as regras de transcrição do **Art. 22 da NSCA 5-3**.

Disponível como botão "Texto Sugerido" tanto na página de comparação (`ComparisonPage.vue`) quanto na tela de visualização do documento (`DocumentViewerPage.vue`), implementado **inteiramente no frontend** (`frontend/src/utils/textoSugeridoPortaria.js`) — não requer nenhuma chamada adicional ao backend além dos dados já carregados (mapa de alteração do ciclo pendente + portarias registradas).

### Regras aplicadas

| Regra (Art. 22) | Como é aplicada |
|---|---|
| I — transcrição entre aspas + "(NR)" | Todo artigo/dispositivo incluído ou alterado é transcrito entre aspas, terminado em `(NR)` |
| II — a palavra "revogado" nunca aparece em texto entre aspas | Dispositivos revogados entram numa cláusula separada, no fim: "Ficam revogados os seguintes dispositivos: ..." |
| V — número/letra de dispositivo revogado nunca é reaproveitado | Garantido pela numeração já existente (`numbering.js`/`NumeracaoService`) — o texto sugerido só consome essa numeração, não a recalcula |
| §1º — sufixo de letra para dispositivo inserido | Idem — já calculado por `renumberElementsEmAlteracao` |
| VI-a — cabeçalho menciona espécie e data da publicação original | `"${especie} nº ${numero}, de ${data da portaria de EDICAO}, passa a vigorar com as seguintes alterações:"` |
| VI-c-1 — linha pontilhada precedida do artigo, quando só o caput está preservado antes do dispositivo alterado | Um bloco por artigo parcialmente alterado, com uma linha pontilhada logo após "Art. X" |
| VI-c-2 — duas linhas pontilhadas quando o caput **e** o dispositivo subsequente (primeiro filho do artigo) estão ambos preservados | A segunda linha (genérica, sem rótulo — o texto normativo só define rótulo para a primeira) entra sempre que o primeiro dispositivo tocado não é o primeiro filho do artigo, não importa quantos dispositivos são pulados até lá |
| VI-c-3 — alteração de unidade inferior dentro de unidade superior do artigo | Cada contêiner intermediário preservado (ex.: um § cujo texto próprio não mudou mas contém um inciso alterado) recebe sua própria linha pontilhada rotulada, aplicado recursivamente por nível — dois dispositivos irmãos alterados dentro do mesmo contêiner (ex.: dois incisos do mesmo §) compartilham uma única linha desse contêiner |

### Escopo

O texto é gerado sempre a partir do **ciclo pendente atual** (`cicloReferencia == null` no mapa de alteração) — independente de qual ciclo estiver selecionado no seletor da própria tela de comparação. Sem itens pendentes, exibe "Nenhuma alteração pendente neste ciclo." em vez de texto vazio. Sem portaria de edição registrada (documento antigo, dado ausente), o cabeçalho cai para o rótulo do documento, sem data.

Fora de escopo: qualquer geração/validação do PDF final da portaria (continua 100% manual/externa — só o texto de apoio é gerado).
