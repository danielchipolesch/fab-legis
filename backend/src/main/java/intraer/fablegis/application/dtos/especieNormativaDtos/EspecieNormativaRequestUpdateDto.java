package intraer.fablegis.application.dtos.especieNormativaDtos;

import lombok.Data;

@Data
public class EspecieNormativaRequestUpdateDto {

    private String sigla;
    private String nome;
    private String descricao;
}
