package br.com.danielchipolesch.domain.regras;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;

import java.util.List;

// A diagramação do documento em PDF: monta o XSL-FO completo, que o Apache FOP transforma em PDF. O layout
// é da espécie -- página de Portaria + Capa + Sumário para um ato normativo; moldura com tabela de cabeçalho
// para uma NPA. A geração do arquivo (FOP, armazenamento, limite de concorrência) não depende da espécie.
public interface LeiauteDoPdf {

    String gerarFo(Documento documento,
                   List<ItemPartePreliminarResponseDto> preliminares,
                   List<ItemAnexoParteNormativaResponseDto> normativos,
                   List<AnexoResponseDto> anexos);
}
