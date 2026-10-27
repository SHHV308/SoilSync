package com.soilsync.app.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantSafetyPolicyTest {

    private final AssistantSafetyPolicy policy = new AssistantSafetyPolicy();

    @Test
    void pesticideDoseRequestNeverReturnsAProductOrNumberWithoutLocalPesticideSources() {
        String answer = policy.enforce(
            "What pesticide dose should I use?",
            "Use 5 ml per liter of water.",
            true,
            List.of("A generic crop document"),
            List.of("https://example.org/source"),
            true
        );

        assertThat(answer)
            .doesNotContainPattern("\\d")
            .contains("local pesticide source", "agriculture authority");
    }

    @Test
    void unsourcedFertilizerQuantityRequestReturnsTheSoilTestFallback() {
        String answer = policy.enforce(
            "How much fertilizer should I apply?",
            "Apply 20 kg per hectare.",
            false,
            List.of(),
            List.of(),
            true
        );

        assertThat(answer)
            .doesNotContainPattern("\\d")
            .contains("soil test", "package-of-practices source");
    }

    @Test
    void weatherClaimsAreRemovedWhenNoLiveForecastToolWasUsed() {
        String answer = policy.enforce(
            "Can I apply nutrients today?",
            "The forecast shows rain tomorrow.",
            false,
            List.of(),
            List.of(),
            false
        );

        assertThat(answer).contains("don't have a live forecast").doesNotContain("rain tomorrow");
    }

    @Test
    void unverifiedDoseIsBlockedEvenWhenARegionalSourceWasAvailable() {
        String answer = policy.enforce(
            "What nutrient plan fits this crop?",
            "Apply 25 kg per hectare.",
            true,
            List.of("Use soil analysis to guide nutrient decisions."),
            List.of("Local extension source"),
            true
        );

        assertThat(answer).doesNotContainPattern("\\d").contains("soil test");
    }

    @Test
    void nonEnglishPesticideQuestionsGetTheSameRefusal() {
        for (String question : List.of(
            "¿Qué pesticida debo usar?",
            "Quel produit phytosanitaire utiliser ?",
            "Qual agrotóxico devo usar?",
            "कीटनाशक कितना डालूँ?",
            "Nitumie viuatilifu gani?"
        )) {
            String answer = policy.enforce(question, "Use 5 ml per liter.", true, List.of(), List.of(), true);

            assertThat(answer).as(question).doesNotContainPattern("\\d").contains("agriculture authority");
        }
    }

    @Test
    void doseWithoutAPerAreaUnitIsStillBlockedWithoutAVettedSource() {
        String answer = policy.enforce(
            "What nutrient plan fits this crop?",
            "Apply 50 kg of urea before planting.",
            false,
            List.of(),
            List.of(),
            true
        );

        assertThat(answer).doesNotContainPattern("\\d").contains("soil test");
    }
}
