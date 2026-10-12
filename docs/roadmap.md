# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — o Authorization Server embutido (Spring Authorization Server, OAuth2 + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhado para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.
- **Cobertura de testes** — faltam os demais métodos do ciclo de emenda (`emendar`, `incluir`, `reordenarIncluido`, mapa de alteração), o restante da lógica do frontend (ex.: `stores/editor.js`) e testes de integração dos controllers com Testcontainers — estes dependem de o ambiente de CI/CD ter Docker disponível.

## Médio prazo — fluxo de trabalho completo

- **Assinatura digital** — a assinatura digital dos documentos será implementada no prazo que lhe é devido.
- **Anexos com parte textual** — hoje os anexos além do ANEXO I (sumário + corpo normativo) são só de imagem, e já têm a regra "**Continuação do ANEXO X**" da segunda página em diante (`DocumentoFoFrontMatterBuilder.buildContinuacaoAnexo`, master `a4-anexo` — ver [Exportação](exportacao-pdf.md)). Quando houver anexos com texto, bastará usar o mesmo layout de página na sequência de páginas de cada anexo de texto, com o próprio rótulo. O ANEXO I continua sem esse cabeçalho, por decisão explícita.
- **Grafo de referências normativas** — mapear quais atos alteram, revogam ou citam quais outros, e alertar automaticamente quando um ato referenciado for revogado.

## NPA (Norma Padrão de Ação) — o que falta

A NPA já funciona de ponta a ponta — criação, edição, PDF/HTML/prévia, publicação e revogação no Boletim Interno; as regras estão em [Modelo de Domínio](dominio.md#npa-norma-padrao-de-acao). Ficaram de fora, por dependerem de decisão ou de escopo maior:

- **DOM (distintivo da OM):** hoje o cabeçalho reserva o espaço e mostra a identificação abaixo dele. Falta decidir o formato (imagem por OM, tamanho e posição) e o cadastro dessa imagem por OM. **Critério de aceite:** ao clonar uma NPA, o DOM do clone tem de ser o da OM de quem clona, nunca o da OM do documento original — mesma regra que `Documento.om` já segue hoje (`CriacaoDeNpa.novo`, `.om(autor.getOm())`, coberta por `CriacaoDeNpaTest`); se a imagem do DOM vier de uma consulta separada (não diretamente de `Documento.om`), essa consulta precisa usar a OM do documento, não uma referência fixada no momento da clonagem.
- **"Substituir":** para mudar uma NPA publicada cria-se outra e revoga-se a anterior. Hoje isso é feito à mão (clonar e, depois, pedir a revogação da antiga); a ação "Substituir" faria as duas coisas ligadas, registrando qual NPA substitui qual.
- **Glossário:** confirmar os termos oficiais da NPA para `docs/dominio.md`.

## Longo prazo — plataforma normativa

- **Portal público de consulta** — versão somente leitura do acervo publicado, com URLs permanentes por dispositivo (`/ica-5-3/art-12`), à semelhança do Planalto e do LexML.
- **Consolidação automática** — geração da versão vigente de um ato a partir da aplicação sucessiva de todas as suas alterações.
- **API pública e interoperabilidade** — adoção do padrão **LexML/Akoma Ntoso** para intercâmbio de atos normativos com outros órgãos da Administração.
- **Assistente de escrita por IA, on-premise** — modelo de linguagem hospedado na própria infraestrutura do COMAER (sem enviar texto normativo a serviços externos), integrado ao editor para sugestão de redação conforme a técnica legislativa, detecção de ambiguidades e contradições, verificação de conformidade com a LC 95/1998 e o Decreto 12.002/2024, e resumo automático de ementas.
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.

## Contingência — se o MinIO deixar de servir

- **Substituto do MinIO (object storage S3-compatível)** — não é uma funcionalidade planejada, só o plano B caso a perda do suporte open-source do MinIO passe a afetar o projeto. As opções são **Ceph com RADOS Gateway** (mantido pela Red Hat/IBM, licença LGPL v2.1, sem restrições de uso corporativo e comum em ambientes governamentais e de defesa, mas exige capacidade operacional para um cluster dedicado) ou um **MinIO distribuído** (modo erasure-coded, mais simples de operar). Ambos expõem a mesma API S3 que o sistema já usa, então a troca é predominantemente de infraestrutura, sem alteração de código: atualizar as variáveis de ambiente `MINIO_ENDPOINT`, `MINIO_PUBLIC_URL`, `MINIO_ACCESS_KEY` e `MINIO_SECRET_KEY`, sincronizar os objetos existentes (ex.: `rclone sync`) e aplicar uma migration Flyway que atualize as URLs armazenadas em `url_pdf`, `url_html` e `tx_url_portaria_pdf` da tabela `t_documento` (e nas demais colunas de URL de objeto, como as da cópia em tramitação).
