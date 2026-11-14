package com.chaitanya.tradeflow.order.event;

import com.chaitanya.tradeflow.order.domain.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        Long accountId,
        List<OrderCreatedEventItem> items

) {
    public static OrderCreatedEvent from(Order order){
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                order.getOrderId(),
                order.getAccountId(),
                order.getItems().stream().map(OrderCreatedEventItem::from).toList()
        );
    }
}
