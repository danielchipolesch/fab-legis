package br.com.danielchipolesch.domain.regras.atonormativo;

import br.com.danielchipolesch.domain.regras.CalculadoraDeNumeracaoDosElementos;
import br.com.danielchipolesch.domain.regras.CamposEspecificosDaEspecie;
import br.com.danielchipolesch.domain.regras.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.regras.LeiauteDoHtml;
import br.com.danielchipolesch.domain.regras.LeiauteDoPdf;
import br.com.danielchipolesch.domain.regras.RegrasDaEspecieNormativa;
import br.com.danielchipolesch.domain.regras.RegrasDeCriacaoDoDocumento;
import br.com.danielchipolesch.domain.regras.RegrasDeHierarquiaDosElementos;
import br.com.danielchipolesch.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import br.com.danielchipolesch.domain.regras.RotuloDosAnexos;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import br.com.danielchipolesch.domain.services.CapitulosPadronizadosService;
import br.com.danielchipolesch.domain.services.DocumentoFoBuilder;
import br.com.danielchipolesch.domain.services.NumeracaoService;
import org.springframework.stereotype.Component;

// As regras dos atos normativos (DCA, ICA, NSCA...): LC 95/1998, Decreto 12.002/2024 e NSCA 5-3. Reúne as
// regras que já existiam espalhadas pelo sistema, sem alterar nenhuma -- cada uma agora atrás da sua
// interface.
@Component
public class RegrasDeAtoNormativo implements RegrasDaEspecieNormativa {

    private final RegrasDeCriacaoDoDocumento criacao;
    private final RegrasDeHierarquiaDosElementos hierarquia;
    private final CamposEspecificosDaEspecie camposEspecificos;
    private final CalculadoraDeNumeracaoDosElementos numeracao;
    private final EstruturaInicialDeNovoDocumento estruturaInicial;
    private final RotuloDosAnexos rotuloDosAnexos;
    private final LeiauteDoPdf leiauteDoPdf;
    private final LeiauteDoHtml leiauteDoHtml;
    private final RegrasDoCicloDeVidaDoDocumento cicloDeVida;

    public RegrasDeAtoNormativo(CriacaoDeAtoNormativo criacao,
                                HierarquiaDeAtoNormativo hierarquia,
                                SemCamposEspecificos camposEspecificos,
                                NumeracaoService numeracao,
                                CapitulosPadronizadosService estruturaInicial,
                                RotuloDeAnexoDeAtoNormativo rotuloDosAnexos,
                                DocumentoFoBuilder leiauteDoPdf,
                                LeiauteHtmlDeAtoNormativo leiauteDoHtml,
                                CicloDeVidaDeAtoNormativo cicloDeVida) {
        this.criacao = criacao;
        this.hierarquia = hierarquia;
        this.camposEspecificos = camposEspecificos;
        this.numeracao = numeracao;
        this.estruturaInicial = estruturaInicial;
        this.rotuloDosAnexos = rotuloDosAnexos;
        this.leiauteDoPdf = leiauteDoPdf;
        this.leiauteDoHtml = leiauteDoHtml;
        this.cicloDeVida = cicloDeVida;
    }

    @Override public TipoDeRegras tipo() { return TipoDeRegras.ATO_NORMATIVO; }
    @Override public RegrasDeCriacaoDoDocumento criacao() { return criacao; }
    @Override public RegrasDeHierarquiaDosElementos hierarquia() { return hierarquia; }
    @Override public CamposEspecificosDaEspecie camposEspecificos() { return camposEspecificos; }
    @Override public CalculadoraDeNumeracaoDosElementos numeracao() { return numeracao; }
    @Override public EstruturaInicialDeNovoDocumento estruturaInicial() { return estruturaInicial; }
    @Override public RotuloDosAnexos rotuloDosAnexos() { return rotuloDosAnexos; }
    @Override public LeiauteDoPdf leiauteDoPdf() { return leiauteDoPdf; }
    @Override public LeiauteDoHtml leiauteDoHtml() { return leiauteDoHtml; }
    @Override public RegrasDoCicloDeVidaDoDocumento cicloDeVida() { return cicloDeVida; }
}
