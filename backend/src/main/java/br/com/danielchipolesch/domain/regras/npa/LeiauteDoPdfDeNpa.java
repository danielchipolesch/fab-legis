package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.regras.LeiauteDoPdf;
import org.springframework.stereotype.Component;

import java.util.List;

// Provisório: o layout do PDF da NPA (moldura em todas as páginas, tabela de cabeçalho, "n/total") é a etapa 4 da
// proposta em docs/roadmap.md. Até lá uma NPA não gera PDF.
@Component
public class LeiauteDoPdfDeNpa implements LeiauteDoPdf {

    @Override
    public String gerarFo(Documento documento, List<ItemPartePreliminarResponseDto> preliminares,
                          List<ItemAnexoParteNormativaResponseDto> normativos, List<AnexoResponseDto> anexos) {
        throw new UnsupportedOperationException("O PDF da NPA ainda não foi implementado.");
    }
}
