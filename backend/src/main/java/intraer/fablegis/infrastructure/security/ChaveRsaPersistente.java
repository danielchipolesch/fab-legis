package intraer.fablegis.infrastructure.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

// O par de chaves RSA com que o Authorization Server assina os access tokens (JWT).
//
//  - Com um arquivo (app.oauth2.chave-privada) que EXISTE: é a chave (PEM PKCS#8: "-----BEGIN PRIVATE KEY-----"), colocada
//    ali de propósito por quem administra o ambiente. Reiniciar o backend, atualizar a imagem ou recriar o container não
//    derruba mais as sessões, porque a chave é a mesma e o kid (impressão digital dela) também.
//  - Sem o arquivo (não configurado, ou configurado mas ausente): chave EFÊMERA, em memória -- cada boot gera outra e invalida
//    os tokens anteriores. É o normal em desenvolvimento. O sistema NUNCA grava chave nenhuma em disco por conta própria: a
//    chave persistente é sempre um artefato deliberado do ambiente. Quem exige que ela exista (produção) liga
//    app.oauth2.chave-privada-obrigatoria: com o arquivo ausente, o boot falha em vez de cair em silêncio na efêmera.
//
// Não é um certificado nem usa TLS: é só o par com que se assinam e se verificam os tokens. A chave pública sai no JWKS
// (/oauth2/jwks); a privada nunca sai do servidor -- por isso NÃO vai dentro da imagem do Docker (quem tivesse a imagem a
// teria, e todas as instalações dividiriam a mesma chave).
public final class ChaveRsaPersistente {

    private static final Logger log = LoggerFactory.getLogger(ChaveRsaPersistente.class);

    static final int TAMANHO_MINIMO_EM_BITS = 2048;
    private static final String CABECALHO = "-----BEGIN PRIVATE KEY-----";
    private static final String RODAPE = "-----END PRIVATE KEY-----";

    private ChaveRsaPersistente() {}

    public static RSAKey carregarOuGerar(String caminho, boolean obrigatoria) {
        if (caminho != null && !caminho.isBlank()) {
            Path arquivo = Path.of(caminho);
            if (Files.isRegularFile(arquivo)) {
                RSAKey chave = paraJwk(ler(arquivo));
                log.info("Authorization Server: chave RSA persistente carregada de {} (kid {}).", arquivo, chave.getKeyID());
                return chave;
            }
            if (obrigatoria) {
                throw new IllegalStateException("A chave RSA de assinatura dos tokens é obrigatória (app.oauth2.chave-privada-obrigatoria)"
                        + " mas não existe em " + arquivo + ". Gere uma e coloque ali -- ver \"Chave de assinatura\" no README.md.");
            }
        } else if (obrigatoria) {
            throw new IllegalStateException("A chave RSA de assinatura dos tokens é obrigatória (app.oauth2.chave-privada-obrigatoria)"
                    + " mas app.oauth2.chave-privada não aponta para nenhum arquivo.");
        }
        log.warn("Authorization Server: sem chave RSA persistente{} -- par de chaves EFÊMERO (em memória); tokens emitidos antes de um "
                + "restart deixam de ser válidos depois dele (todos precisam entrar de novo).",
                caminho == null || caminho.isBlank() ? "" : " em " + caminho);
        return paraJwk(gerar());
    }

    private static KeyPair gerar() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(TAMANHO_MINIMO_EM_BITS);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar o par de chaves RSA do Authorization Server.", e);
        }
    }

    private static KeyPair ler(Path arquivo) {
        try {
            String pem = Files.readString(arquivo, StandardCharsets.US_ASCII).trim();
            if (!pem.startsWith(CABECALHO)) {
                throw new IllegalStateException("A chave em " + arquivo + " não está em PEM PKCS#8 (deve começar com \""
                        + CABECALHO + "\"). Se começa com \"BEGIN RSA PRIVATE KEY\", converta: "
                        + "openssl pkcs8 -topk8 -nocrypt -in chave.pem -out jwt-rsa.pem");
            }
            byte[] der = Base64.getMimeDecoder().decode(pem.replace(CABECALHO, "").replace(RODAPE, "").replaceAll("\\s", ""));
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            if (!(fabrica.generatePrivate(new PKCS8EncodedKeySpec(der)) instanceof RSAPrivateCrtKey privada)) {
                throw new IllegalStateException("A chave em " + arquivo + " não é uma chave privada RSA completa.");
            }
            if (privada.getModulus().bitLength() < TAMANHO_MINIMO_EM_BITS) {
                throw new IllegalStateException("A chave em " + arquivo + " tem " + privada.getModulus().bitLength()
                        + " bits; o mínimo aceito é " + TAMANHO_MINIMO_EM_BITS + ".");
            }
            // A pública se deriva da privada (módulo + expoente público): o arquivo só precisa ter a privada.
            var publica = (RSAPublicKey) fabrica.generatePublic(new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new KeyPair(publica, privada);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler a chave RSA de " + arquivo + ": " + e.getMessage(), e);
        }
    }

    // kid = impressão digital (SHA-256) da chave pública: o mesmo arquivo dá sempre o mesmo kid, então os tokens já emitidos
    // continuam verificáveis depois de um restart (o JWKS publica o mesmo kid).
    private static RSAKey paraJwk(KeyPair par) {
        try {
            RSAKey semKid = new RSAKey.Builder((RSAPublicKey) par.getPublic()).privateKey(par.getPrivate()).build();
            return new RSAKey.Builder(semKid).keyID(semKid.computeThumbprint().toString()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao montar a chave RSA do Authorization Server.", e);
        }
    }
}
