package intraer.fablegis.domain.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

// Baixa uma imagem por HTTP e a devolve como data URI -- o último recurso dos geradores de PDF e HTML quando a imagem não é
// do MinIO (que tem leitura própria: ImagemService.getImageAsDataUri) e precisa ir embutida no arquivo. Antes, o
// XslFoContentRenderer e o LeiauteHtmlDeEspecieConvencional tinham, cada um, a sua cópia deste código.
public final class ImagemRemota {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private ImagemRemota() {
    }

    // Devolve o data URI da imagem; se não der para baixá-la (erro, status diferente de 200, não é imagem), devolve a própria
    // URL -- o arquivo sai com a referência original em vez de falhar a exportação inteira.
    public static String comoDataUri(String url) {
        try {
            var pedido = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build();
            var resposta = HTTP_CLIENT.send(pedido, HttpResponse.BodyHandlers.ofByteArray());
            if (resposta.statusCode() != 200) return url;
            String tipo = resposta.headers().firstValue("content-type").orElse(mimePelaExtensao(url)).split(";")[0].trim();
            if (!tipo.startsWith("image/")) return url;
            return "data:" + tipo + ";base64," + Base64.getEncoder().encodeToString(resposta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // quem chamou pediu para parar: mantém o sinal
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    static String mimePelaExtensao(String url) {
        String minuscula = url.toLowerCase(Locale.ROOT);
        if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) return "image/jpeg";
        if (minuscula.endsWith(".gif")) return "image/gif";
        if (minuscula.endsWith(".webp")) return "image/webp";
        if (minuscula.endsWith(".svg")) return "image/svg+xml";
        return "image/png";
    }
}
