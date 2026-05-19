package br.com.danielchipolesch.domain.entities.estruturaDocumento;

public enum ItemAnexoParteNormativaTipoEnum {

    // Tipos legados (parte normativa)
    TITULO,
    CAPITULO,
    SECAO,
    SUBSECAO,
    ARTIGO,
    PARAGRAFO_NUMERADO,
    PARAGRAFO_UNICO,
    INCISO,
    ALINEA,
    ITEM,

    // Parte Preliminar
    EPIGRAFE,
    EMENTA,
    PREAMBULO,
    FUNDAMENTACAO,

    // Parte Normativa (nomenclatura do frontend)
    SECAO_NORMATIVA,
    SUBSECAO_NORMATIVA,
    PARAGRAFO,
    SUB_ALINEA,

    // Parte Final
    CLAUSULA_REVOGATORIA,
    CLAUSULA_VIGENCIA,
    FECHO,
    ASSINATURA,
    REFERENDA
}
