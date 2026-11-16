package com.chaitanya.tradeflow.notification.event;

import java.time.Instant;
import java.util.UUID;

public record RiskDecidedEvent(UUID eventId, Instant occurredAt, Long orderId, boolean approved,
                               String reason) {
}
