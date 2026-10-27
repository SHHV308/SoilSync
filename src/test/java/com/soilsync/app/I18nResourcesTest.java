package com.soilsync.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class I18nResourcesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void everyListedLanguageHasExactlyTheEnglishKeysAndTheSamePlaceholders() throws IOException {
        JsonNode english = load("en");
        for (String code : languageCodes()) {
            JsonNode strings = load(code);
            assertThat(keys(strings)).as("keys of %s", code).isEqualTo(keys(english));
            for (String key : keys(english)) {
                if (key.startsWith("_")) continue;
                assertThat(placeholders(strings.get(key).asText()))
                    .as("placeholders of %s in %s", key, code)
                    .isEqualTo(placeholders(english.get(key).asText()));
            }
        }
    }

    @Test
    void translatedLanguagesAreFlaggedForHumanReviewAndEnglishIsNot() throws IOException {
        assertThat(load("en").path("_meta").path("machineTranslated").asBoolean()).isFalse();
        for (String code : languageCodes()) {
            if (code.equals("en")) continue;
            assertThat(load(code).path("_meta").path("machineTranslated").asBoolean())
                .as("%s must stay flagged until a human agronomist reviews it", code)
                .isTrue();
        }
    }

    @Test
    void launchLanguagesAreAllPresent() throws IOException {
        assertThat(languageCodes()).containsExactlyInAnyOrder("en", "es", "fr", "pt", "hi", "sw");
    }

    @Test
    void everyKeyUsedInTheHtmlExistsInEnglish() throws IOException {
        String html = new String(read("static/index.html"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("data-i18n(?:-placeholder)?=\"([^\"]+)\"").matcher(html);
        Set<String> english = keys(load("en"));
        int found = 0;
        while (matcher.find()) {
            found++;
            assertThat(english).as("key used in index.html").contains(matcher.group(1));
        }
        assertThat(found).isGreaterThan(40);
    }

    private static List<String> languageCodes() throws IOException {
        List<String> codes = new ArrayList<>();
        MAPPER.readTree(read("static/i18n/languages.json")).forEach(node -> codes.add(node.get("code").asText()));
        return codes;
    }

    private static JsonNode load(String code) throws IOException {
        return MAPPER.readTree(read("static/i18n/" + code + ".json"));
    }

    private static Set<String> keys(JsonNode node) {
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static Set<String> placeholders(String text) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(text);
        while (matcher.find()) names.add(matcher.group(1));
        return names;
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream stream = new ClassPathResource(path).getInputStream()) {
            return stream.readAllBytes();
        }
    }
}
