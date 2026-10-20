package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um episódio de um anime.
 *
 * Os dados base (numero, titulo, sinopse) vêm da Kitsu API
 * e ficam persistidos no PostgreSQL, enriquecidos com as notas
 * dos utilizadores (tabela episode_ratings).
 */
@Entity
@Table(name = "episodes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Episodio {

    /** ID composto: "{animeId}-{numero}" — ex: "1-1", "42-3" */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** ID do anime na Kitsu (String, ex: "1", "42") */
    @Column(name = "anime_id", nullable = false, length = 64)
    private String animeId;

    /** ID da temporada a que pertence este episódio (ex: "1-s1"). Null se sem temporada. */
    @Column(name = "season_id", length = 64)
    private String seasonId;

    @Column(name = "numero")
    private int numero;

    @Column(name = "titulo")
    private String titulo;

    @Column(name = "sinopse", length = 2000)
    private String sinopse;

    /** Média das notas dos utilizadores (0.0 se sem notas) */
    @Column(name = "nota_media")
    private double notaMedia;

    /** Total de avaliações recebidas */
    @Column(name = "total_notas")
    private int totalNotas;

    /** Lista de notas individuais deste episódio */
    @OneToMany(mappedBy = "episodio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<NotaEpisodio> notas = new ArrayList<>();

    // ── Construtor padrão obrigatório para o JPA/Jackson ──
    public Episodio() {}

    public Episodio(String animeId, int numero, String titulo, String sinopse) {
        this.id       = animeId + "-" + numero;
        this.animeId  = animeId;
        this.numero   = numero;
        this.titulo   = titulo;
        this.sinopse  = sinopse;
        this.notaMedia = 0.0;
        this.totalNotas = 0;
    }

    // ── Getters e Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getSeasonId() { return seasonId; }
    public void setSeasonId(String seasonId) { this.seasonId = seasonId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getSinopse() { return sinopse; }
    public void setSinopse(String sinopse) { this.sinopse = sinopse; }

    public double getNotaMedia() { return notaMedia; }
    public void setNotaMedia(double notaMedia) { this.notaMedia = notaMedia; }

    public int getTotalNotas() { return totalNotas; }
    public void setTotalNotas(int totalNotas) { this.totalNotas = totalNotas; }

    public List<NotaEpisodio> getNotas() { return notas; }
    public void setNotas(List<NotaEpisodio> notas) { this.notas = notas != null ? notas : new ArrayList<>(); }

    /**
     * Recalcula notaMedia e totalNotas a partir da lista de notas.
     * Deve ser chamado sempre que uma nova NotaEpisodio é adicionada.
     */
    public void recalcularMedia() {
        if (notas == null || notas.isEmpty()) {
            this.notaMedia  = 0.0;
            this.totalNotas = 0;
            return;
        }
        double soma = notas.stream().mapToInt(NotaEpisodio::getNota).sum();
        this.totalNotas = notas.size();
        // Arredonda a 1 casa decimal
        this.notaMedia = Math.round((soma / totalNotas) * 10.0) / 10.0;
    }
}
