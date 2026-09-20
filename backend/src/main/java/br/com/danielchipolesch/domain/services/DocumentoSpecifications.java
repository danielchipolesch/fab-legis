package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

// Predicados dinâmicos pra listagem paginada de documentos (ver
// DocumentoService.getAllPaginado) -- mesmo padrão de LogAuditoriaService.filtrar: cada
// filtro só entra na consulta se foi realmente informado (um Predicate nulo casa
// com tudo -- é o que o JPA Criteria API entende como "sem restrição"; substitui
// Specification.where(null), removido no Spring Data JPA 4/Boot 4), nunca
// "(:param IS NULL OR campo = :param)".
public class DocumentoSpecifications {

    private DocumentoSpecifications() {
    }

    // Espelha EXATAMENTE ABA_FILTROS em HomePage.vue (não é uma expansão de escopo).
    // "meus" é autoria OU coautoria (ver DocumentoCompartilhamento), sempre, independente
    // da OM do documento -- coautoria nunca é barrada por OM. "minha_om"/"outras_oms" são
    // só sobre a OM do documento em si (om do autor no momento da criação), sem levar em
    // conta autoria/coautoria nenhuma -- por isso as 4 abas propositalmente NÃO são uma
    // partição estrita do acervo: um documento que eu autorei (OM A) com um coautor da OM
    // B aparece em "meus" pros dois, mas em "minha_om" só pra mim (é da OM A) e em
    // "outras_oms" só pro coautor (não é da OM dele) -- a OM nunca esconde um documento de
    // quem é autor/coautor dele, só decide em qual das outras duas abas ele cai pra quem
    // não é. Mesmo comentário já existente em HomePage.vue.
    public static Specification<Documento> aba(String aba, Long usuarioId, Long omId) {
        if (aba == null) return (root, query, cb) -> null;
        return switch (aba) {
            case "meus" -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("autor").get("id"), usuarioId),
                    root.get("id").in(coautorDocumentoIds(query, cb, usuarioId))
            );
            case "minha_om" -> (root, query, cb) -> cb.equal(root.get("om").get("id"), omId);
            case "outras_oms" -> (root, query, cb) -> cb.notEqual(root.get("om").get("id"), omId);
            case "revogados" -> (root, query, cb) -> cb.equal(root.get("situacaoBca"), SituacaoBcaEnum.REVOGADO);
            default -> (root, query, cb) -> null;
        };
    }

    // Ids de documento em que usuarioId é coautor (t_documento_compartilhamento), não
    // autor -- usado só por "meus", pra incluir esses documentos além dos que o usuário
    // autorou diretamente.
    private static Subquery<Long> coautorDocumentoIds(CommonAbstractCriteria query, CriteriaBuilder cb, Long usuarioId) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<DocumentoCompartilhamento> compRoot = sub.from(DocumentoCompartilhamento.class);
        Predicate condicao = cb.equal(compRoot.get("usuario").get("id"), usuarioId);
        return sub.select(compRoot.get("documento").get("id")).where(condicao);
    }

    // Mesmos três campos que a busca em texto livre já comparava no frontend
    // (HomePage.vue, documentosDaAbaFiltrados) antes de virar filtro de servidor: nome e
    // código do assunto básico, e sigla da espécie.
    public static Specification<Documento> busca(String texto) {
        if (texto == null || texto.isBlank()) return (root, query, cb) -> null;
        String termo = "%" + texto.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("assuntoBasico").get("nome")), termo),
                cb.like(cb.lower(root.get("assuntoBasico").get("codigo")), termo),
                cb.like(cb.lower(root.get("especieNormativa").get("sigla")), termo)
        );
    }

    public static Specification<Documento> especieSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) return (root, query, cb) -> null;
        return (root, query, cb) -> cb.equal(root.get("especieNormativa").get("sigla"), sigla);
    }

    public static Specification<Documento> situacaoBca(SituacaoBcaEnum situacao) {
        if (situacao == null) return (root, query, cb) -> null;
        return (root, query, cb) -> cb.equal(root.get("situacaoBca"), situacao);
    }

    public static Specification<Documento> situacaoLocal(SituacaoLocalEnum situacao) {
        if (situacao == null) return (root, query, cb) -> null;
        return (root, query, cb) -> cb.equal(root.get("situacaoLocal"), situacao);
    }
}
