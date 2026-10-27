package com.soilsync.app.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Pattern;

@Service
public class RegionalKnowledgeBase {

    private static final Pattern COUNTRY_CODE = Pattern.compile("[a-z]{2}");
    private static final Pattern CROP_SLUG = Pattern.compile("[a-z0-9-]+");

    private final ResourceLoader resourceLoader;

    public RegionalKnowledgeBase(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Optional<KnowledgeDocument> findVettedDocument(
        String countryCode,
        String country,
        String admin1,
        String admin2,
        String crop
    ) {
        if (countryCode == null || crop == null) {
            return Optional.empty();
        }

        String normalizedCountry = countryCode.toLowerCase(Locale.ROOT);
        String cropSlug = toCropSlug(crop);
        if (!COUNTRY_CODE.matcher(normalizedCountry).matches()
            || !CROP_SLUG.matcher(cropSlug).matches()) {
            return Optional.empty();
        }

        for (KnowledgeResource resourcePath : List.of(
            new KnowledgeResource(
                "classpath:/knowledge/" + normalizedCountry + "/" + cropSlug + ".md",
                false
            ),
            new KnowledgeResource("classpath:/knowledge/global/" + cropSlug + ".md", true)
        )) {
            Resource resource = resourceLoader.getResource(resourcePath.path());
            if (resource.exists()) {
                Optional<KnowledgeDocument> document = readVettedDocument(resource, resourcePath.path());
                if (document.isPresent()
                    && coversRegion(
                        document.get().regionCovered(),
                        resourcePath.global(),
                        country,
                        admin1,
                        admin2
                    )) {
                    return document;
                }
            }
        }
        return Optional.empty();
    }

    static boolean coversRegion(
        String coverage,
        boolean globalDocument,
        String country,
        String admin1,
        String admin2
    ) {
        Set<String> coverageAreas = Arrays.stream(coverage.split("\\|"))
            .map(RegionalKnowledgeBase::normalizeRegion)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet());
        if (globalDocument) {
            return coverageAreas.contains("global") || coverageAreas.contains("worldwide");
        }
        if (coverageAreas.contains("national") || coverageAreas.contains("nationwide")
            || coverageAreas.contains("all regions")) {
            return true;
        }
        return Stream.of(country, admin1, admin2)
            .filter(value -> value != null && !value.isBlank())
            .map(RegionalKnowledgeBase::normalizeRegion)
            .anyMatch(coverageAreas::contains);
    }

    private static Optional<KnowledgeDocument> readVettedDocument(Resource resource, String path) {
        try (var input = resource.getInputStream()) {
            String markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return parseVettedDocument(markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read agronomy knowledge document: " + path, exception);
        }
    }

    private static Optional<KnowledgeDocument> parseVettedDocument(String markdown) {
        if (!markdown.startsWith("---")) {
            return Optional.empty();
        }

        int metadataEnd = markdown.indexOf("\n---", 3);
        if (metadataEnd < 0) {
            return Optional.empty();
        }

        Map<String, String> metadata = new HashMap<>();
        for (String line : markdown.substring(3, metadataEnd).split("\\R")) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            metadata.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }

        if (!"vetted".equalsIgnoreCase(metadata.get("status"))) {
            return Optional.empty();
        }

        String sourceName = value(metadata, "source_name");
        String sourceUrl = value(metadata, "source_url");
        String publisher = value(metadata, "publisher");
        String regionCovered = value(metadata, "region_covered");
        String publicationYear = value(metadata, "publication_year");
        String lastReviewed = value(metadata, "last_reviewed");
        String content = markdown.substring(metadataEnd + 4).trim();

        if (List.of(sourceName, sourceUrl, publisher, regionCovered, publicationYear, lastReviewed, content)
            .stream().anyMatch(value -> value.isBlank() || value.toUpperCase(Locale.ROOT).contains("TODO"))
            || !isHttpsUrl(sourceUrl)
            || !publicationYear.matches("\\d{4}")
            || !isValidPublicationYear(publicationYear)
            || !isValidDate(lastReviewed)) {
            return Optional.empty();
        }

        return Optional.of(new KnowledgeDocument(
            sourceName,
            sourceUrl,
            publisher,
            Integer.parseInt(publicationYear),
            regionCovered,
            lastReviewed,
            content
        ));
    }

    private static String value(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean isValidPublicationYear(String value) {
        int year = Integer.parseInt(value);
        return year >= 1900 && year <= Year.now().getValue();
    }

    private static boolean isValidDate(String value) {
        try {
            return !LocalDate.parse(value).isAfter(LocalDate.now());
        } catch (java.time.format.DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String toCropSlug(String crop) {
        String normalized = crop.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", "-");
        return normalized.replaceAll("[^a-z0-9-]", "");
    }

    private static String normalizeRegion(String region) {
        return region == null ? "" : region.trim().toLowerCase(Locale.ROOT);
    }

    private record KnowledgeResource(String path, boolean global) {
    }

    public record KnowledgeDocument(
        String sourceName,
        String sourceUrl,
        String publisher,
        int publicationYear,
        String regionCovered,
        String lastReviewed,
        String content
    ) {
        public String citation() {
            return sourceName + " — " + publisher + " (" + publicationYear + "), covers "
                + regionCovered + "; last reviewed " + lastReviewed + ": " + sourceUrl;
        }
    }
}
