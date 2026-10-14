package intraer.fablegis.domain.entities.usuario;

// Os perfis (papéis) de acesso. Cada um tem o CÓDIGO (o próprio nome da constante, que é o que se grava em t_usuario_papel.sg_papel) e o
// NOME POR EXTENSO (o que as telas e a documentação mostram) -- a tabela t_papel guarda os dois, e PapelEnumTest garante que enum e
// tabela dizem a mesma coisa. Os códigos têm trigrama + sufixo de escopo, com sublinhado.
//
// Todo usuário autenticado pode visualizar qualquer documento -- isso não depende de nenhum papel, é o que "estar logado" já garante
// (DocumentoAcessoService). Os demais poderes são concedidos explicitamente por papel, sem implícito nenhum. O escopo vem da OM de
// quem tem o papel (EscopoDeAcesso): os gerais (_GER) só existem no CENDOC; os da OM (_OM), em qualquer outra; a publicação (PUB_BCA),
// só no CENDOC.
public enum PapelEnum {

    ELB("Elaborador", "Cria e edita documentos como autor ou coautor e os envia para aprovação"),
    APR_OM("Aprovador da OM", "Aprova ou devolve os documentos da própria OM (qualquer aprovador da OM age)"),
    PUB_BCA("Publicador do BCA", "Registra a publicação e a revogação de documentos (papel exclusivo do CENDOC)"),
    AUD_OM("Auditor da OM", "Consulta a trilha de auditoria da própria OM"),
    AUD_GER("Auditor Geral", "Consulta a trilha de auditoria de todas as OMs (papel exclusivo do CENDOC)"),
    ADM_OM("Administrador da OM", "Gerencia os usuários da própria OM; não altera outros administradores"),
    ADM_GER("Administrador Geral", "Gerencia os usuários de todas as OMs e os administradores das OMs; cadastra habilitações e DOM (papel exclusivo do CENDOC)");

    private final String nome;
    private final String descricao;

    PapelEnum(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    // O código do perfil (o que vai para o banco e para o token).
    public String codigo() {
        return name();
    }

    // O nome por extenso.
    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }
}
