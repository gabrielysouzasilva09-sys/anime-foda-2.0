package com.animeplatform.repository;

import com.animeplatform.model.Temporada;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de temporadas.
 * Persiste os dados em seasons.json na raiz do projecto.
 *
 * Estrutura do ficheiro:
 * [
 *   {
 *     "id": "1-s1",
 *     "animeId": "1",
 *     "numero": 1,
 *     "nome": "Temporada 1",
 *     "episodioIds": ["1-1", "1-2", "1-3"]
 *   }
 * ]
 */
@Repository
public class TemporadaRepository {

    private static final String DATA_FILE =
            System.getProperty("user.dir") + File.separator + "seasons.json";

    private List<Temporada> temporadas = new ArrayList<>();

    private final ObjectMapper objectMapper;

    public TemporadaRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        ensureFileExists();
        load();
    }

    // ── Garante que o ficheiro existe ─────────────────────────────────────────

    private void ensureFileExists() {
        try {
            Path filePath = Paths.get(DATA_FILE);
            Path parentDir = filePath.getParent();

            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                System.out.println("[TemporadaRepository] Pasta criada: " + parentDir.toAbsolutePath());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
                System.out.println("[TemporadaRepository] Ficheiro criado: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[TemporadaRepository] ERRO ao criar ficheiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Carrega do disco ──────────────────────────────────────────────────────

    private void load() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists() && file.length() > 2) {
                temporadas = objectMapper.readValue(file, new TypeReference<List<Temporada>>() {});
                System.out.println("[TemporadaRepository] " + temporadas.size() + " temporada(s) carregada(s).");
            } else {
                temporadas = new ArrayList<>();
                System.out.println("[TemporadaRepository] Nenhuma temporada em cache — lista vazia.");
            }
        } catch (IOException e) {
            System.err.println("[TemporadaRepository] ERRO ao carregar seasons.json: " + e.getMessage());
            e.printStackTrace();
            temporadas = new ArrayList<>();
        }
    }

    // ── Persiste no disco ─────────────────────────────────────────────────────

    private void save() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DATA_FILE), temporadas);
            System.out.println("[TemporadaRepository] seasons.json guardado — " + temporadas.size() + " temporada(s).");
        } catch (IOException e) {
            System.err.println("[TemporadaRepository] ERRO ao guardar seasons.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Devolve todas as temporadas de um anime, ordenadas pelo número.
     */
    public List<Temporada> findByAnimeId(String animeId) {
        List<Temporada> result = new ArrayList<>();
        for (Temporada t : temporadas) {
            if (animeId.equals(t.getAnimeId())) {
                result.add(t);
            }
        }
        result.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
        return result;
    }

    /**
     * Encontra uma temporada pelo seu ID composto "{animeId}-s{numero}".
     */
    public Optional<Temporada> findById(String id) {
        return temporadas.stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst();
    }

    /**
     * Cria ou actualiza uma temporada.
     * Se já existir uma com o mesmo id, o nome e a lista de episódios são actualizados.
     */
    public Temporada saveOrUpdate(Temporada temporada) {
        if (temporada.getId() == null || temporada.getId().isBlank()) {
            temporada.setId(temporada.getAnimeId() + "-s" + temporada.getNumero());
        }

        Optional<Temporada> existing = findById(temporada.getId());
        if (existing.isPresent()) {
            Temporada t = existing.get();
            t.setNome(temporada.getNome());
            t.setEpisodioIds(temporada.getEpisodioIds());
            save();
            return t;
        } else {
            temporadas.add(temporada);
            save();
            return temporada;
        }
    }

    /**
     * Adiciona um episódioId à temporada indicada (se ainda não existir).
     */
    public void addEpisodioToTemporada(String temporadaId, String episodioId) {
        findById(temporadaId).ifPresent(t -> {
            if (!t.getEpisodioIds().contains(episodioId)) {
                t.getEpisodioIds().add(episodioId);
                save();
            }
        });
    }
}
