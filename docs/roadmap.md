# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — o Authorization Server embutido (Spring Authorization Server, OAuth2 + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhado para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.
- **Cobertura de testes** — a numeração (backend e `numbering.js` no frontend) e as transições de situação já têm testes unitários (ver [Instalação — Testes](instalacao.md#testes)). Faltam os demais serviços de domínio (ciclo de emenda, regras de acesso), o restante da lógica do frontend (ex.: `stores/editor.js`) e testes de integração dos controllers com Testcontainers — estes dependem de o ambiente de CI/CD ter Docker disponível.
- **Exportação DOCX nativa** — o HTML portável produzido pelo editor já foi pensado para isso; falta o conversor no backend.

## Médio prazo — fluxo de trabalho completo

- **Assinatura digital ICP-Brasil** — assinatura do PDF da portaria e/ou do documento final com carimbo de tempo, conferindo validade jurídica ao ato publicado. A decisão de manter a portaria como arquivo separado (não mesclado ao PDF do documento — ver [Portaria, BCA e registro de publicações](ciclo-de-vida.md#portaria-bca-e-registro-de-publicacoes)) foi tomada justamente para preservar essa possibilidade: uma assinatura cobre um intervalo de bytes exato do arquivo original.
- **Cache de PDF em tramitação no MinIO** — hoje a versão em tramitação nas etapas em que o texto ainda muda (`RASCUNHO`, `MINUTA`, `EM_REVISAO`, `EM_ALTERACAO`) é renderizada pelo FOP a cada visualização, protegida só pelo [limite de gerações simultâneas](exportacao-pdf.md#limite-de-geracoes-simultaneas-de-pdf). A ideia é guardar o PDF renderizado no próprio MinIO, com chave (documento, carimbo do conteúdo), para as réplicas compartilharem o cache sem serviço novo (sem Redis) — reabrir a mesma minuta sem editá-la passa a custar uma ida ao MinIO em vez de uma renderização; uma regra de expiração do bucket descarta as entradas antigas. Cuidado central: a chave **precisa** mudar a cada alteração de conteúdo — a edição colaborativa grava os elementos por `PATCH /elementos/{id}/conteudo`, que pode não incrementar a versão do documento, então a chave deve usar também um carimbo do conteúdo (última alteração entre os elementos ou um hash); errar aqui serve PDF desatualizado. Só vale a pena se a carga real justificar; um cache em memória por réplica é correto (a chave é imutável), mas com baixa taxa de acerto sem afinidade de sessão.
- **Anexos com parte textual** — hoje os anexos além do ANEXO I (sumário + corpo normativo) são só de imagem. Quando houver anexos com texto, a regra "**Continuação do ANEXO X**" da segunda página em diante (já implementada para os anexos de imagem — ver [Exportação](exportacao-pdf.md)) se estende a eles: bastará usar o mesmo layout de página (`a4-anexo`, `DocumentoFoFrontMatterBuilder.buildContinuacaoAnexo`) na sequência de páginas de cada anexo de texto, com o próprio rótulo. O ANEXO I continua sem esse cabeçalho, por decisão explícita.
- **Grafo de referências normativas** — mapear quais atos alteram, revogam ou citam quais outros, e alertar automaticamente quando um ato referenciado for revogado.

## NPA (Norma Padrão de Ação) — proposta de implementação

A NPA é uma espécie de **uso interno da OM**, para disciplinar rotinas internas, com layout, elementos, numeração e ciclo de vida próprios (modelo: Anexo XII da NSCA 5-3). As demais espécies são produzidas pelas OM, mas seu âmbito extrapola a OM. Ela será o **segundo regime normativo** (`NPA`), implementado nas interfaces de [Regimes normativos](arquitetura.md#regimes-normativos-regras-por-especie-atras-de-interfaces) — a refatoração que as introduziu já está feita (sem mudar comportamento).

### Como é a NPA (decifrado do layout e de uma NPA real)

- **Página:** A4, margens de 2 cm; todo o conteúdo dentro de **uma moldura que continua em todas as páginas**; número de página **"n/total"** no cabeçalho das páginas 2 em diante. O título "ANEXO XII — NORMA PADRÃO DE AÇÃO" do layout é o nome do modelo na NSCA, **não** é impresso na NPA.
- **Cabeçalho (tabela de 4 colunas):** linhas centralizadas em negrito com Comando, OM e o setor emissor; à esquerda o **DOM** (distintivo da OM — tratado depois), com a **identificação** logo abaixo (`NPA-AGO-01` / `NPA 44-__/2026`), **campo livre** que o usuário preenche conforme o setor; **DATAS** (EMISSÃO = data da aprovação; EFETIVAÇÃO = Boletim Interno nº e data, preenchidos na publicação); **DISTRIBUIÇÃO** (sempre OSTENSIVA — constante do regime); **ASSUNTO**; **ANEXOS** (`A - …; B - …; e C - …`, gerado dos próprios anexos).
- **Corpo, numeração pelo nível (algarismo arábico):** `1` capítulo (maiúsculas, negrito) → `1.1` seção → `1.1.1` subseção → `1.1.1.1`; **parágrafo** (o dispositivo em si, com o texto) fica direto sob capítulo, seção ou subseção e recebe o número do caminho (`3.1` sob o capítulo 3; `2.1.1.1` sob uma subseção). Seção, subseção e parágrafo do mesmo pai **dividem uma única sequência**. **Todo elemento é numerado**. **Alíneas** (`a)`, `b)`…) enumeram um parágrafo — **só existem depois de um parágrafo**. Número em negrito; título de seção sublinhado.
- **Estrutura obrigatória (do layout):** capítulo 1 DISPOSIÇÕES PRELIMINARES com as seções Finalidade, Âmbito e Referências; capítulo 2 DISPOSIÇÕES GERAIS com a seção Conceituações; capítulo 3 DISPOSIÇÕES FINAIS com um parágrafo direto (`3.1`). Como alínea só existe depois de parágrafo, a seção Referências nasce com um parágrafo introdutório ("Constituem referências:") seguido das alíneas.
- **Fecho:** linha "Local, dd de mês de aaaa" e **blocos de assinatura em texto livre** (rótulo — "Elaborado por", "Visto", "Proposto por", "Aprovo" — e linhas livres); ao final, "(Publicada no Boletim Interno Ostensivo nº __, de __ de ____)".
- **Anexos** ao final do documento (como nos demais normativos), rotulados **A, B, C…** e listados no cabeçalho.
- **Ciclo de vida:** **sem alteração** — só publicação (no Boletim Interno) e revogação. Para mudar uma NPA publicada, cria-se outra e revoga-se a anterior.

### Regras por interface

| Interface | Regra da NPA |
|---|---|
| `RegrasDeHierarquiaDosElementos` (nova) | capítulo na raiz; seção sob capítulo; subseção sob seção; parágrafo sob capítulo, seção ou subseção; alínea só sob parágrafo. Reaproveita os tipos já existentes (`CAPITULO`, `SECAO_NORMATIVA`, `SUBSECAO_NORMATIVA`, `PARAGRAFO`, `ALINEA`); artigo, inciso, parágrafo único e subalínea não existem no regime. O backend recusa, no salvamento, o que a hierarquia não permite |
| `CalculadoraDeNumeracaoDosElementos` | número pelo caminho de posições (`1`, `1.1`, `1.1.1.1`), alínea em letra; sem sufixo de letra, sem "único", recalculada livremente até a publicação |
| `RegrasDeCriacaoDoDocumento` (nova) | pede identificação (texto livre) e assunto; não usa assunto básico nem sequencial gerado |
| `EstruturaInicialDeNovoDocumento` | os três capítulos e as seções fixas acima |
| `CamposEspecificosDaEspecie` (nova) | setor emissor, identificação, datas, assunto, assinaturas (lista de blocos de texto livre), "Local, data" — numa estrutura 1:1 com o documento, não em colunas novas de `Documento` |
| `RotuloDosAnexos` | letras (A, B, C…); a lista do cabeçalho vem dos anexos |
| `RegrasDoCicloDeVidaDoDocumento` | rascunho → minuta → revisão → publicação (Boletim Interno) → revogação; sem `INICIAR_ALTERACAO`/`CANCELAR_ALTERACAO`, sem ciclo de emenda |
| `LeiauteDoPdf` / `LeiauteDoHtml` | moldura em todas as páginas, tabela de cabeçalho, "n/total"; e um componente de prévia próprio no frontend |

O que já existe e a NPA reaproveita: permissões e posse, revisão/publicação por atribuição pessoal, versões vigente/em tramitação, busca textual, limite de gerações de PDF, marcação vermelha do texto de orientação. **Distribuição sempre ostensiva** → toda NPA é visível para todas as OMs, sem regra de restrição.

### Etapas

1. **Refatoração das interfaces (concluída para 6 interfaces):** regime, registro e as interfaces de numeração, estrutura inicial, rótulo dos anexos, layouts PDF/HTML e ciclo de vida, com o regime `ATO_NORMATIVO` envolvendo o código existente e os testes atuais como rede de segurança. Faltam: **hierarquia**, **criação** (identificação/assunto), **campos específicos** e o **espelho no frontend** (`perfis/…`, com fixtures de teste compartilhadas com o backend para numeração e hierarquia não divergirem).
2. **Gramática, numeração e modelo da NPA** (backend e frontend), com testes; menu "adicionar elemento" guiado pela hierarquia (parágrafo ou seção/subseção; alínea só sob parágrafo).
3. **Campos do cabeçalho e assinaturas:** persistência, formulário de criação e edição.
4. **Exportação:** PDF (moldura como borda do corpo, tabela de cabeçalho, "n/total" por citação de última página), HTML e prévia, com teste de layout pelo FOP.
5. **Ciclo de publicação por Boletim Interno** (nº e data preenchem EFETIVAÇÃO) **e revogação**; generalizar o conceito de "situação BCA" para "publicação oficial" com o veículo definido pelo regime; ação "Substituir" (cria a nova NPA e revoga a anterior).
6. **Biblioteca e reaproveitamento:** seletor **Atos normativos | NPA** na homepage; dentro da NPA, **Da minha OM**, **Biblioteca de outras OMs** (filtros por OM e setor, com o DOM no cartão) e **Revogadas**; "Usar como base" clona a NPA para a OM de quem clicou, registrando de qual veio; catálogo de setores compartilhado; o **DOM** de cada OM.

**Em aberto:** termos oficiais para o glossário (`docs/dominio.md`); formato do DOM (imagem por OM, tamanho e posição); catálogo de setores.

## Longo prazo — plataforma normativa

- **Portal público de consulta** — versão somente leitura do acervo publicado, com URLs permanentes por dispositivo (`/ica-5-3/art-12`), à semelhança do Planalto e do LexML.
- **Consolidação automática** — geração da versão vigente de um ato a partir da aplicação sucessiva de todas as suas alterações.
- **API pública e interoperabilidade** — adoção do padrão **LexML/Akoma Ntoso** para intercâmbio de atos normativos com outros órgãos da Administração.
- **Assistente de escrita por IA, on-premise** — modelo de linguagem hospedado na própria infraestrutura do COMAER (sem enviar texto normativo a serviços externos), integrado ao editor para sugestão de redação conforme a técnica legislativa, detecção de ambiguidades e contradições, verificação de conformidade com a LC 95/1998 e o Decreto 12.002/2024, e resumo automático de ementas.
- **Aplicativo móvel e modo offline** — consulta ao acervo publicado em campo, sem conectividade.
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.
- **Alta disponibilidade e object storage distribuído** — implantação em Kubernetes com réplicas do backend e PostgreSQL em cluster. Para o object storage, a escolha depende da infraestrutura disponível: **MinIO distribuído** (modo erasure-coded, mais simples de operar) se a equipe de TI for enxuta, ou **Ceph com RADOS Gateway** (mantido pela Red Hat/IBM, licença LGPL v2.1, sem restrições de uso corporativo, e padrão em ambientes governamentais e de defesa) se houver capacidade operacional para um cluster dedicado. Ambos expõem a mesma API S3 que o sistema já usa — a migração é predominantemente de infraestrutura, sem alteração de código: basta atualizar as variáveis de ambiente `MINIO_ENDPOINT`, `MINIO_PUBLIC_URL`, `MINIO_ACCESS_KEY` e `MINIO_SECRET_KEY`, executar uma sincronização dos objetos existentes (ex: `rclone sync`) e aplicar uma migration Flyway para atualizar as URLs armazenadas nas colunas `url_pdf`, `url_html` e `tx_url_portaria_pdf` da tabela `t_documento`.
