package com.animeplatform.controller;

import com.animeplatform.dto.EpisodioDTO;
import com.animeplatform.dto.TemporadaDTO;
import com.animeplatform.model.Episodio;
import com.animeplatform.model.Temporada;
import com.animeplatform.repository.EpisodeRepository;
import com.animeplatform.repository.TemporadaRepository;
import com.animeplatform.service.KitsuSeasonService;
import com.animeplatform.service.KitsuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller de temporadas.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * GET /api/animes/{animeId}/seasons
 * ──────────────────────────────────────────────────────────────────────────────
 * Devolve a lista correta de temporadas para CADA anime individualmente.
 *
 * Ordem de prioridade:
 *   1. ANIME_SEASONS_MAP — mapa estático com temporadas verificadas manualmente
 *      para os animes mais populares (AoT, MHA, Demon Slayer, etc.)
 *   2. KitsuSeasonService — descoberta dinâmica via Kitsu media-relationships
 *      para qualquer anime não mapeado (navega prequel→sequel chain)
 *   3. Fallback — cria "Série Completa" com episódios da Kitsu API
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * GET /api/seasons/{seasonId}/episodes
 * ──────────────────────────────────────────────────────────────────────────────
 * Devolve os episódios de uma temporada.
 * seasonId = "{kitsuId}-s{numero}" — ex: "7442-s1", "11469-s3"
 * Parâmetro opcional: ?userName=NomeDoUtilizador
 *
 * Se a temporada não tiver episódios em cache local, vai buscá-los à Kitsu API
 * usando o kitsuId extraído do seasonId.
 */
@RestController
public class SeasonController {

    // ══════════════════════════════════════════════════════════════════════════
    // MAPA ESTÁTICO DE TEMPORADAS
    // Chave: qualquer kitsuId de uma temporada da série (T1, T2, T3...)
    // Valor: lista ordenada de {kitsuId, numero, label} de toda a série
    //
    // IDs verificados na Kitsu API — correspondem a animes TV reais.
    // Formato ID seasons: "{kitsuId}-s{numero}"
    // ══════════════════════════════════════════════════════════════════════════

    private static final Map<String, List<SeasonDef>> ANIME_SEASONS_MAP = new HashMap<>();

