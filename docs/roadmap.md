# Roadmap

O FAB Legis foi desenhado para crescer. As linhas de evolução abaixo estão organizadas por horizonte e refletem o que a arquitetura atual já prepara.

## Curto prazo — consolidar o núcleo

- **Migração para Keycloak/SSO** — o Authorization Server embutido (Spring Authorization Server, OAuth2 + PKCE, ver [Autenticação e Colaboração](autenticacao.md)) foi desenhado para essa troca ser só de emissor de token: as *claims* já espelham as do Keycloak, e `DocumentoAcessoService` não referencia nada do mecanismo de autenticação em si.

## Médio prazo — fluxo de trabalho completo

- **Assinatura digital** — a assinatura digital dos documentos será implementada no prazo que lhe é devido.
- **Versão nas imagens Docker** — hoje as imagens do backend e do frontend são construídas sem tag de versão. A versão do sistema já é automatizada (SemVer + release-please, ver [Testes](testes.md#versionamento)); falta publicar as imagens com a tag da versão (`fab-legis-backend:0.3.0`) e uma tag de pré-versão (`0.3.0-dev.N`) a cada merge na `desenvolvimento`, para que a produção aponte só para uma tag estável (nunca `latest`) e dê para voltar a uma versão anterior.
- **Anexos com parte textual** — hoje os anexos além do ANEXO I (sumário + corpo normativo) são só de imagem, e já têm a regra "**Continuação do ANEXO X**" da segunda página em diante (`DocumentoFoFrontMatterBuilder.buildContinuacaoAnexo`, master `a4-anexo` — ver [Exportação](exportacao-pdf.md)). Quando houver anexos com texto, bastará usar o mesmo layout de página na sequência de páginas de cada anexo de texto, com o próprio rótulo. O ANEXO I continua sem esse cabeçalho, por decisão explícita.
- **Grafo de referências normativas** — mapear quais atos alteram, revogam ou citam quais outros, e alertar automaticamente quando um ato referenciado for revogado.

## Longo prazo — plataforma normativa

- **Portal público de consulta** — versão somente leitura do acervo publicado, com URLs permanentes por dispositivo (`/ica-5-3/art-12`), à semelhança do Planalto e do LexML.
- **Consolidação automática** — geração da versão vigente de um ato a partir da aplicação sucessiva de todas as suas alterações.
- **API pública e interoperabilidade** — adoção do padrão **LexML/Akoma Ntoso** para intercâmbio de atos normativos com outros órgãos da Administração.
- **Assistente de escrita por IA, on-premise** — modelo de linguagem hospedado na própria infraestrutura do COMAER (sem enviar texto normativo a serviços externos), integrado ao editor para sugestão de redação conforme a técnica legislativa, detecção de ambiguidades e contradições, verificação de conformidade com a LC 95/1998 e o Decreto 12.002/2024, e resumo automático de ementas.
- **Backend com várias réplicas** — o login em andamento (Authorization Server) e as conexões em tempo real vivem na memória de uma instância, e a numeração dos documentos tem uma corrida; ver o roteiro em [Réplicas e escalabilidade](replicas.md).
- **Painel analítico** — indicadores de tempo médio de tramitação, gargalos por setor, volume por espécie e assunto, apoiando a gestão da atividade normativa.

## Contingência — se o MinIO deixar de servir

- **Substituto do MinIO** — não é uma funcionalidade planejada, só o plano B caso a perda do suporte open-source do MinIO passe a afetar o projeto: outro object storage S3-compatível de código aberto, como **Garage**, **RustFS** ou **SeaweedFS**. Como a API S3 é a mesma, a troca é de infraestrutura, sem alterar código: atualizar as variáveis `MINIO_*`, sincronizar os objetos existentes (ex.: `rclone sync`) e migrar as URLs armazenadas.
