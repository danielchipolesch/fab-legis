package intraer.fablegis.application.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VersaoControllerTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<BuildProperties> com(BuildProperties props) {
        ObjectProvider<BuildProperties> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(props);
        return p;
    }

    @Test
    void devolveAVersaoDoBuild() {
        Properties p = new Properties();
        p.setProperty("version", "0.1.0");
        assertEquals("0.1.0", new VersaoController(com(new BuildProperties(p))).versao().get("versao"));
    }

    @Test
    void semBuildInfoDevolveDesconhecida() {
        assertEquals("desconhecida", new VersaoController(com(null)).versao().get("versao"));
    }
}
