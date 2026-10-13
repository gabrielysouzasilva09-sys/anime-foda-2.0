package com.animeplatform.repository;

import com.animeplatform.model.Episodio;
import com.animeplatform.model.NotaEpisodio;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de episódios e notas.
 * Persiste os dados em episodes.json na raiz do projecto.
 *
 * Estrutura do ficheiro:
 * [
 *   {
 *     "id": "1-1",
 *     "animeId": "1",
 *     "numero": 1,
 *     "titulo": "...",
 *     "notaMedia": 8.5,
 *     "totalNotas": 2,
 *     "notas": [ { "id": "...", "userName": "...", "nota": 9, ... } ]
 *   }
 * ]
 */
@Repository
public class EpisodeRepository {

    private static final String DATA_FILE =
            System.getProperty("user.dir") + File.separator + "episodes.json";

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private List<Episodio> episodios = new ArrayList<>();

    private final ObjectMapper objectMapper;

    public EpisodeRepository() {
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
                System.out.println("[EpisodeRepository] Pasta criada: " + parentDir.toAbsolutePath());
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, "[]");
                System.out.println("[EpisodeRepository] Ficheiro criado: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[EpisodeRepository] ERRO ao criar ficheiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Carrega do disco ──────────────────────────────────────────────────────

    private void load() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists() && file.length() > 2) {
                episodios = objectMapper.readValue(file, new TypeReference<List<Episodio>>() {});
                System.out.println("[EpisodeRepository] " + episodios.size() + " episódio(s) carregado(s).");
            } else {
                episodios = new ArrayList<>();
                System.out.println("[EpisodeRepository] Nenhum episódio em cache — lista vazia.");
            }
        } catch (IOException e) {
            System.err.println("[EpisodeRepository] ERRO ao carregar episodes.json: " + e.getMessage());
            e.printStackTrace();
            episodios = new ArrayList<>();
        }
    }

    // ── Persiste no disco ─────────────────────────────────────────────────────

    private void save() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DATA_FILE), episodios);
            System.out.println("[EpisodeRepository] episodes.json guardado — " + episodios.size() + " episódio(s).");
        } catch (IOException e) {
            System.err.println("[EpisodeRepository] ERRO ao guardar episodes.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /** Devolve todos os episódios de um anime, ordenados pelo número. */
    public List<Episodio> findByAnimeId(String animeId) {
        List<Episodio> result = new ArrayList<>();
        for (Episodio ep : episodios) {
            if (animeId.equals(ep.getAnimeId())) {
                result.add(ep);
            }
        }
        result.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
        return result;
    }

    /** Encontra um episódio pelo seu ID composto "{animeId}-{numero}". */
    public Optional<Episodio> findById(String id) {
        return episodios.stream()
                .filter(ep -> id.equals(ep.getId()))
                .findFirst();
    }

    /**
     * Cria ou actualiza um episódio no repositório.
     * Se já existir um episódio com o mesmo id, os dados base são actualizados
     * mas as notas são preservadas.
     */
    public Episodio saveEpisodio(Episodio episodio) {
        // Garante que o ID está definido
        if (episodio.getId() == null || episodio.getId().isBlank()) {
            episodio.setId(episodio.getAnimeId() + "-" + episodio.getNumero());
        }

        Optional<Episodio> existing = findById(episodio.getId());
        if (existing.isPresent()) {
            // Actualiza dados base mantendo as notas já existentes
            Episodio ep = existing.get();
            ep.setTitulo(episodio.getTitulo());
            ep.setSinopse(episodio.getSinopse());
            save();
            return ep;
        } else {
            episodios.add(episodio);
            save();
            return episodio;
        }
    }

    /**
     * Guarda ou actualiza a nota de um utilizador para um episódio.
     *
     * Regra de upsert: se o userName já avaliou este episódio,
     * a nota é substituída pela nova. Caso contrário, é adicionada.
     *
     * @param animeId    ID do anime na Kitsu
     * @param episodioId ID composto do episódio ("{animeId}-{numero}")
     * @param userName   Nome do utilizador (pode ser "Anônimo")
     * @param nota       Nota de 1 a 10
     * @return O episódio actualizado com a nova média
     * @throws IllegalArgumentException se o episódio não for encontrado
     */
    public Episodio saveOrUpdateNota(String animeId, String episodioId,
                                     String userName, int nota) {
        Optional<Episodio> opt = findById(episodioId);

        Episodio ep;
        if (opt.isPresent()) {
            ep = opt.get();
        } else {
            // Cria o episódio com dados mínimos se ainda não existir
            // (será enriquecido pelo controller se tiver dados da Kitsu)
            ep = new Episodio(animeId, extrairNumero(episodioId), "Episódio", "");
            episodios.add(ep);
        }

        List<NotaEpisodio> notas = ep.getNotas();
        if (notas == null) {
            notas = new ArrayList<>();
            ep.setNotas(notas);
        }

        // Upsert: substitui nota existente do mesmo userName
        String nomeFinal = (userName == null || userName.isBlank()) ? "Anônimo" : userName;
        boolean actualizado = false;
        for (NotaEpisodio n : notas) {
            if (nomeFinal.equalsIgnoreCase(n.getUserName())) {
                System.out.println("[EpisodeRepository] Actualizando nota de '"
                        + nomeFinal + "' no episódio " + episodioId
                        + ": " + n.getNota() + " → " + nota);
                n.setNota(nota);
                n.setCreatedAt(ISO_FMT.format(Instant.now()));
                actualizado = true;
                break;
            }
        }

        if (!actualizado) {
            NotaEpisodio novaNota = new NotaEpisodio();
            novaNota.setId(UUID.randomUUID().toString());
            novaNota.setEpisodioId(episodioId);
            novaNota.setAnimeId(animeId);
            novaNota.setUserName(nomeFinal);
            novaNota.setNota(nota);
            novaNota.setCreatedAt(ISO_FMT.format(Instant.now()));
            notas.add(novaNota);
            System.out.println("[EpisodeRepository] Nova nota de '"
                    + nomeFinal + "' no episódio " + episodioId + ": " + nota);
        }

        ep.recalcularMedia();
        save();
        return ep;
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
