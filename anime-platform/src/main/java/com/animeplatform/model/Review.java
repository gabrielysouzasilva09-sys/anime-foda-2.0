package com.animeplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Entidade de resenha/comentário.
 *
 * Está vinculada ao {@link User} que a escreveu (campo {@code author}),
 * o qual é definido no servidor a partir do token JWT — o corpo do
 * pedido nunca decide quem é o autor (evita falsificação de identidade).
 *
 * O campo {@code userName} é mantido como nome de exibição desnormalizado
 * para o feed, mas é sempre preenchido a partir do utilizador autenticado.
 */
@Entity
@Table(name = "reviews")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Review {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "anime_id", length = 64)
    private String animeId;

    @Column(name = "anime_title")
    private String animeTitle;

    @Column(name = "anime_image", length = 1000)
    private String animeImage;

    /** Nome de exibição do autor (derivado do JWT). */
    @Column(name = "user_name")
    private String userName;

    /** Autor da resenha — definido no servidor a partir do token JWT. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(name = "rating")
    private int rating;

    @Column(name = "comment", length = 4000)
    private String comment;

    @Column(name = "contains_spoiler")
    private boolean containsSpoiler;

    /** Data de criação como String ISO-8601 (ex: "2026-10-09T10:43:00"). */
    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "likes")
    private int likes;

    // ── Construtor padrão obrigatório para o Jackson/JPA ──
    public Review() {}

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null || createdAt.isBlank()) {
            createdAt = ISO_FMT.format(Instant.now());
        }
    }

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

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

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
