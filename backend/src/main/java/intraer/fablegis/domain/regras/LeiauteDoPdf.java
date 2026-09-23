package intraer.fablegis.domain.regras;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import intraer.fablegis.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;

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
