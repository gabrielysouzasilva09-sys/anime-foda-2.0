package com.animeplatform.controller;

import com.animeplatform.dto.AvaliacaoDTO;
import com.animeplatform.dto.EpisodioDTO;
import com.animeplatform.model.Episodio;
import com.animeplatform.repository.EpisodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller de episódios e avaliações.
 *
 * GET  /api/animes/{animeId}/episodes  — lista episódios com notas
 * POST /api/episodes/review            — guarda/actualiza nota de um episódio
 *
 * O autor da avaliação é derivado do token JWT (utilizador autenticado),
 * nunca do corpo do pedido.
 */
@RestController
public class EpisodeController {

    @Autowired
    private EpisodeRepository episodeRepository;

    // ── GET /api/animes/{animeId}/episodes ────────────────────────────────────

    /**
     * Devolve a lista de episódios de um anime, ordenada por número.
     *
     * Parâmetro opcional de query: ?userName=NomeDoUtilizador
     * Se fornecido, cada EpisodioDTO terá o campo notaUsuario preenchido
     * com a nota que esse utilizador já deu ao episódio (null se ainda não avaliou).
     *
     * Exemplos:
     *   GET /api/animes/1/episodes
     *   GET /api/animes/1/episodes?userName=Naruto+Fan
     *
     * Resposta 200:
     * [
     *   {
     *     "id": "1-1", "animeId": "1", "numero": 1, "titulo": "...",
     *     "notaMedia": 8.5, "totalNotas": 3, "notaUsuario": 9.0
     *   }, ...
     * ]
     */
    @GetMapping("/api/animes/{animeId}/episodes")
    public ResponseEntity<?> getEpisodesByAnime(
            @PathVariable String animeId,
            @RequestParam(required = false) String userName,
            Authentication authentication) {
        try {
            // Se houver token JWT válido, o utilizador autenticado tem prioridade
            final String effectiveUser = resolveUserName(authentication, userName);

            System.out.println("[EpisodeController] GET /api/animes/" + animeId + "/episodes"
                    + (effectiveUser != null ? "?userName=" + effectiveUser : ""));

            List<Episodio> episodios = episodeRepository.findByAnimeId(animeId);

            // Converte para DTOs, preenchendo notaUsuario se houver utilizador
            List<EpisodioDTO> dtos = episodios.stream()
                    .map(ep -> effectiveUser != null
                            ? new EpisodioDTO(ep, effectiveUser)
                            : new EpisodioDTO(ep))
                    .collect(Collectors.toList());

            System.out.println("[EpisodeController] " + dtos.size() + " episódio(s) devolvido(s).");
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar episódios: " + e.getMessage()));
        }
    }

    // ── POST /api/episodes/review ─────────────────────────────────────────────

    /**
     * Guarda ou actualiza a nota de um utilizador para um episódio.
     * Se o mesmo userName avaliar o mesmo episódio novamente, a nota é substituída.
     *
     * Payload (AvaliacaoDTO):
     * {
     *   "episodioId": "1-3",        // obrigatório
     *   "nota":       8.5,          // obrigatório — 1.0 a 10.0 (aceita meias notas)
     *   "animeId":    "1",          // obrigatório
     *   "userName":   "Naruto Fan"  // opcional — padrão "Anônimo"
     * }
     *
     * Resposta 200: EpisodioDTO actualizado com nova notaMedia, totalNotas e notaUsuario
     * Resposta 400: campos obrigatórios em falta ou nota fora do intervalo
     * Resposta 500: erro interno com mensagem
     */
    @PostMapping("/api/episodes/review")
    public ResponseEntity<?> reviewEpisode(@RequestBody AvaliacaoDTO avaliacao,
                                           Authentication authentication) {
        try {
            // Autor derivado do token JWT — o userName do corpo é ignorado
            String userName = authentication.getName();

            System.out.println("[EpisodeController] POST /api/episodes/review");
            System.out.println("  episodioId = " + avaliacao.getEpisodioId());
            System.out.println("  animeId    = " + avaliacao.getAnimeId());
            System.out.println("  userName   = " + userName + " (do token)");
            System.out.println("  nota       = " + avaliacao.getNota());

            // Validação via método do DTO
            if (!avaliacao.isValid()) {
                String motivo = avaliacao.getEpisodioId() == null || avaliacao.getEpisodioId().isBlank()
                        ? "Campo 'episodioId' é obrigatório"
                        : avaliacao.getAnimeId() == null || avaliacao.getAnimeId().isBlank()
                        ? "Campo 'animeId' é obrigatório"
                        : "Campo 'nota' deve ser entre 1.0 e 10.0";
                return ResponseEntity.badRequest().body(Map.of("error", motivo));
            }

            // Guarda/actualiza a nota (nota Double → int arredondado)
            Episodio updated = episodeRepository.saveOrUpdateNota(
                    avaliacao.getAnimeId(),
                    avaliacao.getEpisodioId(),
                    userName,
                    avaliacao.getNotaInt()
            );

            // Devolve o DTO com notaUsuario preenchida
            EpisodioDTO dto = new EpisodioDTO(updated, userName);

            System.out.println("[EpisodeController] OK — notaMedia=" + dto.getNotaMedia()
                    + " totalNotas=" + dto.getTotalNotas()
                    + " notaUsuario=" + dto.getNotaUsuario());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro interno ao guardar nota: " + e.getMessage()));
        }
    }

    /**
     * Decide qual o nome de utilizador a usar na leitura:
     * o utilizador autenticado (token JWT) tem prioridade sobre o parâmetro de query.
     */
    private String resolveUserName(Authentication authentication, String userName) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return (userName != null && !userName.isBlank()) ? userName : null;
    }
}
