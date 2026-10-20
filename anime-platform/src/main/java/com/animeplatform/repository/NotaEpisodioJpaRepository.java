package com.animeplatform.repository;

import com.animeplatform.model.NotaEpisodio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acesso direto à tabela episode_ratings. */
@Repository
public interface NotaEpisodioJpaRepository extends JpaRepository<NotaEpisodio, String> {

    Optional<NotaEpisodio> findByEpisodio_IdAndUserNameIgnoreCase(String episodioId, String userName);
}
