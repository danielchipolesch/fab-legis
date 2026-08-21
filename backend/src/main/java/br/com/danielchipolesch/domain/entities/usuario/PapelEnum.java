package br.com.danielchipolesch.domain.entities.usuario;

// Concessões ADICIONAIS por cima do perfil padrão (todo usuário autenticado já
// pode criar documentos, editar os próprios/compartilhados, visualizar
// qualquer documento e excluir conforme DocumentoAcessoService -- isso não
// depende de nenhum papel aqui, é o que "estar logado" já garante).
public enum PapelEnum {
    APROVADOR, // aprova/publica documentos da própria OM
    ADMIN      // gestão de usuários/papéis, sem escopo por OM
}
