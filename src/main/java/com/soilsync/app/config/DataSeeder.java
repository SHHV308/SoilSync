package com.soilsync.app.config;

import com.soilsync.app.model.CareEvent;
import com.soilsync.app.model.Order;
import com.soilsync.app.model.OrderItem;
import com.soilsync.app.model.Plant;
import com.soilsync.app.model.Product;
import com.soilsync.app.model.Reminder;
import com.soilsync.app.repository.CareEventRepository;
import com.soilsync.app.repository.OrderItemRepository;
import com.soilsync.app.repository.OrderRepository;
import com.soilsync.app.repository.PlantRepository;
import com.soilsync.app.repository.ProductRepository;
import com.soilsync.app.repository.ReminderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(ProductRepository productRepository,
                                     PlantRepository plantRepository,
                                     CareEventRepository careEventRepository,
                                     ReminderRepository reminderRepository,
                                     OrderRepository orderRepository,
                                     OrderItemRepository orderItemRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                    new Product("H-course", "Soil Booster", 1490, "Bio-organic soil treatment designed to improve texture, moisture retention, and root-zone activity for vegetable and grain plots.", "bag", 46, "Apply as a soil amendment before sowing or during early plant growth."),
                    new Product("C-course", "Yield Booster", 1680, "Organic nutrient blend for crop vigor and balanced feeding in mixed-season farming and garden beds.", "bag", 38, "Use as a balanced feed during vegetative growth and flowering."),
                    new Product("Compost Mix", "Organic Compost", 980, "Mature compost enriched with nutrient-dense organic material for soil regeneration and healthier root zones.", "bag", 72, "Ideal for pots, raised beds, and field application."),
                    new Product("Neem Soil Guard", "Plant Protection", 1240, "Organic bio-protectant supporting healthier soil and root-zone balance under changing weather conditions.", "bottle", 29, "Apply around roots or as a soil drench in monitoring intervals."),
                    new Product("Microbe Pack", "Microbial Support", 1110, "Microbial soil enhancement aimed at improving organic matter breakdown and nutrient cycling.", "pack", 34, "Use with compost or as part of a seasonal soil program.")
                ));
            }

            if (plantRepository.count() == 0) {
                Plant tomato = plantRepository.save(new Plant(
                    "Tomato Bed 1",
                    "Tomato hybrid",
                    "Greenhouse A",
                    "Loamy soil",
                    "Strong flowering stage. Leaves are deep green and the plant is setting fruit steadily.",
                    "Healthy",
                    LocalDateTime.now().minusDays(2),
                    LocalDateTime.now().plusDays(1)
                ));

                Plant chilli = plantRepository.save(new Plant(
                    "Chilli Patch",
                    "Chilli variety",
                    "North polyhouse",
                    "Sandy loam",
                    "Healthy growth after recent organic feed. Soil remains slightly dry at the top layer.",
                    "Stable",
                    LocalDateTime.now().minusDays(4),
                    LocalDateTime.now().plusDays(2)
                ));

                careEventRepository.saveAll(List.of(
                    new CareEvent(tomato.getId(), "watering", "Watered the crop at the recommended early-morning window.", LocalDateTime.now().minusDays(1)),
                    new CareEvent(chilli.getId(), "fertilizer", "Applied organic feed to support strong flowering and fruit set.", LocalDateTime.now().minusDays(3)),
                    new CareEvent(tomato.getId(), "pruning", "Trimmed low leaves to improve airflow around the fruit set.", LocalDateTime.now().minusDays(2))
                ));

                reminderRepository.saveAll(List.of(
                    new Reminder(tomato.getId(), "watering", LocalDateTime.now().plusDays(1), "scheduled", "Check soil moisture before applying water."),
                    new Reminder(chilli.getId(), "fertilizer", LocalDateTime.now().plusDays(4), "scheduled", "Apply organic feed after soil moisture check."),
                    new Reminder(tomato.getId(), "inspection", LocalDateTime.now().plusDays(3), "pending", "Review leaf color and pest signs.")
                ));
            }

            if (orderRepository.count() == 0) {
                Order firstOrder = orderRepository.save(new Order("Ramesh K", 6340, "In transit", LocalDateTime.now().minusDays(4)));
                Order secondOrder = orderRepository.save(new Order("Seema P", 2890, "Delivered", LocalDateTime.now().minusDays(12)));

                orderItemRepository.saveAll(List.of(
                    new OrderItem(firstOrder.getId(), "H-course", 2, 1490),
                    new OrderItem(firstOrder.getId(), "Compost Mix", 1, 980),
                    new OrderItem(secondOrder.getId(), "C-course", 1, 1680),
                    new OrderItem(secondOrder.getId(), "Neem Soil Guard", 1, 1240)
                ));
            }
        };
    }
}
