package com.animeplatform.controller;

import com.animeplatform.model.Review;
import com.animeplatform.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    // ── GET /api/reviews ──────────────────────────────────────────────────────
    @GetMapping("/api/reviews")
    public ResponseEntity<?> getReviews() {
        try {
            List<Review> reviews = reviewRepository.findAllOrderByCreatedAtDesc();
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar resenhas: " + e.getMessage()));
        }
    }

    // ── POST /api/reviews ─────────────────────────────────────────────────────
    /**
     * O Jackson deserializa o JSON directamente para Review.
     * @JsonAlias na model garante compatibilidade com os nomes legados
     * enviados pelo frontend (username, review, hasSpoiler).
     */
    @PostMapping("/api/reviews")
    public ResponseEntity<?> addReview(@RequestBody Review incoming) {
        try {
            System.out.println("[ReviewController] POST /api/reviews recebido");
            System.out.println("  animeId       = " + incoming.getAnimeId());
            System.out.println("  animeTitle    = " + incoming.getAnimeTitle());
            System.out.println("  userName      = " + incoming.getUserName());
            System.out.println("  rating        = " + incoming.getRating());
            System.out.println("  comment       = " + incoming.getComment());
            System.out.println("  containsSpoiler = " + incoming.isContainsSpoiler());

            // Validações mínimas
            if (incoming.getRating() < 1 || incoming.getRating() > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Campo 'rating' deve ser entre 1 e 10"));
            }
            if (incoming.getComment() == null || incoming.getComment().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Campo 'comment' (ou 'review') é obrigatório"));
            }

            // Valores padrão para campos opcionais
            if (incoming.getUserName() == null || incoming.getUserName().trim().isEmpty()) {
                incoming.setUserName("Anônimo");
            }
            if (incoming.getAnimeTitle() == null) {
                incoming.setAnimeTitle("Sem título");
            }
            if (incoming.getAnimeImage() == null) {
                incoming.setAnimeImage("");
            }

            Review saved = reviewRepository.save(incoming);
            System.out.println("[ReviewController] Resenha salva — ID: " + saved.getId());
            return ResponseEntity.status(201).body(saved);

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
            boolean success = reviewRepository.likeById(id);
            if (success) {
                int likes = reviewRepository.findById(id)
                        .map(Review::getLikes)
                        .orElse(0);
                return ResponseEntity.ok(Map.of("likes", likes));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao curtir resenha: " + e.getMessage()));
        }
    }
}
