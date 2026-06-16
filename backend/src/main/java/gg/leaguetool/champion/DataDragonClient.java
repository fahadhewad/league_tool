package gg.leaguetool.champion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gg.leaguetool.config.DataDragonProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for Riot's public Data Dragon static-data CDN. No API key is required.
 *
 * <p>Only the two endpoints needed to keep the champion roster current are wrapped:
 * the version manifest and the champion metadata file for a given version.
 */
@Component
public class DataDragonClient {

    private static final ParameterizedTypeReference<List<String>> STRING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final DataDragonProperties props;

    public DataDragonClient(RestClient dataDragonRestClient, DataDragonProperties props) {
        this.client = dataDragonRestClient;
        this.props = props;
    }

    /** Latest published Data Dragon version (e.g. {@code 16.12.1}); the manifest is newest-first. */
    public String latestVersion() {
        List<String> versions = client.get().uri("/api/versions.json").retrieve().body(STRING_LIST);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalStateException("Data Dragon returned no versions");
        }
        return versions.get(0);
    }

    /** Champion metadata for a version, in the configured locale. */
    public ChampionFile fetchChampions(String version) {
        return client.get()
                .uri("/cdn/{version}/data/{locale}/champion.json", version, props.locale())
                .retrieve()
                .body(ChampionFile.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChampionFile(String version, Map<String, Entry> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String key, String name, List<String> tags, Info info) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(int attack, int magic) {
    }
}
