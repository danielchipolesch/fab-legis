package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;

// Candidato a um seletor de "escolher pessoa" (revisor/publicador -- ver
// UsuarioController.elegiveis/SelecionarPessoaDialog.vue). Deliberadamente mínimo
// (sem papéis, sem OM -- o próprio endpoint já filtrou por isso).
public record UsuarioElegivelResponseDto(
        Long id,
        String nome,
        String nomeGuerra,
        String postoGraduacaoBigrama,
        String cpf
) {
    public static UsuarioElegivelResponseDto from(Usuario usuario) {
        var posto = usuario.getPostoGraduacao();
        return new UsuarioElegivelResponseDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getNomeGuerra(),
                posto != null ? posto.getBigrama() : null,
                usuario.getCpf()
        );
    }
}
