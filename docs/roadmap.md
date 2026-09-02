# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — a fase 0 (login próprio + JWT, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhada para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.
- **Versionamento por snapshot** — `EmendaHistorico` já registra o quê mudou em cada ciclo de emenda (texto anterior/novo, justificativa, ciclo de publicação), mas não guarda uma foto completa da árvore do documento em cada publicação; um snapshot imutável por ciclo daria ao `DiffViewer` comparações de estrutura inteira, não só por elemento.
- **Cobertura de testes** — testes unitários dos serviços de domínio (com destaque para a numeração e as transições de status) e testes de integração dos controllers com Testcontainers.
- **Exportação DOCX nativa** — o HTML portável produzido pelo editor já foi pensado para isso; falta o conversor no backend.
- **Backend como fonte única da numeração/ordenação** — hoje `numbering.js` (frontend) e a classe `Numbering` de `DocumentoFoBuilder.java` (backend) implementam a mesma regra em paralelo, mantidas manualmente em sincronia; um endpoint que devolva a numeração já calculada eliminaria esse risco de divergência.
- **Notificação de sessão expirada via SSE** — a conexão de notificações (`EventSource`) reconecta silenciosamente em caso de erro, sem avisar a store de autenticação; hoje isso é aceito como comportamento reativo por design (ver nota em [Autenticação e Colaboração](autenticacao.md)), mas poderia ganhar um handler de erro que force um refresh de token ou logout quando apropriado.
- **Dois novos status intermediários — `EM_REVISAO` e `EM_PUBLICACAO`** — hoje `DocumentoStatusEnum`/`DocumentoStatusService` só enxergam o documento como "aprovado" ou "não aprovado", "publicado" ou "não publicado" (transições atuais: `MINUTA ← RASCUNHO/APROVADO`, `APROVADO ← RASCUNHO/MINUTA`, `PUBLICADO ← APROVADO/ALTERADO`, `EM_ALTERACAO ← PUBLICADO/ALTERADO`, `ALTERADO ← EM_ALTERACAO`), sem diferenciar quem está com o documento em mãos nesse meio-tempo nem por quanto tempo. Os dois novos status se encaixam como etapas intermediárias explícitas:
  - `EM_REVISAO` — entre `MINUTA` e `APROVADO` (fluxo normal) e entre `EM_ALTERACAO` e `ALTERADO` (ciclo de emenda): o Aprovador está revisando o conteúdo antes de aprovar.
  - `EM_PUBLICACAO` — entre `APROVADO`/`ALTERADO` e `PUBLICADO`: o documento já foi aprovado, mas a publicação formal (Portaria/BCA) ainda está em curso.

  O objetivo é permitir listar/filtrar especificamente os documentos parados nessas duas situações — hoje eles ficam misturados em `APROVADO`/`ALTERADO` sem distinção de "já em processamento" vs. "esperando alguém pegar" — e abrir uma **tela de controle** dedicada (à parte da `HomePage` atual) onde o Aprovador seleciona um documento da lista, abre, revisa ou publica individualmente; nessa tela, diferente do editor normal (só RASCUNHO/MINUTA/EM_ALTERACAO editáveis hoje — ver `isReadonly` em `DocumentoEditorPage.vue`), **o Aprovador também poderia editar o documento** enquanto ele estiver `EM_REVISAO`/`EM_PUBLICACAO`, o que exige revisitar essa regra de edição.

  Precisa de um **workflow explícito** para mover um documento entre esses status (verbos como "impulsionar" ou "movimentar" um documento para a próxima etapa, não só a troca de status genérica que já existe em `PATCH /{id}/status`), com autorização própria (hoje `podeAprovarNaOm`/`podeMudarStatus` em `DocumentoAcessoService` cobre aprovar/publicar, mas não um estado intermediário assumido individualmente por um Aprovador específico).

## Médio prazo — fluxo de trabalho completo

- **Workflow de tramitação** — encaminhamento entre setores, fila de revisão, comentários em linha e aprovação eletrônica, transformando o sistema de editor em plataforma de processo.
- **Assinatura digital ICP-Brasil** — assinatura do PDF da portaria e/ou do documento final com carimbo de tempo, conferindo validade jurídica ao ato publicado. A decisão de manter a portaria como arquivo separado (não mesclado ao PDF do documento — ver [Portaria, BCA e registro de publicações](ciclo-de-vida.md#portaria-bca-e-registro-de-publicacoes)) foi tomada justamente para preservar essa possibilidade: uma assinatura cobre um intervalo de bytes exato do arquivo original.
- **Busca full-text no acervo** — indexação do conteúdo dos artigos com PostgreSQL `tsvector` ou Elasticsearch, permitindo localizar dispositivos por texto e não apenas por metadados.
- **Grafo de referências normativas** — mapear quais atos alteram, revogam ou citam quais outros, e alertar automaticamente quando um ato referenciado for revogado.
- **Modelos (templates) por espécie** — estruturas pré-montadas de ICA, NSCA, MCA etc., reduzindo o esforço de partida de cada novo documento.
- **Importação de atos legados** — ingestão de documentos existentes em DOCX/PDF com reconhecimento automático da estrutura hierárquica.

## Longo prazo — plataforma normativa

- **Portal público de consulta** — versão somente leitura do acervo publicado, com URLs permanentes por dispositivo (`/ica-5-3/art-12`), à semelhança do Planalto e do LexML.
- **Consolidação automática** — geração da versão vigente de um ato a partir da aplicação sucessiva de todas as suas alterações.
- **API pública e interoperabilidade** — adoção do padrão **LexML/Akoma Ntoso** para intercâmbio de atos normativos com outros órgãos da Administração.
- **Assistente de escrita por IA, on-premise** — modelo de linguagem hospedado na própria infraestrutura do COMAER (sem enviar texto normativo a serviços externos), integrado ao editor para sugestão de redação conforme a técnica legislativa, detecção de ambiguidades e contradições, verificação de conformidade com a LC 95/1998 e o Decreto 12.002/2024, e resumo automático de ementas.
- **Aplicativo móvel e modo offline** — consulta ao acervo publicado em campo, sem conectividade.
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.
- **Alta disponibilidade** — implantação em Kubernetes com réplicas do backend, MinIO distribuído e PostgreSQL em cluster.
