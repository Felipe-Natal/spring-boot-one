package br.com.literalura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {
    Optional<Livro> findByTituloIgnoreCase(String titulo);

    List<Livro> findByIdiomaIgnoreCase(String idioma);

    long countByIdiomaIgnoreCase(String idioma);

    List<Livro> findByAutorId(Long autorId);
}
