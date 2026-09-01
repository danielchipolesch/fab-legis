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

Regras de numeração automática aplicadas pelo editor estão detalhadas em [Modelo de Domínio](dominio.md#numeracao-automatica-conforme-a-tecnica-legislativa).

## Figuras com numeração sequencial

Extensão TipTap customizada (`extensions/figure.js`) com *NodeView* em Vue: upload da imagem para o MinIO, título, legenda e linha "Fonte:" embutida no HTML para garantir portabilidade na exportação. A numeração ("Figura 1", "Figura 2"…) usa CSS counters no preview web e é resolvida literalmente na exportação, de modo que o mesmo HTML gera PDF e DOCX corretos. O sumário inclui automaticamente a **Lista de Figuras**.

## Exportação de documentos

Geração de PDF **server-side** via **Apache FOP 2.10 / XSL-FO**. Detalhamento completo do pipeline, das regras da NSCA 5-3 e da configuração de fontes em [Geração de PDF e Portarias](exportacao-pdf.md).

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
| **Anexos** | Upload/listagem/remoção de arquivos vinculados ao documento (`AnexoController`), incluídos como páginas próprias na exportação em PDF |
| **Histórico de Versões** | Acesso direto à página de comparação de versões |

Ações disponíveis na topbar: baixar PDF (rascunho gerado sob demanda), ver texto sugerido da portaria (quando `ALTERADO`), clonar e navegar para a comparação de versões.

## Gestão do acervo

A `HomePage` organiza o acervo em três abas — **Meus Documentos**, **Documentos da Minha OM** e **Documentos de Outras OMs** — cada uma com paginação e busca próprias e um contador de quantos documentos ela contém; a visualização em si é universal (qualquer usuário autenticado vê e baixa qualquer documento, independente de autoria, OM ou situação — ver [Autenticação e Colaboração](autenticacao.md)), as abas são só uma forma de navegar esse mesmo conjunto.

Dentro de cada aba: visão em tabela (ordenada por data de criação decrescente por padrão) ou cards, filtros por espécie, situação e busca textual, resumo quantitativo por situação e ações contextuais — editar (Rascunho/Minuta/Em Alteração), clonar, comparar (habilitado só quando o documento tem emendas registradas), exportar em PDF e transicionar de situação (algumas transições exigem o papel Aprovador).

**Excluir** só aparece no menu de ações para documentos próprios (ou compartilhados) em Rascunho ou Minuta — outras combinações já são bloqueadas no backend (`DocumentoAcessoService.podeExcluir()`), e a opção nem é oferecida na interface.
