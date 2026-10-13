package com.animeplatform.service;

import com.animeplatform.model.Episodio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço que comunica com a Kitsu API para obter dados de episódios.
 *
 * Documentação Kitsu: https://kitsu.docs.apiary.io/#reference/episodes
 *
 * Endpoint usado:
 *   GET https://kitsu.io/api/edge/anime/{animeId}/episodes
 *       ?page[limit]=20&page[offset]=0&sort=number
 *
 * Estrutura de resposta (JSON:API):
 * {
 *   "data": [
 *     {
 *       "id": "...",
 *       "attributes": {
 *         "number": 1,
 *         "canonicalTitle": "Ep title",
 *         "synopsis": "..."
 *       }
 *     }
 *   ],
 *   "meta": { "count": 26 },
 *   "links": { "next": "..." }
 * }
 */
@Service
public class KitsuService {

    private static final String KITSU_BASE    = "https://kitsu.io/api/edge";
    private static final String ACCEPT_HEADER = "application/vnd.api+json";
    private static final int    PAGE_SIZE     = 20;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtém todos os episódios de um anime na Kitsu API.
     *
     * Faz paginação automática para animes com mais de 20 episódios.
     * Devolve uma lista de Episodio com dados base (id, numero, titulo, sinopse).
     *
     * @param animeId ID do anime na Kitsu (ex: "1", "11469")
     * @return Lista de episódios ordenada por número, ou lista vazia se erro
     */
    public List<Episodio> fetchEpisodios(String animeId) {
        List<Episodio> resultado = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", ACCEPT_HEADER);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int offset = 0;
        boolean temMais = true;

        while (temMais) {
            String url = String.format(
                    "%s/anime/%s/episodes?page[limit]=%d&page[offset]=%d&sort=number",
                    KITSU_BASE, animeId, PAGE_SIZE, offset
            );

            System.out.println("[KitsuService] A buscar: " + url);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, String.class
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    System.err.println("[KitsuService] Resposta inválida: " + response.getStatusCode());
                    break;
                }

                JsonNode root     = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.path("data");

                if (!dataNode.isArray() || dataNode.size() == 0) {
                    temMais = false;
                    break;
                }

                for (JsonNode item : dataNode) {
                    JsonNode attrs = item.path("attributes");

                    int    numero  = attrs.path("number").asInt(0);
                    // Alguns episódios da Kitsu têm número 0 (OVA/especiais sem número)
                    // Saltamos para não poluir a lista principal
                    if (numero <= 0) continue;

                    String titulo  = attrs.path("canonicalTitle").asText("");
                    if (titulo.isBlank()) {
                        titulo = "Episódio " + numero;
                    }
                    String sinopse = attrs.path("synopsis").asText("");

                    Episodio ep = new Episodio(animeId, numero, titulo, sinopse);
                    resultado.add(ep);
                }

                // Verifica se há página seguinte
                JsonNode nextLink = root.path("links").path("next");
                temMais = !nextLink.isMissingNode() && !nextLink.isNull() && !nextLink.asText("").isBlank();
                offset += PAGE_SIZE;

                // Limite de segurança: máximo 500 episódios (25 páginas)
                if (offset >= 500) {
                    System.out.println("[KitsuService] Limite de segurança atingido (500 eps).");
                    temMais = false;
                }

            } catch (Exception e) {
                System.err.println("[KitsuService] Erro ao chamar Kitsu API: " + e.getMessage());
                break;
            }
        }

        System.out.println("[KitsuService] " + resultado.size() + " episódio(s) obtido(s) da Kitsu para anime " + animeId);
        return resultado;
    }
}