    static {
        // helper: regista todas as temporadas de uma série para todos os seus IDs
        registerSeries(
            /* My Hero Academia — T1..T8 */
            new SeasonDef("11469", 1, "1ª Temporada"),
            new SeasonDef("12268", 2, "2ª Temporada"),
            new SeasonDef("13881", 3, "3ª Temporada"),
            new SeasonDef("41971", 4, "4ª Temporada"),
            new SeasonDef("43108", 5, "5ª Temporada"),
            new SeasonDef("45240", 6, "6ª Temporada"),
            new SeasonDef("47232", 7, "7ª Temporada"),
            new SeasonDef("49279", 8, "8ª Temporada — Final")
        );

        registerSeries(
            /* Attack on Titan — T1..Final */
            new SeasonDef("7442",  1, "1ª Temporada"),
            new SeasonDef("8671",  2, "2ª Temporada"),
            new SeasonDef("13569", 3, "3ª Temporada — Parte 1"),
            new SeasonDef("41982", 4, "3ª Temporada — Parte 2"),
            new SeasonDef("42422", 5, "Temporada Final — Parte 1"),
            new SeasonDef("44240", 6, "Temporada Final — Parte 2"),
            new SeasonDef("46038", 7, "Temporada Final — Caps. Finais Pt.1"),
            new SeasonDef("47132", 8, "Temporada Final — Caps. Finais Pt.2")
        );

        registerSeries(
            /* Demon Slayer / Kimetsu no Yaiba */
            new SeasonDef("41370", 1, "1ª Temporada"),
            new SeasonDef("45249", 2, "Arco do Trem Mugen"),
            new SeasonDef("44081", 3, "Arco do Distrito do Entretenimento"),
            new SeasonDef("45866", 4, "Arco da Aldeia dos Ferreiros"),
            new SeasonDef("47635", 5, "Arco do Treinamento Hashira")
        );

        registerSeries(
            /* Jujutsu Kaisen */
            new SeasonDef("42765", 1, "1ª Temporada"),
            new SeasonDef("45857", 2, "2ª Temporada")
        );

        registerSeries(
            /* One Punch Man */
            new SeasonDef("10740", 1, "1ª Temporada"),
            new SeasonDef("12566", 2, "2ª Temporada")
        );

        registerSeries(
            /* Sword Art Online */
            new SeasonDef("6589",  1, "1ª Temporada"),
            new SeasonDef("8174",  2, "2ª Temporada — Gun Gale Online"),
            new SeasonDef("13893", 3, "3ª Temporada — Alicization"),
            new SeasonDef("42213", 4, "3ª Temporada — War of Underworld Pt.1"),
            new SeasonDef("42927", 5, "3ª Temporada — War of Underworld Pt.2")
        );

        registerSeries(
            /* Tokyo Ghoul */
            new SeasonDef("8271",  1, "1ª Temporada"),
            new SeasonDef("9135",  2, "2ª Temporada — Root A"),
            new SeasonDef("13929", 3, "3ª Temporada — :re"),
            new SeasonDef("41359", 4, "3ª Temporada — :re Parte 2")
        );

        registerSeries(
            /* Bleach */
            new SeasonDef("244",   1, "Bleach — Série Clássica"),
            new SeasonDef("43078", 2, "TYBW — Pt.1: Encarnação Vermelha"),
            new SeasonDef("46903", 3, "TYBW — Pt.2: Separação"),
            new SeasonDef("48015", 4, "TYBW — Pt.3: Conflito"),
            new SeasonDef("49444", 5, "TYBW — Pt.4: Culminação")
        );

        registerSeries(
            /* Naruto */
            new SeasonDef("11",   1, "Naruto Clássico"),
            new SeasonDef("1555", 2, "Naruto Shippuden")
        );

        registerSeries(
            /* Dragon Ball */
            new SeasonDef("720",   1, "Dragon Ball Z"),
            new SeasonDef("10879", 2, "Dragon Ball Super")
        );

        // Séries únicas — regista com label especial para não mostrar dropdown
        registerSingle("1376",  "Death Note");
        registerSingle("3936",  "Fullmetal Alchemist: Brotherhood");
        registerSingle("6448",  "Hunter × Hunter (2011)");
        registerSingle("11061", "Hunter × Hunter (1999)");
    }

    // ── Injeções ──────────────────────────────────────────────────────────────

