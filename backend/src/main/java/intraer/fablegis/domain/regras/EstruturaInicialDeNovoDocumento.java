package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;

// Os elementos com que um documento novo já nasce. Num ato normativo, os capítulos padronizados da NSCA
// 5-3 (arts. 63 a 67); numa NPA, os três capítulos obrigatórios do layout (Disposições Preliminares,
// Gerais e Finais) com suas seções fixas.
public interface EstruturaInicialDeNovoDocumento {

    // Grava a estrutura inicial do documento já persistido.
    void criarEm(Documento documento);
}
