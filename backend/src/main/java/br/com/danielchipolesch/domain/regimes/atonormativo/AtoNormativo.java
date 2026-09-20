package br.com.danielchipolesch.domain.regimes.atonormativo;

import br.com.danielchipolesch.domain.regimes.CalculadoraDeNumeracaoDosElementos;
import br.com.danielchipolesch.domain.regimes.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.regimes.LeiauteDoHtml;
import br.com.danielchipolesch.domain.regimes.LeiauteDoPdf;
import br.com.danielchipolesch.domain.regimes.RegimeDoDocumento;
import br.com.danielchipolesch.domain.regimes.RegimeNormativo;
import br.com.danielchipolesch.domain.regimes.RegrasDoCicloDeVidaDoDocumento;
import br.com.danielchipolesch.domain.regimes.RotuloDosAnexos;
import br.com.danielchipolesch.domain.services.CapitulosPadronizadosService;
import br.com.danielchipolesch.domain.services.DocumentoFoBuilder;
import br.com.danielchipolesch.domain.services.NumeracaoService;
import org.springframework.stereotype.Component;

// O regime dos atos normativos (DCA, ICA, NSCA...): LC 95/1998, Decreto 12.002/2024 e NSCA 5-3. Reúne as
// regras que já existiam espalhadas pelo sistema, sem alterar nenhuma -- cada uma agora atrás da sua
// interface.
@Component
public class AtoNormativo implements RegimeDoDocumento {

    private final CalculadoraDeNumeracaoDosElementos numeracao;
    private final EstruturaInicialDeNovoDocumento estruturaInicial;
    private final RotuloDosAnexos rotuloDosAnexos;
    private final LeiauteDoPdf leiauteDoPdf;
    private final LeiauteDoHtml leiauteDoHtml;
    private final RegrasDoCicloDeVidaDoDocumento cicloDeVida;

    public AtoNormativo(NumeracaoService numeracao,
                        CapitulosPadronizadosService estruturaInicial,
                        RotuloDeAnexoDeAtoNormativo rotuloDosAnexos,
                        DocumentoFoBuilder leiauteDoPdf,
                        LeiauteHtmlDeAtoNormativo leiauteDoHtml,
                        CicloDeVidaDeAtoNormativo cicloDeVida) {
        this.numeracao = numeracao;
        this.estruturaInicial = estruturaInicial;
        this.rotuloDosAnexos = rotuloDosAnexos;
        this.leiauteDoPdf = leiauteDoPdf;
        this.leiauteDoHtml = leiauteDoHtml;
        this.cicloDeVida = cicloDeVida;
    }

    @Override public RegimeNormativo regime() { return RegimeNormativo.ATO_NORMATIVO; }
    @Override public CalculadoraDeNumeracaoDosElementos numeracao() { return numeracao; }
    @Override public EstruturaInicialDeNovoDocumento estruturaInicial() { return estruturaInicial; }
    @Override public RotuloDosAnexos rotuloDosAnexos() { return rotuloDosAnexos; }
    @Override public LeiauteDoPdf leiauteDoPdf() { return leiauteDoPdf; }
    @Override public LeiauteDoHtml leiauteDoHtml() { return leiauteDoHtml; }
    @Override public RegrasDoCicloDeVidaDoDocumento cicloDeVida() { return cicloDeVida; }
}
