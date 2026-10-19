package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.Usuario;

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

    // Quais OMs podem criar documentos da espécie (todas, ou só as habilitadas pelo administrador).
    RegraDeHabilitacaoDaOm habilitacao();

    // A espécie é de uso interno da OM (a NPA)? Então cada OM a produz, a publica e a acompanha: quem publica é da própria OM do
    // documento (não do CENDOC) e ela não entra nos indicadores do Painel Gerencial Geral -- só no da OM. Isso NÃO limita quem a vê:
    // qualquer OM vê qualquer documento, NPA inclusive (premissa do sistema, para estimular as boas práticas).
    boolean deUsoInternoDaOm();

    // Quem pode publicar (e revogar) um documento desta espécie: tem o papel de quem publica (cicloDeVida().papelQuePublica()) e, se a
    // espécie é de uso interno, é da OM do documento. O Publicador do BCA (CENDOC) publica as convencionais de qualquer OM.
    default boolean podePublicar(Usuario usuario, Long omDoDocumento) {
        if (!usuario.getPapeis().contains(cicloDeVida().papelQuePublica())) return false;
        return !deUsoInternoDaOm() || (usuario.getOm() != null && usuario.getOm().getId().equals(omDoDocumento));
    }
}
