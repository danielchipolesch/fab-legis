package intraer.fablegis.domain.regras;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import intraer.fablegis.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;

import java.util.List;

// A diagramação do documento em HTML (cópia de leitura). Mesma regra do PDF: o layout é da espécie, e o
// restante -- escolher a versão vigente ou em tramitação, armazenar, servir -- não depende dele. Deve
// seguir as mesmas regras de numeração, cláusulas e elementos do PDF (ver CLAUDE.md, consistência entre
// formatos).
public interface LeiauteDoHtml {

    String gerarHtml(Documento documento,
                     List<ItemPartePreliminarResponseDto> preliminares,
                     List<ItemAnexoParteNormativaResponseDto> normativos,
                     List<AnexoResponseDto> anexos);
}
