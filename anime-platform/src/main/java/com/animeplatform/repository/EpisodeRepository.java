package com.animeplatform.repository;

import com.animeplatform.model.Episodio;
import com.animeplatform.model.NotaEpisodio;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de episódios e notas, persistido no PostgreSQL.
 *
 * Mantém a mesma API pública de antes (findByAnimeId, saveEpisodio,
 * saveOrUpdateNota) mas delegando em repositórios Spring Data JPA.
 */
@Repository
public class EpisodeRepository {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final EpisodioJpaRepository episodioRepo;
    private final NotaEpisodioJpaRepository notaRepo;

    public EpisodeRepository(EpisodioJpaRepository episodioRepo,
                             NotaEpisodioJpaRepository notaRepo) {
        this.episodioRepo = episodioRepo;
        this.notaRepo = notaRepo;
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /** Devolve todos os episódios de um anime, ordenados pelo número. */
    @Transactional(readOnly = true)
    public List<Episodio> findByAnimeId(String animeId) {
        return episodioRepo.findByAnimeIdOrderByNumeroAsc(animeId);
    }

    /** Encontra um episódio pelo seu ID composto "{animeId}-{numero}". */
    @Transactional(readOnly = true)
    public Optional<Episodio> findById(String id) {
        return episodioRepo.findById(id);
    }

    /**
     * Cria ou actualiza um episódio.
     * Se já existir, actualiza os dados base mas preserva as notas existentes.
     */
    @Transactional
    public Episodio saveEpisodio(Episodio episodio) {
        if (episodio.getId() == null || episodio.getId().isBlank()) {
            episodio.setId(episodio.getAnimeId() + "-" + episodio.getNumero());
        }

        Optional<Episodio> existing = episodioRepo.findById(episodio.getId());
        if (existing.isPresent()) {
            Episodio ep = existing.get();
            ep.setTitulo(episodio.getTitulo());
            ep.setSinopse(episodio.getSinopse());
            if (episodio.getSeasonId() != null && !episodio.getSeasonId().isBlank()) {
                ep.setSeasonId(episodio.getSeasonId());
            }
            return episodioRepo.save(ep);
        } else {
            // Episódio novo: garante que não traz notas soltas para persistir
            episodio.setNotas(new ArrayList<>());
            return episodioRepo.save(episodio);
        }
    }

    /**
     * Guarda ou actualiza a nota de um utilizador para um episódio.
     * Se o mesmo userName já avaliou este episódio, a nota é substituída.
     */
    @Transactional
    public Episodio saveOrUpdateNota(String animeId, String episodioId,
                                     String userName, int nota) {
        Episodio ep = episodioRepo.findById(episodioId).orElseGet(() -> {
            Episodio novo = new Episodio(animeId, extrairNumero(episodioId), "Episódio", "");
            novo.setId(episodioId);
            return episodioRepo.save(novo);
        });

        String nomeFinal = (userName == null || userName.isBlank()) ? "Anônimo" : userName;

        Optional<NotaEpisodio> existente =
                notaRepo.findByEpisodio_IdAndUserNameIgnoreCase(episodioId, nomeFinal);

        if (existente.isPresent()) {
            NotaEpisodio n = existente.get();
            n.setNota(nota);
            n.setCreatedAt(ISO_FMT.format(Instant.now()));
            notaRepo.save(n);
        } else {
            NotaEpisodio novaNota = new NotaEpisodio();
            novaNota.setId(UUID.randomUUID().toString());
            novaNota.setEpisodio(ep);
            novaNota.setAnimeId(animeId);
            novaNota.setUserName(nomeFinal);
            novaNota.setNota(nota);
            novaNota.setCreatedAt(ISO_FMT.format(Instant.now()));
            ep.getNotas().add(novaNota);
        }

        ep.recalcularMedia();
        return episodioRepo.save(ep);
    }

    // ── Utilitário ────────────────────────────────────────────────────────────

    /** Extrai o número do episódio a partir do ID composto "animeId-numero". */
    private int extrairNumero(String episodioId) {
        try {
            String[] parts = episodioId.split("-");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception e) {
            return 0;
        }
    }
}
