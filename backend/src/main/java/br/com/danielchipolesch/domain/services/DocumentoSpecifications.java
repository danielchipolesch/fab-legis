package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import org.springframework.data.jpa.domain.Specification;

// Predicados dinâmicos pra listagem paginada de documentos (ver
// DocumentoService.getAllPaginado) -- mesmo padrão de LogAuditoriaService.filtrar: cada
// filtro só entra na consulta se foi realmente informado (Specification.where(null) casa
// com tudo), nunca "(:param IS NULL OR campo = :param)".
public class DocumentoSpecifications {

    private DocumentoSpecifications() {
    }

    // Espelha EXATAMENTE ABA_FILTROS em HomePage.vue (não é uma expansão de escopo --
    // "meus" hoje é só autoria direta, sem coautoria, e "minha_om" exclui os documentos
    // que já aparecem em "meus"). As 4 abas não são uma partição estrita do acervo (um
    // documento revogado que eu mesmo autorei aparece tanto em "revogados" quanto em
    // "meus") -- mesmo comentário já existente em HomePage.vue.
    public static Specification<Documento> aba(String aba, Long usuarioId, Long omId) {
        if (aba == null) return Specification.where(null);
        return switch (aba) {
            case "meus" -> (root, query, cb) -> cb.equal(root.get("autor").get("id"), usuarioId);
            case "minha_om" -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("om").get("id"), omId),
                    cb.notEqual(root.get("autor").get("id"), usuarioId)
            );
            case "outras_oms" -> (root, query, cb) -> cb.notEqual(root.get("om").get("id"), omId);
            case "revogados" -> (root, query, cb) -> cb.equal(root.get("documentoStatus"), DocumentoStatusEnum.REVOGADO);
            default -> Specification.where(null);
        };
    }

    // Mesmos três campos que a busca em texto livre já comparava no frontend
    // (HomePage.vue, documentosDaAbaFiltrados) antes de virar filtro de servidor: nome e
    // código do assunto básico, e sigla da espécie.
    public static Specification<Documento> busca(String texto) {
        if (texto == null || texto.isBlank()) return Specification.where(null);
        String termo = "%" + texto.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("assuntoBasico").get("nome")), termo),
                cb.like(cb.lower(root.get("assuntoBasico").get("codigo")), termo),
                cb.like(cb.lower(root.get("especieNormativa").get("sigla")), termo)
        );
    }

    public static Specification<Documento> especieSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("especieNormativa").get("sigla"), sigla);
    }

    public static Specification<Documento> status(DocumentoStatusEnum status) {
        if (status == null) return Specification.where(null);
        return (root, query, cb) -> cb.equal(root.get("documentoStatus"), status);
    }
}
