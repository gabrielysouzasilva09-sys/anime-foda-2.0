package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Modelo de resenha.
 *
 * Campos canónicos alinhados com o payload do frontend:
 *   animeId, animeTitle, animeImage, userName, rating,
 *   comment, containsSpoiler, createdAt, id, likes
 *
 * @JsonAlias permite receber tanto os nomes antigos quanto os novos
 * sem precisar alterar o HTML.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Review {

    private String id;

    private String animeId;

    private String animeTitle;

    private String animeImage;

    // Aceita "userName" (novo) ou "username" (legado do frontend)
    @JsonAlias({"username"})
    private String userName;

    private int rating;

    // Aceita "comment" (novo) ou "review" (legado do frontend)
    @JsonAlias({"review"})
    private String comment;

    // Aceita "containsSpoiler" (novo) ou "hasSpoiler" (legado do frontend)
    @JsonAlias({"hasSpoiler"})
    private boolean containsSpoiler;

    // Data de criação como String ISO-8601 (ex: "2026-10-09T10:43:00")
    private String createdAt;

    private int likes;

    // ── Construtor padrão obrigatório para o Jackson ──
    public Review() {}

    // ── Getters e Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getAnimeTitle() { return animeTitle; }
    public void setAnimeTitle(String animeTitle) { this.animeTitle = animeTitle; }

    public String getAnimeImage() { return animeImage; }
    public void setAnimeImage(String animeImage) { this.animeImage = animeImage; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isContainsSpoiler() { return containsSpoiler; }
    public void setContainsSpoiler(boolean containsSpoiler) { this.containsSpoiler = containsSpoiler; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
}
