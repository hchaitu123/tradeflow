package com.chaitanya.tradeflow.order.web.dto;

import com.chaitanya.tradeflow.order.domain.Order;
import com.chaitanya.tradeflow.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(Long orderId, Long accountId, OrderStatus status,
                            String rejectionReason, Instant createdAt, List<OrderItemResponse> items) {

    public static OrderResponse create(Order order) {
        return new OrderResponse(order.getOrderId(), order.getAccountId(),
                 order.getStatus(), order.getRejectionReason(), order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse :: create).toList());
    }
}
