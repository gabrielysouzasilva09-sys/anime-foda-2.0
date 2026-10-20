package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * Representa a nota que um utilizador deu a um episódio.
 *
 * Uma nota está sempre associada a um {@link Episodio}.
 * O {@code userName} é definido no servidor a partir do token JWT.
 *
 * Regra: um mesmo userName só pode ter uma nota por episódio
 * (garantida por índice único na tabela episode_ratings).
 */
@Entity
@Table(name = "episode_ratings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rating_episode_user", columnNames = {"episodio_id", "user_name"})
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotaEpisodio {

    /** UUID gerado automaticamente */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** Episódio ao qual esta nota pertence. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episodio_id", nullable = false)
    private Episodio episodio;

    /** ID do anime na Kitsu (desnormalizado para queries rápidas). */
    @Column(name = "anime_id", length = 64)
    private String animeId;

    /** Nome do utilizador (derivado do token JWT). */
    @Column(name = "user_name")
    private String userName;

    /** Nota de 1 a 10 */
    @Column(name = "nota")
    private int nota;

    /** Data/hora da avaliação em formato ISO-8601 */
    @Column(name = "created_at")
    private String createdAt;

    // ── Construtor padrão obrigatório para o JPA/Jackson ──
    public NotaEpisodio() {}

    // ── Getters e Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Episodio getEpisodio() { return episodio; }
    public void setEpisodio(Episodio episodio) { this.episodio = episodio; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getNota() { return nota; }
    public void setNota(int nota) { this.nota = nota; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
