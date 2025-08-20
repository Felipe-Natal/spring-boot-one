package br.com.literalura;

import jakarta.persistence.*;

@Entity
public class Livro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String titulo;

    private String idioma;      // ex.: "en", "pt"
    private Integer downloads;  // download_count

    @ManyToOne
    @JoinColumn(name = "autor_id")
    private br.com.literalura.Autor autor;

    // Getters/Setters
    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    public Integer getDownloads() { return downloads; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public br.com.literalura.Autor getAutor() { return autor; }
    public void setAutor(br.com.literalura.Autor autor) { this.autor = autor; }

    @Override
    public String toString() {
        String a = (autor == null) ? "Desconhecido" : autor.getNome();
        return "Livro{id=%d, titulo='%s', idioma='%s', downloads=%s, autor='%s'}"
                .formatted(id, titulo, idioma, downloads, a);
    }
}
