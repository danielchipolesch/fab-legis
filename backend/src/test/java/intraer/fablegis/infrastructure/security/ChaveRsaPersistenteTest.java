package intraer.fablegis.infrastructure.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A chave que assina os tokens sobrevive a reinícios (README, "Chave de assinatura"): sem arquivo configurado é efêmera; com
// arquivo, é gerada na primeira vez e reaproveitada nas seguintes -- ou é uma chave externa colocada ali.
class ChaveRsaPersistenteTest {

    @TempDir Path pasta;

    private static String pemPkcs8(KeyPair par) {
        String corpo = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(par.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + corpo + "\n-----END PRIVATE KEY-----\n";
    }

    private static KeyPair novoPar(int bits) throws Exception {
        var g = KeyPairGenerator.getInstance("RSA");
        g.initialize(bits);
        return g.generateKeyPair();
    }

    @Test
    void semArquivoConfiguradoACadaBootGeraUmaChaveDiferente() {
        RSAKey a = ChaveRsaPersistente.carregarOuGerar("", false);
        RSAKey b = ChaveRsaPersistente.carregarOuGerar(null, false);

        assertThat(a.getKeyID()).isNotEqualTo(b.getKeyID());
        assertThat(a.isPrivate()).isTrue();
    }

    @Test
    void comCaminhoMasSemOArquivoAChaveEEfemeraENadaEGravadoEmDisco() throws Exception {
        Path arquivo = pasta.resolve("chaves/jwt-rsa.pem");   // nem a pasta existe

        RSAKey a = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);
        RSAKey b = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);

        assertThat(a.getKeyID()).isNotEqualTo(b.getKeyID());     // cada boot, outra chave
        assertThat(arquivo).doesNotExist();                       // o sistema nunca grava chave por conta própria
        assertThat(pasta.resolve("chaves")).doesNotExist();
    }

    @Test
    void comAChaveObrigatoriaOBootFalhaSeOArquivoNaoExiste() {
        Path ausente = pasta.resolve("jwt-rsa.pem");

        assertThatThrownBy(() -> ChaveRsaPersistente.carregarOuGerar(ausente.toString(), true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("obrigatória").hasMessageContaining(ausente.toString());
        assertThatThrownBy(() -> ChaveRsaPersistente.carregarOuGerar("", true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("obrigatória");
    }

    @Test
    void comAChaveObrigatoriaEOArquivoPresenteOBootSegue() throws Exception {
        Path arquivo = pasta.resolve("jwt-rsa.pem");
        Files.writeString(arquivo, pemPkcs8(novoPar(2048)));

        assertThat(ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), true).isPrivate()).isTrue();
    }

    @Test
    void aMesmaChaveNoArquivoDaOMesmoKidEmQualquerReinicio() throws Exception {
        Path arquivo = pasta.resolve("jwt-rsa.pem");
        Files.writeString(arquivo, pemPkcs8(novoPar(2048)));

        RSAKey primeira = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);
        RSAKey depoisDoRestart = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);

        assertThat(depoisDoRestart.getKeyID()).isEqualTo(primeira.getKeyID());          // os tokens antigos continuam válidos
        assertThat(depoisDoRestart.toRSAPublicKey()).isEqualTo(primeira.toRSAPublicKey());
    }

    @Test
    void umaChaveExternaColocadaNoArquivoEhUsadaEASuaPublicaSaiDaPrivada() throws Exception {
        KeyPair externa = novoPar(2048);
        Path arquivo = pasta.resolve("externa.pem");
        Files.writeString(arquivo, pemPkcs8(externa));

        RSAKey carregada = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);

        assertThat(carregada.toRSAPublicKey()).isEqualTo(externa.getPublic());
        assertThat(carregada.toRSAPrivateKey()).isEqualTo(externa.getPrivate());
    }

    @Test
    void oKidEAImpressaoDigitalDaChaveEPortantoEstavel() throws Exception {
        Path arquivo = pasta.resolve("externa.pem");
        Files.writeString(arquivo, pemPkcs8(novoPar(2048)));

        RSAKey a = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);
        RSAKey b = ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false);

        assertThat(a.getKeyID()).isEqualTo(b.getKeyID()).isEqualTo(a.computeThumbprint().toString());
    }

    @Test
    void chaveCurtaDemaisEhRecusada() throws Exception {
        Path arquivo = pasta.resolve("curta.pem");
        Files.writeString(arquivo, pemPkcs8(novoPar(1024)));

        assertThatThrownBy(() -> ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("mínimo");
    }

    @Test
    void chaveEmPkcs1AvisaComoConverter() throws Exception {
        Path arquivo = pasta.resolve("pkcs1.pem");
        Files.writeString(arquivo, "-----BEGIN RSA PRIVATE KEY-----\nMIIB\n-----END RSA PRIVATE KEY-----\n");

        assertThatThrownBy(() -> ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("openssl pkcs8 -topk8");
    }

    @Test
    void arquivoCorrompidoNaoViraChaveNova() throws Exception {
        Path arquivo = pasta.resolve("estragado.pem");
        Files.writeString(arquivo, "-----BEGIN PRIVATE KEY-----\nlixo-que-nao-e-base64-valido!!\n-----END PRIVATE KEY-----\n");

        // Gerar outra por cima derrubaria as sessões sem ninguém perceber: o boot falha e avisa.
        assertThatThrownBy(() -> ChaveRsaPersistente.carregarOuGerar(arquivo.toString(), false))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Files.readString(arquivo)).contains("lixo-que-nao-e-base64-valido");
    }
}
