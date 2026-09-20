package br.com.danielchipolesch.domain.regimes;

// As regras de UM regime normativo, reunidas. Cada implementação (AtoNormativo hoje; NPA depois) entrega
// uma implementação de cada interface de regra -- o restante do sistema nunca testa a espécie do documento,
// só pede a regra ao regime (RegimesNormativos.para(...)).
public interface RegimeDoDocumento {

    RegimeNormativo regime();

    CalculadoraDeNumeracaoDosElementos numeracao();

    EstruturaInicialDeNovoDocumento estruturaInicial();

    RotuloDosAnexos rotuloDosAnexos();

    LeiauteDoPdf leiauteDoPdf();

    LeiauteDoHtml leiauteDoHtml();

    RegrasDoCicloDeVidaDoDocumento cicloDeVida();
}
