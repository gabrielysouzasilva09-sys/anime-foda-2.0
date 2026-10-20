package com.animeplatform.repository;

import com.animeplatform.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acesso direto à tabela seasons. */
@Repository
public interface TemporadaJpaRepository extends JpaRepository<Temporada, String> {

    List<Temporada> findByAnimeIdOrderByNumeroAsc(String animeId);
}
