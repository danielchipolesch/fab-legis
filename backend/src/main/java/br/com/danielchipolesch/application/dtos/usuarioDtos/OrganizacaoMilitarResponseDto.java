package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;

public record OrganizacaoMilitarResponseDto(
        Long id,
        String nome,
        String sigla
) {
    public static OrganizacaoMilitarResponseDto from(OrganizacaoMilitar om) {
        return new OrganizacaoMilitarResponseDto(om.getId(), om.getNome(), om.getSigla());
    }
}
