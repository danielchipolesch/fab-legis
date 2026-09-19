# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — o Authorization Server embutido (Spring Authorization Server, OAuth2 + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhado para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.
- **Cobertura de testes** — testes unitários dos serviços de domínio (com destaque para a numeração e as transições de status) e testes de integração dos controllers com Testcontainers.
- **Exportação DOCX nativa** — o HTML portável produzido pelo editor já foi pensado para isso; falta o conversor no backend.

## Médio prazo — fluxo de trabalho completo

- **Assinatura digital ICP-Brasil** — assinatura do PDF da portaria e/ou do documento final com carimbo de tempo, conferindo validade jurídica ao ato publicado. A decisão de manter a portaria como arquivo separado (não mesclado ao PDF do documento — ver [Portaria, BCA e registro de publicações](ciclo-de-vida.md#portaria-bca-e-registro-de-publicacoes)) foi tomada justamente para preservar essa possibilidade: uma assinatura cobre um intervalo de bytes exato do arquivo original.
- **Grafo de referências normativas** — mapear quais atos alteram, revogam ou citam quais outros, e alertar automaticamente quando um ato referenciado for revogado.

## Longo prazo — plataforma normativa

- **Portal público de consulta** — versão somente leitura do acervo publicado, com URLs permanentes por dispositivo (`/ica-5-3/art-12`), à semelhança do Planalto e do LexML.
- **Consolidação automática** — geração da versão vigente de um ato a partir da aplicação sucessiva de todas as suas alterações.
- **API pública e interoperabilidade** — adoção do padrão **LexML/Akoma Ntoso** para intercâmbio de atos normativos com outros órgãos da Administração.
- **Assistente de escrita por IA, on-premise** — modelo de linguagem hospedado na própria infraestrutura do COMAER (sem enviar texto normativo a serviços externos), integrado ao editor para sugestão de redação conforme a técnica legislativa, detecção de ambiguidades e contradições, verificação de conformidade com a LC 95/1998 e o Decreto 12.002/2024, e resumo automático de ementas.
- **Aplicativo móvel e modo offline** — consulta ao acervo publicado em campo, sem conectividade.
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.
- **Alta disponibilidade e object storage distribuído** — implantação em Kubernetes com réplicas do backend e PostgreSQL em cluster. Para o object storage, a escolha depende da infraestrutura disponível: **MinIO distribuído** (modo erasure-coded, mais simples de operar) se a equipe de TI for enxuta, ou **Ceph com RADOS Gateway** (mantido pela Red Hat/IBM, licença LGPL v2.1, sem restrições de uso corporativo, e padrão em ambientes governamentais e de defesa) se houver capacidade operacional para um cluster dedicado. Ambos expõem a mesma API S3 que o sistema já usa — a migração é predominantemente de infraestrutura, sem alteração de código: basta atualizar as variáveis de ambiente `MINIO_ENDPOINT`, `MINIO_PUBLIC_URL`, `MINIO_ACCESS_KEY` e `MINIO_SECRET_KEY`, executar uma sincronização dos objetos existentes (ex: `rclone sync`) e aplicar uma migration Flyway para atualizar as URLs armazenadas nas colunas `url_pdf`, `url_html` e `tx_url_portaria_pdf` da tabela `t_documento`.
