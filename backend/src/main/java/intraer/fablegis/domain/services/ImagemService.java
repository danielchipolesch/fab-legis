package intraer.fablegis.domain.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Todo acesso do sistema ao armazenamento de objetos (API S3; hoje o SeaweedFS) passa por aqui -- imagens, anexos, distintivos
// das OMs, PDFs e HTML armazenados. O nome vem da primeira função; hoje a classe é o serviço de armazenamento como um todo.
@Service
@RequiredArgsConstructor
public class ImagemService {

    private static final Logger log = LoggerFactory.getLogger(ImagemService.class);

    // Validade da URL assinada devolvida ao navegador -- longa o bastante para não
    // expirar no meio de uma sessão de leitura/edição normal, curta o bastante para
    // que um link vazado não fique acessível indefinidamente (ver bucket privado
    // abaixo).
    private static final int EXPIRY_MINUTES = 60;

    private final S3Client s3Client;

    // Usado só pra assinar URLs -- ver ArmazenamentoConfig.s3Presigner().
    private final S3Presigner s3Presigner;

    @Value("${armazenamento.bucket}")
    private String bucket;

    @Value("${armazenamento.public-url}")
    private String publicUrl;

    // Cria o bucket já no boot (e não só no primeiro upload): o cache de PDF/HTML (DocumentoRenderCacheService) grava sem passar
    // pelos uploads. Se o servidor de armazenamento ainda não estiver de pé, o boot não cai -- o próximo upload tenta de novo.
    @PostConstruct
    void prepararBucketNoBoot() {
        try {
            garantirBucket();
        } catch (Exception e) {
            log.warn("Não foi possível preparar o bucket de armazenamento no boot; será tentado de novo no próximo upload.", e);
        }
    }

    public String uploadImagem(MultipartFile arquivo) throws Exception {
        garantirBucket();

        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID() + extensao;

        try (InputStream conteudo = arquivo.getInputStream()) {
            s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(nomeArquivo).contentType(arquivo.getContentType()).build(),
                RequestBody.fromInputStream(conteudo, arquivo.getSize())
            );
        }

