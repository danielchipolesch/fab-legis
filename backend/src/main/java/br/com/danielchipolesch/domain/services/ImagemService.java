package br.com.danielchipolesch.domain.services;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class ImagemService {

    @Autowired
    private MinioClient minioClient;

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

    private void garantirBucket() throws Exception {
        boolean existe = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            String politica = """
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }]
                }
                """.formatted(bucket);
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(politica).build()
            );
        }
    }

    /**
     * Busca uma imagem armazenada no MinIO e retorna como data URI base64.
     * Usado pelo gerador de PDF para embutir imagens sem depender de acesso HTTP público.
     *
     * @param url URL pública da imagem (ex: http://localhost:9000/bucket/uuid.png)
     * @return data URI (data:image/png;base64,...) ou null se não for uma URL MinIO reconhecida
     */
    public String getImageAsDataUri(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String prefix = publicUrl + "/" + bucket + "/";
            if (!url.startsWith(prefix)) return null;
            String objectKey = url.substring(prefix.length());

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
        if (url == null || url.isBlank()) return null;
        try {
            String prefix = publicUrl + "/" + bucket + "/";
            if (!url.startsWith(prefix)) return null;
            String objectKey = url.substring(prefix.length());

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
