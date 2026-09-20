package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.regras.LeiauteDoHtml;
import org.springframework.stereotype.Component;

import java.util.List;

// Provisório: o layout do HTML da NPA é a etapa 4 da proposta em docs/roadmap.md. Até lá uma NPA não gera HTML.
@Component
public class LeiauteHtmlDeNpa implements LeiauteDoHtml {

    @Override
    public String gerarHtml(Documento documento, List<ItemPartePreliminarResponseDto> preliminares,
                            List<ItemAnexoParteNormativaResponseDto> normativos, List<AnexoResponseDto> anexos) {
        throw new UnsupportedOperationException("O HTML da NPA ainda não foi implementado.");
    }
}
