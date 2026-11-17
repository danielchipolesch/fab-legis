package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

// Cache, no armazenamento de objetos, do PDF/HTML de um documento AINDA em tramitação (sem cópia definitiva
// armazenada -- ver VersoesDocumento.emTramitacaoArmazenada): hoje DocumentoPdfService.streamPdf e
// DocumentoHtmlService.streamHtml renderizam a cada visualização enquanto o texto ainda pode
// mudar. Reabrir a mesma minuta sem editá-la custa, com este cache, uma ida ao armazenamento em vez de uma
// renderização inteira (FOP, no caso do PDF). Era o item "Cache de PDF em tramitação" do
// roadmap (docs/roadmap.md).
//
// A chave (fingerprint) tem de mudar a cada alteração de conteúdo -- o PATCH de edição
// colaborativa (/elementos/{id}/conteudo) grava só no item (dt_atualizacao próprio) e NÃO toca
// Documento.dtAlteracao nem o @Version (ver DocumentoConcorrenciaService): por isso o fingerprint
// combina Documento.versao/dtAlteracao (mudanças estruturais/metadados) com o carimbo de
// atualização de cada item de conteúdo (mudanças de texto) e os ids dos anexos de imagem
// (adicionar/remover anexo cria/apaga linha, nunca atualiza uma existente -- ver AnexoService).
//
// É estritamente um atalho: qualquer falha de leitura ou escrita no armazenamento só vira um cache miss
// (loga warning), nunca uma falha da exportação -- o chamador sempre pode cair para a renderização
// ao vivo que já existia antes deste cache.
//
// A expiração do cache é feita aqui mesmo (removerExpirados), e não por uma regra de ciclo de vida (lifecycle) do servidor:
// assim não depende de o servidor de armazenamento implementar essa função (nem todo servidor compatível com S3 a tem).
@Service
@RequiredArgsConstructor
public class DocumentoRenderCacheService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoRenderCacheService.class);

    private static final String PREFIXO = "cache/";

    public enum TipoDeRenderizacao {
        PDF("pdf", "pdf"),
        HTML("html", "html");

        final String pasta;
        final String extensao;

        TipoDeRenderizacao(String pasta, String extensao) {
            this.pasta = pasta;
            this.extensao = extensao;
        }
    }

    private final S3Client s3Client;

    private final ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    private final ItemPartePreliminarRepository itemPartePreliminarRepository;

    private final AnexoRepository anexoRepository;

    @Value("${armazenamento.bucket}")
    private String bucket;

    @Value("${app.pdf.cache.ttl-dias:2}")
    private int ttlDias;

    // Todo dia, de madrugada: apaga do cache o que tem mais de ttlDias dias. Cada instância do backend roda o seu -- apagar duas
    // vezes o mesmo objeto é inofensivo.
    @Scheduled(cron = "0 30 3 * * *")
    public void removerExpirados() {
        removerExpiradosAntesDe(Instant.now().minus(ttlDias, ChronoUnit.DAYS));
    }

    // Devolve quantos objetos apagou. Falha em silêncio (só loga): o cache só ocupa espaço, nunca estraga a exportação.
    int removerExpiradosAntesDe(Instant limite) {
        int removidos = 0;
        try {
            var pagina = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).prefix(PREFIXO).build());
            for (S3Object objeto : pagina.contents()) {
                if (objeto.lastModified() != null && objeto.lastModified().isBefore(limite)) {
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objeto.key()).build());
                    removidos++;
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível limpar o cache expirado de PDF/HTML no armazenamento (apagados até a falha: {}).", removidos, e);
        }
        return removidos;
    }

    public Optional<byte[]> buscar(Documento documento, TipoDeRenderizacao tipo) {
        String chave = chaveDoCache(documento, tipo);
        try {
            return Optional.of(s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(chave).build()).asByteArray());
        } catch (Exception e) {
            // Cobre tanto "não encontrado" (cache miss normal) quanto qualquer erro de
            // comunicação com o armazenamento -- em ambos os casos, quem chama deve renderizar ao vivo.
            return Optional.empty();
        }
    }

    public void salvar(Documento documento, TipoDeRenderizacao tipo, byte[] conteudo) {
        String chave = chaveDoCache(documento, tipo);
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(chave)
                            .contentType(tipo == TipoDeRenderizacao.PDF ? "application/pdf" : "text/html; charset=UTF-8")
                            .build(),
                    RequestBody.fromBytes(conteudo));
        } catch (Exception e) {
            // Idem: não guardar no cache não é um erro para quem já tem o PDF/HTML pronto na mão.
            log.warn("Não foi possível gravar o cache de {} do documento {}.", tipo, documento.getId(), e);
        }
    }

    private String chaveDoCache(Documento documento, TipoDeRenderizacao tipo) {
        return PREFIXO + tipo.pasta + "/" + documento.getId() + "/" + fingerprint(documento) + "." + tipo.extensao;
    }

    // SHA-256 sobre tudo que pode mudar o PDF/HTML renderizado: identidade + versão/data de
    // alteração do documento (metadados, estrutura -- ver DocumentoConcorrenciaService), a data de
    // atualização de cada item de conteúdo (preliminar e normativo/anexo -- a edição colaborativa
    // grava só aqui) e os ids dos anexos de imagem (adicionar/remover muda o conjunto; não há
    // edição em lugar de um anexo existente -- ver AnexoService.adicionar/remover).
    private String fingerprint(Documento documento) {
        StringBuilder base = new StringBuilder();
        base.append(documento.getId()).append(':').append(documento.getVersao()).append(':');
        // O DOM (distintivo) da OM entra no PDF/HTML da NPA: trocar a imagem da OM tem de invalidar o cache.
        if (documento.getOm() != null) base.append(documento.getOm().getUrlDistintivo()).append(':');
        base.append(documento.getDtAlteracao() == null ? 0 : documento.getDtAlteracao().getTime());

        itemPartePreliminarRepository.findIdsEDataAtualizacaoByDocumentoId(documento.getId())
                .forEach(linha -> base.append('|').append(linha[0]).append(':').append(carimbo((LocalDateTime) linha[1])));

        itemAnexoParteNormativaRepository.findIdsEDataAtualizacaoByDocumentoId(documento.getId())
                .forEach(linha -> base.append('|').append(linha[0]).append(':').append(carimbo((LocalDateTime) linha[1])));

        anexoRepository.findByDocumentoIdOrderByOrdemAsc(documento.getId())
                .forEach(anexo -> base.append('|').append(anexo.getId()));

        return sha256Hex(base.toString());
    }

    private static long carimbo(LocalDateTime dataHora) {
        return dataHora == null ? 0L : dataHora.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido por toda JVM -- nunca deveria acontecer.
            throw new IllegalStateException(e);
        }
    }
}
