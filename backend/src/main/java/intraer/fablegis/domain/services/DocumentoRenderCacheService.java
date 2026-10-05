package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

// Cache, no próprio MinIO, do PDF/HTML de um documento AINDA em tramitação (sem cópia definitiva
// armazenada -- ver VersoesDocumento.emTramitacaoArmazenada): hoje DocumentoPdfService.streamPdf e
// DocumentoHtmlService.streamHtml renderizam a cada visualização enquanto o texto ainda pode
// mudar. Reabrir a mesma minuta sem editá-la custa, com este cache, uma ida ao MinIO em vez de uma
// renderização inteira (FOP, no caso do PDF). Era o item "Cache de PDF em tramitação no MinIO" do
// roadmap (docs/roadmap.md).
//
// A chave (fingerprint) tem de mudar a cada alteração de conteúdo -- o PATCH de edição
// colaborativa (/elementos/{id}/conteudo) grava só no item (dt_atualizacao próprio) e NÃO toca
// Documento.dtAlteracao nem o @Version (ver DocumentoConcorrenciaService): por isso o fingerprint
// combina Documento.versao/dtAlteracao (mudanças estruturais/metadados) com o carimbo de
// atualização de cada item de conteúdo (mudanças de texto) e os ids dos anexos de imagem
// (adicionar/remover anexo cria/apaga linha, nunca atualiza uma existente -- ver AnexoService).
//
// É estritamente um atalho: qualquer falha de leitura ou escrita no MinIO só vira um cache miss
// (loga warning), nunca uma falha da exportação -- o chamador sempre pode cair para a renderização
// ao vivo que já existia antes deste cache.
@Service
public class DocumentoRenderCacheService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoRenderCacheService.class);

    private static final String PREFIXO = "cache/";
    private static final String LIFECYCLE_RULE_ID = "fab-legis-cache-ttl";

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

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    @Autowired
    private ItemPartePreliminarRepository itemPartePreliminarRepository;

    @Autowired
    private AnexoRepository anexoRepository;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${app.pdf.cache.ttl-dias:2}")
    private int ttlDias;

    // Aplica a regra de expiração (lifecycle) do prefixo cache/ ao subir a aplicação. Idempotente
    // -- setBucketLifecycle substitui a configuração inteira, então repetir isso a cada boot é
    // seguro (não acumula regra).
    @PostConstruct
    void configurarLifecycleDoCache() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            LifecycleRule regra = new LifecycleRule(
                    Status.ENABLED,
                    null,
                    new Expiration((ZonedDateTime) null, ttlDias, null),
                    new RuleFilter(PREFIXO),
                    LIFECYCLE_RULE_ID,
                    null, null, null);
            minioClient.setBucketLifecycle(SetBucketLifecycleArgs.builder()
                    .bucket(bucket)
                    .config(new LifecycleConfiguration(List.of(regra)))
                    .build());
        } catch (Exception e) {
            // Sem a regra de expiração o cache só perde o "flush" automático -- continua
            // funcional (os cache hits/misses não dependem disso), então não derruba o boot.
            log.warn("Não foi possível configurar a expiração automática do cache de PDF/HTML no MinIO.", e);
        }
    }

    public Optional<byte[]> buscar(Documento documento, TipoDeRenderizacao tipo) {
        String chave = chaveDoCache(documento, tipo);
        try (var objeto = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(chave).build())) {
            try (var saida = new ByteArrayOutputStream()) {
                objeto.transferTo(saida);
                return Optional.of(saida.toByteArray());
            }
        } catch (Exception e) {
            // Cobre tanto "não encontrado" (cache miss normal) quanto qualquer erro de
            // comunicação com o MinIO -- em ambos os casos, quem chama deve renderizar ao vivo.
            return Optional.empty();
        }
    }

    public void salvar(Documento documento, TipoDeRenderizacao tipo, byte[] conteudo) {
        String chave = chaveDoCache(documento, tipo);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chave)
                    .stream(new ByteArrayInputStream(conteudo), conteudo.length, -1)
                    .contentType(tipo == TipoDeRenderizacao.PDF ? "application/pdf" : "text/html; charset=UTF-8")
                    .build());
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
