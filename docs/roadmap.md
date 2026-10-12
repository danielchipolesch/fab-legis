# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — o Authorization Server embutido (Spring Authorization Server, OAuth2 + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhado para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.

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
- **Backend com várias réplicas** — o login em andamento (Authorization Server) e as conexões em tempo real vivem na memória de uma instância, e a numeração dos documentos tem uma corrida; ver o roteiro em [Réplicas e escalabilidade](replicas.md).
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.

## Contingência — se o MinIO deixar de servir

- **Substituto do MinIO** — não é uma funcionalidade planejada, só o plano B caso a perda do suporte open-source do MinIO passe a afetar o projeto: outro object storage S3-compatível de código aberto, como **Garage**, **RustFS** ou **SeaweedFS**. Como a API S3 é a mesma, a troca é de infraestrutura, sem alterar código: atualizar as variáveis `MINIO_*`, sincronizar os objetos existentes (ex.: `rclone sync`) e migrar as URLs armazenadas.
