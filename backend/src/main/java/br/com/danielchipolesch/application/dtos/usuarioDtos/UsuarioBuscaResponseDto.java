package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;

// Candidato a um seletor de "buscar pessoa por nome" (coautoria -- ver
// UsuarioController.buscar/CompartilharDialog.vue). Ao contrário de
// UsuarioElegivelResponseDto (elegiveis/SelecionarPessoaDialog.vue), a busca de
// coautoria não é restrita à OM de quem chama, então carrega a sigla da OM --
// necessária pra desambiguar nomes iguais em OMs diferentes na lista de resultados.
public record UsuarioBuscaResponseDto(
        Long id,
        String nome,
        String nomeGuerra,
        String postoGraduacaoBigrama,
        String cpf,
        String omSigla
) {
    public static UsuarioBuscaResponseDto from(Usuario usuario) {
        var posto = usuario.getPostoGraduacao();
        return new UsuarioBuscaResponseDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getNomeGuerra(),
                posto != null ? posto.getBigrama() : null,
                usuario.getCpf(),
                usuario.getOm() != null ? usuario.getOm().getSigla() : null
        );
    }
}
