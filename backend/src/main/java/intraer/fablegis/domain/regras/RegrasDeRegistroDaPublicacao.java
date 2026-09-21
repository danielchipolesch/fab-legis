package intraer.fablegis.domain.regras;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;

// O que se registra quando um documento é publicado ou revogado oficialmente, e o que é obrigatório informar para isso.
// Num ato normativo: portaria + BCA (e, na primeira publicação, a parte preliminar). Numa NPA: só o número e a data
// do Boletim Interno. É a única transição que muda a situação BCA -- quem a muda é o DocumentoStatusService, depois
// que este registro valida o pedido e grava os dados no documento.
public interface RegrasDeRegistroDaPublicacao {

    // acao é PUBLICAR ou REVOGAR. Lança StatusCannotBeUpdatedException se faltar ou estiver errado algo obrigatório.
    void registrar(Documento documento, DocumentoStatusRequestDto pedido, AcaoDeEtapa acao, SituacaoBcaEnum bcaAnterior);
}
