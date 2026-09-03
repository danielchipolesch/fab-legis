package br.com.danielchipolesch.domain.entities.usuario;

// Todo usuário autenticado pode visualizar qualquer documento -- isso não
// depende de nenhum papel aqui, é o que "estar logado" já garante
// (DocumentoAcessoService). Os demais poderes do ciclo de vida do documento
// (criar/editar, revisar, publicar) são todos concedidos explicitamente por
// papel, sem implícito nenhum -- ver DocumentoAcessoService.
public enum PapelEnum {
    EDIT,   // cria e edita documentos (autor/coautor), envia para revisão escolhendo o APROV
    APROV,  // revisa um documento que lhe foi atribuído: aprova (escolhendo o PUBLIC) ou devolve
    PUBLIC, // publica/revoga formalmente um documento que lhe foi atribuído (portaria/BCA)
    ADMIN,  // gestão de usuários/OMs -- papel puramente administrativo, sem poder nenhum sobre
            // o ciclo de vida de documentos (não edita, não revisa, não publica)
    AUDITOR // acesso de leitura à trilha de auditoria (ver LogAuditoria), sem escopo por OM
}
