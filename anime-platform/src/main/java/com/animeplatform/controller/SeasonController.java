package com.animeplatform.controller;

import com.animeplatform.dto.EpisodioDTO;
import com.animeplatform.dto.TemporadaDTO;
import com.animeplatform.model.Episodio;
import com.animeplatform.model.Temporada;
import com.animeplatform.repository.EpisodeRepository;
import com.animeplatform.repository.TemporadaRepository;
import com.animeplatform.service.KitsuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller de temporadas.
 *
 * GET /api/animes/{animeId}/seasons
 *   Devolve a lista de temporadas de um anime.
 *   Lógica de geração automática (por ordem de prioridade):
 *     1. Devolve as temporadas já guardadas em seasons.json
 *     2. Se não existirem temporadas mas existirem episódios locais,
 *        agrupa-os em "Temporada 1" (compatibilidade retroativa)
 *     3. Se não existir nada local, vai buscar à Kitsu API,
 *        guarda os episódios e cria "Temporada 1" automaticamente
 *
 * GET /api/seasons/{seasonId}/episodes
 *   Devolve os episódios da temporada indicada com a nota do utilizador.
 *   Parâmetro opcional: ?userName=NomeDoUtilizador
 */
@RestController
@CrossOrigin(origins = "*")
public class SeasonController {

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private KitsuService kitsuService;

    // ── GET /api/animes/{animeId}/seasons ─────────────────────────────────────

    @GetMapping("/api/animes/{animeId}/seasons")
    public ResponseEntity<?> getSeasonsByAnime(@PathVariable String animeId) {
        try {
            System.out.println("[SeasonController] GET /api/animes/" + animeId + "/seasons");

            // ── Passo 1: já temos temporadas guardadas? ──────────────────────
            List<Temporada> temporadas = temporadaRepository.findByAnimeId(animeId);
            if (!temporadas.isEmpty()) {
                System.out.println("[SeasonController] " + temporadas.size() + " temporada(s) em cache.");
                return ok(temporadas);
            }

            // ── Passo 2: temos episódios locais sem temporada? ───────────────
            List<Episodio> episodiosLocais = episodeRepository.findByAnimeId(animeId);
            if (!episodiosLocais.isEmpty()) {
                Temporada t1 = criarTemporada1(animeId, episodiosLocais);
                temporadas.add(t1);
                System.out.println("[SeasonController] Temporada 1 criada a partir de episódios locais ("
                        + episodiosLocais.size() + " eps).");
                return ok(temporadas);
            }

            // ── Passo 3: nada local — vai buscar à Kitsu API ─────────────────
            System.out.println("[SeasonController] Sem dados locais. A buscar episódios na Kitsu API...");
            List<Episodio> episodiosKitsu = kitsuService.fetchEpisodios(animeId);

            if (episodiosKitsu.isEmpty()) {
                // A Kitsu também não tem episódios (ex: manga, filme sem episódios)
                // Cria uma temporada com 1 episódio-placeholder para o utilizador
                // poder avaliar mesmo assim
                System.out.println("[SeasonController] Kitsu não devolveu episódios. A criar placeholder.");
                Episodio placeholder = new Episodio(animeId, 1, "Episódio 1", "");
                episodiosKitsu.add(placeholder);
            }

            // Guarda os episódios localmente
            for (Episodio ep : episodiosKitsu) {
                episodeRepository.saveEpisodio(ep);
            }

            // Cria e guarda a Temporada 1
            Temporada t1 = criarTemporada1(animeId, episodiosKitsu);
            temporadas.add(t1);

            System.out.println("[SeasonController] Temporada 1 gerada com " + episodiosKitsu.size()
                    + " episódio(s) da Kitsu API.");
            return ok(temporadas);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar temporadas: " + e.getMessage()));
        }
    }

    // ── GET /api/seasons/{seasonId}/episodes ─────────────────────────────────

    @GetMapping("/api/seasons/{seasonId}/episodes")
    public ResponseEntity<?> getEpisodesBySeason(
            @PathVariable String seasonId,
            @RequestParam(required = false) String userName) {
        try {
            System.out.println("[SeasonController] GET /api/seasons/" + seasonId + "/episodes"
                    + (userName != null ? "?userName=" + userName : ""));

            Optional<Temporada> optTemporada = temporadaRepository.findById(seasonId);
            if (optTemporada.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Temporada não encontrada: " + seasonId));
            }

            Temporada temporada = optTemporada.get();

            List<EpisodioDTO> dtos = temporada.getEpisodioIds().stream()
                    .map(id -> episodeRepository.findById(id))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> Integer.compare(a.getNumero(), b.getNumero()))
                    .map(ep -> userName != null
                            ? new EpisodioDTO(ep, userName)
                            : new EpisodioDTO(ep))
                    .collect(Collectors.toList());

            System.out.println("[SeasonController] " + dtos.size()
                    + " episódio(s) devolvido(s) para temporada " + seasonId);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar episódios da temporada: " + e.getMessage()));
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    /** Cria, guarda e devolve a "Temporada 1" com a lista de episódios dada. */
    private Temporada criarTemporada1(String animeId, List<Episodio> episodios) {
        Temporada t1 = new Temporada(animeId, 1, "Temporada 1");
        List<String> ids = episodios.stream()
                .map(Episodio::getId)
                .collect(Collectors.toList());
        t1.setEpisodioIds(ids);
        temporadaRepository.saveOrUpdate(t1);

        // Associa seasonId a cada episódio
        for (Episodio ep : episodios) {
            if (ep.getSeasonId() == null || ep.getSeasonId().isBlank()) {
                ep.setSeasonId(t1.getId());
                episodeRepository.saveEpisodio(ep);
            }
        }
        return t1;
    }

    /** Converte lista de Temporada em ResponseEntity<List<TemporadaDTO>>. */
    private ResponseEntity<?> ok(List<Temporada> temporadas) {
        List<TemporadaDTO> dtos = temporadas.stream()
                .map(TemporadaDTO::new)
                .collect(Collectors.toList());
        System.out.println("[SeasonController] " + dtos.size() + " temporada(s) devolvida(s).");
        return ResponseEntity.ok(dtos);
    }
}
