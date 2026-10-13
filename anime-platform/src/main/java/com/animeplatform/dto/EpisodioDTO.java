package com.animeplatform.dto;

import com.animeplatform.model.Episodio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de saída para um episódio.
 *
 * Expõe apenas os dados relevantes para o frontend,
 * incluindo a nota do utilizador actual (notaUsuario)
 * que é preenchida pelo controller com base no userName da sessão.
 *
 * Exemplo de resposta do GET /api/animes/{id}/episodes:
 * {
 *   "id":          "1-1",
 *   "animeId":     "1",
 *   "numero":      1,
 *   "titulo":      "Entra: Naruto Uzumaki!",
 *   "sinopse":     "...",
 *   "notaMedia":   8.5,
 *   "totalNotas":  12,
 *   "notaUsuario": 9.0   ← nota do utilizador actual (null se ainda não avaliou)
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EpisodioDTO {

    private String id;
    private String animeId;
    private String seasonId;
    private int    numero;
    private String titulo;
    private String sinopse;
    private double notaMedia;
    private int    totalNotas;

    /**
     * Nota do utilizador actual para este episódio.
     * null se o utilizador ainda não avaliou.
     */
    private Double notaUsuario;

    // ── Construtor padrão ──
    public EpisodioDTO() {}

    /**
     * Construtor de conveniência — converte um Episodio para DTO
     * sem nota de utilizador (notaUsuario = null).
     */
    public EpisodioDTO(Episodio ep) {
        this.id         = ep.getId();
        this.animeId    = ep.getAnimeId();
        this.seasonId   = ep.getSeasonId();
        this.numero     = ep.getNumero();
        this.titulo     = ep.getTitulo();
        this.sinopse    = ep.getSinopse();
        this.notaMedia  = ep.getNotaMedia();
        this.totalNotas = ep.getTotalNotas();
        this.notaUsuario = null;
    }

    /**
     * Construtor de conveniência — converte um Episodio para DTO
     * com a nota do utilizador preenchida.
     *
     * @param ep       episódio de origem
     * @param userName nome do utilizador para procurar a nota
     */
    public EpisodioDTO(Episodio ep, String userName) {
        this(ep);
        if (userName != null && ep.getNotas() != null) {
            ep.getNotas().stream()
                .filter(n -> userName.equalsIgnoreCase(n.getUserName()))
                .findFirst()
                .ifPresent(n -> this.notaUsuario = (double) n.getNota());
        }
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

    public Double getNotaUsuario() { return notaUsuario; }
    public void setNotaUsuario(Double notaUsuario) { this.notaUsuario = notaUsuario; }
}