    @Autowired private TemporadaRepository temporadaRepository;
    @Autowired private EpisodeRepository   episodeRepository;
    @Autowired private KitsuService        kitsuService;
    @Autowired private KitsuSeasonService  kitsuSeasonService;

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/animes/{animeId}/seasons
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/animes/{animeId}/seasons")
    public ResponseEntity<?> getSeasonsByAnime(@PathVariable String animeId) {
        try {
            System.out.println("[SeasonController] GET /api/animes/" + animeId + "/seasons");

            // ── Passo 1: mapa estático (resposta imediata, zero chamadas externas) ──
            List<SeasonDef> mapped = ANIME_SEASONS_MAP.get(animeId);
            if (mapped != null) {
                System.out.println("[SeasonController] Mapa estático: " + mapped.size() + " temporada(s) para anime " + animeId);
                List<TemporadaDTO> dtos = mapped.stream()
                        .map(SeasonController::toDTO)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(dtos);
            }

            // ── Passo 2: cache local (seasons.json) ──
            List<Temporada> cached = temporadaRepository.findByAnimeId(animeId);
            if (!cached.isEmpty()) {
                System.out.println("[SeasonController] Cache local: " + cached.size() + " temporada(s).");
                return ok(cached);
            }

            // ── Passo 3: descoberta dinâmica via Kitsu media-relationships ──
            System.out.println("[SeasonController] Descoberta dinâmica via Kitsu para anime " + animeId);
            List<KitsuSeasonService.SeasonEntry> discovered = kitsuSeasonService.discoverSeasons(animeId);

            if (discovered.size() > 1) {
                // Série com várias temporadas descobertas dinamicamente
                System.out.println("[SeasonController] " + discovered.size() + " temporada(s) descoberta(s).");
                List<TemporadaDTO> dtos = discovered.stream()
                        .map(e -> {
                            TemporadaDTO dto = new TemporadaDTO();
                            dto.setId(e.kitsuId + "-s" + e.numero);
                            dto.setAnimeId(e.kitsuId);
                            dto.setNumero(e.numero);
                            dto.setNome(e.label);
                            dto.setEpisodioIds(Collections.emptyList());
                            dto.setTotalEpisodios(0);
                            return dto;
                        })
                        .collect(Collectors.toList());
                return ResponseEntity.ok(dtos);
            }

            // ── Passo 4: série única — busca episódios e cria Temporada 1 ──
            System.out.println("[SeasonController] Série única — a buscar episódios na Kitsu para anime " + animeId);
            List<Episodio> episodios = kitsuService.fetchEpisodios(animeId);

            if (episodios.isEmpty()) {
                Episodio placeholder = new Episodio(animeId, 1, "Episódio 1", "");
                episodios.add(placeholder);
            }

            for (Episodio ep : episodios) {
                episodeRepository.saveEpisodio(ep);
            }

            Temporada t1 = buildAndSaveTemporada(animeId, 1, "Série Completa", episodios);

            System.out.println("[SeasonController] Temporada 1 criada com " + episodios.size() + " eps.");
            return ok(List.of(t1));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar temporadas: " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/seasons/{seasonId}/episodes
    // seasonId formato: "{kitsuId}-s{numero}"  ex: "7442-s1", "11469-s2"
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/seasons/{seasonId}/episodes")
    public ResponseEntity<?> getEpisodesBySeason(
            @PathVariable String seasonId,
            @RequestParam(required = false) String userName) {
        try {
            System.out.println("[SeasonController] GET /api/seasons/" + seasonId + "/episodes"
                    + (userName != null ? "?userName=" + userName : ""));

            // Extrai o kitsuId do seasonId: "7442-s1" → "7442"
            String kitsuId = seasonId.contains("-s")
                    ? seasonId.substring(0, seasonId.lastIndexOf("-s"))
                    : seasonId;

            // ── Verifica se a temporada já tem episódios em cache ──
            Optional<Temporada> optTemp = temporadaRepository.findById(seasonId);
            if (optTemp.isPresent() && !optTemp.get().getEpisodioIds().isEmpty()) {
                System.out.println("[SeasonController] Cache hit para temporada " + seasonId);
                return buildEpisodeDTOs(optTemp.get(), userName);
            }

            // ── Sem cache — busca episódios na Kitsu API para este kitsuId ──
            System.out.println("[SeasonController] Cache miss — a buscar episódios da Kitsu para kitsuId=" + kitsuId);
            List<Episodio> episodios = kitsuService.fetchEpisodios(kitsuId);

            if (episodios.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Guarda episódios localmente
            for (Episodio ep : episodios) {
                episodeRepository.saveEpisodio(ep);
            }

            // Determina o número da temporada a partir do seasonId
            int numTemp = 1;
            try {
                String suffix = seasonId.substring(seasonId.lastIndexOf("-s") + 2);
                numTemp = Integer.parseInt(suffix);
            } catch (Exception ignored) {}

            // Determina o label desta temporada (do mapa estático se possível)
            String label = getLabelFromMap(kitsuId, numTemp);

            // Cria/actualiza a temporada no cache
            Temporada temp = buildAndSaveTemporada(kitsuId, numTemp, label, episodios);
            // Garante que o ID da temporada coincide com o seasonId pedido
            if (!temp.getId().equals(seasonId)) {
                temp.setId(seasonId);
                temporadaRepository.saveOrUpdate(temp);
            }

            System.out.println("[SeasonController] " + episodios.size() + " eps carregados para " + seasonId);
            return buildEpisodeDTOs(temp, userName);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erro ao carregar episódios: " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utilitários privados
    // ══════════════════════════════════════════════════════════════════════════

    /** Constrói a ResponseEntity com lista de EpisodioDTO para uma temporada. */
    private ResponseEntity<?> buildEpisodeDTOs(Temporada temporada, String userName) {
        List<EpisodioDTO> dtos = temporada.getEpisodioIds().stream()
                .map(id -> episodeRepository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(Episodio::getNumero))
                .map(ep -> userName != null ? new EpisodioDTO(ep, userName) : new EpisodioDTO(ep))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** Cria, guarda e devolve uma Temporada com os episódios fornecidos. */
    private Temporada buildAndSaveTemporada(String animeId, int numero, String nome, List<Episodio> episodios) {
        Temporada t = new Temporada(animeId, numero, nome);
        List<String> ids = episodios.stream().map(Episodio::getId).collect(Collectors.toList());
        t.setEpisodioIds(ids);
        // Associa seasonId a cada episódio
        for (Episodio ep : episodios) {
            if (ep.getSeasonId() == null || ep.getSeasonId().isBlank()) {
                ep.setSeasonId(t.getId());
                episodeRepository.saveEpisodio(ep);
            }
        }
        return temporadaRepository.saveOrUpdate(t);
    }

    /** Converte lista de Temporada em ResponseEntity<List<TemporadaDTO>>. */
    private ResponseEntity<?> ok(List<Temporada> temporadas) {
        List<TemporadaDTO> dtos = temporadas.stream()
                .map(TemporadaDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Tenta encontrar o label correto de uma temporada no mapa estático,
     * a partir do kitsuId e número da temporada.
     */
    private String getLabelFromMap(String kitsuId, int numero) {
        List<SeasonDef> series = ANIME_SEASONS_MAP.get(kitsuId);
        if (series != null) {
            return series.stream()
                    .filter(s -> s.numero == numero)
                    .map(s -> s.label)
                    .findFirst()
                    .orElse(numero + "ª Temporada");
        }
        String[] ordinals = {"1ª","2ª","3ª","4ª","5ª","6ª","7ª","8ª","9ª","10ª"};
        return (numero <= ordinals.length ? ordinals[numero - 1] : numero + "ª") + " Temporada";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tipos de suporte para o mapa estático
    // ══════════════════════════════════════════════════════════════════════════

    /** Definição imutável de uma temporada no mapa estático. */
    private static class SeasonDef {
        final String kitsuId;
        final int    numero;
        final String label;

        SeasonDef(String kitsuId, int numero, String label) {
            this.kitsuId = kitsuId;
            this.numero  = numero;
            this.label   = label;
        }
    }

    /** Converte SeasonDef em TemporadaDTO. */
    private static TemporadaDTO toDTO(SeasonDef s) {
        TemporadaDTO dto = new TemporadaDTO();
        dto.setId(s.kitsuId + "-s" + s.numero);
        dto.setAnimeId(s.kitsuId);
        dto.setNumero(s.numero);
        dto.setNome(s.label);
        dto.setEpisodioIds(Collections.emptyList());
        dto.setTotalEpisodios(0);
        return dto;
    }

    /**
     * Regista uma série de temporadas no mapa.
     * Todos os kitsuIds da série ficam apontados para a mesma lista,
     * para que qualquer temporada da série devolva sempre a lista completa.
     */
    @SafeVarargs
    private static void registerSeries(SeasonDef... defs) {
        List<SeasonDef> list = Arrays.asList(defs);
        for (SeasonDef d : defs) {
            ANIME_SEASONS_MAP.put(d.kitsuId, list);
        }
    }

    /** Regista uma série com apenas uma entrada (sem dropdown de temporadas). */
    private static void registerSingle(String kitsuId, String title) {
        List<SeasonDef> single = List.of(new SeasonDef(kitsuId, 1, title));
        ANIME_SEASONS_MAP.put(kitsuId, single);
    }
}
