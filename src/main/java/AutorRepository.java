package br.com.literalura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {
    Optional<Autor> findByNomeIgnoreCase(String nome);

    @Query("""
           select a from Autor a
           where a.anoNascimento <= :ano
             and (a.anoFalecimento is null or a.anoFalecimento >= :ano)
           """)
    List<Autor> autoresVivosNoAno(int ano);
}