        return publicUrl + "/" + bucket + "/" + nomeArquivo;
    }

    public String uploadPdf(byte[] pdfBytes, String filename) throws Exception {
        garantirBucket();
        String key = "pdf/" + filename;
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").build(),
            RequestBody.fromBytes(pdfBytes)
        );
        return publicUrl + "/" + bucket + "/" + key;
    }

    // Mesmo padrão de uploadPdf -- ver DocumentoHtmlService, que gera/armazena/serve o
    // HTML pelas mesmas transições de status que o PDF (DocumentoStatusService).
    public String uploadHtml(byte[] htmlBytes, String filename) throws Exception {
        garantirBucket();
        String key = "html/" + filename;
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).contentType("text/html; charset=UTF-8").build(),
            RequestBody.fromBytes(htmlBytes)
        );
        return publicUrl + "/" + bucket + "/" + key;
    }

    // Bucket privado -- o navegador nunca acessa o armazenamento diretamente; toda leitura
    // passa por uma URL assinada de curta duração (ver gerarUrlAssinada), emitida só
    // por quem já está autenticado no /v1/**. A política é removida incondicionalmente
    // (não só na criação): um bucket criado antes desta mudança pode ter ficado com a
    // política pública de uma versão anterior do código, e isso não se autocorrige
    // sozinho só porque o bucket já existe. (Alguns servidores respondem erro quando não há
    // política a remover -- por isso a falha é ignorada.)
    void garantirBucket() {
        boolean existe;
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            existe = true;
        } catch (S3Exception e) {
            if (e.statusCode() != 404) throw e;
            existe = false;
        }
        if (!existe) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        try {
            s3Client.deleteBucketPolicy(DeleteBucketPolicyRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            // Já não há política nenhuma -- nada a remover, tudo certo.
        }
    }

    /**
     * Gera uma URL assinada (válida por {@link #EXPIRY_MINUTES}) para um objeto já
     * armazenado, a partir da URL "canônica" devolvida no upload (ex.:
     * http://localhost:9000/bucket/uuid.png). Usa o assinador do endereço público (não o
     * cliente interno) porque a assinatura AWS SigV4 inclui o header Host — assinar
     * com o endereço interno e trocar o host depois invalidaria a assinatura.
     *
     * @return a URL assinada, ou null se {@code urlArmazenada} não pertencer a este
     *         bucket. (Não verifica se o objeto existe: o navegador recebe 404 ao baixar.)
     */
    public String gerarUrlAssinada(String urlArmazenada) {
        String objectKey = extrairObjectKey(urlArmazenada);
        if (objectKey == null) return null;
        try {
            return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(EXPIRY_MINUTES))
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                    .build()
            ).url().toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Mesma coisa que {@link #gerarUrlAssinada}, para várias URLs de uma vez (evita
     * um round-trip por imagem). URLs inválidas ou de outro bucket são simplesmente
     * omitidas do mapa devolvido -- quem chama decide o que fazer com uma URL sem
     * correspondência. */
    public Map<String, String> gerarUrlsAssinadas(List<String> urls) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (urls == null) return resultado;
        for (String url : urls) {
            String assinada = gerarUrlAssinada(url);
            if (assinada != null) resultado.put(url, assinada);
        }
        return resultado;
    }

    /** Apaga um objeto armazenado (ex.: o distintivo antigo de uma OM). Falha em silêncio: um objeto órfão no bucket é inofensivo. */
    public void remover(String urlArmazenada) {
        String objectKey = extrairObjectKey(urlArmazenada);
        if (objectKey == null) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (Exception e) {
            // nada a fazer
        }
    }

    private String extrairObjectKey(String urlArmazenada) {
        if (urlArmazenada == null || urlArmazenada.isBlank()) return null;
        String prefix = publicUrl + "/" + bucket + "/";
        if (!urlArmazenada.startsWith(prefix)) return null;
        return urlArmazenada.substring(prefix.length());
    }

    /**
     * Busca uma imagem armazenada e retorna como data URI base64.
     * Usado pelo gerador de PDF para embutir imagens sem depender de acesso HTTP público.
     *
     * @param url URL pública da imagem (ex: http://localhost:9000/bucket/uuid.png)
     * @return data URI (data:image/png;base64,...) ou null se não for uma URL deste armazenamento ou a busca falhar
     */
    public String getImageAsDataUri(String url) {
        String objectKey = extrairObjectKey(url);
        if (objectKey == null) return null;
        try {
            byte[] bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(objectKey).build()).asByteArray();
            String mime = guessMimeFromKey(objectKey);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Busca um objeto armazenado (ex: PDF já renderizado) e retorna seu InputStream
     * cru, sem carregá-lo inteiro em memória. Quem chama é responsável por fechar o stream.
     * Usado para transmitir um PDF já gerado direto para a resposta HTTP (ver
     * DocumentoPdfService.streamPdf) sem materializar o arquivo inteiro no backend.
     *
     * @param url URL pública do objeto (ex: http://localhost:9000/bucket/pdf/arquivo.pdf)
     * @return InputStream do objeto, ou null se não for uma URL deste armazenamento ou a busca falhar
     */
    public InputStream getObjectStream(String url) {
        String objectKey = extrairObjectKey(url);
        if (objectKey == null) return null;
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
        } catch (Exception e) {
            return null;
        }
    }

    private static String guessMimeFromKey(String key) {
        String lc = key.toLowerCase();
        if (lc.endsWith(".png"))                        return "image/png";
        if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) return "image/jpeg";
        if (lc.endsWith(".gif"))                        return "image/gif";
        if (lc.endsWith(".webp"))                       return "image/webp";
        return "image/jpeg";
    }

    private String obterExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) return ".jpg";
        return nomeArquivo.substring(nomeArquivo.lastIndexOf(".")).toLowerCase();
    }
}
