package gg.leaguetool.champion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.leaguetool.config.DataDragonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps the champion roster current from Data Dragon.
 *
 * <p>Data Dragon publishes the full roster (id, name, class tags) but not positions or crowd
 * control, so those are layered in from the curated {@code champion-overrides.json} — the same
 * merge that {@code tools/generate_champion_seed.py} bakes into the bundled seed. This keeps the
 * offline baseline and the live refresh in agreement.
 */
@Service
public class DataDragonService {

    private static final Logger log = LoggerFactory.getLogger(DataDragonService.class);

    private final DataDragonClient client;
    private final ChampionRepository repository;
    private final DataDragonProperties props;
    private final Map<Integer, Override> overrides;

    private volatile String currentVersion;

    public DataDragonService(DataDragonClient client,
                             ChampionRepository repository,
                             DataDragonProperties props,
                             ObjectMapper objectMapper) {
        this.client = client;
        this.repository = repository;
        this.props = props;
        this.overrides = loadOverrides(objectMapper);
    }

    /** Fetches the latest roster and swaps it into the repository. Returns the champion count. */
    public int refresh() {
        String version = client.latestVersion();
        DataDragonClient.ChampionFile file = client.fetchChampions(version);
        List<Champion> champions = file.data().values().stream()
                .map(this::toChampion)
                .sorted(Comparator.comparingInt(Champion::id))
                .toList();
        repository.refresh(champions);
        this.currentVersion = version;
        log.info("Refreshed {} champions from Data Dragon {}", champions.size(), version);
        return champions.size();
    }

    /** Data Dragon version of the last successful refresh, or {@code null} if never refreshed. */
    public String currentVersion() {
        return currentVersion;
    }

    @EventListener(ApplicationReadyEvent.class)
    void refreshOnStartupIfEnabled() {
        if (!props.refreshOnStartup()) {
            return;
        }
        try {
            refresh();
        } catch (RuntimeException e) {
            log.warn("Data Dragon refresh on startup failed; keeping bundled seed: {}", e.toString());
        }
    }

    Champion toChampion(DataDragonClient.Entry entry) {
        int id = Integer.parseInt(entry.key());
        List<ChampionTag> tags = entry.tags() == null ? List.of()
                : entry.tags().stream().map(DataDragonService::mapTag).filter(t -> t != null).toList();
        Override ov = overrides.get(id);

        List<Role> roles = ov != null && ov.roles() != null && !ov.roles().isEmpty()
                ? ov.roles() : deriveRoles(tags);
        DamageType damage = ov != null && ov.damageType() != null
                ? ov.damageType() : deriveDamageType(tags, entry.info());
        boolean cc = ov != null && ov.cc() != null ? ov.cc() : false;

        return new Champion(id, entry.name(), roles, damage, tags, cc);
    }

    private static ChampionTag mapTag(String ddragonTag) {
        if (ddragonTag == null) {
            return null;
        }
        try {
            return ChampionTag.valueOf(ddragonTag.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null; // e.g. tags we don't model
        }
    }

    private static DamageType deriveDamageType(List<ChampionTag> tags, DataDragonClient.Info info) {
        boolean marksman = tags.contains(ChampionTag.MARKSMAN);
        boolean mage = tags.contains(ChampionTag.MAGE);
        if (marksman && !mage) {
            return DamageType.PHYSICAL;
        }
        if (mage && !marksman) {
            return DamageType.MAGIC;
        }
        int attack = info == null ? 0 : info.attack();
        int magic = info == null ? 0 : info.magic();
        if (magic > attack) {
            return DamageType.MAGIC;
        }
        if (attack > magic) {
            return DamageType.PHYSICAL;
        }
        return DamageType.MIXED;
    }

    /** A safe single-position fallback so every champion is queryable by role. */
    private static List<Role> deriveRoles(List<ChampionTag> tags) {
        if (tags.contains(ChampionTag.SUPPORT)) {
            return List.of(Role.UTILITY);
        }
        if (tags.contains(ChampionTag.MARKSMAN)) {
            return List.of(Role.BOTTOM);
        }
        if (tags.contains(ChampionTag.ASSASSIN) || tags.contains(ChampionTag.MAGE)) {
            return List.of(Role.MIDDLE);
        }
        if (tags.contains(ChampionTag.TANK) || tags.contains(ChampionTag.FIGHTER)) {
            return List.of(Role.TOP);
        }
        return List.of(Role.MIDDLE);
    }

    private static Map<Integer, Override> loadOverrides(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("data/champion-overrides.json").getInputStream()) {
            OverridesFile file = mapper.readValue(in, OverridesFile.class);
            return file.overrides().stream()
                    .collect(Collectors.toUnmodifiableMap(Override::id, Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load champion overrides", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OverridesFile(List<Override> overrides) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Override(int id, List<Role> roles, DamageType damageType, Boolean cc) {
    }
}
