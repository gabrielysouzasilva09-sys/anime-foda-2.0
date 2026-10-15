package com.animeplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Serviço responsável por descobrir a cadeia de temporadas de um anime via Kitsu API.
 *
 * Usa o endpoint /media-relationships com filter[source_id] e include=destination
 * para navegar prequels (encontrar T1) e sequels (percorrer toda a chain).
 *
 * Endpoint:
 *   GET https://kitsu.io/api/edge/media-relationships
 *       ?filter[source_id]={animeId}
 *       &filter[source_type]=Anime
 *       &include=destination
 *       &page[limit]=20
 *
 * Cada entrada retornada tem:
 *   attributes.role  →  "sequel" | "prequel" | "adaptation" | "side_story" | ...
 *   relationships.destination.data.id    → ID do anime destino
 *   relationships.destination.data.type  → "anime" | "manga" (filtrar só anime)
 */
@Service
public class KitsuSeasonService {

    private static final String KITSU_BASE    = "https://kitsu.io/api/edge";
    private static final String ACCEPT_HEADER = "application/vnd.api+json";

    /** Máximo de saltos ao navegar prequels para chegar à T1 */
    private static final int MAX_PREQUEL_DEPTH = 8;
    /** Máximo de temporadas a seguir via sequels */
    private static final int MAX_SEQUEL_DEPTH  = 10;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Estrutura de resultado ────────────────────────────────────────────────

    public static class SeasonEntry {
        public final String kitsuId;
        public final int    numero;
        public final String label;

        public SeasonEntry(String kitsuId, int numero, String label) {
            this.kitsuId = kitsuId;
            this.numero  = numero;
            this.label   = label;
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Retorna a lista ordenada de temporadas para o anime indicado.
     * Navega até ao root (T1, sem prequel) e depois segue os sequels.
     *
     * @param animeId  ID Kitsu do anime (pode ser qualquer temporada da série)
     * @return lista de SeasonEntry ordenada da T1 para a última; ou lista com
     *         um único entry com label "Série Completa" se não existirem relações
     */
    public List<SeasonEntry> discoverSeasons(String animeId) {
        try {
            // ── 1. Navegar para trás até encontrar o root (T1 = sem prequel anime) ──
            String rootId = findRoot(animeId);

            // ── 2. Percorrer sequels a partir do root ──
            List<String> chain = buildChain(rootId);

            if (chain.isEmpty()) chain.add(rootId);

            // ── 3. Converter chain em SeasonEntry com labels ordinais ──
            String[] ordinals = {"1ª","2ª","3ª","4ª","5ª","6ª","7ª","8ª","9ª","10ª"};
            List<SeasonEntry> result = new ArrayList<>();
            for (int i = 0; i < chain.size(); i++) {
                String ord   = i < ordinals.length ? ordinals[i] : (i + 1) + "ª";
                String label = chain.size() == 1 ? "Série Completa" : ord + " Temporada";
                result.add(new SeasonEntry(chain.get(i), i + 1, label));
            }
            return result;

        } catch (Exception e) {
            System.err.println("[KitsuSeasonService] Erro ao descobrir temporadas para " + animeId + ": " + e.getMessage());
            // fallback: série única
            List<SeasonEntry> fallback = new ArrayList<>();
            fallback.add(new SeasonEntry(animeId, 1, "Série Completa"));
            return fallback;
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Navega prequels até encontrar o anime sem prequel (root = T1).
     */
    private String findRoot(String startId) {
        String current = startId;
        Set<String> visited = new LinkedHashSet<>();

        for (int depth = 0; depth < MAX_PREQUEL_DEPTH; depth++) {
            if (visited.contains(current)) break;
            visited.add(current);

            List<String> prequels = getRelated(current, "prequel");
            if (prequels.isEmpty()) break;
            current = prequels.get(0);
        }
        return current;
    }

    /**
     * A partir do rootId, segue sequels para montar a chain completa.
     */
    private List<String> buildChain(String rootId) {
        List<String> chain = new ArrayList<>();
        chain.add(rootId);

        Set<String> seen   = new LinkedHashSet<>(chain);
        String cursor      = rootId;

        for (int depth = 0; depth < MAX_SEQUEL_DEPTH; depth++) {
            List<String> sequels = getRelated(cursor, "sequel");
            // Filtra IDs já na chain para evitar ciclos
            Optional<String> next = sequels.stream()
                    .filter(id -> !seen.contains(id))
                    .findFirst();
            if (next.isEmpty()) break;
            cursor = next.get();
            seen.add(cursor);
            chain.add(cursor);
        }
        return chain;
    }

    /**
     * Chama a Kitsu media-relationships API e devolve os IDs de anime destino
     * que têm o role indicado ("sequel" ou "prequel").
     */
    private List<String> getRelated(String animeId, String role) {
        List<String> result = new ArrayList<>();
        try {
            String url = String.format(
                    "%s/media-relationships?filter[source_id]=%s&filter[source_type]=Anime&include=destination&page[limit]=20",
                    KITSU_BASE, animeId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", ACCEPT_HEADER);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return result;
            }

            JsonNode root     = objectMapper.readTree(resp.getBody());
            JsonNode dataNode = root.path("data");

            if (!dataNode.isArray()) return result;

            for (JsonNode rel : dataNode) {
                String relRole = rel.path("attributes").path("role").asText("");
                if (!role.equalsIgnoreCase(relRole)) continue;

                JsonNode destData = rel.path("relationships")
                                       .path("destination")
                                       .path("data");
                if (destData.isMissingNode()) continue;

                String destType = destData.path("type").asText("");
                if (!"anime".equalsIgnoreCase(destType)) continue; // ignora manga

                String destId = destData.path("id").asText("");
                if (!destId.isBlank()) result.add(destId);
            }

        } catch (Exception e) {
            System.err.println("[KitsuSeasonService] Erro em getRelated(" + animeId + ", " + role + "): " + e.getMessage());
        }
        return result;
    }
}
