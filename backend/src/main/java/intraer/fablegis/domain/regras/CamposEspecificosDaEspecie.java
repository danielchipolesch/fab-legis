package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;

// Dados que só algumas espécies têm, guardados numa estrutura própria 1:1 com o documento -- não em colunas de
// Documento. Os atos normativos não têm nenhum (a parte preliminar cobre epígrafe, ementa e preâmbulo); a NPA tem
// setor emissor, local do fecho e blocos de assinatura (CamposDaNpa). Esta interface cobre só o ciclo de vida
// desses dados junto com o documento; ler e alterar é feito pela API própria de cada espécie.
//
// A exclusão não aparece aqui de propósito: a linha dos campos é removida em cascata pelo banco junto com o documento.
public interface CamposEspecificosDaEspecie {

    // Cria os campos com valores iniciais, logo depois de o documento novo ser gravado.
    void criarPara(Documento documento);

    // A cópia de um documento leva os mesmos campos do original.
    void copiar(Documento original, Documento copia);
}
