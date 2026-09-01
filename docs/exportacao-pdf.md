# Geração de PDF e Portarias

## Pipeline de geração do PDF oficial

Geração de PDF **server-side** via **Apache FOP 2.10 / XSL-FO**, seguindo o padrão da **NSCA 5-3**:

- margens A4 oficiais;
- cabeçalho com brasão da República (Portaria de Aprovação) e brasão da FAB (Capa);
- estrutura de três páginas: **Portaria de Aprovação → Capa → Sumário + Corpo normativo**;
- **sumário automático** com intervalos de artigos por capítulo/seção/subseção e hiperlinks internos;
- **marca d'água por status** — `RASCUNHO` e `MINUTA` em vermelho, `APROVADO` em verde;
- renderização de imagens embutidas via MinIO, tabelas e figuras.

O pipeline de geração é: conteúdo JSON TipTap → `XslFoContentRenderer` (serializa inlines: negrito, itálico, cor, links, imagens) → `DocumentoFoBuilder` (monta o XSL-FO completo) → Apache FOP → bytes PDF. Ao entrar em `APROVADO` ou `ALTERADO`, o PDF é gerado e armazenado no MinIO (`urlPdf`) — a prévia embutida na página de visualização usa essa cópia quando disponível. O botão **Baixar PDF**, porém, sempre chama a geração ao vivo (`GET /{id}/pdf`), independente de já existir uma cópia armazenada (ver [Roadmap](roadmap.md) — cache do PDF gerado).

A portaria (PDF enviado pelo usuário no momento de publicar/alterar/revogar) **não é mesclada** ao PDF do documento — permanece um arquivo próprio e íntegro, registrado em `PortariaPublicacao` (ver [Portaria, BCA e registro de publicações](ciclo-de-vida.md#portaria-bca-e-registro-de-publicacoes)). O Apache PDFBox permanece como dependência do projeto (não usado no momento, mas mantido disponível para necessidades futuras, como manipulação de PDF em outros fluxos).

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

Quando um documento está em `ALTERADO` (ciclo de emenda aprovado, aguardando a portaria que vai republicá-lo), o sistema gera um **texto sugerido** — um rascunho para revisão humana, não um documento jurídico final — que ajuda a redigir a portaria de alteração, seguindo as regras de transcrição do **Art. 22 da NSCA 5-3**.

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
