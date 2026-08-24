package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.usuario.PostoGraduacao;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostoGraduacaoRepository extends JpaRepository<PostoGraduacao, Long> {
    default List<PostoGraduacao> findAllOrdenado() {
        return findAll(Sort.by("ordem"));
    }
}
