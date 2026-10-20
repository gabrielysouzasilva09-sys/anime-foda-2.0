package com.animeplatform.controller;

import com.animeplatform.model.Review;
import com.animeplatform.model.User;
import com.animeplatform.repository.ReviewRepository;
import com.animeplatform.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller de resenhas/comentários.
 *
 * O autor é sempre derivado do token JWT (utilizador autenticado) —
 * o corpo do pedido NUNCA decide quem escreveu a resenha, o que impede
 * que um utilizador comente em nome de outro.
 */
@RestController
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final AuthService authService;

    public ReviewController(ReviewRepository reviewRepository, AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.authService = authService;
    }

    // ── GET /api/reviews ──────────────────────────────────────────────────────
    /**
     * Lista as resenhas guardadas no servidor.
     * Se for passado ?animeId=..., devolve apenas as resenhas desse anime.
     */
    @GetMapping("/api/reviews")
    public ResponseEntity<?> getReviews(@RequestParam(required = false) String animeId) {
        try {
            List<Review> reviews = (animeId != null && !animeId.isBlank())
                    ? reviewRepository.findByAnimeIdOrderByCreatedAtDesc(animeId)
                    : reviewRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar resenhas: " + e.getMessage()));
        }
    }

    // ── POST /api/reviews ─────────────────────────────────────────────────────
    /**
     * Cria uma resenha. Requer token JWT válido.
     * O campo userName do corpo é ignorado — o autor vem do token.
     */
    @PostMapping("/api/reviews")
    public ResponseEntity<?> addReview(@RequestBody Review incoming, Authentication authentication) {
        try {
            // Validações mínimas
            if (incoming.getRating() < 1 || incoming.getRating() > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Campo 'rating' deve ser entre 1 e 10"));
            }
            if (incoming.getComment() == null || incoming.getComment().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Campo 'comment' (ou 'review') é obrigatório"));
            }

            // Autor derivado do token — impossível falsificar
            User author = authService.findByUsername(authentication.getName());

            incoming.setId(null);          // forçado a gerar novo id
            incoming.setCreatedAt(null);   // data definida no servidor
            incoming.setLikes(0);          // sem likes iniciais
            incoming.setAuthor(author);
            incoming.setUserName(author.getUsername());

            // Valores padrão para campos opcionais
            if (incoming.getAnimeTitle() == null) {
                incoming.setAnimeTitle("Sem título");
            }
            if (incoming.getAnimeImage() == null) {
                incoming.setAnimeImage("");
            }

            Review saved = reviewRepository.save(incoming);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro interno ao salvar resenha: " + e.getMessage()));
        }
    }

    // ── POST /api/reviews/{id}/like ───────────────────────────────────────────
    @PostMapping("/api/reviews/{id}/like")
    public ResponseEntity<?> likeReview(@PathVariable String id) {
        try {
            Review review = reviewRepository.findById(id).orElse(null);
            if (review == null) {
                return ResponseEntity.notFound().build();
            }
            review.setLikes(review.getLikes() + 1);
            reviewRepository.save(review);
            return ResponseEntity.ok(Map.of("likes", review.getLikes()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao curtir resenha: " + e.getMessage()));
        }
    }
}
