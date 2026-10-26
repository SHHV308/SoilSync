package com.soilsync.app.controller;

import com.soilsync.app.model.Order;
import com.soilsync.app.repository.OrderRepository;
import com.soilsync.app.repository.PlantRepository;
import com.soilsync.app.repository.ProductRepository;
import com.soilsync.app.repository.ReminderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    private final ProductRepository productRepository;
    private final PlantRepository plantRepository;
    private final ReminderRepository reminderRepository;
    private final OrderRepository orderRepository;

    public DashboardController(ProductRepository productRepository,
                              PlantRepository plantRepository,
                              ReminderRepository reminderRepository,
                              OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.plantRepository = plantRepository;
        this.reminderRepository = reminderRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard() {
        long productCount = productRepository.count();
        long plantCount = plantRepository.count();
        long activeReminders = reminderRepository.count();
        double revenue = orderRepository.findAll().stream().mapToDouble(Order::getTotalAmount).sum();

        List<Map<String, Object>> recentOrders = new ArrayList<>();
        for (Order order : orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))) {
            Map<String, Object> item = new HashMap<>();
            item.put("customerName", order.getCustomerName());
            item.put("status", order.getStatus());
            item.put("totalAmount", order.getTotalAmount());
            item.put("createdAt", order.getCreatedAt());
            recentOrders.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("productCount", productCount);
        response.put("plantCount", plantCount);
        response.put("activeReminderCount", activeReminders);
        response.put("monthlyRevenue", revenue);
        response.put("recentOrders", recentOrders);
        response.put("summary", "Demo season summary for Karnataka gardens and field plots");
        return response;
    }
}
