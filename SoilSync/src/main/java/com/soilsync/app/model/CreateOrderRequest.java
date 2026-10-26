package com.soilsync.app.model;

import java.util.List;

public record CreateOrderRequest(String customerName, List<OrderItemRequest> items) {
}
