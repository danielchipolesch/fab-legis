package intraer.fablegis.domain.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// O armazenamento de objetos guarda registro oficial (PDF da portaria, imagens, anexos, distintivos): o bucket é sempre
// privado, e o que o banco grava é a URL "canônica" (endereço público/bucket/chave), da qual o sistema extrai a chave.
// (docs/exportacao-pdf.md e README -- "Imagens e arquivos no armazenamento")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImagemServiceTest {

    private static final String PUBLICA = "https://s3.exemplo.mil.br";
    private static final String BUCKET = "fab-legis-teste";

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;

    ImagemService service;

    @BeforeEach
    void setUp() {
        service = new ImagemService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(service, "bucket", BUCKET);
        ReflectionTestUtils.setField(service, "publicUrl", PUBLICA);
    }

    private static S3Exception erro(int status) {
        return (S3Exception) S3Exception.builder().statusCode(status).message("erro " + status).build();
    }

    // ── Bucket ────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void criaOBucketQuandoEleAindaNaoExiste() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().statusCode(404).build());

        service.garantirBucket();

        ArgumentCaptor<CreateBucketRequest> captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
    }

    @Test
    void naoRecriaOBucketQueJaExiste() {
        service.garantirBucket();

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void erroDiferenteDeNaoEncontradoAoVerificarOBucketNaoVirouCriacao() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(erro(403));

        assertThatThrownBy(() -> service.garantirBucket()).isInstanceOf(S3Exception.class);
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void removeSempreAPoliticaDoBucketParaGarantirQueEleSejaPrivado() {
        service.garantirBucket();

        ArgumentCaptor<DeleteBucketPolicyRequest> captor = ArgumentCaptor.forClass(DeleteBucketPolicyRequest.class);
        verify(s3Client).deleteBucketPolicy(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
    }

    @Test
    void falhaAoRemoverAPoliticaNaoImpedeOUso() {
        when(s3Client.deleteBucketPolicy(any(DeleteBucketPolicyRequest.class))).thenThrow(erro(501));

        assertThatCode(() -> service.garantirBucket()).doesNotThrowAnyException();
    }

    @Test
    void bucketIndisponivelNoBootNaoDerrubaAAplicacao() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(new RuntimeException("armazenamento fora do ar"));

        assertThatCode(() -> service.prepararBucketNoBoot()).doesNotThrowAnyException();
    }

    // ── Upload: a URL canônica devolvida é endereço público/bucket/chave ─────────────────────────────────

    @Test
    void uploadDePdfGuardaSobPdfEDevolveAUrlCanonica() throws Exception {
        String url = service.uploadPdf("pdf".getBytes(), "doc-1.pdf");

        assertThat(url).isEqualTo(PUBLICA + "/" + BUCKET + "/pdf/doc-1.pdf");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo("pdf/doc-1.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void uploadDeHtmlGuardaSobHtmlEmUtf8() throws Exception {
        String url = service.uploadHtml("<p>olá</p>".getBytes(), "doc-1.html");

        assertThat(url).isEqualTo(PUBLICA + "/" + BUCKET + "/html/doc-1.html");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().contentType()).isEqualTo("text/html; charset=UTF-8");
    }

    // ── URL assinada ──────────────────────────────────────────────────────────────────────────────────────

    @Test
    void geraUrlAssinadaDe60MinutosParaUmObjetoDoBucket() throws Exception {
        PresignedGetObjectRequest assinada = mock(PresignedGetObjectRequest.class);
        when(assinada.url()).thenReturn(URI.create(PUBLICA + "/" + BUCKET + "/a.png?X-Amz-Signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(assinada);

        String resultado = service.gerarUrlAssinada(PUBLICA + "/" + BUCKET + "/a.png");

        assertThat(resultado).isEqualTo(PUBLICA + "/" + BUCKET + "/a.png?X-Amz-Signature=abc");
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).hasMinutes(60);
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("a.png");
    }

    @Test
    void naoAssinaUrlDeOutroBucketNemDeOutroEnderecoNemVazia() {
        assertThat(service.gerarUrlAssinada(PUBLICA + "/outro-bucket/a.png")).isNull();
        assertThat(service.gerarUrlAssinada("https://externo.exemplo.com/" + BUCKET + "/a.png")).isNull();
        assertThat(service.gerarUrlAssinada(null)).isNull();
        assertThat(service.gerarUrlAssinada("  ")).isNull();
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void urlsAssinadasEmLoteOmitemAsQueNaoSaoDesteArmazenamento() throws Exception {
        PresignedGetObjectRequest assinada = mock(PresignedGetObjectRequest.class);
        when(assinada.url()).thenReturn(URI.create("https://s3.exemplo.mil.br/x?sig=1").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(assinada);

        var resultado = service.gerarUrlsAssinadas(List.of(PUBLICA + "/" + BUCKET + "/a.png", "https://outro.com/b.png"));

        assertThat(resultado).containsOnlyKeys(PUBLICA + "/" + BUCKET + "/a.png");
    }

    // ── Leitura pelo backend (embutir no PDF/HTML) ────────────────────────────────────────────────────────

    @Test
    void imagemDoBucketViraDataUriComOTipoCerto() {
        byte[] bytes = {1, 2, 3};
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

        String uri = service.getImageAsDataUri(PUBLICA + "/" + BUCKET + "/foto.png");

        assertThat(uri).isEqualTo("data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
    }

    @Test
    void imagemForaDoBucketOuComFalhaDeLeituraNaoViraDataUri() {
        assertThat(service.getImageAsDataUri("https://externo.exemplo.com/foto.png")).isNull();

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(erro(404));
        assertThat(service.getImageAsDataUri(PUBLICA + "/" + BUCKET + "/some.png")).isNull();
    }

    @Test
    void streamDeObjetoForaDoBucketOuComFalhaDevolveNull() {
        assertThat(service.getObjectStream("https://externo.exemplo.com/a.pdf")).isNull();

        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(erro(404));
        assertThat(service.getObjectStream(PUBLICA + "/" + BUCKET + "/pdf/a.pdf")).isNull();
    }

    // ── Remoção ───────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void removeOObjetoPelaChaveExtraidaDaUrl() {
        service.remover(PUBLICA + "/" + BUCKET + "/distintivo.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("distintivo.png");
    }

    @Test
    void removerNaoApagaNadaDeOutroBucketENuncaLanca() {
        service.remover("https://externo.exemplo.com/distintivo.png");
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(erro(500));
        assertThatCode(() -> service.remover(PUBLICA + "/" + BUCKET + "/x.png")).doesNotThrowAnyException();
    }
}
