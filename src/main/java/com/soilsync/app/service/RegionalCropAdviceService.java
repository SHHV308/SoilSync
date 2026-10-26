package com.soilsync.app.service;

import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.WeatherSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RegionalCropAdviceService {

    private static final Map<String, CropPlan> CROP_PLANS = Map.ofEntries(
        Map.entry("rice", new CropPlan(
            "Rice is a high nitrogen-demanding crop. Base phosphorus and potassium on a soil test; split nitrogen across establishment, active tillering, and panicle initiation rather than applying it all at once.",
            "For rice, urea is a common nitrogen source; DAP or SSP can supply basal phosphorus, and MOP supplies potassium. Choose among them using the soil test and district rice package.",
            "Use a locally recommended rice nutrient schedule for the district."
        )),
        Map.entry("wheat", new CropPlan(
            "Wheat generally needs phosphorus at sowing and nitrogen split between sowing and early crown-root/tillering growth. Base potassium on the soil test and local package of practices.",
            "For wheat, the local schedule commonly combines a basal phosphorus source such as DAP/SSP with split urea; add MOP only if the soil test and local recommendation call for potassium.",
            "Do not apply nitrogen to saturated soil or immediately before forecast heavy rain."
        )),
        Map.entry("maize", new CropPlan(
            "Maize responds to split nitrogen: apply a portion at planting and side-dress around knee-high growth and before tasseling. Place phosphorus near the root zone and use soil-test results for potassium.",
            "For maize, use a soil-test-guided basal phosphorus source such as DAP/SSP and split nitrogen from urea; potassium source such as MOP is conditional on the soil test.",
            "Avoid a single large nitrogen application, especially on light-textured soil."
        )),
        Map.entry("cotton", new CropPlan(
            "Cotton nutrient demand rises from squaring through boll development. Use soil-test-based phosphorus and potassium, and split nitrogen through the season instead of over-applying early.",
            "For cotton, split urea-based nitrogen and use basal phosphorus (DAP/SSP) or potassium (MOP) only at rates in the district package and confirmed by soil testing.",
            "Keep fertilizer away from the stem and follow the district cotton package."
        )),
        Map.entry("tomato", new CropPlan(
            "Tomato needs balanced nutrition, with adequate phosphorus for establishment and steady potassium as flowering and fruit fill progress. Avoid excess nitrogen once flowering starts; use split applications or fertigation only if your irrigation setup supports it.",
            "For tomato, consider well-decomposed compost/FYM for organic matter, a basal phosphorus source if required, and split nitrogen/potassium through the crop. Select DAP/SSP, urea, or MOP/SOP only according to the soil test and local horticulture schedule.",
            "Base rates on a soil test and the local horticulture recommendation."
        )),
        Map.entry("chilli", new CropPlan(
            "Chilli benefits from balanced basal nutrition and split feeding, with potassium support during flowering and fruiting. Excess nitrogen can promote leafy growth at the expense of fruiting.",
            "For chilli, use compost/FYM where appropriate and split nutrient applications; choose phosphorus, nitrogen, and potassium sources from soil-test results rather than applying a fixed NPK mix.",
            "Use only soil-test-based rates and avoid feeding water-stressed plants."
        )),
        Map.entry("sugarcane", new CropPlan(
            "Sugarcane has a long nutrient-demand period. Split nitrogen and potassium across early establishment and tillering; phosphorus is usually planned at planting based on soil testing and the local sugarcane package.",
            "For sugarcane, the district schedule may use basal DAP/SSP with split urea and MOP; confirm the rates and timing from the soil test and local sugar factory/agriculture package.",
            "Do not apply the full nitrogen dose at planting."
        )),
        Map.entry("groundnut", new CropPlan(
            "Groundnut is a legume and usually needs modest starter nitrogen, with phosphorus and calcium management guided by soil testing and local recommendations. Do not assume more nitrogen will increase pod yield.",
            "For groundnut, prioritize a locally recommended phosphorus source and rhizobium inoculant; use gypsum/calcium only when advised for the soil and crop stage. Avoid routine heavy urea.",
            "Ask your local agriculture officer about gypsum/calcium timing for your soil and variety."
        )),
        Map.entry("pulses", new CropPlan(
            "For pulses, avoid high nitrogen because it can suppress nodulation. Prioritize soil-test-based phosphorus and use the crop-appropriate rhizobium inoculant if recommended for your pulse and district.",
            "For pulses, a soil-test-guided phosphorus source and crop-specific rhizobium inoculant are generally more relevant than a high-nitrogen fertilizer; confirm whether sulphur or micronutrients are needed before applying.",
            "Confirm micronutrient or sulphur needs with a soil test rather than applying them routinely."
        )),
        Map.entry("millet", new CropPlan(
            "Millets typically use modest, split nitrogen with phosphorus planned at sowing. Match the dose to rainfall, soil fertility, and the district's dryland package; do not apply ahead of a downpour.",
            "For millet, a basal phosphorus source and split urea are common options in local packages; potassium or other amendments should depend on soil-test results and the district's dryland recommendation.",
            "Use split application to reduce losses in rainfed fields."
        ))
    );

    private final RegionalWeatherClient weatherClient;

    public RegionalCropAdviceService(RegionalWeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    public CropAdviceResponse advise(CropAdviceRequest request) {
        OpenMeteoClient.WeatherAtLocation live = weatherClient.getWeather(request.region().trim());
        WeatherSnapshot weather = live.weather();
        List<String> recommendations = new ArrayList<>();

        CropPlan plan = CROP_PLANS.get(normalizeCrop(request.crop()));
        if (plan == null) {
            recommendations.add("There is no validated crop-specific nutrient profile for " + request.crop()
                + " in this advisor yet. Ask your local agriculture extension office for the district crop package before choosing fertilizer.");
        } else {
            recommendations.add(plan.nutrientGuidance());
            recommendations.add(plan.materialGuidance());
            recommendations.add(plan.caution());
        }

        recommendations.add(stageGuidance(request.crop(), request.growthStage()));
        recommendations.add(soilGuidance(request.soilType()));
        recommendations.add("If a soil test confirms a need, common nutrient sources include urea for nitrogen, DAP or SSP for phosphorus (their nitrogen/sulphur contributions differ), and MOP for potassium. These are not interchangeable; select the material and rate from your soil-test result and local crop package.");
        if (weather.forecastRainProbabilityPercent() >= 70 || weather.precipitationMm() >= 5) {
            recommendations.add("Rain is likely or occurring near " + live.location()
                + ". Delay surface-applied fertilizer until the soil is workable and the heavy-rain window has passed to reduce runoff and nutrient loss.");
        } else if (weather.temperatureC() >= 35) {
            recommendations.add("It is currently hot near " + live.location()
                + ". Avoid applying fertilizer to heat- or drought-stressed plants; apply only when the crop is adequately watered and conditions are cooler.");
        } else {
            recommendations.add("Current conditions do not show an immediate heavy-rain or extreme-heat warning. Check the field soil moisture before top-dressing.");
        }

        String questionContext = request.question() == null || request.question().isBlank()
            ? ""
            : " Your question, “" + request.question().trim() + "”, should be assessed using these field and weather details.";
        String answer = String.format(
            "Regional crop check for %s: %s at %s on %s soil. Live weather is %.1f°C with %d%% humidity and %.1f mm current precipitation. %s%s Fertilizer rates cannot be safely calculated from weather and location alone; use a recent soil test and the district agriculture department or university recommendation for exact quantities.",
            live.location(),
            request.crop(),
            request.growthStage().toLowerCase(Locale.ROOT),
            request.soilType().toLowerCase(Locale.ROOT),
            weather.temperatureC(),
            weather.humidityPercent(),
            weather.precipitationMm(),
            weather.forecastSummary(),
            questionContext
        );

        return new CropAdviceResponse(
            live.location(),
            request.crop(),
            request.growthStage(),
            answer,
            recommendations,
            weather,
            List.of(
                "Live location and weather: Open-Meteo geocoding and forecast APIs (" + weather.source() + ")",
                "Soil type is supplied by the grower; no lab soil test or live soil nutrient dataset is queried.",
                "Nutrient guidance: crop-stage agronomy rules; exact fertilizer quantities require a soil test and local crop package."
            ),
            Instant.now()
        );
    }

    private static String normalizeCrop(String crop) {
        return crop.toLowerCase(Locale.ROOT).trim();
    }

    private static String stageGuidance(String crop, String growthStage) {
        String stage = growthStage.toLowerCase(Locale.ROOT);
        if (stage.contains("sowing") || stage.contains("planting")) {
            return "For the sowing / planting stage of " + crop
                + ", prioritize correct seed or transplant establishment; apply basal fertilizer only at soil-test-based rates and keep it from direct contact with seed or young roots.";
        }
        if (stage.contains("establishment") || stage.contains("early")) {
            return "During establishment of " + crop
                + ", avoid a heavy one-time dose. Apply only the locally recommended early split after checking stand establishment and soil moisture.";
        }
        if (stage.contains("vegetative")) {
            return "During vegetative growth of " + crop
                + ", this is generally the period to assess planned split nitrogen. Apply only the scheduled portion if the crop is actively growing and rain is not imminent.";
        }
        if (stage.contains("flowering")) {
            return "At flowering in " + crop
                + ", avoid excess nitrogen; maintain even soil moisture and consider potassium only where the crop plan or soil test indicates a need.";
        }
        if (stage.contains("filling")) {
            return "During fruit / grain filling in " + crop
                + ", avoid late heavy nitrogen. Keep moisture steady and use potassium only as advised by the soil test and the district crop package.";
        }
        return "With harvest approaching for " + crop
            + ", avoid late nitrogen applications unless a local crop specialist has specifically advised them; follow the crop's pre-harvest guidance.";
    }

    private static String soilGuidance(String soilType) {
        return switch (soilType.toLowerCase(Locale.ROOT)) {
            case "sandy" -> "Sandy soil loses nutrients more easily: use smaller split applications and avoid applying before rain or irrigation that could leach nutrients.";
            case "clay" -> "Clay soil can retain water and nutrients: avoid fertilizer application while waterlogged, and use soil-test results to prevent excess phosphorus or potassium.";
            case "loam" -> "Loam is generally well-drained, but the fertilizer plan should still follow a soil test and the crop's local package of practices.";
            case "red" -> "Red soils can be low in organic matter and some nutrients, but deficiencies vary by field. Test pH and available nutrients before selecting amendments.";
            case "black" -> "Black soils may retain moisture and nutrients; avoid applying fertilizer to saturated soil and use test results to guide phosphorus and potassium.";
            default -> "Soil type alone cannot identify nutrient deficiencies. Use a recent soil test to select fertilizer and rates.";
        };
    }

    private record CropPlan(String nutrientGuidance, String materialGuidance, String caution) {
    }
}
