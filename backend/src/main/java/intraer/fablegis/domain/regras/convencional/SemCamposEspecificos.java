package intraer.fablegis.domain.regras.convencional;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.regras.CamposEspecificosDaEspecie;
import org.springframework.stereotype.Component;

// Um ato normativo não tem campos além dos de Documento: epígrafe, ementa, preâmbulo, fecho e assinatura são itens
// da parte preliminar/final, preenchidos na publicação.
@Component
public class SemCamposEspecificos implements CamposEspecificosDaEspecie {

    @Override
    public void criarPara(Documento documento) {
        // nada a criar
    }

    @Override
    public void copiar(Documento original, Documento copia) {
        // nada a copiar
    }
}
