package br.com.literalura;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpringBootApplication
public class LiteraluraApplication implements CommandLineRunner {

    @Autowired
    private br.com.literalura.LivroRepository livroRepository;

    @Autowired
    private br.com.literalura.AutorRepository autorRepository;

    private final Scanner scanner = new Scanner(System.in);
    private final br.com.literalura.ConsumoApi http = new br.com.literalura.ConsumoApi();
    private final ObjectMapper mapper = new br.com.literalura.ObjectMapper();

    public static void main(String[] args) {
        SpringApplication.run(LiteraluraApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        menu();
    }

    private void menu() throws Exception {
        String op;
        do {
            System.out.println("""
                    
                    ====== LITERALURA ======
                    1) Buscar livro pelo título (API -> salvar no banco)
                    2) Listar livros registrados
                    3) Listar autores
                    4) Listar autores vivos em determinado ano
                    5) Listar livros por idioma (ex.: en, pt, es, fr)
                    0) Sair
                    Escolha: """);
            op = scanner.nextLine().trim();

            switch (op) {
                case "1" -> buscarLivroPorTitulo();
                case "2" -> listarLivros();
                case "3" -> listarAutores();
                case "4" -> listarAutoresVivosNoAno();
                case "5" -> listarLivrosPorIdioma();
                case "0" -> System.out.println("Até mais!");
                default -> System.out.println("Opção inválida.");
            }
        } while (!op.equals("0"));
    }

    // ========== OPÇÃO 1 ==========
    private void buscarLivroPorTitulo() throws Exception {
        System.out.print("Digite o título: ");
        String titulo = scanner.nextLine().trim();
        if (titulo.isEmpty()) {
            System.out.println("Título vazio.");
            return;
        }

        String url = "https://gutendex.com/books/?search=" +
                URLEncoder.encode(titulo, StandardCharsets.UTF_8);
        String json = http.obterDados(url);

        // Mapeia apenas o que precisamos do JSON
        RespostaApi resposta = mapper.readValue(json, RespostaApi.class);
        if (resposta.getResults() == null || resposta.getResults().isEmpty()) {
            System.out.println("Nenhum resultado na API.");
            return;
        }

        LivroApi primeiro = resposta.getResults().get(0);

        // Pega primeiro autor (regra do desafio)
        br.com.literalura.Autor autor = null;
        if (primeiro.getAuthors() != null && !primeiro.getAuthors().isEmpty()) {
            AutorApi a = primeiro.getAuthors().get(0);
            autor = autorRepository.findByNomeIgnoreCase(a.getName())
                    .orElseGet(() -> {
                        br.com.literalura.Autor novo = new br.com.literalura.Autor();
                        novo.setNome(a.getName());
                        novo.setAnoNascimento(a.getBirthYear());
                        novo.setAnoFalecimento(a.getDeathYear());
                        return autorRepository.save(novo);
                    });
        }

        // Pega primeiro idioma (regra do desafio)
        String idioma = "unknown";
        if (primeiro.getLanguages() != null && !primeiro.getLanguages().isEmpty()) {
            idioma = primeiro.getLanguages().get(0).toLowerCase();
        }

        // Evita duplicar livro pelo título
        Optional<br.com.literalura.Livro> jaExiste = livroRepository.findByTituloIgnoreCase(primeiro.getTitle());
        br.com.literalura.Livro livro = jaExiste.orElseGet(br.com.literalura.Livro::new);

        livro.setTitulo(primeiro.getTitle());
        livro.setIdioma(idioma);
        livro.setDownloads(Optional.ofNullable(primeiro.getDownloadCount()).orElse(0));
        livro.setAutor(autor);

        livroRepository.save(livro);

        System.out.println("\nSalvo/atualizado com sucesso:");
        System.out.println(livro);
    }

    // ========== OPÇÃO 2 ==========
    private void listarLivros() {
        List<br.com.literalura.Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro no banco.");
            return;
        }
        livros.forEach(System.out::println);
    }

    // ========== OPÇÃO 3 ==========
    private void listarAutores() {
        List<br.com.literalura.Autor> autores = autorRepository.findAll();
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor no banco.");
            return;
        }
        autores.forEach(a -> {
            System.out.println(a);
            // Se quiser, mostre os livros do autor:
            List<br.com.literalura.Livro> doAutor = livroRepository.findByAutorId(a.getId());
            if (!doAutor.isEmpty()) {
                System.out.println("  Livros: ");
                doAutor.forEach(l -> System.out.println("   - " + l.getTitulo()));
            }
        });
    }

    // ========== OPÇÃO 4 ==========
    private void listarAutoresVivosNoAno() {
        try {
            System.out.print("Informe o ano (ex.: 1900): ");
            int ano = Integer.parseInt(scanner.nextLine().trim());
            List<br.com.literalura.Autor> vivos = autorRepository.autoresVivosNoAno(ano);
            if (vivos.isEmpty()) {
                System.out.println("Nenhum autor vivo em " + ano + ".");
                return;
            }
            vivos.forEach(System.out::println);
        } catch (NumberFormatException e) {
            System.out.println("Ano inválido.");
        }
    }

    // ========== OPÇÃO 5 ==========
    private void listarLivrosPorIdioma() {
        System.out.print("Informe idioma (en, pt, es, fr...): ");
        String idioma = scanner.nextLine().trim().toLowerCase();
        if (idioma.isEmpty()) {
            System.out.println("Idioma vazio.");
            return;
        }
        List<Livro> livros = livroRepository.findByIdiomaIgnoreCase(idioma);
        long total = livroRepository.countByIdiomaIgnoreCase(idioma);

        if (livros.isEmpty()) {
            System.out.println("Não há livros no idioma '" + idioma + "'.");
            return;
        }
        System.out.println("Total no idioma '" + idioma + "': " + total);
        livros.forEach(System.out::println);
    }

    // ======== Classes simples p/ mapear o JSON da Gutendex ========

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RespostaApi {
        private List<LivroApi> results;
        public List<LivroApi> getResults() { return results; }
        public void setResults(List<LivroApi> results) { this.results = results; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LivroApi {
        private String title;
        private List<AutorApi> authors;
        private List<String> languages;
        @JsonProperty("download_count")
        private Integer downloadCount;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<AutorApi> getAuthors() { return authors; }
        public void setAuthors(List<AutorApi> authors) { this.authors = authors; }
        public List<String> getLanguages() { return languages; }
        public void setLanguages(List<String> languages) { this.languages = languages; }
        public Integer getDownloadCount() { return downloadCount; }
        public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AutorApi {
        private String name;
        @JsonProperty("birth_year")
        private Integer birthYear;
        @JsonProperty("death_year")
        private Integer deathYear;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getBirthYear() { return birthYear; }
        public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
        public Integer getDeathYear() { return deathYear; }
        public void setDeathYear(Integer deathYear) { thisdeathYear = deathYear; }
    }
}
