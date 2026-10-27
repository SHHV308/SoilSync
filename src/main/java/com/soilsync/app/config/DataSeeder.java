package com.soilsync.app.config;

import com.soilsync.app.model.CareEvent;
import com.soilsync.app.model.Plant;
import com.soilsync.app.model.Reminder;
import com.soilsync.app.repository.CareEventRepository;
import com.soilsync.app.repository.PlantRepository;
import com.soilsync.app.repository.ReminderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(PlantRepository plantRepository,
                                     CareEventRepository careEventRepository,
                                     ReminderRepository reminderRepository) {
        return args -> {
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
        };
    }
}
