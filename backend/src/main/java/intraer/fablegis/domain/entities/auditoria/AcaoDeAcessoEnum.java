package intraer.fablegis.domain.entities.auditoria;

// O que mudou no acesso de um usuário (ver LogDeAcesso).
public enum AcaoDeAcessoEnum {
    CRIOU_USUARIO,
    ALTEROU_PAPEIS,
    ATIVOU_USUARIO,
    DESATIVOU_USUARIO,
    TROCOU_OM,
    REDEFINIU_SENHA,
    REVISOU_ACESSO
}
