package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um episódio de um anime.
 *
 * Os dados base (numero, titulo, sinopse) vêm da Kitsu API
 * e são guardados localmente no episodes.json para enriquecimento
 * com as notas dos utilizadores.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Episodio {

    /** ID composto: "{animeId}-{numero}" — ex: "1-1", "42-3" */
    private String id;

    /** ID do anime na Kitsu (String, ex: "1", "42") */
    private String animeId;

    /** ID da temporada a que pertence este episódio (ex: "1-s1"). Null se sem temporada. */
    private String seasonId;

    private int numero;
    private String titulo;
    private String sinopse;

    /** Média das notas dos utilizadores (0.0 se sem notas) */
    private double notaMedia;

    /** Total de avaliações recebidas */
    private int totalNotas;

    /** Lista de notas individuais deste episódio */
    private List<NotaEpisodio> notas = new ArrayList<>();

    // ── Construtor padrão obrigatório para o Jackson ──
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
