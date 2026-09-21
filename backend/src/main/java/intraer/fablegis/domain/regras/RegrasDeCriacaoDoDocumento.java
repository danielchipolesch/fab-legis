package intraer.fablegis.domain.regras;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.Usuario;

// Como um documento novo (ou a cópia de um existente) é montado -- o que a espécie exige para criá-lo e como ele
// passa a se identificar. Um ato normativo pede o assunto básico e recebe o próximo sequencial ("DCA 11-3");
// uma NPA pede a identificação em texto livre e não tem assunto básico nem sequencial.
//
// Devolve o Documento ainda não gravado (rascunho, com autor e OM de quem cria); a gravação, o histórico e a
// estrutura inicial continuam com DocumentoService.
public interface RegrasDeCriacaoDoDocumento {

    // Lança InvalidInputException quando falta algo que a espécie exige (ex.: o assunto básico de um ato normativo).
    Documento montar(DocumentoRequestCreateDto pedido, EspecieNormativa especie, Usuario autor);

    // A cópia é um documento novo: recebe a identificação que a espécie daria a um documento criado agora.
    Documento montarCopiaDe(Documento original, Usuario autor);
}
