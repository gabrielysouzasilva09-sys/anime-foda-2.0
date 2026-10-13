package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representa a nota que um utilizador deu a um episódio.
 *
 * Uma nota está sempre associada a um Episodio (via episodioId)
 * e a um anime (via animeId) para facilitar queries.
 *
 * Regra: um mesmo userName só pode ter uma nota por episódio.
 * Se avaliar novamente, a nota é actualizada (upsert).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotaEpisodio {

    /** UUID gerado automaticamente */
    private String id;

    /** ID do episódio: "{animeId}-{numero}" */
    private String episodioId;

    /** ID do anime na Kitsu */
    private String animeId;

    /** Nome do utilizador (pode ser "Anônimo") */
    private String userName;

    /** Nota de 1 a 10 */
    private int nota;

    /** Data/hora da avaliação em formato ISO-8601 */
    private String createdAt;

    // ── Construtor padrão obrigatório para o Jackson ──
    public NotaEpisodio() {}

    // ── Getters e Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEpisodioId() { return episodioId; }
    public void setEpisodioId(String episodioId) { this.episodioId = episodioId; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getNota() { return nota; }
    public void setNota(int nota) { this.nota = nota; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
