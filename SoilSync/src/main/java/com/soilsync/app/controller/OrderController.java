package com.soilsync.app.controller;

import com.soilsync.app.model.CreateOrderRequest;
import com.soilsync.app.model.Order;
import com.soilsync.app.model.OrderItem;
import com.soilsync.app.model.OrderItemRequest;
import com.soilsync.app.model.Product;
import com.soilsync.app.repository.OrderItemRepository;
import com.soilsync.app.repository.OrderRepository;
import com.soilsync.app.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderController(OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/api/orders")
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @PostMapping("/api/orders")
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        if (request == null || request.customerName() == null || request.customerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required.");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must include at least one item.");
        }

        double totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.items()) {
            if (itemRequest == null || itemRequest.productId() == null || itemRequest.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each cart item must include a valid product ID and quantity.");
            }

            Product product = productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + itemRequest.productId()));

            if (product.getStock() < itemRequest.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for " + product.getName());
            }

            product.setStock(product.getStock() - itemRequest.quantity());
            productRepository.save(product);

            double lineTotal = product.getPrice() * itemRequest.quantity();
            totalAmount += lineTotal;
            orderItems.add(new OrderItem(null, product.getName(), itemRequest.quantity(), product.getPrice()));
        }

        Order order = orderRepository.save(new Order(request.customerName(), totalAmount, "Processing", LocalDateTime.now()));

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
