package br.com.danielchipolesch.domain.services;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImagemService {

    // Validade da URL assinada devolvida ao navegador -- longa o bastante para não
    // expirar no meio de uma sessão de leitura/edição normal, curta o bastante para
    // que um link vazado não fique acessível indefinidamente (ver bucket privado
    // abaixo).
    private static final int EXPIRY_MINUTES = 60;

    @Autowired
    private MinioClient minioClient;

    // Usado só pra assinar URLs -- ver MinioConfig.minioClientPublico().
    @Autowired
    private MinioClient minioClientPublico;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    public String uploadImagem(MultipartFile arquivo) throws Exception {
        garantirBucket();

        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = UUID.randomUUID() + extensao;

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(nomeArquivo)
                .stream(arquivo.getInputStream(), arquivo.getSize(), -1)
                .contentType(arquivo.getContentType())
                .build()
        );

        return publicUrl + "/" + bucket + "/" + nomeArquivo;
    }

    public String uploadPdf(byte[] pdfBytes, String filename) throws Exception {
        garantirBucket();
        String key = "pdf/" + filename;
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                .contentType("application/pdf")
                .build()
        );
        return publicUrl + "/" + bucket + "/" + key;
    }

    // Bucket privado -- o navegador nunca acessa o MinIO diretamente; toda leitura
    // passa por uma URL assinada de curta duração (ver gerarUrlAssinada), emitida só
    // por quem já está autenticado no /v1/**. A política é removida incondicionalmente
    // (não só na criação): um bucket criado antes desta mudança pode ter ficado com a
    // política pública de uma versão anterior do código, e isso não se autocorrige
    // sozinho só porque o bucket já existe.
    private void garantirBucket() throws Exception {
        boolean existe = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        try {
            minioClient.deleteBucketPolicy(DeleteBucketPolicyArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            // Já não há política nenhuma -- nada a remover, tudo certo.
        }
    }

    /**
     * Gera uma URL assinada (válida por {@link #EXPIRY_MINUTES}) para um objeto já
     * armazenado, a partir da URL "canônica" devolvida no upload (ex.:
     * http://localhost:9000/bucket/uuid.png). Usa {@code minioClientPublico} (não o
     * cliente interno) porque a assinatura AWS SigV4 inclui o header Host — assinar
     * com o endpoint interno e trocar o host depois invalidaria a assinatura.
     *
     * @return a URL assinada, ou null se {@code urlArmazenada} não pertencer a este
     *         bucket ou o objeto não existir.
     */
    public String gerarUrlAssinada(String urlArmazenada) {
        String objectKey = extrairObjectKey(urlArmazenada);
        if (objectKey == null) return null;
        try {
            return minioClientPublico.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /** Mesma coisa que {@link #gerarUrlAssinada}, para várias URLs de uma vez (evita
     * um round-trip por imagem). URLs inválidas ou inexistentes são simplesmente
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

    private String extrairObjectKey(String urlArmazenada) {
        if (urlArmazenada == null || urlArmazenada.isBlank()) return null;
        String prefix = publicUrl + "/" + bucket + "/";
        if (!urlArmazenada.startsWith(prefix)) return null;
        return urlArmazenada.substring(prefix.length());
    }

    /**
     * Busca uma imagem armazenada no MinIO e retorna como data URI base64.
     * Usado pelo gerador de PDF para embutir imagens sem depender de acesso HTTP público.
     *
     * @param url URL pública da imagem (ex: http://localhost:9000/bucket/uuid.png)
     * @return data URI (data:image/png;base64,...) ou null se não for uma URL MinIO reconhecida
     */
    public String getImageAsDataUri(String url) {
        String objectKey = extrairObjectKey(url);
        if (objectKey == null) return null;
        try {
            try (var response = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
                byte[] bytes = response.readAllBytes();
                String mime = guessMimeFromKey(objectKey);
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Busca um objeto armazenado no MinIO (ex: PDF já renderizado) e retorna seu InputStream
     * cru, sem carregá-lo inteiro em memória. Quem chama é responsável por fechar o stream.
     * Usado para transmitir um PDF já gerado direto para a resposta HTTP (ver
     * DocumentoPdfService.streamPdf) sem materializar o arquivo inteiro no backend.
     *
     * @param url URL pública do objeto (ex: http://localhost:9000/bucket/pdf/arquivo.pdf)
     * @return InputStream do objeto, ou null se não for uma URL MinIO reconhecida ou a busca falhar
     */
    public InputStream getObjectStream(String url) {
        String objectKey = extrairObjectKey(url);
        if (objectKey == null) return null;
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
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
