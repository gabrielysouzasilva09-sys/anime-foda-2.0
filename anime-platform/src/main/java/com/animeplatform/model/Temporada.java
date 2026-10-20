package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma temporada de um anime.
 *
 * ID gerado como "{animeId}-s{numero}" — ex: "1-s1", "42-s2".
 * Os episódios são referenciados pelos seus IDs compostos
 * (tabela season_episodes).
 */
@Entity
@Table(name = "seasons")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Temporada {

    /** ID composto: "{animeId}-s{numero}" — ex: "1-s1" */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** ID do anime na Kitsu */
    @Column(name = "anime_id", nullable = false, length = 64)
    private String animeId;

    /** Número da temporada (1, 2, 3...) */
    @Column(name = "numero")
    private int numero;

    /** Nome/título da temporada (ex: "Temporada 1", "Arco de Chunin") */
    @Column(name = "nome")
    private String nome;

    /** IDs dos episódios pertencentes a esta temporada */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "season_episodes", joinColumns = @JoinColumn(name = "season_id"))
    @Column(name = "episode_id", length = 64)
    private List<String> episodioIds = new ArrayList<>();

    // ── Construtor padrão obrigatório para o JPA/Jackson ──
    public Temporada() {}

    public Temporada(String animeId, int numero, String nome) {
        this.id      = animeId + "-s" + numero;
        this.animeId = animeId;
        this.numero  = numero;
        this.nome    = nome;
    }

    // ── Getters e Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public List<String> getEpisodioIds() { return episodioIds; }
    public void setEpisodioIds(List<String> episodioIds) {
        this.episodioIds = episodioIds != null ? episodioIds : new ArrayList<>();
    }
}
