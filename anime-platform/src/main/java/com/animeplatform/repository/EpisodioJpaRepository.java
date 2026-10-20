package com.animeplatform.repository;

import com.animeplatform.model.Episodio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acesso direto à tabela episodes. */
@Repository
public interface EpisodioJpaRepository extends JpaRepository<Episodio, String> {

    List<Episodio> findByAnimeIdOrderByNumeroAsc(String animeId);
}
