package com.animeplatform.repository;

import com.animeplatform.model.Review;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReviewRepository {

    private static final String DATA_FILE =
            System.getProperty("user.dir") + File.separator + "reviews.json";

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private List<Review> reviews = new ArrayList<>();

    private final ObjectMapper objectMapper;

    public ReviewRepository() {
        this.objectMapper = new ObjectMapper();
        // Não precisamos do módulo JSR310 pois createdAt agora é String
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        ensureFileExists();
        loadReviews();
    }

    // ── Garante que pasta e ficheiro existem ──────────────────────────────────
    private void ensureFileExists() {
        try {
            Path filePath = Paths.get(DATA_FILE);
            Path parentDir = filePath.getParent();

            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("[ReviewRepository] Pasta criada: " + parentDir.toAbsolutePath());
            }

            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
                System.out.println("[ReviewRepository] Ficheiro criado: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[ReviewRepository] ERRO ao criar ficheiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Carrega do disco ──────────────────────────────────────────────────────
    private void loadReviews() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists() && file.length() > 2) { // > 2 para ignorar ficheiros com só "[]"
                reviews = objectMapper.readValue(file, new TypeReference<List<Review>>() {});
                System.out.println("[ReviewRepository] " + reviews.size() + " resenha(s) carregada(s) de " + DATA_FILE);
            } else {
                reviews = new ArrayList<>();
                System.out.println("[ReviewRepository] Sem resenhas — lista vazia.");
            }
        } catch (IOException e) {
            System.err.println("[ReviewRepository] ERRO ao carregar reviews.json: " + e.getMessage());
            e.printStackTrace();
            reviews = new ArrayList<>();
        }
    }

    // ── Persiste no disco ─────────────────────────────────────────────────────
    private void saveReviews() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DATA_FILE), reviews);
            System.out.println("[ReviewRepository] reviews.json guardado — " + reviews.size() + " resenha(s).");
        } catch (IOException e) {
            System.err.println("[ReviewRepository] ERRO ao guardar reviews.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    public List<Review> findAll() {
        return new ArrayList<>(reviews);
    }

    /** Ordena por createdAt decrescente (mais recentes primeiro). */
    public List<Review> findAllOrderByCreatedAtDesc() {
        List<Review> sorted = new ArrayList<>(reviews);
        sorted.sort((a, b) -> {
            String ta = a.getCreatedAt();
            String tb = b.getCreatedAt();
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta); // comparação lexicográfica funciona para ISO-8601
        });
        return sorted;
    }

    public Review save(Review review) {
        // Garante ID único
        if (review.getId() == null || review.getId().isBlank()) {
            review.setId(UUID.randomUUID().toString());
        }
        // Garante data de criação
        if (review.getCreatedAt() == null || review.getCreatedAt().isBlank()) {
            review.setCreatedAt(ISO_FMT.format(Instant.now()));
        }
        // Garante likes = 0 para resenhas novas
        // (primitivo int já começa em 0, mas por clareza:)
        reviews.add(review);
        saveReviews();
        return review;
    }

    public Optional<Review> findById(String id) {
        return reviews.stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst();
    }

    public boolean likeById(String id) {
        Optional<Review> found = findById(id);
        if (found.isPresent()) {
            Review r = found.get();
            r.setLikes(r.getLikes() + 1);
            saveReviews();
            return true;
        }
        return false;
    }
}
