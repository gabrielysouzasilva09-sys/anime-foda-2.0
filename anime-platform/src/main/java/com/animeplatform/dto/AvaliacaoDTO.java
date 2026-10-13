package com.animeplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de entrada para avaliar um episódio.
 *
 * Payload esperado no POST /api/episodes/review:
 * {
 *   "episodioId": "1-3",        // "{animeId}-{numero}" — obrigatório
 *   "nota":       8.5,          // 1.0 a 10.0 — obrigatório
 *   "animeId":    "1",          // ID do anime na Kitsu — obrigatório
 *   "userName":   "Naruto Fan"  // opcional — padrão "Anônimo"
 * }
 *
 * A nota é recebida como Double para aceitar meias notas (ex: 8.5),
 * mas será arredondada para o inteiro mais próximo ao guardar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvaliacaoDTO {

    private String episodioId;
    private Double nota;
    private String animeId;
    private String userName;

    // ── Construtor padrão obrigatório para o Jackson ──
    public AvaliacaoDTO() {}

    // ── Getters e Setters ──

    public String getEpisodioId() { return episodioId; }
    public void setEpisodioId(String episodioId) { this.episodioId = episodioId; }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }

    public String getAnimeId() { return animeId; }
    public void setAnimeId(String animeId) { this.animeId = animeId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // ── Validação ──

    /** Devolve true se os campos obrigatórios estão presentes e válidos. */
    public boolean isValid() {
        return episodioId != null && !episodioId.isBlank()
            && animeId    != null && !animeId.isBlank()
            && nota       != null && nota >= 1.0 && nota <= 10.0;
    }

    /** Converte a nota Double para int (arredondado). */
    public int getNotaInt() {
        return nota != null ? (int) Math.round(nota) : 0;
    }
}
