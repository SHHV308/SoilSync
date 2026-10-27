package com.soilsync.app;

import com.soilsync.app.controller.OrderController;
import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.CreateOrderRequest;
import com.soilsync.app.model.LocationCandidate;
import com.soilsync.app.model.OrderItemRequest;
import com.soilsync.app.model.Product;
import com.soilsync.app.model.WeatherSnapshot;
import com.soilsync.app.repository.ProductRepository;
import com.soilsync.app.service.OpenMeteoClient;
import com.soilsync.app.service.RegionalCropAdviceService;
import com.soilsync.app.service.RegionalKnowledgeBase;
import com.soilsync.app.service.RegionalWeatherClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SoilSyncApplicationTests {

    @Autowired
    private OrderController orderController;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void cropAdviceWithoutVettedRegionalSourceOnlyReturnsGeneralPrinciples() {
        LocationCandidate nairobi = new LocationCandidate(
            "123", "Nairobi", "Nairobi County", "", "Kenya", "KE",
            -1.2864, 36.8172, "Africa/Nairobi", "UNCLASSIFIED", "Open-Meteo geocoding"
        );
        RegionalWeatherClient weatherClient = new RegionalWeatherClient() {
            @Override
            public List<LocationCandidate> searchLocations(String query) {
                return List.of(nairobi);
            }

            @Override
            public OpenMeteoClient.WeatherAtLocation getWeather(
                String region,
                LocationCandidate selection
            ) {
                return new OpenMeteoClient.WeatherAtLocation(
                    "Nairobi, Kenya",
                    new WeatherSnapshot(
                        29.0, 72, 0, 95,
                        "Next 24 hours: up to 95% rain chance, 18.0 mm expected rain, and a high near 31.0°C.",
                        "https://open-meteo.com/"
                    ),
                    nairobi
                );
            }
        };
        RegionalCropAdviceService regionalCropAdviceService = new RegionalCropAdviceService(
            weatherClient,
            new RegionalKnowledgeBase(new DefaultResourceLoader())
        );
        LocationCandidate enteredCoordinates = regionalCropAdviceService.coordinateLocation(
            -1.2864, 36.8172, "Nairobi County", "Kenya", "KE"
        );
        assertThat(enteredCoordinates.id()).isEqualTo("coordinates");
        assertThat(enteredCoordinates.countryCode()).isEqualTo("KE");
        assertThat(enteredCoordinates.climateZone()).isEqualTo("UNCLASSIFIED");

        CropAdviceResponse response = regionalCropAdviceService.advise(
            new CropAdviceRequest("Nairobi, Kenya", "Maize", "Flowering", "Clay", "What fertilizer should I use?")
        );

        assertThat(response.location()).isEqualTo("Nairobi, Nairobi County, Kenya");
        assertThat(response.locationDetails().countryCode()).isEqualTo("KE");
        assertThat(response.locationDetails().timezone()).isEqualTo("Africa/Nairobi");
        assertThat(response.locationDetails().climateZone()).isEqualTo("UNCLASSIFIED");
        assertThat(response.answer()).contains("No vetted crop-specific guidance", "Kenya", "Maize");
        assertThat(response.recommendations()).anyMatch(item -> item.contains("recent soil test"));
        assertThat(response.recommendations()).anyMatch(item -> item.contains("no locally validated timing threshold"));
        assertThat(response.recommendations().toString()).doesNotContain("DAP", "urea", "kg/ha");
        assertThat(response.dataSources()).anyMatch(item -> item.contains("Open-Meteo"));
        assertThat(response.dataSources()).anyMatch(item -> item.contains("No vetted regional crop source"));
    }

    @Test
    void orderControllerCreatesOrderAndUpdatesStock() {
        Product product = productRepository.save(new Product("Demo Blend", "Soil Booster", 500, "Demo product", "bag", 10, "Use in beds"));

        ResponseEntity<?> response = orderController.createOrder(new CreateOrderRequest("Test Farmer", List.of(new OrderItemRequest(product.getId(), 2))));

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
    }
}
