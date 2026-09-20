package br.com.danielchipolesch.domain.regras;

// As regras de UMA espécie normativa (ou de um grupo de espécies que se comportam igual), reunidas. Cada
// implementação (RegrasDeAtoNormativo hoje; RegrasDeNpa depois) entrega uma implementação de cada interface de
// regra -- o restante do sistema nunca testa a espécie do documento, só pede a regra a RegrasDasEspecies.
public interface RegrasDaEspecieNormativa {

    TipoDeRegras tipo();

    RegrasDeCriacaoDoDocumento criacao();

    RegrasDeHierarquiaDosElementos hierarquia();

    CalculadoraDeNumeracaoDosElementos numeracao();

    EstruturaInicialDeNovoDocumento estruturaInicial();

    RotuloDosAnexos rotuloDosAnexos();

    LeiauteDoPdf leiauteDoPdf();

    LeiauteDoHtml leiauteDoHtml();

    RegrasDoCicloDeVidaDoDocumento cicloDeVida();
}
