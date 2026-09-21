package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.domain.regras.CalculadoraDeNumeracaoDosElementos;
import br.com.danielchipolesch.domain.regras.CamposEspecificosDaEspecie;
import br.com.danielchipolesch.domain.regras.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.regras.LeiauteDoHtml;
import br.com.danielchipolesch.domain.regras.LeiauteDoPdf;
import br.com.danielchipolesch.domain.regras.RegrasDaEspecieNormativa;
import br.com.danielchipolesch.domain.regras.RegrasDeCriacaoDoDocumento;
import br.com.danielchipolesch.domain.regras.RegrasDeHierarquiaDosElementos;
import br.com.danielchipolesch.domain.regras.RegrasDeRegistroDaPublicacao;
import br.com.danielchipolesch.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import br.com.danielchipolesch.domain.regras.RotuloDosAnexos;
import br.com.danielchipolesch.domain.regras.TipoDeEspecie;
import br.com.danielchipolesch.domain.services.DocumentoFoNpaBuilder;
import org.springframework.stereotype.Component;

// As regras das Espécies de Comunicações Oficiais Padronizadas (NSCA 5-3, Capítulo VIII, Seção VIII), de uso interno da
// OM -- hoje a NPA (Norma Padrão de Ação): identificação livre, gramática de elementos e numeração próprias
// (1, 1.1, 1.1.1.1), layout próprio e sem alteração -- só publicação e revogação. Ver docs/dominio.md.
@Component
public class RegrasDeComunicacaoOficialPadronizada implements RegrasDaEspecieNormativa {

    private final CriacaoDeNpa criacao;
    private final HierarquiaDeNpa hierarquia;
    private final CamposDeNpa camposEspecificos;
    private final PublicacaoDeNpa registroDaPublicacao;
    private final NumeracaoDeNpa numeracao;
    private final EstruturaInicialDeNpa estruturaInicial;
    private final RotuloDeAnexoDeNpa rotuloDosAnexos;
    private final DocumentoFoNpaBuilder leiauteDoPdf;
    private final LeiauteHtmlDeNpa leiauteDoHtml;
    private final CicloDeVidaDeNpa cicloDeVida;

    public RegrasDeComunicacaoOficialPadronizada(CriacaoDeNpa criacao, HierarquiaDeNpa hierarquia, CamposDeNpa camposEspecificos,
                       PublicacaoDeNpa registroDaPublicacao, NumeracaoDeNpa numeracao,
                       EstruturaInicialDeNpa estruturaInicial, RotuloDeAnexoDeNpa rotuloDosAnexos,
                       DocumentoFoNpaBuilder leiauteDoPdf, LeiauteHtmlDeNpa leiauteDoHtml, CicloDeVidaDeNpa cicloDeVida) {
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

    @Override public TipoDeEspecie tipo() { return TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA; }
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
