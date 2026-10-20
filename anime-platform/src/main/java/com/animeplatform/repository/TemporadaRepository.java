package com.animeplatform.repository;

import com.animeplatform.model.Temporada;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de temporadas, persistido no PostgreSQL.
 *
 * Mantém a mesma API pública de antes (findByAnimeId, findById,
 * saveOrUpdate, addEpisodioToTemporada) mas delegando no
 * TemporadaJpaRepository (Spring Data JPA).
 */
@Repository
public class TemporadaRepository {

    private final TemporadaJpaRepository repo;

    public TemporadaRepository(TemporadaJpaRepository repo) {
        this.repo = repo;
    }

    /** Devolve todas as temporadas de um anime, ordenadas pelo número. */
    @Transactional(readOnly = true)
    public List<Temporada> findByAnimeId(String animeId) {
        return repo.findByAnimeIdOrderByNumeroAsc(animeId);
    }

    /** Encontra uma temporada pelo seu ID composto "{animeId}-s{numero}". */
    @Transactional(readOnly = true)
    public Optional<Temporada> findById(String id) {
        return repo.findById(id);
    }

    /**
     * Cria ou actualiza uma temporada.
     * Se já existir uma com o mesmo id, o nome e a lista de episódios são actualizados.
     */
    @Transactional
    public Temporada saveOrUpdate(Temporada temporada) {
        if (temporada.getId() == null || temporada.getId().isBlank()) {
            temporada.setId(temporada.getAnimeId() + "-s" + temporada.getNumero());
        }

        Optional<Temporada> existing = repo.findById(temporada.getId());
        if (existing.isPresent()) {
            Temporada t = existing.get();
            t.setNumero(temporada.getNumero());
            t.setNome(temporada.getNome());
            t.getEpisodioIds().clear();
            t.getEpisodioIds().addAll(temporada.getEpisodioIds());
            return repo.save(t);
        } else {
            return repo.save(temporada);
        }
    }

    /** Adiciona um episodioId à temporada indicada (se ainda não existir). */
    @Transactional
    public void addEpisodioToTemporada(String temporadaId, String episodioId) {
        repo.findById(temporadaId).ifPresent(t -> {
            if (!t.getEpisodioIds().contains(episodioId)) {
                t.getEpisodioIds().add(episodioId);
                repo.save(t);
            }
        });
    }
}
