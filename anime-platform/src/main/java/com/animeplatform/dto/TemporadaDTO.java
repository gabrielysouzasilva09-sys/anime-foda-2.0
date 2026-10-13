package com.animeplatform.dto;

import com.animeplatform.model.Temporada;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO de saída para uma temporada.
 *
 * Exemplo de resposta do GET /api/animes/{id}/seasons:
 * {
 *   "id":           "1-s1",
 *   "animeId":      "1",
 *   "numero":       1,
 *   "nome":         "Temporada 1",
 *   "totalEpisodios": 26,
 *   "episodioIds":  ["1-1", "1-2", ...]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemporadaDTO {

    private String       id;
    private String       animeId;
    private int          numero;
    private String       nome;
    private int          totalEpisodios;
    private List<String> episodioIds;

    // ── Construtor padrão ──
    public TemporadaDTO() {}

    /** Converte uma Temporada para DTO. */
    public TemporadaDTO(Temporada temporada) {
        this.id             = temporada.getId();
        this.animeId        = temporada.getAnimeId();
        this.numero         = temporada.getNumero();
        this.nome           = temporada.getNome();
        this.episodioIds    = temporada.getEpisodioIds();
        this.totalEpisodios = temporada.getEpisodioIds() != null
                ? temporada.getEpisodioIds().size()
                : 0;
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

    public int getTotalEpisodios() { return totalEpisodios; }
    public void setTotalEpisodios(int totalEpisodios) { this.totalEpisodios = totalEpisodios; }

    public List<String> getEpisodioIds() { return episodioIds; }
    public void setEpisodioIds(List<String> episodioIds) { this.episodioIds = episodioIds; }
}
