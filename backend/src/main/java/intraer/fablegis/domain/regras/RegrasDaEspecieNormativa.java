package intraer.fablegis.domain.regras;

// As regras de UMA espécie normativa (ou de um grupo de espécies que se comportam igual), reunidas. Cada
// implementação (RegrasDeEspecieConvencional e RegrasDeComunicacaoOficialPadronizada, uma por TipoDeEspecie) entrega
// uma implementação de cada interface de regra -- o restante do sistema nunca testa a espécie do documento, só pede a regra a RegrasDasEspecies.
public interface RegrasDaEspecieNormativa {

    TipoDeEspecie tipo();

    RegrasDeCriacaoDoDocumento criacao();

    RegrasDeHierarquiaDosElementos hierarquia();

    CamposEspecificosDaEspecie camposEspecificos();

    RegrasDeRegistroDaPublicacao registroDaPublicacao();

    CalculadoraDeNumeracaoDosElementos numeracao();

    EstruturaInicialDeNovoDocumento estruturaInicial();

    RotuloDosAnexos rotuloDosAnexos();

    LeiauteDoPdf leiauteDoPdf();

    LeiauteDoHtml leiauteDoHtml();

    RegrasDoCicloDeVidaDoDocumento cicloDeVida();
}
