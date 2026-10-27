package com.soilsync.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soilsync.app.model.AssistantChatRequest;
import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantChatServiceTest {

    private final LocationCandidate location = new LocationCandidate(
        "101", "Nairobi", "Nairobi County", "", "Kenya", "KE",
        -1.2864, 36.8172, "Africa/Nairobi", "UNCLASSIFIED", "Open-Meteo geocoding"
    );

    @Test
    void pesticideDoseRequestIsReplacedWithTheCodeEnforcedSafeAnswer() {
        ScriptedProvider provider = new ScriptedProvider(
            new LlmProvider.LlmReply("Apply 5 ml per liter.", List.of())
        );
        AssistantChatService assistantChatService = service(
            provider,
            weatherClient(null, null),
            cropAdviceService(weatherClient(null, null), null)
        );

        AssistantChatService.ChatResult result = assistantChatService.chat(new AssistantChatRequest(
            "What pesticide dose should I use?",
            null,
            null,
            null,
            null,
            "en",
            "metric"
        ));

        assertThat(result.response().answer())
            .doesNotContainPattern("\\d")
            .contains("local pesticide source");
    }

    @Test
    void liveWeatherToolUsesSelectedLocationAndReturnsRequestedImperialUnits() {
        WeatherSnapshot weather = new WeatherSnapshot(
            20.0, 55, 2.0, 80, "Forecast summary", "Open-Meteo",
            "2026-04-15", 10.0, 25.0
        );
        RegionalWeatherClient weatherClient = weatherClient(
            location,
            new OpenMeteoClient.WeatherAtLocation("Nairobi, Kenya", weather, location)
        );
        ScriptedProvider provider = new ScriptedProvider(
            new LlmProvider.LlmReply(null, List.of(
                new LlmProvider.LlmToolCall("call-weather", "get_live_weather_forecast", "{}")
            )),
            new LlmProvider.LlmReply("Use the forecast with field conditions.", List.of())
        );
        AssistantChatService assistantChatService = service(
            provider,
            weatherClient,
            cropAdviceService(weatherClient, null)
        );

        AssistantChatService.ChatResult result = assistantChatService.chat(new AssistantChatRequest(
            "How should I use the forecast for timing?",
            location,
            null,
            null,
            null,
            "en",
            "imperial"
        ));

        String toolContent = provider.requests().get(1).stream()
            .filter(message -> "tool".equals(message.role()))
            .findFirst()
            .orElseThrow()
            .content();
        assertThat(toolContent)
            .contains("\"currentTemperature\":68.0")
            .contains("\"forecastPrecipitation\":0.39")
            .contains("\"forecastHigh\":77.0")
            .contains("\"precipitationUnit\":\"in\"");
        assertThat(result.response().units()).isEqualTo("imperial");
        assertThat(result.weather()).isEqualTo(weather);
        assertThat(result.response().sources()).anyMatch(source -> source.contains("Open-Meteo"));
    }

    @Test
    void unsourcedFertilizerDoseIsBlockedAfterRegionalToolReturnsNoVettedSource() {
        WeatherSnapshot weather = new WeatherSnapshot(
            20.0, 55, 0.0, 10, "No rain expected.", "Open-Meteo"
        );
        CropAdviceResponse regionalAdvice = new CropAdviceResponse(
            "Nairobi, Kenya",
            "Maize",
            "Flowering",
            "No vetted crop-specific guidance is available.",
            List.of("Use a soil test and ask a local extension service."),
            weather,
            List.of(
                "Live location and weather: Open-Meteo",
                "No vetted regional crop source is currently configured for this country and crop."
            ),
            java.time.Instant.now(),
            location
        );
        RegionalWeatherClient weatherClient = weatherClient(
            location,
            new OpenMeteoClient.WeatherAtLocation("Nairobi, Kenya", weather, location)
        );
        RegionalCropAdviceService cropAdviceService = cropAdviceService(weatherClient, regionalAdvice);
        ScriptedProvider provider = new ScriptedProvider(
            new LlmProvider.LlmReply(null, List.of(
                new LlmProvider.LlmToolCall("call-advice", "get_regional_crop_advice", "{}")
            )),
            new LlmProvider.LlmReply("Apply 25 kg per hectare.", List.of())
        );
        AssistantChatService assistantChatService = service(provider, weatherClient, cropAdviceService);

        AssistantChatService.ChatResult result = assistantChatService.chat(new AssistantChatRequest(
            "How much fertilizer should I apply?",
            location,
            "maize",
            "flowering",
            "loam",
            "en",
            "metric"
        ));

        assertThat(result.response().answer())
            .doesNotContainPattern("\\d")
            .contains("soil test", "package-of-practices source");
        assertThat(result.response().vettedRegionalSourceUsed()).isFalse();
        assertThat(provider.requests()).hasSize(2);
    }

    @Test
    void forecastClaimIsRejectedWhenNoLocationWasSelected() {
        RegionalWeatherClient weatherClient = weatherClient(null, null);
        ScriptedProvider provider = new ScriptedProvider(
            new LlmProvider.LlmReply("The forecast shows rain tomorrow.", List.of())
        );
        AssistantChatService assistantChatService = service(
            provider,
            weatherClient,
            cropAdviceService(weatherClient, null)
        );

        AssistantChatService.ChatResult result = assistantChatService.chat(new AssistantChatRequest(
            "When should I apply nutrients?",
            null,
            null,
            null,
            null,
            "en",
            "metric"
        ));

        assertThat(result.response().answer()).contains("don't have a live forecast");
        assertThat(result.response().sources()).isEmpty();
    }

    private static AssistantChatService service(
        LlmProvider llmProvider,
        RegionalWeatherClient weatherClient,
        RegionalCropAdviceService cropAdviceService
    ) {
        return new AssistantChatService(
            llmProvider,
            weatherClient,
            cropAdviceService,
            new AssistantSafetyPolicy(),
            new ObjectMapper()
        );
    }

    private static RegionalWeatherClient weatherClient(
        LocationCandidate selectedLocation,
        OpenMeteoClient.WeatherAtLocation weather
    ) {
        return new RegionalWeatherClient() {
            @Override
            public List<LocationCandidate> searchLocations(String query) {
                return selectedLocation == null ? List.of() : List.of(selectedLocation);
            }

            @Override
            public OpenMeteoClient.WeatherAtLocation getWeather(
                String region,
                LocationCandidate selection
            ) {
                return weather;
            }
        };
    }

    private static RegionalCropAdviceService cropAdviceService(
        RegionalWeatherClient weatherClient,
        CropAdviceResponse response
    ) {
        return new RegionalCropAdviceService(
            weatherClient,
            new RegionalKnowledgeBase(new DefaultResourceLoader())
        ) {
            @Override
            public CropAdviceResponse advise(CropAdviceRequest request) {
                return response;
            }
        };
    }

    private static final class ScriptedProvider implements LlmProvider {

        private final Deque<LlmReply> replies;
        private final List<List<LlmMessage>> requests = new ArrayList<>();

        private ScriptedProvider(LlmReply... replies) {
            this.replies = new ArrayDeque<>(List.of(replies));
        }

        @Override
        public LlmReply complete(List<LlmMessage> messages, List<com.fasterxml.jackson.databind.JsonNode> tools) {
            requests.add(List.copyOf(messages));
            if (replies.isEmpty()) {
                throw new AssertionError("The chat service made more provider calls than expected.");
            }
            return replies.removeFirst();
        }

        private List<List<LlmMessage>> requests() {
            return requests;
        }
    }
}
