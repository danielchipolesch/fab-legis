package intraer.fablegis.domain.entities.estruturaDocumento;

// Qual versão do documento exportar/visualizar (PDF e HTML):
//   VIGENTE    -- a que a situação BCA descreve (o texto oficialmente em vigor);
//   TRAMITACAO -- o texto da etapa local em curso (ex.: a alteração em elaboração).
// Um documento só tem versão em tramitação enquanto a situação local não for SEM_ETAPA, e só
// tem versão vigente depois da primeira publicação. Ver VersoesDocumento.
public enum VersaoDocumentoEnum {
    VIGENTE,
    TRAMITACAO
}
