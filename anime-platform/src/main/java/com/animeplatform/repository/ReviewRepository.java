package com.animeplatform.repository;

import com.animeplatform.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de resenhas (PostgreSQL via Spring Data JPA).
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    /** Resenhas ordenadas pela data de criação, mais recentes primeiro. */
    List<Review> findAllByOrderByCreatedAtDesc();

    /** Resenhas de um anime específico, mais recentes primeiro. */
    List<Review> findByAnimeIdOrderByCreatedAtDesc(String animeId);
}
