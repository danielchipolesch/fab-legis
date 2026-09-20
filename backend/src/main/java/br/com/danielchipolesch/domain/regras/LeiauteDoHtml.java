package br.com.danielchipolesch.domain.regras;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;

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
