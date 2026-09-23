package intraer.fablegis.domain.regras.convencional;

import intraer.fablegis.domain.regras.CalculadoraDeNumeracaoDosElementos;
import intraer.fablegis.domain.regras.CamposEspecificosDaEspecie;
import intraer.fablegis.domain.regras.EstruturaInicialDeNovoDocumento;
import intraer.fablegis.domain.regras.LeiauteDoHtml;
import intraer.fablegis.domain.regras.LeiauteDoPdf;
import intraer.fablegis.domain.regras.RegrasDaEspecieNormativa;
import intraer.fablegis.domain.regras.RegrasDeCriacaoDoDocumento;
import intraer.fablegis.domain.regras.RegrasDeHierarquiaDosElementos;
import intraer.fablegis.domain.regras.RegrasDeRegistroDaPublicacao;
import intraer.fablegis.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import intraer.fablegis.domain.regras.RotuloDosAnexos;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.domain.services.CapitulosPadronizadosService;
import intraer.fablegis.domain.services.DocumentoFoBuilder;
import intraer.fablegis.domain.services.NumeracaoService;
import intraer.fablegis.domain.services.PublicacaoDeEspecieConvencional;
import org.springframework.stereotype.Component;

// As regras das Espécies Convencionais (MCA, NSCA, ICA, ROCA, DCA...): LC 95/1998, Decreto 12.002/2024 e NSCA 5-3. Reúne
// as regras que já existiam espalhadas pelo sistema, sem alterar nenhuma -- cada uma agora atrás da sua interface.
@Component
public class RegrasDeEspecieConvencional implements RegrasDaEspecieNormativa {

    private final RegrasDeCriacaoDoDocumento criacao;
    private final RegrasDeHierarquiaDosElementos hierarquia;
    private final CamposEspecificosDaEspecie camposEspecificos;
    private final RegrasDeRegistroDaPublicacao registroDaPublicacao;
    private final CalculadoraDeNumeracaoDosElementos numeracao;
    private final EstruturaInicialDeNovoDocumento estruturaInicial;
    private final RotuloDosAnexos rotuloDosAnexos;
    private final LeiauteDoPdf leiauteDoPdf;
    private final LeiauteDoHtml leiauteDoHtml;
    private final RegrasDoCicloDeVidaDoDocumento cicloDeVida;

    public RegrasDeEspecieConvencional(CriacaoDeEspecieConvencional criacao,
                                HierarquiaDeEspecieConvencional hierarquia,
                                SemCamposEspecificos camposEspecificos,
                                PublicacaoDeEspecieConvencional registroDaPublicacao,
                                NumeracaoService numeracao,
                                CapitulosPadronizadosService estruturaInicial,
                                RotuloDeAnexoDeEspecieConvencional rotuloDosAnexos,
                                DocumentoFoBuilder leiauteDoPdf,
                                LeiauteHtmlDeEspecieConvencional leiauteDoHtml,
                                CicloDeVidaDeEspecieConvencional cicloDeVida) {
        this.criacao = criacao;
        this.hierarquia = hierarquia;
        this.camposEspecificos = camposEspecificos;
        this.registroDaPublicacao = registroDaPublicacao;
        this.numeracao = numeracao;
        this.estruturaInicial = estruturaInicial;
        this.rotuloDosAnexos = rotuloDosAnexos;
        this.leiauteDoPdf = leiauteDoPdf;
        this.leiauteDoHtml = leiauteDoHtml;
        this.cicloDeVida = cicloDeVida;
    }

    @Override public TipoDeEspecie tipo() { return TipoDeEspecie.CONVENCIONAL; }
    @Override public RegrasDeCriacaoDoDocumento criacao() { return criacao; }
    @Override public RegrasDeHierarquiaDosElementos hierarquia() { return hierarquia; }
    @Override public CamposEspecificosDaEspecie camposEspecificos() { return camposEspecificos; }
    @Override public RegrasDeRegistroDaPublicacao registroDaPublicacao() { return registroDaPublicacao; }
    @Override public CalculadoraDeNumeracaoDosElementos numeracao() { return numeracao; }
    @Override public EstruturaInicialDeNovoDocumento estruturaInicial() { return estruturaInicial; }
    @Override public RotuloDosAnexos rotuloDosAnexos() { return rotuloDosAnexos; }
    @Override public LeiauteDoPdf leiauteDoPdf() { return leiauteDoPdf; }
    @Override public LeiauteDoHtml leiauteDoHtml() { return leiauteDoHtml; }
    @Override public RegrasDoCicloDeVidaDoDocumento cicloDeVida() { return cicloDeVida; }
}
