package com.chaitanya.tradeflow.risk.event;

import com.chaitanya.tradeflow.risk.rules.RiskDecision;

import java.time.Instant;
import java.util.UUID;

public record RiskDecidedEvent(
        UUID eventId,
        Instant occurredAt,
        Long orderId,
        boolean approved,
        String reason
) {
    public static RiskDecidedEvent from(Long orderId, RiskDecision decision){
        return new RiskDecidedEvent(UUID.randomUUID(),Instant.now(),
                orderId, decision.approved(), decision.reason());
    }
}
