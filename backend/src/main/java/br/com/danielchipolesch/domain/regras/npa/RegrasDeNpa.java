package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.domain.regras.CalculadoraDeNumeracaoDosElementos;
import br.com.danielchipolesch.domain.regras.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.regras.LeiauteDoHtml;
import br.com.danielchipolesch.domain.regras.LeiauteDoPdf;
import br.com.danielchipolesch.domain.regras.RegrasDaEspecieNormativa;
import br.com.danielchipolesch.domain.regras.RegrasDeCriacaoDoDocumento;
import br.com.danielchipolesch.domain.regras.RegrasDeHierarquiaDosElementos;
import br.com.danielchipolesch.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import br.com.danielchipolesch.domain.regras.RotuloDosAnexos;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import org.springframework.stereotype.Component;

// As regras da NPA (Norma Padrão de Ação): espécie de uso interno da OM, com identificação livre, gramática de
// elementos e numeração próprias (1, 1.1, 1.1.1.1), layout próprio e sem alteração -- só publicação e revogação.
// Ver docs/dominio.md.
@Component
public class RegrasDeNpa implements RegrasDaEspecieNormativa {

    private final CriacaoDeNpa criacao;
    private final HierarquiaDeNpa hierarquia;
    private final NumeracaoDeNpa numeracao;
    private final EstruturaInicialDeNpa estruturaInicial;
    private final RotuloDeAnexoDeNpa rotuloDosAnexos;
    private final LeiauteDoPdfDeNpa leiauteDoPdf;
    private final LeiauteHtmlDeNpa leiauteDoHtml;
    private final CicloDeVidaDeNpa cicloDeVida;

    public RegrasDeNpa(CriacaoDeNpa criacao, HierarquiaDeNpa hierarquia, NumeracaoDeNpa numeracao,
                       EstruturaInicialDeNpa estruturaInicial, RotuloDeAnexoDeNpa rotuloDosAnexos,
                       LeiauteDoPdfDeNpa leiauteDoPdf, LeiauteHtmlDeNpa leiauteDoHtml, CicloDeVidaDeNpa cicloDeVida) {
        this.criacao = criacao;
        this.hierarquia = hierarquia;
        this.numeracao = numeracao;
        this.estruturaInicial = estruturaInicial;
        this.rotuloDosAnexos = rotuloDosAnexos;
        this.leiauteDoPdf = leiauteDoPdf;
        this.leiauteDoHtml = leiauteDoHtml;
        this.cicloDeVida = cicloDeVida;
    }

    @Override public TipoDeRegras tipo() { return TipoDeRegras.NPA; }
    @Override public RegrasDeCriacaoDoDocumento criacao() { return criacao; }
    @Override public RegrasDeHierarquiaDosElementos hierarquia() { return hierarquia; }
    @Override public CalculadoraDeNumeracaoDosElementos numeracao() { return numeracao; }
    @Override public EstruturaInicialDeNovoDocumento estruturaInicial() { return estruturaInicial; }
    @Override public RotuloDosAnexos rotuloDosAnexos() { return rotuloDosAnexos; }
    @Override public LeiauteDoPdf leiauteDoPdf() { return leiauteDoPdf; }
    @Override public LeiauteDoHtml leiauteDoHtml() { return leiauteDoHtml; }
    @Override public RegrasDoCicloDeVidaDoDocumento cicloDeVida() { return cicloDeVida; }
}
