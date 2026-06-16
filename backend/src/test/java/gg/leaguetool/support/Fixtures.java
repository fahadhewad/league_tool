package gg.leaguetool.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads Riot API JSON fixtures from {@code src/test/resources/fixtures/riot}. */
public final class Fixtures {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Fixtures() {
    }

    public static String load(String name) {
        String path = "/fixtures/riot/" + name;
        try (InputStream in = Fixtures.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T read(String name, Class<T> type) {
        try {
            return MAPPER.readValue(load(name), type);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T read(String name, TypeReference<T> type) {
        try {
            return MAPPER.readValue(load(name), type);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
