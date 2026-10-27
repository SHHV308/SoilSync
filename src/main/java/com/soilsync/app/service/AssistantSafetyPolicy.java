package com.soilsync.app.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AssistantSafetyPolicy {

    // Keyword lists for es/fr/pt/hi/sw are a first pass and need review by native-speaking agronomists.
    private static final Pattern PESTICIDE_QUESTION = Pattern.compile(
        "(?iU)\\b(pesticides?|insecticides?|herbicides?|fungicides?|chemicals?|spray dose"
            + "|pesticidas?|plaguicidas?|insecticidas?|herbicidas?|fungicidas?|agrotóxicos?|agrotoxicos?"
            + "|defensivos|fongicides?|produits? phytosanitaires?|कीटनाशक|खरपतवारनाशी|फफूंदनाशक"
            + "|viuatilifu|dawa ya wadudu)\\b"
    );
    private static final Pattern PLANT_DIAGNOSIS = Pattern.compile(
        "(?iU)\\b(diagnos\\w*|disease|pests?|symptom\\w*|blight|spots|yellowing|wilting"
            + "|enfermedad\\w*|plagas?|síntomas?|maladies?|ravageurs?|symptômes?|doenças?|pragas?"
            + "|बीमारी|कीट|magonjwa?|ugonjwa|wadudu)\\b"
            + "|\\bwhat(?:'s| is) wrong\\b|\\bleaf spot\\b|\\byellow leaves\\b"
    );
    private static final Pattern FERTILIZER_QUESTION = Pattern.compile(
        "(?iU)\\b(fertili[sz]ers?|nutrients?|manure|compost|fertilizantes?|abono|engrais|adubo"
            + "|खाद|उर्वरक|mbolea)\\b"
    );
    private static final Pattern EXACT_FERTILIZER_RATE_REQUEST = Pattern.compile(
        "(?iU)\\b(how much|how many|dose|rate|quantity|amount|exact|kg\\s*/\\s*ha"
            + "|cuánto|cuanto|dosis|cantidad|combien|quantité|quanto|quantidade"
            + "|कितना|मात्रा|kiasi|kiwango|ngapi)\\b"
    );
    private static final Pattern BARE_DOSE = Pattern.compile(
        "(?iU)\\b\\d+(?:[.,]\\d+)?\\s*(?:kg|kilograms?|g|grams?|lb|pounds?|ml|l|lit(?:er|re)s?)"
            + "(?:\\s+(?:of|de|d')\\s*\\p{L}+|\\s*(?:N|P2O5|K2O)\\b)"
    );
    private static final Pattern DOSE = Pattern.compile(
        "(?i)\\b(?:\\d+(?:[.,]\\d+)?|zero|one|two|three|four|five|six|seven|eight|nine|ten)"
            + "\\s*(?:kg|kilograms?|g|grams?|mg|milligrams?|l|lit(?:er|re)s?|ml|millilit(?:er|re)s?"
            + "|oz|ounces?|lb|pounds?|tsp|teaspoons?|tbsp|tablespoons?|bags?|sachets?)"
            + "\\s*(?:/|per\\s+)(?:ha|hectare|acre|plant|lit(?:er|re)|gallon)\\b"
    );
    private static final Pattern NUTRIENT_RATIO = Pattern.compile(
        "\\b\\d{1,3}[-:/]\\d{1,3}(?:[-:/]\\d{1,3})?\\b"
    );
    private static final Pattern URL = Pattern.compile("(?i)https?://\\S+");
    private static final Pattern WEATHER_CLAIM = Pattern.compile(
        "(?i)\\b(it will rain|rain is expected|forecast (?:shows|indicates)|currently raining|"
            + "chance of rain|rain tomorrow|temperature will)\\b"
    );

    public String enforce(
        String question,
        String proposedAnswer,
        boolean vettedSourceUsed,
        List<String> trustedGuidance,
        List<String> verifiedSources,
        boolean liveWeatherUsed
    ) {
        if (PESTICIDE_QUESTION.matcher(question).find()) {
            return "I can't recommend a pesticide product or dose without a vetted local pesticide source. "
                + "Approved products, legality, and safety requirements vary by country. Contact your local "
                + "agriculture authority or extension service, and follow local rules and product labels.";
        }
        if (!vettedSourceUsed
            && EXACT_FERTILIZER_RATE_REQUEST.matcher(question).find()
            && FERTILIZER_QUESTION.matcher(question).find()) {
            return fertilizerFallback();
        }
        if (!vettedSourceUsed
            && FERTILIZER_QUESTION.matcher(question).find()
            && (DOSE.matcher(proposedAnswer).find()
                || BARE_DOSE.matcher(proposedAnswer).find()
                || NUTRIENT_RATIO.matcher(proposedAnswer).find())) {
            return fertilizerFallback();
        }
        if (containsUnsupportedDose(proposedAnswer, vettedSourceUsed, trustedGuidance)) {
            return fertilizerFallback();
        }
        if (PLANT_DIAGNOSIS.matcher(question).find()) {
            return "I can't reliably diagnose plant disease or pest damage from this chat alone. Note the "
                + "affected plant parts and recent field conditions, and ask a local extension service or "
                + "qualified agronomist to confirm before applying treatments.";
        }
        if (!liveWeatherUsed && WEATHER_CLAIM.matcher(proposedAnswer).find()) {
            return "I don't have a live forecast for this location in this response, so I can't give "
                + "weather-based timing advice. Check a current local forecast and consult your local "
                + "agricultural extension service.";
        }
        if (containsUnverifiedUrl(proposedAnswer, verifiedSources)) {
            return "I can't verify the source cited in that draft. I don't want to present an unverified "
                + "agronomy claim as fact; contact your local agricultural extension service for guidance.";
        }
        return proposedAnswer;
    }

    private static boolean containsUnsupportedDose(
        String answer,
        boolean vettedSourceUsed,
        List<String> trustedGuidance
    ) {
        for (Pattern pattern : List.of(DOSE, BARE_DOSE)) {
            Matcher matcher = pattern.matcher(answer);
            while (matcher.find()) {
                String dose = matcher.group().toLowerCase(Locale.ROOT);
                if (!vettedSourceUsed || trustedGuidance.stream()
                    .noneMatch(source -> source.toLowerCase(Locale.ROOT).contains(dose))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsUnverifiedUrl(String answer, List<String> verifiedSources) {
        Matcher matcher = URL.matcher(answer);
        while (matcher.find()) {
            String url = matcher.group().replaceAll("[).,;]+$", "");
            if (verifiedSources.stream().noneMatch(source -> source.contains(url))) {
                return true;
            }
        }
        return false;
    }

    private static String fertilizerFallback() {
        return "I can't provide an exact fertilizer quantity without a soil test or a cited, applicable local "
            + "package-of-practices source supporting that rate. Use a recent soil test and ask your local "
            + "agricultural extension service for a crop- and region-specific recommendation.";
    }
}
