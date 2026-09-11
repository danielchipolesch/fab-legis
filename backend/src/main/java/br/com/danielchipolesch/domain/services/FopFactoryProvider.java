package br.com.danielchipolesch.domain.services;

import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;

import java.io.File;
import java.io.InputStream;

// Fábrica compartilhada do FopFactory, configurada com a fonte Calibri
// (Carlito, ver fop-config.xml e Dockerfile -- Carlito é metricamente
// compatível com Calibri e livremente redistribuível, ao contrário dela) --
// usada por todo gerador de PDF via Apache FOP (DocumentoPdfService,
// MapaAlteracaoPdfService), pra não duplicar o carregamento de configuração
// em cada um.
final class FopFactoryProvider {

    private static final FopFactory INSTANCE;

    static {
        try (InputStream config = FopFactoryProvider.class.getResourceAsStream("/fop-config.xml")) {
            var parser = new FopConfParser(config, new File(".").toURI());
            INSTANCE = parser.getFopFactoryBuilder().build();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private FopFactoryProvider() {
    }

    static FopFactory get() {
        return INSTANCE;
    }
}
