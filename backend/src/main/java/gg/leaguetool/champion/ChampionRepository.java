package gg.leaguetool.champion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads static champion metadata from the bundled seed and serves fast lookups by id, name and role.
 *
 * <p>The seed is curated for offline development; a {@code DataDragonService} can later refresh and
 * extend it from Riot's static data without changing this API.
 */
@Component
public class ChampionRepository {

    private final List<Champion> all;
    private final Map<Integer, Champion> byId;
    private final Map<String, Champion> byName;

    public ChampionRepository(ObjectMapper objectMapper) {
        this.all = load(objectMapper);
        this.byId = all.stream().collect(Collectors.toUnmodifiableMap(Champion::id, Function.identity()));
        this.byName = all.stream().collect(Collectors.toUnmodifiableMap(
                c -> c.name().toLowerCase(Locale.ROOT), Function.identity()));
    }

    private static List<Champion> load(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("data/champions-seed.json").getInputStream()) {
            SeedFile seed = mapper.readValue(in, SeedFile.class);
            return List.copyOf(seed.champions());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load champion seed data", e);
        }
    }

    public List<Champion> all() {
        return all;
    }

    public Optional<Champion> byId(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Champion> byName(String name) {
        return name == null ? Optional.empty()
                : Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    public List<Champion> byRole(Role role) {
        return all.stream().filter(c -> c.playsRole(role)).toList();
    }

    /** Resolves a champion name for an id, falling back to a placeholder for ids not in the seed. */
    public String nameOf(int championId) {
        Champion c = byId.get(championId);
        return c != null ? c.name() : "Champion " + championId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedFile(List<Champion> champions) {
    }
}
