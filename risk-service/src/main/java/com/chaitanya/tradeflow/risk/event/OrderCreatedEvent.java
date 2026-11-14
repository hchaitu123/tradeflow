package com.chaitanya.tradeflow.risk.event;

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
}
