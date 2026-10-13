package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;

public record OrganizacaoMilitarResponseDto(
        Long id,
        String nome,
        String sigla,
        String urlDistintivo
) {
    public static OrganizacaoMilitarResponseDto from(OrganizacaoMilitar om) {
        return new OrganizacaoMilitarResponseDto(om.getId(), om.getNome(), om.getSigla(), om.getUrlDistintivo());
    }
}
