package com.soilsync.app;

import com.soilsync.app.controller.OrderController;
import com.soilsync.app.model.CropAdviceRequest;
import com.soilsync.app.model.CropAdviceResponse;
import com.soilsync.app.model.CreateOrderRequest;
import com.soilsync.app.model.OrderItemRequest;
import com.soilsync.app.model.Product;
import com.soilsync.app.model.WeatherSnapshot;
import com.soilsync.app.repository.ProductRepository;
import com.soilsync.app.service.OpenMeteoClient;
import com.soilsync.app.service.RegionalCropAdviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    void cropAdviceUsesRegionalLiveWeatherAndCropStage() {
        RegionalCropAdviceService regionalCropAdviceService = new RegionalCropAdviceService(region ->
            new OpenMeteoClient.WeatherAtLocation(
                "Mysuru, Karnataka",
                new WeatherSnapshot(
                    29.0, 72, 0, 95,
                    "Next 24 hours: up to 95% rain chance, 18.0 mm expected rain, and a high near 31.0°C.",
                    "https://open-meteo.com/"
                )
            )
        );

        CropAdviceResponse response = regionalCropAdviceService.advise(
            new CropAdviceRequest("Mysuru, Karnataka", "Tomato", "Flowering", "Clay", "Rain is forecast")
        );

        assertThat(response.location()).isEqualTo("Mysuru, Karnataka");
        assertThat(response.answer()).contains("Tomato", "flowering", "95% rain chance");
        assertThat(response.recommendations()).anyMatch(item -> item.contains("heavy-rain window"));
        assertThat(response.recommendations()).anyMatch(item -> item.contains("flowering"));
        assertThat(response.dataSources()).anyMatch(item -> item.contains("Open-Meteo"));
    }

    @Test
    void orderControllerCreatesOrderAndUpdatesStock() {
        Product product = productRepository.save(new Product("Demo Blend", "Soil Booster", 500, "Demo product", "bag", 10, "Use in beds"));

        ResponseEntity<?> response = orderController.createOrder(new CreateOrderRequest("Test Farmer", List.of(new OrderItemRequest(product.getId(), 2))));

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
    }
}
